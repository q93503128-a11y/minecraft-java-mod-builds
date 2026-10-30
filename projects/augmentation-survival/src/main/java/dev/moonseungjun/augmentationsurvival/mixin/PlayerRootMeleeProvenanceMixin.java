package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.WallSlamMeleeRuntime;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.movement.MovementAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Pins actual vanilla/Better Combat Player.attack damage to ROOT_MELEE and dispatches post-hit melee augments there. */
@Mixin(Player.class)
abstract class PlayerRootMeleeProvenanceMixin {
    @WrapOperation(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean augmentationSurvival$markRootMeleeDamage(
            Entity target,
            DamageSource source,
            float proposedDamage,
            Operation<Boolean> original
    ) {
        Player attacker = (Player) (Object) this;
        if (!(attacker instanceof ServerPlayer serverPlayer)) {
            return original.call(target, source, proposedDamage);
        }

        float healthBefore = target instanceof LivingEntity living ? living.getHealth() : Float.NaN;
        boolean applied = CombatProvenanceContext.callAs(
                CombatProvenance.ROOT_MELEE,
                () -> original.call(target, source, proposedDamage)
        );
        if (applied && target instanceof LivingEntity living) {
            MovementAugmentService.onSuccessfulRootMelee(serverPlayer, living);
            float actualHealthDamage = Float.isFinite(healthBefore)
                    ? Math.max(0.0F, healthBefore - living.getHealth())
                    : 0.0F;
            float referenceDamage = actualHealthDamage > 0.0F ? actualHealthDamage : proposedDamage;
            WallSlamMeleeRuntime.arm(serverPlayer, living, referenceDamage);
        }
        return applied;
    }
}
