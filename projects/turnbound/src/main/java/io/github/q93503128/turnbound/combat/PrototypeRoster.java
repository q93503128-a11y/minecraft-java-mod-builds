package io.github.q93503128.turnbound.combat;

import java.util.List;

public final class PrototypeRoster {
    private PrototypeRoster() {}
    public static CombatantDefinition kyren(){return new CombatantDefinition("P01","카이렌",new BattleStats(900,120,85,105),"p01_basic",List.of(new SkillDefinition("p01_basic","추적 베기",TargetRule.ENEMY_SINGLE,0,List.of(SkillEffect.damage(1.00))),new SkillDefinition("p01_shatter","파쇄 일격",TargetRule.ENEMY_SINGLE,2,List.of(SkillEffect.damage(2.20))),new SkillDefinition("p01_duel_lock","결투 고정",TargetRule.ENEMY_SINGLE,3,List.of(SkillEffect.selfGaugeAdd(120)))));}
    public static CombatantDefinition lumea(){return new CombatantDefinition("P02","루메아",new BattleStats(780,90,75,125),"p02_basic",List.of(new SkillDefinition("p02_basic","가속",TargetRule.ALLY_SINGLE,0,List.of(SkillEffect.gaugeAdd(180))),new SkillDefinition("p02_time_leap","시간 도약",TargetRule.ALLY_SINGLE,4,List.of(SkillEffect.gaugeAtLeast(1000))),new SkillDefinition("p02_delay_field","지연장",TargetRule.ENEMY_ALL,3,List.of(SkillEffect.gaugeAdd(-120)))));}
    public static CombatantDefinition bram(){return new CombatantDefinition("P03","브람",new BattleStats(1250,88,130,75),"p03_basic",List.of(new SkillDefinition("p03_basic","방진",TargetRule.SELF,0,List.of(SkillEffect.barrier(0.12))),new SkillDefinition("p03_guard","보호 전환",TargetRule.ALLY_SINGLE,3,List.of(SkillEffect.guardRedirect(0.70,2))),new SkillDefinition("p03_press","방패 압박",TargetRule.ENEMY_SINGLE,2,List.of(SkillEffect.damage(0.90),SkillEffect.gaugeAdd(-120)))));}
    public static CombatantDefinition elysia(){return new CombatantDefinition("P04","엘리시아",new BattleStats(820,105,70,95),"p04_basic",List.of(new SkillDefinition("p04_basic","치유",TargetRule.ALLY_SINGLE,0,List.of(SkillEffect.heal(0.70))),new SkillDefinition("p04_revive","되돌아온 숨",TargetRule.DEAD_ALLY_SINGLE,5,List.of(SkillEffect.revive(0.30))),new SkillDefinition("p04_rest_light","안식의 빛",TargetRule.ALLY_ALL,3,List.of(SkillEffect.heal(0.90)))));}
    public static CombatantDefinition trainingEnemy(String id,String name,int hp,int attack,int defense,int speed){String basic=id.toLowerCase()+"_basic";return new CombatantDefinition(id,name,new BattleStats(hp,attack,defense,speed),basic,List.of(new SkillDefinition(basic,"공격",TargetRule.ENEMY_SINGLE,0,List.of(SkillEffect.damage(1.00)))));}
}
