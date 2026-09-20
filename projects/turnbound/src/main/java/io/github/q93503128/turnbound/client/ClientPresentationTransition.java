package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;

/** Prevents a one-frame field/survival HUD flash while battle and field packets hand presentation ownership over. */
final class ClientPresentationTransition {
    private static final int RETURN_GUARD_TICKS = 20;
    private static boolean returning;
    private static int returnGuardTicks;

    private ClientPresentationTransition() {}

    static void onBattleSnapshot(boolean wasActive, ClientBattleState.Snapshot snapshot) {
        if (snapshot != null && snapshot.active()) {
            returning = false;
            returnGuardTicks = 0;
            return;
        }
        if (wasActive) {
            returning = !fieldReady(ClientFieldState.snapshot());
            returnGuardTicks = returning ? RETURN_GUARD_TICKS : 0;
        }
    }

    static void onFieldSnapshot(FieldUiSnapshot snapshot) {
        if (returning && fieldReady(snapshot)) {
            returning = false;
            returnGuardTicks = 0;
        }
    }

    static void tick() {
        if (!returning) return;
        if (fieldReady(ClientFieldState.snapshot()) || --returnGuardTicks <= 0) {
            returning = false;
            returnGuardTicks = 0;
        }
    }

    static void reset() {
        returning = false;
        returnGuardTicks = 0;
    }

    static boolean fieldPresentationSuppressed() {
        return returning
                || ClientBattleState.snapshot().active()
                || ClientFieldState.snapshot().mode() == FieldUiSnapshot.Mode.BATTLE_TRANSITION;
    }

    static boolean rpgHudOwned() {
        return returning || ClientBattleState.snapshot().active() || ClientFieldState.snapshot().active();
    }

    static boolean battlePresentationOwned() {
        return returning
                || ClientBattleState.snapshot().active()
                || ClientFieldState.snapshot().mode() == FieldUiSnapshot.Mode.BATTLE_TRANSITION;
    }

    static boolean fieldReady(FieldUiSnapshot snapshot) {
        return snapshot != null
                && snapshot.active()
                && snapshot.mode() != FieldUiSnapshot.Mode.LOADING
                && snapshot.mode() != FieldUiSnapshot.Mode.BATTLE_TRANSITION;
    }
}
