package kr.moonseungjun.riftfrontier.combat.presentation;

import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClip;
import kr.moonseungjun.riftfrontier.combat.presentation.mesh.AnimationClipInventory;
import kr.moonseungjun.riftfrontier.content.ContentId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BossAnimationSourceBindingTest {
    @Test
    void resolvesOnlyExplicitVerifiedSourceClipNames() {
        AnimationClip punch = clip("Punch");
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(punch));
        ContentId logical = ContentId.rift("boss/region_01/attack/active");
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(Map.of(logical, "Punch"));

        assertSame(punch, binding.resolve(inventory).get(logical));
        new BossAnimationSampleBridge(binding, inventory);
    }

    @Test
    void rejectsBindingToSourceClipThatWasNotImported() {
        AnimationClipInventory inventory = AnimationClipInventory.fromImported(List.of(clip("Punch")));
        BossAnimationSourceBinding binding = new BossAnimationSourceBinding(Map.of(
            ContentId.rift("boss/region_01/attack/active"), "Headbutt"
        ));

        assertThrows(IllegalArgumentException.class, () -> binding.resolve(inventory));
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSampleBridge(binding, inventory));
    }

    @Test
    void rejectsBlankSourceClipName() {
        assertThrows(IllegalArgumentException.class, () -> new BossAnimationSourceBinding(Map.of(
            ContentId.rift("boss/region_01/attack/active"), " "
        )));
    }

    private static AnimationClip clip(String name) {
        return new AnimationClip(name, 1.0f, List.of(new AnimationClip.Channel(
            0,
            AnimationClip.Path.ROTATION,
            AnimationClip.Interpolation.LINEAR,
            new float[]{0.0f, 1.0f},
            new float[]{0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f}
        )));
    }
}
