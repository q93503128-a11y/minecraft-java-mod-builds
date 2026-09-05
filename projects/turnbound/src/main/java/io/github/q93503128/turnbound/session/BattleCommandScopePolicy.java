package io.github.q93503128.turnbound.session;

/** Pure command-boundary policy so shared-world restoration can never inherit private battle presentation scope. */
final class BattleCommandScopePolicy {
    private BattleCommandScopePolicy() {}

    static boolean privatePresentation(String opcode) {
        if (opcode == null) return false;
        return switch (opcode) {
            case "ACT", "FOCUS", "AUTO", "SPEED" -> true;
            default -> false;
        };
    }
}
