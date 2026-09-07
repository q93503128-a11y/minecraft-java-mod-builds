package kr.moonseungjun.riftfrontier.expedition;

import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

/** Read-only Minecraft adapter for evidence-driven Region 01 field-play review. */
public final class FieldPlayReview {
    private FieldPlayReview() {}

    public static FieldPlayReviewSnapshot capture(ServerPlayer player) {
        ServerLevel playerLevel = (ServerLevel) player.level();
        RiftfrontierWorldData world = RiftfrontierWorldData.get(playerLevel);
        ExpeditionRun run = latestFor(player, world);

        boolean terminal = run.status().terminal();
        long now = playerLevel.getGameTime();
        long end = terminal ? run.endedGameTime() : now;
        long elapsedTicks = Math.max(0L, end - run.startedGameTime());

        ExpeditionStartContext context;
        String contextSource;
        if (run.startContext().isPresent()) {
            context = run.startContext().orElseThrow();
            contextSource = "persisted";
        } else {
            int reconstructedPressure = ExpeditionReviewHistory.pressureAtStart(run, world.expeditions(), world.region01Pressure());
            Region01EncounterRuntime.EncounterPlan reconstructedPlan = Region01EncounterRuntime.planForPressure(reconstructedPressure);
            context = new ExpeditionStartContext(
                reconstructedPressure,
                Math.min(3, 1 + (reconstructedPressure / 2)),
                reconstructedPlan.hunters(),
                reconstructedPlan.scouts(),
                reconstructedPlan.elites(),
                reconstructedPlan.hazardTicks(),
                reconstructedPlan.hazardAmplifier()
            );
            contextSource = "legacy-reconstructed";
        }

        int liveThreats = -1;
        if (!terminal) {
            ServerLevel overworld = playerLevel.getServer().overworld();
            liveThreats = Region01EncounterRuntime.liveThreatCount(overworld, ExpeditionGameplayService.technicalRegionCenter(), run.sequence());
        }

        int recovered = run.recoveredResources().getOrDefault(ExpeditionGameplayService.RESOURCE_ID, 0);
        String currentFingerprint = ContentRuntime.requireCurrent().fingerprint();
        return new FieldPlayReviewSnapshot(
            run.sequence(),
            run.ownerId().map(Object::toString).orElse("legacy-unowned"),
            run.status().serializedName(),
            run.endReason().serializedName(),
            terminal,
            elapsedTicks,
            recovered,
            context.regionPressure(),
            context.plannedHunters(),
            context.plannedScouts(),
            context.plannedElites(),
            liveThreats,
            context.hazardTicks(),
            context.hazardAmplifier(),
            world.securedRegion01Salvage(),
            world.expeditionSupply(),
            world.region01PreparationSupplyCost(),
            contextSource,
            currentFingerprint.equals(run.contentFingerprint()),
            run.contentFingerprint()
        );
    }

    /**
     * Returns the bounded persisted lifecycle evidence for the caller's latest run. Empty legacy trails
     * remain explicitly labelled instead of being reconstructed as if they were observed facts.
     */
    public static List<String> trail(ServerPlayer player) {
        RiftfrontierWorldData world = RiftfrontierWorldData.get((ServerLevel) player.level());
        ExpeditionRun run = latestFor(player, world);
        if (run.evidenceTrail().isEmpty()) {
            return List.of("run=" + run.sequence() + ";evidence=legacy-unavailable");
        }
        return run.evidenceTrail().stream()
            .map(checkpoint -> checkpoint.reportLine(run.sequence(), run.startedGameTime()))
            .toList();
    }

    private static ExpeditionRun latestFor(ServerPlayer player, RiftfrontierWorldData world) {
        return world.expeditions().stream()
            .filter(value -> value.ownerId().isEmpty() || value.ownedBy(player.getUUID()))
            .max(Comparator.comparingLong(ExpeditionRun::sequence))
            .orElseThrow(() -> new IllegalStateException("No expedition has been recorded for this player yet"));
    }
}
