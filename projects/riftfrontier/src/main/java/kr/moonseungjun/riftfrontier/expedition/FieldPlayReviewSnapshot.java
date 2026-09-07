package kr.moonseungjun.riftfrontier.expedition;

import java.util.Objects;

/**
 * Immutable, machine-readable observation emitted for manual Region 01 field-play review.
 *
 * This record is deliberately Minecraft-free so review formatting and invariants can be regression
 * tested without booting a game runtime. It is evidence, not a balance verdict: no field-play gate is
 * considered complete merely because a snapshot can be captured.
 */
public record FieldPlayReviewSnapshot(
    long sequence,
    String status,
    String endReason,
    boolean terminal,
    long elapsedTicks,
    int recoveredSalvage,
    int pressureAtRun,
    int plannedHunters,
    int plannedScouts,
    int plannedElites,
    int liveThreats,
    int hazardTicks,
    int hazardAmplifier,
    int hubSalvage,
    int expeditionSupply,
    int nextSupplyCost,
    boolean contentFingerprintCurrent,
    String contentFingerprint
) {
    public FieldPlayReviewSnapshot {
        if (sequence <= 0L) throw new IllegalArgumentException("sequence must be > 0");
        status = Objects.requireNonNull(status, "status");
        if (status.isBlank()) throw new IllegalArgumentException("status cannot be blank");
        endReason = Objects.requireNonNull(endReason, "endReason");
        if (endReason.isBlank()) throw new IllegalArgumentException("endReason cannot be blank");
        if (!terminal && !"none".equals(endReason)) {
            throw new IllegalArgumentException("active snapshots must use endReason=none");
        }
        if (elapsedTicks < 0L) throw new IllegalArgumentException("elapsedTicks must be >= 0");
        if (recoveredSalvage < 0) throw new IllegalArgumentException("recoveredSalvage must be >= 0");
        if (pressureAtRun < 0) throw new IllegalArgumentException("pressureAtRun must be >= 0");
        if (plannedHunters < 0 || plannedScouts < 0 || plannedElites < 0) {
            throw new IllegalArgumentException("planned threat counts must be >= 0");
        }
        if (terminal && liveThreats != -1) throw new IllegalArgumentException("terminal snapshots must use liveThreats=-1");
        if (!terminal && liveThreats < 0) throw new IllegalArgumentException("active snapshots require a live threat count");
        if (hazardTicks < 0 || hazardAmplifier < 0) throw new IllegalArgumentException("hazard values must be >= 0");
        if (hubSalvage < 0 || expeditionSupply < 0 || nextSupplyCost < 0) {
            throw new IllegalArgumentException("economy values must be >= 0");
        }
        contentFingerprint = Objects.requireNonNull(contentFingerprint, "contentFingerprint");
        if (contentFingerprint.isBlank()) throw new IllegalArgumentException("contentFingerprint cannot be blank");
    }

    public int plannedThreats() {
        return plannedHunters + plannedScouts + plannedElites;
    }

    /** Stable one-line format intended for screenshots, logs and issue reports. */
    public String reportLine() {
        String threatObservation = terminal ? "terminal" : Integer.toString(liveThreats);
        return "run=" + sequence
            + ";status=" + status
            + ";endReason=" + endReason
            + ";elapsedTicks=" + elapsedTicks
            + ";salvage=" + recoveredSalvage
            + ";pressure=" + pressureAtRun
            + ";plan=" + plannedThreats() + "[hunter=" + plannedHunters + ",scout=" + plannedScouts + ",elite=" + plannedElites + "]"
            + ";liveThreats=" + threatObservation
            + ";hazard=" + hazardTicks + "t@" + (hazardAmplifier + 1)
            + ";hubSalvage=" + hubSalvage
            + ";supply=" + expeditionSupply
            + ";nextCost=" + nextSupplyCost
            + ";content=" + (contentFingerprintCurrent ? "current" : "stale")
            + ";fingerprint=" + abbreviate(contentFingerprint);
    }

    private static String abbreviate(String fingerprint) {
        return fingerprint.length() <= 12 ? fingerprint : fingerprint.substring(0, 12);
    }
}
