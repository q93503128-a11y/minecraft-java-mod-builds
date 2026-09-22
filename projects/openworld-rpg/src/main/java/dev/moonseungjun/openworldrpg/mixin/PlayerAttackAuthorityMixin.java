package dev.moonseungjun.openworldrpg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectMinecraftDamageApplicator;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.bettercombat.BetterCombatAuthorityAdapter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

        /*
         * Until a creature has a project combat binding, Better Combat/vanilla remains the
         * compatibility fallback. A project-owned actor may never fall back to donor damage.
         */
        if (!(target instanceof LivingEntity livingTarget)
                || !ExternalActorBindingRuntime.ownsDamageAuthority(livingTarget)) {
            return original.call(target, source, proposedDamage);
        }

        long gameTick = attacker.level().getGameTime();
        var build = CombatStateServices.combatBuilds().build(attacker.getUUID()).orElse(null);
        var targetSnapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(livingTarget, gameTick)
                .orElse(null);
        if (build == null || targetSnapshot == null) {
            return false;
        }

        CombatDamageAuthority.MeleeDamageDecision decision =
                CombatDamageAuthority.authorizeBetterCombatMelee(
                        proposedDamage,
                        context.comboCount(),
                        build,
                        targetSnapshot
                );
        if (!decision.accepted()) {
            return false;
        }

        boolean applied = ProjectMinecraftDamageApplicator.applyDirectPhysical(
                attacker,
                livingTarget,
                decision.finalDamage()
        );
        if (!applied) {
            return false;
        }

        if (decision.poiseDamage() > 0.0) {
            ExternalActorBindingRuntime.applyProjectPoiseDamage(
                    livingTarget,
                    decision.poiseDamage(),
                    gameTick
            );
        }
        CombatStateServices.markCombatActivity(attacker.getUUID(), gameTick);
        return true;
    }
}
