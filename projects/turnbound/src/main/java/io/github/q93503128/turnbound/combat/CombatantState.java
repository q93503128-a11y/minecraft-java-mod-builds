package io.github.q93503128.turnbound.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CombatantState {
    private final String instanceId; private final CombatantDefinition definition; private final CombatantSide side; private final int initiativeSeed;
    private int hp; private int barrier; private long gauge; private boolean downed;
    private final Map<String,Integer> cooldowns=new HashMap<>(), counters=new HashMap<>();
    private final Map<String,String> refs=new HashMap<>();
    private final Map<String,StatusInstance> statuses=new HashMap<>();
    private final Set<String> flags=new HashSet<>();
    public CombatantState(String instanceId, CombatantDefinition definition, CombatantSide side, int seed){this.instanceId=instanceId;this.definition=definition;this.side=side;this.initiativeSeed=seed;this.hp=definition.stats().maxHp();}
    public String instanceId(){return instanceId;} public CombatantDefinition definition(){return definition;} public CombatantSide side(){return side;} public int initiativeSeed(){return initiativeSeed;}
    public int hp(){return hp;} public int maxHp(){return definition.stats().maxHp();} public int attack(){return definition.stats().attack();} public int defense(){return definition.stats().defense();} public int speed(){return definition.stats().speed();}
    public int barrier(){return barrier;} public long gauge(){return gauge;} public boolean downed(){return downed;}
    public void setGauge(long v){gauge=Math.max(0,v);} public void addGauge(long v){setGauge(gauge+v);} public void spendTurnGauge(){gauge=Math.max(0,gauge-BattleEngine.TURN_THRESHOLD);}
    public void setCooldown(String id,int v){if(v<=0)cooldowns.remove(id);else cooldowns.put(id,v);} public int cooldown(String id){return cooldowns.getOrDefault(id,0);} public Map<String,Integer> cooldownsView(){return Map.copyOf(cooldowns);}
    public int counter(String id){return counters.getOrDefault(id,0);} public void setCounter(String id,int v){if(v==0)counters.remove(id);else counters.put(id,v);} public void incrementCounter(String id,int d,int max){setCounter(id,Math.min(max,counter(id)+d));}
    public String ref(String id){return refs.get(id);} public void setRef(String id,String v){if(v==null)refs.remove(id);else refs.put(id,v);} public boolean flag(String id){return flags.contains(id);} public void setFlag(String id){flags.add(id);}
    public StatusInstance status(String id){return statuses.get(id);} public void putStatus(StatusInstance s){statuses.put(s.id(),s);} public void removeStatus(String id){statuses.remove(id);}
    public int addBarrier(int amount){int cap=(int)Math.floor(maxHp()*0.60);int before=barrier;barrier=Math.min(cap,Math.max(0,barrier+amount));return barrier-before;}
    public int heal(int amount){if(downed||amount<=0)return 0;int before=hp;hp=Math.min(maxHp(),hp+amount);return hp-before;}
    public int takeDamage(int amount){if(downed||amount<=0)return 0;int remain=amount, absorbed=Math.min(barrier,remain);barrier-=absorbed;remain-=absorbed;int before=hp;hp=Math.max(0,hp-remain);if(hp==0)downed=true;return before-hp;}
    public int revive(double ratio){if(!downed)return 0;hp=Math.max(1,(int)Math.floor(maxHp()*ratio));barrier=0;gauge=0;downed=false;statuses.clear();return hp;}
    public void tickStatusesOnOwnTurn(){for(var e:Map.copyOf(statuses).entrySet()){var n=e.getValue().tickOwnerTurn();if(n.remainingOwnerTurns()<=0)statuses.remove(e.getKey());else statuses.put(e.getKey(),n);}}
}
