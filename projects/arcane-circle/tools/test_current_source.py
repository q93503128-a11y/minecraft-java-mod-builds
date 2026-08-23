from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'
world = root / 'src/main/java/kr/moonseungjun/arcanecircle/world'
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'


def text(path):
    return path.read_text(encoding='utf-8')


def need(body, *tokens):
    for token in tokens:
        assert token in body, token


# Canonical version/catalogue contract.
gradle = text(root / 'gradle.properties')
main = text(root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index = json.loads(text(root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'))
need(gradle, 'mod_version=0.12.1-alpha.73')
need(main, 'VERSION = "0.12.1-alpha.73"')
assert index['version'] == '0.12.1-alpha.73'
assert index['implemented_circles'] == list(range(1, 10))
assert index['direct_spells'] == 90 and index['fusion_spells'] == 19

catalog = text(magic / 'SpellCatalog.java')
direct = set(re.findall(r'\badd\("([a-z0-9_]+)"', catalog))
fusions = set(re.findall(r'\baddFusion\("([a-z0-9_]+)"', catalog))
spells = direct | fusions
assert (len(direct), len(fusions), len(spells)) == (90, 19, 109)
summary = text(magic / 'SpellEffectSummary.java')
assert set(re.findall(r'case "([a-z0-9_]+)"', summary)) == spells
need(text(magic / 'SpellDefinition.java'), 'SecondCircleSpellSummary.summary(id)', 'ThirdCircleSpellSummary.summary(id)', 'FourthCircleSpellSummary.summary(id)', 'FifthCircleSpellSummary.summary(id)', 'NinthCircleSpellSummary.summary(id)')

circle_specs = {
    'FirstCircleSpellService.java': ['magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor'],
    'SecondCircleSpellService.java': ['scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'],
    'ThirdCircleSpellService.java': ['fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'],
    'FourthCircleSpellService.java': ['wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door','stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer'],
    'FifthCircleSpellService.java': ['cone_of_cold','wall_of_force','cloudkill','telekinesis','flame_strike','hold_monster','mass_cure_wounds','passwall','dominate_person','insect_plague'],
    'SixthCircleSpellService.java': ['disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam','true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death'],
    'SeventhCircleSpellService.java': ['delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage','plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport'],
    'EighthCircleSpellService.java': ['antimagic_field','clone','control_weather','demiplane','dominate_monster','earthquake','feeblemind','incendiary_cloud','maze','sunburst'],
    'NinthCircleSpellService.java': ['meteor_swarm','power_word_kill','prismatic_wall','shapechange','time_stop','true_polymorph','weird','wish','gate','foresight'],
}
for filename, ids in circle_specs.items():
    body = text(magic / filename)
    for spell in ids:
        need(body, f'"{spell}"')
    need(body, 'public static boolean executeNpc(')

# Earlier deep-pass regression anchors.
need(text(magic / 'FirstCircleSpellService.java'), 'sleepEligible(target, power)', 'ArcaneLightService.illuminate(player, 1800)')
need(text(magic / 'SecondCircleSpellService.java'), 'new RaySalvo(level, caster.getUUID(), target.getUUID()', 'holdEligible(target)')
need(text(magic / 'ThirdCircleSpellService.java'), 'ArcaneDamage.isResolving()', 'SLEET_ZONES.add(new SleetZone')
need(text(magic / 'FourthCircleSpellService.java'), 'FIRE_WALLS.add(new FireWall', 'ICE_STORMS.add(new IceStorm')
need(text(magic / 'FifthCircleSpellService.java'), 'public static boolean intercepts(LivingEntity caster, CastTargetSnapshot snapshot)', 'DOMINATED.put')


# Alpha.73 second-circle authority/value pass.
second = text(magic / 'SecondCircleSpellService.java')
second_summary = text(magic / 'SecondCircleSpellSummary.java')
second_authority = text(client / 'SecondCircleAuthorityOverlay.java')
world_magic_2 = text(magic / 'WorldMagicService.java')
tracker_2 = text(client / 'WorldMagicTracker.java')
need(second,
     'clearStepPath(level, caster, p)', 'HitResult.Type.MISS',
     'public static double webRadius(double range)',
     'caster instanceof ServerPlayer && stripFragileWindBlocks',
     'caster instanceof ServerPlayer && shatterBrittle',
     'public static final int LEVITATE_RISE_TICKS = 60',
     'public static final int LEVITATE_TOTAL_TICKS = 140',
     '3초 상승 → 4초 정점 부양 → 안전 하강',
     'target.setDeltaMovement(motion.x * .60, Math.max(-.02, Math.min(.02, motion.y)), motion.z * .60);',
     'cancelOwnerRelease(salvo.level, salvo.ownerId, "scorching_ray")',
     'cancelOwnerRelease(state.level, state.ownerId, "hold_person")',
     'cancelOwnerRelease(state.level, state.ownerId, "levitate")')
need(second_summary,
     '최대 약 12m 단거리 안전 이동', '고체 벽은 관통할 수 없음',
     '13초 동안 적대 직접 공격 3회', '매번 35% 확률',
     '3초 상승시킨 뒤 4초 정점에 붙잡아 두고 종료 후 4초 안전 하강')
need(world_magic_2,
     'duration = secondCircleVisualDuration(spell.id(), duration);',
     'case "scorching_ray" -> Math.max(baseDuration, 20);',
     'case "web" -> Math.max(baseDuration, SecondCircleSpellService.WEB_TICKS);',
     'case "mirror_image" -> Math.max(baseDuration, SecondCircleSpellService.MIRROR_TICKS);',
     'case "invisibility" -> Math.max(baseDuration, SecondCircleSpellService.INVISIBILITY_TICKS);',
     'case "hold_person" -> Math.max(baseDuration, SecondCircleSpellService.HOLD_PERSON_TICKS);',
     'case "blur" -> Math.max(baseDuration, SecondCircleSpellService.BLUR_TICKS);',
     'case "levitate" -> Math.max(baseDuration, SecondCircleSpellService.LEVITATE_TOTAL_TICKS);')
assert world_magic_2.count('duration = secondCircleVisualDuration(spell.id(), duration);') == 2
need(second_authority, '"web".equals(spell.id())', 'SecondCircleSpellService.webRadius(range)',
     'double pulse = 1.0 - ((t % .20) / .20);')
need(tracker_2, 'if(v.spell.circle()==2){', 'SecondCircleAuthorityOverlay.release(')
expected2 = {
    'misty_step': '12m_line_of_sight_safe_reposition_no_solid_geometry_phase',
    'levitate': '3s_controlled_rise_plus_4s_apex_hover_then_4s_safe_descent',
}
assert index['second_circle_value_pass_1'] == expected2
roles2 = index['second_circle_role_audit']
assert set(roles2) == {'scorching_ray','misty_step','web','mirror_image','invisibility','gust_of_wind','hold_person','shatter','blur','levitate'}
assert len(set(roles2.values())) == 10
assert index['second_circle_visual_lifetime_sync'] == 'maintained_server_effects_and_release_vfx_aligned'
assert index['second_circle_dispel_visual_cleanup'] is True
assert index['second_circle_npc_terrain_safety'] == 'gust_and_shatter_keep_combat_authority_but_skip_npc_world_edit'
assert index['second_circle_npc_parity'] is True
assert index['second_circle_scorching_ray_visual_ticks'] == 20

# Alpha.72 third-circle authority/value pass.
third = text(magic / 'ThirdCircleSpellService.java')
third_summary = text(magic / 'ThirdCircleSpellSummary.java')
third_authority = text(client / 'ThirdCircleAuthorityOverlay.java')
arcane_buff = text(magic / 'ArcaneBuffRuntime.java')
mage_ai = text(world / 'ArcaneMageService.java')
world_magic_3 = text(magic / 'WorldMagicService.java')
tracker_3 = text(client / 'WorldMagicTracker.java')
need(third,
     'NPC_HASTE = new HashMap<>()', 'case "haste" -> npcHaste(level, caster);',
     'public static double npcCastTimeMultiplier(LivingEntity caster)', 'return hasNpcHaste(caster) ? .72 : 1.0;',
     'public static double npcCooldownMultiplier(LivingEntity caster)', 'return hasNpcHaste(caster) ? .85 : 1.0;',
     'ArcaneBuffRuntime.clearSpell(subject, "haste")',
     '1~3써클 유지형 강화·제어 마법을 해제했습니다.', '4써클 이상 권능은 보존됩니다.',
     'public static double slowRadius(double range)', 'public static double sleetStormRadius(double range)',
     'public static double blinkDistance(double range)', 'findPhaseDestination(level, player.position(), snapshot.launchDirection(), blinkDistance(range))',
     '중간 고체 지형을 무시하는 위상 통과 완료')
assert 'FourthCircleSpellService.clear(target);' not in third
assert 'FifthCircleSpellService.clear(target);' not in third
assert 'EighthCircleSpellService.clear(target);' not in third
need(arcane_buff,
     'player.addEffect(new MobEffectInstance(MobEffects.SPEED, 12, 1, true, false));',
     'public static boolean clearSpell(LivingEntity subject, String spellId)')
need(mage_ai,
     'ThirdCircleSpellService.npcCooldownMultiplier(caster)',
     'ThirdCircleSpellService.npcCastTimeMultiplier(caster)',
     '!ThirdCircleSpellService.hasNpcHaste(caster)', 'SpellCatalog.spell("haste")',
     'if (!"haste".equals(spell.id())) applyControl(caster, target, profile);')
need(third_summary,
     '플레이어와 NPC 모두 시전시간 28% 단축', '1~3써클 강화·제어 마법만 확정 해제',
     '4써클 이상 권능은 보존', '출발~종착 사이 고체 지형은 무시')
need(world_magic_3,
     'duration = thirdCircleVisualDuration(spell.id(), duration);',
     'case "haste" -> Math.max(baseDuration, ThirdCircleSpellService.HASTE_TICKS);',
     'case "slow" -> Math.max(baseDuration, ThirdCircleSpellService.SLOW_TICKS);',
     'case "sleet_storm" -> Math.max(baseDuration, ThirdCircleSpellService.SLEET_TICKS);')
need(third_authority,
     '"slow".equals(spell.id())', '"sleet_storm".equals(spell.id())',
     'ThirdCircleSpellService.slowRadius(range)', 'ThirdCircleSpellService.sleetStormRadius(range)')
need(tracker_3, 'if(v.spell.circle()==3){', 'ThirdCircleAuthorityOverlay.release(')
expected3 = {
    'haste': '30s_player_and_npc_arcane_tempo_acceleration_0.72_cast_0.85_cooldown',
    'dispel_magic': 'deterministic_maintained_magic_purge_circles_1_to_3_only',
    'blink': 'solo_safe_endpoint_phase_traversal_ignoring_intervening_solid_geometry_up_to_20m',
}
assert index['third_circle_value_pass_1'] == expected3
roles3 = index['third_circle_role_audit']
assert set(roles3) == {'fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'}
assert len(set(roles3.values())) == 10
assert index['third_circle_dispel_ceiling'] == 'circles_1_to_3_deterministic_only'
assert index['third_circle_npc_parity'] is True

# Alpha.71 fourth-circle battlefield-authority value pass.
fourth = text(magic / 'FourthCircleSpellService.java')
fourth_summary = text(magic / 'FourthCircleSpellSummary.java')
fourth_authority = text(client / 'FourthCircleAuthorityOverlay.java')
world_magic_4 = text(magic / 'WorldMagicService.java')
tracker_4 = text(client / 'WorldMagicTracker.java')
need(fourth,
     'ICE_STORM_TICKS = 120', 'ICE_STORM_PULSE_TICKS = 10',
     'ICE_STORM_PULSES = ICE_STORM_TICKS / ICE_STORM_PULSE_TICKS', 'PHANTASM_TICKS = 280',
     'public static double iceStormRadius(double range)', 'state.power * .10',
     'target.removeEffect(MobEffects.LEVITATION);', 'target.push(0.0, -.30, 0.0);',
     'FearState terror = FEAR.get(attacker.getUUID());', 'terror.ownerId.equals(target.getUUID())',
     '시전자에게 직접 피해를 줄 수 없고 2초마다 정신 피해')
need(fourth_summary,
     '6초 고정 우박 제압구역', '반복 강제 하강으로 공중 이동을 억제',
     '14초 단일 공포 결속', '시전자에게 직접 피해를 줄 수 없으며')
need(world_magic_4,
     'duration = fourthCircleVisualDuration(spell.id(), duration);',
     'case "ice_storm" -> Math.max(baseDuration, FourthCircleSpellService.ICE_STORM_TICKS);',
     'case "phantasmal_killer" -> Math.max(baseDuration, FourthCircleSpellService.PHANTASM_TICKS);')
need(fourth_authority,
     '"ice_storm".equals(spell.id())', 'FourthCircleSpellService.iceStormRadius(range)',
     'double pulse = 1.0 - ((t % .50) / .50);')
need(tracker_4, 'if(v.spell.circle()==4){', 'FourthCircleAuthorityOverlay.release(')
expected4 = {
    'ice_storm': '6s_fixed_anti_air_hail_suppression_with_0.5s_pulses',
    'phantasmal_killer': '14s_single_target_terror_bond_forced_retreat_and_owner_damage_denial',
}
assert index['fourth_circle_value_pass_1'] == expected4
roles4 = index['fourth_circle_role_audit']
assert set(roles4) == {'wall_of_fire','ice_storm','greater_invisibility','resilient_sphere','dimension_door','stoneskin','confusion','blight','freedom_of_movement','phantasmal_killer'}
assert len(set(roles4.values())) == 10
assert index['fourth_circle_npc_parity'] is True

# Alpha.70 fifth-circle battlefield-command value pass.
fifth = text(magic / 'FifthCircleSpellService.java')
fifth_summary = text(magic / 'FifthCircleSpellSummary.java')
fifth_authority = text(client / 'FifthCircleAuthorityOverlay.java')
world_magic_5 = text(magic / 'WorldMagicService.java')
tracker_5 = text(client / 'WorldMagicTracker.java')
need(fifth,
     'FLAME_STRIKE_TICKS = 80', 'FLAME_STRIKE_PULSE_TICKS = 10',
     'DOMINATE_PERSON_TICKS = 600', 'tickFlameStrikes(level, now);',
     'state.power * .09', 'if (top < center.y - .60 || bottom > center.y + 13.5) continue;',
     'public static double flameStrikeRadius(double range)',
     '의 의지를 30초간 장악했습니다.')
need(fifth_summary,
     '4초 천공 화염기둥', '0.5초마다 내부 적을 다시 태웁니다.',
     '인간형 체급 비플레이어 적 하나의 전투 진영을 30초 탈취')
need(world_magic_5,
     'duration = fifthCircleVisualDuration(spell.id(), duration);',
     'case "flame_strike" -> Math.max(baseDuration, FifthCircleSpellService.FLAME_STRIKE_TICKS);')
need(fifth_authority,
     '"flame_strike".equals(spell.id())', 'FifthCircleSpellService.flameStrikeRadius(range)',
     'Vec3 top = floor.add(0.0, 13.5, 0.0);')
need(tracker_5, 'if(v.spell.circle()==5){', 'FifthCircleAuthorityOverlay.release(')
expected5 = {
    'flame_strike': '4s_locked_vertical_fire_column_with_initial_breach_and_0.5s_pulses',
    'dominate_person': '30s_person_scale_combat_asset_control',
}
assert index['fifth_circle_value_pass_1'] == expected5
roles5 = index['fifth_circle_role_audit']
assert set(roles5) == {'cone_of_cold','wall_of_force','cloudkill','telekinesis','flame_strike','hold_monster','mass_cure_wounds','passwall','dominate_person','insect_plague'}
assert len(set(roles5.values())) == 10
assert index['fifth_circle_npc_parity'] is True

# Alpha.69 sixth-circle Grand Archmage value pass.
sixth = text(magic / 'SixthCircleSpellService.java')
control = text(magic / 'HighControlSpellService.java')
move_earth = text(magic / 'MoveEarthService.java')
summary6 = text(magic / 'SixthCircleSpellSummary.java')
world_magic = text(magic / 'WorldMagicService.java')
authority6 = text(client / 'SixthCircleAuthorityOverlay.java')
tracker = text(client / 'WorldMagicTracker.java')

need(sixth,
     'SUNBEAM_TICKS = 120', 'FREEZING_SPHERE_TICKS = 200',
     'SUNBEAM_PULSE_TICKS = 10', 'FREEZING_PULSE_TICKS = 10',
     'return Math.max(10.5, Math.min(15.5, Math.max(0.0, range) * .28));',
     'SunbeamField field = new SunbeamField', 'FreezingField field = new FreezingField',
     'tickSunbeams(level, now);', 'tickFreezingFields(level, now);',
     'pulseSunbeam(field, owner, field.power * .16);',
     'pulseFreezing(field, owner, field.power * .075, false);',
     'case "circle_of_death" -> DeathDoctrineService.execute(caster, spellId, range, power, snapshot);',
     'DeathDoctrineService.executeNpc(level, caster, designatedTarget,')
assert 'int executions = 0;' not in sixth
assert 'ordinary && weak' not in sixth

need(control,
     'public static final int MASS_SUGGESTION_TICKS = 400;',
     '약 20초 동안 공격·Arcane 시전을 끊고 전장을 이탈합니다.',
     'level.getGameTime() + MASS_SUGGESTION_TICKS')

need(move_earth,
     'public static final int MAX_MOVED_BLOCKS = 144;',
     'return Math.max(20.0, Math.min(36.0, Math.max(0.0, range) * .64));',
     'return trenchHalfWidth(range) + 2.6;',
     'if (!movable(level, source, state)) continue;',
     'if (!level.setBlock(source, Blocks.AIR.defaultBlockState(), 3)) continue;',
     'if (!level.setBlock(destination, state, 3))',
     'state.hasBlockEntity()', '!state.getFluidState().isEmpty()',
     'state.getBlock().getExplosionResistance() < 1000.0F')

need(summary6,
     '20초 범위 정신 명령', '약 20~36m 방향성 지형공학',
     '6초간 월드 좌표에 고정', '10초간 유지되는 반경 약 10.5~15.5m',
     '처형 판정은 전혀 없고 즉사 권능도 없습니다.')

need(world_magic,
     'duration = sixthCircleVisualDuration(spell.id(), duration);',
     'case "mass_suggestion" -> Math.max(baseDuration, SixthCircleSpellService.NPC_SUGGESTION_TICKS);',
     'case "sunbeam" -> Math.max(baseDuration, SixthCircleSpellService.SUNBEAM_TICKS);',
     'case "freezing_sphere" -> Math.max(baseDuration, SixthCircleSpellService.FREEZING_SPHERE_TICKS);')
need(authority6,
     'Set.of(\n            "mass_suggestion", "move_earth", "sunbeam", "freezing_sphere")',
     'SixthCircleSpellService.massSuggestionRadius(range)',
     'SixthCircleSpellService.moveEarthLength(range)',
     'SixthCircleSpellService.moveEarthTrenchHalfWidth(range)',
     'SixthCircleSpellService.moveEarthBermOffset(range)',
     'SixthCircleSpellService.sunbeamHalfWidth()',
     'SixthCircleSpellService.freezingSphereRadius(range)')
need(tracker, 'if(v.spell.circle()==6){', 'SixthCircleAuthorityOverlay.release(')

expected6 = {
    'mass_suggestion': '20s_group_retreat_and_arcane_suppression',
    'move_earth': '20_to_36m_directional_trench_and_dual_berm_using_relocated_surface_blocks',
    'sunbeam': '6s_locked_piercing_solar_corridor_with_0.5s_pulses',
    'freezing_sphere': '10s_10.5_to_15.5m_cryogenic_denial_with_0.5s_refreeze_pulses',
}
assert index['sixth_circle_value_pass_1'] == expected6
roles6 = index['sixth_circle_role_audit']
assert set(roles6) == {'disintegrate','globe_of_invulnerability','mass_suggestion','move_earth','sunbeam','true_seeing','freezing_sphere','eyebite','flesh_to_stone','circle_of_death'}
assert len(set(roles6.values())) == 10
assert index['sixth_circle_npc_terrain_safety'] == 'move_earth_retains_battlefield_split_but_skips_npc_world_edit'
assert index['sixth_circle_npc_parity'] is True

# Alpha.68 seventh-circle contracts.
seventh = text(magic / 'SeventhCircleSpellService.java')
need(seventh,
     'DELAYED_BLAST_FUSE_TICKS = 72', 'DELAYED_BLAST_VISUAL_TICKS = 90',
     'FIRE_STORM_SEQUENCE_TICKS = 48', 'FIRE_STORM_VISUAL_TICKS = 70', 'FIRE_STORM_STRIKE_INTERVAL = 8',
     'case "finger_of_death" -> DeathDoctrineService.execute(caster, spellId, range, power, snapshot);',
     'TELEPORT_GATHER_RADIUS = 7.0', 'TELEPORT_MAX_COMPANIONS = 6', 'player.isCrouching()')
assert 'double threshold = Math.max(32.0, power * .72)' not in seventh
value7 = index['seventh_circle_value_pass_1']
assert value7 == {
    'delayed_blast_fireball': '3.6s_time_locked_siege_core_with_primary_breach_and_overpressure_shock',
    'finger_of_death': 'canonical_death_doctrine_single_soul_rupture_no_execution_threshold',
    'fire_storm': 'six_step_0.4s_siege_bombardment_with_repeat_hit_attenuation',
    'teleport': 'same_dimension_7m_gather_tactical_group_relocation_up_to_6_companions',
}
roles7 = index['seventh_circle_role_audit']
assert len(roles7) == 10 and len(set(roles7.values())) == 10

# Alpha.66/67 eighth-circle contracts.
eighth = text(magic / 'EighthCircleSpellService.java')
need(eighth,
     'NPC_CLONE_TICKS = 1800', 'NPC_DOMINATE_TICKS = 1200', 'NPC_FEEBLEMIND_TICKS = 1800', 'NPC_MAZE_TICKS = 480',
     'EARTHQUAKES.add(new EarthquakeField', 'INCENDIARY_CLOUDS.add(field)', 'SUNBURSTS.add(field)',
     'DestructiveMagicService.quakeField(player, center, radius, power);',
     'tickSunbursts(level, now)', 'pulseScorchedWake(level, owner, field)')
assert index['eighth_circle_value_pass_1'] == {
    'clone': '90s_single_bound_combat_copy_no_equipment_duplication',
    'dominate_monster': '60s_enemy_combat_asset_theft',
    'feeblemind': '90s_total_arcane_shutdown_and_severe_combat_degradation',
    'maze': '24s_total_battlefield_exile_plus_6s_aftershock',
}
assert index['eighth_circle_value_pass_2'] == {
    'earthquake': '9s_regional_fault_disaster_with_budgeted_player_terrain_and_unstable_ground',
    'incendiary_cloud': '12s_moving_firefront_with_scorched_wake_route_denial',
    'sunburst': '12s_solar_revelation_purification_and_darkness_denial_domain',
}
roles8 = index['eighth_circle_role_audit']
assert len(roles8) == 10 and len(set(roles8.values())) == 10

# Alpha.65 ninth-circle/divine-scale and death hierarchy regressions.
death = text(magic / 'DeathDoctrineService.java')
need(death,
     'Set.of("circle_of_death", "finger_of_death", "power_word_kill")',
     'double threshold = Math.max(180.0, power * 1.24);',
     'boolean executed = pool <= threshold;')
circle_section = death[death.index('private static boolean circleOfDeath'):death.index('private static boolean fingerOfDeath')]
finger_section = death[death.index('private static boolean fingerOfDeath'):death.index('private static boolean powerWordKill')]
assert 'threshold' not in circle_section and 'executed' not in circle_section
assert 'threshold' not in finger_section and 'executed' not in finger_section

magnitude = text(magic / 'NinthCircleMagnitude.java')
need(magnitude,
     'BASE_METEOR_CAST_RANGE = 72.0', 'BASE_METEOR_CITY_RADIUS = 112.0',
     'return clamp(Math.max(110.0, range * 1.5555555556), 110.0, 168.0);')
pattern = text(magic / 'MeteorBarragePattern.java')
need(pattern, 'GOLDEN_ANGLE', 'NinthCircleMagnitude.meteorStrikeCount(range)', 'NinthCircleMagnitude.meteorFieldRadius(effectiveRange)')
assert 'BASE_STRIKES' not in pattern
alpha65 = text(magic / 'Alpha65NinthCircleRuntime.java')
need(alpha65,
     'WEIRD_ESCAPE_TICKS = 300', 'GATE_TICKS = 600',
     'GroundTargetResolver.safeStanding(level, requested, 18)',
     'public static boolean worldSunder', 'double lateral = Math.abs(flat.dot(right))')
assert 'TeleportTransition.Relative' not in alpha65
need(alpha65, 'Set.<Relative>of()')

assert index['presentation_scale_grammar'] == {
    '1-2': 'hand_scale_immediate', '3-4': 'combat_space', '5-6': 'multi_plane_grand_magic',
    '7': 'fortress_planar_authority', '8': 'regional_reality_authority', '9': 'world_law_catastrophe'}
assert index['common_high_circle_array_overlay'] == 'removed_and_forbidden'
assert index['death_doctrine'] == {
    'circle_of_death': 'wide_life_erosion_no_execution',
    'finger_of_death': 'single_target_soul_rupture_no_threshold_execution',
    'power_word_kill': 'exclusive_ninth_circle_execution_with_fallback_life_collapse'}
city = index['meteor_cityfall']
assert city['baseline_radius'] == 112 and city['baseline_strikes'] == 49 and city['range_scaled'] is True
assert city['impact_surface'] == 'actual_loaded_surface_per_strike_xz'

# UI/lifecycle/package verifier linkage.
primary = text(client / 'PrimaryGrimoireScreen.java')
need(primary, 'Rect slotStrip()', 'Rect effectDetail()', 's.effectSummary()', '"실제 효과 / 판정"')
need(main,
     'SixthCircleSpellService.clear(player);', 'SixthCircleSpellService.tick(level);', 'SixthCircleSpellService.clearAll();',
     'Alpha65NinthCircleRuntime.clear(player);', 'Alpha65NinthCircleRuntime.tick(level);', 'Alpha65NinthCircleRuntime.clearAll();')
verify = text(root / 'tools/verify_jar.py')
need(verify,
     '0.12.1-alpha.73', 'SecondCircleSpellSummary.class', 'SecondCircleAuthorityOverlay.class', 'ThirdCircleSpellSummary.class', 'ThirdCircleAuthorityOverlay.class', 'FourthCircleSpellSummary.class', 'FourthCircleAuthorityOverlay.class', 'FifthCircleSpellSummary.class', 'FifthCircleAuthorityOverlay.class',
     'MoveEarthService.class', 'SixthCircleAuthorityOverlay.class',
     'alpha69_sixth_circle_value_pass_1=PASS', 'alpha68_seventh_circle_value_pass_1=PASS')

print('Arcane Circle current-source audit: PASS')
print('catalog_90_direct_19_fusion=PASS')
print('all_109_explicit_effect_summaries=PASS')
print('alpha73_scorching_ray_full_salvo_visual_window=PASS')
print('alpha73_misty_step_line_of_sight_role_boundary=PASS')
print('alpha73_levitate_apex_hover_authority=PASS')
print('alpha73_second_circle_visual_lifetime_sync=PASS')
print('alpha73_second_circle_dispel_visual_cleanup=PASS')
print('alpha73_second_circle_npc_terrain_safety=PASS')
print('alpha73_second_circle_role_audit=PASS')
print('alpha73_second_circle_value_pass_1=PASS')
print('alpha72_haste_player_npc_arcane_tempo_parity=PASS')
print('alpha72_dispel_magic_circle_1_to_3_ceiling=PASS')
print('alpha72_blink_solid_geometry_phase_relocation=PASS')
print('alpha72_third_circle_visual_hitbox_lifetime_sync=PASS')
print('alpha72_third_circle_role_audit=PASS')
print('alpha72_third_circle_value_pass_1=PASS')
print('alpha71_ice_storm_6s_anti_air_suppression=PASS')
print('alpha71_phantasmal_killer_14s_terror_bond=PASS')
print('alpha71_fourth_circle_visual_hitbox_lifetime_sync=PASS')
print('alpha71_fourth_circle_role_audit=PASS')
print('alpha71_fourth_circle_value_pass_1=PASS')
print('alpha70_flame_strike_4s_vertical_column=PASS')
print('alpha70_dominate_person_30s_person_scale_control=PASS')
print('alpha70_fifth_circle_visual_hitbox_lifetime_sync=PASS')
print('alpha70_fifth_circle_role_audit=PASS')
print('alpha70_fifth_circle_value_pass_1=PASS')
print('alpha69_mass_suggestion_20s_group_disengage=PASS')
print('alpha69_move_earth_precision_engineering=PASS')
print('alpha69_sunbeam_6s_solar_corridor=PASS')
print('alpha69_freezing_sphere_10s_denial=PASS')
print('alpha69_circle_of_death_no_execution_reentry=PASS')
print('alpha69_sixth_circle_visual_hitbox_lifetime_sync=PASS')
print('alpha69_sixth_circle_role_audit=PASS')
print('alpha69_sixth_circle_value_pass_1=PASS')
print('alpha68_seventh_circle_value_pass_1=PASS')
print('alpha67_eighth_circle_value_pass_2=PASS')
print('alpha66_eighth_circle_value_pass=PASS')
print('alpha65_ground_target_and_ninth_circle_regressions=PASS')
print('alpha65_common_grand_array_forbidden=PASS')
print('alpha65_lifecycle_and_grimoire_regressions=PASS')
