package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;

/** Read-only Minecraft adapter for evidence-driven Region 01 field-play review. */
public final class FieldPlayReview {
    private FieldPlayReview() {}

    public static FieldPlayReviewSnapshot capture(ServerPlayer player) {
        ServerLevel playerLevel = (ServerLevel) player.level();
        RiftfrontierWorldData world = RiftfrontierWorldData.get(playerLevel);
        ExpeditionRun run = world.expeditions().stream()
            .max(Comparator.comparingLong(ExpeditionRun::sequence))
            .orElseThrow(() -> new IllegalStateException("No expedition has been recorded yet"));

        boolean terminal = run.status().terminal();
        long now = playerLevel.getGameTime();
        long end = terminal ? run.endedGameTime() : now;
        long elapsedTicks = Math.max(0L, end - run.startedGameTime());

        // Region 01 pressure advances exactly once on successful extraction. For the latest run this
        // reconstructs the pressure used to create its encounter without adding persistence solely for
        // developer telemetry. Failed/non-terminal runs do not advance pressure.
        int pressureAtRun = run.status() == ExpeditionRun.Status.EXTRACTED
            ? Math.max(0, world.region01Pressure() - 1)
            : world.region01Pressure();
        Region01EncounterRuntime.EncounterPlan plan = Region01EncounterRuntime.planForPressure(pressureAtRun);

        int liveThreats = -1;
        if (!terminal) {
            ServerLevel overworld = playerLevel.getServer().overworld();
            liveThreats = Region01EncounterRuntime.liveThreatCount(
                overworld,
                ExpeditionGameplayService.technicalRegionCenter(),
                run.sequence()
            );
        }

        int recovered = run.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        String currentFingerprint = ContentRuntime.requireCurrent().fingerprint();
        return new FieldPlayReviewSnapshot(
            run.sequence(),
            run.status().serializedName(),
            run.endReason().serializedName(),
            terminal,
            elapsedTicks,
            recovered,
            pressureAtRun,
            plan.hunters(),
            plan.scouts(),
            plan.elites(),
            liveThreats,
            plan.hazardTicks(),
            plan.hazardAmplifier(),
            world.securedRegion01Salvage(),
            world.expeditionSupply(),
            world.region01PreparationSupplyCost(),
            currentFingerprint.equals(run.contentFingerprint()),
            run.contentFingerprint()
        );
    }
}
