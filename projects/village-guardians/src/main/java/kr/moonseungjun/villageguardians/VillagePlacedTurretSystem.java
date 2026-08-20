package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Player-placed, destructible defense emplacements. Fixed corner towers are no longer the combat ownership model. */
public final class VillagePlacedTurretSystem {
    private static final String PREFIX = "turret_";
    private static final Map<Integer, TurretState> TURRETS = new LinkedHashMap<>();
    private static final Map<UUID, PendingPlacement> PENDING = new HashMap<>();
    private static final Map<Integer, Integer> DISABLED_TICKS = new HashMap<>();
    private static final List<PendingBombard> PENDING_BOMBARDS = new ArrayList<>();
    private static int combatTicks;

    private VillagePlacedTurretSystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        TURRETS.clear();
        PENDING.clear();
        DISABLED_TICKS.clear();
        PENDING_BOMBARDS.clear();
        combatTicks = 0;
        VillageSiegePersistence.stringsWithPrefix(PREFIX).forEach((key, value) -> {
            try {
                int id = Integer.parseInt(key.substring(PREFIX.length()));
                TurretState state = decode(id, value);
                if (state != null) TURRETS.put(id, state);
            } catch (NumberFormatException ignored) { }
        });
        if (server != null) {
            ServerLevel level = server.overworld();
            VillageTurretPresentationSystem.initialize(level, states());
            rebuildVisuals(level);
        }
    }

    /** Re-project an externally restored/reset siege snapshot into runtime state and world visuals. */
    public static synchronized void reloadAfterPersistenceChange(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        for (TurretState state : new ArrayList<>(TURRETS.values())) clearVisual(level, state);
        TURRETS.clear();
        PENDING.clear();
        DISABLED_TICKS.clear();
        PENDING_BOMBARDS.clear();
        combatTicks = 0;
        VillageSiegePersistence.stringsWithPrefix(PREFIX).forEach((key, value) -> {
            try {
                int id = Integer.parseInt(key.substring(PREFIX.length()));
                TurretState state = decode(id, value);
                if (state != null) TURRETS.put(id, state);
            } catch (NumberFormatException ignored) { }
        });
        VillageTurretPresentationSystem.initialize(level, states());
        rebuildVisuals(level);
    }

    public static synchronized int count() { return TURRETS.size(); }
    public static synchronized int activeCount() {
        return (int) TURRETS.values().stream().filter(TurretState::active).count();
    }
    public static int capacity() {
        return 2 + VillageProgressionSystem.wallLevel()
                + VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.TOWER);
    }

    public static synchronized List<TurretState> states() { return List.copyOf(TURRETS.values()); }

    public static String selectPlacement(ServerPlayer player, TurretType type) {
        if (type == null) return "알 수 없는 포탑 계열입니다.";
        if (VillageRaidSystem.isRaidLocked() || VillageCouncilState.currentPhase() != VillageTimePhase.DAY) {
            return "포탑 배치는 낮 정비 시간에만 가능합니다.";
        }
        if (count() >= capacity()) return "포탑 설치 한도입니다. 현재 " + count() + " / " + capacity();
        PENDING.put(player.getUUID(), new PendingPlacement(type, null));
        return type.displayName() + " 배치 모드 시작 · 설치할 바닥을 우클릭하면 위치를 미리 검증하고, 같은 위치를 다시 우클릭하면 확정합니다.";
    }

    public static boolean handlePlacementClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return false;
        PendingPlacement pending = PENDING.get(player.getUUID());
        if (pending == null) return false;
        event.setCanceled(true);
        BlockPos candidate = event.getPos().above();
        String invalid = invalidReason(level, candidate, -1);
        if (invalid != null) {
            level.sendParticles(ParticleTypes.SMOKE, candidate.getX() + 0.5, candidate.getY() + 0.4,
                    candidate.getZ() + 0.5, 10, 0.35, 0.2, 0.35, 0.03);
            player.sendSystemMessage(Component.literal("§c[배치 불가] §f" + invalid));
            PENDING.put(player.getUUID(), new PendingPlacement(pending.type(), null));
            return true;
        }
        if (pending.preview() == null || !pending.preview().equals(candidate)) {
            PENDING.put(player.getUUID(), new PendingPlacement(pending.type(), candidate.immutable()));
            VillageDefenseEffectSystem.turretPlacementPreview(level,
                    Vec3.atCenterOf(candidate).add(0.0, -0.45, 0.0), pending.type());
            player.sendSystemMessage(Component.literal("§a[배치 미리보기] §f유효한 위치입니다. 같은 블록을 다시 우클릭해 확정하세요."));
            return true;
        }
        int cost = pending.type().installCost();
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            player.sendSystemMessage(Component.literal("§c설치 주화가 부족합니다. 필요 " + cost));
            return true;
        }
        int id = Math.max(1, VillageSiegePersistence.getInt("next_turret_id", 1));
        VillageSiegePersistence.putInt("next_turret_id", id + 1);
        TurretState state = new TurretState(id, pending.type(), candidate.immutable(), 1,
                pending.type().baseHp(), true);
        state = new TurretState(id, state.type(), state.pos(), state.level(), maxHp(state), true);
        synchronized (VillagePlacedTurretSystem.class) { TURRETS.put(id, state); persist(state); }
        buildVisual(level, state);
        VillageDefenseEffectSystem.turretDeployPulse(level,
                Vec3.atCenterOf(state.pos()).add(0.0, -0.45, 0.0), state.type());
        PENDING.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("§6[포탑 설치] §f" + pending.type().displayName()
                + " #" + id + " 설치 완료 · 주화 " + cost + " 사용"));
        return true;
    }

    public static String cancelPlacement(ServerPlayer player) {
        return PENDING.remove(player.getUUID()) == null ? "진행 중인 포탑 배치가 없습니다." : "포탑 배치를 취소했습니다.";
    }

    public static synchronized String fieldRepairNearest(ServerPlayer player, int amount) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return "";
        double rangeSquared = 12.0 * 12.0;
        TurretState target = TURRETS.values().stream()
                .filter(TurretState::active)
                .filter(state -> state.hp() > 0 && state.hp() < maxHp(state))
                .filter(state -> player.position().distanceToSqr(Vec3.atCenterOf(state.pos())) <= rangeSquared)
                .min(Comparator.comparingDouble(state ->
                        player.position().distanceToSqr(Vec3.atCenterOf(state.pos()))))
                .orElse(null);
        if (target == null) return "";
        int repairedHp = Math.min(maxHp(target), target.hp() + Math.max(1, amount));
        TurretState repaired = new TurretState(target.id(), target.type(), target.pos(), target.level(), repairedHp, true);
        TURRETS.put(target.id(), repaired);
        persist(repaired);
        buildVisual(level, repaired);
        VillageDefenseEffectSystem.turretRepairPulse(level,
                Vec3.atCenterOf(repaired.pos()).add(0.0, -0.35, 0.0));
        return "응급 포탑 수리 키트 사용 · " + repaired.type().displayName() + " #" + repaired.id()
                + " HP " + target.hp() + " → " + repairedHp;
    }

    public static synchronized String repair(ServerPlayer player, int id) {
        if (VillageRaidSystem.isRaidLocked()) return "습격 중에는 포탑을 수리할 수 없습니다.";
        TurretState state = TURRETS.get(id);
        if (state == null) return "해당 포탑을 찾을 수 없습니다.";
        int maximum = maxHp(state);
        if (state.hp() >= maximum && state.active()) return "이미 완전한 상태입니다.";
        int cost = Math.max(30, (maximum - Math.max(0, state.hp()) + 5) / 6);
        if (!VillageProgressionSystem.spendCoins(player, cost)) return "수리 주화가 부족합니다. 필요 " + cost;
        TurretState repaired = new TurretState(id, state.type(), state.pos(), state.level(), maximum, true);
        TURRETS.put(id, repaired); persist(repaired);
        if (player.level() instanceof ServerLevel level) {
            buildVisual(level, repaired);
            VillageDefenseEffectSystem.turretRepairPulse(level,
                    Vec3.atCenterOf(repaired.pos()).add(0.0, -0.35, 0.0));
        }
        return state.type().displayName() + " #" + id + " 수리 완료 · HP " + maximum + "/" + maximum;
    }

    public static synchronized String upgrade(ServerPlayer player, int id) {
        if (VillageRaidSystem.isRaidLocked()) return "습격 중에는 포탑을 강화할 수 없습니다.";
        TurretState state = TURRETS.get(id);
        if (state == null) return "해당 포탑을 찾을 수 없습니다.";
        if (!state.active()) return "파괴된 포탑은 먼저 수리해야 합니다.";
        if (state.level() >= 5) return "포탑이 최고 레벨입니다.";
        int cost = 130 + state.level() * 110;
        if (!VillageProgressionSystem.spendCoins(player, cost)) return "강화 주화가 부족합니다. 필요 " + cost;
        int newLevel = state.level() + 1;
        TurretState upgradedBase = new TurretState(id, state.type(), state.pos(), newLevel,
                state.type().baseHp() + (newLevel - 1) * 70, true);
        TurretState upgraded = new TurretState(id, state.type(), state.pos(), newLevel,
                maxHp(upgradedBase), true);
        TURRETS.put(id, upgraded); persist(upgraded);
        if (player.level() instanceof ServerLevel level) {
            buildVisual(level, upgraded);
            VillageDefenseEffectSystem.turretUpgradePulse(level,
                    Vec3.atCenterOf(upgraded.pos()).add(0.0, -0.35, 0.0), newLevel);
        }
        return state.type().displayName() + " #" + id + " Lv." + newLevel + " 강화 완료";
    }

    public static synchronized String dismantle(ServerPlayer player, int id) {
        if (VillageRaidSystem.isRaidLocked()) return "습격 중에는 포탑을 철거할 수 없습니다.";
        TurretState state = TURRETS.remove(id);
        if (state == null) return "해당 포탑을 찾을 수 없습니다.";
        VillageSiegePersistence.removeString(PREFIX + id);
        DISABLED_TICKS.remove(id);
        if (player.level() instanceof ServerLevel level) clearVisual(level, state);
        int refund = Math.max(20, state.type().installCost() / 3 + (state.level() - 1) * 25);
        VillageProgressionSystem.addCoins(player, refund, "포탑 철거 환급");
        return state.type().displayName() + " #" + id + " 철거 완료 · 주화 " + refund + " 환급";
    }

    public static synchronized String repairAll(ServerPlayer player) {
        if (VillageRaidSystem.isRaidLocked()) return "습격 중에는 포탑을 일괄 수리할 수 없습니다.";
        int repaired = 0;
        int totalCost = 0;
        List<TurretState> damaged = TURRETS.values().stream()
                .filter(state -> !state.active() || state.hp() < maxHp(state)).toList();
        for (TurretState state : damaged) {
            int cost = Math.max(30, (maxHp(state) - Math.max(0, state.hp()) + 5) / 6);
            if (VillageProgressionSystem.coins(player) < cost) break;
            VillageProgressionSystem.spendCoins(player, cost);
            TurretState fixed = new TurretState(state.id(), state.type(), state.pos(), state.level(), maxHp(state), true);
            TURRETS.put(state.id(), fixed); persist(fixed);
            if (player.level() instanceof ServerLevel level) {
                buildVisual(level, fixed);
                VillageDefenseEffectSystem.turretRepairPulse(level,
                        Vec3.atCenterOf(fixed.pos()).add(0.0, -0.35, 0.0));
            }
            totalCost += cost; repaired++;
        }
        return repaired == 0 ? "수리할 포탑이 없거나 주화가 부족합니다."
                : "손상 포탑 " + repaired + "기 일괄 수리 · 주화 " + totalCost + " 사용";
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        tickDisruptions();
        ServerLevel level = server.overworld();
        VillageTurretPresentationSystem.tick(level, states());
        if (!VillageRaidSystem.isActive()) {
            PENDING_BOMBARDS.clear();
            DISABLED_TICKS.clear();
            return;
        }
        combatTicks++;
        resolveBombards(level);
        List<TurretState> snapshot = states();
        for (TurretState state : snapshot) {
            if (!state.active()) continue;
            enemyPressure(level, server, state);
            if (isDisabled(state.id())) continue;
            if (!state.active() || state.type() == TurretType.BEACON) {
                if (state.type() == TurretType.BEACON && combatTicks % 60 == 0) supportPulse(level, server, state);
                continue;
            }
            int interval = Math.max(8, state.type().interval() - (state.level() - 1) * 2);
            if (Math.floorMod(combatTicks + state.id() * 7, interval) != 0) continue;
            fire(level, state);
        }
    }

    private static void fire(ServerLevel level, TurretState state) {
        double range = (state.type().range() + (state.level() - 1) * 2.5)
                * VillageDefenseResearchSystem.towerRangeMultiplier();
        List<Mob> nearby = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null);
        List<Mob> candidates = state.type() == TurretType.BOMBARD
                ? nearby
                : nearby.stream().filter(mob -> VillageDefenseLineOfSight.hasLine(level, turretMuzzle(state, mob), mob)).toList();
        if (candidates.isEmpty()) return;
        Mob target = candidates.stream().min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(state.pos())))).orElse(null);
        if (state.type() == TurretType.ANTI_AIR) {
            double baseY = VillageCouncilState.villageCenter().map(BlockPos::getY).orElse(state.pos().getY());
            target = candidates.stream().filter(mob -> mob.getY() > baseY + 6.0)
                    .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(state.pos()))))
                    .orElse(target);
        }
        if (target == null) return;
        VillageTurretPresentationSystem.aim(level, state,
                target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
        float damage = (state.type().damage() + (state.level() - 1) * state.type().damage() * 0.16f)
                * VillageDefenseResearchSystem.towerDamageMultiplier();
        switch (state.type()) {
            case PIERCER -> hit(level, state, target, damage * piercingMultiplier(target), ParticleTypes.CRIT);
            case CHAIN -> {
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
            case BOMBARD -> queueBombard(level, state, target, damage);
            case FLAME -> {
                hit(level, state, target, damage, ParticleTypes.FLAME);
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80 + state.level() * 30));
            }
            case FROST -> {
                hit(level, state, target, damage, ParticleTypes.SNOWFLAKE);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 65 + state.level() * 20,
                        Math.min(3, state.level() / 2)));
            }
            case NULLIFIER -> {
                hit(level, state, target, damage, ParticleTypes.ENCHANT);
                target.removeEffect(MobEffects.STRENGTH);
                target.removeEffect(MobEffects.REGENERATION);
            }
            default -> hit(level, state, target, damage, ParticleTypes.CRIT);
        }
    }

    private static float piercingMultiplier(Mob target) {
        VillageEnemyArchetypeSystem.Archetype type = VillageRaidSystem.archetypeOf(target);
        if (type == null) return target.hasEffect(MobEffects.RESISTANCE) ? 1.30f : 1.05f;
        return switch (type) {
            case BULWARK, SHIELDBREAKER, SIEGE_BEAST, IRON_WARLORD -> 1.55f;
            case DREAD_KNIGHT -> 1.35f;
            default -> target.hasEffect(MobEffects.RESISTANCE) ? 1.30f : 1.05f;
        };
    }

    private static Vec3 turretMuzzle(TurretState state, Mob target) {
        Vec3 capCenter = Vec3.atCenterOf(state.pos().above(2));
        Vec3 delta = target.position().subtract(capCenter);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal < 1.0e-4) {
            return new Vec3(capCenter.x, state.pos().getY() + 3.05, capCenter.z);
        }
        return capCenter.add(delta.x / horizontal * 0.72, 0.22, delta.z / horizontal * 0.72);
    }

    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,
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
    }

    private static void supportPulse(ServerLevel level, MinecraftServer server, TurretState state) {
        double radius = (state.type().range() + state.level() * 2.0)
                * VillageDefenseResearchSystem.towerRangeMultiplier();
        double squared = radius * radius;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(Vec3.atCenterOf(state.pos())) <= squared
                    && !VillageRespawnSystem.isDowned(player)) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 70, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 70, 0, false, true, true));
            }
        }
        Vec3 center = Vec3.atCenterOf(state.pos());
        VillageDefenseEffectSystem.beaconPulse(level, center, radius);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, state.pos().getX() + 0.5, state.pos().getY() + 1.5,
                state.pos().getZ() + 0.5, 8, 0.9, 0.55, 0.9, 0.03);
    }

    private static void enemyPressure(ServerLevel level, MinecraftServer server, TurretState state) {
        if (combatTicks % 30 != 0) return;
        List<Mob> enemies = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), 36.0, 18, null);
        int damage = 0;
        for (Mob mob : enemies) {
            VillageEnemyArchetypeSystem.Archetype type = VillageRaidSystem.archetypeOf(mob);
            double distanceSquared = mob.distanceToSqr(Vec3.atCenterOf(state.pos()));
            if (type == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER && distanceSquared <= 36.0 * 36.0) {
                // Navigation ownership lives in VillageRaidSystem; this layer only resolves physical turret contact damage.
                if (distanceSquared <= 7.5 * 7.5) {
                    damage = Math.max(damage, 18 + VillageCouncilState.currentDay());
                }
            } else if (type == VillageEnemyArchetypeSystem.Archetype.SAPPER
                    && distanceSquared <= 6.0 * 6.0) {
                damage = Math.max(damage, 30);
            } else if (type != null && VillageEnemyArchetypeSystem.isBoss(type)
                    && distanceSquared <= 8.0 * 8.0) {
                damage = Math.max(damage, 38);
            }
        }
        if (damage > 0) damage(server, state.id(), damage);
    }

    public static synchronized TurretState nearestActiveTurret(Vec3 origin, double range) {
        if (origin == null || range <= 0.0) return null;
        double squared = range * range;
        return TURRETS.values().stream()
                .filter(TurretState::active)
                .filter(state -> Vec3.atCenterOf(state.pos()).distanceToSqr(origin) <= squared)
                .min(Comparator.comparingDouble(state -> Vec3.atCenterOf(state.pos()).distanceToSqr(origin)))
                .orElse(null);
    }

    public static synchronized int disableNearestActiveTurret(Vec3 origin, double range, int ticks) {
        TurretState selected = nearestActiveTurret(origin, range);
        if (selected == null) return -1;
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

    public static synchronized void damage(MinecraftServer server, int id, int damage) {
        TurretState state = TURRETS.get(id);
        if (state == null || !state.active() || damage <= 0) return;
        int next = Math.max(0, state.hp() - damage);
        TurretState updated = new TurretState(id, state.type(), state.pos(), state.level(), next, next > 0);
        TURRETS.put(id, updated); persist(updated);
        if (server != null) {
            if (next <= 0) {
                buildWreck(server.overworld(), updated);
                server.getPlayerList().broadcastSystemMessage(Component.literal(
                        "§c[포탑 파괴] §f" + state.type().displayName() + " #" + id
                                + "이 잔해 상태가 되었습니다. 아이템은 드롭되지 않습니다."), false);
            } else buildVisual(server.overworld(), updated);
        }
    }

    private static String invalidReason(ServerLevel level, BlockPos pos, int ignoredId) {
        if (VillageRaidSystem.isRaidLocked() || VillageCouncilState.currentPhase() != VillageTimePhase.DAY) return "낮 정비 시간에만 설치할 수 있습니다.";
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return "마을 중심이 설정되지 않았습니다.";
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        int r = VillageWorldSystem.FORTRESS_RADIUS - 2;
        if ((long) dx * dx + (long) dz * dz > (long) r * r) return "마을 방어구역 안에 배치해야 합니다.";
        if (Math.abs(dx) <= 7 && dz >= -72 && dz <= 40) return "주 통행로를 막을 수 없습니다.";
        if (dz <= -54 && Math.abs(dx) <= 28) return "북문 진입로 도배 방지를 위해 성문 앞에는 설치할 수 없습니다.";
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            BlockPos buildingPos = VillageWorldSystem.buildingCenter(building);
            if (buildingPos != null && buildingPos.distSqr(pos) < 100.0) return "건물 출입구와 운영 공간을 막을 수 없습니다.";
        }
        for (TurretState state : TURRETS.values()) {
            if (state.id() != ignoredId && state.pos().distSqr(pos) < 64.0) return "다른 포탑과 최소 8블록 간격이 필요합니다.";
        }
        BlockPos floor = pos.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return "단단한 바닥 위에 설치해야 합니다.";
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                || !level.getBlockState(pos.above(2)).isAir()) return "포탑 공간 3블록이 비어 있어야 합니다.";
        if (count() >= capacity()) return "전체 포탑 설치 한도를 초과합니다.";
        return null;
    }

    private static int maxHp(TurretState state) {
        int base = state.type().baseHp() + (state.level() - 1) * 70;
        return Math.max(base, Math.round(base * VillageDefenseResearchSystem.towerDurabilityMultiplier()));
    }

    private static void rebuildVisuals(ServerLevel level) { for (TurretState state : TURRETS.values()) buildVisual(level, state); }
    private static void buildVisual(ServerLevel level, TurretState state) {
        if (!state.active()) { buildWreck(level, state); return; }
        Block base = state.level() >= 4 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.STONE_BRICK_WALL;
        VillageFortressTerrain.set(level, state.pos(), base);
        // Collision and raycast footprint stay physical; the visible machinery is a synchronized procedural mesh actor.
        VillageFortressTerrain.set(level, state.pos().above(), Blocks.BARRIER);
        VillageFortressTerrain.set(level, state.pos().above(2), Blocks.BARRIER);
        VillageTurretPresentationSystem.show(level, state);
    }
    private static void buildWreck(ServerLevel level, TurretState state) {
        VillageFortressTerrain.set(level, state.pos(), Blocks.CRACKED_STONE_BRICKS);
        VillageFortressTerrain.set(level, state.pos().above(), Blocks.AIR);
        VillageFortressTerrain.set(level, state.pos().above(2), Blocks.AIR);
        VillageTurretPresentationSystem.show(level, state);
    }
    private static void clearVisual(ServerLevel level, TurretState state) {
        VillageTurretPresentationSystem.remove(level, state.id());
        BlockPos pos = state.pos();
        VillageFortressTerrain.set(level, pos, Blocks.AIR);
        VillageFortressTerrain.set(level, pos.above(), Blocks.AIR);
        VillageFortressTerrain.set(level, pos.above(2), Blocks.AIR);
    }

    private static void persist(TurretState state) {
        VillageSiegePersistence.putString(PREFIX + state.id(), encode(state));
    }
    private static String encode(TurretState state) {
        return state.type().id() + "|" + state.pos().getX() + "|" + state.pos().getY() + "|"
                + state.pos().getZ() + "|" + state.level() + "|" + state.hp() + "|" + (state.active() ? 1 : 0);
    }
    private static TurretState decode(int id, String value) {
        String[] p = value.split("\\|", -1);
        if (p.length != 7) return null;
        TurretType type = TurretType.fromId(p[0]);
        if (type == null) return null;
        try {
            return new TurretState(id, type,
                    new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])),
                    Math.max(1, Math.min(5, Integer.parseInt(p[4]))), Math.max(0, Integer.parseInt(p[5])),
                    "1".equals(p[6]));
        } catch (NumberFormatException ignored) { return null; }
    }

    public record TurretState(int id, TurretType type, BlockPos pos, int level, int hp, boolean active) {
        public String summary() {
            String state = active ? "가동" : "파괴";
            if (active && isDisabled(id)) state += " · §5교란 " + disabledSeconds(id) + "초";
            return type.displayName() + " #" + id + " · Lv." + level + " · HP " + hp + "/" + maxHp(this)
                    + " · " + state;
        }
    }
    private record PendingPlacement(TurretType type, BlockPos preview) {}
    private record PendingBombard(int dueTick, Vec3 impact, double radius, int limit, float damage) {}

    public enum TurretType {
        BALLISTA("ballista", "중쇠뇌", 28, 44, 58, 300, 180, Blocks.DISPENSER, "단일 고화력"),
        REPEATER("repeater", "연사 포탑", 12, 18, 46, 250, 150, Blocks.OBSERVER, "짧은 간격 연사"),
        PIERCER("piercer", "철갑 관통포", 24, 34, 62, 290, 190, Blocks.TARGET, "중장갑 대응"),
        FLAME("flame", "화염 투사기", 16, 30, 44, 270, 180, Blocks.CAMPFIRE, "지속 화상"),
        FROST("frost", "서리 억제기", 11, 28, 48, 270, 180, Blocks.PACKED_ICE, "감속 제어"),
        CHAIN("chain", "연쇄 전격탑", 13, 36, 50, 260, 210, Blocks.END_ROD, "다중 연쇄"),
        BOMBARD("bombard", "광역 투석포", 32, 70, 55, 340, 250, Blocks.BLAST_FURNACE, "광역 포격"),
        NULLIFIER("nullifier", "마법 억제탑", 9, 40, 50, 280, 220, Blocks.AMETHYST_BLOCK, "강화 효과 제거"),
        ANTI_AIR("anti_air", "대공 발사대", 19, 24, 72, 260, 220, Blocks.IRON_BARS, "고고도 우선 사격"),
        BEACON("beacon", "지원 봉화", 0, 60, 24, 320, 240, Blocks.BEACON, "주변 수호자 회복·저항");

        private final String id, displayName, role;
        private final int damage, interval, range, baseHp, installCost;
        private final Block visual;
        TurretType(String id, String displayName, int damage, int interval, int range,
                   int baseHp, int installCost, Block visual, String role) {
            this.id = id; this.displayName = displayName; this.damage = damage; this.interval = interval;
            this.range = range; this.baseHp = baseHp; this.installCost = installCost; this.visual = visual; this.role = role;
        }
        public String id() { return id; }
        public String displayName() { return displayName; }
        public int damage() { return damage; }
        public int interval() { return interval; }
        public int range() { return range; }
        public int baseHp() { return baseHp; }
        public int installCost() { return installCost; }
        public Block visual() { return visual; }
        public String role() { return role; }
        public static TurretType fromId(String id) {
            if (id == null) return null;
            String value = id.toLowerCase(Locale.ROOT);
            for (TurretType type : values()) if (type.id.equals(value)) return type;
            return null;
        }
    }
}
