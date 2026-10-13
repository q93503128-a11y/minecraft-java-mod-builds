package dev.moonseungjun.openworldrpg.mount;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.network.TrailStagMountRequestPayload;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server authority for Trail Stag mount-button requests and the one-second summon channel.
 *
 * <p>The live entity/presentation binder is intentionally registered separately. Until the exact
 * accepted Stag asset and rideable runtime exist, requests fail closed rather than exposing a
 * vanilla, static, or invisible substitute.</p>
 */
public final class TrailStagMountIntentRuntime {
    private static final long NO_HOSTILE_ACTIVITY_CUTOFF = Long.MIN_VALUE / 8L;
    private static final ConcurrentHashMap<UUID, PlayerRuntimeState> STATES =
            new ConcurrentHashMap<>();

    private static volatile LiveBinding liveBinding;

    private TrailStagMountIntentRuntime() {
    }

    public static synchronized void registerLiveBinding(LiveBinding binding) {
        Objects.requireNonNull(binding, "binding");
        if (liveBinding != null && liveBinding != binding) {
            throw new IllegalStateException("Trail Stag live binding is already registered.");
        }
        liveBinding = binding;
    }

    public static RequestResult request(
            ServerPlayer player,
            long sequence,
            int protocolVersion
    ) {
        Objects.requireNonNull(player, "player");
        if (protocolVersion != TrailStagMountRequestPayload.CURRENT_PROTOCOL_VERSION) {
            return RequestResult.INVALID_PROTOCOL;
        }
        if (sequence < 0L) {
            return RequestResult.INVALID_SEQUENCE;
        }

        PlayerRuntimeState runtime = STATES.computeIfAbsent(
                player.getUUID(),
                ignored -> new PlayerRuntimeState()
        );
        synchronized (runtime) {
            if (sequence <= runtime.lastSequence) {
                return RequestResult.REPLAYED_SEQUENCE;
            }
            runtime.lastSequence = sequence;
            if (runtime.channel != null) {
                return RequestResult.ALREADY_CHANNELING;
            }

            long nowTick = player.level().getGameTime();
            long hostileActivityTick = hostileHpActivityTick(player, nowTick);
            LiveBinding binding = liveBinding;
            boolean presentationReady = binding != null
                    && binding.presentationRuntimeReady();
            TrailStagMountService.SummonEligibility eligibility =
                    TrailStagMountService.evaluatePreChannel(
                            player,
                            nowTick,
                            ticksSinceHostileActivity(nowTick, hostileActivityTick),
                            presentationReady
                    );
            if (eligibility != TrailStagMountService.SummonEligibility.ALLOWED) {
                return fromEligibility(eligibility);
            }

            runtime.channel = new TrailStagSummonChannelState(
                    sequence,
                    nowTick,
                    hostileActivityTick
            );
            return RequestResult.CHANNEL_STARTED;
        }
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        for (var entry : STATES.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PlayerRuntimeState runtime = entry.getValue();
            synchronized (runtime) {
                TrailStagSummonChannelState channel = runtime.channel;
                if (channel == null) {
                    continue;
                }
                if (player == null || !player.isAlive() || player.isSpectator()) {
                    runtime.channel = null;
                    continue;
                }

                long nowTick = player.level().getGameTime();
                long hostileActivityTick = hostileHpActivityTick(player, nowTick);
                if (channel.canceledByHostileActivity(hostileActivityTick)) {
                    runtime.channel = null;
                    continue;
                }
                if (!channel.complete(nowTick)) {
                    continue;
                }

                LiveBinding binding = liveBinding;
                boolean presentationReady = binding != null
                        && binding.presentationRuntimeReady();
                TrailStagMountService.SummonEligibility preChannel =
                        TrailStagMountService.evaluatePreChannel(
                                player,
                                nowTick,
                                ticksSinceHostileActivity(nowTick, hostileActivityTick),
                                presentationReady
                        );
                if (preChannel != TrailStagMountService.SummonEligibility.ALLOWED) {
                    runtime.channel = null;
                    continue;
                }

                Objects.requireNonNull(
                        binding.completeSummon(player),
                        "Trail Stag live binding completion result"
                );
                runtime.channel = null;
            }
        }
    }

    public static void disconnect(UUID playerId) {
        STATES.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    private static long hostileHpActivityTick(
            ServerPlayer player,
            long nowTick
    ) {
        return CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick)
                .persistentSnapshot(nowTick)
                .lastHostileHpActivityTick();
    }

    static long ticksSinceHostileActivity(
            long nowTick,
            long lastHostileActivityTick
    ) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException("Game tick must be non-negative.");
        }
        if (lastHostileActivityTick <= NO_HOSTILE_ACTIVITY_CUTOFF) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, nowTick - lastHostileActivityTick);
    }

    private static RequestResult fromEligibility(
            TrailStagMountService.SummonEligibility eligibility
    ) {
        return switch (eligibility) {
            case LOCKED -> RequestResult.LOCKED;
            case PRESENTATION_NOT_READY -> RequestResult.PRESENTATION_NOT_READY;
            case INVALID_PLAYER -> RequestResult.INVALID_PLAYER;
            case FORCED_COOLDOWN -> RequestResult.FORCED_COOLDOWN;
            case COMBAT_LOCK -> RequestResult.COMBAT_LOCK;
            case ALLOWED, FORBIDDEN_VOLUME, NEED_OPEN_GROUND ->
                    throw new IllegalStateException(
                            "Completion-only Trail Stag eligibility reached the pre-channel gate: "
                                    + eligibility
                    );
        };
    }

    public enum RequestResult {
        CHANNEL_STARTED,
        INVALID_PROTOCOL,
        INVALID_SEQUENCE,
        REPLAYED_SEQUENCE,
        ALREADY_CHANNELING,
        LOCKED,
        PRESENTATION_NOT_READY,
        INVALID_PLAYER,
        FORCED_COOLDOWN,
        COMBAT_LOCK
    }

    /**
     * Production entity binder. Completion must run the real authored-volume and placement probes;
     * ALLOWED may be returned only after the project-owned Trail Stag was actually created safely.
     */
    public interface LiveBinding {
        boolean presentationRuntimeReady();

        TrailStagMountService.SummonEligibility completeSummon(ServerPlayer player);
    }

    private static final class PlayerRuntimeState {
        private long lastSequence = -1L;
        private TrailStagSummonChannelState channel;
    }
}
