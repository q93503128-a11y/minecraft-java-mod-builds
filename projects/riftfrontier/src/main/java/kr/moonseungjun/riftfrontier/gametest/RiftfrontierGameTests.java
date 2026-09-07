package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.content.ContentCatalog;
import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionLifecycle;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionRun;
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
        helper.assertTrue(snapshot.definitionCount() >= 10, "Content runtime must publish the M2 expedition fixture graph");
        helper.assertTrue(!snapshot.fingerprint().isBlank(), "Content runtime fingerprint must not be blank");

        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var lifecycle = new ExpeditionLifecycle(pack.registry());
        String fixtureFingerprint = ContentCatalog.from(pack.registry()).fingerprint();
        ContentId region = ContentId.rift("region/vertical_slice_01");
        ContentId contract = ContentId.rift("contract/salvage_recovery");
        ContentId salvage = ContentId.rift("resource/rift_salvage");

        var worldData = RiftfrontierWorldData.get(helper.getLevel());
        long before = worldData.expeditionSequence();
        ExpeditionRun preparing = lifecycle.begin(before + 1L, region, contract, fixtureFingerprint, helper.getLevel().getGameTime());
        long allocated = worldData.allocateExpeditionSequence(fixtureFingerprint);
        helper.assertTrue(allocated == preparing.sequence(), "Domain run sequence must match authoritative allocation");

        ExpeditionRun persisted = worldData.createExpedition(region, contract, fixtureFingerprint, helper.getLevel().getGameTime());
        helper.assertTrue(persisted.sequence() == allocated + 1L, "Persisted run allocation must advance monotonically");
        ExpeditionRun deployed = lifecycle.deploy(persisted);
        worldData.updateExpedition(deployed);
        ExpeditionRun recovered = lifecycle.recover(deployed, salvage, 3);
        worldData.updateExpedition(recovered);
        ExpeditionRun requested = lifecycle.requestExtraction(recovered);
        worldData.updateExpedition(requested);
        var resolution = lifecycle.resolveExtraction(requested, helper.getLevel().getGameTime());
        worldData.updateExpedition(resolution.run());

        var resolvedAgain = RiftfrontierWorldData.get(helper.getLevel());
        var persistedAgain = resolvedAgain.expedition(resolution.run().sequence()).orElseThrow();

        helper.assertTrue(resolvedAgain == worldData, "SavedData lookup must return the authoritative cached world root");
        helper.assertTrue(persistedAgain.status() == ExpeditionRun.Status.EXTRACTED, "Completed expedition must persist as EXTRACTED");
        helper.assertTrue(persistedAgain.recoveredResources().getOrDefault(salvage, 0) == 3, "Recovered resources must survive authoritative updates");
        helper.assertTrue(
            fixtureFingerprint.equals(resolvedAgain.contentFingerprint()),
            "World root must retain the content fingerprint that authored the expedition"
        );
        helper.assertTrue(resolvedAgain.worldRevision() > 0L, "World revision must advance after authoritative mutations");
        helper.succeed();
    }
}
