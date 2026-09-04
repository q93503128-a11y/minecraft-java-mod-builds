package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.BattleVfx;
import io.github.q93503128.turnbound.presentation.BossBattleVfx;
import io.github.q93503128.turnbound.presentation.EnemyBattleTelegraphs;
import io.github.q93503128.turnbound.presentation.EnemyPresentationProfile;
import io.github.q93503128.turnbound.presentation.SignatureBattleActors;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class BattlePresentation {
    private static final double[][] ALLY_FORMATION = {{-3.0,4.0},{-1.0,4.0},{1.0,4.0},{3.0,4.0}};
    private static final double[][] ENEMY_FORMATION = {{-4.0,-4.0},{-2.0,-4.0},{0.0,-4.0},{2.0,-4.0},{4.0,-4.0}};

    private final Map<String, UUID> actors = new LinkedHashMap<>();
    private final Map<String, Vec3> homes = new LinkedHashMap<>();
    private final Map<String, Float> homeYaws = new LinkedHashMap<>();
    private final Map<String, CombatantSide> sides = new LinkedHashMap<>();
    private final Map<String, Boolean> summons = new LinkedHashMap<>();
    private final Map<String, String> visualIds = new LinkedHashMap<>();
    private final Map<String, Boolean> downed = new LinkedHashMap<>();
    private final Map<String, Integer> barriers = new LinkedHashMap<>();
    private final Map<String, Integer> bossPhases = new LinkedHashMap<>();
    private final Map<String, Integer> pendingRemovalTicks = new LinkedHashMap<>();
    /** Multiple actors may still be returning at 2x speed; never strand the previous attacker. */
    private final Map<String, Integer> returnTimers = new LinkedHashMap<>();
    private UUID focusMarker;
    private UUID dangerMarker;
    private String dangerTarget = "";
    private boolean finishPlayed;

    void spawn(ServerLevel level, Vec3 center, float facingYaw, Iterable<CombatantState> combatants) {
        cleanupActors(level); finishPlayed = false; spawnMissing(level, center, facingYaw, combatants);
    }

    void spawnMissing(ServerLevel level, Vec3 center, float facingYaw, Iterable<CombatantState> combatants) {
        List<CombatantState> units = new ArrayList<>(); combatants.forEach(units::add); removeMissing(level, units);
        Vec3 forward = BattleArenaLocator.forward(facingYaw); Vec3 right = new Vec3(-forward.z,0.0,forward.x);
        int allyIndex=0, enemyIndex=0;
        for (CombatantState combatant : units) {
            if (combatant.definition().summon()) continue;
            boolean ally = combatant.side()==CombatantSide.ALLY; int index=ally?allyIndex++:enemyIndex++;
            if (actors.containsKey(combatant.instanceId())) continue;
            double[][] formation=ally?ALLY_FORMATION:ENEMY_FORMATION; if(index>=formation.length) continue;
            Vec3 raw=localToWorld(center,right,forward,formation[index][0],formation[index][1]);
            spawnActor(level,combatant,BattleArenaLocator.groundPosition(level,raw),facingYaw,combatant.definition().rules());
        }
        for (CombatantState combatant : units) {
            if (!combatant.definition().summon() || actors.containsKey(combatant.instanceId())) continue;
            String ownerId=combatant.ref("ownerId");
            Vec3 ownerHome=homes.get(ownerId); if(ownerHome==null) ownerHome=center;
            Iterable<String> visualRules=combatant.definition().rules();
            if(ownerId!=null){
                for(CombatantState unit:units){if(ownerId.equals(unit.instanceId())){visualRules=unit.definition().rules();break;}}
            }
            Vec3 raw=ownerHome.add(right.scale(.8)).subtract(forward.scale(1.0));
            spawnActor(level,combatant,BattleArenaLocator.groundPosition(level,raw),facingYaw,visualRules);
        }
    }

    private void spawnActor(ServerLevel level, CombatantState combatant, Vec3 pos, float facingYaw, Iterable<String> visualRules) {
        boolean ally=combatant.side()==CombatantSide.ALLY; float yaw=facingYaw+(ally?0F:180F); Entity actor=null;
        String baseVisualId=combatant.definition().id();
        String visualId=SignatureBattleActors.visualId(baseVisualId,visualRules);
        if(SignatureBattleActors.contains(visualId)){
            BattleActorEntity animated=SignatureBattleActors.spawn(level,visualId,pos,yaw);
            if(animated!=null){animated.setCustomName(Component.literal(combatant.definition().name()));animated.setCustomNameVisible(false);actor=animated;}
        } else if (TurnboundBattleActors.contains(baseVisualId)) {
            BattleActorEntity animated=TurnboundBattleActors.spawn(level,baseVisualId,pos,yaw);
            if(animated!=null){animated.setCustomName(Component.literal(combatant.definition().name()));animated.setCustomNameVisible(false);actor=animated;}
            visualId=baseVisualId;
        }
        if(actor==null){
            ArmorStand stand=new ArmorStand(level,pos.x,pos.y,pos.z);stand.setCustomName(Component.literal(combatant.definition().name()));
            stand.setCustomNameVisible(false);stand.setInvulnerable(true);stand.setNoGravity(true);stand.setShowArms(true);stand.setYRot(yaw);
            equipStandIn(stand,combatant);level.addFreshEntity(stand);actor=stand;visualId=baseVisualId;
        }
        actors.put(combatant.instanceId(),actor.getUUID()); homes.put(combatant.instanceId(),pos); homeYaws.put(combatant.instanceId(),yaw);
        sides.put(combatant.instanceId(),combatant.side()); summons.put(combatant.instanceId(),combatant.definition().summon());
        visualIds.put(combatant.instanceId(),visualId); downed.put(combatant.instanceId(),combatant.downed()); barriers.put(combatant.instanceId(),combatant.barrier());
        bossPhases.put(combatant.instanceId(),phaseFor(combatant)); pendingRemovalTicks.remove(combatant.instanceId()); returnTimers.remove(combatant.instanceId());
    }

    private void removeMissing(ServerLevel level,List<CombatantState> units){
        Set<String> liveIds=new HashSet<>();for(CombatantState unit:units)liveIds.add(unit.instanceId());
        for(String id:List.copyOf(actors.keySet())){if(liveIds.contains(id)||pendingRemovalTicks.containsKey(id))continue;removeActor(level,id);}
    }

    private void removeActor(ServerLevel level,String id){
        UUID uuid=actors.remove(id);Entity entity=uuid==null?null:level.getEntity(uuid);if(entity!=null)entity.discard();
        homes.remove(id);homeYaws.remove(id);sides.remove(id);summons.remove(id);visualIds.remove(id);downed.remove(id);barriers.remove(id);bossPhases.remove(id);pendingRemovalTicks.remove(id);returnTimers.remove(id);
    }

    private static Vec3 localToWorld(Vec3 center,Vec3 right,Vec3 forward,double x,double z){return center.add(right.scale(x)).subtract(forward.scale(z));}
    Vec3 home(String combatantId){return homes.get(combatantId);}
    Vec3 center(){Vec3 ally=centroid(CombatantSide.ALLY),enemy=centroid(CombatantSide.ENEMY);if(ally!=null&&enemy!=null)return ally.add(enemy).scale(.5);if(ally!=null)return ally;if(enemy!=null)return enemy;return Vec3.ZERO;}
    private Vec3 centroid(CombatantSide side){double x=0,y=0,z=0;int count=0;for(var e:homes.entrySet()){if(sides.get(e.getKey())!=side||Boolean.TRUE.equals(summons.get(e.getKey())))continue;Vec3 h=e.getValue();x+=h.x;y+=h.y;z+=h.z;count++;}return count==0?null:new Vec3(x/count,y/count,z/count);}

    void focus(ServerLevel level,String targetId){clearFocus(level);Vec3 target=homes.get(targetId);if(target==null)return;ChatFormatting color=sides.get(targetId)==CombatantSide.ALLY?ChatFormatting.AQUA:ChatFormatting.RED;ArmorStand marker=marker(level,target.add(0,1,0),"▼",color);focusMarker=marker.getUUID();}

    void syncDanger(ServerLevel level,Iterable<CombatantState> combatants){
        String targetId="",warningVisualId="";for(CombatantState unit:combatants){if(!unit.downed()&&(unit.hasStatus("e003_armed")||unit.hasStatus("b01_charge_warning")||unit.hasStatus("b04_eruption_warning")||unit.hasStatus("b05_collapse_warning"))){targetId=unit.instanceId();warningVisualId=unit.definition().id();break;}}
        if(targetId.equals(dangerTarget))return;clearDanger(level);dangerTarget=targetId;if(targetId.isBlank())return;Vec3 target=homes.get(targetId);if(target==null)return;
        ArmorStand marker=marker(level,target.add(0,1.35,0),"!",ChatFormatting.GOLD);dangerMarker=marker.getUUID();Entity warningActor=entity(level,targetId);if(warningActor instanceof BattleActorEntity a)a.playTelegraph();BattleVfx.warning(level,warningVisualId,target);
    }

    void syncStates(ServerLevel level,Iterable<CombatantState> combatants){
        for(CombatantState unit:combatants){
            String id=unit.instanceId();Boolean before=downed.get(id);
            if(before==null)downed.put(id,unit.downed());else if(before!=unit.downed()){
                downed.put(id,unit.downed());Entity entity=entity(level,id);Vec3 home=homes.get(id);
                if(unit.downed()){if(entity instanceof BattleActorEntity a)a.playDeath();if(home!=null)BattleVfx.down(level,home);}else{if(entity instanceof BattleActorEntity a)a.playRevive();if(home!=null)BattleVfx.revive(level,home);}
            }

            int oldBarrier=barriers.getOrDefault(id,unit.barrier()),newBarrier=unit.barrier();
            if(!unit.downed()&&"B03".equals(unit.definition().id())&&oldBarrier>0&&newBarrier==0){
                Entity entity=entity(level,id);if(entity instanceof BattleActorEntity a)a.playBossStagger();Vec3 home=homes.get(id);if(home!=null)BossBattleVfx.oroBarrierBreak(level,home);
            }
            barriers.put(id,newBarrier);

            int phase=phaseFor(unit),previous=bossPhases.getOrDefault(id,phase);
            if(!unit.downed()&&phase>previous){
                bossPhases.put(id,phase);Entity entity=entity(level,id);if(entity instanceof BattleActorEntity a)playBossPhaseAnimation(a,unit.definition().id(),phase);
                Vec3 home=homes.get(id);if(home!=null){BattleVfx.phase(level,unit.definition().id(),home,phase);BossBattleVfx.phaseAccent(level,unit.definition().id(),home,phase);}
            }else bossPhases.putIfAbsent(id,phase);
        }
    }

    private static void playBossPhaseAnimation(BattleActorEntity actor,String visualId,int phase){
        if(phase==2){
            switch(visualId){
                case "B01","B02","B03","B04","B05" -> actor.playSummon();
                default -> actor.playPhase();
            }
            return;
        }
        actor.playPhase();
    }

    /** Replays authoritative combat events as authored hit/reaction/status clips without changing results. */
    void presentEvents(ServerLevel level, BattleState state, int eventStart) {
        List<BattleEvent> events=state.events();
        if(eventStart<0||eventStart>=events.size())return;
        Set<String> buffPlayed=new HashSet<>(),debuffPlayed=new HashSet<>();
        for(int i=eventStart;i<events.size();i++){
            BattleEvent event=events.get(i);
            switch(event.type()){
                case "REACTION_DAMAGE" -> {
                    Entity source=entity(level,event.sourceId());if(source instanceof BattleActorEntity a)a.playReaction();
                    playHitFor(level,state,event.targetId(),event.value());
                }
                case "DAMAGE","DOT" -> playHitFor(level,state,event.targetId(),event.value());
                case "DAMAGE_REDIRECT" -> {
                    Entity guardian=entity(level,event.targetId());if(guardian instanceof BattleActorEntity a)a.playReaction();
                    playHitFor(level,state,event.targetId(),event.value());
                }
                case "HEAL","BARRIER","STATUS_CLEAR" -> playBuffFor(level,state,event.targetId(),buffPlayed);
                case "STATUS" -> playStatusFor(level,state,event,buffPlayed,debuffPlayed);
                case "GAUGE" -> {
                    if(event.value()>0)playBuffFor(level,state,event.targetId(),buffPlayed);
                    else if(event.value()<0)playDebuffFor(level,state,event.targetId(),debuffPlayed);
                }
                case "SUMMON_DOWN" -> presentSummonDown(level,state,event.sourceId());
                default -> { }
            }
        }
    }

    private void playStatusFor(ServerLevel level,BattleState state,BattleEvent event,Set<String> buffPlayed,Set<String> debuffPlayed){
        CombatantState source=state.find(event.sourceId()),target=state.find(event.targetId());
        if(!isCoreHero(target))return;
        if(source!=null&&source.side()==target.side())playBuffFor(level,state,event.targetId(),buffPlayed);
        else playDebuffFor(level,state,event.targetId(),debuffPlayed);
    }

    private void playBuffFor(ServerLevel level,BattleState state,String targetId,Set<String> played){
        CombatantState target=state.find(targetId);if(!isCoreHero(target)||target.downed()||!played.add(targetId))return;
        Entity targetActor=entity(level,targetId);if(targetActor instanceof BattleActorEntity animated)animated.playBuff();
    }

    private void playDebuffFor(ServerLevel level,BattleState state,String targetId,Set<String> played){
        CombatantState target=state.find(targetId);if(!isCoreHero(target)||target.downed()||!played.add(targetId))return;
        Entity targetActor=entity(level,targetId);if(targetActor instanceof BattleActorEntity animated)animated.playDebuff();
    }

    private static boolean isCoreHero(CombatantState target){
        if(target==null)return false;
        return switch(target.definition().id()){case "P01","P02","P03","P04","P05","P06","P07","P08"->true;default->false;};
    }

    private void presentSummonDown(ServerLevel level,BattleState state,String summonId){
        Entity summon=entity(level,summonId);Vec3 home=homes.get(summonId);if(!(summon instanceof BattleActorEntity animated))return;
        CombatantState immediateReplacement=state.find(summonId);
        if(immediateReplacement!=null&&!immediateReplacement.downed()){
            animated.playRevive();if(home!=null)BattleVfx.revive(level,home);pendingRemovalTicks.remove(summonId);return;
        }
        animated.playDeath();if(home!=null)BattleVfx.down(level,home);pendingRemovalTicks.put(summonId,12);
    }

    private void playHitFor(ServerLevel level,BattleState state,String targetId,int damage){
        CombatantState target=state.find(targetId);if(target==null||target.downed())return;
        Entity targetActor=entity(level,targetId);if(!(targetActor instanceof BattleActorEntity animated))return;
        boolean heavy=damage>=Math.max(1,(int)Math.floor(target.maxHp()*.18));animated.playHit(heavy);
    }

    private static int phaseFor(CombatantState unit){if(!unit.definition().boss())return 1;double hp=unit.hp()/(double)Math.max(1,unit.maxHp());double p2=unit.definition().param("phase2",-1),p3=unit.definition().param("phase3",-1);if(p3>0&&hp<=p3)return 3;if(p2>0&&hp<=p2)return 2;return 1;}
    void turnReady(ServerLevel level,String actorId){Entity actor=entity(level,actorId);if(actor instanceof BattleActorEntity a)a.playReady();}

    void finish(ServerLevel level,BattleOutcome outcome){if(finishPlayed||outcome==BattleOutcome.RUNNING)return;finishPlayed=true;CombatantSide winning=outcome==BattleOutcome.ALLY_VICTORY?CombatantSide.ALLY:CombatantSide.ENEMY;for(String id:actors.keySet()){if(sides.get(id)!=winning||Boolean.TRUE.equals(downed.get(id)))continue;Entity entity=entity(level,id);if(entity instanceof BattleActorEntity a)a.playVictory();Vec3 home=homes.get(id);if(home!=null)BattleVfx.victory(level,home);}}

    private ArmorStand marker(ServerLevel level,Vec3 pos,String text,ChatFormatting color){ArmorStand marker=new ArmorStand(level,pos.x,pos.y,pos.z);marker.setInvisible(true);marker.setInvulnerable(true);marker.setNoGravity(true);marker.setCustomName(Component.literal(text).withStyle(color,ChatFormatting.BOLD));marker.setCustomNameVisible(true);level.addFreshEntity(marker);return marker;}
    void clearFocus(ServerLevel level){if(focusMarker==null)return;Entity entity=level.getEntity(focusMarker);if(entity!=null)entity.discard();focusMarker=null;}
    private void clearDanger(ServerLevel level){if(dangerMarker!=null){Entity entity=level.getEntity(dangerMarker);if(entity!=null)entity.discard();}dangerMarker=null;dangerTarget="";}

    void performSkill(ServerLevel level,String actorId,String visualId,String skillId,String targetId,boolean damaging){
        Entity actor=entity(level,actorId);
        Vec3 source=homes.get(actorId);Vec3 target=targetId==null||targetId.isBlank()?source:homes.get(targetId);
        boolean closesDistance=shouldCloseDistance(visualId,skillId,damaging);

        if(actor!=null&&source!=null&&target!=null){
            faceAt(actor,target);
            if(closesDistance){
                Vec3 delta=target.subtract(source);
                if(delta.lengthSqr()>.001)actor.setPos(target.subtract(delta.normalize().scale(1.45)));
            }
            returnTimers.put(actorId,actionPresentationTicks(visualId,skillId));
        }

        if(actor instanceof BattleActorEntity animated) playSkillAnimation(animated,visualId,skillId,damaging,closesDistance);
        if(source!=null){
            Vec3 resolvedTarget=target==null?source:target;
            EnemyBattleTelegraphs.present(level,visualId,skillId,source,resolvedTarget);
            if(BossBattleVfx.handles(visualId))BossBattleVfx.skill(level,visualId,skillId,source,resolvedTarget);
            else BattleVfx.skill(level,visualId,skillId,source,resolvedTarget,damaging);
        }
    }

    /** Canonical Skill IDs choose authored clips instead of damage/cast guessing. */
    private static void playSkillAnimation(BattleActorEntity actor,String visualId,String skillId,boolean damaging,boolean moving){
        if(EnemyPresentationProfile.handles(skillId)){
            EnemyPresentationProfile.play(actor,skillId);
            return;
        }
        switch(skillId){
            case "p01_chase_slash","p02_accelerate","p03_guard_stance","p04_heal","p05_suppressive_shot","p06_echo","p07_command","p08_frenzy" -> playBasic(actor,moving);
            case "p01_breaker_strike","p02_time_leap","p03_guard_transfer","p04_returned_breath","p05_piercing_shot","p06_condolence","p07_summon_toto","p08_blood_charge" -> playActive1(actor,moving);
            case "p01_duel_lock","p02_delay_field","p03_shield_pressure","p04_resting_light","p05_hunt_signal","p06_funeral_order","p07_joint_attack","p08_battle_mania" -> playActive2(actor,moving);

            case "b01_basic" -> actor.playStrike();
            case "b01_scratch" -> actor.playCast();
            case "b01_warn" -> actor.playCast();
            case "b01_charge" -> actor.playCharge();

            case "b02_basic" -> actor.playStrike();
            case "b02_root_prison","b02_thorn_wave" -> actor.playCast();
            case "b02_summon" -> actor.playSummon();

            case "b03_basic" -> actor.playStrike();
            case "b03_drain","b03_barrier" -> actor.playCast();
            case "b03_overclock" -> actor.playPhase();

            case "b04_basic" -> actor.playStrike();
            case "b04_collapse" -> actor.playCharge();
            case "b04_fury","b04_warn","b04_eruption" -> actor.playCast();

            case "b05_basic" -> actor.playStrike();
            case "b05_time_cut" -> actor.playCharge();
            case "b05_mark","b05_order_collapse","b05_rift_wave","b05_warn" -> actor.playCast();
            case "b05_relay_collapse" -> actor.playPhase();

            default -> {if(damaging)actor.playStrike();else actor.playCast();}
        }
    }

    private static void playBasic(BattleActorEntity actor,boolean moving){if(moving)actor.playMovingBasic();else actor.playBasic();}
    private static void playActive1(BattleActorEntity actor,boolean moving){if(moving)actor.playMovingActive1();else actor.playActive1();}
    private static void playActive2(BattleActorEntity actor,boolean moving){if(moving)actor.playMovingActive2();else actor.playActive2();}

    /**
     * Closing distance is authored, not inferred from "deals damage". This keeps archers, casters,
     * time manipulation and ground eruptions on their anchors while melee/dash attacks still read physically.
     */
    private static boolean shouldCloseDistance(String visualId,String skillId,boolean damaging){
        if(!damaging)return false;
        if(EnemyPresentationProfile.handles(skillId))return EnemyPresentationProfile.closeDistance(skillId);
        return switch(visualId){
            case "P01" -> "p01_chase_slash".equals(skillId)||"p01_breaker_strike".equals(skillId);
            case "P03" -> "p03_shield_pressure".equals(skillId);
            case "P08" -> "p08_frenzy".equals(skillId)||"p08_blood_charge".equals(skillId);
            case "P07_SUMMON" -> true;
            case "B01" -> "b01_basic".equals(skillId)||"b01_charge".equals(skillId);
            case "B04" -> "b04_basic".equals(skillId);
            case "B05" -> "b05_basic".equals(skillId)||"b05_time_cut".equals(skillId);
            default -> false;
        };
    }

    /**
     * Visual pacing in ticks at 1x. These values follow the authored clip lengths closely enough that
     * the next turn does not overwrite the current action before its readable hit/recovery beat.
     */
    static int actionPresentationTicks(String visualId,String skillId){
        if(EnemyPresentationProfile.handles(skillId))return EnemyPresentationProfile.oneXticks(skillId);
        return switch(skillId){
            case "p01_chase_slash" -> 28; case "p01_breaker_strike" -> 41; case "p01_duel_lock" -> 18;
            case "p02_accelerate" -> 16; case "p02_time_leap" -> 28; case "p02_delay_field" -> 34;
            case "p03_guard_stance" -> 17; case "p03_guard_transfer" -> 23; case "p03_shield_pressure" -> 35;
            case "p04_heal" -> 22; case "p04_returned_breath" -> 46; case "p04_resting_light" -> 36;
            case "p05_suppressive_shot" -> 16; case "p05_piercing_shot" -> 24; case "p05_hunt_signal" -> 19;
            case "p06_echo" -> 19; case "p06_condolence" -> 28; case "p06_funeral_order" -> 32;
            case "p07_command" -> 18; case "p07_summon_toto" -> 34; case "p07_joint_attack" -> 26;
            case "p08_frenzy" -> 27; case "p08_blood_charge" -> 34; case "p08_battle_mania" -> 21;

            case "b01_basic" -> 18; case "b01_scratch" -> 23; case "b01_warn" -> 20; case "b01_charge" -> 24;
            case "b02_basic" -> 18; case "b02_root_prison" -> 24; case "b02_summon" -> 28; case "b02_thorn_wave" -> 26;
            case "b03_basic" -> 18; case "b03_drain","b03_barrier" -> 24; case "b03_overclock" -> 31;
            case "b04_basic" -> 20; case "b04_collapse" -> 24; case "b04_fury" -> 24; case "b04_warn" -> 20; case "b04_eruption" -> 28;
            case "b05_basic" -> 17; case "b05_time_cut" -> 20; case "b05_mark" -> 21; case "b05_order_collapse" -> 27; case "b05_rift_wave" -> 26; case "b05_warn" -> 19; case "b05_relay_collapse" -> 31;
            default -> BossBattleVfx.handles(visualId)?20:8;
        };
    }

    private static void faceAt(Entity actor,Vec3 target){
        Vec3 delta=target.subtract(actor.position());double horizontal=delta.x*delta.x+delta.z*delta.z;if(horizontal<.0001)return;
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));actor.setYRot(yaw);actor.setYHeadRot(yaw);
    }

    void lunge(ServerLevel level,String actorId,String visualId,String skillId,String targetId){performSkill(level,actorId,visualId,skillId,targetId,true);}

    void tick(ServerLevel level){
        tickPendingRemovals(level);tickReturns(level);
    }

    private void tickReturns(ServerLevel level){
        for(String id:List.copyOf(returnTimers.keySet())){
            int left=returnTimers.getOrDefault(id,0)-1;
            if(left>0){returnTimers.put(id,left);continue;}
            returnTimers.remove(id);Entity actor=entity(level,id);Vec3 home=homes.get(id);if(actor==null||home==null)continue;
            actor.setPos(home);Float yaw=homeYaws.get(id);if(yaw!=null){actor.setYRot(yaw);actor.setYHeadRot(yaw);}
        }
    }

    private void tickPendingRemovals(ServerLevel level){
        for(String id:List.copyOf(pendingRemovalTicks.keySet())){
            int left=pendingRemovalTicks.getOrDefault(id,0)-1;
            if(left<=0)removeActor(level,id);else pendingRemovalTicks.put(id,left);
        }
    }

    void cleanup(ServerLevel level){clearFocus(level);clearDanger(level);cleanupActors(level);finishPlayed=false;}
    private void cleanupActors(ServerLevel level){for(UUID id:actors.values()){Entity entity=level.getEntity(id);if(entity!=null)entity.discard();}actors.clear();homes.clear();homeYaws.clear();sides.clear();summons.clear();visualIds.clear();downed.clear();barriers.clear();bossPhases.clear();pendingRemovalTicks.clear();returnTimers.clear();}
    private Entity entity(ServerLevel level,String id){UUID uuid=actors.get(id);return uuid==null?null:level.getEntity(uuid);}

    private static void equipStandIn(ArmorStand stand,CombatantState combatant){
        boolean ally=combatant.side()==CombatantSide.ALLY;String id=combatant.definition().id();if(combatant.definition().summon()){setSmall(stand);stand.setItemSlot(EquipmentSlot.HEAD,Items.WOLF_ARMOR.getDefaultInstance());return;}
        stand.setItemSlot(EquipmentSlot.CHEST,(ally?Items.CHAINMAIL_CHESTPLATE:Items.IRON_CHESTPLATE).getDefaultInstance());stand.setItemSlot(EquipmentSlot.LEGS,(ally?Items.LEATHER_LEGGINGS:Items.IRON_LEGGINGS).getDefaultInstance());stand.setItemSlot(EquipmentSlot.FEET,(ally?Items.LEATHER_BOOTS:Items.IRON_BOOTS).getDefaultInstance());
        if(ally){ItemStack mainHand=switch(id){case"P01"->Items.DIAMOND_SWORD.getDefaultInstance();case"P02"->Items.CLOCK.getDefaultInstance();case"P03"->Items.IRON_SWORD.getDefaultInstance();case"P04"->Items.BLAZE_ROD.getDefaultInstance();case"P05","F03"->Items.CROSSBOW.getDefaultInstance();case"P06"->Items.IRON_HOE.getDefaultInstance();case"P07"->Items.PAPER.getDefaultInstance();case"P08"->Items.IRON_AXE.getDefaultInstance();case"F04"->Items.IRON_SWORD.getDefaultInstance();default->Items.IRON_SWORD.getDefaultInstance();};stand.setItemSlot(EquipmentSlot.MAINHAND,mainHand);if(id.equals("P03")||id.equals("F04"))stand.setItemSlot(EquipmentSlot.OFFHAND,Items.SHIELD.getDefaultInstance());stand.setItemSlot(EquipmentSlot.HEAD,(id.equals("F03")||id.equals("F04"))?Items.LEATHER_HELMET.getDefaultInstance():Items.DIAMOND_HELMET.getDefaultInstance());return;}
        stand.setItemSlot(EquipmentSlot.HEAD,Items.IRON_HELMET.getDefaultInstance());switch(id){case"E002"->stand.setItemSlot(EquipmentSlot.MAINHAND,Items.BOW.getDefaultInstance());case"E003"->stand.setItemSlot(EquipmentSlot.MAINHAND,Items.TNT.getDefaultInstance());case"E005","E007","E011","E013"->stand.setItemSlot(EquipmentSlot.MAINHAND,Items.BLAZE_ROD.getDefaultInstance());case"B01","B04"->stand.setItemSlot(EquipmentSlot.MAINHAND,Items.IRON_AXE.getDefaultInstance());case"B05"->stand.setItemSlot(EquipmentSlot.MAINHAND,Items.DIAMOND_SWORD.getDefaultInstance());default->stand.setItemSlot(EquipmentSlot.MAINHAND,Items.IRON_SWORD.getDefaultInstance());}
    }
    private static void setSmall(ArmorStand stand){byte flags=stand.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS);stand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS,(byte)(flags|ArmorStand.CLIENT_FLAG_SMALL));}
}
