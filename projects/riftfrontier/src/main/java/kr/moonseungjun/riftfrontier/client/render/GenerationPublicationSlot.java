package kr.moonseungjun.riftfrontier.client.render;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free publication slot for asynchronously prepared client resources.
 *
 * <p>Every update cycle obtains an opaque ticket. Starting a newer cycle or invalidating the slot clears the
 * currently published value and makes every older ticket stale. A stale asynchronous completion therefore cannot
 * resurrect resources that belong to an earlier reload, world, or client lifecycle.</p>
 */
final class GenerationPublicationSlot<T> {
    private final AtomicReference<State<T>> state = new AtomicReference<>(new State<>(0L, Optional.empty()));

    Ticket beginUpdate() {
        while (true) {
            State<T> current = state.get();
            State<T> next = new State<>(nextGeneration(current.generation()), Optional.empty());
            if (state.compareAndSet(current, next)) {
                return new Ticket(this, next.generation());
            }
        }
    }

    boolean isCurrent(Ticket ticket) {
        requireOwner(ticket);
        return state.get().generation() == ticket.generation;
    }

    boolean publish(Ticket ticket, T value) {
        requireOwner(ticket);
        Objects.requireNonNull(value, "value");
        while (true) {
            State<T> current = state.get();
            if (current.generation() != ticket.generation) {
                return false;
            }
            State<T> next = new State<>(current.generation(), Optional.of(value));
            if (state.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    void invalidate() {
        while (true) {
            State<T> current = state.get();
            State<T> next = new State<>(nextGeneration(current.generation()), Optional.empty());
            if (state.compareAndSet(current, next)) {
                return;
            }
        }
    }

    Optional<T> current() {
        return state.get().value();
    }

    long generation() {
        return state.get().generation();
    }

    private void requireOwner(Ticket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        if (ticket.owner != this) {
            throw new IllegalArgumentException("publication ticket belongs to a different slot");
        }
    }

    private static long nextGeneration(long generation) {
        if (generation == Long.MAX_VALUE) {
            throw new IllegalStateException("publication generation exhausted");
        }
        return generation + 1L;
    }

    static final class Ticket {
        private final GenerationPublicationSlot<?> owner;
        private final long generation;

        private Ticket(GenerationPublicationSlot<?> owner, long generation) {
            this.owner = owner;
            this.generation = generation;
        }

        long generation() {
            return generation;
        }
    }

    private record State<T>(long generation, Optional<T> value) {
        private State {
            Objects.requireNonNull(value, "value");
        }
    }
}
