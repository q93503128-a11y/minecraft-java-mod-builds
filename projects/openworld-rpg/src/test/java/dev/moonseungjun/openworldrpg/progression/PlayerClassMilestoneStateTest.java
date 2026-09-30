package dev.moonseungjun.openworldrpg.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class PlayerClassMilestoneStateTest {
    @Test
    void milestonesStayOrderedAndDoctrineCanSwitchAfterRank32() {
        var specialization =
                ClassSpecialization.HUNTER_RANGER;
        var state = PlayerClassMilestoneState.initial();
        var progress = state.progress(specialization);
        var beforeRank20 = progress;

        assertThrows(
                IllegalStateException.class,
                () -> beforeRank20.completeRank32Doctrine(
                        ClassDoctrine.RANGER_SKIRMISHER
                )
        );

        progress = progress.completeRank20Technique();
        progress = progress.completeRank32Doctrine(
                ClassDoctrine.RANGER_SKIRMISHER
        );
        assertTrue(progress.rank20TechniqueComplete());
        assertTrue(progress.rank32DoctrineComplete());
        assertFalse(progress.rank44AscendantComplete());
        assertEquals(
                ClassDoctrine.RANGER_SKIRMISHER,
                progress.activeDoctrine().orElseThrow()
        );

        var completedRank32 = progress;
        assertEquals(
                completedRank32,
                completedRank32.completeRank32Doctrine(
                        ClassDoctrine.RANGER_FIELD_CONTROLLER
                )
        );

        progress = progress.selectDoctrine(
                ClassDoctrine.RANGER_FIELD_CONTROLLER
        );
        assertEquals(
                ClassDoctrine.RANGER_FIELD_CONTROLLER,
                progress.activeDoctrine().orElseThrow()
        );

        progress = progress.completeRank44Ascendant();
        assertTrue(progress.rank44AscendantComplete());

        state = state.update(specialization, progress);
        assertEquals(progress, state.progress(specialization));
    }

    @Test
    void stateRejectsDoctrineStoredUnderWrongSpecialization() {
        var bad = new BranchMilestoneProgress(
                true,
                true,
                false,
                java.util.Optional.of(
                        ClassDoctrine.MARKSMAN_PATIENT_AIM
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerClassMilestoneState(
                        PlayerClassMilestoneState
                                .CURRENT_SCHEMA_VERSION,
                        java.util.Map.of(
                                ClassSpecialization.HUNTER_RANGER.id(),
                                bad
                        )
                )
        );
    }

    @Test
    void milestoneStateSurvivesCodecRoundTrip() {
        var specialization =
                ClassSpecialization.MAGE_ELEMENTALIST;
        var progress = BranchMilestoneProgress.initial()
                .completeRank20Technique()
                .completeRank32Doctrine(
                        ClassDoctrine.ELEMENTALIST_CONFLUENCE
                )
                .completeRank44Ascendant();
        var original = PlayerClassMilestoneState.initial()
                .update(specialization, progress);

        var encoded = PlayerClassMilestoneState.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        var decoded = PlayerClassMilestoneState.CODEC
                .parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(original, decoded);
    }

    @Test
    void everySpecializationOwnsExactlyTwoDoctrines() {
        for (ClassSpecialization specialization
                : ClassSpecialization.values()) {
            var doctrines =
                    ClassDoctrine.forSpecialization(
                            specialization
                    );
            assertEquals(2, doctrines.size());
            assertTrue(
                    doctrines.stream().allMatch(
                            doctrine ->
                                    doctrine.specialization()
                                            == specialization
                    )
            );
        }
    }
}
