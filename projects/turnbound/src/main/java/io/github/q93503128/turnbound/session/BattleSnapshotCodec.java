package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.*;
import java.util.List;
import java.util.stream.Collectors;

public final class BattleSnapshotCodec {
    private BattleSnapshotCodec(){}
    public static String encode(BattleSession s){
        BattleState st=s.state(); String actor=st.currentActorId()==null?"":st.currentActorId();
        StringBuilder out=new StringBuilder();
        out.append("H|").append(1).append('|').append(s.auto()?1:0).append('|').append(s.speed()).append('|').append(st.outcome()).append('|').append(actor).append('|').append(s.finished()?1:0).append('\n');
        for(CombatantState c:st.combatants()){
            out.append("U|").append(c.instanceId()).append('|').append(c.definition().id()).append('|').append(c.side()).append('|').append(c.definition().name()).append('|')
               .append(c.hp()).append('|').append(c.maxHp()).append('|').append(c.barrier()).append('|').append(c.gauge()).append('|').append(c.downed()?1:0).append('\n');
        }
        out.append("T|").append(st.timelinePreview(8).stream().map(CombatantState::instanceId).collect(Collectors.joining(","))).append('\n');
        if(st.currentActorId()!=null){
            CombatantState current=st.combatant(st.currentActorId());
            for(SkillDefinition skill:current.definition().skills()) out.append("S|").append(skill.id()).append('|').append(skill.name()).append('|').append(skill.targetRule()).append('|').append(skill.cooldown()).append('|').append(current.cooldown(skill.id())).append('\n');
        }
        List<BattleEvent> ev=st.events();
        if(!ev.isEmpty()){ BattleEvent e=ev.getLast(); out.append("M|").append(e.type()).append('|').append(e.sourceId()).append('|').append(e.targetId()).append('|').append(e.value()).append('|').append(e.detail().replace('|','/')).append('\n'); }
        return out.toString();
    }
}
