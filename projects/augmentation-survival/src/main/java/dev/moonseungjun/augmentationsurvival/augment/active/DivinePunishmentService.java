package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.mixin.SpecialActiveAugmentServiceInvoker;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Dedicated server-authoritative execution path for the mythic percent-health strike. */
public final class DivinePunishmentService {
    public static final String ID = AugmentationSurvival.MOD_ID + ":divine_punishment";
    private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "divine_punishment")
    );

    private DivinePunishmentService() {
    }

    public static boolean tryUse(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int augmentLevel = boundedLevel(state);
        long now = player.level().getGameTime();
        if (augmentLevel <= 0 || now < state.cooldownReadyTick(ID)) return false;

        SpecialActiveTuning.Snapshot tuning = SpecialActiveTuning.current();
        ServerLevel level = player.level();
        double radius = tuning.divinePunishmentRadius();
        double radiusSquared = radius * radius;
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radiusSquared
        );
        if (targets.isEmpty()) return false;

        DamageSource source = player.damageSources().source(DAMAGE, null, player);
        boolean affected = false;
        for (LivingEntity target : targets) {
            boolean targetAffected = false;
            boolean boss = EntityCompatibilityProfileService.isBoss(target);
            if (EntityCompatibilityProfileService.allowPercentHpDamage(target)) {
                float damage = (float) Math.min(
                        Float.MAX_VALUE,
                        target.getMaxHealth() * tuning.divinePunishmentFraction(augmentLevel, boss)
                );
                if (damage > 0.0F && Float.isFinite(damage)) {
                    boolean damaged = CombatProvenanceContext.callAs(
                            CombatProvenance.SPECIAL_PERCENT_HP,
                            () -> target.hurtServer(level, source, damage)
                    );
                    targetAffected |= damaged;
                }
            }
            if (augmentLevel >= 2 && EntityCompatibilityProfileService.allowStun(target)) {
                SpecialActiveAugmentServiceInvoker.augmentationSurvival$applyStun(
                        target,
                        tuning.divinePunishmentStunTicksLevel2()
                );
                targetAffected = true;
            }
            if (targetAffected) {
                emitStrike(level, target);
                affected = true;
            }
        }
        if (!affected) return false;

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerStateService.state(player).withCooldownReadyTick(
                        ID,
                        now + tuning.divinePunishmentCooldownTicks()
                )
        );
        return true;
    }

    private static void emitStrike(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double bottom = target.getY();
        double top = bottom + Math.max(3.0D, target.getBbHeight() + 4.0D);
        int segments = 14;
        for (int i = 0; i <= segments; i++) {
            double y = bottom + (top - bottom) * i / segments;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, target.getZ(),
                    2, 0.08D, 0.08D, 0.08D, 0.01D);
        }
        level.sendParticles(ParticleTypes.END_ROD,
                x, bottom + target.getBbHeight() * 0.55D, target.getZ(),
                4, 0.18D, 0.28D, 0.18D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.PLAYERS, 0.75F, 1.15F);
    }

    private static int boundedLevel(AugmentationPlayerState state) {
        int level = state.level(ID);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(ID).maxLevel());
    }
}
