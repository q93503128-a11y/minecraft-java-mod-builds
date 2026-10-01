package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side immutable launch metadata for project-owned ranged basics.
 *
 * <p>The projectile captures the complete canonical combat build at release. Impact authority must
 * use this snapshot rather than the shooter's then-current equipment/class, so swapping gear while
 * an arrow or bolt is in flight cannot alter damage, critical affixes, weapon family or poise.</p>
 */
public final class ProjectRangedProjectileContext {
    private static final Map<AbstractArrow, RangedShot> SHOTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ProjectRangedProjectileContext() {
    }

    public static void recordBowShot(
            LivingEntity shooter,
            Projectile projectile,
            float drawPower
    ) {
        recordShot(shooter, projectile, ProjectWeaponFamily.BOW, drawPower);
    }

    public static void recordCrossbowShot(
            LivingEntity shooter,
            Projectile projectile
    ) {
        recordShot(shooter, projectile, ProjectWeaponFamily.CROSSBOW, 1.0F);
    }

    private static void recordShot(
            LivingEntity shooter,
            Projectile projectile,
            ProjectWeaponFamily expectedFamily,
            float drawPower
    ) {
        Objects.requireNonNull(expectedFamily, "expectedFamily");
        if (shooter.level().isClientSide()
                || !(shooter instanceof Player player)
                || !(projectile instanceof AbstractArrow arrow)
                || !Float.isFinite(drawPower)
                || drawPower <= 0.0F
                || drawPower > 1.0F) {
            return;
        }

        PlayerCombatBuildState build = CombatStateServices.combatBuilds()
                .build(player.getUUID())
                .orElse(null);
        if (build == null || build.equipment().weaponFamily() != expectedFamily) {
            return;
        }

        SHOTS.put(
                arrow,
                new RangedShot(
                        player.getUUID(),
                        expectedFamily,
                        build,
                        drawPower,
                        shooter.position(),
                        PlayerEquipmentService.state(player)
                                .aggregateWeakPointDamageBonus()
                )
        );
    }

    public static Optional<RangedShot> shot(
            AbstractArrow arrow,
            Player expectedShooter
    ) {
        RangedShot shot = SHOTS.get(arrow);
        if (shot == null || !shot.shooterId().equals(expectedShooter.getUUID())) {
            return Optional.empty();
        }
        return Optional.of(shot);
    }

    public record RangedShot(
            UUID shooterId,
            ProjectWeaponFamily weaponFamily,
            PlayerCombatBuildState build,
            double drawPower,
            Vec3 launchPosition,
            double weakPointDamageBonus
    ) {
        public RangedShot {
            Objects.requireNonNull(shooterId, "shooterId");
            Objects.requireNonNull(weaponFamily, "weaponFamily");
            Objects.requireNonNull(build, "build");
            Objects.requireNonNull(launchPosition, "launchPosition");
            if (!Double.isFinite(weakPointDamageBonus)
                    || weakPointDamageBonus < 0.0) {
                throw new IllegalArgumentException(
                        "weakPointDamageBonus must be finite and non-negative."
                );
            }
            if (weaponFamily != ProjectWeaponFamily.BOW
                    && weaponFamily != ProjectWeaponFamily.CROSSBOW) {
                throw new IllegalArgumentException(
                        "Ranged projectile snapshot supports only Bow/Crossbow basics."
                );
            }
            if (build.equipment().weaponFamily() != weaponFamily) {
                throw new IllegalArgumentException(
                        "Ranged projectile build family must match launch family."
                );
            }
            if (!Double.isFinite(drawPower) || drawPower <= 0.0 || drawPower > 1.0) {
                throw new IllegalArgumentException(
                        "drawPower must be finite and inside (0, 1]."
                );
            }
            if (weaponFamily == ProjectWeaponFamily.CROSSBOW && drawPower != 1.0) {
                throw new IllegalArgumentException(
                        "Crossbow projectile snapshots are committed full shots."
                );
            }
        }
    }
}
