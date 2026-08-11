#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def t(p): return (ROOT/p).read_text(encoding='utf-8')
def need(s,x,l):
    if x not in s: raise SystemExit(f'{l}: missing {x!r}')
def forbid(s,x,l):
    if x in s: raise SystemExit(f'{l}: forbidden {x!r}')
props=t('gradle.properties'); main=t('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java'); arche=t('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellArchetype.java'); data=t('src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java'); casting=t('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java'); kinetics=t('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java'); world=t('src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java'); expanded=t('src/main/java/kr/moonseungjun/arcanecircle/magic/ExpandedSpellEffects.java'); high=t('src/main/java/kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.java'); mage=t('src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java'); resolver=t('src/main/java/kr/moonseungjun/arcanecircle/world/NpcSpellResolver.java'); survival=t('src/main/java/kr/moonseungjun/arcanecircle/world/MagicWorldService.java'); index=t('src/main/resources/data/arcanecircle/spell_catalog/index.json')
need(props,'mod_version=0.12.1-alpha.19','version'); need(main,'VERSION = "0.12.1-alpha.19"','runtime version'); need(index,'"version": "0.12.1-alpha.19"','catalog version')
need(arche,'"void_lance"','projectile execution'); forbid(arche,'"storm_of_vengeance", "meteor_swarm"','meteor field execution'); forbid(arche,'"prismatic_spray", "void_lance"','hidden channel multiplication')
forbid(casting,'age <= 1L','swallowed repeat input'); forbid(casting,'shouldBlockHotbarSwitch','legacy hotbar lock'); need(casting,'WorldMagicService.kineticDistance(player, spell, range)','shared distance'); need(kinetics,'SpellCastingService.kineticDistance(player, cast.spell(), cast.range())','kinetics distance')
need(world,'public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range)','visual metric export'); forbid(world,'Math.min(80.0, Math.max(2.0, range))','visual range cap'); forbid(world,'Math.min(Math.max(3.0, range), 72.0)','front range cap')
need(data,'int maximum = 100_000;','fusion mastery persistence'); need(data,'Math.max(mastery.getOrDefault(formula.result(), 0), required)','fusion mastery preservation'); need(data,'circle >= SpellCatalog.IMPLEMENTED_MAX_CIRCLE','circle HUD')
need(expanded,'public static boolean safeTeleport','safe teleport');
for old in ('academyReturn(player','longTeleport(player'): forbid(high,old,'disabled teleport')
for spell in ('plane_shift','demiplane','teleport','gate'): need(high,f'case "{spell}" -> ExpandedSpellEffects.safeTeleport',f'{spell} travel')
forbid(high,'Vec3 p = center.add(right.scale(i / 24.0 * half))','dead prismatic loop')
need(mage,'NpcSpellResolver.impactDelay','NPC timing'); need(mage,'NpcSpellResolver.execute','NPC impact'); need(mage,'boolean released, long impactAt','NPC flight state'); forbid(mage,'ArcaneDamage.hurt(level, caster, target, (float) cast.power())','generic immediate NPC damage')
need(resolver,'SpellPresentationProfile.profile(spell).motion()','NPC motion resolver'); need(resolver,'SpellMetrics.effectRadius','NPC area metrics')
forbid(survival,'setFoodLevel(20)','forced hunger'); forbid(survival,'setSaturation(20.0F)','forced saturation'); need(index,'"vanilla hunger"','survival contract')
print('Arcane Circle alpha.19 runtime integrity audit: PASS')
