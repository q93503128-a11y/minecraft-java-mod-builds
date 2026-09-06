package kr.moonseungjun.turnboundre.battle;

public record BattleCommand(long expectedRevision, String actorId, String actionId) {
    public BattleCommand {
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must be >= 0");
        if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("actorId must not be blank");
        if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("actionId must not be blank");
    }
}
