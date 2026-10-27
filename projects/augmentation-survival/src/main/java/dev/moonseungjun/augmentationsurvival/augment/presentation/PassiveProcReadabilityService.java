package dev.moonseungjun.augmentationsurvival.augment.presentation;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight world-space feedback for successful passive procs that are mechanically real but
 * otherwise easy to miss. It never changes gameplay state and deliberately avoids persistent HUD.
 */
public final class PassiveProcReadabilityService {
    private static final String KILL_STREAK_ID = id("kill_streak");
    private static final String URGENT_STEPS_ID = id("urgent_steps");
    private static final String EXPERIENCE_PRESERVATION_ID = id("experience_preservation");

    private static final Map<UUID, KillCueState> KILL_CUES = new HashMap<>();
    private static final Map<UUID, Long> URGENT_UNTIL = new HashMap<>();
    private static boolean initialized;

    private PassiveProcReadabilityService() {
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLivingEntityEvents.AFTER_DAMAGE.register(PassiveProcReadabilityService::afterDamage);
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(PassiveProcReadabilityService::afterKill);
        ServerPlayerEvents.JOIN.register(PassiveProcReadabilityService::clearTransient);
        ServerPlayerEvents.LEAVE.register(PassiveProcReadabilityService::clearTransient);
        ServerPlayerEvents.AFTER_RESPAWN.register(PassiveProcReadabilityService::afterRespawn);
    }

    /** Called only when SurvivalAugmentService actually cancels a durability event. */
    public static void presentDurabilitySaved(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty() || player.isDeadOrDying()) return;
        boolean armor = isEquippedArmor(player, stack);
        if (!armor && !stack.has(DataComponents.TOOL)) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = armor
                ? player.position().add(0.0D, Math.max(0.70D, player.getBbHeight() * 0.55D), 0.0D)
                : player.getEyePosition().add(player.getLookAngle().scale(0.48D));
        ring(level, ParticleTypes.ENCHANT, at, armor ? 0.46D : 0.22D, armor ? 7 : 5);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 2, 0.08D, 0.08D, 0.08D, 0.006D);
        sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.10F, armor ? 1.18F : 1.42F);
    }

    /** Called after a confirmed crop/ore bonus has spawned one or more extra item entities. */
    public static void presentResourceBonus(
            ServerPlayer player,
            BlockPos pos,
            boolean ore,
            int spawnedEntities
    ) {
        if (player == null || pos == null || spawnedEntities <= 0 || player.isDeadOrDying()) return;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 at = Vec3.atCenterOf(pos).add(0.0D, 0.16D, 0.0D);
        int strength = Math.min(2, spawnedEntities);
        level.sendParticles(
                ParticleTypes.ENCHANT,
                at.x, at.y, at.z,
                2 + strength,
                0.18D, 0.12D, 0.18D,
                0.018D
        );
        if (ore) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0.08D, 0.08D, 0.08D, 0.004D);
        }
        sound(
                level,
                at,
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                0.08F + strength * 0.02F,
                ore ? 1.52F : 1.32F + strength * 0.06F
        );
    }

    private static void afterDamage(
            LivingEntity entity,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        if (!(entity instanceof ServerPlayer player)
                || blocked
                || !(damageTaken > 0.0F)
                || CombatProvenanceContext.isDelayedDamage()
                || !owns(player, URGENT_STEPS_ID)) {
            return;
        }

        long now = player.level().getGameTime();
        long previousUntil = URGENT_UNTIL.getOrDefault(player.getUUID(), 0L);
        long nextUntil = Math.max(
                previousUntil,
                now + CombatTuning.current().urgentStepsDurationTicks()
        );
        URGENT_UNTIL.put(player.getUUID(), nextUntil);
        if (previousUntil > now) return;

        ServerLevel level = (ServerLevel) player.level();
        Vec3 feet = player.position().add(0.0D, 0.08D, 0.0D);
        ring(level, ParticleTypes.CLOUD, feet, 0.58D, 8);
        level.sendParticles(ParticleTypes.POOF, feet.x, feet.y, feet.z, 4, 0.22D, 0.04D, 0.22D, 0.025D);
        sound(level, feet, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.14F, 1.64F);
    }

    private static void afterKill(
            ServerLevel level,
            Entity entity,
            LivingEntity killedEntity,
            DamageSource damageSource
    ) {
        if (!(entity instanceof ServerPlayer player)
                || !CompanionAugmentService.isHostileEntity(killedEntity)
                || !owns(player, KILL_STREAK_ID)) {
            return;
        }

        CombatTuning.Snapshot tuning = CombatTuning.current();
        long now = player.level().getGameTime();
        KillCueState cue = KILL_CUES.computeIfAbsent(player.getUUID(), ignored -> new KillCueState());
        int previous = cue.untilTick >= now ? cue.stacks : 0;
        int next = Math.min(tuning.killStreakMaxStacks(), previous + 1);
        cue.stacks = next;
        cue.untilTick = now + tuning.killStreakDurationTicks();
        if (next == previous) return;

        Vec3 chest = player.position().add(0.0D, Math.max(0.72D, player.getBbHeight() * 0.50D), 0.0D);
        ring(level, ParticleTypes.CRIT, chest, 0.42D + next * 0.08D, 6 + next * 2);
        if (next >= tuning.killStreakMaxStacks()) {
            ring(level, ParticleTypes.END_ROD, chest, 0.72D, 10);
        }
        sound(
                level,
                chest,
                next >= tuning.killStreakMaxStacks()
                        ? SoundEvents.AMETHYST_BLOCK_CHIME
                        : SoundEvents.EXPERIENCE_ORB_PICKUP,
                next >= tuning.killStreakMaxStacks() ? 0.24F : 0.12F,
                0.92F + next * 0.18F
        );
    }

    private static void afterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        clearTransient(oldPlayer);
        clearTransient(newPlayer);
        if (alive
                || oldPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY)
                || !owns(oldPlayer, EXPERIENCE_PRESERVATION_ID)
                || newPlayer.totalExperience <= 0) {
            return;
        }

        ServerLevel level = (ServerLevel) newPlayer.level();
        Vec3 chest = newPlayer.position().add(0.0D, Math.max(0.72D, newPlayer.getBbHeight() * 0.55D), 0.0D);
        ring(level, ParticleTypes.ENCHANT, chest, 0.62D, 10);
        level.sendParticles(ParticleTypes.END_ROD, chest.x, chest.y, chest.z, 5, 0.20D, 0.28D, 0.20D, 0.010D);
        sound(level, chest, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.24F, 1.18F);
    }

    private static boolean owns(ServerPlayer player, String augmentId) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        return state.level(augmentId) > 0;
    }

    private static boolean isEquippedArmor(ServerPlayer player, ItemStack stack) {
        return player.getItemBySlot(EquipmentSlot.HEAD) == stack
                || player.getItemBySlot(EquipmentSlot.CHEST) == stack
                || player.getItemBySlot(EquipmentSlot.LEGS) == stack
                || player.getItemBySlot(EquipmentSlot.FEET) == stack;
    }

    private static void clearTransient(ServerPlayer player) {
        if (player == null) return;
        UUID playerId = player.getUUID();
        KILL_CUES.remove(playerId);
        URGENT_UNTIL.remove(playerId);
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

    private static final class KillCueState {
        private int stacks;
        private long untilTick;
    }
}
