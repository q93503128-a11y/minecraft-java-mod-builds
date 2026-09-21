package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Minecraft-side application of an already-resolved project damage amount.
 *
 * <p>The backing damage type bypasses vanilla armor/effect/enchantment/resistance/shield/cooldown
 * layers so the amount is not mitigated a second time after project Defense/MR and authored
 * mitigation have already resolved. It intentionally does not bypass invulnerability/creative
 * protections.</p>
 */
public final class ProjectMinecraftDamageApplicator {
    private static final ResourceKey<DamageType> PROJECT_DIRECT_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "project_direct_magic")
    );

    private ProjectMinecraftDamageApplicator() {
    }

    public static boolean applyDirectMagic(
            Player attacker,
            LivingEntity target,
            double finalDamage
    ) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)
                || target.level() != serverLevel
                || target == attacker
                || !Double.isFinite(finalDamage)
                || finalDamage <= 0.0) {
            return false;
        }

        float amount = (float) Math.min(finalDamage, Float.MAX_VALUE);
        var damageTypes = serverLevel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        var damageType = damageTypes.getOrThrow(PROJECT_DIRECT_MAGIC);
        DamageSource source = new DamageSource(damageType, attacker);
        return target.hurtServer(serverLevel, source, amount);
    }
}
