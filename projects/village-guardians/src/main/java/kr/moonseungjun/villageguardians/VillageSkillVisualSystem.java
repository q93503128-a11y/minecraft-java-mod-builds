package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Non-particle combat feedback. Skills are represented by real target motion,
 * aggro changes and role-specific spatial sounds while gameplay is handled by
 * VillageRoleSkillSystem.
 */
public final class VillageSkillVisualSystem {
    private VillageSkillVisualSystem() {}

    public static void render(ServerPlayer player, VillageRoleSkillSystem.ActiveSkill skill) {
        if (player == null || skill == null || !(player.level() instanceof ServerLevel level)) return;
        switch (skill.role()) {
            case VANGUARD -> {
                play(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.15f, 0.78f);
                pushFromPlayer(level, player, skill.name().endsWith("STORM") ? 8.5 : 5.5,
                        skill.name().endsWith("STORM") ? 14 : 8, 0.55, 0.08);
            }
            case RANGER -> {
                play(level, player, SoundEvents.ARROW_SHOOT, 1.0f, 1.12f);
                markTargets(level, player, skill.name().endsWith("FIRE_RAIN") ? 14.0 : 12.0,
                        skill.name().endsWith("FIRE_RAIN") ? 14 : 7);
            }
            case ARCANIST -> {
                play(level, player, SoundEvents.BLAZE_SHOOT, 1.0f, 0.72f);
                liftTargets(level, player, skill.name().endsWith("NOVA") ? 9.0 : 7.0,
                        skill.name().endsWith("NOVA") ? 16 : 10, 0.18);
            }
            case LUMINAR -> play(level, player, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.18f);
            case WARDEN -> {
                play(level, player, SoundEvents.SHIELD_BLOCK.value(), 1.15f, 0.78f);
                tauntTargets(level, player, skill.name().endsWith("FIELD") ? 14.0 : 9.0,
                        skill.name().endsWith("FIELD") ? 18 : 14);
            }
        }
    }

    private static void play(ServerLevel level, ServerPlayer player,
                             net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static List<Mob> targets(ServerLevel level, ServerPlayer player, double radius, int limit) {
        return VillageRaidSystem.activeEnemiesNear(level, player.position(), radius, limit, null);
    }

    private static void pushFromPlayer(ServerLevel level, ServerPlayer player, double radius, int limit,
                                       double horizontal, double vertical) {
        for (Mob target : targets(level, player, radius, limit)) {
            Vec3 delta = target.position().subtract(player.position());
            Vec3 direction = new Vec3(delta.x, 0.0, delta.z);
            if (direction.lengthSqr() < 0.001) direction = new Vec3(0.0, 0.0, 1.0);
            direction = direction.normalize().scale(horizontal);
            target.push(direction.x, vertical, direction.z);
            target.hurtMarked = true;
        }
    }

    private static void markTargets(ServerLevel level, ServerPlayer player, double radius, int limit) {
        for (Mob target : targets(level, player, radius, limit)) {
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 0.35, Math.max(0.05, motion.y), motion.z * 0.35);
            target.hurtMarked = true;
        }
    }

    private static void liftTargets(ServerLevel level, ServerPlayer player, double radius, int limit, double amount) {
        for (Mob target : targets(level, player, radius, limit)) {
            target.push(0.0, amount, 0.0);
            target.hurtMarked = true;
        }
    }

    private static void tauntTargets(ServerLevel level, ServerPlayer player, double radius, int limit) {
        for (Mob target : targets(level, player, radius, limit)) {
            target.setTarget(player);
        }
    }
}
