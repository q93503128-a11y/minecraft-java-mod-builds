package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * Server-side launch metadata that donor projectile damage cannot reconstruct safely at impact.
 *
 * <p>BowItem receives the final launch power after Ranged Weapon API has applied its authoritative
 * pull-time handling. We preserve that value on the projectile boundary and use it only as a
 * project basic-attack coefficient input. Donor projectile damage remains non-authoritative.</p>
 */
public final class ProjectRangedProjectileContext {
    private static final Map<AbstractArrow, BowShot> BOW_SHOTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ProjectRangedProjectileContext() {
    }

    public static void recordBowShot(
            LivingEntity shooter,
            Projectile projectile,
            float drawPower
    ) {
        if (shooter.level().isClientSide()
                || !(shooter instanceof Player player)
                || !(projectile instanceof AbstractArrow arrow)
                || !Float.isFinite(drawPower)
                || drawPower <= 0.0F
                || drawPower > 1.0F) {
            return;
        }

        var build = CombatStateServices.combatBuilds().build(player.getUUID()).orElse(null);
        if (build == null || build.equipment().weaponFamily() != ProjectWeaponFamily.BOW) {
            return;
        }

        BOW_SHOTS.put(
                arrow,
                new BowShot(player.getUUID(), drawPower)
        );
    }

    public static Optional<BowShot> bowShot(
            AbstractArrow arrow,
            Player expectedShooter
    ) {
        BowShot shot = BOW_SHOTS.get(arrow);
        if (shot == null || !shot.shooterId().equals(expectedShooter.getUUID())) {
            return Optional.empty();
        }
        return Optional.of(shot);
    }

    public record BowShot(UUID shooterId, double drawPower) {
        public BowShot {
            if (shooterId == null) {
                throw new IllegalArgumentException("shooterId must not be null.");
            }
            if (!Double.isFinite(drawPower) || drawPower <= 0.0 || drawPower > 1.0) {
                throw new IllegalArgumentException("drawPower must be finite and inside (0, 1].");
            }
        }
    }
}
