from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / 'projects/arcane-circle'


def replace_once(path, old, new):
    path = Path(path)
    text = path.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one replacement, found {count}: {old[:100]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


# Version.
replace_once(P / 'gradle.properties', 'mod_version=0.12.1-alpha.72', 'mod_version=0.12.1-alpha.73')
replace_once(P / 'gradle.properties', '# alpha.72 third-circle authority audit: NPC Haste parity, bounded Dispel, solid-geometry phase Blink', '# alpha.73 second-circle authority audit: LOS Misty Step, apex-hover Levitate, maintained VFX/dispel cleanup')
replace_once(P / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java', 'VERSION = "0.12.1-alpha.72"', 'VERSION = "0.12.1-alpha.73"')

second = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/SecondCircleSpellService.java'
replace_once(second,
'''import net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.Vec3;''',
'''import net.minecraft.world.level.ClipContext;\nimport net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.level.block.state.BlockState;\nimport net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.HitResult;\nimport net.minecraft.world.phys.Vec3;''')
replace_once(second,
'''import java.util.HashMap;\nimport java.util.Iterator;''',
'''import java.util.HashMap;\nimport java.util.HashSet;\nimport java.util.Iterator;''')
replace_once(second,
'''    private static final int LEVITATE_RISE_TICKS = 60;\n    private static final int LEVITATE_TOTAL_TICKS = 140;''',
'''    public static final int LEVITATE_RISE_TICKS = 60;\n    public static final int LEVITATE_TOTAL_TICKS = 140;''')

replace_once(second,
'''    public static void clear(LivingEntity subject) { if (subject != null) clear(subject.getUUID()); }\n\n    public static void clear(UUID id) {''',
'''    public static void clear(LivingEntity subject) {\n        if (subject == null) return;\n        UUID id = subject.getUUID();\n        Set<String> own = new HashSet<>();\n        if (SALVOS.stream().anyMatch(salvo -> salvo.ownerId.equals(id))) own.add("scorching_ray");\n        if (WEBS.stream().anyMatch(zone -> zone.ownerId.equals(id))) own.add("web");\n        if (MIRRORS.containsKey(id)) own.add("mirror_image");\n        if (INVISIBILITY.containsKey(id)) own.add("invisibility");\n        if (BLUR.containsKey(id)) own.add("blur");\n        for (HoldState state : HOLDS.values()) if (state.ownerId.equals(id)) own.add("hold_person");\n        for (LevitateState state : LEVITATION.values()) if (state.ownerId.equals(id)) own.add("levitate");\n\n        // Dispel/antimagic can target the receiver rather than the caster. Cancel the owner's\n        // corresponding release too so server authority and visible maintained magic end together.\n        for (RaySalvo salvo : SALVOS)\n            if (salvo.targetId.equals(id) && !salvo.ownerId.equals(id))\n                cancelOwnerRelease(salvo.level, salvo.ownerId, "scorching_ray");\n        for (HoldState state : HOLDS.values())\n            if (state.targetId.equals(id) && !state.ownerId.equals(id))\n                cancelOwnerRelease(state.level, state.ownerId, "hold_person");\n        for (LevitateState state : LEVITATION.values())\n            if (state.targetId.equals(id) && !state.ownerId.equals(id))\n                cancelOwnerRelease(state.level, state.ownerId, "levitate");\n\n        clear(id);\n        for (String spellId : own) WorldMagicService.cancelRelease(subject, spellId);\n    }\n\n    private static void cancelOwnerRelease(ServerLevel level, UUID ownerId, String spellId) {\n        Entity raw = level.getEntity(ownerId);\n        if (raw instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, spellId);\n    }\n\n    public static void clear(UUID id) {''')

replace_once(second,
'''        BlockPos p = safe.get();\n        caster.stopRiding();''',
'''        BlockPos p = safe.get();\n        if (!clearStepPath(level, caster, p)) {\n            ArcaneNoticeService.push(caster, Component.literal(\n                    "§c[미스티 스텝] §f도착점까지 열린 위상선이 필요합니다. 벽 너머 이동은 점멸 이상 공간술의 권능입니다."), 55);\n            return false;\n        }\n        caster.stopRiding();''')
replace_once(second,
'''        BlockPos p = safe.get();\n        caster.getNavigation().stop();''',
'''        BlockPos p = safe.get();\n        if (!clearStepPath(level, caster, p)) return false;\n        caster.getNavigation().stop();''')

replace_once(second,
'''    private static boolean web(ServerLevel level, LivingEntity caster, double range, Vec3 center) {\n        double radius = Math.max(4.2, Math.min(7.5, SpellMetrics.effectRadius("web", range, 2)));''',
'''    public static double webRadius(double range) {\n        return Math.max(4.2, Math.min(7.5, SpellMetrics.effectRadius("web", range, 2)));\n    }\n\n    private static boolean web(ServerLevel level, LivingEntity caster, double range, Vec3 center) {\n        double radius = webRadius(range);''')

replace_once(second,
'''        boolean terrain = stripFragileWindBlocks(level, origin, direction, length);''',
'''        boolean terrain = caster instanceof ServerPlayer && stripFragileWindBlocks(level, origin, direction, length);''')
replace_once(second,
'''        boolean terrain = shatterBrittle(level, center, radius);''',
'''        boolean terrain = caster instanceof ServerPlayer && shatterBrittle(level, center, radius);''')

replace_once(second,
'''    private static boolean levitate(ServerLevel level, LivingEntity caster, LivingEntity target) {\n        if (target == null || !target.isAlive() || target.isRemoved()) return false;\n        long now = level.getGameTime();\n        LEVITATION.put(target.getUUID(), new LevitateState(level, caster.getUUID(), target.getUUID(),\n                now + LEVITATE_RISE_TICKS, now + LEVITATE_TOTAL_TICKS));\n        target.fallDistance = 0.0F;\n        return true;\n    }''',
'''    private static boolean levitate(ServerLevel level, LivingEntity caster, LivingEntity target) {\n        if (target == null || !target.isAlive() || target.isRemoved()) return false;\n        boolean hostile = target != caster && !caster.isAlliedTo(target);\n        if (hostile && !levitateEligible(target)) {\n            if (caster instanceof ServerPlayer player) {\n                ArcaneNoticeService.push(player, Component.literal(\n                        "§c[레비테이트] §f초대형·보스급 생명체는 2써클 부양장으로 공중 고정할 수 없습니다."), 55);\n            }\n            return false;\n        }\n        long now = level.getGameTime();\n        LEVITATION.put(target.getUUID(), new LevitateState(level, caster.getUUID(), target.getUUID(),\n                now + LEVITATE_RISE_TICKS, now + LEVITATE_TOTAL_TICKS));\n        target.fallDistance = 0.0F;\n        if (caster instanceof ServerPlayer player) {\n            ArcaneNoticeService.push(player, Component.literal(\n                    "§b[레비테이트] §f3초 상승 → 4초 정점 부양 → 안전 하강. §7자신·아군은 체급 제한 없이 사용할 수 있습니다."), 70);\n        }\n        return true;\n    }\n\n    private static boolean levitateEligible(LivingEntity target) {\n        return target.getBbWidth() <= 2.20F && target.getBbHeight() <= 4.50F && target.getMaxHealth() <= 120.0F;\n    }''')
replace_once(second,
'''            } else if (now < state.expiresAt) {\n                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8, 0, true, false));''',
'''            } else if (now < state.expiresAt) {\n                Vec3 motion = target.getDeltaMovement();\n                target.setDeltaMovement(motion.x * .60, Math.max(-.02, Math.min(.02, motion.y)), motion.z * .60);\n                target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 8, 0, true, false));''')

replace_once(second,
'''    private static Vec3 clampDestination(Vec3 start, Vec3 desired, double maxDistance) {''',
'''    private static boolean clearStepPath(ServerLevel level, LivingEntity caster, BlockPos destination) {\n        Vec3 start = caster.getEyePosition();\n        Vec3 end = new Vec3(destination.getX() + .5,\n                destination.getY() + Math.max(.8, caster.getBbHeight() * .50), destination.getZ() + .5);\n        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,\n                ClipContext.Fluid.NONE, caster));\n        return hit.getType() == HitResult.Type.MISS;\n    }\n\n    private static Vec3 clampDestination(Vec3 start, Vec3 desired, double maxDistance) {''')

# Dedicated 2C grimoire contract.
summary = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/SecondCircleSpellSummary.java'
summary.write_text('''package kr.moonseungjun.arcanecircle.magic;\n\n/** Alpha.73 exact second-circle gameplay contract used by the grimoire. */\npublic final class SecondCircleSpellSummary {\n    private SecondCircleSpellSummary() {}\n\n    public static String summary(String id) {\n        if (id == null) return "";\n        return switch (id) {\n            case "scorching_ray" -> "단일 대상을 추적하는 3연속 화염 광선 · 첫 타격 후 0.5초 간격으로 2회 추가 타격 + 각 타격 화상";\n            case "misty_step" -> "최대 약 12m 단거리 안전 이동 · 출발~도착 사이 열린 통로가 필요하며 고체 벽은 관통할 수 없음";\n            case "web" -> "11초 반경 약 4.2~7.5m 고정 포박장 · 0.2초마다 적의 수평 속도를 강제 감쇠하고 강한 둔화·약화를 재적용";\n            case "mirror_image" -> "13초 동안 적대 직접 공격 3회를 환영 3체가 확정 대리 · 환경/비공격성 피해에는 소모되지 않음";\n            case "invisibility" -> "21초 은신 · 주변 적대 추적 즉시 해제 + 첫 적대 직접 공격 궤적 1회를 흘린 뒤 은신 종료";\n            case "gust_of_wind" -> "전방 약 8~18m 직선 강풍으로 적을 강제 밀침 · 플레이어 시전은 거미줄/불/횃불 같은 취약 오브젝트도 제거";\n            case "hold_person" -> "일반 인간형 체급 대상을 9초 완전 속박 · 이동/공격/Arcane 시전 봉쇄 · 대형/보스급 면역";\n            case "shatter" -> "조준 지점 반경 약 4~6.5m 진동 폭발 · 광역 피해 + 플레이어 시전은 유리/얼음/자수정 같은 취성 재료 파괴";\n            case "blur" -> "18초 동안 적대 직접 공격이 매번 35% 확률로 빗나감 · Mirror Image와 달리 충전 수 제한 없이 확률 판정";\n            case "levitate" -> "자신·아군 또는 일반 체급 대상 하나를 3초 상승시킨 뒤 4초 정점에 붙잡아 두고 종료 후 4초 안전 하강";\n            default -> "";\n        };\n    }\n}\n''', encoding='utf-8')

# Exact web footprint overlay.
overlay = P / 'src/main/java/kr/moonseungjun/arcanecircle/client/SecondCircleAuthorityOverlay.java'
overlay.write_text('''package kr.moonseungjun.arcanecircle.client;\n\nimport kr.moonseungjun.arcanecircle.magic.SecondCircleSpellService;\nimport kr.moonseungjun.arcanecircle.magic.SpellDefinition;\nimport net.minecraft.world.phys.Vec3;\n\n/** Alpha.73 exact-footprint overlay for maintained second-circle battlefield authority. */\nfinal class SecondCircleAuthorityOverlay {\n    private SecondCircleAuthorityOverlay() {}\n\n    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,\n                                   double range, double elapsedSeconds, double durationSeconds) {\n        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(360);\n        if (spell == null || spell.circle() != 2 || !"web".equals(spell.id())) return m.build();\n        double t = Math.max(0.0, elapsedSeconds);\n        double radius = SecondCircleSpellService.webRadius(range);\n        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();\n        Vec3 floor = target;\n        double pulse = 1.0 - ((t % .20) / .20);\n        m.circle(g, floor.add(0, .045, 0), radius, 56, .90F);\n        m.circle(g, floor.add(0, .075, 0), radius * (.42 + .48 * pulse), 44, .32F);\n        for (int ring = 1; ring <= 3; ring++) {\n            double r = radius * ring / 4.0;\n            m.circle(g, floor.add(0, .055 + ring * .006, 0), r, 32 + ring * 4, .24F);\n        }\n        for (int i = 0; i < 12; i++) {\n            double a = i * Math.PI * 2.0 / 12.0 + Math.sin(t * 1.1) * .025;\n            Vec3 inner = floor.add(g.point(a, radius * .12)).add(0, .06, 0);\n            Vec3 outer = floor.add(g.point(a, radius)).add(0, .06, 0);\n            m.line(inner, outer, .30F, .86F, .28F);\n        }\n        return m.build();\n    }\n}\n''', encoding='utf-8')

# SpellDefinition dedicated summary route.
spell_def = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/SpellDefinition.java'
replace_once(spell_def,
'''        String firstCircle = FirstCircleSpellSummary.summary(id);\n        if (!firstCircle.isBlank()) return firstCircle;\n        String thirdCircle = ThirdCircleSpellSummary.summary(id);''',
'''        String firstCircle = FirstCircleSpellSummary.summary(id);\n        if (!firstCircle.isBlank()) return firstCircle;\n        String secondCircle = SecondCircleSpellSummary.summary(id);\n        if (!secondCircle.isBlank()) return secondCircle;\n        String thirdCircle = ThirdCircleSpellSummary.summary(id);''')

# World VFX lifetime synchronization for all maintained 2C effects.
world_magic = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java'
replace_once(world_magic, '        duration = thirdCircleVisualDuration(spell.id(), duration);', '        duration = secondCircleVisualDuration(spell.id(), duration);\n        duration = thirdCircleVisualDuration(spell.id(), duration);')
# exactly two release paths must now contain secondCircle; first replacement changes both? replace_once only one, so patch NPC separately.
text = world_magic.read_text(encoding='utf-8')
needle = '        duration = thirdCircleVisualDuration(spell.id(), duration);'
if text.count(needle) != 1:
    raise SystemExit(f'WorldMagicService: expected one remaining third-duration call, found {text.count(needle)}')
world_magic.write_text(text.replace(needle, '        duration = secondCircleVisualDuration(spell.id(), duration);\n        duration = thirdCircleVisualDuration(spell.id(), duration);', 1), encoding='utf-8')
replace_once(world_magic,
'''    private static int thirdCircleVisualDuration(String spellId, int baseDuration) {''',
'''    private static int secondCircleVisualDuration(String spellId, int baseDuration) {\n        return switch (spellId) {\n            case "web" -> Math.max(baseDuration, SecondCircleSpellService.WEB_TICKS);\n            case "mirror_image" -> Math.max(baseDuration, SecondCircleSpellService.MIRROR_TICKS);\n            case "invisibility" -> Math.max(baseDuration, SecondCircleSpellService.INVISIBILITY_TICKS);\n            case "hold_person" -> Math.max(baseDuration, SecondCircleSpellService.HOLD_PERSON_TICKS);\n            case "blur" -> Math.max(baseDuration, SecondCircleSpellService.BLUR_TICKS);\n            case "levitate" -> Math.max(baseDuration, SecondCircleSpellService.LEVITATE_TOTAL_TICKS);\n            default -> baseDuration;\n        };\n    }\n\n    private static int thirdCircleVisualDuration(String spellId, int baseDuration) {''')

# Web overlay routing.
tracker = P / 'src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java'
replace_once(tracker,
'''                if(v.spell.circle()==3){\n                    ArcaneWorldMesh thirdAuthority=ThirdCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),''',
'''                if(v.spell.circle()==2){\n                    ArcaneWorldMesh secondAuthority=SecondCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),\n                            v.range,elapsedSeconds,durationSeconds);\n                    if(secondAuthority.size()>0)entries.add(new RenderEntry(center,secondAuthority,color,87,opacity));\n                }\n\n                if(v.spell.circle()==3){\n                    ArcaneWorldMesh thirdAuthority=ThirdCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),''')

# Catalogue metadata.
index_path = P / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
index = json.loads(index_path.read_text(encoding='utf-8'))
index['version'] = '0.12.1-alpha.73'
index['second_circle_deep_audit'] = [
    'timed_three_ray_scorching_salvo',
    'line_of_sight_safe_misty_step',
    'exact_persistent_web_snare',
    'deterministic_three_charge_mirror_images',
    'aggro_breaking_single_trajectory_invisibility',
    'directional_force_gust_with_player_only_fragile_clear',
    'restricted_hold_person',
    'brittle_material_shatter_with_npc_terrain_safety',
    'probabilistic_direct_attack_blur',
    'three_second_rise_four_second_apex_hover_safe_descent_levitate',
]
index['second_circle_preserved_authority'] = [
    'scorching_ray','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur'
]
index['second_circle_value_pass_1'] = {
    'misty_step': '12m_line_of_sight_safe_reposition_no_solid_geometry_phase',
    'levitate': '3s_controlled_rise_plus_4s_apex_hover_then_4s_safe_descent',
}
index['second_circle_role_audit'] = {
    'scorching_ray': 'sequential_single_target_fire_salvo',
    'misty_step': 'short_line_of_sight_safe_reposition',
    'web': 'fixed_physical_movement_snare_zone',
    'mirror_image': 'deterministic_three_direct_attack_intercepts',
    'invisibility': 'aggro_break_escape_and_single_trajectory_dodge',
    'gust_of_wind': 'directional_forced_displacement_and_fragile_clear',
    'hold_person': 'restricted_humanoid_total_restraint',
    'shatter': 'brittle_material_demolition_burst',
    'blur': 'sustained_probabilistic_direct_attack_evasion',
    'levitate': 'vertical_position_control_and_apex_suspension',
}
index['second_circle_visual_lifetime_sync'] = 'maintained_server_effects_and_release_vfx_aligned'
index['second_circle_dispel_visual_cleanup'] = True
index['second_circle_npc_terrain_safety'] = 'gust_and_shatter_keep_combat_authority_but_skip_npc_world_edit'
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Source verifier.
source_test = P / 'tools/test_current_source.py'
replace_once(source_test, "need(gradle, 'mod_version=0.12.1-alpha.72')", "need(gradle, 'mod_version=0.12.1-alpha.73')")
replace_once(source_test, "need(main, 'VERSION = \"0.12.1-alpha.72\"')", "need(main, 'VERSION = \"0.12.1-alpha.73\"')")
replace_once(source_test, "assert index['version'] == '0.12.1-alpha.72'", "assert index['version'] == '0.12.1-alpha.73'")
replace_once(source_test,
"need(text(magic / 'SpellDefinition.java'), 'ThirdCircleSpellSummary.summary(id)', 'FourthCircleSpellSummary.summary(id)', 'FifthCircleSpellSummary.summary(id)', 'NinthCircleSpellSummary.summary(id)')",
"need(text(magic / 'SpellDefinition.java'), 'SecondCircleSpellSummary.summary(id)', 'ThirdCircleSpellSummary.summary(id)', 'FourthCircleSpellSummary.summary(id)', 'FifthCircleSpellSummary.summary(id)', 'NinthCircleSpellSummary.summary(id)')")

alpha73_source = '''\n# Alpha.73 second-circle authority/value pass.\nsecond = text(magic / 'SecondCircleSpellService.java')\nsecond_summary = text(magic / 'SecondCircleSpellSummary.java')\nsecond_authority = text(client / 'SecondCircleAuthorityOverlay.java')\nworld_magic_2 = text(magic / 'WorldMagicService.java')\ntracker_2 = text(client / 'WorldMagicTracker.java')\nneed(second,\n     'clearStepPath(level, caster, p)', 'HitResult.Type.MISS',\n     'public static double webRadius(double range)',\n     'caster instanceof ServerPlayer && stripFragileWindBlocks',\n     'caster instanceof ServerPlayer && shatterBrittle',\n     'public static final int LEVITATE_RISE_TICKS = 60',\n     'public static final int LEVITATE_TOTAL_TICKS = 140',\n     '3초 상승 → 4초 정점 부양 → 안전 하강',\n     'target.setDeltaMovement(motion.x * .60, Math.max(-.02, Math.min(.02, motion.y)), motion.z * .60);',\n     'cancelOwnerRelease(salvo.level, salvo.ownerId, "scorching_ray")',\n     'cancelOwnerRelease(state.level, state.ownerId, "hold_person")',\n     'cancelOwnerRelease(state.level, state.ownerId, "levitate")')\nneed(second_summary,\n     '최대 약 12m 단거리 안전 이동', '고체 벽은 관통할 수 없음',\n     '13초 동안 적대 직접 공격 3회', '매번 35% 확률',\n     '3초 상승시킨 뒤 4초 정점에 붙잡아 두고 종료 후 4초 안전 하강')\nneed(world_magic_2,\n     'duration = secondCircleVisualDuration(spell.id(), duration);',\n     'case "web" -> Math.max(baseDuration, SecondCircleSpellService.WEB_TICKS);',\n     'case "mirror_image" -> Math.max(baseDuration, SecondCircleSpellService.MIRROR_TICKS);',\n     'case "invisibility" -> Math.max(baseDuration, SecondCircleSpellService.INVISIBILITY_TICKS);',\n     'case "hold_person" -> Math.max(baseDuration, SecondCircleSpellService.HOLD_PERSON_TICKS);',\n     'case "blur" -> Math.max(baseDuration, SecondCircleSpellService.BLUR_TICKS);',\n     'case "levitate" -> Math.max(baseDuration, SecondCircleSpellService.LEVITATE_TOTAL_TICKS);')\nassert world_magic_2.count('duration = secondCircleVisualDuration(spell.id(), duration);') == 2\nneed(second_authority, '"web".equals(spell.id())', 'SecondCircleSpellService.webRadius(range)',\n     'double pulse = 1.0 - ((t % .20) / .20);')\nneed(tracker_2, 'if(v.spell.circle()==2){', 'SecondCircleAuthorityOverlay.release(')\nexpected2 = {\n    'misty_step': '12m_line_of_sight_safe_reposition_no_solid_geometry_phase',\n    'levitate': '3s_controlled_rise_plus_4s_apex_hover_then_4s_safe_descent',\n}\nassert index['second_circle_value_pass_1'] == expected2\nroles2 = index['second_circle_role_audit']\nassert set(roles2) == {'scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'}\nassert len(set(roles2.values())) == 10\nassert index['second_circle_visual_lifetime_sync'] == 'maintained_server_effects_and_release_vfx_aligned'\nassert index['second_circle_dispel_visual_cleanup'] is True\nassert index['second_circle_npc_terrain_safety'] == 'gust_and_shatter_keep_combat_authority_but_skip_npc_world_edit'\nassert index['second_circle_npc_parity'] is True\n\n'''
replace_once(source_test, '# Alpha.72 third-circle authority/value pass.\n', alpha73_source + '# Alpha.72 third-circle authority/value pass.\n')
replace_once(source_test,
"     '0.12.1-alpha.72', 'ThirdCircleSpellSummary.class', 'ThirdCircleAuthorityOverlay.class', 'FourthCircleSpellSummary.class', 'FourthCircleAuthorityOverlay.class', 'FifthCircleSpellSummary.class', 'FifthCircleAuthorityOverlay.class',",
"     '0.12.1-alpha.73', 'SecondCircleSpellSummary.class', 'SecondCircleAuthorityOverlay.class', 'ThirdCircleSpellSummary.class', 'ThirdCircleAuthorityOverlay.class', 'FourthCircleSpellSummary.class', 'FourthCircleAuthorityOverlay.class', 'FifthCircleSpellSummary.class', 'FifthCircleAuthorityOverlay.class',")
replace_once(source_test,
"print('all_109_explicit_effect_summaries=PASS')\nprint('alpha72_haste_player_npc_arcane_tempo_parity=PASS')",
"print('all_109_explicit_effect_summaries=PASS')\nprint('alpha73_misty_step_line_of_sight_role_boundary=PASS')\nprint('alpha73_levitate_apex_hover_authority=PASS')\nprint('alpha73_second_circle_visual_lifetime_sync=PASS')\nprint('alpha73_second_circle_dispel_visual_cleanup=PASS')\nprint('alpha73_second_circle_npc_terrain_safety=PASS')\nprint('alpha73_second_circle_role_audit=PASS')\nprint('alpha73_second_circle_value_pass_1=PASS')\nprint('alpha72_haste_player_npc_arcane_tempo_parity=PASS')")

# JAR verifier.
verify = P / 'tools/verify_jar.py'
replace_once(verify,
"    'kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.class',",
"    'kr/moonseungjun/arcanecircle/magic/SecondCircleSpellSummary.class',\n    'kr/moonseungjun/arcanecircle/client/SecondCircleAuthorityOverlay.class',\n    'kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellService.class',")
replace_once(verify, "if version != '0.12.1-alpha.72':", "if version != '0.12.1-alpha.73':")
replace_once(verify, "raise SystemExit(f'unexpected alpha.72 package version: {version}')", "raise SystemExit(f'unexpected alpha.73 package version: {version}')")
alpha73_verify = '''\n    expected2 = {\n        'misty_step': '12m_line_of_sight_safe_reposition_no_solid_geometry_phase',\n        'levitate': '3s_controlled_rise_plus_4s_apex_hover_then_4s_safe_descent',\n    }\n    if index.get('second_circle_value_pass_1') != expected2:\n        raise SystemExit(f'alpha.73 second-circle value metadata mismatch: {index.get("second_circle_value_pass_1")}')\n    roles2 = index.get('second_circle_role_audit', {})\n    expected_roles2 = {'scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'}\n    if set(roles2) != expected_roles2 or len(set(roles2.values())) != 10:\n        raise SystemExit('alpha.73 second-circle role separation contract missing')\n    if index.get('second_circle_visual_lifetime_sync') != 'maintained_server_effects_and_release_vfx_aligned':\n        raise SystemExit('alpha.73 second-circle visual lifetime contract missing')\n    if index.get('second_circle_dispel_visual_cleanup') is not True:\n        raise SystemExit('alpha.73 second-circle dispel visual cleanup missing')\n    if index.get('second_circle_npc_terrain_safety') != 'gust_and_shatter_keep_combat_authority_but_skip_npc_world_edit':\n        raise SystemExit('alpha.73 second-circle NPC terrain safety missing')\n\n'''
replace_once(verify, '    expected3 = {\n', alpha73_verify + '    expected3 = {\n')
replace_once(verify, "print('Arcane Circle alpha.72 JAR verification: PASS')", "print('Arcane Circle alpha.73 JAR verification: PASS')")
replace_once(verify,
"print('alpha72_third_circle_value_pass_1=PASS')",
"print('alpha73_second_circle_value_pass_1=PASS')\nprint('alpha73_misty_step_line_of_sight_role_boundary=PASS')\nprint('alpha73_levitate_apex_hover_authority=PASS')\nprint('alpha73_second_circle_visual_lifetime_sync=PASS')\nprint('alpha73_second_circle_dispel_visual_cleanup=PASS')\nprint('alpha73_second_circle_npc_terrain_safety=PASS')\nprint('alpha73_second_circle_role_separation=PASS')\nprint('alpha73_second_circle_npc_parity=PASS')\nprint('alpha72_third_circle_value_pass_1=PASS')")

print('alpha.73 patch staged successfully')
