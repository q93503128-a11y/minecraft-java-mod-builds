package dev.moonseungjun.augmentationsurvival.augment.presentation;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Restrained feedback for passive augments whose effect is otherwise easy to miss.
 * Gameplay remains server-authoritative; this class only reports already-confirmed procs.
 */
public final class PassivePresentationService {
    private static final int FIRST_BANNER_DELAY_TICKS = 2;
    private static final int BANNER_SPACING_TICKS = 13;
    private static final int SAME_PROC_DEBOUNCE_TICKS = 10;
    private static final int MAX_QUEUED_BANNERS = 6;
    private static final long STALE_TICKS = 20L * 30L;

    private static final Map<UUID, PlayerFeedback> FEEDBACK = new HashMap<>();
    private static boolean initialized;

    private PassivePresentationService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(PassivePresentationService::tick);
    }

    /** Queue a confirmed passive proc and emit a small immediate world cue. */
    public static void proc(ServerPlayer player, String augmentId) {
        if (player == null || player.isDeadOrDying() || !owned(player, augmentId)) return;
        long now = player.level().getGameTime();
        PlayerFeedback feedback = FEEDBACK.computeIfAbsent(player.getUUID(), ignored -> new PlayerFeedback());
        long last = feedback.lastProcTicks.getOrDefault(augmentId, Long.MIN_VALUE / 4L);
        if (now - last < SAME_PROC_DEBOUNCE_TICKS) return;
        feedback.lastProcTicks.put(augmentId, now);

        if (!feedback.queued.contains(augmentId) && feedback.queued.size() < MAX_QUEUED_BANNERS) {
            feedback.queued.addLast(augmentId);
            if (feedback.nextBannerTick <= now) {
                feedback.nextBannerTick = now + FIRST_BANNER_DELAY_TICKS;
            }
        }
        emitWorldCue(player, augmentId);
    }

    /** Emits a proc only on the false -> true edge of a server-owned passive state. */
    public static void observeState(ServerPlayer player, String augmentId, boolean active) {
        PlayerFeedback feedback = FEEDBACK.computeIfAbsent(player.getUUID(), ignored -> new PlayerFeedback());
        boolean previous = feedback.observedStates.getOrDefault(augmentId, false);
        feedback.observedStates.put(augmentId, active);
        if (active && !previous) proc(player, augmentId);
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PlayerFeedback>> iterator = FEEDBACK.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PlayerFeedback> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            PlayerFeedback feedback = entry.getValue();
            long now = player.level().getGameTime();
            feedback.lastProcTicks.entrySet().removeIf(proc -> now - proc.getValue() > STALE_TICKS);

            if (!feedback.queued.isEmpty() && now >= feedback.nextBannerTick) {
                String augmentId = feedback.queued.removeFirst();
                if (player.connection != null && ServerPlayNetworking.canSend(player, ActivePresentationPayload.TYPE)) {
                    ServerPlayNetworking.send(player, new ActivePresentationPayload(
                            augmentId,
                            ActivePresentationPayload.INSTANT,
                            0,
                            0.0F
                    ));
                }
                feedback.nextBannerTick = now + BANNER_SPACING_TICKS;
            }

            if (feedback.queued.isEmpty()
                    && feedback.lastProcTicks.isEmpty()
                    && feedback.observedStates.values().stream().noneMatch(Boolean::booleanValue)) {
                iterator.remove();
            }
        }
    }

    private static void emitWorldCue(ServerPlayer player, String augmentId) {
        ParticleOptions particle = particleFor(augmentId);
        player.level().sendParticles(
                particle,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                7,
                0.35D, 0.45D, 0.35D,
                0.025D
        );
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.34F,
                pitchFor(augmentId)
        );
    }

    private static ParticleOptions particleFor(String augmentId) {
        String path = path(augmentId);
        return switch (path) {
            case "last_stand", "emergency_totem", "reincarnation", "invincibility", "shield_burst" ->
                    ParticleTypes.END_ROD;
            case "lifesteal", "life_wave", "healing_share", "overflow_healing" ->
                    ParticleTypes.HAPPY_VILLAGER;
            case "status_reflect", "return_to_sender", "strong_mind", "weakness_mark" ->
                    ParticleTypes.WITCH;
            case "kill_streak", "urgent_steps", "retrace", "chain_explosion", "lightning",
                    "echo_strike", "heavy_strike", "combat_cycle" -> ParticleTypes.CRIT;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static float pitchFor(String augmentId) {
        return switch (path(augmentId)) {
            case "last_stand", "emergency_totem", "reincarnation" -> 0.78F;
            case "lightning", "heavy_strike", "chain_explosion" -> 0.68F;
            case "status_reflect", "return_to_sender", "weakness_mark" -> 1.28F;
            default -> 1.08F;
        };
    }

    private static boolean owned(ServerPlayer player, String augmentId) {
        if (augmentId == null || augmentId.isBlank()) return false;
        try {
            AugmentCatalog.require(augmentId);
        } catch (RuntimeException ignored) {
            return false;
        }
        return AugmentationPlayerStateService.state(player).level(augmentId) > 0;
    }

    private static String path(String augmentId) {
        int split = augmentId == null ? -1 : augmentId.indexOf(':');
        return split >= 0 ? augmentId.substring(split + 1) : augmentId == null ? "" : augmentId;
    }

    public static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class PlayerFeedback {
        private final ArrayDeque<String> queued = new ArrayDeque<>();
        private final Map<String, Long> lastProcTicks = new LinkedHashMap<>();
        private final Map<String, Boolean> observedStates = new HashMap<>();
        private long nextBannerTick;
    }
}
