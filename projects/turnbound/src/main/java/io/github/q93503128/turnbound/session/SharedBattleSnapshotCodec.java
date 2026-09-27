package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/** Per-viewer projection of one shared authoritative battle. */
final class SharedBattleSnapshotCodec {
    private SharedBattleSnapshotCodec() {}

    static String encode(UUID playerId,SharedBattleSession session){
        BattleState state=session.state();
        boolean running=state.outcome()==BattleOutcome.RUNNING;
        String actorId=running&&state.currentActorId()!=null?state.currentActorId():"";
        boolean controlsAllowed=running&&!session.finished();
        StringBuilder out=new StringBuilder();
        out.append("H|1|").append(session.auto(playerId)?1:0).append('|').append(session.speed()).append('|').append(state.outcome()).append('|').append(actorId).append('|').append(session.finished()?1:0).append('|')
                .append(controlsAllowed&&session.autoAllowed(playerId)?1:0).append('|').append(controlsAllowed&&session.speedAllowed(playerId)?1:0).append('|').append(session.finished()||(controlsAllowed&&session.fleeAllowed(playerId))?1:0).append('\n');
        out.append("C|").append(safe(session.encounterId())).append('\n');
        Vec3 arena=session.battleAnchor();
        out.append("A|").append(number(arena.x)).append('|').append(number(arena.y)).append('|').append(number(arena.z)).append('|').append(number(session.battleYaw())).append('\n');
        out.append("V|").append(number(session.returnYaw(playerId))).append('|').append(number(session.returnPitch(playerId))).append('\n');

        String message=BattleContextualGuidance.resolve(session.encounterId(),state).text();
        if(running&&!actorId.isBlank()){
            CombatantState current=state.find(actorId);
            if(current!=null&&current.side()==CombatantSide.ALLY&&!session.canControlActor(playerId,actorId))message="동료의 행동을 기다리는 중";
        }
        out.append("M|").append(safe(message)).append('\n');

        for(CombatantState unit:state.combatants()){
            Vec3 pos=session.combatantPosition(unit.instanceId());if(pos==null)pos=arena;
            String statuses=BattleSnapshotCodec.presentationStates(state,unit).stream().collect(Collectors.joining(","));
            out.append("U|").append(unit.instanceId()).append('|').append(unit.definition().id()).append('|').append(unit.side()).append('|').append(safe(unit.definition().name())).append('|')
                    .append(unit.hp()).append('|').append(unit.maxHp()).append('|').append(unit.barrier()).append('|').append(unit.gauge()).append('|').append(unit.downed()?1:0).append('|')
                    .append(number(pos.x)).append('|').append(number(pos.y)).append('|').append(number(pos.z)).append('|').append(statuses).append('\n');
            UUID owner=session.ownerOf(unit.instanceId());
            if(owner!=null)out.append("O|").append(unit.instanceId()).append('|').append(owner).append('|').append(owner.equals(playerId)?1:0).append('\n');
        }
        out.append("T|").append(state.timelinePreview(8).stream().map(CombatantState::instanceId).collect(Collectors.joining(","))).append('\n');

        if(running&&state.currentActorId()!=null&&session.canControlActor(playerId,state.currentActorId())){
            CombatantState current=state.combatant(state.currentActorId());
            for(SkillDefinition skill:current.definition().skills()){
                String canonical=current.definition().canonicalSkillId(skill.id());
                out.append("S|").append(canonical).append('|').append(safe(skill.name())).append('|').append(skill.targetRule()).append('|').append(skill.cooldown()).append('|').append(current.cooldown(skill.id())).append('|').append(safe(skill.description())).append('\n');
            }
        }

        BattleResultPreview.View preview=BattleResultPreview.enrich(playerId,session.rewardTransactionId(playerId),session.encounterId(),state,session.resultSummary(playerId));
        BattleResultSummary result=preview.summary();
        out.append("R|").append(result.xp()).append('|').append(result.gold()).append('|').append(result.firstClear()?1:0).append('|').append(result.crystal()).append('|').append(result.starEssence()).append('|').append(safe(String.join(",",result.equipmentRewards()))).append('\n');
        for(BattleResultPreview.Notice notice:preview.notices())out.append("N|").append(safe(notice.code())).append('|').append(safe(notice.text())).append('\n');
        for(BattleResultSummary.PartyXp member:result.party())out.append("P|").append(safe(member.characterId())).append('|').append(safe(member.name())).append('|').append(member.levelBefore()).append('|').append(member.xpBefore()).append('|').append(member.levelAfter()).append('|').append(member.xpAfter()).append('|').append(member.xpToNextAfter()).append('\n');
        return out.toString();
    }

    private static String safe(String v){return v==null?"":v.replace('|','/').replace('\n',' ').replace('\r',' ');}
    private static String number(double v){return String.format(Locale.ROOT,"%.3f",v);}
}
