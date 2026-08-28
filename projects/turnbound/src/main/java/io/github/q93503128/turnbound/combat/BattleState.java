package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BattleState {
    private final List<CombatantState> combatants; private final List<BattleEvent> events=new ArrayList<>(); private String currentActorId; private long logicalPulse;
    public BattleState(List<CombatantState> combatants){this.combatants=List.copyOf(combatants);long a=combatants.stream().filter(c->c.side()==CombatantSide.ALLY).count(),e=combatants.stream().filter(c->c.side()==CombatantSide.ENEMY).count();if(a<1||a>4||e<1||e>5)throw new IllegalArgumentException("Battle requires 1-4 allies and 1-5 enemies");}
    public List<CombatantState> combatants(){return combatants;} public List<BattleEvent> events(){return List.copyOf(events);} public String currentActorId(){return currentActorId;} public long logicalPulse(){return logicalPulse;}
    void setCurrentActorId(String id){currentActorId=id;} void addLogicalPulse(long p){logicalPulse+=p;} void addEvent(BattleEvent e){events.add(e);}
    public CombatantState combatant(String id){return combatants.stream().filter(c->c.instanceId().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown combatant "+id));}
    public List<CombatantState> living(CombatantSide side){return combatants.stream().filter(c->c.side()==side&&!c.downed()).toList();} public List<CombatantState> downed(CombatantSide side){return combatants.stream().filter(c->c.side()==side&&c.downed()).toList();}
    public BattleOutcome outcome(){if(living(CombatantSide.ENEMY).isEmpty())return BattleOutcome.ALLY_VICTORY;if(living(CombatantSide.ALLY).isEmpty())return BattleOutcome.ENEMY_VICTORY;return BattleOutcome.RUNNING;}
    public List<CombatantState> timelinePreview(int count){record N(CombatantState c,long[] g){} List<N> nodes=combatants.stream().filter(c->!c.downed()).map(c->new N(c,new long[]{c.gauge()})).toList();List<CombatantState> out=new ArrayList<>();while(out.size()<count&&!nodes.isEmpty()){long p=nodes.stream().mapToLong(n->pulses(n.c(),n.g()[0])).min().orElse(0);for(N n:nodes)n.g()[0]+=p*n.c().speed();N selected=nodes.stream().filter(n->n.g()[0]>=BattleEngine.TURN_THRESHOLD).max(Comparator.comparingLong((N n)->n.g()[0]).thenComparingInt(n->n.c().speed()).thenComparingInt(n->-n.c().initiativeSeed())).orElseThrow();out.add(selected.c());selected.g()[0]-=BattleEngine.TURN_THRESHOLD;}return List.copyOf(out);}
    private static long pulses(CombatantState c,long gauge){if(gauge>=BattleEngine.TURN_THRESHOLD)return 0;long m=BattleEngine.TURN_THRESHOLD-gauge;return(m+c.speed()-1L)/c.speed();}
}
