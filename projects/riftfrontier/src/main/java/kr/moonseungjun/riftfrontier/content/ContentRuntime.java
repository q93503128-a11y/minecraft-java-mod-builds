package kr.moonseungjun.riftfrontier.content;

import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfile;
import kr.moonseungjun.riftfrontier.combat.presentation.BossPresentationProfileValidator;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes validated content snapshots atomically. Failed reloads never replace the last known-good snapshot.
 */
public final class ContentRuntime {
    private static final AtomicReference<ContentRuntimeSnapshot> CURRENT = new AtomicReference<>();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static final ContentValidator VALIDATOR = new ContentValidator();
    private static final BossPresentationProfileValidator PRESENTATION_VALIDATOR = new BossPresentationProfileValidator();

    private ContentRuntime() {}

    public static Optional<ContentRuntimeSnapshot> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static ContentRuntimeSnapshot requireCurrent() {
        ContentRuntimeSnapshot snapshot = CURRENT.get();
        if (snapshot == null) throw new IllegalStateException("Riftfrontier content runtime has not been initialized");
        return snapshot;
    }

    public static ContentRuntimeSnapshot installValidated(ContentRegistry registry, List<String> packIds) {
        return installValidated(registry, packIds, List.of());
    }

    public static synchronized ContentRuntimeSnapshot installValidated(
        ContentRegistry registry,
        List<String> packIds,
        Collection<BossPresentationProfile> presentationProfiles
    ) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(packIds, "packIds");
        Objects.requireNonNull(presentationProfiles, "presentationProfiles");

        ContentValidator.Report report = VALIDATOR.validate(registry);
        if (report.hasErrors()) {
            throw new IllegalStateException("Refusing to publish invalid content snapshot:\n" + report.format());
        }

        List<BossPresentationProfile> presentations = List.copyOf(presentationProfiles);
        BossPresentationProfileValidator.Report presentationReport = PRESENTATION_VALIDATOR.validate(registry, presentations);
        if (presentationReport.hasErrors()) {
            String formatted = presentationReport.issues().stream()
                .map(issue -> issue.code() + " " + issue.source() + " - " + issue.message())
                .collect(java.util.stream.Collectors.joining("\n"));
            throw new IllegalStateException("Refusing to publish invalid boss presentation snapshot:\n" + formatted);
        }

        long generation = GENERATION.incrementAndGet();
        ContentRuntimeSnapshot snapshot = new ContentRuntimeSnapshot(generation, Instant.now(), packIds, registry, presentations);
        CURRENT.set(snapshot);
        return snapshot;
    }

    static synchronized void resetForTests() {
        CURRENT.set(null);
        GENERATION.set(0);
    }
}
