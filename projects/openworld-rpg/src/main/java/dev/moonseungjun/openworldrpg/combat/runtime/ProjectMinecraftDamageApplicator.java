package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01NatureSpiritCombatRuntime;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01RegalhartCombatRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01NatureSpiritRewardService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartRewardService;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Minecraft-side application of an already-resolved project damage amount.
 *
 * <p>The backing project damage types bypass vanilla armor/effect/enchantment/resistance/shield/
 * cooldown layers so an amount is not mitigated a second time after project Defense/MR and authored
 * mitigation have already resolved. They also suppress damage-source knockback because the caller
 * owns presentation/knockback exactly once (Better Combat for melee, spell delivery for magic).
 * Creative/invulnerability protections remain intact.</p>
 */
public final class ProjectMinecraftDamageApplicator {
    private static final ResourceKey<DamageType> PROJECT_DIRECT_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "project_direct_magic")
    );
    private static final ResourceKey<DamageType> PROJECT_DIRECT_PHYSICAL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "project_direct_physical")
    );

    private ProjectMinecraftDamageApplicator() {
    }

    public static boolean applyDirectMagic(
            LivingEntity attacker,
            LivingEntity target,
            double finalDamage
    ) {
        return applyResolved(attacker, target, finalDamage, PROJECT_DIRECT_MAGIC);
    }

    public static boolean applyTimedMagic(
            LivingEntity attacker,
            LivingEntity target,
            double finalDamage
    ) {
        return applyResolved(
                attacker,
                target,
                finalDamage,
                PROJECT_DIRECT_MAGIC
        );
    }

    public static boolean applyDirectPhysical(
            LivingEntity attacker,
            LivingEntity target,
            double finalDamage
    ) {
        final LivingEntity combatTarget;
        if (ExternalActorBindingRuntime.ownsDamageAuthority(target)) {
            combatTarget = ExternalActorBindingRuntime
                    .damageAuthorityTarget(target)
                    .orElse(null);
            if (combatTarget == null) {
                return false;
            }
        } else {
            combatTarget = target;
        }

        if (attacker instanceof ServerPlayer hunter) {
            finalDamage = ProjectCombatRules.roundFinal(
                    finalDamage
                            * HunterRootPassiveEffects
                                    .quarryDirectDamageMultiplier(
                                            hunter,
                                            HunterSkillRuntime.isCurrentQuarry(
                                                    hunter,
                                                    combatTarget
                                            )
                                    )
            );
        }
        return applyResolved(
                attacker,
                combatTarget,
                finalDamage,
                PROJECT_DIRECT_PHYSICAL
        );
    }

    private static boolean applyResolved(
            LivingEntity attacker,
            LivingEntity target,
            double finalDamage,
            ResourceKey<DamageType> damageTypeKey
    ) {
        final boolean externalTarget =
                ExternalActorBindingRuntime.ownsDamageAuthority(target);
        final LivingEntity damageTarget;
        if (externalTarget) {
            damageTarget = ExternalActorBindingRuntime
                    .damageAuthorityTarget(target)
                    .orElse(null);
            if (damageTarget == null) {
                return false;
            }
        } else {
            damageTarget = target;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)
                || damageTarget.level() != serverLevel
                || damageTarget == attacker
                || !Double.isFinite(finalDamage)
                || finalDamage <= 0.0) {
            return false;
        }

        var damageTypes = serverLevel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        var damageType = damageTypes.getOrThrow(damageTypeKey);
        DamageSource source = new DamageSource(damageType, attacker);

        if (externalTarget) {
            long gameTick = serverLevel.getGameTime();
            double encounterAdjustedDamage = finalDamage
                    * R01EarthloongPhysicalEncounterRuntime
                            .incomingDamageMultiplier(damageTarget)
                    * R01RegalhartCombatRuntime
                            .incomingDamageMultiplier(
                                    damageTarget,
                                    gameTick
                            );
            var application = ExternalActorBindingRuntime.applyProjectHealthDamage(
                    damageTarget,
                    encounterAdjustedDamage,
                    proxyDamage -> ProjectDamageApplicationContext.authorizeNext(
                            damageTarget,
                            () -> damageTarget.hurtServer(
                                    serverLevel,
                                    source,
                                    (float) Math.min(proxyDamage, Float.MAX_VALUE)
                            )
                    )
            );
            if (application.isEmpty()) {
                return false;
            }
            var applied = application.orElseThrow();
            if (attacker instanceof ServerPlayer) {
                R01NatureSpiritCombatRuntime
                        .recordPostMitigationHostileDamage(
                                damageTarget,
                                applied.appliedDamage(),
                                gameTick
                        );
            }
            if (attacker instanceof ServerPlayer player) {
                R01EarthloongEncounterService.recordDamageContribution(
                        damageTarget,
                        player
                );
                R01NatureSpiritRewardService.recordDamageContribution(
                        damageTarget,
                        player
                );
                R01RegalhartRewardService.recordDamageContribution(
                        damageTarget,
                        player
                );
                R01EarthloongPhysicalEncounterRuntime.recordProjectDamageThreat(
                        damageTarget,
                        player,
                        applied.appliedDamage(),
                        gameTick
                );
            }
            if (applied.killed()) {
                R01EarthloongEncounterService.resolveDefeat(damageTarget);
                R01NatureSpiritRewardService.resolveDefeat(damageTarget);
                R01RegalhartRewardService.resolveDefeat(damageTarget);
            }
            return true;
        }

        float amount = (float) Math.min(finalDamage, Float.MAX_VALUE);
        if (damageTarget instanceof Player
                && ExternalActorBindingRuntime.ownsDamageAuthority(attacker)) {
            return ProjectDamageApplicationContext.authorizeNext(
                    damageTarget,
                    () -> damageTarget.hurtServer(serverLevel, source, amount)
            );
        }
        return damageTarget.hurtServer(serverLevel, source, amount);
    }
}
