package kr.moonseungjun.riftfrontier.content;

import java.time.Instant;
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

    private ContentRuntime() {}

    public static Optional<ContentRuntimeSnapshot> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static ContentRuntimeSnapshot requireCurrent() {
        ContentRuntimeSnapshot snapshot = CURRENT.get();
        if (snapshot == null) throw new IllegalStateException("Riftfrontier content runtime has not been initialized");
        return snapshot;
    }

    public static synchronized ContentRuntimeSnapshot installValidated(ContentRegistry registry, List<String> packIds) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(packIds, "packIds");

        ContentValidator.Report report = VALIDATOR.validate(registry);
        if (report.hasErrors()) {
            throw new IllegalStateException("Refusing to publish invalid content snapshot:\n" + report.format());
        }

        long generation = GENERATION.incrementAndGet();
        ContentRuntimeSnapshot snapshot = new ContentRuntimeSnapshot(generation, Instant.now(), packIds, registry);
        CURRENT.set(snapshot);
        return snapshot;
    }

    static synchronized void resetForTests() {
        CURRENT.set(null);
        GENERATION.set(0);
    }
}
