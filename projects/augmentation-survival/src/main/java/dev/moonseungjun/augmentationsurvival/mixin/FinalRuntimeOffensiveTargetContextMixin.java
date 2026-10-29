package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes player-derived final augments to non-allies and resolves obvious wall impacts immediately. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class FinalRuntimeOffensiveTargetContextMixin {
    @Unique private static final String AUGMENTATION_SURVIVAL$WALL_SLAM_ID =
            AugmentationSurvival.MOD_ID + ":wall_slam";
    @Unique private static final double AUGMENTATION_SURVIVAL$IMMEDIATE_WALL_REACH = 0.85D;

    @Shadow @Final private static Map<UUID, ?> WALL_SLAMS;

    @Inject(method = "afterDamage", at = @At("HEAD"))
    private static void augmentationSurvival$pushOffensiveContext(
            LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        OffensiveTargetContext.push();
    }

    @Inject(method = "afterDamage", at = @At("RETURN"))
    private static void augmentationSurvival$resolveImmediateWallAndPop(
            LivingEntity target, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked, CallbackInfo ci
    ) {
        try {
            if (!WALL_SLAMS.containsKey(target.getUUID())
                    || !(source.getEntity() instanceof ServerPlayer attacker)
                    || !(target.level() instanceof ServerLevel level)
                    || !(baseDamageTaken > 0.0F)
                    || !augmentationSurvival$isNearWall(target)) {
                return;
            }

            int ownedLevel = AugmentationPlayerStateService.state(attacker)
                    .level(AUGMENTATION_SURVIVAL$WALL_SLAM_ID);
            if (ownedLevel <= 0) return;
            int augmentLevel = Math.min(
                    ownedLevel,
                    AugmentCatalog.require(AUGMENTATION_SURVIVAL$WALL_SLAM_ID).maxLevel()
            );
            float wallDamage = (float) (baseDamageTaken
                    * FinalAugmentTuning.current().wallSlamDamageFraction(augmentLevel));
            if (!(wallDamage > 0.0F) || !Float.isFinite(wallDamage)) return;

            // Consume the pending window before dealing the derived hit so its own AFTER_DAMAGE event
            // cannot recursively resolve the same wall impact.
            WALL_SLAMS.remove(target.getUUID());
            int previousInvulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            try {
                DamageSource wallSource = attacker.damageSources().playerAttack(attacker);
                CombatProvenanceContext.runAs(
                        CombatProvenance.DERIVED_AOE,
                        () -> target.hurtServer(level, wallSource, wallDamage)
                );
            } finally {
                target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            }
        } finally {
            OffensiveTargetContext.pop();
        }
    }

    @Unique
    private static boolean augmentationSurvival$isNearWall(LivingEntity target) {
        if (target.horizontalCollision) return true;
        AABB box = target.getBoundingBox();
        double reach = AUGMENTATION_SURVIVAL$IMMEDIATE_WALL_REACH;
        return !target.level().noCollision(target, box.expandTowards(new Vec3(reach, 0.0D, 0.0D)))
                || !target.level().noCollision(target, box.expandTowards(new Vec3(-reach, 0.0D, 0.0D)))
                || !target.level().noCollision(target, box.expandTowards(new Vec3(0.0D, 0.0D, reach)))
                || !target.level().noCollision(target, box.expandTowards(new Vec3(0.0D, 0.0D, -reach)));
    }
}
