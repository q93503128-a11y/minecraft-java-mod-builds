package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEngine;
import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.EffectType;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.TargetRule;
import io.github.q93503128.turnbound.presentation.HeroBattleBarks;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import io.github.q93503128.turnbound.world.EndgameProgressService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BattleSession {
    private final BattleEngine engine;
    private final String encounterId;
    private final UUID ownerId;
    private final String rewardTransactionId = UUID.randomUUID().toString();
    private final Vec3 returnPosition;
    private final float returnYaw;
    private final float returnPitch;
    private final boolean playerWasInvisible;
    private final Vec3 presentationCenter;
    private final Vec3 battleAnchor;
    private final float battleYaw;
    private final boolean autoAllowed;
    private final boolean speedAllowed;
    private final boolean fleeAllowed;
    private final BattlePresentation presentation = new BattlePresentation();
    private final Map<String, Boolean> barkDowned = new HashMap<>();
    private final Set<String> low50Barked = new HashSet<>();
    private final Set<String> low30Barked = new HashSet<>();
    private boolean auto;
    private int speed = 1;
    private int delayTicks = 8;
    /** -1 while combat is live; counts the authored world outro before result UI is allowed to replace it. */
    private int outcomePresentationTicks = -1;
    private boolean finished;
    private boolean readyShown;
    private boolean outcomeBarked;
    private BattleResultSummary resultSummary = BattleResultSummary.none();

    BattleSession(ServerPlayer player) { this(player,"",true,true,true,BattleArenaLocator.locate(player)); }
    BattleSession(ServerPlayer player,String encounterId,boolean autoAllowed,boolean speedAllowed,boolean fleeAllowed) { this(player,encounterId,autoAllowed,speedAllowed,fleeAllowed,BattleArenaLocator.locate(player)); }

    BattleSession(ServerPlayer player,String encounterId,boolean autoAllowed,boolean speedAllowed,boolean fleeAllowed,BattleArenaLocator.Arena arena) {
        this.encounterId=encounterId==null?"":encounterId;ownerId=player.getUUID();this.autoAllowed=autoAllowed;this.speedAllowed=speedAllowed;this.fleeAllowed=fleeAllowed;
        BattleState initial;
        if(CampaignEncounterCatalog.contains(this.encounterId))initial=CampaignEncounterCatalog.createBattle(ownerId,this.encounterId);
        else if(EndgameEncounterCatalog.contains(this.encounterId))initial=EndgameEncounterCatalog.createBattle(ownerId,this.encounterId);
        else initial=P0Scenario.create();
        engine=new BattleEngine(initial);returnPosition=player.position();returnYaw=player.getYRot();returnPitch=player.getXRot();playerWasInvisible=player.isInvisible();
        presentationCenter=arena.center();battleYaw=arena.facingYaw();player.setInvisible(true);
        presentation.spawn((ServerLevel)player.level(),presentationCenter,battleYaw,engine.state().combatants());
        for(CombatantState unit:engine.state().combatants())barkDowned.put(unit.instanceId(),unit.downed());
        firstLivingHero().ifPresent(hero->HeroBattleBarks.say(player,hero.definition().id(),HeroBattleBarks.Event.START));
        Vec3 actualCenter=presentation.center();battleAnchor=actualCenter.lengthSqr()<.001?presentationCenter:actualCenter;
        player.setPos(battleAnchor.x,battleAnchor.y,battleAnchor.z);player.setYRot(battleYaw);player.setXRot(18F);player.setDeltaMovement(Vec3.ZERO);
    }

    public BattleState state(){return engine.state();} public boolean auto(){return auto;} public int speed(){return speed;} public boolean finished(){return finished;}
    public boolean autoAllowed(){return autoAllowed;} public boolean speedAllowed(){return speedAllowed;} public boolean fleeAllowed(){return fleeAllowed;}
    public BattleResultSummary resultSummary(){return resultSummary;} String encounterId(){return encounterId;} String rewardTransactionId(){return rewardTransactionId;}
    Vec3 battleAnchor(){return battleAnchor;} float battleYaw(){return battleYaw;} Vec3 combatantPosition(String id){return presentation.home(id);}

    void tick(ServerPlayer player){
        ServerLevel level=(ServerLevel)player.level();presentation.tick(level);syncPresentation(level);syncBarks(player);lock(player);
        if(engine.state().outcome()!=BattleOutcome.RUNNING){tickOutcomePresentation(player);return;}
        if(delayTicks>0){delayTicks--;return;}
        CombatantState actor=engine.state().currentActorId()==null?engine.nextReady():engine.state().combatant(engine.state().currentActorId());
        if(!readyShown){readyShown=true;presentation.turnReady(level,actor.instanceId());if(actor.side()==CombatantSide.ALLY&&HeroBattleBarks.contains(actor.definition().id()))HeroBattleBarks.say(player,actor.definition().id(),HeroBattleBarks.Event.TURN);delayTicks=presentationDelay();BattleNetwork.sync(player,this);return;}
        if(actor.side()==CombatantSide.ENEMY||auto){presentation.clearFocus(level);autoAct(player,level,actor);syncPresentation(level);syncBarks(player);readyShown=false;delayTicks=presentationDelay();syncAfterResolution(player);}
    }

    void action(ServerPlayer player,String actorId,String skillId,String targetId){
        if(finished||auto||engine.state().outcome()!=BattleOutcome.RUNNING)return;if(!actorId.equals(engine.state().currentActorId()))return;CombatantState actor=engine.state().combatant(actorId);if(actor.side()!=CombatantSide.ALLY)return;
        ServerLevel level=(ServerLevel)player.level();try{SkillDefinition skill=actor.definition().skill(skillId);presentation.clearFocus(level);int eventStart=engine.state().events().size();
            if(skill.targetRule()==TargetRule.SELF||skill.targetRule()==TargetRule.ALLY_ALL||skill.targetRule()==TargetRule.ENEMY_ALL)engine.useSkill(actorId,skillId);else engine.useSkill(actorId,skillId,targetId);
            barkSkill(player,actor.definition().id(),skill.id());animateSkill(level,actor,skill,targetId);presentation.presentEvents(level,engine.state(),eventStart);barkReactionEvents(player,eventStart);BattleAudioEmitter.emit(player,engine.state(),eventStart);syncPresentation(level);syncBarks(player);readyShown=false;delayTicks=presentationDelay();syncAfterResolution(player);
        }catch(RuntimeException ignored){BattleNetwork.sync(player,this);}
    }

    void focusTarget(ServerPlayer player,String targetId){ServerLevel level=(ServerLevel)player.level();if(targetId==null||targetId.isBlank()){presentation.clearFocus(level);return;}try{engine.state().combatant(targetId);presentation.focus(level,targetId);}catch(RuntimeException ignored){presentation.clearFocus(level);}}
    void toggleAuto(ServerPlayer player){if(finished||engine.state().outcome()!=BattleOutcome.RUNNING||!autoAllowed)return;presentation.clearFocus((ServerLevel)player.level());auto=!auto;BattleNetwork.sync(player,this);}
    void toggleSpeed(ServerPlayer player){if(finished||engine.state().outcome()!=BattleOutcome.RUNNING||!speedAllowed)return;speed=speed==1?2:1;BattleNetwork.sync(player,this);}
    void cleanup(ServerPlayer player){presentation.cleanup((ServerLevel)player.level());player.setInvisible(playerWasInvisible);player.setPos(returnPosition.x,returnPosition.y,returnPosition.z);player.setYRot(returnYaw);player.setXRot(returnPitch);player.setDeltaMovement(Vec3.ZERO);}

    private void autoAct(ServerPlayer player,ServerLevel level,CombatantState actor){int eventStart=engine.state().events().size();try{P0Scenario.chooseAutoAction(engine,engine.state(),actor);}catch(RuntimeException ex){safeBasicFallback(actor);}animateRecordedAction(player,level,actor,eventStart);presentation.presentEvents(level,engine.state(),eventStart);barkReactionEvents(player,eventStart);BattleAudioEmitter.emit(player,engine.state(),eventStart);}

    /** Sends the resolved HP/down state immediately, but keeps the client on the 3D battlefield until the outro finishes. */
    private void syncAfterResolution(ServerPlayer player){
        if(engine.state().outcome()==BattleOutcome.RUNNING){BattleNetwork.sync(player,this);return;}
        beginOutcomePresentation(player);
    }

    private void beginOutcomePresentation(ServerPlayer player){
        if(outcomePresentationTicks>=0||finished)return;
        barkOutcome(player);
        outcomePresentationTicks=outcomePresentationDelay();
        BattleNetwork.sync(player,this);
    }

    private void tickOutcomePresentation(ServerPlayer player){
        if(finished)return;
        if(outcomePresentationTicks<0){beginOutcomePresentation(player);return;}
        if(outcomePresentationTicks>0){outcomePresentationTicks--;return;}
        markFinished();
        BattleNetwork.sync(player,this);
    }

    /** Boss victory waits for the longest authored 3.8s collapse; defeat/non-boss clears keep a shorter readable outro. */
    private int outcomePresentationDelay(){
        boolean bossClear=engine.state().outcome()==BattleOutcome.ALLY_VICTORY&&engine.state().combatants().stream().anyMatch(unit->unit.definition().boss());
        return bossClear?80:32;
    }

    private void markFinished(){if(finished||engine.state().outcome()==BattleOutcome.RUNNING)return;finished=true;if(engine.state().outcome()==BattleOutcome.ALLY_VICTORY&&!encounterId.isBlank()){if(EndgameEncounterCatalog.contains(encounterId))resultSummary=EndgameProgressService.previewVictory(ownerId,encounterId);else resultSummary=CampaignProgressStore.previewVictory(ownerId,encounterId);}}

    private void syncPresentation(ServerLevel level){presentation.spawnMissing(level,presentationCenter,battleYaw,engine.state().combatants());presentation.syncStates(level,engine.state().combatants());presentation.syncDanger(level,engine.state().combatants());presentation.finish(level,engine.state().outcome());}

    private void animateRecordedAction(ServerPlayer player,ServerLevel level,CombatantState actor,int eventStart){List<BattleEvent> events=engine.state().events();for(int index=events.size()-1;index>=eventStart;index--){BattleEvent event=events.get(index);if(!"ACTION".equals(event.type())||!actor.instanceId().equals(event.sourceId()))continue;SkillDefinition skill=actor.definition().skill(event.detail());String targetId=event.targetId();if(targetId!=null&&!targetId.isBlank()){int comma=targetId.indexOf(',');if(comma>=0)targetId=targetId.substring(0,comma);}if(actor.side()==CombatantSide.ALLY)barkSkill(player,actor.definition().id(),skill.id());animateSkill(level,actor,skill,targetId);return;}}
    private void animateSkill(ServerLevel level,CombatantState actor,SkillDefinition skill,String targetId){boolean damages=skill.effects().stream().anyMatch(effect->effect.type()==EffectType.DAMAGE);presentation.performSkill(level,actor.instanceId(),actor.definition().id(),skill.id(),targetId,damages);}

    private void syncBarks(ServerPlayer player){
        boolean newDeath=false;String deadAlly="";
        for(CombatantState unit:engine.state().combatants()){
            boolean before=barkDowned.getOrDefault(unit.instanceId(),unit.downed());boolean now=unit.downed();barkDowned.put(unit.instanceId(),now);
            if(!before&&now){newDeath=true;if(unit.side()==CombatantSide.ALLY)deadAlly=unit.instanceId();if("P07_SUMMON".equals(unit.definition().id()))HeroBattleBarks.say(player,"P07",HeroBattleBarks.Event.SPECIAL,"TOTO_DEATH");}
            if(before&&!now&&unit.side()==CombatantSide.ALLY&&HeroBattleBarks.contains(unit.definition().id()))HeroBattleBarks.say(player,unit.definition().id(),HeroBattleBarks.Event.REVIVE);
            if(unit.side()!=CombatantSide.ALLY||unit.downed()||!HeroBattleBarks.contains(unit.definition().id()))continue;
            double ratio=unit.hp()/(double)Math.max(1,unit.maxHp());
            if(ratio<=.30&&low30Barked.add(unit.instanceId()))HeroBattleBarks.say(player,unit.definition().id(),HeroBattleBarks.Event.LOW_30);
            else if("P08".equals(unit.definition().id())&&ratio<=.50&&low50Barked.add(unit.instanceId()))HeroBattleBarks.say(player,"P08",HeroBattleBarks.Event.LOW_50);
        }
        if(newDeath){livingHero("P06").ifPresent(hero->HeroBattleBarks.say(player,"P06",HeroBattleBarks.Event.SPECIAL,"MEMORY"));if(!deadAlly.isBlank())firstLivingHeroExcept(deadAlly).ifPresent(hero->HeroBattleBarks.say(player,hero.definition().id(),HeroBattleBarks.Event.ALLY_DEATH));}
    }

    private void barkSkill(ServerPlayer player,String heroId,String skillId){
        if(!HeroBattleBarks.contains(heroId))return;
        if(skillId.equals("p01_breaker_strike")||skillId.equals("p02_time_leap")||skillId.equals("p03_guard_transfer")||skillId.equals("p04_returned_breath")||skillId.equals("p07_summon_toto"))HeroBattleBarks.say(player,heroId,HeroBattleBarks.Event.SPECIAL,skillId);
    }

    private void barkReactionEvents(ServerPlayer player,int eventStart){
        List<BattleEvent> events=engine.state().events();
        for(int i=Math.max(0,eventStart);i<events.size();i++){
            BattleEvent event=events.get(i);if(!"REACTION_DAMAGE".equals(event.type()))continue;
            CombatantState source=engine.state().find(event.sourceId());
            if(source!=null&&source.side()==CombatantSide.ALLY&&"P05".equals(source.definition().id())){
                HeroBattleBarks.say(player,"P05",HeroBattleBarks.Event.SPECIAL,"REACTION");return;
            }
        }
    }

    private void barkOutcome(ServerPlayer player){if(outcomeBarked||engine.state().outcome()!=BattleOutcome.ALLY_VICTORY)return;outcomeBarked=true;firstLivingHero().ifPresent(hero->HeroBattleBarks.say(player,hero.definition().id(),HeroBattleBarks.Event.VICTORY));}
    private java.util.Optional<CombatantState> firstLivingHero(){return engine.state().living(CombatantSide.ALLY).stream().filter(u->HeroBattleBarks.contains(u.definition().id())).findFirst();}
    private java.util.Optional<CombatantState> firstLivingHeroExcept(String instanceId){return engine.state().living(CombatantSide.ALLY).stream().filter(u->!u.instanceId().equals(instanceId)&&HeroBattleBarks.contains(u.definition().id())).findFirst();}
    private java.util.Optional<CombatantState> livingHero(String heroId){return engine.state().living(CombatantSide.ALLY).stream().filter(u->heroId.equals(u.definition().id())).findFirst();}

    private void safeBasicFallback(CombatantState actor){SkillDefinition basic=actor.definition().skill(actor.definition().basicSkillId());switch(basic.targetRule()){case SELF,ALLY_ALL,ENEMY_ALL->engine.useSkill(actor.instanceId(),basic.id());case ENEMY_SINGLE->{List<CombatantState> targets=engine.state().living(actor.side().opposite());if(!targets.isEmpty())engine.useSkill(actor.instanceId(),basic.id(),targets.getFirst().instanceId());}case ALLY_SINGLE->{CombatantState target=engine.state().living(actor.side()).stream().min(Comparator.comparingDouble(unit->unit.hp()/(double)unit.maxHp())).orElse(actor);engine.useSkill(actor.instanceId(),basic.id(),target.instanceId());}case DEAD_ALLY_SINGLE->{List<CombatantState> targets=engine.state().downed(actor.side());if(!targets.isEmpty())engine.useSkill(actor.instanceId(),basic.id(),targets.getFirst().instanceId());}}}
    private void lock(ServerPlayer player){if(player.position().distanceToSqr(battleAnchor)>.0025)player.setPos(battleAnchor.x,battleAnchor.y,battleAnchor.z);player.setDeltaMovement(Vec3.ZERO);}
    private int presentationDelay(){return speed==2?4:8;}
}
