package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.neoforged.testframework.DynamicTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.gametest.GameTest;

/** Minimal in-world regression gate for the authoritative M1 runtime/persistence path. */
public final class RiftfrontierGameTests {
    private RiftfrontierGameTests() {}

    @GameTest
    @EmptyTemplate
    @TestHolder(description = "Validated content snapshot and authoritative world SavedData remain connected")
    static void authoritativeRuntimeState(final DynamicTest test) {
        test.onGameTest(helper -> {
            var snapshot = ContentRuntime.snapshot();
            helper.assertTrue(snapshot.definitionCount() > 0, "Content runtime must publish at least one validated definition");
            helper.assertTrue(!snapshot.fingerprint().isBlank(), "Content runtime fingerprint must not be blank");

            var worldData = RiftfrontierWorldData.get(helper.getLevel());
            long before = worldData.expeditionSequence();
            long allocated = worldData.allocateExpeditionSequence(snapshot.fingerprint());
            var resolvedAgain = RiftfrontierWorldData.get(helper.getLevel());

            helper.assertTrue(resolvedAgain == worldData, "SavedData lookup must return the authoritative cached world root");
            helper.assertTrue(allocated == before + 1L, "Expedition sequence must increase exactly once");
            helper.assertTrue(
                snapshot.fingerprint().equals(resolvedAgain.contentFingerprint()),
                "World root must retain the active validated content fingerprint"
            );
            helper.assertTrue(resolvedAgain.worldRevision() > 0L, "World revision must advance after authoritative mutation");
            helper.succeed();
        });
    }
}
