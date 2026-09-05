package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.combat.SkillDefinition;
import io.github.q93503128.turnbound.combat.StatusInstance;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public final class BattleSnapshotCodec {
    private BattleSnapshotCodec() {}
    public static String encode(BattleSession session) { return encode(null, session); }

    public static String encode(UUID playerId, BattleSession session) {
        BattleState state = session.state();
        boolean running = state.outcome() == BattleOutcome.RUNNING;
        String actor = running && state.currentActorId() != null ? state.currentActorId() : "";
        boolean controlsAllowed = running && !session.finished();
        StringBuilder out = new StringBuilder();
        out.append("H|1|").append(session.auto()?1:0).append('|').append(session.speed()).append('|').append(state.outcome()).append('|').append(actor).append('|').append(session.finished()?1:0).append('|')
                .append(controlsAllowed&&session.autoAllowed()?1:0).append('|').append(controlsAllowed&&session.speedAllowed()?1:0).append('|').append(controlsAllowed&&session.fleeAllowed()?1:0).append('\n');
        out.append("C|").append(safe(session.encounterId())).append('\n');
        Vec3 arena=session.battleAnchor();out.append("A|").append(number(arena.x)).append('|').append(number(arena.y)).append('|').append(number(arena.z)).append('|').append(number(session.battleYaw())).append('\n');

        for(CombatantState combatant:state.combatants()){
            Vec3 pos=session.combatantPosition(combatant.instanceId());if(pos==null)pos=arena;
            String statuses=presentationStates(combatant).stream().collect(Collectors.joining(","));
            out.append("U|").append(combatant.instanceId()).append('|').append(combatant.definition().id()).append('|').append(combatant.side()).append('|').append(safe(combatant.definition().name())).append('|')
                    .append(combatant.hp()).append('|').append(combatant.maxHp()).append('|').append(combatant.barrier()).append('|').append(combatant.gauge()).append('|').append(combatant.downed()?1:0).append('|')
                    .append(number(pos.x)).append('|').append(number(pos.y)).append('|').append(number(pos.z)).append('|').append(statuses).append('\n');
        }
        out.append("T|").append(state.timelinePreview(8).stream().map(CombatantState::instanceId).collect(Collectors.joining(","))).append('\n');
        if(running&&state.currentActorId()!=null){CombatantState current=state.combatant(state.currentActorId());for(SkillDefinition skill:current.definition().skills()){String canonicalSkillId=current.definition().canonicalSkillId(skill.id());out.append("S|").append(canonicalSkillId).append('|').append(safe(skill.name())).append('|').append(skill.targetRule()).append('|').append(skill.cooldown()).append('|').append(current.cooldown(skill.id())).append('|').append(safe(skill.description())).append('\n');}}

        BattleResultPreview.View preview=playerId==null?new BattleResultPreview.View(session.resultSummary(),java.util.List.of()):BattleResultPreview.enrich(playerId,session.rewardTransactionId(),session.encounterId(),state,session.resultSummary());
        BattleResultSummary result=preview.summary();out.append("R|").append(result.xp()).append('|').append(result.gold()).append('|').append(result.firstClear()?1:0).append('|').append(result.crystal()).append('|').append(result.starEssence()).append('|').append(safe(String.join(",",result.equipmentRewards()))).append('\n');
        for(BattleResultPreview.Notice notice:preview.notices())out.append("N|").append(safe(notice.code())).append('|').append(safe(notice.text())).append('\n');
        for(BattleResultSummary.PartyXp member:result.party())out.append("P|").append(safe(member.characterId())).append('|').append(safe(member.name())).append('|').append(member.levelBefore()).append('|').append(member.xpBefore()).append('|').append(member.levelAfter()).append('|').append(member.xpAfter()).append('|').append(member.xpToNextAfter()).append('\n');
        return out.toString();
    }

    /** Preserve raw status IDs for client rules while adding HUD-only stack/duration/resource tokens. */
    static List<String> presentationStates(CombatantState combatant){
        List<String> out=new ArrayList<>();
        combatant.statusesView().values().stream().sorted(java.util.Comparator.comparing(StatusInstance::id).thenComparing(StatusInstance::sourceId)).forEach(status->{
            if(!out.contains(status.id()))out.add(status.id());
            out.add("@s:"+status.id()+":"+status.stacks()+":"+status.remainingOwnerTurns()+":"+number(status.magnitude()));
        });
        String id=combatant.definition().id();
        if("P01".equals(id))resource(out,"focus",combatant.counter("focus"),combatant.definition().intParam("focusMax",3));
        else if("P06".equals(id))resource(out,"memory",combatant.counter("memory"),combatant.definition().intParam("memoryMax",5));
        else if("P07".equals(id))resource(out,"contract",combatant.counter("contract_prep"),combatant.definition().intParam("prepMax",2));
        else if("P05".equals(id)&&combatant.counter("p05_hunt_actions")>0)resource(out,"hunt",combatant.counter("p05_hunt_actions"),2);
        return List.copyOf(out);
    }

    private static void resource(List<String> out,String id,int value,int max){if(value>0)out.add("@r:"+id+":"+value+":"+Math.max(1,max));}
    private static String safe(String value){return value==null?"":value.replace('|','/').replace('\n',' ').replace('\r',' ');}
    private static String number(double value){return String.format(Locale.ROOT,"%.3f",value);}
}
