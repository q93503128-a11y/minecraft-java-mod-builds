package dev.moonseungjun.openworldrpg.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OrdinaryEquipmentAffixRollerTest {
    private static final List<OrdinaryEquipmentAffixRoller.AffixDefinition>
            FULL_FIXTURE = List.of(
            primary("vit"),
            primary("end"),
            primary("str"),
            primary("dex"),
            primary("int"),
            primary("wil"),
            percent("physical_power",
                    OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                    2.5, 7.5, false),
            percent("critical_chance",
                    OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                    1.0, 3.5, true),
            percent("attack_speed",
                    OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                    2.0, 6.0, false),
            percent("defense",
                    OrdinaryEquipmentAffixRoller.AffixCategory.DEFENSE,
                    3.0, 9.0, false),
            percent("max_hp",
                    OrdinaryEquipmentAffixRoller.AffixCategory.DEFENSE,
                    2.5, 7.0, false),
            percent("max_mana",
                    OrdinaryEquipmentAffixRoller.AffixCategory.RESOURCE,
                    3.0, 9.0, false),
            percent("mana_recovery",
                    OrdinaryEquipmentAffixRoller.AffixCategory.RESOURCE,
                    4.0, 12.0, false),
            percent("movement_speed",
                    OrdinaryEquipmentAffixRoller.AffixCategory.UTILITY,
                    1.0, 3.5, true),
            percent("healing_done",
                    OrdinaryEquipmentAffixRoller.AffixCategory.UTILITY,
                    4.0, 12.0, false)
    );

    @Test
    void gradeCountsAndFloorsMatchCanon() {
        assertEquals(1, OrdinaryEquipmentAffixRoller.affixCount(
                ProjectItemGrade.STANDARD
        ));
        assertEquals(2, OrdinaryEquipmentAffixRoller.affixCount(
                ProjectItemGrade.REFINED
        ));
        assertEquals(3, OrdinaryEquipmentAffixRoller.affixCount(
                ProjectItemGrade.SUPERIOR
        ));
        assertEquals(4, OrdinaryEquipmentAffixRoller.affixCount(
                ProjectItemGrade.EXALTED
        ));

        assertEquals(0.60, OrdinaryEquipmentAffixRoller.percentileFloor(
                ProjectItemGrade.STANDARD
        ));
        assertEquals(0.65, OrdinaryEquipmentAffixRoller.percentileFloor(
                ProjectItemGrade.REFINED
        ));
        assertEquals(0.70, OrdinaryEquipmentAffixRoller.percentileFloor(
                ProjectItemGrade.SUPERIOR
        ));
        assertEquals(0.75, OrdinaryEquipmentAffixRoller.percentileFloor(
                ProjectItemGrade.EXALTED
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> OrdinaryEquipmentAffixRoller.affixCount(
                        ProjectItemGrade.MYTHIC
                )
        );
    }

    @Test
    void categoryWeightsMatchGlobalAndArmorOverrides() {
        assertEquals(
                new OrdinaryEquipmentAffixRoller.CategoryWeights(
                        20, 50, 0, 20, 10
                ),
                OrdinaryEquipmentAffixRoller.categoryWeights(
                        OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON
                )
        );
        assertEquals(
                new OrdinaryEquipmentAffixRoller.CategoryWeights(
                        25, 20, 15, 30, 10
                ),
                OrdinaryEquipmentAffixRoller.categoryWeights(
                        OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_LIGHT
                )
        );
        assertEquals(
                new OrdinaryEquipmentAffixRoller.CategoryWeights(
                        25, 10, 45, 5, 15
                ),
                OrdinaryEquipmentAffixRoller.categoryWeights(
                        OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_HEAVY
                )
        );
        assertEquals(
                new OrdinaryEquipmentAffixRoller.CategoryWeights(
                        20, 35, 5, 30, 10
                ),
                OrdinaryEquipmentAffixRoller.categoryWeights(
                        OrdinaryEquipmentAffixRoller.ItemFamily.MAGICAL_FOCUS
                )
        );
    }

    @Test
    void rollsAreDeterministicUniqueAndRespectTwoPerCategoryCap() {
        var request = new OrdinaryEquipmentAffixRoller.RollRequest(
                ProjectItemGrade.EXALTED,
                20,
                OrdinaryEquipmentAffixRoller.ItemFamily.ARMOR_MEDIUM,
                FULL_FIXTURE,
                987654321L
        );

        var first = OrdinaryEquipmentAffixRoller.roll(request);
        var second = OrdinaryEquipmentAffixRoller.roll(request);
        assertEquals(first, second);
        assertEquals(4, first.size());
        assertEquals(
                first.size(),
                first.stream().map(
                        OrdinaryEquipmentAffixRoller.GeneratedAffix::id
                ).distinct().count()
        );

        Map<OrdinaryEquipmentAffixRoller.AffixCategory, Long> counts =
                first.stream().collect(Collectors.groupingBy(
                        OrdinaryEquipmentAffixRoller.GeneratedAffix::category,
                        Collectors.counting()
                ));
        assertTrue(counts.values().stream().allMatch(value -> value <= 2L));
    }

    @Test
    void fixedAffixIsKeptAndOnlyRemainingSlotsRollRandomly() {
        var result = OrdinaryEquipmentAffixRoller.roll(
                new OrdinaryEquipmentAffixRoller.RollRequest(
                        ProjectItemGrade.SUPERIOR,
                        6,
                        OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON,
                        FULL_FIXTURE,
                        444L,
                        List.of("openworld_rpg:affix/str")
                )
        );

        assertEquals(3, result.size());
        assertEquals("openworld_rpg:affix/str", result.getFirst().id());
        assertEquals(
                3,
                result.stream()
                        .map(OrdinaryEquipmentAffixRoller.GeneratedAffix::id)
                        .distinct()
                        .count()
        );
    }

    @Test
    void fixedAffixMustBelongToEligiblePool() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrdinaryEquipmentAffixRoller.RollRequest(
                        ProjectItemGrade.REFINED,
                        4,
                        OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON,
                        FULL_FIXTURE,
                        1L,
                        List.of("openworld_rpg:affix/not_real")
                )
        );
    }

    @Test
    void seedChangesDeterministicResult() {
        var a = OrdinaryEquipmentAffixRoller.roll(
                new OrdinaryEquipmentAffixRoller.RollRequest(
                        ProjectItemGrade.SUPERIOR,
                        6,
                        OrdinaryEquipmentAffixRoller.ItemFamily.ACCESSORY,
                        FULL_FIXTURE,
                        111L
                )
        );
        var b = OrdinaryEquipmentAffixRoller.roll(
                new OrdinaryEquipmentAffixRoller.RollRequest(
                        ProjectItemGrade.SUPERIOR,
                        6,
                        OrdinaryEquipmentAffixRoller.ItemFamily.ACCESSORY,
                        FULL_FIXTURE,
                        222L
                )
        );
        assertNotEquals(a, b);
    }

    @Test
    void valueRollsStayInsideGradeFlooredCanonicalRanges() {
        for (long seed = 0L; seed < 200L; seed++) {
            var rolled = OrdinaryEquipmentAffixRoller.roll(
                    new OrdinaryEquipmentAffixRoller.RollRequest(
                            ProjectItemGrade.SUPERIOR,
                            6,
                            OrdinaryEquipmentAffixRoller.ItemFamily.ACCESSORY,
                            FULL_FIXTURE,
                            seed
                    )
            );

            for (var affix : rolled) {
                if (affix.id().endsWith("/critical_chance")
                        || affix.id().endsWith("/movement_speed")) {
                    assertEquals(
                            Math.rint(affix.value() * 10.0),
                            affix.value() * 10.0,
                            0.000001
                    );
                } else if (affix.category()
                        != OrdinaryEquipmentAffixRoller.AffixCategory.PRIMARY) {
                    assertEquals(
                            Math.rint(affix.value() * 2.0),
                            affix.value() * 2.0,
                            0.000001
                    );
                } else {
                    assertEquals(
                            Math.rint(affix.value()),
                            affix.value(),
                            0.000001
                    );
                }
            }
        }
    }

    @Test
    void filteredCategoriesAreRenormalizedWithoutInventingInvalidAffixes() {
        var onlyOffenseAndUtility = List.of(
                percent("physical_power",
                        OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                        2.5, 7.5, false),
                percent("critical_chance",
                        OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE,
                        1.0, 3.5, true),
                percent("movement_speed",
                        OrdinaryEquipmentAffixRoller.AffixCategory.UTILITY,
                        1.0, 3.5, true)
        );

        for (long seed = 0L; seed < 100L; seed++) {
            var result = OrdinaryEquipmentAffixRoller.roll(
                    new OrdinaryEquipmentAffixRoller.RollRequest(
                            ProjectItemGrade.REFINED,
                            4,
                            OrdinaryEquipmentAffixRoller.ItemFamily.NORMAL_WEAPON,
                            onlyOffenseAndUtility,
                            seed
                    )
            );
            assertTrue(result.stream().allMatch(value ->
                    value.category()
                            == OrdinaryEquipmentAffixRoller.AffixCategory.OFFENSE
                    || value.category()
                            == OrdinaryEquipmentAffixRoller.AffixCategory.UTILITY
            ));
        }
    }

    private static OrdinaryEquipmentAffixRoller.AffixDefinition primary(
            String id
    ) {
        return OrdinaryEquipmentAffixRoller.AffixDefinition.primary(
                "openworld_rpg:affix/" + id,
                "openworld_rpg:runtime_affix/" + id,
                new OrdinaryEquipmentAffixRoller.PrimaryCurve(
                        1.0,
                        0.045,
                        1.0,
                        2.5
                )
        );
    }

    private static OrdinaryEquipmentAffixRoller.AffixDefinition percent(
            String id,
            OrdinaryEquipmentAffixRoller.AffixCategory category,
            double min,
            double max,
            boolean tenth
    ) {
        return OrdinaryEquipmentAffixRoller.AffixDefinition.percent(
                "openworld_rpg:affix/" + id,
                category,
                min,
                max,
                tenth,
                "openworld_rpg:runtime_affix/" + id
        );
    }
}
