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
    replace_once(ROOT / "gradle.properties", "mod_version=0.18.11-alpha.1", "mod_version=0.18.12-alpha.1")
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

    boss = JAVA / "VillageSiegeBossSystem.java"
    replace_once(boss,
'''        server.getPlayerList().broadcastSystemMessage(Component.literal(\n                "§4[보스 2페이즈] §f" + doctrine.displayName() + "의 전투 방식이 격화됩니다. · "\n                        + doctrine.phaseTwo()), false);''',
'''        if (mob.level() instanceof ServerLevel level) {\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,\n                    mob.getX(), mob.getY() + 1.2, mob.getZ(), 42, 1.6, 1.0, 1.6, 0.08);\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,\n                    mob.getX(), mob.getY() + 0.8, mob.getZ(), 5, 0.8, 0.4, 0.8, 0.02);\n        }\n        server.getPlayerList().broadcastSystemMessage(Component.literal(\n                "§4[보스 2페이즈] §f" + doctrine.displayName() + "의 전투 방식이 격화됩니다. · "\n                        + doctrine.phaseTwo()), false);''')
    replace_once(boss,
'''        VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.primarySideFor(front);\n        if (segment == VillageSiegeSegmentSystem.Segment.NORTH_GATE && front == VillageAttackPlanSystem.Front.NORTH) return;\n        BlockPos target = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());\n        if (!VillageSiegeSegmentSystem.breached(segment)) {\n            mob.setTarget(null);\n            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.18);\n            if (ticks % 45 == 0 && VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) {\n                int damage = PHASE_TWO.contains(mob.getUUID()) ? 72 : 48;\n                VillageSiegeSegmentSystem.damage(server, segment, damage, mob.blockPosition());\n            }\n        }''',
'''        VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.primarySideFor(front);\n        BlockPos target = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());\n        if (!VillageSiegeSegmentSystem.breached(segment)) {\n            mob.setTarget(null);\n            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.18);\n            if (!VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) return;\n            boolean phaseTwo = PHASE_TWO.contains(mob.getUUID());\n            int interval = phaseTwo ? 30 : 45;\n            int offset = Math.floorMod(mob.getUUID().hashCode(), interval);\n            int phase = Math.floorMod(ticks - offset, interval);\n            ServerLevel level = server.overworld();\n            if (phase == interval - 10) {\n                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,\n                        target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,\n                        18, 1.2, 0.5, 1.2, 0.03);\n                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,\n                        mob.getX(), mob.getY() + 1.1, mob.getZ(), 14, 0.6, 0.6, 0.6, 0.03);\n            }\n            if (phase == 0) {\n                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);\n                int damage = phaseTwo ? 72 : 48;\n                VillageSiegeSegmentSystem.damage(server, segment, damage, mob.blockPosition());\n                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,\n                        target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,\n                        5, 1.0, 0.5, 1.0, 0.02);\n            }\n        }''')
    replace_once(boss,
'''    private static void tickRitual(ServerLevel level, Mob boss) {\n        if (ticks % 120 != Math.floorMod(boss.getUUID().hashCode(), 120)) return;\n        for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, boss.position(), 15.0, 20, boss.getUUID())) {''',
'''    private static void tickRitual(ServerLevel level, Mob boss) {\n        int offset = Math.floorMod(boss.getUUID().hashCode(), 120);\n        int phase = Math.floorMod(ticks - offset, 120);\n        if (phase == 100) {\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,\n                    boss.getX(), boss.getY() + 1.3, boss.getZ(), 30, 2.4, 1.0, 2.4, 0.04);\n            return;\n        }\n        if (phase != 0) return;\n        for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, boss.position(), 15.0, 20, boss.getUUID())) {''')
    replace_once(boss,
'''        boss.setTarget(target);\n        boss.getNavigation().moveTo(target, PHASE_TWO.contains(boss.getUUID()) ? 1.58 : 1.34);\n        if (ticks % 105 == 0) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 1));''',
'''        boss.setTarget(target);\n        boss.getNavigation().moveTo(target, PHASE_TWO.contains(boss.getUUID()) ? 1.58 : 1.34);\n        ServerLevel level = server.overworld();\n        if (ticks % 105 == 70) {\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,\n                    target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.6, 0.8, 0.6, 0.02);\n        }\n        if (ticks % 105 == 0) {\n            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 1));\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,\n                    target.getX(), target.getY() + 0.8, target.getZ(), 16, 0.5, 0.5, 0.5, 0.04);\n        }''')
    replace_once(boss,
'''        BREACH_COLOSSUS("파성 거신", "성벽 구역을 직접 목표로 삼고 50% 이하에서 파쇄 주기가 빨라집니다.",\n                "성벽 파쇄 피해 50% 증가"),''',
'''        BREACH_COLOSSUS("파성 거신", "성벽 구역을 직접 목표로 삼고 50% 이하에서 파쇄 주기가 빨라집니다.",\n                "파쇄 주기 45→30틱 · 파쇄 피해 50% 증가"),''')

    elite = JAVA / "VillageEnemyEliteSystem.java"
    replace_once(elite,
'''    private static void grappler(ServerLevel level, Mob mob) {\n        if (ticks % 100 != Math.floorMod(mob.getUUID().hashCode(), 100)) return;\n        VillageAttackPlanSystem.Front front = VillageAttackPlanSystem.frontOf(mob.getUUID());''',
'''    private static void grappler(ServerLevel level, Mob mob) {\n        int offset = Math.floorMod(mob.getUUID().hashCode(), 100);\n        int phase = Math.floorMod(ticks - offset, 100);\n        VillageAttackPlanSystem.Front front = VillageAttackPlanSystem.frontOf(mob.getUUID());''')
    replace_once(elite,
'''        BlockPos wall = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());\n        if (mob.blockPosition().distSqr(wall) > 144.0) return;\n        BlockPos inside = VillageSiegeSegmentSystem.insideApproach(segment);''',
'''        BlockPos wall = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());\n        if (mob.blockPosition().distSqr(wall) > 144.0) return;\n        if (phase == 88) {\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,\n                    wall.getX() + 0.5, wall.getY() + 1.0, wall.getZ() + 0.5, 12, 0.7, 0.6, 0.7, 0.03);\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,\n                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 8, 0.35, 0.5, 0.35, 0.02);\n            return;\n        }\n        if (phase != 0) return;\n        BlockPos inside = VillageSiegeSegmentSystem.insideApproach(segment);''')
    replace_once(elite,
'''    private static void firebrand(ServerLevel level, MinecraftServer server, Mob mob) {\n        if (ticks % 100 != Math.floorMod(mob.getUUID().hashCode(), 100)) return;\n        for (ServerPlayer player : nearbyPlayers(server, mob, 10.0)) {\n            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 70));\n            player.hurtServer(level, level.damageSources().magic(), 2.5f + VillageCouncilState.currentDay() * 0.12f);\n        }\n        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, mob.getX(), mob.getY() + 1.0, mob.getZ(),\n                20, 1.0, 0.5, 1.0, 0.04);\n    }''',
'''    private static void firebrand(ServerLevel level, MinecraftServer server, Mob mob) {\n        int offset = Math.floorMod(mob.getUUID().hashCode(), 100);\n        int phase = Math.floorMod(ticks - offset, 100);\n        if (phase == 82) {\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,\n                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 16, 0.8, 0.5, 0.8, 0.03);\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,\n                    mob.getX(), mob.getY() + 0.8, mob.getZ(), 10, 0.6, 0.4, 0.6, 0.02);\n            return;\n        }\n        if (phase != 0) return;\n        for (ServerPlayer player : nearbyPlayers(server, mob, 10.0)) {\n            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), 70));\n            player.hurtServer(level, level.damageSources().magic(), 2.5f + VillageCouncilState.currentDay() * 0.12f);\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,\n                    player.getX(), player.getY() + 0.8, player.getZ(), 10, 0.35, 0.55, 0.35, 0.03);\n        }\n        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,\n                mob.getX(), mob.getY() + 1.0, mob.getZ(), 3, 0.8, 0.4, 0.8, 0.02);\n    }''')
    replace_once(elite,
'''        ServerPlayer target = nearbyPlayers(server, mob, 28.0).stream().findFirst().orElse(null);''',
'''        ServerPlayer target = nearbyPlayers(server, mob, 28.0).stream()\n                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);''')
    replace_once(elite,
'''            mob.getNavigation().moveTo(target, 1.48);\n            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 45, 0));''',
'''            mob.getNavigation().moveTo(target, 1.48);\n            mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 45, 0));\n            if (mob.level() instanceof ServerLevel level) {\n                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,\n                        mob.getX(), mob.getY() + 0.8, mob.getZ(), 10, 0.35, 0.5, 0.35, 0.03);\n            }''')
    replace_once(elite,
'''    private static void plague(MinecraftServer server, Mob mob) {\n        if (ticks % 120 != Math.floorMod(mob.getUUID().hashCode(), 120)) return;\n        for (ServerPlayer player : nearbyPlayers(server, mob, 9.0)) {\n            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));\n            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));\n        }\n    }''',
'''    private static void plague(MinecraftServer server, Mob mob) {\n        int offset = Math.floorMod(mob.getUUID().hashCode(), 120);\n        int phase = Math.floorMod(ticks - offset, 120);\n        if (!(mob.level() instanceof ServerLevel level)) return;\n        if (phase == 100) {\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,\n                    mob.getX(), mob.getY() + 1.0, mob.getZ(), 20, 1.4, 0.6, 1.4, 0.04);\n            return;\n        }\n        if (phase != 0) return;\n        for (ServerPlayer player : nearbyPlayers(server, mob, 9.0)) {\n            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));\n            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));\n            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,\n                    player.getX(), player.getY() + 0.7, player.getZ(), 9, 0.4, 0.5, 0.4, 0.03);\n        }\n    }''')
    replace_once(elite,
'''        ServerPlayer target = nearbyPlayers(server, mob, 24.0).stream().findFirst().orElse(null);''',
'''        ServerPlayer target = nearbyPlayers(server, mob, 24.0).stream()\n                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);''')
    replace_once(elite,
'''            mob.getNavigation().moveTo(target, 1.62);\n            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 55, 1));''',
'''            mob.getNavigation().moveTo(target, 1.62);\n            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 55, 1));\n            if (mob.level() instanceof ServerLevel level) {\n                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,\n                        mob.getX(), mob.getY() + 0.9, mob.getZ(), 12, 0.45, 0.55, 0.45, 0.04);\n            }''')

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

    (ROOT / ".build-trigger-v01812").write_text(
        "Village Guardians v0.18.12-alpha.1 manual quality audit acceptance\n", encoding="utf-8")
    print("[OK] v0.18.12 quality audit patch applied")


if __name__ == "__main__":
    main()
