package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Production-code registration boundary for Riftfrontier's required in-world regression tests.
 * Test instances and their tiny empty structure remain data-driven under data/riftfrontier/test_instance.
 */
public final class RiftfrontierGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        Riftfrontier.MOD_ID
    );

    static {
        TEST_FUNCTIONS.register("authoritative_runtime_state", () -> RiftfrontierGameTests::authoritativeRuntimeState);
    }

    private RiftfrontierGameTests() {}

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private static void authoritativeRuntimeState(GameTestHelper helper) {
        var snapshot = ContentRuntime.requireCurrent();
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
    }
}
