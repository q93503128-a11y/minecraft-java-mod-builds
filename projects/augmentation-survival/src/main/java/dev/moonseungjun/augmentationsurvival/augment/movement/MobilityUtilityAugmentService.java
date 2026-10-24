package dev.moonseungjun.augmentationsurvival.augment.movement;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/** Server-owned conditional movement runtime for flowing water and directly controlled mounts. */
public final class MobilityUtilityAugmentService {
    private static final String RIDE_CURRENT_ID = id("ride_current");
    private static final String RIDER_ID = id("rider");

    private static final Identifier RIDE_CURRENT_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_mobility/ride_current"
    );
    private static final Identifier RIDER_SPEED_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_mobility/rider_speed"
    );
    private static final Identifier RIDER_JUMP_MODIFIER = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_mobility/rider_jump"
    );

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private MobilityUtilityAugmentService() {
    }

    public static void initialize() {
        AugmentCatalog.require(RIDE_CURRENT_ID);
        AugmentCatalog.require(RIDER_ID);
        ServerPlayerEvents.JOIN.register(player -> reconcile(
                player,
                AugmentationPlayerStateService.state(player)
        ));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            clearRuntime(newPlayer);
            reconcile(newPlayer, AugmentationPlayerStateService.state(newPlayer));
        });
        ServerPlayerEvents.LEAVE.register(MobilityUtilityAugmentService::clearRuntime);
        ServerTickEvents.END_SERVER_TICK.register(MobilityUtilityAugmentService::tickRuntime);
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        int currentLevel = boundedLevel(state, RIDE_CURRENT_ID);
        int riderLevel = boundedLevel(state, RIDER_ID);
        if (currentLevel <= 0 && riderLevel <= 0) {
            clearRuntime(player);
            return;
        }
        RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        if (currentLevel <= 0) {
            removeRideCurrent(player);
        }
        if (riderLevel <= 0) {
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            if (runtime != null) {
                clearMount(runtime);
            }
        }
    }

    private static void tickRuntime(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RuntimeState>> iterator = RUNTIME.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RuntimeState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            RuntimeState runtime = entry.getValue();
            if (player == null || player.isDeadOrDying()) {
                clearMount(runtime);
                iterator.remove();
                continue;
            }
            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            int currentLevel = boundedLevel(state, RIDE_CURRENT_ID);
            int riderLevel = boundedLevel(state, RIDER_ID);
            if (currentLevel <= 0 && riderLevel <= 0) {
                removeRideCurrent(player);
                clearMount(runtime);
                iterator.remove();
                continue;
            }
            applyRideCurrent(player, currentLevel);
            applyRider(player, runtime, riderLevel);
        }
    }

    private static void applyRideCurrent(ServerPlayer player, int level) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        if (level <= 0 || !player.isSwimming()) {
            movement.removeModifier(RIDE_CURRENT_MODIFIER);
            return;
        }
        BlockPos pos = player.blockPosition();
        FluidState fluid = player.level().getFluidState(pos);
        Vec3 flow = fluid.getFlow(player.level(), pos);
        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontalFlow = new Vec3(flow.x, 0.0D, flow.z);
        Vec3 horizontalMotion = new Vec3(motion.x, 0.0D, motion.z);
        if (horizontalFlow.lengthSqr() <= 1.0E-7D
                || horizontalMotion.lengthSqr() <= 1.0E-7D
                || horizontalFlow.dot(horizontalMotion) <= 0.0D) {
            movement.removeModifier(RIDE_CURRENT_MODIFIER);
            return;
        }
        movement.addOrUpdateTransientModifier(new AttributeModifier(
                RIDE_CURRENT_MODIFIER,
                MobilityUtilityTuning.current().rideCurrentMovementBonus(level),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static void applyRider(ServerPlayer player, RuntimeState runtime, int level) {
        Entity vehicle = player.getVehicle();
        Animal mount = vehicle instanceof Animal animal
                && animal.getControllingPassenger() == player
                && !animal.isInWater()
                ? animal
                : null;
        if (level <= 0 || mount == null) {
            clearMount(runtime);
            return;
        }
        if (runtime.mount != mount) {
            clearMount(runtime);
            runtime.mount = mount;
        }
        applyMountModifier(
                mount,
                Attributes.MOVEMENT_SPEED,
                RIDER_SPEED_MODIFIER,
                MobilityUtilityTuning.current().riderMovementBonus(level)
        );
        applyMountModifier(
                mount,
                Attributes.JUMP_STRENGTH,
                RIDER_JUMP_MODIFIER,
                MobilityUtilityTuning.current().riderJumpBonus(level)
        );
    }

    private static void applyMountModifier(
            LivingEntity mount,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            Identifier id,
            double amount
    ) {
        AttributeInstance instance = mount.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                id,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static void clearRuntime(ServerPlayer player) {
        removeRideCurrent(player);
        RuntimeState runtime = RUNTIME.remove(player.getUUID());
        if (runtime != null) {
            clearMount(runtime);
        }
    }

    private static void clearMount(RuntimeState runtime) {
        if (runtime.mount == null) {
            return;
        }
        AttributeInstance movement = runtime.mount.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(RIDER_SPEED_MODIFIER);
        }
        AttributeInstance jump = runtime.mount.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            jump.removeModifier(RIDER_JUMP_MODIFIER);
        }
        runtime.mount = null;
    }

    private static void removeRideCurrent(ServerPlayer player) {
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(RIDE_CURRENT_MODIFIER);
        }
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class RuntimeState {
        private Animal mount;
    }
}
