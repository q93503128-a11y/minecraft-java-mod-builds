package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.authority.CombatDamageAuthority;
import dev.moonseungjun.openworldrpg.combat.runtime.HunterSkillRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDamageApplicationContext;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectMinecraftDamageApplicator;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectRangedProjectileContext;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.multiplayer.MultiplayerCombatRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Project-owned external-actor damage fails closed in both directions.
 *
 * <p>Bound actors cannot receive donor/vanilla HP damage, and their donor-origin outgoing damage
 * cannot directly hit players. Exactly one project applicator call may cross either boundary with
 * a one-shot authorization token.</p>
 */
@Mixin(LivingEntity.class)
public abstract class ExternalActorDamageAuthorityMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void openworldRpg$enforceExternalActorDamageAuthority(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!MultiplayerCombatRules.DIRECT_PLAYER_PVP_ENABLED
                && self instanceof Player victim
                && source.getEntity() instanceof Player attacker
                && attacker != victim) {
            cir.setReturnValue(false);
            return;
        }

        if (ExternalActorBindingRuntime.ownsDamageAuthority(self)) {
            if (ProjectDamageApplicationContext.consumeIfAuthorized(self)) {
                return;
            }

            if (source.getEntity() instanceof Player shooter
                    && source.getDirectEntity() instanceof AbstractArrow arrow) {
                long gameTick = level.getGameTime();
                var shot = ProjectRangedProjectileContext.shot(
                        arrow,
                        shooter
                ).orElse(null);
                var targetSnapshot = ExternalActorBindingRuntime
                        .projectTargetSnapshot(self, gameTick)
                        .orElse(null);
                if (shot == null || targetSnapshot == null) {
                    cir.setReturnValue(false);
                    return;
                }

                CombatDamageAuthority.RangedDamageDecision decision =
                        switch (shot.weaponFamily()) {
                            case BOW -> CombatDamageAuthority.authorizeBowProjectileBasic(
                                    amount,
                                    shot.drawPower(),
                                    shot.build(),
                                    targetSnapshot,
                                    shooter.getRandom().nextDouble()
                            );
                            case CROSSBOW -> CombatDamageAuthority.authorizeProjectileBasic(
                                    amount,
                                    shot.build(),
                                    targetSnapshot,
                                    shooter.getRandom().nextDouble()
                            );
                            default -> CombatDamageAuthority.RangedDamageDecision.rejected();
                        };
                if (!decision.accepted()) {
                    cir.setReturnValue(false);
                    return;
                }

                boolean applied = ProjectMinecraftDamageApplicator.applyDirectPhysical(
                        shooter,
                        self,
                        decision.finalDamage()
                );
                var poiseApplication = applied
                        && decision.poiseDamage() > 0.0
                        ? ExternalActorBindingRuntime.applyProjectPoiseDamage(
                                self,
                                decision.poiseDamage(),
                                gameTick
                        ).orElse(null)
                        : null;
                if (applied) {
                    CombatStateServices.markCombatActivity(
                            shooter.getUUID(),
                            gameTick
                    );
                    if (shooter instanceof ServerPlayer serverShooter) {
                        HunterSkillRuntime.onRangedBasicHit(
                                serverShooter,
                                self,
                                shot.launchPosition().distanceTo(
                                        arrow.position()
                                ),
                                false,
                                poiseApplication != null
                                        && poiseApplication.breakTriggered(),
                                gameTick
                        );
                    }
                }
                cir.setReturnValue(applied);
                return;
            }

            cir.setReturnValue(false);
            return;
        }

        if (self instanceof Player
                && source.getEntity() instanceof LivingEntity attacker
                && ExternalActorBindingRuntime.ownsDamageAuthority(attacker)
                && !ProjectDamageApplicationContext.consumeIfAuthorized(self)) {
            cir.setReturnValue(false);
        }
    }
}
