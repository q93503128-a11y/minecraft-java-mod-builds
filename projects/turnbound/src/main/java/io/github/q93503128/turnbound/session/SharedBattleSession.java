package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleAutoController;
import io.github.q93503128.turnbound.combat.BattleEngine;
import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.EffectType;
import io.github.q93503128.turnbound.combat.SharedBattleFactory;
import io.github.q93503128.turnbound.combat.SharedBattleOwnership;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.TargetRule;
import io.github.q93503128.turnbound.presentation.HeroBattleBarks;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One server-authoritative PvE BattleState shared by multiple player owners. */
final class SharedBattleSession {
    private record ReturnState(Vec3 position, float yaw, float pitch, boolean invisible) {}

    private final ServerLevel level;
    private final BattleEngine engine;
    private final String encounterId;
    private final UUID initiatorId;
    private final List<UUID> participantIds;
    private final Map<String, UUID> actorOwners;
    private final Map<UUID, ReturnState> returnStates = new LinkedHashMap<>();
    private final Map<UUID, String> rewardTransactionIds = new LinkedHashMap<>();
    private final Map<UUID, BattleResultSummary> resultSummaries = new LinkedHashMap<>();
    private final Set<UUID> autoOwners = new LinkedHashSet<>();
    private final Set<UUID> settledOwners = new LinkedHashSet<>();
    private final Set<UUID> readyToExit = new LinkedHashSet<>();
    private final BattlePresentation presentation = new BattlePresentation();
    private final Map<String, Boolean> barkDowned = new HashMap<>();
    private final Set<String> low50Barked = new HashSet<>();
    private final Set<String> low30Barked = new HashSet<>();
    private final Vec3 presentationCenter;
    private final Vec3 battleAnchor;
    private final float battleYaw;
    private final boolean autoAllowed;
    private final boolean speedAllowed;
    private final boolean fleeAllowed;

    private int speed = 1;
    private int delayTicks = 8;
    private int outcomePresentationTicks = -1;
    private boolean finished;
    private boolean readyShown;
    private boolean outcomeBarked;
    private long lastLogicTick = Long.MIN_VALUE;

    SharedBattleSession(ServerLevel level, List<ServerPlayer> participants, UUID initiatorId, String encounterId,
                        boolean autoAllowed, boolean speedAllowed, boolean fleeAllowed,
                        BattleArenaLocator.Arena arena, SharedBattleFactory.Blueprint blueprint) {
        if (level == null || participants == null || participants.size() < 2 || initiatorId == null
                || arena == null || blueprint == null) throw new IllegalArgumentException("Incomplete shared battle session");
        this.level = level;
        this.encounterId = encounterId == null ? "" : encounterId;
        this.initiatorId = initiatorId;
        this.autoAllowed = autoAllowed;
        this.speedAllowed = speedAllowed;
        this.fleeAllowed = fleeAllowed;
        this.participantIds = List.copyOf(blueprint.participants());
        this.actorOwners = Map.copyOf(blueprint.actorOwners());
        this.engine = new BattleEngine(blueprint.state());
        this.presentationCenter = arena.center();
        this.battleYaw = arena.facingYaw();

        Map<UUID, ServerPlayer> supplied = new LinkedHashMap<>();
        for (ServerPlayer player : participants) {
            if (player == null || player.level() != level) throw new IllegalArgumentException("Shared battle players must share one level");
            supplied.put(player.getUUID(), player);
        }
        if (!supplied.keySet().equals(new LinkedHashSet<>(participantIds)) || !supplied.containsKey(initiatorId)) {
            throw new IllegalArgumentException("Shared battle participant mismatch");
        }
        for (UUID id : participantIds) {
            ServerPlayer player = supplied.get(id);
            returnStates.put(id, new ReturnState(player.position(), player.getYRot(), player.getXRot(), player.isInvisible()));
            rewardTransactionIds.put(id, UUID.randomUUID().toString());
            resultSummaries.put(id, BattleResultSummary.none());
        }

        Vec3 resolvedAnchor;
        try {
            presentation.spawn(level, presentationCenter, battleYaw, engine.state().combatants());
            for (CombatantState unit : engine.state().combatants()) barkDowned.put(unit.instanceId(), unit.downed());
            Vec3 actualCenter = presentation.center();
            resolvedAnchor = actualCenter.lengthSqr() < .001 ? presentationCenter : actualCenter;
            for (ServerPlayer player : participants) enterBattle(player, resolvedAnchor);
        } catch (RuntimeException ex) {
            try { presentation.cleanup(level); } catch (RuntimeException ignored) { }
            for (ServerPlayer player : participants) restorePlayer(player);
            throw ex;
        }
        this.battleAnchor = resolvedAnchor;
        firstLivingHero().ifPresent(hero -> barkAll(hero.definition().id(), HeroBattleBarks.Event.START));
    }

    BattleState state(){return engine.state();}
    String encounterId(){return encounterId;}
    UUID initiatorId(){return initiatorId;}
    List<UUID> participantIds(){return participantIds;}
    int speed(){return speed;}
    boolean finished(){return finished;}
    Vec3 battleAnchor(){return battleAnchor;}
    float battleYaw(){return battleYaw;}
    Vec3 combatantPosition(String id){return presentation.home(id);}
    boolean auto(UUID id){return id!=null&&autoOwners.contains(id);}
    boolean autoAllowed(UUID id){return autoAllowed&&participantIds.contains(id);}
    boolean speedAllowed(UUID id){return speedAllowed&&initiatorId.equals(id);}
    boolean fleeAllowed(UUID id){return fleeAllowed&&initiatorId.equals(id);}
    boolean participant(UUID id){return id!=null&&participantIds.contains(id);}
    boolean settled(UUID id){return settledOwners.contains(id);}
    void markSettled(UUID id){if(participant(id))settledOwners.add(id);}
    void markReadyToExit(UUID id){if(participant(id))readyToExit.add(id);}
    boolean allReadyToExit(){return readyToExit.containsAll(participantIds);}
    String rewardTransactionId(UUID id){return rewardTransactionIds.getOrDefault(id,"");}
    BattleResultSummary resultSummary(UUID id){return resultSummaries.getOrDefault(id,BattleResultSummary.none());}

    float returnYaw(UUID id){ReturnState s=returnStates.get(id);return s==null?0F:s.yaw();}
    float returnPitch(UUID id){ReturnState s=returnStates.get(id);return s==null?0F:s.pitch();}
    UUID ownerOf(String actorId){return SharedBattleOwnership.ownerOf(engine.state(),actorOwners,actorId);}

    boolean canControlActor(UUID playerId,String actorId){
        if(playerId==null||actorId==null||actorId.isBlank())return false;
        CombatantState actor=engine.state().find(actorId);
        return actor!=null&&actor.side()==CombatantSide.ALLY&&playerId.equals(ownerOf(actorId));
    }

    void tick(ServerPlayer caller){
        if(caller==null||!participant(caller.getUUID())||caller.level()!=level)return;
        lock(caller);
        long gameTime=level.getGameTime();
        if(lastLogicTick==gameTime)return;
        lastLogicTick=gameTime;

        presentation.tick(level);
        syncPresentation();
        syncBarks();
        if(engine.state().outcome()!=BattleOutcome.RUNNING){tickOutcomePresentation();return;}
        if(delayTicks>0){delayTicks--;return;}

        CombatantState actor=engine.state().currentActorId()==null?engine.nextReady():engine.state().combatant(engine.state().currentActorId());
        if(!readyShown){
            readyShown=true;
            presentation.turnReady(level,actor.instanceId());
            if(actor.side()==CombatantSide.ALLY&&HeroBattleBarks.contains(actor.definition().id()))barkAll(actor.definition().id(),HeroBattleBarks.Event.TURN);
            delayTicks=readyPresentationDelay();
            syncAll();
            return;
        }
        UUID owner=actor.side()==CombatantSide.ALLY?ownerOf(actor.instanceId()):null;
        if(actor.side()==CombatantSide.ENEMY||ownerAutoControlled(owner)){
            presentation.clearFocus(level);
            int visualTicks=autoAct(actor);
            syncPresentation();
            syncBarks();
            readyShown=false;
            delayTicks=scaledPresentationDelay(visualTicks);
            syncAfterResolution();
        }
    }

    void action(ServerPlayer player,String actorId,String skillId,String targetId){
        UUID viewer=player==null?null:player.getUUID();
        if(player==null||finished||engine.state().outcome()!=BattleOutcome.RUNNING||auto(viewer))return;
        if(!actorId.equals(engine.state().currentActorId())||!canControlActor(viewer,actorId))return;
        CombatantState actor=engine.state().combatant(actorId);
        try{
            SkillDefinition skill=actor.definition().skill(skillId);
            presentation.clearFocus(level);
            int eventStart=engine.state().events().size();
            if(skill.targetRule()==TargetRule.SELF||skill.targetRule()==TargetRule.ALLY_ALL||skill.targetRule()==TargetRule.ENEMY_ALL)engine.useSkill(actorId,skillId);
            else engine.useSkill(actorId,skillId,targetId);
            barkSkillAll(actor.definition().id(),skill.id());
            int visualTicks=presentResolvedAction(actor,skill,targetId,eventStart);
            barkReactionEvents(eventStart);
            emitAudio(eventStart);
            syncPresentation();
            syncBarks();
            readyShown=false;
            delayTicks=scaledPresentationDelay(visualTicks);
            syncAfterResolution();
        }catch(RuntimeException ignored){BattleNetwork.sync(player,this);}
    }

    void focusTarget(ServerPlayer player,String targetId){
        if(player==null)return;
        UUID viewer=player.getUUID();
        String current=engine.state().currentActorId();
        if(current==null||!canControlActor(viewer,current)){
            PersonalPresentationIsolation.withPrivateActorOwner(viewer,()->presentation.clearFocus(level));
            return;
        }
        PersonalPresentationIsolation.withPrivateActorOwner(viewer,()->{
            if(targetId==null||targetId.isBlank()){presentation.clearFocus(level);return;}
            try{engine.state().combatant(targetId);presentation.focus(level,targetId);}
            catch(RuntimeException ignored){presentation.clearFocus(level);}
        });
    }

    void toggleAuto(ServerPlayer player){
        if(player==null||finished||engine.state().outcome()!=BattleOutcome.RUNNING||!autoAllowed(player.getUUID()))return;
        UUID owner=player.getUUID();
        PersonalPresentationIsolation.withPrivateActorOwner(owner,()->presentation.clearFocus(level));
        if(!autoOwners.remove(owner))autoOwners.add(owner);
        syncAll();
    }

    void toggleSpeed(ServerPlayer player){
        if(player==null||finished||engine.state().outcome()!=BattleOutcome.RUNNING||!speedAllowed(player.getUUID()))return;
        speed=speed==1?2:1;
        syncAll();
    }

    void disconnect(ServerPlayer player){if(player!=null&&participant(player.getUUID()))restorePlayer(player);}
    boolean resume(ServerPlayer player){
        if(player==null||!participant(player.getUUID())||player.level()!=level)return false;
        enterBattle(player,battleAnchor);
        BattleNetwork.sync(player,this);
        return true;
    }

    List<ServerPlayer> onlineParticipants(){
        var server=level.getServer();
        if(server==null)return List.of();
        List<ServerPlayer> out=new ArrayList<>();
        for(UUID id:participantIds){
            ServerPlayer p=server.getPlayerList().getPlayer(id);
            if(p!=null&&p.level()==level)out.add(p);
        }
        return List.copyOf(out);
    }
    boolean allParticipantsOnline(){return onlineParticipants().size()==participantIds.size();}
    ServerPlayer online(UUID id){
        var server=level.getServer();
        if(server==null||id==null)return null;
        ServerPlayer p=server.getPlayerList().getPlayer(id);
        return p!=null&&p.level()==level?p:null;
    }
    void cleanupAllOnline(){presentation.cleanup(level);for(ServerPlayer p:onlineParticipants())restorePlayer(p);}
    void syncAll(){for(ServerPlayer p:onlineParticipants())BattleNetwork.sync(p,this);}

    private boolean ownerAutoControlled(UUID owner){return owner==null||autoOwners.contains(owner)||online(owner)==null;}

    private int autoAct(CombatantState actor){
        int eventStart=engine.state().events().size();
        try{BattleAutoController.chooseAutoAction(engine,engine.state(),actor);}catch(RuntimeException ex){safeBasicFallback(actor);}
        int visualTicks=animateRecordedAction(actor,eventStart);
        barkReactionEvents(eventStart);
        emitAudio(eventStart);
        return visualTicks;
    }

    private void syncAfterResolution(){if(engine.state().outcome()==BattleOutcome.RUNNING){syncAll();return;}beginOutcomePresentation();}
    private void beginOutcomePresentation(){if(outcomePresentationTicks>=0||finished)return;barkOutcome();outcomePresentationTicks=outcomePresentationDelay();syncAll();}
    private void tickOutcomePresentation(){if(finished)return;if(outcomePresentationTicks<0){beginOutcomePresentation();return;}if(outcomePresentationTicks>0){outcomePresentationTicks--;return;}markFinished();syncAll();}
    private int outcomePresentationDelay(){boolean bossClear=engine.state().outcome()==BattleOutcome.ALLY_VICTORY&&engine.state().combatants().stream().anyMatch(u->u.definition().boss());return scaledPresentationDelay(bossClear?102:32);}
    private void markFinished(){if(finished||engine.state().outcome()==BattleOutcome.RUNNING)return;finished=true;if(engine.state().outcome()==BattleOutcome.ALLY_VICTORY&&!encounterId.isBlank())for(UUID id:participantIds)resultSummaries.put(id,CampaignProgressStore.previewVictory(id,encounterId));}

    private void syncPresentation(){
        presentation.spawnMissing(level,presentationCenter,battleYaw,engine.state().combatants());
        presentation.syncStates(level,engine.state().combatants());
        presentation.syncRelations(level,engine.state(),this::ownerOf);
        presentation.syncDanger(level,engine.state().combatants());
        presentation.finish(level,engine.state().outcome());
    }

    private int animateRecordedAction(CombatantState actor,int eventStart){
        List<BattleEvent> events=engine.state().events();
        for(int i=events.size()-1;i>=eventStart;i--){
            BattleEvent event=events.get(i);
            if(!"ACTION".equals(event.type())||!actor.instanceId().equals(event.sourceId()))continue;
            SkillDefinition skill=actor.definition().skill(event.detail());
            String targetId=event.targetId();
            if(targetId!=null&&!targetId.isBlank()){int comma=targetId.indexOf(',');if(comma>=0)targetId=targetId.substring(0,comma);}
            if(actor.side()==CombatantSide.ALLY)barkSkillAll(actor.definition().id(),skill.id());
            return presentResolvedAction(actor,skill,targetId,eventStart);
        }
        return 8;
    }

    private int presentResolvedAction(CombatantState actor,SkillDefinition skill,String targetId,int eventStart){
        UUID presentationOwner=presentationOwner(actor);
        scoped(presentationOwner,()->{
            boolean damages=skill.effects().stream().anyMatch(e->e.type()==EffectType.DAMAGE);
            presentation.performSkill(level,actor.instanceId(),actor.definition().id(),skill.id(),targetId,damages);
            presentation.presentEvents(level,engine.state(),eventStart);
        });
        return BattlePresentation.actionPresentationTicks(actor.definition().id(),skill.id());
    }

    private UUID presentationOwner(CombatantState actor){
        UUID owner=actor.side()==CombatantSide.ALLY?ownerOf(actor.instanceId()):initiatorId;
        return owner!=null&&online(owner)!=null?owner:initiatorId;
    }
    private static void scoped(UUID owner,Runnable action){if(owner==null)action.run();else PersonalPresentationIsolation.withPrivateActorOwner(owner,action);}
    private void emitAudio(int eventStart){for(ServerPlayer p:onlineParticipants())BattleAudioEmitter.emit(p,engine.state(),eventStart);}

    private void syncBarks(){
        boolean newDeath=false;String deadAlly="";
        for(CombatantState unit:engine.state().combatants()){
            boolean before=barkDowned.getOrDefault(unit.instanceId(),unit.downed()),now=unit.downed();
            barkDowned.put(unit.instanceId(),now);
            if(!before&&now){newDeath=true;if(unit.side()==CombatantSide.ALLY)deadAlly=unit.instanceId();if("P07_SUMMON".equals(unit.definition().id()))barkAll("P07",HeroBattleBarks.Event.SPECIAL,"TOTO_DEATH");}
            if(before&&!now&&unit.side()==CombatantSide.ALLY&&HeroBattleBarks.contains(unit.definition().id()))barkAll(unit.definition().id(),HeroBattleBarks.Event.REVIVE);
            if(unit.side()!=CombatantSide.ALLY||unit.downed()||!HeroBattleBarks.contains(unit.definition().id()))continue;
            double ratio=unit.hp()/(double)Math.max(1,unit.maxHp());
            if(ratio<=.30&&low30Barked.add(unit.instanceId()))barkAll(unit.definition().id(),HeroBattleBarks.Event.LOW_30);
            else if("P08".equals(unit.definition().id())&&ratio<=.50&&low50Barked.add(unit.instanceId()))barkAll("P08",HeroBattleBarks.Event.LOW_50);
        }
        if(newDeath){livingHero("P06").ifPresent(h->barkAll("P06",HeroBattleBarks.Event.SPECIAL,"MEMORY"));if(!deadAlly.isBlank()){String excluded=deadAlly;firstLivingHeroExcept(excluded).ifPresent(h->barkAll(h.definition().id(),HeroBattleBarks.Event.ALLY_DEATH));}}
    }

    private void barkSkillAll(String heroId,String skillId){
        if(!HeroBattleBarks.contains(heroId))return;
        if(skillId.equals("p01_breaker_strike")||skillId.equals("p02_time_leap")||skillId.equals("p03_guard_transfer")||skillId.equals("p04_returned_breath")||skillId.equals("p07_summon_toto"))barkAll(heroId,HeroBattleBarks.Event.SPECIAL,skillId);
    }
    private void barkReactionEvents(int eventStart){
        List<BattleEvent> events=engine.state().events();
        for(int i=Math.max(0,eventStart);i<events.size();i++){
            BattleEvent event=events.get(i);if(!"REACTION_DAMAGE".equals(event.type()))continue;
            CombatantState source=engine.state().find(event.sourceId());
            if(source!=null&&source.side()==CombatantSide.ALLY&&"P05".equals(source.definition().id())){barkAll("P05",HeroBattleBarks.Event.SPECIAL,"REACTION");return;}
        }
    }
    private void barkOutcome(){if(outcomeBarked||engine.state().outcome()!=BattleOutcome.ALLY_VICTORY)return;outcomeBarked=true;firstLivingHero().ifPresent(h->barkAll(h.definition().id(),HeroBattleBarks.Event.VICTORY));}
    private void barkAll(String heroId,HeroBattleBarks.Event event){for(ServerPlayer p:onlineParticipants())HeroBattleBarks.say(p,heroId,event);}
    private void barkAll(String heroId,HeroBattleBarks.Event event,String detail){for(ServerPlayer p:onlineParticipants())HeroBattleBarks.say(p,heroId,event,detail);}
    private java.util.Optional<CombatantState> firstLivingHero(){return engine.state().living(CombatantSide.ALLY).stream().filter(u->HeroBattleBarks.contains(u.definition().id())).findFirst();}
    private java.util.Optional<CombatantState> firstLivingHeroExcept(String id){return engine.state().living(CombatantSide.ALLY).stream().filter(u->!u.instanceId().equals(id)&&HeroBattleBarks.contains(u.definition().id())).findFirst();}
    private java.util.Optional<CombatantState> livingHero(String heroId){return engine.state().living(CombatantSide.ALLY).stream().filter(u->heroId.equals(u.definition().id())).findFirst();}

    private void safeBasicFallback(CombatantState actor){
        SkillDefinition basic=actor.definition().skill(actor.definition().basicSkillId());
        switch(basic.targetRule()){
            case SELF,ALLY_ALL,ENEMY_ALL->engine.useSkill(actor.instanceId(),basic.id());
            case ENEMY_SINGLE->{List<CombatantState> targets=engine.state().living(actor.side().opposite());if(!targets.isEmpty())engine.useSkill(actor.instanceId(),basic.id(),targets.getFirst().instanceId());}
            case ALLY_SINGLE->{CombatantState target=engine.state().living(actor.side()).stream().min(Comparator.comparingDouble(u->u.hp()/(double)u.maxHp())).orElse(actor);engine.useSkill(actor.instanceId(),basic.id(),target.instanceId());}
            case DEAD_ALLY_SINGLE->{List<CombatantState> targets=engine.state().downed(actor.side());if(!targets.isEmpty())engine.useSkill(actor.instanceId(),basic.id(),targets.getFirst().instanceId());}
        }
    }

    private void enterBattle(ServerPlayer p,Vec3 anchor){p.setInvisible(true);p.setPos(anchor.x,anchor.y,anchor.z);p.setYRot(battleYaw);p.setXRot(18F);p.setDeltaMovement(Vec3.ZERO);}
    private void restorePlayer(ServerPlayer p){ReturnState s=returnStates.get(p.getUUID());if(s==null)return;p.setInvisible(s.invisible());p.setPos(s.position().x,s.position().y,s.position().z);p.setYRot(s.yaw());p.setXRot(s.pitch());p.setDeltaMovement(Vec3.ZERO);}
    private void lock(ServerPlayer p){p.setInvisible(true);if(p.position().distanceToSqr(battleAnchor)>.0025)p.setPos(battleAnchor.x,battleAnchor.y,battleAnchor.z);p.setDeltaMovement(Vec3.ZERO);}
    private int readyPresentationDelay(){return speed==2?4:8;}
    private int scaledPresentationDelay(int oneX){return speed==2?Math.max(1,(oneX+1)/2):Math.max(1,oneX);}
}
