#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match in {path}: {count}\n--- OLD ---\n{old[:800]}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all_existing(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    if "mod_version=0.18.12-alpha.1" not in props:
        raise SystemExit("expected v0.18.12 source version")
    for test in (ROOT / "tools").glob("test_*.py"):
        replace_all_existing(test, "mod_version=0.18.11-alpha.1", "mod_version=0.18.12-alpha.1")

    turret = JAVA / "VillagePlacedTurretSystem.java"
    replace_once(turret,
'''        double range = state.type().range() + (state.level() - 1) * 2.5;\n        Vec3 muzzle = Vec3.atCenterOf(state.pos().above());\n        List<Mob> candidates = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null)\n                .stream().filter(mob -> VillageDefenseLineOfSight.hasLine(level, muzzle, mob)).toList();''',
'''        double range = state.type().range() + (state.level() - 1) * 2.5;\n        List<Mob> candidates = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null)\n                .stream().filter(mob -> VillageDefenseLineOfSight.hasLine(level, turretMuzzle(state, mob), mob)).toList();''')
    replace_once(turret,
'''            target = candidates.stream().filter(mob -> mob.getY() > baseY + 6.0)\n                    .findFirst().orElse(target);''',
'''            target = candidates.stream().filter(mob -> mob.getY() > baseY + 6.0)\n                    .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(state.pos()))))\n                    .orElse(target);''')
    replace_once(turret,
'''            case CHAIN -> {\n                List<Mob> chain = VillageRaidSystem.activeEnemiesNear(level, target.position(), 7.5,\n                        2 + state.level() / 2, null);\n                for (Mob mob : chain) hit(level, state, mob, damage * 0.78f, ParticleTypes.ELECTRIC_SPARK);\n            }''',
'''            case CHAIN -> {\n                List<Mob> chain = VillageRaidSystem.activeEnemiesNear(level, target.position(), 7.5,\n                        2 + state.level() / 2, null);\n                Vec3 arcStart = turretMuzzle(state, target);\n                for (Mob mob : chain) {\n                    if (!VillageDefenseLineOfSight.hasLine(level, arcStart, mob)) continue;\n                    hitFrom(level, arcStart, mob, damage * 0.78f, ParticleTypes.ELECTRIC_SPARK);\n                    arcStart = mob.position().add(0, mob.getBbHeight() * 0.55, 0);\n                }\n            }''')
    replace_once(turret,
'''    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,\n                            net.minecraft.core.particles.ParticleOptions particle) {\n        Vec3 start = Vec3.atCenterOf(state.pos().above());\n        if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;\n        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);\n        for (int i = 0; i <= 7; i++) {\n            Vec3 point = start.lerp(end, i / 7.0);\n            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);\n        }\n        target.hurtServer(level, level.damageSources().magic(), damage);\n    }''',
'''    private static Vec3 turretMuzzle(TurretState state, Mob target) {\n        Vec3 capCenter = Vec3.atCenterOf(state.pos().above(2));\n        Vec3 delta = target.position().subtract(capCenter);\n        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);\n        if (horizontal < 1.0e-4) {\n            return new Vec3(capCenter.x, state.pos().getY() + 3.05, capCenter.z);\n        }\n        return capCenter.add(delta.x / horizontal * 0.72, 0.22, delta.z / horizontal * 0.72);\n    }\n\n    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,\n                            net.minecraft.core.particles.ParticleOptions particle) {\n        hitFrom(level, turretMuzzle(state, target), target, damage, particle);\n    }\n\n    private static void hitFrom(ServerLevel level, Vec3 start, Mob target, float damage,\n                                net.minecraft.core.particles.ParticleOptions particle) {\n        if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;\n        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);\n        for (int i = 0; i <= 8; i++) {\n            Vec3 point = start.lerp(end, i / 8.0);\n            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);\n        }\n        level.sendParticles(particle, end.x, end.y, end.z, 5, 0.18, 0.22, 0.18, 0.02);\n        target.hurtServer(level, level.damageSources().magic(), damage);\n    }''')

    merc = JAVA / "VillageMercenarySystem.java"
    replace_once(merc,
'''    private static synchronized MercenaryClass mercenaryClass(Mob mob) {\n        return CLASSES.getOrDefault(mob.getUUID(), MercenaryClass.BASTION);\n    }''',
'''    public static synchronized MercenaryClass classOf(Mob mob) {\n        return mob == null ? null : CLASSES.get(mob.getUUID());\n    }\n\n    private static synchronized MercenaryClass mercenaryClass(Mob mob) {\n        MercenaryClass kind = classOf(mob);\n        return kind == null ? MercenaryClass.BASTION : kind;\n    }''')

    deploy = JAVA / "VillageMercenaryDeploymentSystem.java"
    replace_once(deploy, "import net.minecraft.ChatFormatting;\n", "")
    replace_once(deploy,
'''    public static void tick(MinecraftServer server) {\n        if (server == null || ++ticks < 20) return;\n        ticks = 0;''',
'''    public static void tick(MinecraftServer server) {\n        if (server == null || ++ticks < 5) return;\n        ticks = 0;''')
    replace_once(deploy,
'''        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area,\n                mob -> classFromName(mob) == kind && mob.isAlive())) {''',
'''        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area,\n                mob -> VillageMercenarySystem.classOf(mob) == kind && mob.isAlive())) {''')
    replace_once(deploy,
'''            if (force || !VillageRaidSystem.isActive() || golem.blockPosition().distSqr(rally) > leash * leash) {\n                boolean accepted = golem.getNavigation().moveTo(rally.getX() + 0.5, rally.getY(), rally.getZ() + 0.5,\n                        kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 1.18 : 1.02);\n                if (!accepted && zone == Deployment.WALL) {\n                    BlockPos fallback = rallyPoint(center, Deployment.INNER, kind);\n                    golem.getNavigation().moveTo(fallback.getX() + 0.5, fallback.getY(), fallback.getZ() + 0.5, 1.0);\n                }\n            }\n            if (!VillageRaidSystem.isActive()) continue;''',
'''            boolean returningToRally = force || !VillageRaidSystem.isActive()\n                    || golem.blockPosition().distSqr(rally) > leash * leash;\n            if (returningToRally) {\n                boolean accepted = golem.getNavigation().moveTo(rally.getX() + 0.5, rally.getY(), rally.getZ() + 0.5,\n                        kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 1.18 : 1.02);\n                if (!accepted && zone == Deployment.WALL) {\n                    BlockPos fallback = rallyPoint(center, Deployment.INNER, kind);\n                    golem.getNavigation().moveTo(fallback.getX() + 0.5, fallback.getY(), fallback.getZ() + 0.5, 1.0);\n                }\n            }\n            if (!VillageRaidSystem.isActive()) continue;''')
    replace_once(deploy,
'''            } else if (kind == VillageMercenarySystem.MercenaryClass.RANGER\n                    || kind == VillageMercenarySystem.MercenaryClass.MEDIC) {\n                golem.setTarget(null);\n            }''',
'''            } else if (kind == VillageMercenarySystem.MercenaryClass.RANGER\n                    || kind == VillageMercenarySystem.MercenaryClass.MEDIC) {\n                golem.setTarget(null);\n                if (!returningToRally) golem.getNavigation().stop();\n            }''')
    replace_once(deploy,
'''    private static VillageMercenarySystem.MercenaryClass classFromName(IronGolem golem) {\n        Component name = golem.getCustomName();\n        if (name == null) return null;\n        String plain = ChatFormatting.stripFormatting(name.getString());\n        if (plain == null) return null;\n        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {\n            if (plain.startsWith(kind.displayName())) return kind;\n        }\n        return null;\n    }\n\n''', "")

    aspect = JAVA / "VillageBossAspectSystem.java"
    replace_once(aspect,
'''            case BLOODBOUND -> {\n                if (globalTicks % 100 != 0) return;''',
'''            case BLOODBOUND -> {\n                if (globalTicks % 100 == 85) {\n                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,\n                            mob.getX(), mob.getY() + 1.0, mob.getZ(), 18, 1.0, 0.7, 1.0, 0.03);\n                    return;\n                }\n                if (globalTicks % 100 != 0) return;''')
    replace_once(aspect,
'''            case STORMCALLER -> {\n                if (globalTicks % 80 != 0) return;\n                ServerPlayer target = nearbyPlayers(server, mob, 18.0).stream()''',
'''            case STORMCALLER -> {\n                if (globalTicks % 80 == 65) {\n                    ServerPlayer warning = nearbyPlayers(server, mob, 18.0).stream()\n                            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);\n                    if (warning != null) {\n                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,\n                                warning.getX(), warning.getY() + 0.15, warning.getZ(),\n                                18, 0.8, 0.08, 0.8, 0.03);\n                    }\n                    return;\n                }\n                if (globalTicks % 80 != 0) return;\n                ServerPlayer target = nearbyPlayers(server, mob, 18.0).stream()''')
    replace_once(aspect,
'''                .filter(player -> player.level() == mob.level() && player.isAlive()\n                        && !player.isSpectator() && player.distanceToSqr(mob) <= squared)''',
'''                .filter(player -> player.level() == mob.level() && player.isAlive()\n                        && !player.isSpectator() && !VillageRespawnSystem.isDowned(player)\n                        && player.distanceToSqr(mob) <= squared)''')

    # v0.18.11's LOS assertions reference the old self-blocking muzzle local; preserve the intent with the new helper.
    old_test = ROOT / "tools/test_v01811_defense_polish.py"
    replace_all_existing(old_test,
        'assert ".filter(mob -> VillageDefenseLineOfSight.hasLine(level, muzzle, mob))" in turret',
        'assert ".filter(mob -> VillageDefenseLineOfSight.hasLine(level, turretMuzzle(state, mob), mob))" in turret')
    replace_all_existing(old_test,
        'assert "if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;" in turret',
        'assert "if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;" in turret and "turretMuzzle" in turret')

    print("[OK] remaining v0.18.12 manual-audit fixes applied")


if __name__ == "__main__":
    main()
