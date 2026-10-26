package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionPoolAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeAccessor;
import dev.moonseungjun.augmentationsurvival.mixin.ProtectionRuntimeStateAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Overtime extension boundary for the manual Active Shield protection-pool instance. */
public final class ShieldOvertimeService {
    private static final String SHIELD_ID = id("shield");
    private static final String OVERTIME_ID = id("overtime");
    private static final String SHIELD_POOL_KEY = "SHIELD:" + ProtectionAugmentService.ACTIVE_SHIELD_POOL;
    private static final Map<UUID, Long> MAX_END_TICKS = new HashMap<>();

    private ShieldOvertimeService() { }

    public static void initialize() {
        AugmentCatalog.require(SHIELD_ID);
        AugmentCatalog.require(OVERTIME_ID);
        ServerPlayerEvents.JOIN.register(player -> MAX_END_TICKS.remove(player.getUUID()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> MAX_END_TICKS.remove(newPlayer.getUUID()));
        ServerPlayerEvents.LEAVE.register(player -> MAX_END_TICKS.remove(player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MAX_END_TICKS.clear());
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(ShieldOvertimeService::afterKill);
    }

    /** Called only after a successful manual Shield cast, never from Replay. */
    public static void recordManualShield(ServerPlayer player) {
        int duration = CombatTuning.current().shieldDurationTicks();
        long baseEnd = player.level().getGameTime() + duration;
        long maximum = baseEnd + Math.round(
                duration * AdvancedActiveTuning.current().overtimeMaximumExtensionFraction()
        );
        MAX_END_TICKS.put(player.getUUID(), maximum);
    }

    private static void afterKill(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)
                || !CombatProvenanceContext.resolve(source).isRootDirect()) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, SHIELD_ID) <= 0 || boundedLevel(state, OVERTIME_ID) <= 0) {
            return;
        }
        int extension = EntityCompatibilityProfileService.isBoss(killed)
                ? AdvancedActiveTuning.current().overtimeBossExtensionTicks()
                : AdvancedActiveTuning.current().overtimeNormalExtensionTicks();
        extendManualShield(player, extension);
    }

    private static void extendManualShield(ServerPlayer player, int ticks) {
        if (ticks <= 0) return;
        Long maximum = MAX_END_TICKS.get(player.getUUID());
        if (maximum == null) return;

        Object runtime = ProtectionRuntimeAccessor.augmentationSurvival$getRuntime().get(player.getUUID());
        if (!(runtime instanceof ProtectionRuntimeStateAccessor runtimeAccessor)) {
            MAX_END_TICKS.remove(player.getUUID());
            return;
        }
        Object rawPool = runtimeAccessor.augmentationSurvival$getPools().get(SHIELD_POOL_KEY);
        if (!(rawPool instanceof ProtectionPoolAccessor pool)) {
            MAX_END_TICKS.remove(player.getUUID());
            return;
        }

        long now = player.level().getGameTime();
        long end = pool.augmentationSurvival$getExpiresAtTick();
        if (end <= now) {
            MAX_END_TICKS.remove(player.getUUID());
            return;
        }
        pool.augmentationSurvival$setExpiresAtTick(Math.min(maximum, end + ticks));
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
