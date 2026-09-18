package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.integration.bettercombat.BetterCombatAuthorityAdapter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerAttackAuthorityMixin {
    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean openworldRpg$authorizeBetterCombatPrimaryDamage(
            Entity target,
            DamageSource source,
            float proposedDamage
    ) {
        Player attacker = (Player) (Object) this;
        if (attacker.level().isClientSide()) {
            return target.hurtOrSimulate(source, proposedDamage);
        }

        BetterCombatAuthorityAdapter.AttackContext context =
                BetterCombatAuthorityAdapter.currentAttack(attacker);
        if (!context.active()) {
            return target.hurtOrSimulate(source, proposedDamage);
        }

        CombatDamageAuthority.DamageDecision decision =
                CombatDamageAuthority.authorizeBetterCombatMelee(proposedDamage, context.comboCount());
        if (!decision.accepted()) {
            return false;
        }

        return target.hurtOrSimulate(source, decision.amount());
    }

    @Redirect(
            method = "doSweepAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean openworldRpg$suppressVanillaSweepDamageDuringBetterCombat(
            LivingEntity target,
            ServerLevel level,
            DamageSource source,
            float damage
    ) {
        Player attacker = (Player) (Object) this;
        BetterCombatAuthorityAdapter.AttackContext context =
                BetterCombatAuthorityAdapter.currentAttack(attacker);
        if (context.active()) {
            return false;
        }
        return target.hurtServer(level, source, damage);
    }
}
