package dev.moonseungjun.openworldrpg.mount;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical Trail Stag summon/dismount search order without guessed world geometry.
 *
 * <p>The live binder supplies collision/support results using the accepted model dimensions. This
 * authority only owns authored ordering and exact support rules.</p>
 */
public final class TrailStagPlacementAuthority {
    public static final double MAX_SUPPORT_HEIGHT_VARIANCE = 0.75;

    public static final List<SummonCandidate> SUMMON_ORDER = List.of(
            new SummonCandidate(RelativeDirection.BEHIND_RIGHT, 2.5),
            new SummonCandidate(RelativeDirection.BEHIND_LEFT, 2.5),
            new SummonCandidate(RelativeDirection.BEHIND, 2.5),
            new SummonCandidate(RelativeDirection.RIGHT, 2.5),
            new SummonCandidate(RelativeDirection.LEFT, 2.5),
            new SummonCandidate(RelativeDirection.BEHIND_RIGHT, 3.5),
            new SummonCandidate(RelativeDirection.BEHIND_LEFT, 3.5),
            new SummonCandidate(RelativeDirection.BEHIND, 3.5)
    );

    public static final List<RelativeDirection> DISMOUNT_ORDER = List.of(
            RelativeDirection.LEFT,
            RelativeDirection.RIGHT,
            RelativeDirection.BEHIND_LEFT,
            RelativeDirection.BEHIND_RIGHT,
            RelativeDirection.BEHIND
    );

    private TrailStagPlacementAuthority() {
    }

    public static Optional<SummonCandidate> firstLegalSummon(List<SummonProbe> probes) {
        Objects.requireNonNull(probes, "probes");
        if (probes.size() != SUMMON_ORDER.size()) {
            throw new IllegalArgumentException("Trail Stag summon probe count must match authored search order.");
        }
        for (int i = 0; i < SUMMON_ORDER.size(); i++) {
            SummonProbe probe = Objects.requireNonNull(probes.get(i), "probe");
            if (!probe.candidate().equals(SUMMON_ORDER.get(i))) {
                throw new IllegalArgumentException("Trail Stag summon probes must remain in authored order.");
            }
            if (probe.legal()) {
                return Optional.of(probe.candidate());
            }
        }
        return Optional.empty();
    }

    public enum RelativeDirection {
        LEFT,
        RIGHT,
        BEHIND_LEFT,
        BEHIND_RIGHT,
        BEHIND
    }

    public record SummonCandidate(RelativeDirection direction, double distanceBlocks) {
        public SummonCandidate {
            Objects.requireNonNull(direction, "direction");
            if (!Double.isFinite(distanceBlocks) || distanceBlocks <= 0.0) {
                throw new IllegalArgumentException("Summon distance must be positive and finite.");
            }
        }
    }

    public record SummonProbe(
            SummonCandidate candidate,
            boolean collisionVolumeClear,
            boolean stableNonLiquidSupport,
            double supportHeightVariance,
            boolean legalMountVolume,
            boolean entityCollisionClear
    ) {
        public SummonProbe {
            Objects.requireNonNull(candidate, "candidate");
            if (!Double.isFinite(supportHeightVariance) || supportHeightVariance < 0.0) {
                throw new IllegalArgumentException("Support height variance must be finite and non-negative.");
            }
        }

        public boolean legal() {
            return collisionVolumeClear
                    && stableNonLiquidSupport
                    && supportHeightVariance <= MAX_SUPPORT_HEIGHT_VARIANCE
                    && legalMountVolume
                    && entityCollisionClear;
        }
    }

    public record DismountProbe(
            RelativeDirection direction,
            boolean playerCollisionColumnClear,
            boolean stableSupport
    ) {
        public DismountProbe {
            Objects.requireNonNull(direction, "direction");
        }

        public boolean legal() {
            return playerCollisionColumnClear && stableSupport;
        }
    }

    public static Optional<RelativeDirection> firstLegalDismount(List<DismountProbe> probes) {
        Objects.requireNonNull(probes, "probes");
        if (probes.size() != DISMOUNT_ORDER.size()) {
            throw new IllegalArgumentException("Trail Stag dismount probe count must match authored search order.");
        }
        for (int i = 0; i < DISMOUNT_ORDER.size(); i++) {
            DismountProbe probe = Objects.requireNonNull(probes.get(i), "probe");
            if (probe.direction() != DISMOUNT_ORDER.get(i)) {
                throw new IllegalArgumentException("Trail Stag dismount probes must remain in authored order.");
            }
            if (probe.legal()) {
                return Optional.of(probe.direction());
            }
        }
        return Optional.empty();
    }
}
