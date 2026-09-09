package kr.moonseungjun.riftfrontier.combat.presentation;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic client-side publication boundary for physically validated boss presentation assets.
 *
 * <p>A resource-pack reload validates the complete selected manifest against the candidate client resource
 * manager and publishes one complete snapshot only. A rejected resource pack fails closed by replacing any
 * previously-ready snapshot with an inactive snapshot for the attempted content generation; render code must
 * never keep presenting resources that the active pack no longer proves exist. Content generations may be
 * revalidated at the same value for resource-pack reloads, but they may never move backwards.</p>
 */
public final class BossPresentationClientAssetRuntime {
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(Snapshot.inactive(0));

    private BossPresentationClientAssetRuntime() {}

    public static Snapshot current() {
        return CURRENT.get();
    }

    /**
     * Revalidates and atomically publishes one complete client asset snapshot.
     *
     * @throws ResourceValidationException when a selected manifest references any missing physical resource;
     *         the attempted generation is published inactive before the exception is raised.
     * @throws StaleContentGenerationException when an older content generation attempts to replace a newer one.
     */
    public static synchronized Snapshot reload(
        long contentGeneration,
        Optional<BossPresentationAssetManifest> manifest,
        BossPresentationAssetManifest.ResourceProbe probe
    ) {
        if (contentGeneration < 0) throw new IllegalArgumentException("contentGeneration must be >= 0");
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(probe, "probe");

        Snapshot previous = CURRENT.get();
        if (contentGeneration < previous.contentGeneration()) {
            throw new StaleContentGenerationException(contentGeneration, previous.contentGeneration());
        }

        Snapshot candidate;
        if (manifest.isEmpty()) {
            candidate = Snapshot.inactive(contentGeneration);
        } else {
            BossPresentationAssetSelection.Result result = BossPresentationAssetSelection.validate(manifest.get(), probe);
            if (!result.ready()) {
                CURRENT.set(Snapshot.inactive(contentGeneration));
                throw new ResourceValidationException(result.report());
            }
            candidate = new Snapshot(contentGeneration, result.selection());
        }

        CURRENT.set(candidate);
        return candidate;
    }

    static synchronized void resetForTests() {
        CURRENT.set(Snapshot.inactive(0));
    }

    public record Snapshot(
        long contentGeneration,
        Optional<BossPresentationAssetSelection> selection
    ) {
        public Snapshot {
            if (contentGeneration < 0) throw new IllegalArgumentException("contentGeneration must be >= 0");
            selection = Objects.requireNonNull(selection, "selection");
        }

        public static Snapshot inactive(long contentGeneration) {
            return new Snapshot(contentGeneration, Optional.empty());
        }

        public boolean ready() {
            return selection.isPresent();
        }
    }

    public static final class ResourceValidationException extends IllegalStateException {
        private final BossPresentationAssetManifest.Report report;

        public ResourceValidationException(BossPresentationAssetManifest.Report report) {
            super("boss presentation client resources failed validation: " + summarize(report));
            this.report = Objects.requireNonNull(report, "report");
            if (!report.hasErrors()) {
                throw new IllegalArgumentException("resource validation exception requires at least one issue");
            }
        }

        public BossPresentationAssetManifest.Report report() {
            return report;
        }

        private static String summarize(BossPresentationAssetManifest.Report report) {
            Objects.requireNonNull(report, "report");
            return report.issues().stream()
                .map(issue -> issue.code() + "[" + issue.logicalKey() + "]: " + issue.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("unknown validation failure");
        }
    }

    public static final class StaleContentGenerationException extends IllegalStateException {
        private final long attemptedGeneration;
        private final long currentGeneration;

        private StaleContentGenerationException(long attemptedGeneration, long currentGeneration) {
            super("stale boss presentation content generation " + attemptedGeneration
                + " cannot replace current generation " + currentGeneration);
            this.attemptedGeneration = attemptedGeneration;
            this.currentGeneration = currentGeneration;
        }

        public long attemptedGeneration() {
            return attemptedGeneration;
        }

        public long currentGeneration() {
            return currentGeneration;
        }
    }
}
