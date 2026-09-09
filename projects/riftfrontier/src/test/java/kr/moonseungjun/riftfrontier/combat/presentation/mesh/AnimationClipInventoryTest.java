package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import kr.moonseungjun.riftfrontier.combat.presentation.Region01BossDragonEvolvedAnimationContract;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimationClipInventoryTest {
    @Test
    void verifiesExactDragonEvolvedSourceInventoryWithoutGuessingMetrics() {
        List<AnimationClip> clips = Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS.stream()
            .map(AnimationClipInventoryTest::clip)
            .toList();

        AnimationClipInventory inventory = Region01BossDragonEvolvedAnimationContract.verifyImportedClips(clips);

        assertEquals(Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS, inventory.names());
        assertEquals(8, inventory.metrics().size());
        assertSame(clips.get(3), inventory.requireClip("Headbutt"));
        AnimationClipInventory.ClipMetrics punch = inventory.metrics().get(6);
        assertEquals("Punch", punch.name());
        assertEquals(1.0f, punch.durationSeconds(), 1.0e-6f);
        assertEquals(1, punch.channelCount());
        assertEquals(2, punch.totalKeyCount());
        assertEquals(1, punch.translationChannels());
        assertEquals(1, punch.linearChannels());
    }

    @Test
    void rejectsMissingOrUnexpectedDragonSourceClip() {
        List<AnimationClip> clips = new ArrayList<>(Region01BossDragonEvolvedAnimationContract.EXPECTED_SOURCE_CLIPS.stream()
            .map(AnimationClipInventoryTest::clip)
            .toList());
        clips.removeIf(clip -> clip.name().equals("Punch"));
        clips.add(clip("Invented_Attack"));

        assertThrows(IllegalArgumentException.class,
            () -> Region01BossDragonEvolvedAnimationContract.verifyImportedClips(clips));
    }

    @Test
    void rejectsDuplicateImportedSourceNames() {
        assertThrows(IllegalArgumentException.class,
            () -> AnimationClipInventory.fromImported(List.of(clip("Punch"), clip("Punch"))));
    }

    @Test
    void requireClipFailsClosedInsteadOfSubstitutingFallback() {
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(clip("Punch")));
        assertThrows(IllegalArgumentException.class, () -> inventory.requireClip("Headbutt"));
    }

    private static AnimationClip clip(String name) {
        AnimationClip.Channel channel = new AnimationClip.Channel(
            0,
            AnimationClip.Path.TRANSLATION,
            AnimationClip.Interpolation.LINEAR,
            new float[]{0.0f, 1.0f},
            new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f}
        );
        return new AnimationClip(name, 1.0f, List.of(channel));
    }
}
