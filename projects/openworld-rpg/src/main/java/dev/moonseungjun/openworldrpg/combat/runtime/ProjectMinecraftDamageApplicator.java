package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongPhysicalEncounterRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
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

    public static boolean applyDirectPhysical(
            LivingEntity attacker,
            LivingEntity target,
            double finalDamage
    ) {
        return applyResolved(attacker, target, finalDamage, PROJECT_DIRECT_PHYSICAL);
    }

    private static boolean applyResolved(
            LivingEntity attacker,
            LivingEntity target,
            double finalDamage,
            ResourceKey<DamageType> damageTypeKey
    ) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)
                || target.level() != serverLevel
                || target == attacker
                || !Double.isFinite(finalDamage)
                || finalDamage <= 0.0) {
            return false;
        }

        var damageTypes = serverLevel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        var damageType = damageTypes.getOrThrow(damageTypeKey);
        DamageSource source = new DamageSource(damageType, attacker);

        if (ExternalActorBindingRuntime.ownsDamageAuthority(target)) {
            double encounterAdjustedDamage = finalDamage
                    * R01EarthloongPhysicalEncounterRuntime.incomingDamageMultiplier(target);
            var application = ExternalActorBindingRuntime.applyProjectHealthDamage(
                    target,
                    encounterAdjustedDamage,
                    proxyDamage -> ProjectDamageApplicationContext.authorizeNext(
                            target,
                            () -> target.hurtServer(
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
            if (attacker instanceof ServerPlayer player) {
                R01EarthloongEncounterService.recordDamageContribution(
                        target,
                        player
                );
                R01EarthloongPhysicalEncounterRuntime.recordProjectDamageThreat(
                        target,
                        player,
                        applied.appliedDamage(),
                        serverLevel.getGameTime()
                );
            }
            if (applied.killed()) {
                R01EarthloongEncounterService.resolveDefeat(target);
            }
            return true;
        }

        float amount = (float) Math.min(finalDamage, Float.MAX_VALUE);
        if (target instanceof Player
                && ExternalActorBindingRuntime.ownsDamageAuthority(attacker)) {
            return ProjectDamageApplicationContext.authorizeNext(
                    target,
                    () -> target.hurtServer(serverLevel, source, amount)
            );
        }
        return target.hurtServer(serverLevel, source, amount);
    }
}
