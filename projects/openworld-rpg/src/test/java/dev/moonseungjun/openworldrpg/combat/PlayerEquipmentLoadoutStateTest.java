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
import dev.moonseungjun.openworldrpg.combat.state.ProjectArmorArchetype;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.ProjectShieldFamily;
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
    @Test
    void canonicalArmorReferenceTotalsUsePerSlotRoundedScaling() {
        int[] levels = {1, 8, 20, 44, 64, 80};
        int[][] light = {
                {15, 21}, {22, 29}, {30, 42}, {51, 71}, {67, 92}, {81, 112}
        };
        int[][] medium = {
                {25, 13}, {35, 19}, {50, 26}, {83, 44}, {112, 58}, {133, 70}
        };
        int[][] heavy = {
                {33, 7}, {46, 9}, {66, 14}, {111, 23}, {147, 30}, {177, 37}
        };

        for (int i = 0; i < levels.length; i++) {
            assertDefenseTotals(ProjectArmorArchetype.LIGHT, levels[i], light[i]);
            assertDefenseTotals(ProjectArmorArchetype.MEDIUM, levels[i], medium[i]);
            assertDefenseTotals(ProjectArmorArchetype.HEAVY, levels[i], heavy[i]);
        }
    }

    @Test
    void defenseAndMagicResistanceAffixesModifyCanonicalArmorTotals() {
        var loadout = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.armor(
                        "openworld_rpg:river_scholar_chest",
                        ProjectEquipmentSlot.CHEST,
                        8,
                        ProjectArmorArchetype.LIGHT,
                        List.of(
                                EquipmentCombatAffix.flat(EquipmentCombatAffixKind.DEFENSE, 0.09),
                                EquipmentCombatAffix.flat(EquipmentCombatAffixKind.MAGIC_RESISTANCE, 0.05)
                        )
                ),
                EquippedCombatItem.gear(
                        "openworld_rpg:greenwater_pendant",
                        ProjectEquipmentSlot.NECKLACE,
                        8,
                        List.of(
                                EquipmentCombatAffix.flat(EquipmentCombatAffixKind.DEFENSE, 0.03),
                                EquipmentCombatAffix.flat(EquipmentCombatAffixKind.MAGIC_RESISTANCE, 0.04)
                        )
                )
        ));

        var defense = loadout.aggregateDefenseSnapshot();

        assertEquals(7.84, defense.defense(), 0.0001);
        assertEquals(10.90, defense.magicResistance(), 0.0001);
        assertTrue(defense.guardType().isEmpty());
        assertEquals(0.0, defense.guardRating(), 0.0001);
        assertTrue(loadout.aggregateCombatState().isEmpty());
    }

    @Test
    void equippedShieldPublishesCanonicalGuardRatingWithFiftyPercentGearCap() {
        var loadout = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.shield(
                        "openworld_rpg:earthscale_ward",
                        8,
                        ProjectShieldFamily.STANDARD,
                        List.of(EquipmentCombatAffix.flat(
                                EquipmentCombatAffixKind.GUARD_STRENGTH,
                                0.40
                        ))
                ),
                EquippedCombatItem.gear(
                        "openworld_rpg:quarry_seal",
                        ProjectEquipmentSlot.RELIC,
                        8,
                        List.of(EquipmentCombatAffix.flat(
                                EquipmentCombatAffixKind.GUARD_STRENGTH,
                                0.30
                        ))
                )
        ));

        var defense = loadout.aggregateDefenseSnapshot();

        assertEquals(
                dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority.GuardType.STANDARD_SHIELD,
                defense.guardType().orElseThrow()
        );
        assertEquals(42.0, defense.guardRating(), 0.0001);
        assertEquals(0.0, defense.defense(), 0.0001);
        assertEquals(0.0, defense.magicResistance(), 0.0001);
    }

    @Test
    void armorAndShieldMetadataSurviveCodecRoundTripWithoutInventingWeaponGuard() {
        var original = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.armor(
                        "openworld_rpg:ironbound_chest",
                        ProjectEquipmentSlot.CHEST,
                        8,
                        ProjectArmorArchetype.HEAVY,
                        List.of(EquipmentCombatAffix.flat(
                                EquipmentCombatAffixKind.DEFENSE,
                                0.07
                        ))
                ),
                EquippedCombatItem.shield(
                        "openworld_rpg:watch_buckler",
                        4,
                        ProjectShieldFamily.BUCKLER,
                        List.of(EquipmentCombatAffix.flat(
                                EquipmentCombatAffixKind.GUARD_STRENGTH,
                                0.15
                        ))
                )
        ));

        var encoded = PlayerEquipmentLoadoutState.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerEquipmentLoadoutState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
        assertEquals(21.85, decoded.aggregateDefenseSnapshot().guardRating(), 0.0001);
    }

    private static void assertDefenseTotals(
            ProjectArmorArchetype archetype,
            int itemLevel,
            int[] expected
    ) {
        var defense = armorSet(archetype, itemLevel).aggregateDefenseSnapshot();
        assertEquals(expected[0], defense.defense(), 0.0001);
        assertEquals(expected[1], defense.magicResistance(), 0.0001);
    }

    private static PlayerEquipmentLoadoutState armorSet(
            ProjectArmorArchetype archetype,
            int itemLevel
    ) {
        return new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.armor(
                        "openworld_rpg:test_head",
                        ProjectEquipmentSlot.HEAD,
                        itemLevel,
                        archetype,
                        List.of()
                ),
                EquippedCombatItem.armor(
                        "openworld_rpg:test_chest",
                        ProjectEquipmentSlot.CHEST,
                        itemLevel,
                        archetype,
                        List.of()
                ),
                EquippedCombatItem.armor(
                        "openworld_rpg:test_legs",
                        ProjectEquipmentSlot.LEGS,
                        itemLevel,
                        archetype,
                        List.of()
                ),
                EquippedCombatItem.armor(
                        "openworld_rpg:test_gloves",
                        ProjectEquipmentSlot.GLOVES,
                        itemLevel,
                        archetype,
                        List.of()
                ),
                EquippedCombatItem.armor(
                        "openworld_rpg:test_boots",
                        ProjectEquipmentSlot.BOOTS,
                        itemLevel,
                        archetype,
                        List.of()
                )
        ));
    }

    @Test
    void partialArmorPublishesCanonicalPlayerPoiseContributionAndResistanceCap() {
        var loadout = new PlayerEquipmentLoadoutState(List.of(
                EquippedCombatItem.armor(
                        "openworld_rpg:medium_chest",
                        ProjectEquipmentSlot.CHEST,
                        8,
                        ProjectArmorArchetype.MEDIUM,
                        List.of(EquipmentCombatAffix.flat(
                                EquipmentCombatAffixKind.POISE_STAGGER_RESISTANCE,
                                0.15
                        ))
                ),
                EquippedCombatItem.armor(
                        "openworld_rpg:heavy_legs",
                        ProjectEquipmentSlot.LEGS,
                        8,
                        ProjectArmorArchetype.HEAVY,
                        List.of()
                ),
                EquippedCombatItem.gear(
                        "openworld_rpg:poise_relic",
                        ProjectEquipmentSlot.RELIC,
                        8,
                        List.of(EquipmentCombatAffix.flat(
                                EquipmentCombatAffixKind.POISE_STAGGER_RESISTANCE,
                                0.45
                        ))
                )
        ));

        assertEquals(13.70, loadout.aggregateArmorPoise(), 0.0001);
        assertEquals(0.50, loadout.aggregatePoiseStaggerResistanceBonus(), 0.0001);
        assertEquals(
                74.55,
                dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules.maxPlayerPoise(
                        15.0,
                        loadout.aggregateArmorPoise(),
                        loadout.aggregatePoiseStaggerResistanceBonus()
                ),
                0.0001
        );
    }

}
