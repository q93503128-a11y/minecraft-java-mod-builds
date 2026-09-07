package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayService;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionLifecycle;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionRun;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Production-code registration boundary for Riftfrontier's required in-world regression tests. */
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
        helper.assertTrue(snapshot.definitionCount() >= 20, "Content runtime must publish fixture plus production region_01 graph");
        helper.assertTrue(!snapshot.fingerprint().isBlank(), "Content runtime fingerprint must not be blank");

        var lifecycle = new ExpeditionLifecycle(snapshot);
        var worldData = RiftfrontierWorldData.get(helper.getLevel());
        int supplyBefore = worldData.expeditionSupply();
        int storageBefore = worldData.securedRegion01Salvage();
        int pressureBefore = worldData.region01Pressure();
        helper.assertTrue(supplyBefore >= worldData.region01PreparationSupplyCost(), "Fresh authoritative world must fund the first expedition preparation");
        helper.assertTrue(worldData.consumeRegion01PreparationSupply(), "Preparation must atomically consume authoritative supply");
        helper.assertTrue(worldData.expeditionSupply() < supplyBefore, "Preparation supply must decrease before deployment");

        long expected = worldData.expeditionSequence() + 1L;
        ExpeditionRun validated = lifecycle.begin(
            expected,
            ExpeditionGameplayService.REGION_ID,
            ExpeditionGameplayService.CONTRACT_ID,
            snapshot.fingerprint(),
            helper.getLevel().getGameTime()
        );
        ExpeditionRun persisted = worldData.createExpedition(
            ExpeditionGameplayService.REGION_ID,
            ExpeditionGameplayService.CONTRACT_ID,
            snapshot.fingerprint(),
            helper.getLevel().getGameTime()
        );
        helper.assertTrue(persisted.sequence() == validated.sequence(), "Validated run sequence must match authoritative allocation");

        ExpeditionRun deployed = lifecycle.deploy(persisted);
        worldData.updateExpedition(deployed);
        ExpeditionRun recovered = lifecycle.recover(deployed, ExpeditionGameplayService.RESOURCE_ID, 3);
        worldData.updateExpedition(recovered);
        ExpeditionRun requested = lifecycle.requestExtraction(recovered);
        worldData.updateExpedition(requested);
        var resolution = lifecycle.resolveExtraction(requested, helper.getLevel().getGameTime());
        worldData.updateExpedition(resolution.run());

        int retained = resolution.retainedResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        worldData.settleRegion01Extraction(retained);
        helper.assertTrue(worldData.securedRegion01Salvage() == storageBefore + retained, "Retained field resource must settle into authoritative hub storage");
        helper.assertTrue(worldData.region01Pressure() == pressureBefore + 1, "Successful extraction must advance Region 01 world-response pressure");

        int supplyAfterExpedition = worldData.expeditionSupply();
        worldData.provisionRegion01Supply();
        helper.assertTrue(worldData.securedRegion01Salvage() == storageBefore + retained - 1, "Provisioning must spend secured salvage rather than duplicate it");
        helper.assertTrue(worldData.expeditionSupply() == supplyAfterExpedition + 2, "Provisioning must create the documented two-supply preparation stock");

        var resolvedAgain = RiftfrontierWorldData.get(helper.getLevel());
        var persistedAgain = resolvedAgain.expedition(resolution.run().sequence()).orElseThrow();
        helper.assertTrue(resolvedAgain == worldData, "SavedData lookup must return the authoritative cached world root");
        helper.assertTrue(persistedAgain.status() == ExpeditionRun.Status.EXTRACTED, "Completed expedition must persist as EXTRACTED");
        helper.assertTrue(
            persistedAgain.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0) == 3,
            "Recovered production resource must survive authoritative updates"
        );
        helper.assertTrue(snapshot.fingerprint().equals(resolvedAgain.contentFingerprint()), "World root must retain the active content fingerprint");
        helper.assertTrue(resolvedAgain.worldRevision() > 0L, "World revision must advance after authoritative mutations");
        helper.succeed();
    }
}
