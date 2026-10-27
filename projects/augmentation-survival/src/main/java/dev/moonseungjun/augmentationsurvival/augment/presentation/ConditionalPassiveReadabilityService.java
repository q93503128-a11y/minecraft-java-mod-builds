package dev.moonseungjun.augmentationsurvival.augment.presentation;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalTuning;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
    private static final Map<ServerPlayer, AdaptationCueState> ADAPTATION_CUES = new WeakHashMap<>();
    private static final Map<ServerPlayer, MiningCueState> MINING_CUES = new WeakHashMap<>();

    private ConditionalPassiveReadabilityService() {
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

    private static final class AdaptationCueState {
        private String family;
        private long untilTick;
    }

    private static final class MiningCueState {
        private int stacks;
        private long untilTick;
    }
}
