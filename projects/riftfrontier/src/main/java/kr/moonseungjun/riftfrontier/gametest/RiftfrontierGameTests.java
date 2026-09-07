package kr.moonseungjun.riftfrontier.gametest;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionEvidenceCheckpoint;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayService;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionLifecycle;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionRun;
import kr.moonseungjun.riftfrontier.expedition.Region01EncounterRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

/** Production-code registration boundary for Riftfrontier's required in-world regression tests. */
public final class RiftfrontierGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
        BuiltInRegistries.TEST_FUNCTION,
        Riftfrontier.MOD_ID
    );
    private static final UUID TEST_OWNER = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    static {
        TEST_FUNCTIONS.register("authoritative_runtime_state", () -> RiftfrontierGameTests::authoritativeRuntimeState);
        TEST_FUNCTIONS.register("region_01_encounter_runtime", () -> RiftfrontierGameTests::region01EncounterRuntime);
        TEST_FUNCTIONS.register("restart_reconciliation", () -> RiftfrontierGameTests::restartReconciliation);
        TEST_FUNCTIONS.register("extraction_evidence_atomicity", () -> RiftfrontierGameTests::extractionEvidenceAtomicity);
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
            TEST_OWNER,
            snapshot.fingerprint(),
            helper.getLevel().getGameTime()
        );
        ExpeditionRun persisted = worldData.createExpedition(
            ExpeditionGameplayService.REGION_ID,
            ExpeditionGameplayService.CONTRACT_ID,
            TEST_OWNER,
            snapshot.fingerprint(),
            helper.getLevel().getGameTime()
        );
        helper.assertTrue(persisted.sequence() == validated.sequence(), "Validated run sequence must match authoritative allocation");
        helper.assertTrue(persisted.ownerId().equals(validated.ownerId()), "Validated participant must match authoritative persisted owner");
        helper.assertTrue(persisted.ownedBy(TEST_OWNER), "Authoritative run must be bound to the expected participant UUID");
        helper.assertTrue(persisted.endReason() == ExpeditionRun.EndReason.NONE, "New expedition must not begin with a terminal cause");

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
        helper.assertTrue(persistedAgain.endReason() == ExpeditionRun.EndReason.EXTRACTION, "Successful extraction must persist an explicit extraction cause");
        helper.assertTrue(persistedAgain.ownedBy(TEST_OWNER), "Owner UUID must survive authoritative updates through extraction");
        helper.assertTrue(
            persistedAgain.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0) == 3,
            "Recovered production resource must survive authoritative updates"
        );
        helper.assertTrue(snapshot.fingerprint().equals(resolvedAgain.contentFingerprint()), "World root must retain the active content fingerprint");
        helper.assertTrue(resolvedAgain.worldRevision() > 0L, "World revision must advance after authoritative mutations");
        helper.succeed();
    }

    private static void region01EncounterRuntime(GameTestHelper helper) {
        var low = Region01EncounterRuntime.planForPressure(0);
        var high = Region01EncounterRuntime.planForPressure(6);
        helper.assertTrue(low.hunters() == 1 && low.scouts() == 1 && low.elites() == 1, "Pressure zero must begin with the minimal two-role patrol plus elite");
        helper.assertTrue(high.hunters() > low.hunters(), "Higher pressure must add pursuing threats");
        helper.assertTrue(high.scouts() > low.scouts(), "Higher pressure must add ranged/skirmishing threats");
        helper.assertTrue(high.hazardTicks() > low.hazardTicks(), "Higher pressure must lengthen the salvage hazard");
        helper.assertTrue(high.hazardAmplifier() > low.hazardAmplifier(), "Higher pressure must intensify the salvage hazard");

        BlockPos center = helper.absolutePos(new BlockPos(8, 3, 8));
        long technicalRun = 900_001L;
        Region01EncounterRuntime.clearRun(helper.getLevel(), center, technicalRun);
        var spawned = Region01EncounterRuntime.begin(helper.getLevel(), center, technicalRun, 0);
        helper.assertTrue(
            Region01EncounterRuntime.liveThreatCount(helper.getLevel(), center, technicalRun) == spawned.totalThreats(),
            "Encounter runtime must spawn and track every hunter/scout/elite role in the actual GameTest world"
        );
        helper.assertTrue(!Region01EncounterRuntime.patrolCleared(helper.getLevel(), center, technicalRun), "Live encounter must not report patrol-cleared");

        var localThreats = helper.getLevel().getEntitiesOfClass(Mob.class, new AABB(center).inflate(12.0D, 8.0D, 12.0D));
        helper.assertTrue(!localThreats.isEmpty(), "Encounter must expose at least one live proxy for lure-boundary regression coverage");
        Mob lured = localThreats.getFirst();
        lured.snapTo(center.getX() + 48.5D, center.getY(), center.getZ() + 48.5D, lured.getYRot(), lured.getXRot());
        helper.assertTrue(
            Region01EncounterRuntime.liveThreatCount(helper.getLevel(), center, technicalRun) == spawned.totalThreats(),
            "A live proxy outside the technical cell must still block patrol-cleared bonus eligibility"
        );

        Region01EncounterRuntime.clearRun(helper.getLevel(), center, technicalRun);
        helper.assertTrue(lured.isRemoved(), "Terminal cleanup must discard a tracked proxy even after it left the technical cell");
        helper.assertTrue(Region01EncounterRuntime.patrolCleared(helper.getLevel(), center, technicalRun), "Run cleanup must remove every encounter proxy");
        helper.succeed();
    }

    private static void restartReconciliation(GameTestHelper helper) {
        var snapshot = ContentRuntime.requireCurrent();
        var lifecycle = new ExpeditionLifecycle(snapshot);
        var worldData = RiftfrontierWorldData.get(helper.getLevel());
        helper.assertTrue(ExpeditionGameplayService.active(worldData).isEmpty(), "Restart regression requires no pre-existing active expedition");

        int supplyBefore = worldData.expeditionSupply();
        helper.assertTrue(worldData.consumeRegion01PreparationSupply(), "Restart regression must spend preparation supply before deployment");
        int supplyAfterSpend = worldData.expeditionSupply();
        helper.assertTrue(supplyAfterSpend < supplyBefore, "Restart reconciliation must have a non-refundable preparation cost to preserve");

        ExpeditionRun persisted = worldData.createExpedition(
            ExpeditionGameplayService.REGION_ID,
            ExpeditionGameplayService.CONTRACT_ID,
            TEST_OWNER,
            snapshot.fingerprint(),
            helper.getLevel().getGameTime()
        );
        ExpeditionRun deployed = lifecycle.deploy(persisted);
        worldData.updateExpedition(deployed);
        helper.assertTrue(deployed.ownedBy(TEST_OWNER), "Restart fixture must begin as an owner-bound run");
        helper.assertTrue(ExpeditionGameplayService.active(worldData).isPresent(), "Deployed run must be non-terminal before restart reconciliation");

        ExpeditionRun failed = ExpeditionGameplayService.reconcileAfterServerRestart(helper.getLevel()).orElseThrow();
        helper.assertTrue(failed.sequence() == deployed.sequence(), "Restart reconciliation must fail the exact persisted active run");
        helper.assertTrue(failed.status() == ExpeditionRun.Status.FAILED, "Restart reconciliation must choose explicit FAILED instead of guessing recovery state");
        helper.assertTrue(failed.endReason() == ExpeditionRun.EndReason.SERVER_RESTART, "Restart reconciliation must preserve a machine-readable server_restart cause");
        helper.assertTrue(failed.ownedBy(TEST_OWNER), "Restart reconciliation must preserve the participant owner identity");
        helper.assertTrue(ExpeditionGameplayService.active(worldData).isEmpty(), "Restart reconciliation must leave no authoritative non-terminal run");
        helper.assertTrue(worldData.expeditionSupply() == supplyAfterSpend, "Server restart failure must not refund already-spent expedition supply");
        helper.assertTrue(
            worldData.expedition(failed.sequence()).orElseThrow().endReason() == ExpeditionRun.EndReason.SERVER_RESTART,
            "Authoritative SavedData must retain the restart failure cause for later review/re-entry UX"
        );

        long orphanRun = 9_999_991L;
        Zombie orphan = new Zombie(helper.getLevel());
        orphan.addTag("riftfrontier.region01.run." + orphanRun);
        orphan.addTag("riftfrontier.region01.role.hunter");
        BlockPos orphanPos = helper.absolutePos(new BlockPos(4, 3, 4));
        orphan.snapTo(orphanPos.getX() + 0.5D, orphanPos.getY(), orphanPos.getZ() + 0.5D, 0.0F, 0.0F);
        boolean accepted = helper.getLevel().addFreshEntity(orphan);
        helper.assertTrue(!accepted || orphan.isRemoved(), "A persisted tagged proxy with no process-local run tracker must be rejected or discarded when it loads");
        helper.succeed();
    }

    private static void extractionEvidenceAtomicity(GameTestHelper helper) {
        var snapshot = ContentRuntime.requireCurrent();
        var lifecycle = new ExpeditionLifecycle(snapshot);
        var worldData = RiftfrontierWorldData.get(helper.getLevel());
        helper.assertTrue(ExpeditionGameplayService.active(worldData).isEmpty(), "Extraction atomicity regression requires no pre-existing active expedition");

        ExpeditionRun persisted = worldData.createExpedition(
            ExpeditionGameplayService.REGION_ID,
            ExpeditionGameplayService.CONTRACT_ID,
            TEST_OWNER,
            snapshot.fingerprint(),
            helper.getLevel().getGameTime()
        );
        ExpeditionRun deployed = lifecycle.deploy(persisted);
        deployed = deployed.appendEvidence(new ExpeditionEvidenceCheckpoint(
            ExpeditionEvidenceCheckpoint.Stage.DEPLOYED,
            helper.getLevel().getGameTime(),
            0,
            -1,
            worldData.securedRegion01Salvage(),
            worldData.expeditionSupply(),
            worldData.region01Pressure()
        ));
        worldData.updateExpedition(deployed);

        int evidenceBefore = deployed.fieldEvidence().size();
        long revisionBefore = worldData.worldRevision();
        boolean rejected = false;
        try {
            lifecycle.requestExtraction(deployed);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Underfilled Region 01 extraction request must be rejected before any authoritative mutation");

        ExpeditionRun after = worldData.expedition(deployed.sequence()).orElseThrow();
        helper.assertTrue(after.status() == ExpeditionRun.Status.DEPLOYED, "Rejected extraction must leave the authoritative run DEPLOYED");
        helper.assertTrue(after.fieldEvidence().size() == evidenceBefore, "Rejected extraction must not append PRE_EXTRACTION evidence");
        helper.assertTrue(
            after.fieldEvidence().stream().noneMatch(checkpoint -> checkpoint.stage() == ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION),
            "Rejected extraction must not leave a misleading PRE_EXTRACTION checkpoint"
        );
        helper.assertTrue(worldData.worldRevision() == revisionBefore, "Rejected extraction must not dirty authoritative SavedData");

        ExpeditionRun recovered = lifecycle.recover(after, ExpeditionGameplayService.RESOURCE_ID, 3);
        worldData.updateExpedition(recovered);
        ExpeditionRun accepted = lifecycle.requestExtraction(recovered);
        helper.assertTrue(accepted.status() == ExpeditionRun.Status.EXTRACTION_REQUESTED, "A filled contract must still cross the extraction gate normally");
        helper.assertTrue(accepted.fieldEvidence().size() == evidenceBefore, "Lifecycle acceptance itself must not forge gameplay-adapter evidence");
        helper.succeed();
    }
}
