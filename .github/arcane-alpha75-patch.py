from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / 'projects/arcane-circle'


def replace(path, old, new, count=1):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    actual = s.count(old)
    if actual != count:
        raise SystemExit(f'{path}: expected {count} matches, got {actual} for {old[:80]!r}')
    p.write_text(s.replace(old, new, count), encoding='utf-8')

# Version.
replace('projects/arcane-circle/gradle.properties',
        'mod_version=0.12.1-alpha.74', 'mod_version=0.12.1-alpha.75')
replace('projects/arcane-circle/gradle.properties',
        '# alpha.74 first-circle authority audit: NPC ward/light/sleep parity, exact grease/sleep footprints, bounded dispel lifecycle; canonical verification run 2',
        '# alpha.75 integrated progression audit: successful-use mastery floor, hostile-combat insight/economy, server purchase circle gate, high-circle spellbook rarity')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
        'VERSION = "0.12.1-alpha.74"', 'VERSION = "0.12.1-alpha.75"')

# Every successfully resolved spell can actually be mastered. Combat remains the fast path and the sole insight source.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java',
'''        int beforeMastery = state.mastery.getOrDefault(cast.spell().id(), 0);\n        int masteryGain = result.meaningful() ? Math.max(1, result.masteryGain()) : 0;\n        int afterMastery = Math.min(100000, beforeMastery + masteryGain);\n        if (masteryGain > 0) state.mastery.put(cast.spell().id(), afterMastery);\n        // Circle insight comes from meaningful combat, not from merely pressing a spell key.\n        if (result.meaningful()) state.insight = Math.min(1_000_000,\n                state.insight + Math.max(0, result.insightGain()));''',
'''        int beforeMastery = state.mastery.getOrDefault(cast.spell().id(), 0);\n        int masteryGain = masteryGainFor(result);\n        int afterMastery = Math.min(100000, beforeMastery + masteryGain);\n        state.mastery.put(cast.spell().id(), afterMastery);\n        // Every successfully resolved spell earns one practice point. Real hostile combat is the\n        // accelerated mastery path and remains the only source of circle insight.\n        if (result.meaningful()) state.insight = Math.min(1_000_000,\n                state.insight + Math.max(0, result.insightGain()));''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java',
'''    public CastProgress completeCast(ServerPlayer player, CastPreparation cast,\n                                     CombatGrowthService.Impact impact) {\n        beginCast(player, cast);\n        return completeCastProgress(player, cast, impact);\n    }''',
'''    public static int masteryGainFor(CombatGrowthService.Impact impact) {\n        CombatGrowthService.Impact result = impact == null ? CombatGrowthService.Impact.NONE : impact;\n        return result.meaningful() ? Math.max(1, result.masteryGain()) : 1;\n    }\n\n    public CastProgress completeCast(ServerPlayer player, CastPreparation cast,\n                                     CombatGrowthService.Impact impact) {\n        beginCast(player, cast);\n        return completeCastProgress(player, cast, impact);\n    }''')

# Show the same authoritative mastery gain on successful non-fusion cast completion.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java',
'''        MagicPlayerData.MageState state = data.state(player);\n        MagicPlayerData.EffectiveStats stats = data.effectiveStats(player);\n\n        if (cast.fusion() && progress.mastery().changed()) {''',
'''        MagicPlayerData.MageState state = data.state(player);\n        MagicPlayerData.EffectiveStats stats = data.effectiveStats(player);\n        int masteryGain = MagicPlayerData.masteryGainFor(impact);\n\n        if (cast.fusion() && progress.mastery().changed()) {''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java',
'''                    + (cast.cooldownTicks() <= 0 ? "없음" : String.format("%.1f",\n                    ArcaneBuffRuntime.adjustCooldownTicks(player, cast.cooldownTicks()) / 20.0) + "초")));''',
'''                    + (cast.cooldownTicks() <= 0 ? "없음" : String.format("%.1f",\n                    ArcaneBuffRuntime.adjustCooldownTicks(player, cast.cooldownTicks()) / 20.0) + "초")\n                    + " · 숙련 +" + masteryGain));''')

# Passive livestock are not combat progression/economy targets. Hostiles, Arcane mages and mobs actively fighting the player are.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',
'''    private static boolean validTarget(ServerPlayer player, Mob mob) {\n        if (!mob.isAlive() || mob.isRemoved()) return false;\n        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;\n        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);\n    }''',
'''    private static boolean validTarget(ServerPlayer player, Mob mob) {\n        if (!mob.isAlive() || mob.isRemoved()) return false;\n        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;\n        if (player.isAlliedTo(mob)) return false;\n        return mob instanceof Enemy || ArcaneMageService.isMage(mob) || mob.getTarget() == player;\n    }''')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',
''' * Measures real health changes caused during one cast window. Progress is intentionally\n * logarithmic and tightly capped so one weak mob or a large area spell cannot skip circles.''',
''' * Measures real health changes caused to actual combatants during one cast window. Passive\n * livestock cannot feed insight/economy; progress stays logarithmic and tightly capped so one\n * weak mob or a large area spell cannot skip circles.''')

# Server-authoritative academy tier gate; client filtering is never trusted for future-circle equipment.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java',
        'import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;\n',
        'import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;\nimport kr.moonseungjun.arcanecircle.magic.MagicPlayerData;\n')
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java',
'''        long price = priceFor(player, offer);\n        ArcaneWorldData world = data(player);''',
'''        int mageCircle = MagicPlayerData.get(((ServerLevel) player.level()).getServer()).state(player).circle();\n        if (offer.circle() > mageCircle) {\n            ArcaneNoticeService.push(player, Component.literal("§c[학원 상점] §f" + offer.circle()\n                    + "써클 거래는 현재 " + mageCircle + "써클 마력핵으로 구매할 수 없습니다."));\n            return false;\n        }\n        long price = priceFor(player, offer);\n        ArcaneWorldData world = data(player);''')

# 6~9C spellbooks were accidentally falling through to COMMON rarity.
replace('projects/arcane-circle/src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java',
'''                case 4, 5 -> Rarity.EPIC;\n                default -> Rarity.COMMON;''',
'''                case 4, 5, 6, 7, 8, 9 -> Rarity.EPIC;\n                default -> Rarity.COMMON;''')

# Canonical catalogue metadata.
index_path = P / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
index = json.loads(index_path.read_text(encoding='utf-8'))
index['version'] = '0.12.1-alpha.75'
index['global_progression_audit'] = {
    'successful_cast_mastery_floor': 1,
    'combat_mastery_acceleration': 'existing_hostile_damage_and_kill_score_up_to_30_per_cast',
    'circle_insight_source': 'hostile_combat_only',
    'passive_livestock_progression': 'blocked',
    'combat_arcana_source': 'same_hostile_combat_snapshot_as_insight',
    'fusion_registration': 'non_damage_and_maintained_fusions_are_reachable_by_successful_use',
    'academy_purchase_gate': 'server_authoritative_current_circle_or_lower',
    'high_circle_spellbook_rarity': 'circle_4_to_9_epic'
}
index['global_curve_preserved'] = {
    'same_circle_cast_ticks': [6, 10, 16, 26, 42, 68, 105, 155, 220],
    'base_max_mana': [100, 180, 300, 480, 750, 1150, 1800, 2800, 4500],
    'equipment_mana_cost_floor': 0.10,
    'equipment_cooldown_floor': 0.10,
    'progression_layers_after_equipment_floor': ['circle_gap', 'mastery', 'tradition'],
    'zero_cooldown_below_ticks': 2,
    'zero_cast_below_ticks': 1
}
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Source verifier alpha.75 + regression anchors.
test_path = P / 'tools/test_current_source.py'
t = test_path.read_text(encoding='utf-8')
t = t.replace("need(gradle, 'mod_version=0.12.1-alpha.74')", "need(gradle, 'mod_version=0.12.1-alpha.75')", 1)
t = t.replace("need(main, 'VERSION = \"0.12.1-alpha.74\"')", "need(main, 'VERSION = \"0.12.1-alpha.75\"')", 1)
t = t.replace("assert index['version'] == '0.12.1-alpha.74'", "assert index['version'] == '0.12.1-alpha.75'", 1)
anchor = "assert set(re.findall(r'case \"([a-z0-9_]+)\"', summary)) == spells\n"
insert = r'''

# Alpha.75 integrated progression / economy authority pass.
player_data = text(magic / 'MagicPlayerData.java')
growth = text(magic / 'CombatGrowthService.java')
casting = text(magic / 'SpellCastingService.java')
economy = text(world / 'ArcaneEconomyService.java')
items = text(root / 'src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java')
need(player_data,
     'int masteryGain = masteryGainFor(result);',
     'return result.meaningful() ? Math.max(1, result.masteryGain()) : 1;',
     'Every successfully resolved spell earns one practice point',
     'remains the only source of circle insight')
need(casting, 'int masteryGain = MagicPlayerData.masteryGainFor(impact);', '" · 숙련 +" + masteryGain')
need(growth,
     'mob instanceof Enemy || ArcaneMageService.isMage(mob) || mob.getTarget() == player',
     'Passive livestock cannot feed insight/economy')
need(economy,
     'int mageCircle = MagicPlayerData.get(', 'if (offer.circle() > mageCircle)',
     '써클 거래는 현재', '써클 마력핵으로 구매할 수 없습니다.')
need(items, 'case 4, 5, 6, 7, 8, 9 -> Rarity.EPIC;')
assert index['global_progression_audit'] == {
    'successful_cast_mastery_floor': 1,
    'combat_mastery_acceleration': 'existing_hostile_damage_and_kill_score_up_to_30_per_cast',
    'circle_insight_source': 'hostile_combat_only',
    'passive_livestock_progression': 'blocked',
    'combat_arcana_source': 'same_hostile_combat_snapshot_as_insight',
    'fusion_registration': 'non_damage_and_maintained_fusions_are_reachable_by_successful_use',
    'academy_purchase_gate': 'server_authoritative_current_circle_or_lower',
    'high_circle_spellbook_rarity': 'circle_4_to_9_epic',
}
assert index['global_curve_preserved'] == {
    'same_circle_cast_ticks': [6, 10, 16, 26, 42, 68, 105, 155, 220],
    'base_max_mana': [100, 180, 300, 480, 750, 1150, 1800, 2800, 4500],
    'equipment_mana_cost_floor': 0.10,
    'equipment_cooldown_floor': 0.10,
    'progression_layers_after_equipment_floor': ['circle_gap', 'mastery', 'tradition'],
    'zero_cooldown_below_ticks': 2,
    'zero_cast_below_ticks': 1,
}
need(player_data,
     'double circleMana = Math.pow(0.72, masteryGap);',
     'double circleCooldown = Math.pow(0.62, masteryGap);',
     'double equipmentCostMultiplier = Math.max(0.10,',
     'double equipmentCooldownMultiplier = Math.max(0.10,',
     'rawCooldown < 2.0')
need(text(magic / 'SpellCastingService.java'),
     'int[] sameCircleTicks = {0, 6, 10, 16, 26, 42, 68, 105, 155, 220};',
     'return raw < 1.0 ? 0 : Math.max(1, (int) Math.round(raw));')
'''
if anchor not in t:
    raise SystemExit('test verifier insertion anchor missing')
t = t.replace(anchor, anchor + insert, 1)
t = t.replace("     '0.12.1-alpha.74', 'FirstCircleAuthorityOverlay.class'", "     '0.12.1-alpha.75', 'FirstCircleAuthorityOverlay.class'", 1)
t = t.replace("print('all_109_explicit_effect_summaries=PASS')", "print('all_109_explicit_effect_summaries=PASS')\nprint('alpha75_successful_use_mastery_floor=PASS')\nprint('alpha75_hostile_only_insight_economy=PASS')\nprint('alpha75_academy_server_circle_gate=PASS')\nprint('alpha75_global_curve_preserved=PASS')", 1)
test_path.write_text(t, encoding='utf-8')

# JAR verifier alpha.75.
verify_path = P / 'tools/verify_jar.py'
v = verify_path.read_text(encoding='utf-8')
v = v.replace("if version != '0.12.1-alpha.74':\n        raise SystemExit(f'unexpected alpha.74 package version: {version}')",
              "if version != '0.12.1-alpha.75':\n        raise SystemExit(f'unexpected alpha.75 package version: {version}')", 1)
jar_anchor = "    for c in ('first','second','third','fourth','fifth','sixth','seventh','eighth','ninth'):\n        if index.get(f'{c}_circle_npc_parity') is not True:\n            raise SystemExit(f'{c} NPC parity metadata missing')\n"
jar_insert = r'''

    expected_global = {
        'successful_cast_mastery_floor': 1,
        'combat_mastery_acceleration': 'existing_hostile_damage_and_kill_score_up_to_30_per_cast',
        'circle_insight_source': 'hostile_combat_only',
        'passive_livestock_progression': 'blocked',
        'combat_arcana_source': 'same_hostile_combat_snapshot_as_insight',
        'fusion_registration': 'non_damage_and_maintained_fusions_are_reachable_by_successful_use',
        'academy_purchase_gate': 'server_authoritative_current_circle_or_lower',
        'high_circle_spellbook_rarity': 'circle_4_to_9_epic',
    }
    if index.get('global_progression_audit') != expected_global:
        raise SystemExit('alpha.75 global progression audit metadata mismatch')
    curve = index.get('global_curve_preserved', {})
    if curve.get('same_circle_cast_ticks') != [6,10,16,26,42,68,105,155,220]:
        raise SystemExit('alpha.75 same-circle cast curve drift')
    if curve.get('base_max_mana') != [100,180,300,480,750,1150,1800,2800,4500]:
        raise SystemExit('alpha.75 mana curve drift')
    if curve.get('equipment_mana_cost_floor') != 0.10 or curve.get('equipment_cooldown_floor') != 0.10:
        raise SystemExit('alpha.75 equipment floor drift')
'''
if jar_anchor not in v:
    raise SystemExit('jar verifier insertion anchor missing')
v = v.replace(jar_anchor, jar_anchor + jar_insert, 1)
v = v.replace("print('Arcane Circle alpha.74 JAR verification: PASS')", "print('Arcane Circle alpha.75 JAR verification: PASS')", 1)
v = v.replace("print('alpha74_first_circle_value_pass_1=PASS')", "print('alpha75_successful_use_mastery_floor=PASS')\nprint('alpha75_hostile_only_insight_economy=PASS')\nprint('alpha75_academy_server_circle_gate=PASS')\nprint('alpha75_global_curve_preserved=PASS')\nprint('alpha74_first_circle_value_pass_1=PASS')", 1)
verify_path.write_text(v, encoding='utf-8')

print('alpha.75 patch staged')
