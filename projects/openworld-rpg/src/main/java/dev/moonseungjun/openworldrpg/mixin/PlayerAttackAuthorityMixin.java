package dev.moonseungjun.openworldrpg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.integration.bettercombat.BetterCombatAuthorityAdapter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerAttackAuthorityMixin {
    @WrapOperation(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean openworldRpg$authorizeBetterCombatPrimaryDamage(
            Entity target,
            DamageSource source,
            float proposedDamage,
            Operation<Boolean> original
    ) {
        Player attacker = (Player) (Object) this;
        if (attacker.level().isClientSide()) {
            return original.call(target, source, proposedDamage);
        }

        BetterCombatAuthorityAdapter.AttackContext context =
                BetterCombatAuthorityAdapter.currentAttack(attacker);
        if (!context.active()) {
            return original.call(target, source, proposedDamage);
        }

        CombatDamageAuthority.DamageDecision decision =
                CombatDamageAuthority.authorizeBetterCombatMelee(proposedDamage, context.comboCount());
        if (!decision.accepted()) {
            return false;
        }

        return original.call(target, source, decision.amount());
    }
}
