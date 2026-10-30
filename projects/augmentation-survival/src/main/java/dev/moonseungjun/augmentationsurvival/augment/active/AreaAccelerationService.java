package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Server-owned Area Acceleration aura.
 *
 * <p>Only active aura owners are tracked. Growth/machine/production hooks ask for the multiplier at
 * their own position, avoiding per-tick scans of every block entity or living entity in a 24-block
 * sphere. Overlapping auras use the highest effective level rather than multiplying.</p>
 */
public final class AreaAccelerationService {
    private static final String AREA_ACCELERATION_ID = id("area_acceleration");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String COMBAT_CYCLE_ID = id("combat_cycle");
    private static final String OVERTIME_ID = id("overtime");

    private static final int BASE_DURATION_TICKS = 30 * 20;
    private static final int BASE_COOLDOWN_TICKS = 120 * 20;
    private static final int RADIUS = 24;
    private static final double RADIUS_SQUARED = RADIUS * RADIUS;
    private static final Map<UUID, AuraWindow> ACTIVE = new HashMap<>();

    private AreaAccelerationService() {
    }

    public static void initialize() {
        for (String augmentId : new String[]{
                AREA_ACCELERATION_ID, ABILITY_HASTE_ID, DOUBLE_CHARGE_ID, COMBAT_CYCLE_ID, OVERTIME_ID
        }) {
            AugmentCatalog.require(augmentId);
        }
        ServerPlayerEvents.JOIN.register(player -> ACTIVE.remove(player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> ACTIVE.remove(newPlayer.getUUID()));
        ServerPlayerEvents.LEAVE.register(player -> ACTIVE.remove(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(AreaAccelerationService::afterKill);
        ServerTickEvents.END_SERVER_TICK.register(AreaAccelerationService::tick);
    }

    public static boolean tryUse(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, AREA_ACCELERATION_ID);
        if (level <= 0 || player.isDeadOrDying()) {
            return false;
        }
        long now = player.level().getGameTime();
        AuraWindow current = ACTIVE.get(player.getUUID());
        if (current != null && current.endTick > now) {
            return false;
        }
        ChargeReservation reservation = calculateReservation(player, state);
        if (reservation == null) {
            return false;
        }
        long baseEnd = now + BASE_DURATION_TICKS;
        long maxEnd = baseEnd + BASE_DURATION_TICKS / 2L;
        ACTIVE.put(player.getUUID(), new AuraWindow(level, baseEnd, maxEnd));
        commitReservation(player, reservation);
        return true;
    }

    public static int multiplierAt(ServerLevel level, BlockPos pos) {
        return multiplierAt(level, Vec3.atCenterOf(pos));
    }

    public static int multiplierAt(ServerLevel level, Vec3 position) {
        if (ACTIVE.isEmpty()) {
            return 1;
        }
        long now = level.getGameTime();
        int best = 1;
        MinecraftServer server = level.getServer();
        for (Map.Entry<UUID, AuraWindow> entry : ACTIVE.entrySet()) {
            AuraWindow window = entry.getValue();
            if (window.endTick <= now) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null || owner.isDeadOrDying() || owner.level() != level
                    || owner.position().distanceToSqr(position) > RADIUS_SQUARED) {
                continue;
            }
            best = Math.max(best, window.level >= 2 ? 7 : 3);
            if (best >= 7) {
                break;
            }
        }
        return best;
    }

    public static int extraTicksAt(ServerLevel level, BlockPos pos) {
        return Math.max(0, multiplierAt(level, pos) - 1);
    }

    public static int extraTicksAt(ServerLevel level, Vec3 position) {
        return Math.max(0, multiplierAt(level, position) - 1);
    }

    public static int remainingTicks(ServerPlayer player) {
        AuraWindow window = ACTIVE.get(player.getUUID());
        if (window == null || player.isDeadOrDying()) return 0;
        long remaining = Math.max(0L, window.endTick - player.level().getGameTime());
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    public static int radius() {
        return RADIUS;
    }

    /** Canonical crop/sapling scope plus vanilla farmable stalk crops and modded CropBlock subclasses. */
    public static boolean isCompatibleGrowthBlock(BlockState state) {
        Object block = state.getBlock();
        return block instanceof CropBlock
                || block instanceof StemBlock
                || block instanceof NetherWartBlock
                || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof SaplingBlock
                || block instanceof BambooSaplingBlock
                || block instanceof BambooStalkBlock
                || block instanceof SugarCaneBlock
                || block instanceof CactusBlock
                || block instanceof PitcherCropBlock;
    }

    private static void afterKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)
                || !CompanionAugmentService.isHostileEntity(killed)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, AREA_ACCELERATION_ID) <= 0) {
            return;
        }

        int cycle = boundedLevel(state, COMBAT_CYCLE_ID);
        if (cycle > 0) {
            int reduction = 20 * cycle;
            if (EntityCompatibilityProfileService.isBoss(killed)) {
                reduction *= 5;
            }
            reduceCooldown(player, state, reduction);
            state = AugmentationPlayerStateService.state(player);
        }

        AuraWindow window = ACTIVE.get(player.getUUID());
        if (window != null && window.endTick > level.getGameTime() && boundedLevel(state, OVERTIME_ID) > 0) {
            int extension = EntityCompatibilityProfileService.isBoss(killed) ? 80 : 20;
            long previousEnd = window.endTick;
            window.endTick = Math.min(window.maxEndTick, window.endTick + extension);
            if (window.endTick > previousEnd) {
                int remaining = (int) Math.min(Integer.MAX_VALUE,
                        Math.max(0L, window.endTick - level.getGameTime()));
                ActivePresentationService.notifyState(
                        player,
                        AREA_ACCELERATION_ID,
                        ActivePresentationPayload.SUSTAINED,
                        remaining,
                        RADIUS
                );
            }
        }
    }

    private static void reduceCooldown(ServerPlayer player, AugmentationPlayerState state, int ticks) {
        if (ticks <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        AugmentationPlayerState next = state;
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            long ready = state.cooldownReadyTick(AREA_ACCELERATION_ID);
            if (ready > now) {
                next = state.withCooldownReadyTick(AREA_ACCELERATION_ID, Math.max(now, ready - ticks));
            }
        } else {
            String first = chargeKey(0);
            String second = chargeKey(1);
            String earlier = state.cooldownReadyTick(first) <= state.cooldownReadyTick(second) ? first : second;
            String later = earlier.equals(first) ? second : first;
            int remaining = ticks;
            for (String key : new String[]{earlier, later}) {
                if (remaining <= 0) break;
                long ready = next.cooldownReadyTick(key);
                if (ready <= now) continue;
                long applied = Math.min(ready - now, remaining);
                next = next.withCooldownReadyTick(key, ready - applied);
                remaining -= (int) applied;
            }
        }
        if (!next.equals(state)) {
            AugmentationPlayerStateService.replace(player, next);
        }
    }

    private static ChargeReservation calculateReservation(ServerPlayer player, AugmentationPlayerState state) {
        long now = player.level().getGameTime();
        int cooldown = BASE_COOLDOWN_TICKS;
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            cooldown = Math.max(1, (int) Math.round(BASE_COOLDOWN_TICKS
                    * (1.0D - ExpansionTuning.current().abilityHasteReduction())));
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            return state.cooldownReadyTick(AREA_ACCELERATION_ID) > now
                    ? null
                    : new ChargeReservation(AREA_ACCELERATION_ID, now + cooldown);
        }
        String first = chargeKey(0);
        String second = chargeKey(1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else return null;
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new ChargeReservation(selected, queueTail + cooldown);
    }

    private static void commitReservation(ServerPlayer player, ChargeReservation reservation) {
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(
                player,
                latest.withCooldownReadyTick(reservation.key, reservation.readyTick)
        );
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, AuraWindow>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AuraWindow> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()
                    || entry.getValue().endTick <= player.level().getGameTime()) {
                iterator.remove();
            }
        }
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(int index) {
        return AREA_ACCELERATION_ID + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class AuraWindow {
        private final int level;
        private long endTick;
        private final long maxEndTick;

        private AuraWindow(int level, long endTick, long maxEndTick) {
            this.level = level;
            this.endTick = endTick;
            this.maxEndTick = maxEndTick;
        }
    }

    private record ChargeReservation(String key, long readyTick) {
    }
}
