package dev.moonseungjun.openworldrpg.combat.state;

import java.util.Objects;

/**
 * Server-owned commitment/reaction window for one player.
 *
 * <p>This state does not animate anything. It owns only the action legality that presentation must
 * follow: action end, movement multiplier, dodge-cancel opening and hard reaction lock.</p>
 */
public final class PlayerActionRuntimeState {
    private ActiveWindow active;

    public boolean canStartAction(long nowTick) {
        refresh(nowTick);
        return active == null;
    }

    public BeginResult beginAction(
            String actionId,
            long nowTick,
            int totalTicks,
            int dodgeCancelOffsetTicks,
            double movementMultiplier
    ) {
        Objects.requireNonNull(actionId, "actionId");
        if (actionId.isBlank()
                || totalTicks <= 0
                || dodgeCancelOffsetTicks < 0
                || dodgeCancelOffsetTicks > totalTicks
                || !Double.isFinite(movementMultiplier)
                || movementMultiplier <= 0.0
                || movementMultiplier > 1.0) {
            throw new IllegalArgumentException(
                    "Invalid project action commitment."
            );
        }
        refresh(nowTick);
        if (active != null) {
            return BeginResult.rejected();
        }

        active = new ActiveWindow(
                actionId,
                WindowKind.ACTION,
                nowTick,
                Math.addExact(
                        nowTick,
                        dodgeCancelOffsetTicks
                ),
                Math.addExact(nowTick, totalTicks),
                movementMultiplier
        );
        return new BeginResult(
                true,
                active.dodgeCancelAtTick(),
                active.endTick()
        );
    }

    public ReactionResult applyReaction(
            String reactionId,
            long nowTick,
            int durationTicks
    ) {
        Objects.requireNonNull(reactionId, "reactionId");
        if (reactionId.isBlank() || durationTicks <= 0) {
            throw new IllegalArgumentException(
                    "Reaction id/duration must be valid."
            );
        }

        long endTick = Math.addExact(nowTick, durationTicks);
        active = new ActiveWindow(
                reactionId,
                WindowKind.REACTION,
                nowTick,
                endTick,
                endTick,
                1.0
        );
        return new ReactionResult(
                true,
                durationTicks,
                endTick
        );
    }

    public boolean canDodgeCancel(long nowTick) {
        refresh(nowTick);
        if (active == null) {
            return true;
        }
        return active.kind() == WindowKind.ACTION
                && nowTick >= active.dodgeCancelAtTick();
    }

    public boolean commitDodgeCancel(long nowTick) {
        refresh(nowTick);
        if (active == null) {
            return true;
        }
        if (active.kind() != WindowKind.ACTION
                || nowTick < active.dodgeCancelAtTick()) {
            return false;
        }
        active = null;
        return true;
    }

    public boolean basicAttackAllowed(long nowTick) {
        refresh(nowTick);
        return active == null;
    }

    public boolean hardReactionActive(long nowTick) {
        refresh(nowTick);
        return active != null
                && active.kind() == WindowKind.REACTION;
    }

    public double movementMultiplier(long nowTick) {
        refresh(nowTick);
        return active != null
                && active.kind() == WindowKind.ACTION
                ? active.movementMultiplier()
                : 1.0;
    }

    public Snapshot snapshot(long nowTick) {
        refresh(nowTick);
        if (active == null) {
            return Snapshot.idle();
        }
        return new Snapshot(
                true,
                active.actionId(),
                active.kind(),
                active.startTick(),
                active.dodgeCancelAtTick(),
                active.endTick(),
                active.movementMultiplier()
        );
    }

    public void clear() {
        active = null;
    }

    private void refresh(long nowTick) {
        if (active != null && nowTick >= active.endTick()) {
            active = null;
        }
    }

    public enum WindowKind {
        ACTION,
        REACTION
    }

    private record ActiveWindow(
            String actionId,
            WindowKind kind,
            long startTick,
            long dodgeCancelAtTick,
            long endTick,
            double movementMultiplier
    ) {
    }

    public record BeginResult(
            boolean accepted,
            long dodgeCancelAtTick,
            long endTick
    ) {
        public static BeginResult rejected() {
            return new BeginResult(
                    false,
                    Long.MIN_VALUE / 4,
                    Long.MIN_VALUE / 4
            );
        }
    }

    public record ReactionResult(
            boolean accepted,
            int durationTicks,
            long endTick
    ) {
    }

    public record Snapshot(
            boolean active,
            String actionId,
            WindowKind kind,
            long startTick,
            long dodgeCancelAtTick,
            long endTick,
            double movementMultiplier
    ) {
        private static Snapshot idle() {
            return new Snapshot(
                    false,
                    "",
                    WindowKind.ACTION,
                    Long.MIN_VALUE / 4,
                    Long.MIN_VALUE / 4,
                    Long.MIN_VALUE / 4,
                    1.0
            );
        }
    }
}
