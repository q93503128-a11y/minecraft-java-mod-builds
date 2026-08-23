from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
P=ROOT/'projects/arcane-circle'

def replace(path,old,new,count=1):
    p=ROOT/path
    s=p.read_text(encoding='utf-8')
    n=s.count(old)
    if n!=count: raise SystemExit(f'{path}: expected {count}, got {n}: {old[:100]!r}')
    p.write_text(s.replace(old,new,count),encoding='utf-8')

# Combat growth: exact server-authoritative deferred damage ledger with per-cast budgets.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',
'''import java.util.ArrayList;\nimport java.util.List;''',
'''import java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.Iterator;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Set;\nimport java.util.UUID;''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',
'''    private static final int MAX_MASTERY_PER_CAST = 30;\n    private static final int MAX_INSIGHT_PER_CAST = 8;''',
'''    private static final int MAX_MASTERY_PER_CAST = 30;\n    private static final int MAX_INSIGHT_PER_CAST = 8;\n    private static final Set<String> DEFERRED_DAMAGE_SPELLS = Set.of(\n            "scorching_ray", "sleet_storm", "wall_of_fire", "ice_storm", "blight",\n            "phantasmal_killer", "cloudkill", "flame_strike", "insect_plague", "sunbeam",\n            "freezing_sphere", "delayed_blast_fireball", "fire_storm", "earthquake",\n            "incendiary_cloud", "prismatic_wall", "weird", "fire_shield", "wall_of_ice",\n            "winter_domain");\n    private static final Map<DeferredKey, DeferredWindow> DEFERRED = new HashMap<>();''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',
'''    public record Impact(int hits, int kills, int strongHits, int strongKills, int damage, int masteryGain,\n                         int insightGain, int threatPoints, int peakThreat, long combatValue) {\n        public static final Impact NONE = new Impact(0, 0, 0, 0, 0, 0, 0, 0, 0, 0L);\n        public boolean meaningful() { return hits > 0 || kills > 0; }\n    }''',
'''    public record Impact(int hits, int kills, int strongHits, int strongKills, int damage, int masteryGain,\n                         int insightGain, int threatPoints, int peakThreat, long combatValue) {\n        public static final Impact NONE = new Impact(0, 0, 0, 0, 0, 0, 0, 0, 0, 0L);\n        public boolean meaningful() { return hits > 0 || kills > 0; }\n    }\n    public record DeferredSettlement(String spellId, int spellCircle, Impact impact) {}\n    private record DeferredKey(UUID playerId, String spellId) {}\n    private static final class AttributedTarget {\n        final int threat; double damage; boolean killed;\n        AttributedTarget(int threat) { this.threat = Math.max(1, threat); }\n    }\n    private static final class DeferredWindow {\n        final String spellId; final int spellCircle; final long expiresAt;\n        final int masteryBudget; final int insightBudget;\n        final Map<UUID, AttributedTarget> targets = new HashMap<>();\n        DeferredWindow(String spellId, int spellCircle, long expiresAt, int masteryBudget, int insightBudget) {\n            this.spellId=spellId; this.spellCircle=spellCircle; this.expiresAt=expiresAt;\n            this.masteryBudget=masteryBudget; this.insightBudget=insightBudget;\n        }\n    }''')
anchor='''        return new Snapshot(List.copyOf(samples));\n    }\n\n    public static Impact measure(Snapshot snapshot, int spellCircle) {'''
insert='''        return new Snapshot(List.copyOf(samples));\n    }\n\n    public static int deferredCreditTicks(String spellId) {\n        if (!DEFERRED_DAMAGE_SPELLS.contains(spellId)) return 0;\n        return switch (spellId) {\n            case "scorching_ray" -> 30;\n            case "sleet_storm" -> 180;\n            case "wall_of_fire" -> 240;\n            case "ice_storm" -> 120;\n            case "blight" -> 160;\n            case "phantasmal_killer" -> 280;\n            case "cloudkill" -> 220;\n            case "flame_strike" -> 80;\n            case "insect_plague" -> 220;\n            case "sunbeam" -> 120;\n            case "freezing_sphere" -> 200;\n            case "delayed_blast_fireball" -> 90;\n            case "fire_storm" -> 70;\n            case "earthquake" -> 180;\n            case "incendiary_cloud" -> 240;\n            case "prismatic_wall" -> 400;\n            case "weird" -> 300;\n            case "fire_shield" -> 620;\n            case "wall_of_ice" -> 220;\n            case "winter_domain" -> 240;\n            default -> 0;\n        };\n    }\n\n    /** Remove a previous same-spell ledger before a recast so the new opening hit cannot leak into it. */\n    public static DeferredSettlement takeDeferred(ServerPlayer player, String spellId) {\n        if (player == null || spellId == null) return null;\n        return settlement(DEFERRED.remove(new DeferredKey(player.getUUID(), spellId)));\n    }\n\n    /** Start after the synchronous cast result was measured; only later ArcaneDamage is credited here. */\n    public static void startDeferred(ServerPlayer player, String spellId, int spellCircle, Impact immediate) {\n        int ticks = deferredCreditTicks(spellId);\n        if (player == null || ticks <= 0) return;\n        Impact first = immediate == null ? Impact.NONE : immediate;\n        long now = ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime();\n        DeferredKey key = new DeferredKey(player.getUUID(), spellId);\n        DEFERRED.put(key, new DeferredWindow(spellId, Math.max(1, spellCircle), now + ticks,\n                Math.max(0, MAX_MASTERY_PER_CAST - Math.max(0, first.masteryGain())),\n                Math.max(0, MAX_INSIGHT_PER_CAST - Math.max(0, first.insightGain()))));\n    }\n\n    public static boolean attributableTarget(ServerPlayer player, Mob mob) {\n        return player != null && mob != null && validTarget(player, mob);\n    }\n\n    public static void recordAttributed(ServerPlayer player, String spellId, Mob mob, double actualDamage, boolean killed) {\n        if (player == null || mob == null || spellId == null || spellId.isBlank() || actualDamage <= .001) return;\n        DeferredWindow window = DEFERRED.get(new DeferredKey(player.getUUID(), spellId));\n        if (window == null || ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime() > window.expiresAt) return;\n        AttributedTarget target = window.targets.computeIfAbsent(mob.getUUID(), ignored -> new AttributedTarget(threatScore(mob)));\n        target.damage += actualDamage;\n        target.killed |= killed;\n    }\n\n    public static List<DeferredSettlement> drainReady(ServerPlayer player) {\n        if (player == null) return List.of();\n        long now = ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime();\n        List<DeferredSettlement> ready = new ArrayList<>();\n        Iterator<Map.Entry<DeferredKey, DeferredWindow>> iterator = DEFERRED.entrySet().iterator();\n        while (iterator.hasNext()) {\n            Map.Entry<DeferredKey, DeferredWindow> entry = iterator.next();\n            if (!entry.getKey().playerId().equals(player.getUUID()) || now < entry.getValue().expiresAt) continue;\n            DeferredSettlement value = settlement(entry.getValue());\n            iterator.remove();\n            if (value != null && value.impact().meaningful()) ready.add(value);\n        }\n        return List.copyOf(ready);\n    }\n\n    public static void clear(UUID playerId) {\n        if (playerId != null) DEFERRED.keySet().removeIf(key -> key.playerId().equals(playerId));\n    }\n\n    public static void clearAll() { DEFERRED.clear(); }\n\n    private static DeferredSettlement settlement(DeferredWindow window) {\n        if (window == null || window.targets.isEmpty()) return null;\n        int hits=0, kills=0, strongHits=0, strongKills=0, threatPoints=0, peakThreat=0;\n        long combatValue=0L; double damage=0.0, masteryScore=0.0, insightScore=0.0;\n        for (AttributedTarget target : window.targets.values()) {\n            if (target.damage <= .001) continue;\n            hits++; damage += target.damage;\n            int threat=Math.max(1,target.threat), tier=threatTier(threat);\n            peakThreat=Math.max(peakThreat,threat);\n            if (tier>0) strongHits++;\n            if (target.killed) { kills++; if (tier>0) strongKills++; }\n            threatPoints=Math.min(2_000_000, threatPoints + (target.killed ? threat : Math.max(1, threat/8)));\n            double threatLog=Math.log1p(threat);\n            masteryScore += .65 + Math.min(2.4, threatLog*.42) + Math.min(1.6, Math.sqrt(target.damage)*.12);\n            if (target.killed) masteryScore += .8 + Math.min(4.0, threatLog*.72);\n            if (tier>0) insightScore += .20 + tier*.18;\n            if (target.killed) insightScore += tier>0 ? .45+tier*.25 : .22;\n            long hitValue=Math.max(1L,Math.round(Math.sqrt(threat)*.45));\n            long killValue=target.killed ? Math.max(1L,Math.round(Math.pow(threat,1.12)*.24)) : 0L;\n            combatValue=Math.min(2_000_000_000L,combatValue+hitValue+killValue);\n        }\n        if (hits==0) return null;\n        int mastery=Math.min(window.masteryBudget, Math.min(MAX_MASTERY_PER_CAST, Math.max(1,(int)Math.round(masteryScore))));\n        int insight=Math.min(window.insightBudget, Math.min(MAX_INSIGHT_PER_CAST, Math.max(0,(int)Math.floor(insightScore))));\n        if (kills>0 && insight==0 && window.insightBudget>0) insight=1;\n        return new DeferredSettlement(window.spellId, window.spellCircle,\n                new Impact(hits,kills,strongHits,strongKills,(int)Math.round(damage),mastery,insight,\n                        threatPoints,peakThreat,combatValue));\n    }\n\n    public static Impact measure(Snapshot snapshot, int spellCircle) {'''
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',anchor,insert)

# Damage routing: every player cast uses playerAttack even through LivingEntity service APIs; delayed pulses can be attributed exactly.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneDamage.java',
'''    public static boolean hurt(ServerLevel level, LivingEntity caster, LivingEntity target, float amount) {\n        if (target == null || !target.isAlive() || amount <= 0.0F) return false;''',
'''    public static boolean hurt(ServerLevel level, LivingEntity caster, LivingEntity target, float amount) {\n        if (caster instanceof ServerPlayer player) return hurt(level, player, target, amount);\n        if (target == null || !target.isAlive() || amount <= 0.0F) return false;''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneDamage.java',
'''        if (damaged && target instanceof Mob mob && target != caster) mob.setTarget(caster);\n        return damaged;\n    }\n}''',
'''        if (damaged && target instanceof Mob mob && target != caster) mob.setTarget(caster);\n        return damaged;\n    }\n\n    /** Records only server-authoritative post-release damage for an already-open deferred spell ledger. */\n    public static boolean hurtAttributed(ServerLevel level, LivingEntity caster, LivingEntity target,\n                                         float amount, String spellId) {\n        ServerPlayer player = caster instanceof ServerPlayer value ? value : null;\n        Mob mob = target instanceof Mob value ? value : null;\n        boolean track = player != null && mob != null && CombatGrowthService.attributableTarget(player, mob);\n        float before = target == null ? 0.0F : target.getHealth() + target.getAbsorptionAmount();\n        boolean damaged = hurt(level, caster, target, amount);\n        if (track && damaged) {\n            float after = target.isAlive() && !target.isRemoved()\n                    ? Math.max(0.0F, target.getHealth() + target.getAbsorptionAmount()) : 0.0F;\n            double actual = Math.max(0.0, before - after);\n            if (actual > .001) CombatGrowthService.recordAttributed(player, spellId, mob, actual,\n                    !target.isAlive() || target.isRemoved() || after <= .001F);\n        }\n        return damaged;\n    }\n}''',1)

# Player state can apply delayed combat without granting a second successful-use floor.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java',
'''    public static int masteryGainFor(CombatGrowthService.Impact impact) {\n        CombatGrowthService.Impact result = impact == null ? CombatGrowthService.Impact.NONE : impact;\n        return result.meaningful() ? Math.max(1, result.masteryGain()) : 1;\n    }''',
'''    public static int masteryGainFor(CombatGrowthService.Impact impact) {\n        CombatGrowthService.Impact result = impact == null ? CombatGrowthService.Impact.NONE : impact;\n        return result.meaningful() ? Math.max(1, result.masteryGain()) : 1;\n    }\n\n    public CastProgress awardDeferredCombat(ServerPlayer player, String spellId, CombatGrowthService.Impact impact) {\n        CombatGrowthService.Impact result = impact == null ? CombatGrowthService.Impact.NONE : impact;\n        if (!result.meaningful()) return new CastProgress(new CircleAdvance(state(player).circle, state(player).circle), MasteryProgress.none());\n        MageState state = state(player);\n        int before = state.mastery.getOrDefault(spellId, 0);\n        int after = Math.min(100000, before + Math.max(0, result.masteryGain()));\n        if (after != before) state.mastery.put(spellId, after);\n        state.insight = Math.min(1_000_000, state.insight + Math.max(0, result.insightGain()));\n        int previousCircle = state.circle;\n        if (state.circle < SpellCatalog.IMPLEMENTED_MAX_CIRCLE\n                && state.insight >= SpellCatalog.circleInsightThreshold(state.circle + 1)) state.circle++;\n        if (state.circle > previousCircle) state.mana = effectiveStats(player).maxMana();\n        MasteryProgress mastery = MasteryProgress.none();\n        if (SpellCatalog.fusion(spellId).isPresent()) {\n            int required = SpellCatalog.masteryRequired(spellId);\n            boolean registered = after >= required && state.known.add(spellId);\n            if (registered) equipIntoFirstEmptySlot(state, spellId);\n            mastery = new MasteryProgress(true, registered, spellId, after, required);\n        }\n        setDirty();\n        return new CastProgress(new CircleAdvance(previousCircle, state.circle), mastery);\n    }''')

# Spell lifecycle: settle old same-spell window before recast, open new one after synchronous impact, drain at server tick.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java',
'''        CombatGrowthService.Snapshot snapshot = CombatGrowthService.capture(player, cast.range());''',
'''        applyDeferredSettlement(player, CombatGrowthService.takeDeferred(player, spell.id()));\n        CombatGrowthService.Snapshot snapshot = CombatGrowthService.capture(player, cast.range());''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java',
'''        MagicPlayerData.CastProgress progress = data.completeCastProgress(player, cast, impact);\n        ArcaneQuestData.get(((ServerLevel) player.level()).getServer())\n                .recordCast(player, impact, spell.circle(), cast.fusion());''',
'''        MagicPlayerData.CastProgress progress = data.completeCastProgress(player, cast, impact);\n        CombatGrowthService.startDeferred(player, spell.id(), spell.circle(), impact);\n        ArcaneQuestData.get(((ServerLevel) player.level()).getServer())\n                .recordCast(player, impact, spell.circle(), cast.fusion());''')
insert_before='''    private static boolean canExecute(ServerPlayer player, String id, double range) {'''
insert_methods='''    public static void tickDeferredCombat(ServerPlayer player) {\n        for (CombatGrowthService.DeferredSettlement settlement : CombatGrowthService.drainReady(player))\n            applyDeferredSettlement(player, settlement);\n    }\n\n    private static void applyDeferredSettlement(ServerPlayer player, CombatGrowthService.DeferredSettlement settlement) {\n        if (player == null || settlement == null || settlement.impact() == null || !settlement.impact().meaningful()) return;\n        CombatGrowthService.Impact impact = settlement.impact();\n        MagicPlayerData data = data(player);\n        long marksEarned = kr.moonseungjun.arcanecircle.world.ArcaneEconomyService\n                .awardCombat(player, impact, settlement.spellCircle());\n        MagicPlayerData.CastProgress progress = data.awardDeferredCombat(player, settlement.spellId(), impact);\n        ArcaneQuestData.get(((ServerLevel) player.level()).getServer()).recordCombatImpact(player, impact);\n        String name = SpellCatalog.spell(settlement.spellId()).map(SpellDefinition::name).orElse(settlement.spellId());\n        player.sendSystemMessage(Component.literal("§5[지속 주문 정산] §f" + name + " §7· 적중 " + impact.hits()\n                + " · 처치 " + impact.kills() + " · 피해 " + impact.damage() + " · 숙련 +" + impact.masteryGain()\n                + " · 통찰 +" + impact.insightGain() + " · 아르카나 +" + marksEarned));\n        ServerLevel level = (ServerLevel) player.level();\n        if (progress.mastery().registered()) {\n            player.sendSystemMessage(Component.literal("§6[융합 각인] §f" + name\n                    + "의 지속 전투 회로까지 완성되어 마력핵에 각인되었습니다."));\n            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);\n        }\n        if (progress.circle().advanced()) {\n            player.sendSystemMessage(Component.literal("§d[써클 승급] §f지속 주문의 전투 통찰로 마력핵이 §5"\n                    + progress.circle().current() + "써클§f로 확장되었습니다."));\n            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, .8F);\n        }\n    }\n\n    private static boolean canExecute(ServerPlayer player, String id, double range) {'''
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java',insert_before,insert_methods)

# Delayed combat contributes only combat metrics to quests; it never fakes another cast/fusion completion.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneQuestData.java',
'''    private List<PlayerEntry> entries(){java.util.Set<String> keys=new java.util.HashSet<>();''',
'''    public void recordCombatImpact(ServerPlayer player, CombatGrowthService.Impact impact) {\n        List<Quest> list=active.get(key(player)); if(list==null||list.isEmpty()||impact==null||!impact.meaningful())return; boolean changed=false;\n        for(Quest q:list){ if(q.progress>=q.target)continue;\n            int delta=switch(q.id){case "hits"->Math.max(0,impact.hits());case "kills"->Math.max(0,impact.kills());\n                case "damage"->Math.max(0,impact.damage());case "threat"->Math.max(0,impact.threatPoints());default->0;};\n            if(delta<=0)continue;int before=q.progress;q.progress=Math.min(q.target,q.progress+delta);changed|=before!=q.progress;\n            if(before<q.target&&q.progress>=q.target)ArcaneNoticeService.push(player,Component.literal("§6[의뢰 완료] §f"\n                    +difficultyName(q.difficulty)+" · "+description(q.id)+" §7· 보상 §6"+q.reward+" 아르카나"),140);\n        }if(changed)setDirty();\n    }\n\n    private List<PlayerEntry> entries(){java.util.Set<String> keys=new java.util.HashSet<>();''')

# Lifecycle integration.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
'''import kr.moonseungjun.arcanecircle.magic.DestructiveMagicService;''',
'''import kr.moonseungjun.arcanecircle.magic.DestructiveMagicService;\nimport kr.moonseungjun.arcanecircle.magic.CombatGrowthService;''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
'''        SpellGameplayService.clear(player);\n        WorldMagicService.stop(player);''',
'''        SpellGameplayService.clear(player);\n        CombatGrowthService.clear(player.getUUID());\n        WorldMagicService.stop(player);''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
'''        ArcaneFieldService.tick(level);\n        MagicPlayerData data = MagicPlayerData.get(level.getServer());''',
'''        ArcaneFieldService.tick(level);\n        SpellCastingService.tickDeferredCombat(player);\n        MagicPlayerData data = MagicPlayerData.get(level.getServer());''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
'''        ArcaneFieldService.clearAll();\n        DestructiveMagicService.clearAll();''',
'''        ArcaneFieldService.clearAll();\n        CombatGrowthService.clearAll();\n        DestructiveMagicService.clearAll();''')

# Persistent/direct spell pulse attribution.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SecondCircleSpellService.java',
        'boolean hit = ArcaneDamage.hurt(level, caster, target, (float) power);',
        'boolean hit = ArcaneDamage.hurtAttributed(level, caster, target, (float) power, "scorching_ray");')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.java',
        'ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, zone.power * .055));',
        'ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.5, zone.power * .055), "sleet_storm");')
for old,new in [
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .055));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.5, state.power * .055), "wall_of_fire");'),
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(.45, state.power * .10));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.45, state.power * .10), "ice_storm");'),
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .18));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.5, state.power * .18), "blight");'),
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .10));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.5, state.power * .10), "phantasmal_killer");')]:
    replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.java',old,new)
for old,new in [
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(.6, state.power * .065 * executePressure));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.6, state.power * .065 * executePressure), "cloudkill");'),
('if (ArcaneDamage.hurt(level, caster, target, (float) (pulsePower * falloff))) hit = true;','if (ArcaneDamage.hurtAttributed(level, caster, target, (float) (pulsePower * falloff), "flame_strike")) hit = true;'),
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(.5, state.power * .05));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(.5, state.power * .05), "insect_plague");')]:
    replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/FifthCircleSpellService.java',old,new)

# Sixth circle: Sunbeam uses an attributed overload of the shared line helper; Freezing Sphere pulses are direct.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.java',
'''        lineDamage(field.level, caster, field.start, field.end, sunbeamHalfWidth(), pulsePower, target -> {''',
'''        lineDamage(field.level, caster, field.start, field.end, sunbeamHalfWidth(), pulsePower, "sunbeam", target -> {''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.java',
'''    private static boolean lineDamage(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end,\n                                      double halfWidth, double power,\n                                      java.util.function.Consumer<LivingEntity> afterHit) {\n        Vec3 delta = end.subtract(start);''',
'''    private static boolean lineDamage(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end,\n                                      double halfWidth, double power,\n                                      java.util.function.Consumer<LivingEntity> afterHit) {\n        return lineDamage(level, caster, start, end, halfWidth, power, null, afterHit);\n    }\n\n    private static boolean lineDamage(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end,\n                                      double halfWidth, double power, String spellId,\n                                      java.util.function.Consumer<LivingEntity> afterHit) {\n        Vec3 delta = end.subtract(start);''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.java',
'''            if (ArcaneDamage.hurt(level, caster, target, (float) power)) hit = true;\n            afterHit.accept(target);''',
'''            boolean damaged = spellId == null\n                    ? ArcaneDamage.hurt(level, caster, target, (float) power)\n                    : ArcaneDamage.hurtAttributed(level, caster, target, (float) power, spellId);\n            if (damaged) hit = true;\n            afterHit.accept(target);''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SixthCircleSpellService.java',
        'ArcaneDamage.hurt(field.level, caster, target, (float) Math.max(.5, pulsePower * falloff));',
        'ArcaneDamage.hurtAttributed(field.level, caster, target, (float) Math.max(.5, pulsePower * falloff), "freezing_sphere");')

for old,new in [
('ArcaneDamage.hurt(level, caster, target, damage);','ArcaneDamage.hurtAttributed(level, caster, target, damage, "delayed_blast_fireball");'),
('ArcaneDamage.hurt(field.level, caster, target, (float) (field.power * multiplier * falloff));','ArcaneDamage.hurtAttributed(field.level, caster, target, (float) (field.power * multiplier * falloff), "fire_storm");')]:
    replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SeventhCircleSpellService.java',old,new)
for old,new in [
('ArcaneDamage.hurt(level, caster, target, (float) (pulsePower * falloff));','ArcaneDamage.hurtAttributed(level, caster, target, (float) (pulsePower * falloff), "earthquake");'),
('ArcaneDamage.hurt(level, caster, target, (float) (pulsePower * falloff));','ArcaneDamage.hurtAttributed(level, caster, target, (float) (pulsePower * falloff), "incendiary_cloud");'),
('ArcaneDamage.hurt(level, caster, target, (float) (field.power * .035 * falloff));','ArcaneDamage.hurtAttributed(level, caster, target, (float) (field.power * .035 * falloff), "incendiary_cloud");')]:
    # the first two identical source strings occur in different methods; consume one at a time in source order
    replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/EighthCircleSpellService.java',old,new)

# Ninth-circle wall crossings and Weird pulses.
for old,new in [
('ArcaneDamage.hurt(level, owner, target, (float) (power * .20));','ArcaneDamage.hurtAttributed(level, owner, target, (float) (power * .20), "prismatic_wall");'),
('ArcaneDamage.hurt(level, owner, target, (float) (power * .18));','ArcaneDamage.hurtAttributed(level, owner, target, (float) (power * .18), "prismatic_wall");'),
('ArcaneDamage.hurt(level, owner, target, (float) (power * .22));','ArcaneDamage.hurtAttributed(level, owner, target, (float) (power * .22), "prismatic_wall");'),
('ArcaneDamage.hurt(level, owner, target, (float) (power * .28));','ArcaneDamage.hurtAttributed(level, owner, target, (float) (power * .28), "prismatic_wall");'),
('ArcaneDamage.hurt(level, owner, target, (float) Math.max(1.0, state.power * .075));','ArcaneDamage.hurtAttributed(level, owner, target, (float) Math.max(1.0, state.power * .075), "weird");')]:
    replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/NinthCircleSpellService.java',old,new)

# Fusion maintained damage attribution.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellGameplayService.java',
        'ArcaneDamage.hurt((ServerLevel) player.level(), player, attacker, (float) Math.max(2.0, shield.power() * .26));',
        'ArcaneDamage.hurtAttributed((ServerLevel) player.level(), player, attacker, (float) Math.max(2.0, shield.power() * .26), "fire_shield");')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellGameplayService.java',
        'case "winter_domain" -> { ArcaneDamage.hurt(level, owner, target, (float) (zone.power * .11));',
        'case "winter_domain" -> { ArcaneDamage.hurtAttributed(level, owner, target, (float) (zone.power * .11), zone.spellId);')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellGameplayService.java',
        'case "wall_of_ice" -> { ArcaneDamage.hurt(zone.level, owner, target, (float) (zone.power * .10));',
        'case "wall_of_ice" -> { ArcaneDamage.hurtAttributed(zone.level, owner, target, (float) (zone.power * .10), zone.spellId);')

# Metadata + verification contract.
idx=P/'src/main/resources/data/arcanecircle/spell_catalog/index.json'
data=json.loads(idx.read_text(encoding='utf-8'))
data['deferred_damage_attribution']={
    'mode':'server_authoritative_arcane_damage_ledger',
    'initial_damage':'synchronous_snapshot_only_no_double_count',
    'same_spell_recast':'previous_ledger_settled_before_new_release',
    'per_cast_mastery_cap':30,
    'per_cast_insight_cap':8,
    'quest_combat_credit':True,
    'player_damage_source':'player_attack_even_through_living_entity_runtime',
    'tracked_spells':['scorching_ray','sleet_storm','wall_of_fire','ice_storm','blight','phantasmal_killer','cloudkill','flame_strike','insect_plague','sunbeam','freezing_sphere','delayed_blast_fireball','fire_storm','earthquake','incendiary_cloud','prismatic_wall','weird','fire_shield','wall_of_ice','winter_domain']
}
idx.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

test=P/'tools/test_current_source.py'; s=test.read_text(encoding='utf-8')
anchor="assert index['global_curve_preserved'] == {"
pos=s.find(anchor)
if pos<0: raise SystemExit('test global curve anchor missing')
# Insert checks before global curve assertion to keep existing dict untouched.
checks='''assert index['deferred_damage_attribution']['mode'] == 'server_authoritative_arcane_damage_ledger'\nassert index['deferred_damage_attribution']['per_cast_mastery_cap'] == 30\nassert index['deferred_damage_attribution']['per_cast_insight_cap'] == 8\nassert len(index['deferred_damage_attribution']['tracked_spells']) == 20\nneed(text(magic / 'ArcaneDamage.java'),\n     'if (caster instanceof ServerPlayer player) return hurt(level, player, target, amount);',\n     'public static boolean hurtAttributed(', 'CombatGrowthService.recordAttributed(')\nneed(growth, 'public static DeferredSettlement takeDeferred(', 'public static void startDeferred(',\n     'public static void recordAttributed(', 'public static List<DeferredSettlement> drainReady(',\n     'same-spell ledger before a recast')\nneed(casting, 'applyDeferredSettlement(player, CombatGrowthService.takeDeferred(player, spell.id()));',\n     'CombatGrowthService.startDeferred(player, spell.id(), spell.circle(), impact);',\n     'public static void tickDeferredCombat(ServerPlayer player)', '§5[지속 주문 정산]')\nneed(text(world / 'ArcaneQuestData.java'), 'public void recordCombatImpact(ServerPlayer player, CombatGrowthService.Impact impact)')\nneed(main, 'CombatGrowthService.clear(player.getUUID());', 'SpellCastingService.tickDeferredCombat(player);', 'CombatGrowthService.clearAll();')\nneed(text(magic / 'SeventhCircleSpellService.java'), '"delayed_blast_fireball"', 'hurtAttributed(level, caster, target, damage')\nneed(text(magic / 'NinthCircleSpellService.java'), 'hurtAttributed(level, owner, target', '"prismatic_wall"', '"weird"')\n'''
s=s[:pos]+checks+s[pos:]
s=s.replace("print('alpha75_global_curve_preserved=PASS')", "print('alpha75_global_curve_preserved=PASS')\nprint('alpha75_deferred_damage_exact_attribution=PASS')\nprint('alpha75_player_attack_damage_source=PASS')",1)
test.write_text(s,encoding='utf-8')

verify=P/'tools/verify_jar.py'; v=verify.read_text(encoding='utf-8')
needle="    expected1 = {"
pos=v.find(needle)
if pos<0: raise SystemExit('jar expected1 anchor missing')
jarcheck='''    deferred = index.get('deferred_damage_attribution', {})\n    if deferred.get('mode') != 'server_authoritative_arcane_damage_ledger':\n        raise SystemExit('alpha.75 deferred damage attribution metadata missing')\n    if deferred.get('per_cast_mastery_cap') != 30 or deferred.get('per_cast_insight_cap') != 8:\n        raise SystemExit('alpha.75 deferred combat cap drift')\n    if len(deferred.get('tracked_spells', [])) != 20:\n        raise SystemExit('alpha.75 deferred spell coverage drift')\n\n'''
v=v[:pos]+jarcheck+v[pos:]
v=v.replace("print('alpha75_global_curve_preserved=PASS')", "print('alpha75_global_curve_preserved=PASS')\nprint('alpha75_deferred_damage_exact_attribution=PASS')\nprint('alpha75_player_attack_damage_source=PASS')",1)
verify.write_text(v,encoding='utf-8')

# Canonical trigger comment will be updated after manual source inspection.
print('alpha.75 deferred credit patch staged')
