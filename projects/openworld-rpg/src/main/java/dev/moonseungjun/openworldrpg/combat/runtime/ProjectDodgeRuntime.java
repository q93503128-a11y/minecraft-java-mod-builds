package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerActionRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerDefenseRuntimeState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * End-to-end server dodge authority after validated C2S intent.
 *
 * <p>The client never requests distance, i-frames or Stamina cost. A dodge moves in nine
 * collision-resolved server slices; defense state owns the six-tick i-frame and eleven-tick
 * re-entry lock. The no-input backstep distance is a production precision binding because canon
 * says only "short backward evade"; it stays isolated for playtest tuning.</p>
 */
public final class ProjectDodgeRuntime {
    public static final double LEVEL_GROUND_TRAVEL_BLOCKS = 3.20;
    public static final double NO_INPUT_BACKSTEP_BLOCKS = 2.00;
    public static final int MOVEMENT_STEPS =
            Math.toIntExact(
                    PlayerDefenseRuntimeState.DODGE_ACTION_TICKS
            );
    private static final double INPUT_EPSILON = 1.0e-4;
    private static final double MAX_PACKET_COMPONENT = 1.001;
    public static final String DODGE_ACTION_ID = "openworld_rpg:dodge";

    private static final ConcurrentHashMap<UUID, Long>
            LAST_SEQUENCE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, BufferedRequest>
            BUFFERED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ActiveDodge>
            ACTIVE = new ConcurrentHashMap<>();
    private static final List<AcceptedEvent> ACCEPTED_EVENTS =
            new ArrayList<>();

    private ProjectDodgeRuntime() {
    }

    public static RequestResult request(
            ServerPlayer player,
            float forwardIntent,
            float strafeIntent,
            long sequence
    ) {
        Objects.requireNonNull(player, "player");
        if (!validIntent(forwardIntent, strafeIntent)
                || sequence < 0L
                || !player.isAlive()
                || player.isSpectator()) {
            return RequestResult.rejected(
                    RequestStatus.INVALID
            );
        }

        UUID playerId = player.getUUID();
        long lastSequence = LAST_SEQUENCE.getOrDefault(
                playerId,
                -1L
        );
        if (sequence <= lastSequence) {
            return RequestResult.rejected(
                    RequestStatus.DUPLICATE
            );
        }
        LAST_SEQUENCE.put(playerId, sequence);

        long nowTick = player.level().getGameTime();
        if (ProjectPlayerActionRuntime.hardReactionActive(
                playerId,
                nowTick
        )) {
            return RequestResult.rejected(
                    RequestStatus.LOCKED
            );
        }

        DirectionIntent intent = DirectionIntent.resolve(
                forwardIntent,
                strafeIntent
        );

        if (ProjectPlayerActionRuntime.canBufferDodge(
                playerId,
                nowTick
        )) {
            var action = ProjectPlayerActionRuntime.snapshot(
                    playerId,
                    nowTick
            );
            BUFFERED.put(
                    playerId,
                    new BufferedRequest(
                            player.level(),
                            intent,
                            sequence,
                            action.actionId(),
                            action.dodgeCancelAtTick()
                    )
            );
            return new RequestResult(
                    RequestStatus.BUFFERED,
                    intent.directionCode()
            );
        }

        return tryAccept(
                player,
                intent,
                nowTick
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");

        BUFFERED.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            BufferedRequest request = entry.getValue();
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(playerId);
            if (player == null
                    || !player.isAlive()
                    || player.level() != request.level()) {
                return true;
            }

            long nowTick = player.level().getGameTime();
            if (ProjectPlayerActionRuntime.hardReactionActive(
                    playerId,
                    nowTick
            )) {
                return true;
            }

            var action = ProjectPlayerActionRuntime.snapshot(
                    playerId,
                    nowTick
            );
            if (nowTick < request.executeAtTick()) {
                return !sameBufferedAction(
                        action,
                        request
                );
            }
            if (!sameBufferedAction(action, request)
                    && action.active()) {
                return true;
            }

            tryAccept(
                    player,
                    request.intent(),
                    nowTick
            );
            return true;
        });

        ACTIVE.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            ActiveDodge dodge = entry.getValue();
            ServerPlayer player = server.getPlayerList()
                    .getPlayer(playerId);
            if (player == null
                    || !player.isAlive()
                    || player.level() != dodge.level()) {
                return true;
            }
            long nowTick = player.level().getGameTime();
            if (ProjectPlayerActionRuntime.hardReactionActive(
                    playerId,
                    nowTick
            )) {
                return true;
            }
            advanceOneStep(player, dodge);
            return dodge.completed();
        });
    }

    public static List<AcceptedEvent> drainAcceptedEvents() {
        synchronized (ACCEPTED_EVENTS) {
            List<AcceptedEvent> snapshot =
                    List.copyOf(ACCEPTED_EVENTS);
            ACCEPTED_EVENTS.clear();
            return snapshot;
        }
    }

    public static void reset(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        UUID playerId = player.getUUID();
        BUFFERED.remove(playerId);
        ACTIVE.remove(playerId);
        LAST_SEQUENCE.remove(playerId);
    }

    public static void disconnect(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        BUFFERED.remove(playerId);
        ACTIVE.remove(playerId);
        LAST_SEQUENCE.remove(playerId);
    }

    public static boolean active(
            UUID playerId
    ) {
        return ACTIVE.containsKey(
                Objects.requireNonNull(playerId, "playerId")
        );
    }

    private static RequestResult tryAccept(
            ServerPlayer player,
            DirectionIntent intent,
            long nowTick
    ) {
        UUID playerId = player.getUUID();
        var resources = CombatStateServices.states()
                .getOrCreate(playerId, nowTick);
        var defense = CombatStateServices.defenseStates()
                .getOrCreate(playerId);

        boolean mobilityLocked =
                ProjectPlayerActionRuntime.hardReactionActive(
                        playerId,
                        nowTick
                );
        if (!defense.tryBeginDodge(
                resources,
                nowTick,
                mobilityLocked
        )) {
            return RequestResult.rejected(
                    RequestStatus.REJECTED
            );
        }

        defense.releaseGuard();
        var action = ProjectPlayerActionRuntime.beginAction(
                player,
                new ProjectPlayerActionRuntime.ActionSpec(
                        DODGE_ACTION_ID,
                        MOVEMENT_STEPS,
                        MOVEMENT_STEPS,
                        1.0
                )
        );
        if (!action.accepted()) {
            throw new IllegalStateException(
                    "Accepted dodge could not claim its nine-tick action window."
            );
        }

        Vec3 direction = worldDirection(
                player,
                intent
        );
        double travel = intent.noDirectionalInput()
                ? NO_INPUT_BACKSTEP_BLOCKS
                : LEVEL_GROUND_TRAVEL_BLOCKS;

        ActiveDodge dodge = new ActiveDodge(
                (ServerLevel) player.level(),
                direction,
                travel / MOVEMENT_STEPS,
                0,
                Long.MIN_VALUE / 4
        );
        ACTIVE.put(playerId, dodge);

        /*
         * Start movement in the same authoritative acceptance callback. The remaining eight slices
         * are advanced once per server tick.
         */
        advanceOneStep(player, dodge);
        synchronized (ACCEPTED_EVENTS) {
            ACCEPTED_EVENTS.add(
                    new AcceptedEvent(
                            playerId,
                            player.getId(),
                            intent.directionCode()
                    )
            );
        }
        return new RequestResult(
                RequestStatus.ACCEPTED,
                intent.directionCode()
        );
    }

    private static void advanceOneStep(
            ServerPlayer player,
            ActiveDodge dodge
    ) {
        if (dodge.completed()) {
            return;
        }
        long nowTick = player.level().getGameTime();
        if (dodge.lastStepTick() == nowTick) {
            return;
        }
        player.move(
                MoverType.SELF,
                dodge.direction().scale(
                        dodge.blocksPerStep()
                )
        );
        dodge.advance(nowTick);
    }

    private static Vec3 worldDirection(
            ServerPlayer player,
            DirectionIntent intent
    ) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(
                look.x,
                0.0,
                look.z
        );
        if (forward.lengthSqr() <= 1.0e-9) {
            double yaw = Math.toRadians(
                    player.getYRot()
            );
            forward = new Vec3(
                    -Math.sin(yaw),
                    0.0,
                    Math.cos(yaw)
            );
        } else {
            forward = forward.normalize();
        }

        if (intent.noDirectionalInput()) {
            return forward.scale(-1.0);
        }

        Vec3 right = new Vec3(
                -forward.z,
                0.0,
                forward.x
        );
        Vec3 combined = forward.scale(
                intent.forward()
        ).add(
                right.scale(intent.strafe())
        );
        if (combined.lengthSqr() <= 1.0e-9) {
            return forward.scale(-1.0);
        }
        return combined.normalize();
    }

    private static boolean validIntent(
            float forward,
            float strafe
    ) {
        return Float.isFinite(forward)
                && Float.isFinite(strafe)
                && Math.abs(forward)
                        <= MAX_PACKET_COMPONENT
                && Math.abs(strafe)
                        <= MAX_PACKET_COMPONENT;
    }

    private static boolean sameBufferedAction(
            PlayerActionRuntimeState.Snapshot action,
            BufferedRequest request
    ) {
        return action.active()
                && action.kind()
                        == PlayerActionRuntimeState.WindowKind.ACTION
                && action.actionId()
                        .equals(request.actionId())
                && action.dodgeCancelAtTick()
                        == request.executeAtTick();
    }

    public enum RequestStatus {
        ACCEPTED,
        BUFFERED,
        REJECTED,
        LOCKED,
        DUPLICATE,
        INVALID
    }

    public record RequestResult(
            RequestStatus status,
            int directionCode
    ) {
        public static RequestResult rejected(
                RequestStatus status
        ) {
            if (status == RequestStatus.ACCEPTED
                    || status == RequestStatus.BUFFERED) {
                throw new IllegalArgumentException(
                        "Accepted/buffered dodge requires direction."
                );
            }
            return new RequestResult(status, -1);
        }

        public boolean acceptedNow() {
            return status == RequestStatus.ACCEPTED;
        }

        public boolean buffered() {
            return status == RequestStatus.BUFFERED;
        }
    }

    public record AcceptedEvent(
            UUID playerId,
            int entityId,
            int directionCode
    ) {
    }

    private record BufferedRequest(
            ServerLevel level,
            DirectionIntent intent,
            long sequence,
            String actionId,
            long executeAtTick
    ) {
    }

    private static final class ActiveDodge {
        private final ServerLevel level;
        private final Vec3 direction;
        private final double blocksPerStep;
        private int completedSteps;
        private long lastStepTick;

        private ActiveDodge(
                ServerLevel level,
                Vec3 direction,
                double blocksPerStep,
                int completedSteps,
                long lastStepTick
        ) {
            this.level = level;
            this.direction = direction;
            this.blocksPerStep = blocksPerStep;
            this.completedSteps = completedSteps;
            this.lastStepTick = lastStepTick;
        }

        private ServerLevel level() {
            return level;
        }

        private Vec3 direction() {
            return direction;
        }

        private double blocksPerStep() {
            return blocksPerStep;
        }

        private long lastStepTick() {
            return lastStepTick;
        }

        private void advance(long nowTick) {
            completedSteps++;
            lastStepTick = nowTick;
        }

        private boolean completed() {
            return completedSteps >= MOVEMENT_STEPS;
        }
    }

    public record DirectionIntent(
            double forward,
            double strafe,
            boolean noDirectionalInput,
            int directionCode
    ) {
        public static DirectionIntent resolve(
                float rawForward,
                float rawStrafe
        ) {
            double magnitude = Math.hypot(
                    rawForward,
                    rawStrafe
            );
            if (magnitude <= INPUT_EPSILON) {
                return new DirectionIntent(
                        0.0,
                        0.0,
                        true,
                        1
                );
            }

            double forward = rawForward / magnitude;
            double strafe = rawStrafe / magnitude;
            int directionCode;
            if (Math.abs(forward)
                    >= Math.abs(strafe)) {
                directionCode = forward >= 0.0
                        ? 0
                        : 1;
            } else {
                directionCode = strafe >= 0.0
                        ? 3
                        : 2;
            }
            return new DirectionIntent(
                    forward,
                    strafe,
                    false,
                    directionCode
            );
        }
    }
}
