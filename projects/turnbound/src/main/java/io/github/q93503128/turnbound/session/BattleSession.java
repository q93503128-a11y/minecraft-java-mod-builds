package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class BattleSession {
    private final BattleEngine engine; private final Vec3 anchor; private final BattlePresentation presentation=new BattlePresentation();
    private boolean auto; private int speed=1; private int delayTicks=8; private boolean finished; private boolean readyShown;
    BattleSession(ServerPlayer player){ engine=new BattleEngine(P0Scenario.create()); anchor=player.position(); presentation.spawn(player,engine.state().combatants()); }
    public BattleState state(){return engine.state();} public boolean auto(){return auto;} public int speed(){return speed;} public boolean finished(){return finished;}
    void tick(ServerPlayer player){
        ServerLevel level=(ServerLevel)player.level(); presentation.tick(level);
        if(!finished) lock(player);
        if(engine.state().outcome()!=BattleOutcome.RUNNING){ finished=true; return; }
        if(delayTicks>0){delayTicks--;return;}
        CombatantState actor=engine.state().currentActorId()==null?engine.nextReady():engine.state().combatant(engine.state().currentActorId());
        if(!readyShown){ readyShown=true; delayTicks=presentationDelay(); BattleNetwork.sync(player,this); return; }
        if(actor.side()==CombatantSide.ENEMY||auto){ autoAct(level,actor); readyShown=false; delayTicks=presentationDelay(); BattleNetwork.sync(player,this); }
    }
    void action(ServerPlayer player,String actorId,String skillId,String targetId){
        if(finished||auto||engine.state().outcome()!=BattleOutcome.RUNNING)return; if(!actorId.equals(engine.state().currentActorId()))return; CombatantState actor=engine.state().combatant(actorId); if(actor.side()!=CombatantSide.ALLY)return;
        try{ SkillDefinition skill=actor.definition().skill(skillId); if(skill.targetRule()==TargetRule.SELF||skill.targetRule()==TargetRule.ALLY_ALL||skill.targetRule()==TargetRule.ENEMY_ALL) engine.useSkill(actorId,skillId); else engine.useSkill(actorId,skillId,targetId); presentation.lunge((ServerLevel)player.level(),actorId,targetId); readyShown=false; delayTicks=presentationDelay(); if(engine.state().outcome()!=BattleOutcome.RUNNING)finished=true; BattleNetwork.sync(player,this); } catch(RuntimeException ignored){ BattleNetwork.sync(player,this); }
    }
    void toggleAuto(ServerPlayer player){auto=!auto; BattleNetwork.sync(player,this);}
    void toggleSpeed(ServerPlayer player){speed=speed==1?2:1; BattleNetwork.sync(player,this);}
    void cleanup(ServerPlayer player){presentation.cleanup((ServerLevel)player.level());}
    private void autoAct(ServerLevel level,CombatantState actor){
        String targetBefore=defaultTarget(actor); try{ P0Scenario.chooseAutoAction(engine,engine.state(),actor); }catch(RuntimeException ex){ fallback(actor); } if(targetBefore!=null)presentation.lunge(level,actor.instanceId(),targetBefore); if(engine.state().outcome()!=BattleOutcome.RUNNING)finished=true;
    }
    private String defaultTarget(CombatantState actor){ var side=actor.side().opposite(); var living=engine.state().living(side); return living.isEmpty()?null:living.getFirst().instanceId(); }
    private void fallback(CombatantState actor){ var targets=engine.state().living(actor.side().opposite()); if(!targets.isEmpty())engine.useSkill(actor.instanceId(),actor.definition().basicSkillId(),targets.getFirst().instanceId()); }
    private void lock(ServerPlayer p){ if(p.position().distanceToSqr(anchor)>0.0025)p.setPos(anchor.x,anchor.y,anchor.z); p.setDeltaMovement(Vec3.ZERO); }
    private int presentationDelay(){return speed==2?4:8;}
}
