package dev.moonseungjun.augmentationsurvival.augment.projectile;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Tracks the one-second stationary full-draw contract and snapshots the next root bow arrow. */
public final class FocusedAimAugmentService {
    private static final String FOCUSED_AIM_ID = id("focused_aim");
    private static final String PRECISE_AIM_ID = id("precise_aim");
    private static final String LAST_STRENGTH_ID = id("last_strength");
    private static final double STATIONARY_EPSILON_SQUARED = 1.0E-8D;

    private static final Map<UUID, AimState> AIM = new HashMap<>();
    private static final Map<UUID, PendingShot> PENDING = new HashMap<>();
    private static final Map<UUID, Double> FOCUSED_PROJECTILES = new HashMap<>();

    private FocusedAimAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(FOCUSED_AIM_ID);
        ServerTickEvents.END_SERVER_TICK.register(FocusedAimAugmentService::tickAim);
        ServerEntityEvents.ENTITY_LOAD.register(FocusedAimAugmentService::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(FocusedAimAugmentService::onEntityUnload);
        ServerPlayerEvents.JOIN.register(FocusedAimAugmentService::clearPlayer);
        ServerPlayerEvents.LEAVE.register(FocusedAimAugmentService::clearPlayer);
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {
                    clearPlayer(oldPlayer);
                    clearPlayer(newPlayer);
                }
        );
    }

    /** Called from the root projectile-weapon shot boundary before vanilla spawns the arrow. */
    public static void prepareBowShot(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, FOCUSED_AIM_ID);
        if (level <= 0) {
            return;
        }
        AimState aim = AIM.get(player.getUUID());
        if (aim == null
                || aim.stationaryFullDrawTicks
                        < UtilityAugmentTuning.current().focusedAimStationaryTicks()) {
            return;
        }
        PENDING.put(
                player.getUUID(),
                new PendingShot(
                        player.level().getGameTime(),
                        UtilityAugmentTuning.current().focusedAimDamageBonus(level)
                )
        );
    }

    /**
     * Returns a complete root-projectile outgoing result only for a snapshotted focused arrow.
     * The focused percentage is added on the same stat axis as precise aim/last strength before
     * conditional combat expansion runs.
     */
    public static @Nullable Float modifyFocusedProjectileDamage(
            LivingEntity target,
            DamageSource source,
            float baseDamage
    ) {
        if (!(baseDamage > 0.0F) || !Float.isFinite(baseDamage)) {
            return null;
        }
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)
                || !(source.getEntity() instanceof ServerPlayer attacker)
                || CombatProvenanceContext.resolve(source) != CombatProvenance.ROOT_PROJECTILE) {
            return null;
        }
        Double focusedBonus = FOCUSED_PROJECTILES.get(arrow.getUUID());
        if (focusedBonus == null || !(focusedBonus > 0.0D)) {
            return null;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        CombatTuning.Snapshot tuning = CombatTuning.current();
        double bonus = focusedBonus
                + tuning.preciseAimPerLevel() * boundedLevel(state, PRECISE_AIM_ID);
        if (attacker.getMaxHealth() > 0.0F
                && attacker.getHealth() / attacker.getMaxHealth()
                        <= tuning.lastStrengthHealthThreshold()) {
            bonus += tuning.lastStrengthPerLevel() * boundedLevel(state, LAST_STRENGTH_ID);
        }
        float scaled = safeScale(baseDamage, 1.0D + bonus);
        return CombatExpansionService.modifyOutgoingDamage(target, source, scaled);
    }

    private static void tickAim(MinecraftServer server) {
        long cleanupBefore = Long.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            int level = boundedLevel(state, FOCUSED_AIM_ID);
            if (level <= 0 || player.isDeadOrDying()) {
                AIM.remove(player.getUUID());
                PENDING.remove(player.getUUID());
                continue;
            }

            AimState aim = AIM.computeIfAbsent(player.getUUID(), ignored -> new AimState());
            Vec3 position = player.position();
            boolean stationary = aim.lastPosition != null
                    && aim.lastPosition.distanceToSqr(position) <= STATIONARY_EPSILON_SQUARED;
            boolean fullDraw = player.isUsingItem()
                    && player.getUseItem().getItem() instanceof BowItem
                    && player.getTicksUsingItem()
                            >= UtilityAugmentTuning.current().focusedAimFullDrawTicks();
            if (fullDraw && stationary) {
                aim.stationaryFullDrawTicks++;
            } else {
                aim.stationaryFullDrawTicks = 0;
            }
            aim.lastPosition = position;
            cleanupBefore = Math.min(cleanupBefore, player.level().getGameTime() - 1L);
        }

        if (cleanupBefore != Long.MAX_VALUE) {
            long threshold = cleanupBefore;
            PENDING.entrySet().removeIf(entry -> entry.getValue().gameTick < threshold);
        }
    }

    private static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer owner)) {
            return;
        }
        PendingShot pending = PENDING.get(owner.getUUID());
        if (pending == null || pending.gameTick != level.getGameTime()) {
            return;
        }

        if (arrow instanceof ProjectileRuntimeCarrier carrier) {
            ProjectileRuntimeState runtime = carrier.augmentationSurvival$getProjectileRuntime();
            if (runtime != null && runtime.provenance() != CombatProvenance.ROOT_PROJECTILE) {
                return;
            }
        }

        FOCUSED_PROJECTILES.put(arrow.getUUID(), pending.damageBonus);
        PENDING.remove(owner.getUUID());
    }

    private static void onEntityUnload(Entity entity, ServerLevel level) {
        if (entity instanceof AbstractArrow arrow) {
            FOCUSED_PROJECTILES.remove(arrow.getUUID());
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        AIM.remove(player.getUUID());
        PENDING.remove(player.getUUID());
    }

    private static float safeScale(float damage, double multiplier) {
        double scaled = damage * multiplier;
        if (!Double.isFinite(scaled)) {
            return damage;
        }
        return (float) Math.max(0.0D, Math.min(Float.MAX_VALUE, scaled));
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class AimState {
        private Vec3 lastPosition;
        private int stationaryFullDrawTicks;
    }

    private record PendingShot(long gameTick, double damageBonus) {
    }
}
