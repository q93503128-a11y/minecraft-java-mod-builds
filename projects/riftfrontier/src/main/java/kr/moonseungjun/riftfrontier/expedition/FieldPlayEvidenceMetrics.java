package kr.moonseungjun.riftfrontier.expedition;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Pure read-only analysis of one persisted expedition evidence trail.
 *
 * Metrics are intentionally descriptive rather than evaluative. They expose comparable pacing and
 * pressure observations for manual M2 field-play without declaring combat feel, fairness, or a pass/fail
 * result on behalf of the reviewer.
 */
public record FieldPlayEvidenceMetrics(
    long sequence,
    int checkpoints,
    int salvageCheckpoints,
    OptionalLong firstSalvageElapsedTicks,
    OptionalLong preExtractionElapsedTicks,
    OptionalLong terminalElapsedTicks,
    OptionalLong minSalvageIntervalTicks,
    OptionalLong maxSalvageIntervalTicks,
    OptionalInt minObservedLiveThreats,
    OptionalInt maxObservedLiveThreats,
    OptionalInt terminalObservedLiveThreats,
    OptionalInt finalObservedSalvage,
    String terminalStage,
    String endReason
) {
    public static FieldPlayEvidenceMetrics from(ExpeditionRun run) {
        List<ExpeditionEvidenceCheckpoint> trail = run.evidenceTrail();
        if (trail.isEmpty()) {
            return new FieldPlayEvidenceMetrics(
                run.sequence(), 0, 0,
                OptionalLong.empty(), OptionalLong.empty(), optionalTerminalElapsed(run),
                OptionalLong.empty(), OptionalLong.empty(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                "legacy-unavailable", run.endReason().serializedName()
            );
        }

        List<ExpeditionEvidenceCheckpoint> salvage = trail.stream()
            .filter(checkpoint -> checkpoint.stage() == ExpeditionEvidenceCheckpoint.Stage.SALVAGE_RECOVERED)
            .toList();

        OptionalLong firstSalvage = salvage.isEmpty()
            ? OptionalLong.empty()
            : OptionalLong.of(elapsed(run, salvage.getFirst()));

        OptionalLong preExtraction = trail.stream()
            .filter(checkpoint -> checkpoint.stage() == ExpeditionEvidenceCheckpoint.Stage.PRE_EXTRACTION)
            .mapToLong(checkpoint -> elapsed(run, checkpoint))
            .findFirst();

        LongSummaryStatistics intervals = new LongSummaryStatistics();
        for (int i = 1; i < salvage.size(); i++) {
            intervals.accept(salvage.get(i).gameTime() - salvage.get(i - 1).gameTime());
        }

        IntSummaryStatistics threats = trail.stream()
            .mapToInt(ExpeditionEvidenceCheckpoint::liveThreats)
            .filter(value -> value >= 0)
            .summaryStatistics();

        ExpeditionEvidenceCheckpoint last = trail.getLast();
        OptionalInt terminalThreats = last.stage().terminal() && last.liveThreats() >= 0
            ? OptionalInt.of(last.liveThreats())
            : OptionalInt.empty();

        return new FieldPlayEvidenceMetrics(
            run.sequence(),
            trail.size(),
            salvage.size(),
            firstSalvage,
            preExtraction,
            optionalTerminalElapsed(run),
            intervals.getCount() == 0 ? OptionalLong.empty() : OptionalLong.of(intervals.getMin()),
            intervals.getCount() == 0 ? OptionalLong.empty() : OptionalLong.of(intervals.getMax()),
            threats.getCount() == 0 ? OptionalInt.empty() : OptionalInt.of(threats.getMin()),
            threats.getCount() == 0 ? OptionalInt.empty() : OptionalInt.of(threats.getMax()),
            terminalThreats,
            OptionalInt.of(last.recoveredSalvage()),
            last.stage().terminal() ? last.stage().serializedName() : "active",
            run.endReason().serializedName()
        );
    }

    private static OptionalLong optionalTerminalElapsed(ExpeditionRun run) {
        return run.status().terminal()
            ? OptionalLong.of(Math.max(0L, run.endedGameTime() - run.startedGameTime()))
            : OptionalLong.empty();
    }

    private static long elapsed(ExpeditionRun run, ExpeditionEvidenceCheckpoint checkpoint) {
        return Math.max(0L, checkpoint.gameTime() - run.startedGameTime());
    }

    public String reportLine() {
        return "run=" + sequence
            + ";checkpoints=" + checkpoints
            + ";salvageCheckpoints=" + salvageCheckpoints
            + ";firstSalvageTicks=" + render(firstSalvageElapsedTicks)
            + ";preExtractionTicks=" + render(preExtractionElapsedTicks)
            + ";terminalTicks=" + render(terminalElapsedTicks)
            + ";salvageIntervalMinTicks=" + render(minSalvageIntervalTicks)
            + ";salvageIntervalMaxTicks=" + render(maxSalvageIntervalTicks)
            + ";liveThreatsMin=" + render(minObservedLiveThreats)
            + ";liveThreatsMax=" + render(maxObservedLiveThreats)
            + ";terminalLiveThreats=" + render(terminalObservedLiveThreats)
            + ";finalSalvage=" + render(finalObservedSalvage)
            + ";terminalStage=" + terminalStage
            + ";endReason=" + endReason;
    }

    private static String render(OptionalLong value) {
        return value.isPresent() ? Long.toString(value.getAsLong()) : "unavailable";
    }

    private static String render(OptionalInt value) {
        return value.isPresent() ? Integer.toString(value.getAsInt()) : "unavailable";
    }
}
