package kr.moonseungjun.riftfrontier.expedition;

import java.util.Objects;

/**
 * Persisted, bounded observation from one authoritative expedition lifecycle edge.
 *
 * This is evidence for manual field-play review, not a gameplay rule and not a substitute for human
 * judgement of combat feel. liveThreats=-1 is reserved for edges where the previous process/runtime
 * can no longer prove the pre-exit threat count (currently restart reconciliation).
 */
public record ExpeditionEvidenceCheckpoint(
    Stage stage,
    long gameTime,
    int recoveredSalvage,
    int liveThreats,
    int hubSalvage,
    int expeditionSupply,
    int regionPressure
) {
    public enum Stage {
        DEPLOYED("deployed"),
        SALVAGE_RECOVERED("salvage_recovered"),
        PRE_EXTRACTION("pre_extraction"),
        EXTRACTED("extracted"),
        FAILED("failed");

        private final String serializedName;

        Stage(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public boolean terminal() {
            return this == EXTRACTED || this == FAILED;
        }

        public static Stage parse(String value) {
            for (Stage stage : values()) {
                if (stage.serializedName.equals(value)) return stage;
            }
            throw new IllegalArgumentException("Unknown expedition evidence stage '" + value + "'");
        }
    }

    public ExpeditionEvidenceCheckpoint {
        Objects.requireNonNull(stage, "stage");
        if (gameTime < 0L) throw new IllegalArgumentException("gameTime must be >= 0");
        if (recoveredSalvage < 0) throw new IllegalArgumentException("recoveredSalvage must be >= 0");
        if (liveThreats < -1) throw new IllegalArgumentException("liveThreats must be -1 or >= 0");
        if (!stage.terminal() && liveThreats < 0) {
            throw new IllegalArgumentException("non-terminal evidence requires a concrete live threat count");
        }
        if (hubSalvage < 0 || expeditionSupply < 0 || regionPressure < 0) {
            throw new IllegalArgumentException("world-state evidence values must be >= 0");
        }
    }

    public String reportLine(long sequence, long startedGameTime) {
        if (sequence <= 0L) throw new IllegalArgumentException("sequence must be > 0");
        long elapsed = Math.max(0L, gameTime - startedGameTime);
        return "run=" + sequence
            + ";stage=" + stage.serializedName()
            + ";gameTime=" + gameTime
            + ";elapsedTicks=" + elapsed
            + ";salvage=" + recoveredSalvage
            + ";liveThreats=" + (liveThreats < 0 ? "unavailable" : liveThreats)
            + ";hubSalvage=" + hubSalvage
            + ";supply=" + expeditionSupply
            + ";pressure=" + regionPressure;
    }
}
