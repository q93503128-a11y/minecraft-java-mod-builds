package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.R01RegalhartMythicEffectState;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartRewardRules;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative Hartcrown Spear unique-effect runtime.
 *
 * <p>Better Combat still owns visible melee animation and candidate reach. For project-owned
 * targets, Hart's Momentum is consumed only after project target/damage/cadence authority has
 * accepted the melee basic; for compatibility targets, the existing Better Combat/vanilla damage
 * invocation is the accepted melee candidate. The lunge therefore does not manufacture a new
 * target or extend Better Combat reach. Movement uses Minecraft's collision-resolved SELF movement
 * instead of teleporting through authored terrain.</p>
 */
public final class R01RegalhartMythicRuntime {
    public static final double HARTS_MOMENTUM_LUNGE_BLOCKS = 1.5;
    private static final double DIRECTION_EPSILON_SQR = 1.0e-9;

    private static final ConcurrentHashMap<UUID, R01RegalhartMythicEffectState> STATES =
            new ConcurrentHashMap<>();

    private R01RegalhartMythicRuntime() {
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            boolean equipped = hasHartcrownEquipped(player);
            if (!equipped || !player.isAlive() || player.isSpectator()) {
                R01RegalhartMythicEffectState existing = STATES.get(playerId);
                if (existing != null) {
                    existing.resetMovementSegment();
                }
                continue;
            }

            Vec3 velocity = player.getDeltaMovement();
            boolean horizontalMovement =
                    velocity.x * velocity.x + velocity.z * velocity.z > 0.0;
            boolean movingAtSprintSpeed =
                    player.isSprinting() && horizontalMovement;
            state(playerId).recordMovementTick(
                    movingAtSprintSpeed,
                    player.level().getGameTime()
            );
        }
    }

    public static HartsMomentumApplication onAcceptedMeleeBasic(
            ServerPlayer player,
            LivingEntity target,
            long gameTick
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        if (player.level().isClientSide()
                || player.level() != target.level()
                || !player.isAlive()
                || player.isSpectator()
                || !hasHartcrownEquipped(player)) {
            return HartsMomentumApplication.inactive();
        }

        R01RegalhartMythicEffectState.Trigger trigger =
                state(player.getUUID()).tryConsume(gameTick);
        if (!trigger.triggered()) {
            return HartsMomentumApplication.inactive();
        }

        double moved = lungeToward(player, target);
        return new HartsMomentumApplication(
                true,
                trigger.poiseMultiplier(),
                moved,
                trigger.nextReadyTick()
        );
    }

    public static void reset(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
        }
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    static R01RegalhartMythicEffectState state(UUID playerId) {
        return STATES.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                ignored -> new R01RegalhartMythicEffectState()
        );
    }

    private static double lungeToward(
            ServerPlayer player,
            LivingEntity target
    ) {
        Vec3 offset = target.position().subtract(player.position());
        Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
        double distanceSqr = horizontal.lengthSqr();
        if (distanceSqr <= DIRECTION_EPSILON_SQR) {
            return 0.0;
        }

        double distance = Math.sqrt(distanceSqr);
        double requested = Math.min(
                HARTS_MOMENTUM_LUNGE_BLOCKS,
                distance
        );
        Vec3 movement = horizontal.scale(requested / distance);
        Vec3 before = player.position();

        player.move(MoverType.SELF, movement);

        Vec3 applied = player.position().subtract(before);
        return Math.hypot(applied.x, applied.z);
    }

    private static boolean hasHartcrownEquipped(
            ServerPlayer player
    ) {
        return PlayerEquipmentService.state(player)
                .item(ProjectEquipmentSlot.MAIN_WEAPON)
                .map(item -> R01RegalhartRewardRules.HARTCROWN_SPEAR_ID
                        .equals(item.itemId()))
                .orElse(false);
    }

    public record HartsMomentumApplication(
            boolean triggered,
            double poiseMultiplier,
            double horizontalLungeBlocks,
            long nextReadyTick
    ) {
        public HartsMomentumApplication {
            if (!Double.isFinite(poiseMultiplier)
                    || poiseMultiplier < 1.0
                    || !Double.isFinite(horizontalLungeBlocks)
                    || horizontalLungeBlocks < 0.0
                    || horizontalLungeBlocks
                            > HARTS_MOMENTUM_LUNGE_BLOCKS + 1.0e-6) {
                throw new IllegalArgumentException(
                        "Invalid Hart's Momentum application."
                );
            }
            if (!triggered
                    && (poiseMultiplier != 1.0
                            || horizontalLungeBlocks != 0.0)) {
                throw new IllegalArgumentException(
                        "Inactive Hart's Momentum cannot carry bonuses."
                );
            }
        }

        public static HartsMomentumApplication inactive() {
            return new HartsMomentumApplication(
                    false,
                    1.0,
                    0.0,
                    Long.MIN_VALUE / 4
            );
        }
    }
}
