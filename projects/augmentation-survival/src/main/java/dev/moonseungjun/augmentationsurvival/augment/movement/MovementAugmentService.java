package dev.moonseungjun.augmentationsurvival.augment.movement;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.mixin.LivingEntityJumpPowerAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Server-owned shared movement runtime.
 *
 * <p>Only players who own a movement augment that needs live state are tracked. Static step-height
 * reconciliation is event-driven and does not require a global player scan.</p>
 */
public final class MovementAugmentService {
    private static final String CATCH_BREATH_ID = id("catch_breath");
    private static final String NOCTURNAL_ID = id("nocturnal");
    private static final String SUN_AFFINITY_ID = id("sun_affinity");
    private static final String WEBBED_FEET_ID = id("webbed_feet");
    private static final String STEP_UP_ID = id("step_up");
    private static final String LONG_JUMP_ID = id("long_jump");
    private static final String MOMENTUM_SHIFT_ID = id("momentum_shift");
    private static final String DOUBLE_JUMP_ID = id("double_jump");
    private static final String AERIAL_COMBO_ID = id("aerial_combo");

    private static final Identifier CONDITIONAL_MOVEMENT_ID = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_movement/conditional_speed"
    );
    private static final Identifier STEP_HEIGHT_ID = Identifier.fromNamespaceAndPath(
            AugmentationSurvival.MOD_ID,
            "augment_movement/step_height"
    );

    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private MovementAugmentService() {
    }

    public static void initialize() {
        ServerPlayerEvents.JOIN.register(player -> {
            clearRuntime(player);
            reconcile(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            clearRuntime(newPlayer);
            reconcile(newPlayer);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                clearRuntime(player);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(MovementAugmentService::tickRuntime);
    }

    public static void reconcile(ServerPlayer player) {
        reconcile(player, AugmentationPlayerStateService.state(player));
    }

    public static void reconcile(ServerPlayer player, AugmentationPlayerState state) {
        applyStepHeight(player, boundedLevel(state, STEP_UP_ID));

        if (!needsRuntime(state)) {
            RUNTIME.remove(player.getUUID());
            removeConditionalMovement(player);
            return;
        }

        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null) {
            runtime = new RuntimeState();
            boolean resetContact = isResetContact(player);
            runtime.wasGrounded = player.onGround();
            runtime.wasResetContact = resetContact;
            runtime.airJumpAvailable = resetContact && boundedLevel(state, DOUBLE_JUMP_ID) > 0;
            RUNTIME.put(player.getUUID(), runtime);
        } else if (boundedLevel(state, DOUBLE_JUMP_ID) <= 0
                && boundedLevel(state, AERIAL_COMBO_ID) <= 0) {
            runtime.airJumpAvailable = false;
        }
    }

    /** Mirrors only the held state needed by Aerial Combo; actual jump authority stays server-side. */
    public static void setJumpHeld(ServerPlayer player, boolean held) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, DOUBLE_JUMP_ID) <= 0 && boundedLevel(state, AERIAL_COMBO_ID) <= 0) {
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            if (runtime != null) runtime.jumpHeld = false;
            return;
        }
        reconcile(player, state);
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime != null) runtime.jumpHeld = held;
    }

    /**
     * Canonical Aerial Combo entry. Called only after Player.attack successfully reaches a living
     * target, so the recharge cannot drift away from the actual player melee boundary.
     */
    public static void onSuccessfulRootMelee(ServerPlayer attacker, LivingEntity target) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(attacker);
        if (boundedLevel(state, AERIAL_COMBO_ID) <= 0
                || isResetContact(attacker)
                || CompanionAugmentService.isDominated(target)
                || !OffensiveTargetContext.isValid(attacker, target)) {
            return;
        }

        reconcile(attacker, state);
        RuntimeState runtime = RUNTIME.get(attacker.getUUID());
        if (runtime == null) return;
        runtime.airJumpAvailable = true;
        if (runtime.jumpHeld) {
            tryAirJump(attacker);
        }
    }

    /** Called from the inferred vanilla ground-jump boundary on the logical server. */
    public static void afterGroundJump(ServerPlayer player) {
        if (CombatAugmentService.isActionLocked(player)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, LONG_JUMP_ID);
        if (level <= 0 || !player.isSprinting()) {
            return;
        }

        double multiplier = 1.0D
                + MovementTuning.current().longJumpHorizontalBonusPerLevel() * level;
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * multiplier, movement.y, movement.z * multiplier);
        syncMotion(player);
    }

    /** Handles a client jump-key request. The server owns all eligibility and consumption checks. */
    public static boolean tryAirJump(ServerPlayer player) {
        if (player.isSpectator()
                || player.getAbilities().flying
                || player.isPassenger()
                || CombatAugmentService.isActionLocked(player)
                || isResetContact(player)) {
            return false;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (boundedLevel(state, DOUBLE_JUMP_ID) <= 0
                && boundedLevel(state, AERIAL_COMBO_ID) <= 0) {
            return false;
        }

        reconcile(player, state);
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || !runtime.airJumpAvailable) {
            return false;
        }

        MovementTuning.Snapshot tuning = MovementTuning.current();
        float jumpPower = ((LivingEntityJumpPowerAccessor) player)
                .augmentationSurvival$invokeGetJumpPower();
        double vertical = jumpPower * tuning.doubleJumpVerticalMultiplier();
        Vec3 movement = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        double x = movement.x;
        double z = movement.z;
        if (horizontalSpeed > 1.0E-5D) {
            double boostedSpeed = horizontalSpeed + tuning.doubleJumpHorizontalReboost();
            double scale = boostedSpeed / horizontalSpeed;
            x *= scale;
            z *= scale;
        }
        player.setDeltaMovement(x, Math.max(movement.y, vertical), z);
        player.resetFallDistance();
        runtime.maxObservedFallDistance = 0.0D;
        runtime.airJumpAvailable = false;
        syncMotion(player);
        return true;
    }

    private static void tickRuntime(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RuntimeState>> iterator = RUNTIME.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RuntimeState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isDeadOrDying()) {
                iterator.remove();
                continue;
            }

            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            if (!needsRuntime(state)) {
                removeConditionalMovement(player);
                iterator.remove();
                continue;
            }

            RuntimeState runtime = entry.getValue();
            MovementTuning.Snapshot tuning = MovementTuning.current();
            long now = player.level().getGameTime();
            boolean grounded = player.onGround();
            boolean resetContact = isResetContact(player);

            if (!resetContact) {
                runtime.maxObservedFallDistance = Math.max(
                        runtime.maxObservedFallDistance,
                        player.fallDistance
                );
            } else {
                if (grounded && !runtime.wasGrounded
                        && runtime.maxObservedFallDistance >= tuning.momentumMinimumFallDistance()
                        && boundedLevel(state, MOMENTUM_SHIFT_ID) > 0) {
                    runtime.momentumUntilTick = now + tuning.momentumDurationTicks();
                }
                runtime.maxObservedFallDistance = 0.0D;
                if (!runtime.wasResetContact && boundedLevel(state, DOUBLE_JUMP_ID) > 0) {
                    runtime.airJumpAvailable = true;
                }
            }

            int doubleJump = boundedLevel(state, DOUBLE_JUMP_ID);
            int aerialCombo = boundedLevel(state, AERIAL_COMBO_ID);
            if (doubleJump <= 0 && (aerialCombo <= 0 || resetContact)) {
                runtime.airJumpAvailable = false;
            }

            if (player.isSprinting()) {
                runtime.nonSprintingTicks = 0;
            } else {
                runtime.nonSprintingTicks = Math.min(
                        tuning.catchBreathIdleTicks(),
                        runtime.nonSprintingTicks + 1
                );
            }

            applyConditionalMovement(player, state, runtime, now);
            runtime.wasGrounded = grounded;
            runtime.wasResetContact = resetContact;
        }
    }

    private static void applyConditionalMovement(
            ServerPlayer player,
            AugmentationPlayerState state,
            RuntimeState runtime,
            long now
    ) {
        MovementTuning.Snapshot tuning = MovementTuning.current();
        double bonus = 0.0D;

        int catchBreath = boundedLevel(state, CATCH_BREATH_ID);
        if (catchBreath > 0 && runtime.nonSprintingTicks >= tuning.catchBreathIdleTicks()) {
            bonus += tuning.catchBreathMovementPerLevel() * catchBreath;
        }

        int nocturnal = boundedLevel(state, NOCTURNAL_ID);
        int sunAffinity = boundedLevel(state, SUN_AFFINITY_ID);
        if (isDaytime(player)) {
            bonus += tuning.sunAffinityMovementPerLevel() * sunAffinity;
        } else {
            bonus += tuning.nocturnalMovementPerLevel() * nocturnal;
        }

        bonus += webbedFeetMovementBonus(state, player.isInWater());

        int momentum = boundedLevel(state, MOMENTUM_SHIFT_ID);
        if (momentum > 0 && runtime.momentumUntilTick > now) {
            bonus += tuning.momentumMovementPerLevel() * momentum;
        }

        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance == null) {
            throw new IllegalStateException("Player is missing movement speed attribute");
        }
        if (!(bonus > 0.0D)) {
            instance.removeModifier(CONDITIONAL_MOVEMENT_ID);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                CONDITIONAL_MOVEMENT_ID,
                bonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static double webbedFeetMovementBonus(AugmentationPlayerState state, boolean inWater) {
        if (!inWater) {
            return 0.0D;
        }
        int level = boundedLevel(state, WEBBED_FEET_ID);
        if (level <= 0) {
            return 0.0D;
        }
        return MovementTuning.current().webbedFeetMovementPerLevel() * level;
    }

    private static void applyStepHeight(ServerPlayer player, int level) {
        AttributeInstance instance = player.getAttribute(Attributes.STEP_HEIGHT);
        if (instance == null) {
            throw new IllegalStateException("Player is missing step height attribute");
        }
        if (level <= 0) {
            instance.removeModifier(STEP_HEIGHT_ID);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                STEP_HEIGHT_ID,
                MovementTuning.current().stepUpAdditionalHeight(),
                AttributeModifier.Operation.ADD_VALUE
        ));
    }

    private static boolean isDaytime(ServerPlayer player) {
        long dayTime = Math.floorMod(player.level().getOverworldClockTime(), 24_000L);
        return dayTime < 13_000L;
    }

    private static void clearRuntime(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
        removeConditionalMovement(player);
    }

    private static void removeConditionalMovement(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (instance != null) {
            instance.removeModifier(CONDITIONAL_MOVEMENT_ID);
        }
    }

    private static boolean needsRuntime(AugmentationPlayerState state) {
        return boundedLevel(state, CATCH_BREATH_ID) > 0
                || boundedLevel(state, NOCTURNAL_ID) > 0
                || boundedLevel(state, SUN_AFFINITY_ID) > 0
                || boundedLevel(state, WEBBED_FEET_ID) > 0
                || boundedLevel(state, MOMENTUM_SHIFT_ID) > 0
                || boundedLevel(state, DOUBLE_JUMP_ID) > 0
                || boundedLevel(state, AERIAL_COMBO_ID) > 0;
    }

    private static boolean isResetContact(ServerPlayer player) {
        return player.onGround()
                || player.onClimbable()
                || player.level().getFluidState(player.blockPosition()).is(FluidTags.WATER);
    }

    private static void syncMotion(ServerPlayer player) {
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
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

    private static final class RuntimeState {
        private int nonSprintingTicks;
        private long momentumUntilTick;
        private double maxObservedFallDistance;
        private boolean airJumpAvailable;
        private boolean jumpHeld;
        private boolean wasGrounded;
        private boolean wasResetContact;
    }
}
