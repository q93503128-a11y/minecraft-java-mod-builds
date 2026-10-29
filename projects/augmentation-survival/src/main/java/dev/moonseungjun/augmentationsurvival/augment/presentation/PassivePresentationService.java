package dev.moonseungjun.augmentationsurvival.augment.presentation;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Event-shaped presentation for passive augments whose confirmed proc would otherwise be hard to read.
 * This deliberately does not reuse the active-ability HUD/banner channel: the event itself must explain
 * what happened through world-space motion, impact and sound.
 */
public final class PassivePresentationService {
    private static final int SAME_PROC_DEBOUNCE_TICKS = 7;
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

    public static void proc(ServerPlayer player, String augmentId) {
        proc(player, augmentId, null);
    }

    /** Presents a confirmed proc around the player and, when supplied, its actual target/source. */
    public static void proc(ServerPlayer player, String augmentId, Entity focus) {
        if (!acceptProc(player, augmentId)) return;
        String path = path(augmentId);
        if ("emergency_totem".equals(path)) {
            // The gameplay path already consumes a real Totem of Undying and broadcasts vanilla event 35.
            // Duplicating it here would cheapen the canonical totem resurrection animation.
            return;
        }
        emitProc(player, path, focus);
    }

    /** Specialized spatial presentation for a proc whose meaning is movement between two real positions. */
    public static void procPath(ServerPlayer player, String augmentId, Vec3 from, Vec3 to) {
        if (!acceptProc(player, augmentId) || from == null || to == null) return;
        ServerLevel level = (ServerLevel) player.level();
        line(level, ParticleTypes.PORTAL, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D), 16);
        level.sendParticles(ParticleTypes.POOF, from.x, from.y + 1.0D, from.z, 8, 0.30D, 0.45D, 0.30D, 0.025D);
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 1.0D, to.z, 7, 0.25D, 0.45D, 0.25D, 0.020D);
        sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.55F, 1.18F);
    }

    /** Emits state-entry and state-exit cues only on authoritative edges. */
    public static void observeState(ServerPlayer player, String augmentId, boolean active) {
        if (player == null || augmentId == null || augmentId.isBlank()) return;
        PlayerFeedback feedback = FEEDBACK.get(player.getUUID());
        if (feedback == null && !active) return;
        if (feedback == null) {
            feedback = new PlayerFeedback();
            FEEDBACK.put(player.getUUID(), feedback);
        }
        boolean previous = feedback.observedStates.getOrDefault(augmentId, false);
        feedback.observedStates.put(augmentId, active);
        if (active && !previous) {
            proc(player, augmentId);
        } else if (!active && previous && owned(player, augmentId)) {
            emitEnd(player, path(augmentId));
        }
    }

    private static boolean acceptProc(ServerPlayer player, String augmentId) {
        if (player == null || player.isDeadOrDying() || !owned(player, augmentId)) return false;
        long now = player.level().getGameTime();
        PlayerFeedback feedback = FEEDBACK.computeIfAbsent(player.getUUID(), ignored -> new PlayerFeedback());
        long last = feedback.lastProcTicks.getOrDefault(augmentId, Long.MIN_VALUE / 4L);
        if (now - last < SAME_PROC_DEBOUNCE_TICKS) return false;
        feedback.lastProcTicks.put(augmentId, now);
        return true;
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
            if (feedback.lastProcTicks.isEmpty()
                    && feedback.observedStates.values().stream().noneMatch(Boolean::booleanValue)) {
                iterator.remove();
            }
        }
    }

    private static void emitProc(ServerPlayer player, String path, Entity focus) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 self = player.position();
        Vec3 chest = self.add(0.0D, 1.0D, 0.0D);
        Vec3 target = focusPoint(focus, chest.add(player.getLookAngle().scale(1.5D)));

        switch (path) {
            case "last_stand" -> {
                level.sendParticles(ParticleTypes.POOF, chest.x, chest.y, chest.z, 8, 0.16D, 0.22D, 0.16D, 0.045D);
                ring(level, ParticleTypes.END_ROD, self.add(0.0D, 0.85D, 0.0D), 1.10D, 18);
                level.sendParticles(ParticleTypes.CRIT, chest.x, chest.y, chest.z, 10, 0.35D, 0.55D, 0.35D, 0.08D);
                sound(level, self, SoundEvents.SHIELD_BLOCK.value(), 0.70F, 0.58F);
            }
            case "reincarnation" -> {
                ring(level, ParticleTypes.SOUL, self.add(0.0D, 0.20D, 0.0D), 1.45D, 20);
                column(level, ParticleTypes.PORTAL, self.add(0.0D, 0.20D, 0.0D), 2.20D, 8);
                sound(level, self, SoundEvents.ENDERMAN_TELEPORT, 0.62F, 0.62F);
            }
            case "urgent_steps" -> {
                level.sendParticles(ParticleTypes.CLOUD, self.x, self.y + 0.10D, self.z, 12, 0.45D, 0.06D, 0.45D, 0.055D);
                line(level, ParticleTypes.POOF, self.subtract(player.getLookAngle().scale(0.9D)), self, 5);
                sound(level, self, SoundEvents.PLAYER_ATTACK_SWEEP, 0.28F, 1.55F);
            }
            case "kill_streak" -> {
                ring(level, ParticleTypes.CRIT, self.add(0.0D, 1.0D, 0.0D), 0.82D, 12);
                column(level, ParticleTypes.END_ROD, self.add(0.0D, 0.55D, 0.0D), 1.45D, 5);
                sound(level, self, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.42F, 1.36F);
            }
            case "lifesteal" -> {
                line(level, ParticleTypes.DAMAGE_INDICATOR, target, chest, 12);
                level.sendParticles(ParticleTypes.HEART, chest.x, chest.y, chest.z, 4, 0.28D, 0.30D, 0.28D, 0.015D);
                sound(level, self, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.38F, 0.76F);
            }
            case "chain_explosion" -> {
                level.sendParticles(ParticleTypes.EXPLOSION, target.x, target.y, target.z, 2, 0.35D, 0.35D, 0.35D, 0.0D);
                ring(level, ParticleTypes.POOF, target, 1.55D, 20);
                sound(level, target, SoundEvents.GENERIC_EXPLODE.value(), 0.68F, 1.22F);
            }
            case "lightning" -> {
                column(level, ParticleTypes.ELECTRIC_SPARK, target.add(0.0D, -0.8D, 0.0D), 2.8D, 12);
                ring(level, ParticleTypes.ELECTRIC_SPARK, target, 0.72D, 12);
                sound(level, target, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.62F, 1.18F);
            }
            case "echo_strike" -> {
                level.sendParticles(ParticleTypes.SONIC_BOOM, target.x, target.y, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                ring(level, ParticleTypes.END_ROD, target, 0.68D, 10);
                sound(level, target, SoundEvents.PLAYER_ATTACK_CRIT, 0.48F, 0.72F);
            }
            case "weakness_mark" -> {
                ring(level, ParticleTypes.WITCH, target.add(0.0D, 0.35D, 0.0D), 0.76D, 14);
                level.sendParticles(ParticleTypes.WITCH, target.x, target.y + 0.25D, target.z, 6, 0.22D, 0.30D, 0.22D, 0.025D);
                sound(level, target, SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 0.72F);
            }
            case "heavy_strike" -> {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.x, target.y, target.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.CRIT, target.x, target.y, target.z, 13, 0.40D, 0.35D, 0.40D, 0.11D);
                sound(level, target, SoundEvents.PLAYER_ATTACK_SWEEP, 0.65F, 0.56F);
            }
            case "life_wave" -> {
                double radius = ExpansionTuning.current().lifeWaveRadius();
                ring(level, ParticleTypes.HEART, self.add(0.0D, 0.55D, 0.0D), radius * 0.45D, 20);
                ring(level, ParticleTypes.END_ROD, self.add(0.0D, 0.55D, 0.0D), radius, 32);
                sound(level, self, SoundEvents.BEACON_ACTIVATE, 0.38F, 1.42F);
            }
            case "combat_cycle" -> {
                ring(level, ParticleTypes.ENCHANT, self.add(0.0D, 1.05D, 0.0D), 1.02D, 16);
                for (int i = 0; i < 4; i++) {
                    double angle = i * (Math.PI / 2.0D);
                    level.sendParticles(ParticleTypes.END_ROD,
                            self.x + Math.cos(angle) * 1.02D,
                            self.y + 1.05D,
                            self.z + Math.sin(angle) * 1.02D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                sound(level, self, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.32F, 1.72F);
            }
            case "return_to_sender" -> {
                line(level, ParticleTypes.END_ROD, chest, target, 10);
                level.sendParticles(ParticleTypes.CRIT, target.x, target.y, target.z, 9, 0.20D, 0.20D, 0.20D, 0.10D);
                sound(level, target, SoundEvents.SHIELD_BLOCK.value(), 0.50F, 1.34F);
            }
            case "status_reflect" -> {
                line(level, ParticleTypes.WITCH, chest, target, 10);
                ring(level, ParticleTypes.WITCH, target, 0.58D, 12);
                sound(level, target, SoundEvents.AMETHYST_BLOCK_CHIME, 0.40F, 1.28F);
            }
            case "strong_mind" -> {
                ring(level, ParticleTypes.ENCHANT, self.add(0.0D, 1.70D, 0.0D), 0.72D, 16);
                level.sendParticles(ParticleTypes.POOF, self.x, self.y + 1.70D, self.z, 5, 0.20D, 0.15D, 0.20D, 0.015D);
                sound(level, self, SoundEvents.AMETHYST_BLOCK_CHIME, 0.32F, 1.62F);
            }
            case "shield_burst" -> {
                double radius = ReactiveCombatTuning.current().shieldBurstRadius();
                ring(level, ParticleTypes.END_ROD, self.add(0.0D, 0.90D, 0.0D), radius * 0.55D, 22);
                ring(level, ParticleTypes.POOF, self.add(0.0D, 0.90D, 0.0D), radius, 36);
                sound(level, self, SoundEvents.SHIELD_BREAK.value(), 0.60F, 1.00F);
            }
            case "retrace" -> {
                level.sendParticles(ParticleTypes.PORTAL, chest.x, chest.y, chest.z, 16, 0.45D, 0.65D, 0.45D, 0.10D);
                sound(level, self, SoundEvents.ENDERMAN_TELEPORT, 0.50F, 1.26F);
            }
            case "invincibility" -> {
                level.sendParticles(ParticleTypes.ENCHANT, chest.x, chest.y, chest.z, 12, 0.20D, 0.35D, 0.20D, 0.035D);
                ring(level, ParticleTypes.END_ROD, self.add(0.0D, 1.0D, 0.0D), 0.95D, 18);
                sound(level, self, SoundEvents.BEACON_ACTIVATE, 0.44F, 1.08F);
            }
            default -> AugmentationSurvival.LOGGER.debug("No bespoke passive presentation for {}", path);
        }
    }

    private static void emitEnd(ServerPlayer player, String path) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 self = player.position();
        switch (path) {
            case "reincarnation" -> {
                column(level, ParticleTypes.END_ROD, self.add(0.0D, 0.15D, 0.0D), 2.25D, 9);
                ring(level, ParticleTypes.HEART, self.add(0.0D, 0.65D, 0.0D), 1.05D, 12);
                sound(level, self, SoundEvents.BEACON_ACTIVATE, 0.55F, 1.02F);
            }
            case "invincibility" -> {
                ring(level, ParticleTypes.POOF, self.add(0.0D, 1.0D, 0.0D), 0.92D, 14);
                sound(level, self, SoundEvents.BEACON_DEACTIVATE, 0.30F, 1.28F);
            }
            default -> { }
        }
    }

    private static Vec3 focusPoint(Entity entity, Vec3 fallback) {
        if (entity == null) return fallback;
        return entity.position().add(0.0D, Math.max(0.35D, entity.getBbHeight() * 0.55D), 0.0D);
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            level.sendParticles(
                    particle,
                    center.x + Math.cos(angle) * radius,
                    center.y,
                    center.z + Math.sin(angle) * radius,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }
    }

    private static void column(ServerLevel level, ParticleOptions particle, Vec3 base, double height, int points) {
        int count = Math.max(2, points);
        for (int i = 0; i < count; i++) {
            double y = base.y + height * i / (count - 1.0D);
            level.sendParticles(particle, base.x, y, base.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int points) {
        int count = Math.max(2, points);
        for (int i = 0; i < count; i++) {
            double t = i / (count - 1.0D);
            level.sendParticles(
                    particle,
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t,
                    1,
                    0.0D, 0.0D, 0.0D,
                    0.0D
            );
        }
    }

    private static void sound(ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, volume, pitch);
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
        private final Map<String, Long> lastProcTicks = new LinkedHashMap<>();
        private final Map<String, Boolean> observedStates = new HashMap<>();
    }
}
