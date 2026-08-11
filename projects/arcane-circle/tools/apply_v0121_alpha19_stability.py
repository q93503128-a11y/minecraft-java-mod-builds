#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    if new in text and old not in text:
        return
    if old not in text:
        raise SystemExit(f"{label}: source marker missing")
    write(path, text.replace(old, new, 1))


def replace_between(path: Path, start: str, end: str, replacement: str, label: str) -> None:
    text = read(path)
    a = text.find(start)
    b = text.find(end, a + len(start))
    if a < 0 or b < 0:
        if replacement.strip() and replacement.strip() in text:
            return
        raise SystemExit(f"{label}: method markers missing")
    write(path, text[:a] + replacement + text[b:])


def patch_version() -> None:
    replace_once(ROOT / "gradle.properties", "mod_version=0.12.1-alpha.18", "mod_version=0.12.1-alpha.19", "gradle version")
    replace_once(JAVA / "ArcaneCircle.java", 'VERSION = "0.12.1-alpha.18"', 'VERSION = "0.12.1-alpha.19"', "runtime version")
    index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
    index = json.loads(read(index_path))
    index["version"] = "0.12.1-alpha.19"
    index["survival_replacement"] = ["vanilla hunger", "vanilla survival respawn", "magic progression", "arcana economy"]
    write(index_path, json.dumps(index, ensure_ascii=False, indent=2) + "\n")


def patch_execution_modes() -> None:
    path = JAVA / "magic/SpellArchetype.java"
    write(path, '''package kr.moonseungjun.arcanecircle.magic;\n\nimport java.util.Set;\n\n/** Server execution cadence. Visual motion lives in SpellPresentationProfile. */\npublic final class SpellArchetype {\n    private static final Set<String> PROJECTILES = Set.of(\n            "arcane_dart", "ember", "frost_needle", "flame_lance", "ice_lance", "fireball",\n            "triune_barrage", "meteor_shard", "magic_missile", "fire_bolt", "ice_knife",\n            "chromatic_orb", "delayed_blast_fireball", "freezing_sphere", "arcane_hand",\n            "void_lance");\n\n    private static final Set<String> CHANNELS = Set.of(\n            "lightning_arc", "mana_lance", "chain_bolt", "arcane_annihilation", "ray_of_frost");\n\n    private static final Set<String> FIELDS = Set.of(\n            "frost_nova", "phoenix_field", "blizzard_field", "thunder_prison",\n            "inferno_domain", "absolute_zero", "tempest_domain", "aegis_citadel",\n            "wall_of_fire", "cloudkill", "sleet_storm", "antimagic_field",\n            "storm_of_vengeance", "winter_domain", "time_stop");\n\n    private SpellArchetype() {}\n\n    public static Mode mode(String spellId) {\n        if (PROJECTILES.contains(spellId)) return Mode.PROJECTILE;\n        if (CHANNELS.contains(spellId)) return Mode.CHANNEL;\n        if (FIELDS.contains(spellId)) return Mode.FIELD;\n        return Mode.INSTANT;\n    }\n\n    public enum Mode {\n        INSTANT("즉", "instant"),\n        PROJECTILE("탄", "projectile"),\n        CHANNEL("집", "channel"),\n        FIELD("장", "field");\n        private final String badge;\n        private final String key;\n        Mode(String badge, String key) { this.badge = badge; this.key = key; }\n        public String badge() { return badge; }\n        public String key() { return key; }\n    }\n}\n''')


def patch_player_state() -> None:
    path = JAVA / "magic/MagicPlayerData.java"
    replace_once(path, '''                            int maximum = SpellCatalog.isFusionResult(value.spellId())\n                                    ? SpellCatalog.masteryRequired(value.spellId()) : 100_000;\n                            mastery.put(value.spellId(), Math.min(maximum, value.casts()));''', '''                            int maximum = 100_000;\n                            mastery.put(value.spellId(), Math.min(maximum, value.casts()));''', "fusion mastery load cap")
    replace_once(path, '                if (known.contains(formula.result())) mastery.put(formula.result(), required);', '                if (known.contains(formula.result()))\n                    mastery.put(formula.result(), Math.max(mastery.getOrDefault(formula.result(), 0), required));', "fusion mastery preservation")
    replace_once(path, '            return circle >= 5 ? 0 : SpellCatalog.circleInsightThreshold(circle + 1);', '            return circle >= SpellCatalog.IMPLEMENTED_MAX_CIRCLE\n                    ? 0 : SpellCatalog.circleInsightThreshold(circle + 1);', "next-circle insight")


def patch_casting_and_visual_distance() -> None:
    casting = JAVA / "magic/SpellCastingService.java"
    replace_once(casting, '''            long age = now - existing.startedAt;\n            if (existing.slot == slot && existing.spellId.equals(cast.spell().id()) && age <= 1L) return;\n            CHARGES.remove(player.getUUID());''', '            CHARGES.remove(player.getUUID());', "duplicate begin suppression")
    text = read(casting)
    obsolete = '''    public static boolean shouldBlockHotbarSwitch(ServerPlayer player) {\n        return CHARGES.containsKey(player.getUUID());\n    }\n\n'''
    if obsolete in text: write(casting, text.replace(obsolete, "", 1))
    replace_between(casting, "    static double kineticDistance(ServerPlayer player, double range) {", "    private static MagicPlayerData data(ServerPlayer player) {", '''    static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {\n        return WorldMagicService.kineticDistance(player, spell, range);\n    }\n\n''', "shared kinetic distance")
    kinetics = JAVA / "magic/SpellKineticsService.java"
    replace_once(kinetics, "SpellCastingService.kineticDistance(player, cast.range())", "SpellCastingService.kineticDistance(player, cast.spell(), cast.range())", "kinetics shared distance")
    world = JAVA / "magic/WorldMagicService.java"
    text = read(world)
    text = text.replace("visiblePoint(player, look, Math.min(Math.max(3.0, range), 72.0))", "visiblePoint(player, look, Math.max(3.0, range))")
    text = text.replace("double max = Math.min(80.0, Math.max(2.0, range));", "double max = Math.max(2.0, range);")
    text = text.replace("double max = Math.min(Math.max(2.0, range), 72.0);", "double max = Math.max(2.0, range);")
    marker = "    private static double kineticDistanceForVisual(ServerPlayer player, SpellDefinition spell, double range,\n"
    if "public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range)" not in text:
        if marker not in text: raise SystemExit("world visual distance insertion marker missing")
        method = '''    public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {\n        Vec3 direction = safeDirection(player.getLookAngle());\n        Vec3 target = targetPoint(player, spell, range, direction);\n        Vec3 center = presentationCenter(player, spell, target, direction);\n        return kineticDistanceForVisual(player, spell, range, center, target);\n    }\n\n'''
        text = text.replace(marker, method + marker, 1)
    write(world, text)


def patch_safe_teleports_and_survival() -> None:
    expanded = JAVA / "magic/ExpandedSpellEffects.java"
    text = read(expanded)
    anchor = "    private static boolean missile(ServerPlayer player, double range, double power, ParticleOptions particle,\n"
    helper = '''    public static boolean safeTeleport(ServerPlayer player, double range, double power, int tier) {\n        return teleport(player, Math.max(2.0, range), power, Math.max(0, tier));\n    }\n\n'''
    if helper not in text:
        if anchor not in text: raise SystemExit("safe teleport insertion marker missing")
        text = text.replace(anchor, helper + anchor, 1)
    write(expanded, text)
    high = JAVA / "magic/HighCircleSpellEffects.java"
    text = read(high)
    swaps = {
        'case "plane_shift" -> academyReturn(player, "차원 이동");': 'case "plane_shift" -> ExpandedSpellEffects.safeTeleport(player, Math.max(range, 36.0), power, 4);',
        'case "demiplane" -> academyReturn(player, "반차원");': 'case "demiplane" -> ExpandedSpellEffects.safeTeleport(player, Math.max(range, 48.0), power, 5);',
        'case "teleport" -> longTeleport(player, range);': 'case "teleport" -> ExpandedSpellEffects.safeTeleport(player, range, power, 4);',
        'case "gate" -> academyReturn(player, "게이트");': 'case "gate" -> ExpandedSpellEffects.safeTeleport(player, Math.max(range, 64.0), power, 6);',
    }
    for old, new in swaps.items():
        if old in text: text = text.replace(old, new, 1)
        elif new not in text: raise SystemExit(f"high-circle teleport marker missing: {old}")
    write(high, text)
    replace_between(high, "    private static boolean academyReturn(ServerPlayer player, String name) {", "    private static boolean prismaticSpray(ServerPlayer player, double range, double power) {", "", "retired academy return")
    replace_between(high, "    private static boolean longTeleport(ServerPlayer player, double range) {", "    private static boolean antimagic(ServerPlayer player, double range) {", "", "unsafe long teleport")
    text = read(high)
    dead = '''        List<ParticleOptions> particles = List.of(ParticleTypes.FLAME, ParticleTypes.SNOWFLAKE,\n                ParticleTypes.ELECTRIC_SPARK, ParticleTypes.WITCH, ParticleTypes.END_ROD);\n        double half = Math.max(12.0, range * 0.25);\n        for (int layer = 0; layer < particles.size(); layer++) {\n            for (int i = -24; i <= 24; i++) {\n                Vec3 p = center.add(right.scale(i / 24.0 * half)).add(0, layer * 1.1, 0);\n\n            }\n        }\n'''
    if dead in text: text = text.replace(dead, '        double half = Math.max(12.0, range * 0.25);\n', 1)
    write(high, text)
    survival = JAVA / "world/MagicWorldService.java"
    text = read(survival).replace("        player.getFoodData().setFoodLevel(20);\n", "").replace("        player.getFoodData().setSaturation(20.0F);\n", "")
    write(survival, text)


def patch_npc_runtime() -> None:
    resolver = JAVA / "world/NpcSpellResolver.java"
    write(resolver, '''package kr.moonseungjun.arcanecircle.world;\n\nimport kr.moonseungjun.arcanecircle.magic.ArcaneDamage;\nimport kr.moonseungjun.arcanecircle.magic.SpellDefinition;\nimport kr.moonseungjun.arcanecircle.magic.SpellMetrics;\nimport kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.entity.LivingEntity;\nimport net.minecraft.world.entity.Mob;\nimport net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.Vec3;\n\nfinal class NpcSpellResolver {\n    private NpcSpellResolver() {}\n    static int impactDelay(Mob caster, LivingEntity target, SpellDefinition spell) {\n        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);\n        double distance = switch (profile.motion()) {\n            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE -> caster.getEyePosition().distanceTo(target.getEyePosition());\n            case SKY_DROP -> profile.skyHeight();\n            default -> 0.0;\n        };\n        return SpellPresentationProfile.impactDelayTicks(spell, distance);\n    }\n    static boolean execute(ServerLevel level, Mob caster, LivingEntity target, SpellDefinition spell, double range, double power) {\n        if (target == null || !target.isAlive() || caster.isAlliedTo(target)) return false;\n        return switch (SpellPresentationProfile.profile(spell).motion()) {\n            case SKY_DROP, STORM, FIELD -> area(level, caster, target.position(), spell, range, power);\n            case WAVE -> wave(level, caster, target, spell, range, power);\n            case WALL -> wall(level, caster, target.position(), spell, range, power);\n            case BEAM, LANCE -> line(level, caster, target, power);\n            default -> direct(level, caster, target, power);\n        };\n    }\n    private static boolean direct(ServerLevel level, Mob caster, LivingEntity target, double power) { ArcaneDamage.hurt(level, caster, target, (float) power); return true; }\n    private static boolean line(ServerLevel level, Mob caster, LivingEntity target, double power) {\n        Vec3 start=caster.getEyePosition(), end=target.getEyePosition(), delta=end.subtract(start); double length=Math.max(.001,delta.length()); Vec3 unit=delta.scale(1.0/length); boolean hit=false;\n        for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1.25),value->valid(caster,value))){Vec3 relative=entity.getEyePosition().subtract(start);double projection=relative.dot(unit);if(projection<0||projection>length)continue;double width=Math.max(.8,entity.getBbWidth()*.65+.55);if(relative.subtract(unit.scale(projection)).lengthSqr()>width*width)continue;ArcaneDamage.hurt(level,caster,entity,(float)power);hit=true;}\n        return hit;\n    }\n    private static boolean wave(ServerLevel level,Mob caster,LivingEntity target,SpellDefinition spell,double range,double power){Vec3 origin=caster.position().add(0,caster.getBbHeight()*.45,0),direction=target.position().subtract(origin);if(direction.lengthSqr()<1e-8)return direct(level,caster,target,power);direction=direction.normalize();double length=Math.max(4,range),endRadius=SpellMetrics.waveEndRadius(spell.id(),range,spell.circle());boolean hit=false;for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,caster.getBoundingBox().inflate(length),value->valid(caster,value))){Vec3 relative=entity.position().add(0,entity.getBbHeight()*.45,0).subtract(origin);double projection=relative.dot(direction);if(projection<0||projection>length)continue;double allowed=Math.max(.8,endRadius*(projection/length))+entity.getBbWidth()*.5;if(relative.subtract(direction.scale(projection)).lengthSqr()>allowed*allowed)continue;ArcaneDamage.hurt(level,caster,entity,(float)power);hit=true;}return hit;}\n    private static boolean area(ServerLevel level,Mob caster,Vec3 center,SpellDefinition spell,double range,double power){double radius=Math.min(24,Math.max(3,SpellMetrics.effectRadius(spell.id(),range,spell.circle())));boolean hit=false;AABB box=new AABB(center,center).inflate(radius,Math.max(4,radius*.70),radius);for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,box,value->valid(caster,value))){ArcaneDamage.hurt(level,caster,entity,(float)power);hit=true;}return hit;}\n    private static boolean wall(ServerLevel level,Mob caster,Vec3 center,SpellDefinition spell,double range,double power){double halfWidth=Math.min(20,Math.max(4,SpellMetrics.effectRadius(spell.id(),range,spell.circle())));Vec3 forward=center.subtract(caster.position());forward=new Vec3(forward.x,0,forward.z);if(forward.lengthSqr()<1e-8)forward=new Vec3(0,0,1);Vec3 forwardUnit=forward.normalize(),right=new Vec3(-forwardUnit.z,0,forwardUnit.x);boolean hit=false;for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(halfWidth+1.5,5,halfWidth+1.5),value->valid(caster,value))){Vec3 delta=entity.position().subtract(center);double lateral=Math.abs(delta.dot(right)),depth=Math.abs(delta.dot(forwardUnit));if(lateral>halfWidth+entity.getBbWidth()||depth>1.8+entity.getBbWidth())continue;ArcaneDamage.hurt(level,caster,entity,(float)power);hit=true;}return hit;}\n    private static boolean valid(Mob caster,LivingEntity target){return target!=caster&&target.isAlive()&&!target.isRemoved()&&!caster.isAlliedTo(target);}\n}\n''')
    mage = JAVA / "world/ArcaneMageService.java"
    replace_once(mage, '''        NpcCast interrupted = CASTS.remove(mageId);\n        if (interrupted != null) WorldMagicService.stop(mage);''', '''        NpcCast interrupted = CASTS.get(mageId);\n        if (interrupted != null && !interrupted.released()) {\n            CASTS.remove(mageId);\n            WorldMagicService.stop(mage);\n        }''', "released NPC cast interruption")
    replace_once(mage, '''        CASTS.put(caster.getUUID(), new NpcCast(target.getUUID(), visual.id(), now,\n                required, range, power, hostile));''', '''        CASTS.put(caster.getUUID(), new NpcCast(target.getUUID(), visual.id(), now,\n                required, range, power, hostile, false, -1L));''', "NPC cast state")
    replacement = '''    private static boolean tickCast(ServerLevel level, Mob caster, LivingEntity fallbackTarget,\n                                    MageProfile profile, long now, boolean hostile) {\n        NpcCast cast = CASTS.get(caster.getUUID());\n        if (cast == null) return false;\n        Entity rawTarget = level.getEntity(cast.targetId());\n        LivingEntity target = rawTarget instanceof LivingEntity living ? living : fallbackTarget;\n        if (target == null || !target.isAlive() || caster.distanceToSqr(target) > 48.0 * 48.0) {\n            CASTS.remove(caster.getUUID());\n            if (!cast.released()) WorldMagicService.stop(caster);\n            return false;\n        }\n        caster.setTarget(target);\n        caster.getLookControl().setLookAt(target, 35.0F, 35.0F);\n        SpellDefinition spell = SpellCatalog.spell(cast.spellId()).orElseGet(() -> chooseCombatSpell(caster, profile));\n        if (cast.released()) {\n            if (now < cast.impactAt()) return true;\n            finishNpcImpact(level, caster, target, spell, cast, profile, now);\n            return true;\n        }\n        long elapsed = now - cast.startedAt();\n        double progress = Math.min(1.0, elapsed / (double) Math.max(1, cast.requiredTicks()));\n        WorldMagicService.charge(caster, target, spell, progress, cast.range(), cast.power());\n        if (elapsed < cast.requiredTicks()) return true;\n        WorldMagicService.release(caster, target, spell, cast.range(), cast.power());\n        int impactDelay = NpcSpellResolver.impactDelay(caster, target, spell);\n        if (impactDelay > 1) {\n            CASTS.put(caster.getUUID(), new NpcCast(cast.targetId(), cast.spellId(), cast.startedAt(), cast.requiredTicks(), cast.range(), cast.power(), cast.hostile(), true, now + impactDelay));\n            return true;\n        }\n        finishNpcImpact(level, caster, target, spell, cast, profile, now);\n        return true;\n    }\n\n    private static void finishNpcImpact(ServerLevel level, Mob caster, LivingEntity target, SpellDefinition spell, NpcCast cast, MageProfile profile, long now) {\n        CASTS.remove(caster.getUUID());\n        LAST_CAST.put(caster.getUUID(), now);\n        boolean executed = NpcSpellResolver.execute(level, caster, target, spell, cast.range(), cast.power());\n        if (executed) { if (target instanceof Mob mob) mob.setTarget(caster); applyControl(caster, target, profile); }\n        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, cast.hostile() ? SoundSource.HOSTILE : SoundSource.NEUTRAL, 0.78F, 1.25F - profile.circle() * 0.03F);\n        if (RETALIATION_TARGET.containsKey(caster.getUUID())) AGGRO_UNTIL.put(caster.getUUID(), Math.max(AGGRO_UNTIL.getOrDefault(caster.getUUID(), 0L), now + RETALIATION_TICKS / 2L));\n    }\n\n'''
    replace_between(mage, "    private static boolean tickCast(ServerLevel level, Mob caster, LivingEntity fallbackTarget,", "    private static LivingEntity findResidentTarget(ServerLevel level, Villager caster,", replacement, "NPC impact lifecycle")
    replace_once(mage, '''    private record NpcCast(UUID targetId, String spellId, long startedAt, int requiredTicks,\n                           double range, double power, boolean hostile) {}''', '''    private record NpcCast(UUID targetId, String spellId, long startedAt, int requiredTicks,\n                           double range, double power, boolean hostile, boolean released, long impactAt) {}''', "NPC cast record")


def main() -> None:
    patch_version(); patch_execution_modes(); patch_player_state(); patch_casting_and_visual_distance(); patch_safe_teleports_and_survival(); patch_npc_runtime()
    print("Arcane Circle alpha.19 one-time stability migration: PASS")

if __name__ == "__main__": main()
