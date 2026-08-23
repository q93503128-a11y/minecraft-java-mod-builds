from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / 'projects/arcane-circle'


def replace(path, old, new, count=1):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    found = s.count(old)
    if found != count:
        raise SystemExit(f'{path}: expected {count}, got {found}: {old[:120]!r}')
    p.write_text(s.replace(old, new, count), encoding='utf-8')

# 1) Growth capture follows the locked cast target/footprint instead of a fixed player-centred 72m sphere.
growth_path = 'projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java'
replace(growth_path,
'''import net.minecraft.world.phys.AABB;''',
'''import net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.Vec3;''')
replace(growth_path,
'''            "freezing_sphere", "delayed_blast_fireball", "fire_storm", "earthquake",\n            "incendiary_cloud", "prismatic_wall", "weird", "fire_shield", "wall_of_ice",''',
'''            "freezing_sphere", "delayed_blast_fireball", "fire_storm", "earthquake",\n            "incendiary_cloud", "control_weather", "prismatic_wall", "weird", "fire_shield", "wall_of_ice",''')
replace(growth_path,
'''            case "incendiary_cloud" -> 240;\n            case "prismatic_wall" -> 400;''',
'''            case "incendiary_cloud" -> 240;\n            case "control_weather" -> 900;\n            case "prismatic_wall" -> 400;''')
old_capture = '''    public static Snapshot capture(ServerPlayer player, double range) {\n        double radius = Math.min(72.0, Math.max(10.0, range + 7.0));\n        AABB box = player.getBoundingBox().inflate(radius, Math.max(8.0, radius * 0.45), radius);\n        List<Sample> samples = new ArrayList<>();\n        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob))) {\n            samples.add(new Sample(mob, mob.getHealth(), mob.getMaxHealth(), threatScore(mob)));\n        }\n        return new Snapshot(List.copyOf(samples));\n    }'''
new_capture = '''    public static Snapshot capture(ServerPlayer player, SpellDefinition spell, double range,\n                                   CastTargetSnapshot snapshot) {\n        Vec3 start = player.position();\n        Vec3 end = snapshot == null ? start : snapshot.target();\n        double footprint = growthFootprint(spell, range);\n        // Scan only the locked cast corridor plus the authored effect footprint. This covers remote\n        // fields/catastrophes without turning every low-circle cast into a huge player-centred query.\n        AABB box = new AABB(start, end).inflate(footprint, Math.max(8.0, footprint * .72), footprint);\n        List<Sample> samples = new ArrayList<>();\n        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob))) {\n            samples.add(new Sample(mob, mob.getHealth(), mob.getMaxHealth(), threatScore(mob)));\n        }\n        return new Snapshot(List.copyOf(samples));\n    }\n\n    private static double growthFootprint(SpellDefinition spell, double range) {\n        if (spell == null) return Math.max(10.0, Math.min(48.0, range * .30 + 8.0));\n        if ("meteor_swarm".equals(spell.id()))\n            return Math.min(192.0, NinthCircleMagnitude.meteorFieldRadius(range) + 10.0);\n        if ("control_weather".equals(spell.id()))\n            return Math.min(96.0, Math.max(40.0, range * .82 + 8.0));\n        SpellPresentationProfile.MotionStyle motion = SpellPresentationProfile.profile(spell).motion();\n        double authored = switch (motion) {\n            case SKY_DROP, STORM, FIELD, TARGET_BURST, PRISON ->\n                    SpellMetrics.effectRadius(spell.id(), range, spell.circle());\n            case WALL -> SpellMetrics.wallWidth(spell.id(), range, spell.circle()) * .50;\n            case WAVE -> SpellMetrics.waveEndRadius(spell.id(), range, spell.circle());\n            default -> 0.0;\n        };\n        return Math.max(8.0, Math.min(80.0, authored + 8.0));\n    }'''
replace(growth_path, old_capture, new_capture)

# 2) Capture after target locking. The old pre-target scan is replaced by an empty placeholder.
casting_path = 'projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java'
replace(casting_path,
'''        CombatGrowthService.Snapshot snapshot = CombatGrowthService.capture(player, cast.range());\n        releasePrelude(player, cast);\n        data.beginCast(player, cast);\n        data.startCooldown(player, spell.id(), ArcaneBuffRuntime.adjustCooldownTicks(player, cast.cooldownTicks()));\n        if (cast.fusion()) startFusionIngredientCooldowns(player, data, cast.ingredients());\n        SpellKineticsService.launch(player, cast, snapshot);''',
'''        releasePrelude(player, cast);\n        data.beginCast(player, cast);\n        data.startCooldown(player, spell.id(), ArcaneBuffRuntime.adjustCooldownTicks(player, cast.cooldownTicks()));\n        if (cast.fusion()) startFusionIngredientCooldowns(player, data, cast.ingredients());\n        SpellKineticsService.launch(player, cast, CombatGrowthService.Snapshot.EMPTY);''')

kinetics_path = 'projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java'
replace(kinetics_path,
'''        CastTargetSnapshot targetSnapshot = WorldMagicService.captureSnapshot(player, cast.spell(), cast.range());\n        if ("meteor_swarm".equals(cast.spell().id()))''',
'''        CastTargetSnapshot targetSnapshot = WorldMagicService.captureSnapshot(player, cast.spell(), cast.range());\n        growthSnapshot = CombatGrowthService.capture(player, cast.spell(), cast.range(), targetSnapshot);\n        if ("meteor_swarm".equals(cast.spell().id()))''')

# 3) Control Weather is a real 45s offensive authority, so both automatic and G-key lightning credit its owner.
gameplay_path = 'projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellGameplayService.java'
replace(gameplay_path,
'''                ArcaneDamage.hurt(level, owner, target, (float) (state.power() * .16));''',
'''                ArcaneDamage.hurtAttributed(level, owner, target, (float) (state.power() * .16), "control_weather");''')
replace(gameplay_path,
'''                    ArcaneDamage.hurt(level, owner, target, (float) (barrage.power() * .44));''',
'''                    ArcaneDamage.hurtAttributed(level, owner, target, (float) (barrage.power() * .44), "control_weather");''')

# 4) Metadata and hard source/JAR gates.
index_path = P / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
index = json.loads(index_path.read_text(encoding='utf-8'))
deferred = index['deferred_damage_attribution']
tracked = deferred['tracked_spells']
if 'control_weather' not in tracked:
    insert_at = tracked.index('prismatic_wall') if 'prismatic_wall' in tracked else len(tracked)
    tracked.insert(insert_at, 'control_weather')
deferred['growth_snapshot'] = 'locked_cast_corridor_plus_authoritative_spell_footprint'
deferred['meteor_cityfall_snapshot'] = 'target_center_plus_full_range_scaled_cityfall_radius'
deferred['control_weather_credit'] = '45s_automatic_and_g_key_lightning_server_attributed'
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

source_test = 'projects/arcane-circle/tools/test_current_source.py'
replace(source_test,
'''assert len(index['deferred_damage_attribution']['tracked_spells']) == 20''',
'''assert len(index['deferred_damage_attribution']['tracked_spells']) == 21\nassert index['deferred_damage_attribution']['growth_snapshot'] == 'locked_cast_corridor_plus_authoritative_spell_footprint'\nassert index['deferred_damage_attribution']['meteor_cityfall_snapshot'] == 'target_center_plus_full_range_scaled_cityfall_radius'\nassert index['deferred_damage_attribution']['control_weather_credit'] == '45s_automatic_and_g_key_lightning_server_attributed' ''')
replace(source_test,
'''need(growth, 'public static DeferredSettlement takeDeferred(', 'public static void startDeferred(',\n     'public static void recordAttributed(', 'public static List<DeferredSettlement> drainReady(',\n     'same-spell ledger before a recast')''',
'''need(growth, 'public static DeferredSettlement takeDeferred(', 'public static void startDeferred(',\n     'public static void recordAttributed(', 'public static List<DeferredSettlement> drainReady(',\n     'same-spell ledger before a recast',\n     'public static Snapshot capture(ServerPlayer player, SpellDefinition spell, double range,',\n     'NinthCircleMagnitude.meteorFieldRadius(range) + 10.0',\n     '"control_weather" -> 900')''')
replace(source_test,
'''need(text(magic / 'NinthCircleSpellService.java'), 'hurtAttributed(level, owner, target', '"prismatic_wall"', '"weird"')''',
'''need(text(magic / 'NinthCircleSpellService.java'), 'hurtAttributed(level, owner, target', '"prismatic_wall"', '"weird"')\nneed(text(magic / 'SpellKineticsService.java'),\n     'growthSnapshot = CombatGrowthService.capture(player, cast.spell(), cast.range(), targetSnapshot);')\nneed(text(magic / 'SpellGameplayService.java'),\n     'state.power() * .16), "control_weather"', 'barrage.power() * .44), "control_weather"')''')

jar_test = 'projects/arcane-circle/tools/verify_jar.py'
replace(jar_test,
'''    if len(deferred.get('tracked_spells', [])) != 20:\n        raise SystemExit('alpha.75 deferred spell coverage drift')''',
'''    if len(deferred.get('tracked_spells', [])) != 21:\n        raise SystemExit('alpha.75 deferred spell coverage drift')\n    if deferred.get('growth_snapshot') != 'locked_cast_corridor_plus_authoritative_spell_footprint':\n        raise SystemExit('alpha.75 wide growth snapshot contract missing')\n    if deferred.get('meteor_cityfall_snapshot') != 'target_center_plus_full_range_scaled_cityfall_radius':\n        raise SystemExit('alpha.75 meteor cityfall growth coverage missing')\n    if deferred.get('control_weather_credit') != '45s_automatic_and_g_key_lightning_server_attributed':\n        raise SystemExit('alpha.75 Control Weather deferred credit missing')''')

# Self-clean so canonical audit still sees exactly one Arcane workflow and exactly two project tools.
for rel in [
    '.github/arcane-alpha75-wide-credit.py',
    '.github/arcane-alpha75-wide-credit-trigger',
    '.github/workflows/arcane-alpha75-wide-credit.yml',
]:
    p = ROOT / rel
    if p.exists():
        p.unlink()
