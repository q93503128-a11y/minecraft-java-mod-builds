package dev.moonseungjun.openworldrpg.camp;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.phys.Vec3;

public final class R01CampPlacementAuthority {
    private R01CampPlacementAuthority() {
    }

    public static Decision evaluate(
            String ownerUuid,
            Candidate candidate,
            PlacementProbe probe,
            R01CampWorldState worldState
    ) {
        java.util.UUID.fromString(ownerUuid);
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(probe, "probe");
        Objects.requireNonNull(worldState, "worldState");

        if (!candidate.equals(probe.candidate())) {
            return Decision.rejected(Status.PROBE_CANDIDATE_MISMATCH);
        }
        if (!probe.productionSpatialBindingAccepted()) {
            return Decision.rejected(Status.SPATIAL_NOT_PRODUCTION);
        }
        if (probe.forbiddenVolumeIntersection()) {
            return Decision.rejected(Status.FORBIDDEN_VOLUME);
        }
        if (probe.footprintIntersectsLiquid()) {
            return Decision.rejected(Status.LIQUID_INTERSECTION);
        }
        if (!probe.functionalFootprintClear()) {
            return Decision.rejected(Status.FUNCTIONAL_FOOTPRINT_BLOCKED);
        }
        if (probe.clearApproachWidth() + 1.0e-9
                < R01CampRules.MIN_CLEAR_APPROACH_WIDTH) {
            return Decision.rejected(Status.APPROACH_BLOCKED);
        }

        long stableCount = probe.supportSamples().stream()
                .filter(SupportSample::stable)
                .count();
        if (stableCount < R01CampRules.MIN_STABLE_SUPPORT_SAMPLES) {
            return Decision.rejected(Status.INSUFFICIENT_SUPPORT);
        }

        double minHeight = Double.POSITIVE_INFINITY;
        double maxHeight = Double.NEGATIVE_INFINITY;
        for (SupportSample sample : probe.supportSamples()) {
            if (sample.stable()) {
                minHeight = Math.min(minHeight, sample.supportHeight());
                maxHeight = Math.max(maxHeight, sample.supportHeight());
            }
        }
        double variance = maxHeight - minHeight;
        if (variance > R01CampRules.MAX_SUPPORT_HEIGHT_VARIANCE + 1.0e-9) {
            return Decision.rejected(Status.TOO_UNEVEN);
        }

        if (probe.nearestActivatedShrineOrServiceDistance() + 1.0e-9
                < R01CampRules.MIN_SHRINE_OR_SERVICE_DISTANCE) {
            return Decision.rejected(Status.TOO_CLOSE_TO_SHRINE_OR_SERVICE);
        }
        if (probe.nearestActiveBossArenaBoundaryDistance() + 1.0e-9
                < R01CampRules.MIN_BOSS_ARENA_DISTANCE) {
            return Decision.rejected(Status.TOO_CLOSE_TO_BOSS_ARENA);
        }

        double nearestOtherCamp = Double.POSITIVE_INFINITY;
        for (R01CampWorldState.ActiveCamp camp : worldState.activeCamps().values()) {
            if (!ownerUuid.equals(camp.ownerUuid())) {
                nearestOtherCamp = Math.min(
                        nearestOtherCamp,
                        candidate.anchor().distanceTo(camp.anchor())
                );
            }
        }
        if (nearestOtherCamp + 1.0e-9 < R01CampRules.MIN_OTHER_CAMP_DISTANCE) {
            return Decision.rejected(Status.TOO_CLOSE_TO_OTHER_CAMP);
        }

        return new Decision(Status.ALLOWED, stableCount, variance, nearestOtherCamp);
    }

    public enum Status {
        ALLOWED,
        PROBE_CANDIDATE_MISMATCH,
        SPATIAL_NOT_PRODUCTION,
        FORBIDDEN_VOLUME,
        LIQUID_INTERSECTION,
        FUNCTIONAL_FOOTPRINT_BLOCKED,
        APPROACH_BLOCKED,
        INSUFFICIENT_SUPPORT,
        TOO_UNEVEN,
        TOO_CLOSE_TO_SHRINE_OR_SERVICE,
        TOO_CLOSE_TO_BOSS_ARENA,
        TOO_CLOSE_TO_OTHER_CAMP
    }

    public record Decision(
            Status status,
            long stableSupportSamples,
            double supportHeightVariance,
            double nearestOtherCampDistance
    ) {
        public Decision {
            Objects.requireNonNull(status, "status");
            if (stableSupportSamples < 0L
                    || !Double.isFinite(supportHeightVariance)
                    || supportHeightVariance < 0.0
                    || Double.isNaN(nearestOtherCampDistance)
                    || nearestOtherCampDistance < 0.0) {
                throw new IllegalArgumentException("Invalid Camp placement decision metrics.");
            }
        }

        public boolean allowed() {
            return status == Status.ALLOWED;
        }

        static Decision rejected(Status status) {
            return new Decision(status, 0L, 0.0, Double.POSITIVE_INFINITY);
        }
    }

    public record Candidate(Vec3 anchor, double yawDegrees) {
        public Candidate {
            anchor = Objects.requireNonNull(anchor, "anchor");
            if (!Double.isFinite(anchor.x()) || !Double.isFinite(anchor.y())
                    || !Double.isFinite(anchor.z()) || !Double.isFinite(yawDegrees)) {
                throw new IllegalArgumentException("Camp candidate coordinates must be finite.");
            }
            yawDegrees %= 360.0;
            if (yawDegrees < 0.0) {
                yawDegrees += 360.0;
            }
        }
    }

    public record SupportSample(boolean stable, double supportHeight) {
        public SupportSample {
            if (stable && !Double.isFinite(supportHeight)) {
                throw new IllegalArgumentException("Stable support needs a finite height.");
            }
        }

        public static SupportSample unsupported() {
            return new SupportSample(false, 0.0);
        }
    }

    public record PlacementProbe(
            Candidate candidate,
            List<SupportSample> supportSamples,
            boolean footprintIntersectsLiquid,
            boolean functionalFootprintClear,
            boolean forbiddenVolumeIntersection,
            double nearestActivatedShrineOrServiceDistance,
            double nearestActiveBossArenaBoundaryDistance,
            double clearApproachWidth,
            boolean productionSpatialBindingAccepted
    ) {
        public PlacementProbe {
            candidate = Objects.requireNonNull(candidate, "candidate");
            supportSamples = List.copyOf(Objects.requireNonNull(supportSamples, "supportSamples"));
            if (supportSamples.size() != R01CampRules.SUPPORT_SAMPLE_COUNT) {
                throw new IllegalArgumentException("R01 Camp requires exactly 25 support samples.");
            }
            if (Double.isNaN(nearestActivatedShrineOrServiceDistance)
                    || nearestActivatedShrineOrServiceDistance < 0.0
                    || Double.isNaN(nearestActiveBossArenaBoundaryDistance)
                    || nearestActiveBossArenaBoundaryDistance < 0.0
                    || !Double.isFinite(clearApproachWidth)
                    || clearApproachWidth < 0.0) {
                throw new IllegalArgumentException("Invalid Camp placement probe.");
            }
        }
    }
}
