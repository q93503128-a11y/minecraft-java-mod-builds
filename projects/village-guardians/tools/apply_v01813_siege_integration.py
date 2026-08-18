#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match in {path.name}, got {count}: {old[:180]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def all_existing(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    once(ROOT / "gradle.properties", "mod_version=0.18.12-alpha.1", "mod_version=0.18.13-alpha.1")
    for test in (ROOT / "tools").glob("test_*.py"):
        all_existing(test, "mod_version=0.18.12-alpha.1", "mod_version=0.18.13-alpha.1")

    raid = JAVA / "VillageRaidSystem.java"
    once(raid,
'''    private static final Set<UUID> ACTIVE_ENEMIES = new HashSet<>();
    private static final Map<UUID, VillageEnemyArchetypeSystem.Archetype> ACTIVE_ARCHETYPES = new HashMap<>();''',
'''    private static final Set<UUID> ACTIVE_ENEMIES = new HashSet<>();
    private static final Map<UUID, VillageEnemyArchetypeSystem.Archetype> ACTIVE_ARCHETYPES = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_WAVES = new HashMap<>();''')
    once(raid,
'''    public static VillageEnemyArchetypeSystem.Archetype archetypeOf(Mob mob) {
        return mob == null ? null : ACTIVE_ARCHETYPES.get(mob.getUUID());
    }

    public static VillageWaveTrait currentTrait() { return currentTrait; }''',
'''    public static VillageEnemyArchetypeSystem.Archetype archetypeOf(Mob mob) {
        return mob == null ? null : ACTIVE_ARCHETYPES.get(mob.getUUID());
    }

    public static int waveOf(Mob mob) {
        if (mob == null) return Math.max(1, wave);
        return Math.max(1, ACTIVE_WAVES.getOrDefault(mob.getUUID(), Math.max(1, wave)));
    }

    public static VillageWaveTrait currentTrait() { return currentTrait; }''')
    once(raid,
'''            server.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), raidTeam);
            if (level.addFreshEntity(mob)) {
                ACTIVE_ENEMIES.add(mob.getUUID());
                ACTIVE_ARCHETYPES.put(mob.getUUID(), spawned.archetype());
            } else {
                releaseEnemy(server, mob.getUUID(), mob);
            }''',
'''            server.getScoreboard().addPlayerToTeam(mob.getScoreboardName(), raidTeam);
            // Register authoritative combat metadata before addFreshEntity fires EntityJoinLevelEvent.
            ACTIVE_ARCHETYPES.put(mob.getUUID(), spawned.archetype());
            ACTIVE_WAVES.put(mob.getUUID(), wave);
            if (level.addFreshEntity(mob)) {
                ACTIVE_ENEMIES.add(mob.getUUID());
            } else {
                releaseEnemy(server, mob.getUUID(), mob);
            }''')
    once(raid,
'''        structureAttackTicks++;
        abilityTicks++;
        boolean attackTick = structureAttackTicks >= STRUCTURE_ATTACK_INTERVAL;
        if (attackTick) structureAttackTicks = 0;''',
'''        structureAttackTicks = Math.floorMod(structureAttackTicks + 1, STRUCTURE_ATTACK_INTERVAL);
        abilityTicks++;''')
    once(raid,
'''            if (attackTick && VillageFortressBuildings.isTouchingStructure(
                    villageCenter, targetBuilding, mob.blockPosition())) {''',
'''            boolean attackTick = Math.floorMod(structureAttackTicks + id.hashCode(), STRUCTURE_ATTACK_INTERVAL) == 0;
            if (attackTick && VillageFortressBuildings.isTouchingStructure(
                    villageCenter, targetBuilding, mob.blockPosition())) {''')
    once(raid,
'''                VillageProgressionSystem.damageBuilding(server, targetBuilding, damage);
                VillageEnemyArchetypeSystem.onStructureHit(level, mob, archetype);''',
'''                VillageProgressionSystem.damageBuilding(server, targetBuilding, damage);
                VillageDefenseEffectSystem.structureImpact(level, Vec3.atCenterOf(target),
                        VillageEnemyArchetypeSystem.isBoss(archetype) || archetype == VillageEnemyArchetypeSystem.Archetype.SAPPER);
                VillageEnemyArchetypeSystem.onStructureHit(level, mob, archetype);''')
    once(raid,
'''            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()) continue;''',
'''            if (player.level() != mob.level() || !player.isAlive() || player.isSpectator()
                    || VillageRespawnSystem.isDowned(player)) continue;''')
    once(raid,
'''    private static void releaseEnemy(MinecraftServer server, UUID uuid, Entity entity) {
        ACTIVE_ARCHETYPES.remove(uuid);''',
'''    private static void releaseEnemy(MinecraftServer server, UUID uuid, Entity entity) {
        ACTIVE_ARCHETYPES.remove(uuid);
        ACTIVE_WAVES.remove(uuid);''')
    once(raid,
'''        ACTIVE_ENEMIES.clear();
        ACTIVE_ARCHETYPES.clear();
        VillageBossAspectSystem.reset();''',
'''        ACTIVE_ENEMIES.clear();
        ACTIVE_ARCHETYPES.clear();
        ACTIVE_WAVES.clear();
        VillageBossAspectSystem.reset();''')

    attack = JAVA / "VillageAttackPlanSystem.java"
    all_existing(attack, "import net.minecraft.ChatFormatting;\n", "")
    once(attack, "        int wave = parseWave(mob);", "        int wave = VillageRaidSystem.waveOf(mob);")
    once(attack,
'''            if (attackTicks % 30 == 0 && VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) {''',
'''            if (Math.floorMod(attackTicks + id.hashCode(), 30) == 0
                    && VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) {''')
    once(attack,
'''                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageDifficultyTuning.earlyStructureMultiplier(day)
                        * condition(day, parseWave(mob)).structureMultiplier()));
                VillageSiegeSegmentSystem.damage(server, segment, raw, mob.blockPosition());''',
'''                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageDifficultyTuning.earlyStructureMultiplier(day)
                        * condition(day, VillageRaidSystem.waveOf(mob)).structureMultiplier()));
                VillageSiegeSegmentSystem.damage(server, segment, raw, mob.blockPosition());
                VillageDefenseEffectSystem.structureImpact(level,
                        Vec3.atCenterOf(VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition())),
                        VillageEnemyArchetypeSystem.isBoss(archetype) || archetype == VillageEnemyArchetypeSystem.Archetype.SAPPER);''')
    start = attack.read_text(encoding="utf-8").find("    private static int parseWave(Mob mob) {")
    end = attack.read_text(encoding="utf-8").find("    private static String warStage", start)
    if start < 0 or end < 0:
        raise SystemExit("parseWave block not found")
    text = attack.read_text(encoding="utf-8")
    attack.write_text(text[:start] + text[end:], encoding="utf-8")
    if "Vec3" not in attack.read_text(encoding="utf-8").split("public final class", 1)[0]:
        once(attack, "import net.minecraft.world.entity.Mob;\n", "import net.minecraft.world.entity.Mob;\nimport net.minecraft.world.phys.Vec3;\n")

    enemy = JAVA / "VillageEnemyArchetypeSystem.java"
    once(enemy,
'''            case TOWER_HUNTER -> {
                if (!abilityReady(mob, globalTicks, 180)) return;
                VillageTowerSpecializationSystem.disableRandomInstalledTower(20 * 7);
                spawnAura(level, mob, archetype, 20);
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§5[탑 교란] §f탑 사냥꾼이 방어탑 하나를 7초간 정지시켰습니다."), false);
            }''',
'''            case TOWER_HUNTER -> {
                if (!abilityReady(mob, globalTicks, 180)) return;
                int disabledId = VillagePlacedTurretSystem.disableRandomActiveTurret(20 * 7);
                if (disabledId < 0) return;
                spawnAura(level, mob, archetype, 20);
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§5[포탑 교란] §f탑 사냥꾼이 배치 포탑 #" + disabledId
                                + "의 사격 회로를 7초간 마비시켰습니다."), false);
            }''')
    once(enemy,
'''                        && player.isAlive()
                        && !player.isSpectator()
                        && player.distanceToSqr(mob) <= squared)''',
'''                        && player.isAlive()
                        && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player)
                        && player.distanceToSqr(mob) <= squared)''')

    turret = JAVA / "VillagePlacedTurretSystem.java"
    once(turret, "import java.util.HashMap;\n", "import java.util.HashMap;\nimport java.util.Iterator;\n")
    once(turret,
'''    private static final Map<Integer, TurretState> TURRETS = new LinkedHashMap<>();
    private static final Map<UUID, PendingPlacement> PENDING = new HashMap<>();
    private static int combatTicks;''',
'''    private static final Map<Integer, TurretState> TURRETS = new LinkedHashMap<>();
    private static final Map<UUID, PendingPlacement> PENDING = new HashMap<>();
    private static final Map<Integer, Integer> DISABLED_TICKS = new HashMap<>();
    private static final List<PendingBombard> PENDING_BOMBARDS = new ArrayList<>();
    private static int combatTicks;
    private static int disableCursor;''')
    once(turret,
'''        TURRETS.clear();
        PENDING.clear();
        combatTicks = 0;''',
'''        TURRETS.clear();
        PENDING.clear();
        DISABLED_TICKS.clear();
        PENDING_BOMBARDS.clear();
        combatTicks = 0;
        disableCursor = 0;''')
    once(turret,
'''        VillageSiegePersistence.removeString(PREFIX + id);
        if (player.level() instanceof ServerLevel level) clearVisual(level, state.pos());''',
'''        VillageSiegePersistence.removeString(PREFIX + id);
        DISABLED_TICKS.remove(id);
        if (player.level() instanceof ServerLevel level) clearVisual(level, state.pos());''')
    once(turret,
'''    public static void tick(MinecraftServer server) {
        if (server == null || !VillageRaidSystem.isActive()) return;
        combatTicks++;
        ServerLevel level = server.overworld();
        List<TurretState> snapshot = states();
        for (TurretState state : snapshot) {
            if (!state.active()) continue;
            enemyPressure(level, server, state);
            if (!state.active() || state.type() == TurretType.BEACON) {
                if (state.type() == TurretType.BEACON && combatTicks % 60 == 0) supportPulse(level, server, state);
                continue;
            }''',
'''    public static void tick(MinecraftServer server) {
        if (server == null) return;
        tickDisruptions();
        if (!VillageRaidSystem.isActive()) {
            PENDING_BOMBARDS.clear();
            DISABLED_TICKS.clear();
            disableCursor = 0;
            return;
        }
        combatTicks++;
        ServerLevel level = server.overworld();
        resolveBombards(level);
        List<TurretState> snapshot = states();
        for (TurretState state : snapshot) {
            if (!state.active()) continue;
            enemyPressure(level, server, state);
            if (isDisabled(state.id())) continue;
            if (!state.active() || state.type() == TurretType.BEACON) {
                if (state.type() == TurretType.BEACON && combatTicks % 60 == 0) supportPulse(level, server, state);
                continue;
            }''')
    once(turret,
'''            case CHAIN -> {
                List<Mob> chain = VillageRaidSystem.activeEnemiesNear(level, target.position(), 7.5,
                        2 + state.level() / 2, null);
                Vec3 arcStart = turretMuzzle(state, target);
                for (Mob mob : chain) {
                    if (!VillageDefenseLineOfSight.hasLine(level, arcStart, mob)) continue;
                    hitFrom(level, arcStart, mob, damage * 0.78f, ParticleTypes.ELECTRIC_SPARK);
                    arcStart = mob.position().add(0, mob.getBbHeight() * 0.55, 0);
                }
            }
            case BOMBARD -> {
                List<Mob> splash = VillageRaidSystem.activeEnemiesNear(level, target.position(), 4.5,
                        4 + state.level(), null);
                for (Mob mob : splash) hit(level, state, mob, damage * 0.72f, ParticleTypes.EXPLOSION);
            }''',
'''            case CHAIN -> {
                List<Mob> chain = VillageRaidSystem.activeEnemiesNear(level, target.position(), 7.5,
                        2 + state.level() / 2, null);
                Vec3 arcStart = turretMuzzle(state, target);
                for (Mob mob : chain) {
                    if (!VillageDefenseLineOfSight.hasLine(level, arcStart, mob)) continue;
                    Vec3 arcEnd = mob.position().add(0, mob.getBbHeight() * 0.55, 0);
                    VillageDefenseEffectSystem.turretShot(level, TurretType.CHAIN, arcStart, arcEnd);
                    hitFrom(level, arcStart, mob, damage * 0.78f, ParticleTypes.ELECTRIC_SPARK);
                    arcStart = arcEnd;
                }
            }
            case BOMBARD -> queueBombard(level, state, target, damage);''')
    once(turret,
'''    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,
                            net.minecraft.core.particles.ParticleOptions particle) {
        hitFrom(level, turretMuzzle(state, target), target, damage, particle);
    }

    private static void hitFrom(ServerLevel level, Vec3 start, Mob target, float damage,
                                net.minecraft.core.particles.ParticleOptions particle) {
        if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 point = start.lerp(end, i / 8.0);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(particle, end.x, end.y, end.z, 5, 0.18, 0.22, 0.18, 0.02);
        target.hurtServer(level, level.damageSources().magic(), damage);
    }''',
'''    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,
                            net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 start = turretMuzzle(state, target);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        VillageDefenseEffectSystem.turretShot(level, state.type(), start, end);
        hitFrom(level, start, target, damage, particle);
    }

    private static void hitFrom(ServerLevel level, Vec3 start, Mob target, float damage,
                                net.minecraft.core.particles.ParticleOptions particle) {
        if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        level.sendParticles(particle, end.x, end.y, end.z, 4, 0.18, 0.22, 0.18, 0.02);
        target.hurtServer(level, level.damageSources().magic(), damage);
    }

    private static void queueBombard(ServerLevel level, TurretState state, Mob target, float damage) {
        Vec3 start = turretMuzzle(state, target);
        Vec3 impact = target.position();
        VillageDefenseEffectSystem.turretShot(level, TurretType.BOMBARD, start, impact);
        PENDING_BOMBARDS.add(new PendingBombard(combatTicks + 12, impact,
                4.5 + state.level() * 0.15, 4 + state.level(), damage * 0.72f));
    }

    private static void resolveBombards(ServerLevel level) {
        Iterator<PendingBombard> iterator = PENDING_BOMBARDS.iterator();
        while (iterator.hasNext()) {
            PendingBombard shot = iterator.next();
            if (shot.dueTick() > combatTicks) continue;
            iterator.remove();
            for (Mob mob : VillageRaidSystem.activeEnemiesNear(level, shot.impact(), shot.radius(), shot.limit(), null)) {
                mob.hurtServer(level, level.damageSources().magic(), shot.damage());
            }
            VillageDefenseEffectSystem.bombardImpact(level, shot.impact(), shot.radius());
            level.sendParticles(ParticleTypes.EXPLOSION, shot.impact().x, shot.impact().y + 0.2, shot.impact().z,
                    5, 0.65, 0.22, 0.65, 0.03);
        }
    }''')
    once(turret,
'''        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, state.pos().getX() + 0.5, state.pos().getY() + 1.5,
                state.pos().getZ() + 0.5, 12, 1.2, 0.8, 1.2, 0.04);''',
'''        Vec3 center = Vec3.atCenterOf(state.pos());
        VillageDefenseEffectSystem.beaconPulse(level, center, radius);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, state.pos().getX() + 0.5, state.pos().getY() + 1.5,
                state.pos().getZ() + 0.5, 8, 0.9, 0.55, 0.9, 0.03);''')
    once(turret,
'''    public static synchronized void damage(MinecraftServer server, int id, int damage) {''',
'''    public static synchronized int disableRandomActiveTurret(int ticks) {
        List<TurretState> candidates = TURRETS.values().stream()
                .filter(TurretState::active)
                .sorted(Comparator.comparingInt(TurretState::id))
                .toList();
        if (candidates.isEmpty()) return -1;
        TurretState selected = candidates.get(Math.floorMod(disableCursor++, candidates.size()));
        DISABLED_TICKS.put(selected.id(), Math.max(DISABLED_TICKS.getOrDefault(selected.id(), 0), Math.max(1, ticks)));
        return selected.id();
    }

    public static synchronized boolean isDisabled(int id) {
        return DISABLED_TICKS.getOrDefault(id, 0) > 0;
    }

    public static synchronized int disabledSeconds(int id) {
        return Math.max(0, (DISABLED_TICKS.getOrDefault(id, 0) + 19) / 20);
    }

    private static synchronized void tickDisruptions() {
        for (int id : new ArrayList<>(DISABLED_TICKS.keySet())) {
            int remaining = DISABLED_TICKS.getOrDefault(id, 0);
            if (remaining <= 1 || !TURRETS.containsKey(id)) DISABLED_TICKS.remove(id);
            else DISABLED_TICKS.put(id, remaining - 1);
        }
    }

    public static synchronized void damage(MinecraftServer server, int id, int damage) {''')
    once(turret,
'''            return type.displayName() + " #" + id + " · Lv." + level + " · HP " + hp + "/" + maxHp(this)
                    + " · " + (active ? "가동" : "파괴");''',
'''            String state = active ? "가동" : "파괴";
            if (active && isDisabled(id)) state += " · §5교란 " + disabledSeconds(id) + "초";
            return type.displayName() + " #" + id + " · Lv." + level + " · HP " + hp + "/" + maxHp(this)
                    + " · " + state;''')
    once(turret,
'''    private record PendingPlacement(TurretType type, BlockPos preview) {}''',
'''    private record PendingPlacement(TurretType type, BlockPos preview) {}
    private record PendingBombard(int dueTick, Vec3 impact, double radius, int limit, float damage) {}''')

    merc = JAVA / "VillageMercenarySystem.java"
    once(merc,
'''    private static void bastionControl(ServerLevel level, IronGolem mercenary, int rank) {
        double radius = 4.5 + rank * 0.55;
        Vec3 eye = mercenary.position().add(0, 1.8, 0);
        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), radius, 5 + rank, null)) {
            if (!VillageDefenseLineOfSight.hasLine(level, eye, enemy)) continue;
            enemy.setTarget(mercenary);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 28 + rank * 5, 0));
        }
    }''',
'''    private static void bastionControl(ServerLevel level, IronGolem mercenary, int rank) {
        double radius = 4.5 + rank * 0.55;
        Vec3 eye = mercenary.position().add(0, 1.8, 0);
        boolean engaged = false;
        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), radius, 5 + rank, null)) {
            if (!VillageDefenseLineOfSight.hasLine(level, eye, enemy)) continue;
            enemy.setTarget(mercenary);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 28 + rank * 5, 0));
            engaged = true;
        }
        if (engaged) VillageDefenseEffectSystem.mercenaryGuardPulse(level, mercenary.position(), radius);
    }''')
    once(merc,
'''        mercenary.setTarget(target);
        mercenary.getNavigation().moveTo(target, 1.18 + rank * 0.025);''',
'''        mercenary.setTarget(target);
        mercenary.getNavigation().moveTo(target, 1.18 + rank * 0.025);
        VillageDefenseEffectSystem.mercenaryStrikerPressure(level, mercenary.position().add(0, 1.2, 0),
                target.position().add(0, target.getBbHeight() * 0.5, 0));''')
    once(merc,
'''        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        for (int i = 0; i <= 10; i++) {
            Vec3 point = start.lerp(end, i / 10.0);
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);''',
'''        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        VillageDefenseEffectSystem.mercenaryRangerShot(level, start, end);
        level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.14, 0.18, 0.14, 0.02);
        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);''')
    once(merc,
'''        level.sendParticles(ParticleTypes.HEART, medic.getX(), medic.getY() + 1.4, medic.getZ(),
                4 + rank, 0.7, 0.5, 0.7, 0.02);''',
'''        VillageDefenseEffectSystem.mercenaryHealPulse(level, medic.position(), 8.0 + rank);
        level.sendParticles(ParticleTypes.HEART, medic.getX(), medic.getY() + 1.4, medic.getZ(),
                3 + rank, 0.55, 0.4, 0.55, 0.02);''')

    guardians = JAVA / "VillageGuardians.java"
    all_existing(guardians, "        VillageTowerResearchBonusSystem.reset();\n", "")
    all_existing(guardians, "        VillageTowerResearchBonusSystem.tick(event.getServer());\n", "")

    bonus = JAVA / "VillageTowerResearchBonusSystem.java"
    if bonus.exists(): bonus.unlink()

    mesh = JAVA / "VillageSkillMeshLibrary.java"
    once(mesh,
'''            case "warden_aegis" -> renderFortress(pose, out, basis, age, progress, false);
            default -> renderFallbackRune(pose, out, basis, age, progress);''',
'''            case "warden_aegis" -> renderFortress(pose, out, basis, age, progress, false);

            case "turret_ballista_shot" -> renderDefenseShot(pose, out, state, age, progress, 0);
            case "turret_repeater_shot" -> renderDefenseShot(pose, out, state, age, progress, 1);
            case "turret_piercer_shot" -> renderDefenseShot(pose, out, state, age, progress, 2);
            case "turret_flame_shot" -> renderDefenseShot(pose, out, state, age, progress, 3);
            case "turret_frost_shot" -> renderDefenseShot(pose, out, state, age, progress, 4);
            case "turret_chain_shot" -> renderDefenseShot(pose, out, state, age, progress, 5);
            case "turret_bombard_arc" -> renderBombardArc(pose, out, state, age, progress);
            case "turret_bombard_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 0);
            case "turret_nullifier_shot" -> renderDefenseShot(pose, out, state, age, progress, 6);
            case "turret_antiair_shot" -> renderDefenseShot(pose, out, state, age, progress, 7);
            case "turret_beacon_pulse" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 1);
            case "merc_ranger_shot" -> renderDefenseShot(pose, out, state, age, progress, 8);
            case "merc_bastion_guard" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 2);
            case "merc_striker_pressure" -> renderDefenseShot(pose, out, state, age, progress, 9);
            case "merc_medic_pulse" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 3);
            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);
            default -> renderFallbackRune(pose, out, basis, age, progress);''')
    once(mesh,
'''    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''',
'''    private static void renderDefenseShot(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress, int style) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        Vec3 a = points.get(0).subtract(origin);
        Vec3 z = points.get(1).subtract(origin);
        Vec3 delta = z.subtract(a);
        if (delta.lengthSqr() < 1.0E-6) return;
        Basis b = Basis.from(delta);
        double travel = clamp(progress * 1.18, 0.0, 1.0);
        Vec3 center = a.lerp(z, travel);
        int color = switch (style) {
            case 2 -> rgba(255, 235, 180, 230);
            case 3 -> rgba(255, 92, 34, 220);
            case 4 -> rgba(121, 220, 255, 225);
            case 5 -> rgba(126, 208, 255, 230);
            case 6 -> rgba(207, 135, 255, 225);
            case 7 -> rgba(255, 214, 109, 230);
            case 8 -> rgba(151, 224, 255, 230);
            case 9 -> rgba(255, 151, 63, 210);
            default -> rgba(234, 239, 224, 225);
        };
        double fade = 1.0 - progress * 0.58;
        if (style == 3) {
            Vec3 back = center.subtract(b.forward.scale(1.55));
            braidedBeam(pose, out, back, center.add(b.forward.scale(0.45)), age, 0.18,
                    rgba(255, 103, 35, (int) (210 * fade)));
            sphere(pose, out, center, 0.24, 7, 10, color);
        } else if (style == 5 || style == 6) {
            braidedBeam(pose, out, a, z, age * (style == 5 ? 1.8 : 0.7),
                    style == 5 ? 0.10 : 0.13,
                    (color & 0xFFFFFF00) | (int) (205 * fade));
        } else if (style == 9) {
            prism(pose, out, center.subtract(b.forward.scale(1.0)), center.add(b.forward.scale(0.35)),
                    0.10, color);
        } else {
            double length = style == 2 ? 2.05 : style == 7 ? 1.65 : style == 1 ? 0.72 : 1.25;
            double thickness = style == 2 ? 0.105 : style == 1 ? 0.045 : 0.07;
            customArrow(pose, out, b, center, length, thickness, color);
            prism(pose, out, center.subtract(b.forward.scale(length * 1.15)),
                    center.subtract(b.forward.scale(length * 0.25)), thickness * 0.45,
                    rgba((color >> 24) & 255, (color >> 16) & 255, (color >> 8) & 255, (int) (120 * fade)));
        }
    }

    private static void renderBombardArc(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        Vec3 a = points.get(0).subtract(origin);
        Vec3 z = points.get(1).subtract(origin);
        double horizontal = Math.hypot(z.x - a.x, z.z - a.z);
        Vec3 control = a.lerp(z, 0.5).add(0.0, Math.max(4.5, horizontal * 0.16), 0.0);
        Vec3 previous = a;
        for (int i = 1; i <= 18; i++) {
            double t = i / 18.0;
            Vec3 current = bezier(a, control, z, t);
            prism(pose, out, previous, current, 0.045,
                    rgba(255, 137, 58, (int) (110 * (1.0 - progress * 0.6))));
            previous = current;
        }
        Vec3 shell = bezier(a, control, z, clamp(progress * 1.05, 0.0, 1.0));
        sphere(pose, out, shell, 0.24, 8, 12, rgba(255, 202, 93, 235));
        sphere(pose, out, shell, 0.11, 7, 10, rgba(255, 245, 191, 245));
    }

    private static Vec3 bezier(Vec3 a, Vec3 control, Vec3 z, double t) {
        double u = 1.0 - t;
        return a.scale(u * u).add(control.scale(2.0 * u * t)).add(z.scale(t * t));
    }

    private static void renderDefensePulse(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress,
            String encodedRadius, int style) {
        double maxRadius = 4.0;
        try { maxRadius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        maxRadius = Math.max(0.8, maxRadius);
        double radius = 0.35 + progress * maxRadius;
        int color = switch (style) {
            case 1 -> rgba(115, 235, 182, (int) (200 * (1.0 - progress)));
            case 2 -> rgba(108, 186, 255, (int) (210 * (1.0 - progress)));
            case 3 -> rgba(255, 223, 126, (int) (205 * (1.0 - progress)));
            case 4 -> rgba(255, 111, 67, (int) (220 * (1.0 - progress)));
            default -> rgba(255, 165, 72, (int) (220 * (1.0 - progress)));
        };
        ring(pose, out, b, radius, 0.06, style == 4 ? 0.18 : 0.11, 64, color, age * 0.015);
        if (style == 2 || style == 3) {
            ring(pose, out, b, radius * 0.64, 0.82, 0.06, 48, color, -age * 0.025);
        }
        if (style == 0 || style == 4) {
            sphere(pose, out, Vec3.ZERO, Math.max(0.22, radius * 0.32), 8, 12,
                    (color & 0xFFFFFF00) | Math.max(20, (int) (90 * (1.0 - progress))));
        }
    }

    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''')

    print("[PASS] applied v0.18.13 siege integration patch")


if __name__ == "__main__":
    main()
