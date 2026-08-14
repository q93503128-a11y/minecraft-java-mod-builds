from pathlib import Path

root=Path(__file__).resolve().parents[1]
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'
magic=root/'src/main/java/kr/moonseungjun/arcanecircle/magic'
world=root/'src/main/java/kr/moonseungjun/arcanecircle/world'

def text(path): return path.read_text(encoding='utf-8')

# Version/canonical source.
gradle=text(root/'gradle.properties'); main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
index=text(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json')
assert 'mod_version=0.12.1-alpha.35' in gradle
assert 'VERSION = "0.12.1-alpha.35"' in main
assert '"version": "0.12.1-alpha.35"' in index

# Retired presentation stack stays retired.
retired=['CodexVisualLanguage.java','ArcaneSigilDetailGrammar.java','LowCircleVisualIdentity.java',
'MidCircleVisualIdentity.java','FifthCircleVisualIdentity.java','SixthCircleVisualIdentity.java',
'ArchmageVisualIdentity.java','RangeReactivePresentation.java','SpellVisualSignature.java',
'CastingSilhouetteRenderer.java','RobeRegaliaRenderer.java','SignatureGeometry.java']
for name in retired: assert not (client/name).exists(), name

tracker=text(client/'WorldMagicTracker.java')
for token in ['SpellCinematicDirector.charge','SpellCinematicDirector.release','SpellCinematicDirector.castingFamily',
'ArcaneSigilDirector.charge','ArcaneSigilDirector.releaseEcho','MeteorBarragePattern.withSeed','longValue(values,"seed",0L)',
'MAX_FRAME = 9000','MAX_ENTRY = 2800']:
    assert token in tracker, token
assert tracker.count('!"prismatic_wall".equals(v.spell.id())') >= 2

sigil=text(client/'ArcaneSigilDirector.java')
for token in ['formulaFrame','schoolFormula','geometricDepth','anchorFormula','skyRitual','meteorRitual','inscriptionRing','sigilRangeScale','meteor_swarm','fusionFormula']:
    assert token in sigil, token

director=text(client/'SpellCinematicDirector.java')
for token in ['enum Form','NEEDLE','ORB','VOLLEY','RAY','CONE','FIELD','WALL','GATE','PRISON','SKY','WEATHER','AURA',
'MARK','SHIFT','TRANSFORM','CLOCK','TERRAIN','DOMAIN','meteorSwarm','executionWord','chainLightning','fireStorm',
'worldFault','phoenix','SpellMetrics.effectRadius','SpellMetrics.wallWidth','SpellMetrics.waveLength','SpellMetrics.waveEndRadius',
'MeteorBarragePattern.count()','s.impactTick()','prismaticWallFrame','case "power_word_kill"']:
    assert token in director, token
assert 'double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}}' not in director
assert 'fallHeight=42.0' not in director

# 3P artifact regression: no synthetic player body/filled-box overlay may return.
casting=text(client/'ArcaneCastingPerformance.java')
for forbidden in ['debugFilledBox','submitCustomGeometry','strap(','blade(','pose.translate','pose.rotate']:
    assert forbidden not in casting, forbidden
assert 'Intentionally empty' in casting
gear=text(client/'ArcaneGearRenderer.java')
assert 'ArcaneRegaliaRenderer.render' in gear and 'ArcaneCastingPerformance.render' in gear
assert 'CastingSilhouetteRenderer' not in gear and 'RobeRegaliaRenderer' not in gear
regalia=text(client/'ArcaneRegaliaRenderer.java')
for token in ['outfit','bodice','lapel','shoulderMantle','skirtPair','sideGore','backTrain','facetedSkirt','asymmetricSkirt','ceremonialTab']:
    assert token in regalia, token
assert 'private static void torso(' not in regalia

# UI architecture and mechanical selected-spell summaries.
grimoire=text(client/'GrimoireScreen.java')
for token in ['drawSpine','circleIndex','circleStep','contentBottom','compact()','browserViewport','detailWidth','detail()',
'spellTile','primaryAction','drawLoadout','firstEmptySlot','quickEquip','academySelector','academySummary','academyOfferHeader','enableScissor','mouseScrolled']:
    assert token in grimoire, token
assert grimoire.count('private void request(String next)') == 1
assert 'viewport().w()<410' in grimoire
summary=text(magic/'SpellEffectSummary.java'); definition=text(magic/'SpellDefinition.java')
assert 'SpellEffectSummary.summary(this)' in definition and '효과 · ' in definition
for token in ['case "wish"','기존 이로운','case "time_stop"','AI·이동','case "antimagic_field"','Arcane 시전',
'case "meteor_swarm"','16발','case "world_sunder"','방향성 실제 세계 균열','case "fly"','자유 비행',
'case "clone"','치명상','case "control_weather"','실제 폭우·뇌우','case "prismatic_wall"','14초 지속']:
    assert token in summary, token
assert summary.count('case "') >= 109

# Target snapshot / seeded meteor parity.
assert (magic/'CastTargetSnapshot.java').exists() and (magic/'MeteorBarragePattern.java').exists()
target=text(magic/'CastTargetSnapshot.java'); barrage=text(magic/'MeteorBarragePattern.java')
for token in ['targetEntityId','launchDirection','impactSurface','barrageSeed','dimension','executeLocked','resolvedTarget','boolean homing']:
    assert token in target, token
for token in ['BASE_STRIKES','impactTick','durationTicks','count()','strikes(long seed)','withSeed','castSeed','MIN_SEPARATION']:
    assert token in barrage, token
assert barrage.count('new Strike(') >= 16 and 'private static final Strike[] STRIKES' not in barrage
world_magic=text(magic/'WorldMagicService.java')
for token in ['captureSnapshot','CastTargetSnapshot snapshot','seed=%d','PLAYER_CHARGE_SEEDS','NPC_CHARGE_SEEDS','npcChargeSeed',
'npcReleaseSeed','consumeNpcSnapshot','MeteorBarragePattern.firstImpactTick(snapshot.barrageSeed())']:
    assert token in world_magic, token
kinetics=text(magic/'SpellKineticsService.java')
for token in ['CastTargetSnapshot targetSnapshot','captureSnapshot','targetSnapshot().validFor(player)','barrageSeed',
'MeteorBarragePattern.strike(targetSnapshot.barrageSeed(), 0)','applyPhysicalAftermath','ArcaneFieldService.handles',
'ArcaneFieldService.executeSpecial','ArcaneFieldService.blocksCasting','SpellGameplayService.handles',
'SpellGameplayService.execute','gameplayOwned','generic FIELD pulses must not restart']:
    assert token in kinetics, token
assert 'WorldMagicService.lockedTarget' not in kinetics and 'lockedTarget' not in kinetics

# Real gameplay runtime: this gate checks mechanics, not just switch-case existence.
assert (magic/'SpellGameplayService.java').exists()
gameplay=text(magic/'SpellGameplayService.java')
for token in ['LivingIncomingDamageEvent','event.setCanceled(true)','getAbilities().mayfly = true','onUpdateAbilities()',
'setNoAi(true)','wasNoAi','restoreControl','Attributes.SCALE','CommandSourceStack','LevelBasedPermissionSet.ADMIN',
'performPrefixedCommand','/weather thunder ','/weather clear ',
'DeathWard','"simulacrum"','"clone"','"control_weather"','"prismatic_wall"','"incendiary_cloud"',
'"wall_of_force"','"wall_of_ice"','"wind_wall"','"sleet_storm"','"insect_plague"','"flesh_to_stone"',
'"forcecage"','"true_polymorph"','"thunder_cage"','"astral_prison"','setTicksFrozen(0)',
'player.isAlliedTo(value)','SpellMetrics.wallWidth','snapshot.target()','snapshot.targetEntity(player)',
'DestructiveMagicService.impact(player, id, center, radius, power)','LIGHTNING_BOLT_THUNDER']:
    assert token in gameplay, token
assert 'setWeatherParameters' not in gameplay
ice=gameplay[gameplay.index('private static boolean iceKnife'):gameplay.index('private static boolean fireShield')]
assert 'DestructiveMagicService.impact' not in ice and 'flame_strike' not in ice
assert 'case "sleet_storm"' in gameplay and 'MobEffects.POISON' not in gameplay[gameplay.index('case "sleet_storm"'):gameplay.index('case "cloudkill"')]

# Visual lifetime must track actual sustained gameplay, especially Time Stop/Antimagic.
presentation=text(magic/'SpellPresentationProfile.java')
assert 'SpellGameplayService.visualDurationTicks(spell.id())' in presentation
audit_duration=gameplay[gameplay.index('public static int visualDurationTicks'):gameplay.index('/** Used by Arcane-field') if '/** Used by Arcane-field' in gameplay else gameplay.index('public static boolean blocksCasting')]
for token in ['case "time_stop" -> ArcaneFieldService.TIME_STOP_TICKS','case "antimagic_field" -> ArcaneFieldService.ANTIMAGIC_TICKS',
'case "prismatic_wall" -> 280','case "control_weather" -> 400']:
    assert token in audit_duration, token

# Destruction budgets/classification and explicit World Sunder fissure.
destruction=text(magic/'DestructiveMagicService.java')
for token in ['getDestroySpeed','getExplosionResistance','destroyBlock','hasChunkAt','MAX_BLOCK_CHANGES_PER_TICK',
'MAX_BLOCK_SCANS_PER_TICK','MAX_DROPPED_BLOCKS_PER_TICK','dropChangesRemaining','TerrainClass','MAJOR','CONDITIONAL',
'lightning_bolt','thunderwave','gust_of_wind','fissure','seven-point cut','case "world_sunder" -> fissure']:
    assert token in destruction, token
assert 'case "move_earth" -> new Profile(.54, 11.0, 170, false)' in destruction
assert 'case "earthquake" -> new Profile(.58, 14.5, 240, false)' in destruction
assert 'case "world_sunder" -> new Profile(.62, 28.0, 320, false)' in destruction
fusion=text(magic/'FusionSpellEffects.java')
assert 'DestructiveMagicService.impact(player, "world_sunder", center, radius, power)' in fusion
assert 'CastTargetSnapshot.targetOr(player, fallback)' in fusion
assert 'new AABB(lockedPoint, lockedPoint).inflate(2.4)' in fusion
assert 'targetEntity(player)' not in fusion

# Sustained Antimagic/Time Stop and Wish preservation.
field=text(magic/'ArcaneFieldService.java')
for token in ['TIME_STOP_TICKS = 120','ANTIMAGIC_TICKS = 240','activateAntimagic','activateTimeStop','fulfillWish',
'blocksCasting','setNoAi(true)','wasNoAi','restoreFrozenLevel','suppressMagicEffects','cleanseHarmful',
'SpellKineticsService.clear','clearFusion','SpellGameplayService.blocksCasting(caster)','SpellGameplayService.clear(entity.getUUID())']:
    assert token in field, token
assert 'removeAllEffects' not in field
harmful=field[field.index('private static void cleanseHarmful'):field.index('private static void restoreFrozenLevel')]
assert 'MobEffects.SPEED' not in harmful and 'MobEffects.REGENERATION' not in harmful and 'MobEffects.ABSORPTION' not in harmful
casting_service=text(magic/'SpellCastingService.java')
assert casting_service.count('ArcaneFieldService.blocksCasting(player)') >= 4
assert 'READY_HOLD_TIMEOUT_TICKS' in casting_service and 'chargeTimeoutTicks' in casting_service
assert '{0, 6, 10, 16, 26, 42, 68, 105, 155, 220}' in casting_service
assert 'equipped(player).castTimeMultiplier()' in casting_service and 'default -> 190;' in casting_service

npc=text(world/'NpcSpellResolver.java'); npc_barrage=text(world/'NpcMeteorBarrageService.java')
for token in ['consumeNpcSnapshot','NpcMeteorBarrageService.schedule','directAt','snapshot.launchOrigin()','ArcaneFieldService.blocksCasting(caster)']:
    assert token in npc, token
for token in ['MeteorBarragePattern.strike','barrageSeed','MAX_ACTIVE_BARRAGES','resolveStrike','nextStrike','ArcaneFieldService.blocksCasting(caster)']:
    assert token in npc_barrage, token
assert 'NpcMeteorBarrageService.tick' in main and 'NpcMeteorBarrageService.clearAll' in main
assert 'ArcaneFieldService.tick' in main and 'ArcaneFieldService.clearAll' in main
assert 'SpellGameplayService.tick' in main and 'SpellGameplayService.clearAll' in main
assert 'SpellGameplayService::onIncomingDamage' in main
assert main.index('ArcaneMageService.tickNear(player)') < main.index('ArcaneFieldService.tick')
assert main.index('SpellGameplayService.tick') < main.index('ArcaneFieldService.tick')
assert main.count('SpellKineticsService.clear(player.getUUID())') >= 2

# Existing HUD/gear/light/staff contracts.
hud=text(client/'ArcaneHud.java'); assert 'spell_ribbon' in hud and 'drawSeal' in hud and 'drawVitals' in hud and 'drawFusion' in hud
staff=text(root/'src/main/java/kr/moonseungjun/arcanecircle/item/ArcaneStaffItem.java')
assert 'castTimeMultiplier' in staff and '시전 전개시간' in staff
light=text(magic/'ArcaneLightService.java')
for token in ['Blocks.LIGHT','LightBlock.LEVEL','illuminate','clearAll']: assert token in light, token
assert 'ArcaneLightService.illuminate(player,1800)' in text(magic/'ExpandedSpellEffects.java')
assert 'syncAtomicRobe' in text(magic/'MageGearService.java')
assert not (magic/'SpellSigilService.java').exists()

# Active-tree hygiene: history is the archive.
repo=root.parents[1]
retired_tokens=[n.removesuffix('.java') for n in retired]
for path in (root/'src').rglob('*.java'):
    body=text(path)
    for token in retired_tokens: assert token not in body, f'{token} in {path.relative_to(root)}'
tools=root/'tools'
assert {p.name for p in tools.iterdir() if p.is_file()} == {'test_current_source.py','verify_jar.py'}
assert not [p for p in tools.iterdir() if p.is_dir()]
scripts=repo/'.github/scripts'
if scripts.exists():
    assert not (scripts/'arcane-circle').exists() and not list(scripts.glob('*arcane*'))
for obsolete in ['AUDIT_REPORT_V0.5.md','BUILD_AND_RUNTIME_REPORT.md','MAGIC_WORLD_PATCH.md','docs/ALPHA10_WORLD_COMBAT.md','docs/PRESENTATION_OVERHAUL_PHASES.md']:
    assert not (root/obsolete).exists(), obsolete

print('Arcane Circle current-source audit: PASS')
for name in ['target_snapshot_parity','seeded_meteor_barrage','destruction_tick_budget','destruction_drop_budget',
'world_sunder_fissure','npc_meteor_scheduler','non_homing_target_seal','sustained_antimagic','authoritative_time_stop',
'wish_preserves_beneficial_effects','grimoire_effect_summaries','persistent_spell_runtime','hard_control_restore',
'controlled_flight','death_substitution','actual_weather_control','persistent_wall_collision','visual_lifetime_parity']:
    print(name+'=PASS')
print('third_person_fake_geometry=absent')
print('prismatic_white_frame=absent')
print('retired_visual_stack=absent')
print('gameplay_content=preserved')
print('source_mutation=disabled')
print('legacy_arcane_tooling=absent')
