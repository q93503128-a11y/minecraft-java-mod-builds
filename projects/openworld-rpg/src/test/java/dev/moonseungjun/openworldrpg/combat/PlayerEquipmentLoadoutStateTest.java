package dev.moonseungjun.openworldrpg.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffix;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatAffixKind;
import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentLoadoutState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerEquipmentLoadoutStateTest {
    @Test
    void twelveSlotLoadoutAggregatesOnlyCombatRelevantEquippedAffixes() {
        var staff = EquippedCombatItem.weapon(
                "openworld_rpg:initiate_staff",
                8,
                ProjectWeaponFamily.STAFF,
                List.of(
                        EquipmentCombatAffix.flat(EquipmentCombatAffixKind.INT, 3),
                        EquipmentCombatAffix.flat(EquipmentCombatAffixKind.MAGIC_POWER, 0.075)
                )
        );
        var necklace = EquippedCombatItem.gear(
                "openworld_rpg:greenwater_pendant",
                ProjectEquipmentSlot.NECKLACE,
                8,
                List.of(
                        EquipmentCombatAffix.flat(EquipmentCombatAffixKind.WIL, 2),
                        EquipmentCombatAffix.familyPower(ProjectWeaponFamily.STAFF, 0.10),
                        EquipmentCombatAffix.familyPower(ProjectWeaponFamily.BOW, 0.50)
                )
        );
        var relic = EquippedCombatItem.gear(
                "openworld_rpg:quarry_seal",
                ProjectEquipmentSlot.RELIC,
                8,
                List.of(EquipmentCombatAffix.flat(EquipmentCombatAffixKind.POISE_OUTPUT, 0.20))
        );

        var loadout = new PlayerEquipmentLoadoutState(List.of(staff, necklace, relic));
        var equipment = loadout.aggregateCombatState().orElseThrow();
        var build = new PlayerCombatBuildState(
                8,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0),
                equipment
        );
        var source = build.damageSource(ProjectImpactTransaction.DamageSchool.MAGIC);

        assertEquals(3.0, equipment.flatAttributeBonuses().intel(), 0.0001);
        assertEquals(2.0, equipment.flatAttributeBonuses().wil(), 0.0001);
        assertEquals(0.075, equipment.magicPowerBonus(), 0.0001);
        assertEquals(0.10, equipment.weaponFamilyPowerBonus(), 0.0001);
        assertEquals(0.20, equipment.poiseOutputBonus(), 0.0001);
        assertEquals(0.0, equipment.supplementalMagicWeaponPower(), 0.0001);
        assertEquals(30.0, source.weaponPower(), 0.0001);
        assertEquals(13.8, source.weightedOffensiveStat(), 0.0001);
        assertEquals(0.175, source.additivePowerBonus(), 0.0001);
        assertEquals(1.02, source.poiseOutputMultiplier(), 0.0001);
    }

    @Test
    void wandAndFocusUseCanonicalSupplementalMagicWeaponPower() {
        var wand = EquippedCombatItem.weapon(
                "openworld_rpg:initiate_wand",
                8,
                ProjectWeaponFamily.WAND,
                List.of()
        );
        var focus = EquippedCombatItem.focus(
                "openworld_rpg:apprentice_focus",
                8,
                List.of()
        );

        var equipment = new PlayerEquipmentLoadoutState(List.of(wand, focus))
                .aggregateCombatState()
                .orElseThrow();
        var build = new PlayerCombatBuildState(
                8,
                RootClass.MAGE,
                new AttributeAllocation(0, 0, 0, 0, 7, 0),
                equipment
        );

        var magic = build.damageSource(ProjectImpactTransaction.DamageSchool.MAGIC);
        var physical = build.damageSource(ProjectImpactTransaction.DamageSchool.PHYSICAL);

        assertEquals(5.4846, equipment.supplementalMagicWeaponPower(), 0.0001);
        assertEquals(30.4846, magic.weaponPower(), 0.0001);
        assertEquals(25.0, physical.weaponPower(), 0.0001);
    }

    @Test
    void familyPowerRespectsCanonicalSixtyPercentGearCap() {
        var loadout = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.weapon(
                        "openworld_rpg:initiate_staff",
                        8,
                        ProjectWeaponFamily.STAFF,
                        List.of(EquipmentCombatAffix.familyPower(ProjectWeaponFamily.STAFF, 0.40))
                ),
                EquippedCombatItem.gear(
                        "openworld_rpg:roadworn_band",
                        ProjectEquipmentSlot.RING_1,
                        8,
                        List.of(EquipmentCombatAffix.familyPower(ProjectWeaponFamily.STAFF, 0.40))
                )
        ));

        assertEquals(
                0.60,
                loadout.aggregateCombatState().orElseThrow().weaponFamilyPowerBonus(),
                0.0001
        );
    }

    @Test
    void missingMainWeaponProducesNoCombatEquipmentAuthority() {
        var loadout = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.gear(
                        "openworld_rpg:roadworn_band",
                        ProjectEquipmentSlot.RING_1,
                        8,
                        List.of(EquipmentCombatAffix.flat(EquipmentCombatAffixKind.INT, 2))
                )
        ));

        assertTrue(loadout.aggregateCombatState().isEmpty());
        assertEquals(2.0, loadout.aggregateFlatAttributeBonuses().intel(), 0.0001);
    }

    @Test
    void duplicateSlotsAndStaffFocusCombinationFailClosed() {
        var ringA = EquippedCombatItem.gear(
                "openworld_rpg:ring_a",
                ProjectEquipmentSlot.RING_1,
                8,
                List.of()
        );
        var ringB = EquippedCombatItem.gear(
                "openworld_rpg:ring_b",
                ProjectEquipmentSlot.RING_1,
                8,
                List.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerEquipmentLoadoutState(List.of(ringA, ringB))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerEquipmentLoadoutState(List.of(
                        EquippedCombatItem.weapon(
                                "openworld_rpg:initiate_staff",
                                8,
                                ProjectWeaponFamily.STAFF,
                                List.of()
                        ),
                        EquippedCombatItem.focus(
                                "openworld_rpg:apprentice_focus",
                                8,
                                List.of()
                        )
                ))
        );
    }

    @Test
    void equippedLoadoutSurvivesCodecRoundTrip() {
        var original = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.weapon(
                        "openworld_rpg:initiate_staff",
                        8,
                        ProjectWeaponFamily.STAFF,
                        List.of(
                                EquipmentCombatAffix.flat(EquipmentCombatAffixKind.INT, 3),
                                EquipmentCombatAffix.flat(EquipmentCombatAffixKind.MAGIC_POWER, 0.075)
                        )
                )
        ));

        var encoded = PlayerEquipmentLoadoutState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerEquipmentLoadoutState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }
}
