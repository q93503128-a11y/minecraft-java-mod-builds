package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short high-cost control burst. It never attacks for the player; it only keeps camera/movement
 * steering usable at late-game speed while the player is already moving.
 */
public final class CombatAutopilotService {
    private static final double RANGE = 36.0D;
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        long endTick;
        int targetId = -1;
    }

    private CombatAutopilotService() {}

    public static void activate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (AnalysisJammingService.remainingTicks(player) > 0) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        TitanPlayerData.AugmentInstance autopilot = state.firstInstalledInstance("combat_autopilot");
        if (autopilot == null) return;

        AugmentationResourceService.Snapshot resources = AugmentationResourceService.snapshot(state);
        if (resources.neuralOverloaded() || state.heat() >= 95.0D) return;

        int enhancement = autopilot.enhancement();
        int duration = enhancement >= 10 ? 50 : enhancement >= 5 ? 40 : 28;
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        long now = level.getGameTime();
        if (runtime.endTick > now) return;
        runtime.endTick = now + duration;
        runtime.targetId = -1;
        TitanbreakNetwork.sync(player);
    }

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        if (!(player.level() instanceof ServerLevel level)) return;
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        long now = level.getGameTime();
        if (runtime == null || runtime.endTick <= now) {
            if (runtime != null) RUNTIME.remove(player.getUUID());
            return;
        }

        TitanPlayerData.AugmentInstance autopilot = state.firstInstalledInstance("combat_autopilot");
        if (autopilot == null || AnalysisJammingService.remainingTicks(player) > 0 || state.heat() >= 98.0D
                || AugmentationResourceService.snapshot(state).neuralOverloaded()
                || !AugmentationResourceService.trySpendContinuousPower(player, state, "combat_autopilot")) {
            RUNTIME.remove(player.getUUID());
            return;
        }

        LivingEntity target = resolveTarget(player, runtime, autopilot.enhancement());
        if (target != null) assistExistingMovement(player, target, autopilot.enhancement());

        AugmentationCatalog.Definition definition = AugmentationCatalog.byId("combat_autopilot");
        if (definition != null && definition.heatLoad() > 0) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            double rawHeat = definition.heatLoad() * 0.022D * state.heatLoadMultiplier("combat_autopilot");
            data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
            if (player.tickCount % 20 == 0) data.addMasteryXp(player, "combat_autopilot", 2);
        }
    }

    private static LivingEntity resolveTarget(ServerPlayer player, RuntimeState runtime, int enhancement) {
        if (runtime.targetId >= 0 && player.level().getEntity(runtime.targetId) instanceof LivingEntity current
                && current.isAlive() && current.distanceToSqr(player) <= RANGE * RANGE
                && player.hasLineOfSight(current)) {
            return current;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double minDot = enhancement >= 10 ? 0.45D : 0.65D;
        AABB area = player.getBoundingBox().inflate(RANGE);
        LivingEntity selected = player.level().getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && entity instanceof Enemy
                                && player.hasLineOfSight(entity))
                .stream()
                .filter(entity -> {
                    Vec3 direction = entity.getEyePosition().subtract(eye);
                    return direction.lengthSqr() > 1.0E-6D && look.dot(direction.normalize()) >= minDot;
                })
                .min(Comparator.comparingDouble(entity -> targetScore(player, entity, look, enhancement)))
                .orElse(null);
        runtime.targetId = selected == null ? -1 : selected.getId();
        return selected;
    }

    private static double targetScore(ServerPlayer player, LivingEntity target, Vec3 look, int enhancement) {
        Vec3 direction = target.getEyePosition().subtract(player.getEyePosition());
        double distance = Math.max(0.001D, direction.length());
        double anglePenalty = 1.0D - look.dot(direction.scale(1.0D / distance));
        double score = anglePenalty * 9.0D + distance / RANGE;
        if (enhancement >= 7) {
            double healthFraction = target.getHealth() / Math.max(1.0D, target.getMaxHealth());
            score -= (1.0D - healthFraction) * 0.12D;
        }
        return score;
    }

    private static void assistExistingMovement(ServerPlayer player, LivingEntity target, int enhancement) {
        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        double speed = horizontal.length();
        if (speed < 0.22D && !player.isSprinting() && !ReflexDriveService.active(player.getUUID())) return;

        Vec3 direction = target.position().subtract(player.position());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() <= 1.0E-6D) return;
        direction = direction.normalize();

        double preservedSpeed = Math.max(speed, player.isSprinting() ? 0.28D : speed);
        Vec3 desired = direction.scale(preservedSpeed);
        double blend = enhancement >= 10 ? 0.28D : enhancement >= 5 ? 0.18D : 0.12D;
        double nx = motion.x + (desired.x - motion.x) * blend;
        double nz = motion.z + (desired.z - motion.z) * blend;
        player.setDeltaMovement(nx, motion.y, nz);
        player.hurtMarked = true;
    }

    public static boolean active(UUID playerId) {
        RuntimeState runtime = RUNTIME.get(playerId);
        return runtime != null;
    }

    public static int remainingTicks(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) return 0;
        return (int) Math.max(0L, runtime.endTick - player.level().getGameTime());
    }

    public static void clear(UUID playerId) {
        RUNTIME.remove(playerId);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }
}
