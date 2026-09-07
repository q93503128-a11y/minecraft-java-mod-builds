package kr.moonseungjun.riftfrontier.expedition;

import java.util.List;
import java.util.Objects;

/** Pure history calculations used by field-play evidence without depending on Minecraft runtime types. */
public final class ExpeditionReviewHistory {
    private ExpeditionReviewHistory() {}

    /**
     * Reconstructs Region 01 pressure at the beginning of a historical run.
     *
     * M2 advances region pressure exactly once for every successful EXTRACTED run and never for a failed
     * or active run. Therefore current pressure minus successful extractions at/after the target sequence
     * yields the pressure that authored the target encounter, even if later runs belong to other players.
     */
    public static int pressureAtStart(ExpeditionRun target, List<ExpeditionRun> history, int currentPressure) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(history, "history");
        if (currentPressure < 0) throw new IllegalArgumentException("currentPressure must be >= 0");
        if (history.stream().noneMatch(run -> run.sequence() == target.sequence())) {
            throw new IllegalArgumentException("Target expedition is not present in authoritative history: " + target.sequence());
        }

        long successfulAtOrAfter = history.stream()
            .filter(run -> run.sequence() >= target.sequence())
            .filter(run -> run.status() == ExpeditionRun.Status.EXTRACTED)
            .count();
        long reconstructed = (long) currentPressure - successfulAtOrAfter;
        if (reconstructed < 0L || reconstructed > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                "Region pressure/history drift: current=" + currentPressure + ", successfulAtOrAfter=" + successfulAtOrAfter
            );
        }
        return (int) reconstructed;
    }
}
