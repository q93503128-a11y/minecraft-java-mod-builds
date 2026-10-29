package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Pins actual vanilla/Better Combat Player.attack damage to the canonical ROOT_MELEE provenance. */
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
        if (!(attacker instanceof ServerPlayer)) {
            return original.call(target, source, proposedDamage);
        }
        return CombatProvenanceContext.callAs(
                CombatProvenance.ROOT_MELEE,
                () -> original.call(target, source, proposedDamage)
        );
    }
}
