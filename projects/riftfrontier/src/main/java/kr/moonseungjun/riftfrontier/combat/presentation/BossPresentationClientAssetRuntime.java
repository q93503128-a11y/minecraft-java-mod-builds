package kr.moonseungjun.riftfrontier.combat.presentation;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic client-side publication boundary for physically validated boss presentation assets.
 *
 * <p>A resource-pack reload first validates the complete selected manifest against the candidate client
 * resource manager. Publication happens with one atomic swap only after every selected resource is present.
 * Failed validation leaves the previously published selection untouched so render code can never observe a
 * half-promoted manifest. A content snapshot with no selected production manifest is a valid inactive state.</p>
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
     *         the previously published snapshot is preserved.
     */
    public static Snapshot reload(
        long contentGeneration,
        Optional<BossPresentationAssetManifest> manifest,
        BossPresentationAssetManifest.ResourceProbe probe
    ) {
        if (contentGeneration < 0) throw new IllegalArgumentException("contentGeneration must be >= 0");
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(probe, "probe");

        Snapshot candidate;
        if (manifest.isEmpty()) {
            candidate = Snapshot.inactive(contentGeneration);
        } else {
            BossPresentationAssetSelection.Result result = BossPresentationAssetSelection.validate(manifest.get(), probe);
            if (!result.ready()) {
                throw new ResourceValidationException(result.report());
            }
            candidate = new Snapshot(contentGeneration, result.selection());
        }

        CURRENT.set(candidate);
        return candidate;
    }

    static void resetForTests() {
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
}
