package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentRuntimeService;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
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

/** Makes Wall Slam a forgiving direct-melee -> wall-contact window instead of an exact knockback-tick race. */
@Mixin(value = FinalAugmentRuntimeService.class, remap = false)
abstract class WallSlamRuntimeOverrideMixin {
    @Shadow @Final private static Map<UUID, Object> WALL_SLAMS;
    @Unique private static final double AUGMENTATION_SURVIVAL$FORGIVING_WALL_REACH = 0.85D;

    @Inject(method = "tickWallSlams", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$tickForgivingWallSlams(MinecraftServer server, CallbackInfo ci) {
        ci.cancel();
        Iterator<Map.Entry<UUID, Object>> iterator = WALL_SLAMS.entrySet().iterator();
        while (iterator.hasNext()) {
            WallSlamWindowAccessor window = (WallSlamWindowAccessor) iterator.next().getValue();
            LivingEntity target = window.augmentationSurvival$getTarget();
            long now = target.level().getGameTime();
            if (target.isRemoved() || target.isDeadOrDying() || now > window.augmentationSurvival$getExpiresAtTick()) {
                iterator.remove();
                continue;
            }
            if (!augmentationSurvival$isAtWall(target)) continue;

            ServerPlayer owner = server.getPlayerList().getPlayer(window.augmentationSurvival$getOwnerId());
            iterator.remove();
            if (owner == null || owner.level() != target.level() || !(target.level() instanceof ServerLevel level)) continue;

            float damage = (float) (window.augmentationSurvival$getReferenceDamage()
                    * FinalAugmentTuning.current().wallSlamDamageFraction(window.augmentationSurvival$getLevel()));
            if (!(damage > 0.0F)) continue;
            DamageSource wallSource = owner.damageSources().playerAttack(owner);

            // Wall Slam is one derived impact belonging to the qualifying melee hit. It must not be
            // swallowed by vanilla's short post-hit invulnerability window, but we also must not erase
            // that window for unrelated follow-up attacks after the wall impact resolves.
            int previousInvulnerableTime = target.invulnerableTime;
            target.invulnerableTime = 0;
            try {
                CombatProvenanceContext.runAs(
                        CombatProvenance.DERIVED_AOE,
                        () -> target.hurtServer(level, wallSource, damage)
                );
            } finally {
                target.invulnerableTime = Math.max(target.invulnerableTime, previousInvulnerableTime);
            }
        }
    }

    @Unique
    private static boolean augmentationSurvival$isAtWall(LivingEntity target) {
        if (target.horizontalCollision) return true;
        AABB box = target.getBoundingBox();
        double reach = AUGMENTATION_SURVIVAL$FORGIVING_WALL_REACH;
        return !target.level().noCollision(target, box.expandTowards(new Vec3(reach, 0.0D, 0.0D)))
                || !target.level().noCollision(target, box.expandTowards(new Vec3(-reach, 0.0D, 0.0D)))
                || !target.level().noCollision(target, box.expandTowards(new Vec3(0.0D, 0.0D, reach)))
                || !target.level().noCollision(target, box.expandTowards(new Vec3(0.0D, 0.0D, -reach)));
    }
}
