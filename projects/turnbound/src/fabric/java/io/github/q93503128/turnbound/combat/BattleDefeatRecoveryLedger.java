package io.github.q93503128.turnbound.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Server-thread-only settlement boundary. A terminal enemy victory may move
 * each authenticated participant exactly once, but a failed/unsafe move remains
 * retryable. Neither a client packet nor an arbitrary UUID can authorize loss.
 *
 * This class never grants loot, modifies inventory or forces vanilla death.
 */
public final class BattleDefeatRecoveryLedger {
    private final Map<Long, Set<UUID>> recovered = new HashMap<>();

    /**
     * @return true only when this invocation performed the participant's
     *         first successful recovery. False means nothing was settled.
     */
    public boolean recover(BattleSessionAuthority session, UUID participant, String currentWorld,
                           BooleanSupplier safeServerMove) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(participant, "participant");
        Objects.requireNonNull(safeServerMove, "safeServerMove");

        if (session.closed()
                || session.outcome() != BattleOutcome.ENEMY_VICTORY
                || !session.participants().contains(participant)
                || !session.worldId().equals(currentWorld)) {
            return false;
        }
        Set<UUID> done = recovered.get(session.id());
        if (done != null && done.contains(participant)) return false;

        // A missing or obstructed checkpoint is not an accomplished recovery;
        // do not silently consume the one permitted successful return.
        if (!safeServerMove.getAsBoolean()) return false;
        recovered.computeIfAbsent(session.id(), ignored -> new HashSet<>()).add(participant);
        return true;
    }

    public void discard(long sessionId) {
        recovered.remove(sessionId);
    }

    public void clear() {
        recovered.clear();
    }
}
