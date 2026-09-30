package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned projection of canonical project weapon cadence and equipment Attack Speed.
 *
 * <p>Better Combat derives melee animation/cooldown timing from Minecraft ATTACK_SPEED, so the
 * project corrects that attribute to the canonical family event cadence. Bow/Crossbow draw timing
 * is instead owned by Ranged Weapon API's synced {@code ranged_weapon:haste} attribute; the item's
 * authored pull-time remains the family baseline and this publisher applies only the gear bonus.</p>
 */
public final class PlayerAttackSpeedRuntime {
    static final Identifier PROJECT_ATTACK_SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "project_attack_speed"
            );
    static final Identifier PROJECT_RANGED_HASTE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(
                    OpenworldRpgMod.MOD_ID,
                    "equipment_ranged_haste"
            );
    static final Identifier RANGED_HASTE_ATTRIBUTE_ID =
            Identifier.fromNamespaceAndPath("ranged_weapon", "haste");

    private PlayerAttackSpeedRuntime() {
    }

    public static void synchronize(
            Player player,
            Optional<ProjectWeaponFamily> weaponFamily,
            double equipmentAttackSpeedBonus
    ) {
        synchronize(
                player,
                weaponFamily,
                equipmentAttackSpeedBonus,
                0.0
        );
    }

    public static void synchronize(
            Player player,
            Optional<ProjectWeaponFamily> weaponFamily,
            double equipmentAttackSpeedBonus,
            double classAttackSpeedBonus
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(weaponFamily, "weaponFamily");
        if (player.level().isClientSide()) {
            throw new IllegalStateException(
                    "Project Attack Speed authority is server-only."
            );
        }
        if (!Double.isFinite(classAttackSpeedBonus)
                || classAttackSpeedBonus < 0.0
                || classAttackSpeedBonus > 1.0) {
            throw new IllegalArgumentException(
                    "classAttackSpeedBonus must be inside [0, 1]."
            );
        }
        if (!Double.isFinite(equipmentAttackSpeedBonus)
                || equipmentAttackSpeedBonus < 0.0
                || equipmentAttackSpeedBonus
                        > PlayerEquipmentLoadoutState.ATTACK_SPEED_GEAR_CAP) {
            throw new IllegalArgumentException(
                    "equipmentAttackSpeedBonus must be inside the canonical gear cap [0, 0.35]."
            );
        }

        var attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            throw new IllegalStateException(
                    "Server player has no ATTACK_SPEED attribute."
            );
        }
        attackSpeed.removeModifier(PROJECT_ATTACK_SPEED_MODIFIER_ID);

        var rangedHasteHolder = BuiltInRegistries.ATTRIBUTE
                .get(RANGED_HASTE_ATTRIBUTE_ID)
                .orElse(null);
        var rangedHaste = rangedHasteHolder == null
                ? null
                : player.getAttribute(rangedHasteHolder);
        if (rangedHaste != null) {
            rangedHaste.removeModifier(PROJECT_RANGED_HASTE_MODIFIER_ID);
        }

        if (weaponFamily.isEmpty()) {
            return;
        }

        ProjectWeaponFamily family = weaponFamily.orElseThrow();
        if (!family.hasR01AttackSpeedRuntime()) {
            return;
        }

        if (family.usesRangedDrawCadence()) {
            if (rangedHaste == null) {
                if (FabricLoader.getInstance().isModLoaded("ranged_weapon_api")) {
                    throw new IllegalStateException(
                            "Ranged Weapon API is loaded but ranged_weapon:haste is unavailable."
                    );
                }
                return;
            }
            double combinedAttackSpeedBonus =
                    equipmentAttackSpeedBonus + classAttackSpeedBonus;
            if (combinedAttackSpeedBonus > 0.0) {
                rangedHaste.addOrUpdateTransientModifier(
                        new AttributeModifier(
                                PROJECT_RANGED_HASTE_MODIFIER_ID,
                                combinedAttackSpeedBonus,
                                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                        )
                );
            }
            return;
        }

        double targetEventsPerSecond =
                family.basicAttackEventsPerSecond()
                        * (1.0
                        + equipmentAttackSpeedBonus
                        + classAttackSpeedBonus);
        double correction = targetEventsPerSecond - attackSpeed.getValue();
        if (Math.abs(correction) > 1.0e-9) {
            attackSpeed.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            PROJECT_ATTACK_SPEED_MODIFIER_ID,
                            correction,
                            AttributeModifier.Operation.ADD_VALUE
                    )
            );
        }
    }
}
