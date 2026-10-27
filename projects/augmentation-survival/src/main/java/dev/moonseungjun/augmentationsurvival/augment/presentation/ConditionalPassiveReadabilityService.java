package dev.moonseungjun.augmentationsurvival.augment.presentation;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * World-space cues for conditional passives whose authoritative runtime state is otherwise invisible.
 * Gameplay services remain authoritative; this class only mirrors confirmed state edges/procs.
 */
public final class ConditionalPassiveReadabilityService {
    private static final String NOCTURNAL_ID = id("nocturnal");
    private static final String SUN_AFFINITY_ID = id("sun_affinity");

    private static final Map<ServerPlayer, AdaptationCueState> ADAPTATION_CUES = new WeakHashMap<>();
    private static final Map<ServerPlayer, MiningCueState> MINING_CUES = new WeakHashMap<>();
    private static final Map<UUID, DayCycleCueState> DAY_CYCLE_CUES = new HashMap<>();
    private static boolean initialized;

    private ConditionalPassiveReadabilityService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(ConditionalPassiveReadabilityService::tickDayCyclePassives);
        ServerPlayerEvents.JOIN.register(ConditionalPassiveReadabilityService::clearDayCycleCue);
        ServerPlayerEvents.LEAVE.register(ConditionalPassiveReadabilityService::clearDayCycleCue);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            clearDayCycleCue(oldPlayer);
            clearDayCycleCue(newPlayer);
        });
    }

    /** Called only after Damage Adaptation has recorded a real non-delayed hit family. */
    public static void presentDamageAdaptationArmed(ServerPlayer player, DamageSource source) {
        if (player == null || source == null || player.isDeadOrDying()) return;
        long now = player.level().getGameTime();
        String family = damageFamily(source);
        AdaptationCueState cue = ADAPTATION_CUES.computeIfAbsent(player, ignored -> new AdaptationCueState());
        boolean changed = cue.untilTick < now || !Objects.equals(cue.family, family);
        cue.family = family;
        cue.untilTick = now + CombatTuning.current().damageAdaptationDurationTicks();
        if (!changed) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 chest = player.position().add(0.0D, Math.max(0.75D, player.getBbHeight() * 0.55D), 0.0D);
        ring(level, ParticleTypes.ENCHANT, chest, 0.58D, 10);
        level.sendParticles(ParticleTypes.END_ROD, chest.x, chest.y, chest.z, 4, 0.18D, 0.26D, 0.18D, 0.010D);
        sound(level, chest, SoundEvents.AMETHYST_BLOCK_CHIME, 0.24F, 0.76F);
    }

    /** Called from the exact branch where Damage Adaptation scales a same-family hit. */
    public static void presentDamageAdaptationProc(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 chest = player.position().add(0.0D, Math.max(0.75D, player.getBbHeight() * 0.55D), 0.0D);
        ring(level, ParticleTypes.END_ROD, chest, 0.48D, 8);
        level.sendParticles(ParticleTypes.POOF, chest.x, chest.y, chest.z, 4, 0.16D, 0.22D, 0.16D, 0.018D);
        sound(level, chest, SoundEvents.SHIELD_BLOCK.value(), 0.22F, 1.46F);
    }

    /** Mirrors the authoritative Continuous Mining stack transition after a confirmed block break. */
    public static void presentContinuousMiningBreak(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) return;
        long now = player.level().getGameTime();
        SurvivalTuning.Snapshot tuning = SurvivalTuning.current();
        int maxStacks = tuning.continuousMiningMaxStacks();
        MiningCueState cue = MINING_CUES.computeIfAbsent(player, ignored -> new MiningCueState());
        int previous = cue.untilTick >= now ? cue.stacks : 0;
        int next = previous > 0 ? Math.min(maxStacks, previous + 1) : 1;
        cue.stacks = next;
        cue.untilTick = now + tuning.continuousMiningWindowTicks();

        // At max stacks the mechanical timer refreshes, but repeated full-stack spam adds no readability.
        if (next == previous) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 focus = player.getEyePosition().add(player.getLookAngle().scale(0.72D));
        double radius = 0.10D + next * 0.035D;
        ring(level, ParticleTypes.ENCHANT, focus, radius, 4 + next * 2);
        level.sendParticles(
                ParticleTypes.CRIT,
                focus.x, focus.y, focus.z,
                1 + next,
                0.06D + next * 0.012D,
                0.06D + next * 0.012D,
                0.06D + next * 0.012D,
                0.018D
        );

        if (next >= maxStacks) {
            ring(level, ParticleTypes.END_ROD, focus, radius + 0.08D, 10);
            sound(level, focus, SoundEvents.AMETHYST_BLOCK_CHIME, 0.24F, 1.48F);
        } else if (next >= 2) {
            sound(level, focus, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.10F, 0.86F + next * 0.11F);
        }
    }

    private static void tickDayCyclePassives(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isDeadOrDying() || player.isSpectator()) {
                DAY_CYCLE_CUES.remove(player.getUUID());
                continue;
            }

            AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
            boolean nocturnal = state.level(NOCTURNAL_ID) > 0;
            boolean sunAffinity = state.level(SUN_AFFINITY_ID) > 0;
            if (!nocturnal && !sunAffinity) {
                DAY_CYCLE_CUES.remove(player.getUUID());
                continue;
            }

            boolean daytime = isDaytime(player);
            DayCycleCueState cue = DAY_CYCLE_CUES.computeIfAbsent(
                    player.getUUID(),
                    ignored -> new DayCycleCueState(daytime, nocturnal, sunAffinity)
            );
            if (!cue.ready) {
                cue.ready = true;
                cue.daytime = daytime;
                cue.nocturnal = nocturnal;
                cue.sunAffinity = sunAffinity;
                continue;
            }

            boolean phaseChanged = cue.daytime != daytime;
            boolean nocturnalActivatedByOwnership = !cue.nocturnal && nocturnal && !daytime;
            boolean sunActivatedByOwnership = !cue.sunAffinity && sunAffinity && daytime;
            if (phaseChanged || nocturnalActivatedByOwnership || sunActivatedByOwnership) {
                presentDayCycleTransition(player, daytime, nocturnal, sunAffinity);
            }

            cue.daytime = daytime;
            cue.nocturnal = nocturnal;
            cue.sunAffinity = sunAffinity;
        }
    }

    private static void presentDayCycleTransition(
            ServerPlayer player,
            boolean daytime,
            boolean nocturnal,
            boolean sunAffinity
    ) {
        boolean active = daytime ? sunAffinity : nocturnal;
        boolean deactivated = daytime ? nocturnal : sunAffinity;
        if (!active && !deactivated) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 chest = player.position().add(0.0D, Math.max(0.72D, player.getBbHeight() * 0.52D), 0.0D);
        ParticleOptions primary = daytime ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT;
        ring(level, primary, chest, active ? 0.56D : 0.42D, active ? 8 : 5);
        if (active) {
            level.sendParticles(
                    daytime ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD,
                    chest.x, chest.y, chest.z,
                    3,
                    0.15D, 0.20D, 0.15D,
                    0.006D
            );
            sound(
                    level,
                    chest,
                    daytime ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.EXPERIENCE_ORB_PICKUP,
                    0.12F,
                    daytime ? 1.34F : 0.88F
            );
        } else {
            level.sendParticles(ParticleTypes.POOF, chest.x, chest.y, chest.z, 3, 0.14D, 0.16D, 0.14D, 0.010D);
        }
    }

    private static boolean isDaytime(ServerPlayer player) {
        long dayTime = Math.floorMod(player.level().getOverworldClockTime(), 24_000L);
        return dayTime < 13_000L;
    }

    private static void clearDayCycleCue(ServerPlayer player) {
        if (player != null) {
            DAY_CYCLE_CUES.remove(player.getUUID());
        }
    }

    private static String damageFamily(DamageSource source) {
        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(source.getMsgId());
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

    private static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private static final class AdaptationCueState {
        private String family;
        private long untilTick;
    }

    private static final class MiningCueState {
        private int stacks;
        private long untilTick;
    }

    private static final class DayCycleCueState {
        private boolean ready;
        private boolean daytime;
        private boolean nocturnal;
        private boolean sunAffinity;

        private DayCycleCueState(boolean daytime, boolean nocturnal, boolean sunAffinity) {
            this.daytime = daytime;
            this.nocturnal = nocturnal;
            this.sunAffinity = sunAffinity;
        }
    }
}
