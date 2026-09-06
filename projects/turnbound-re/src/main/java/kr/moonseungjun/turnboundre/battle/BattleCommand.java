package kr.moonseungjun.turnboundre.battle;

import java.util.List;

public record BattleCommand(
        long expectedRevision,
        String actorId,
        String actionId,
        String commandId,
        List<String> targetIds
) {
    public BattleCommand(long expectedRevision, String actorId, String actionId) {
        this(expectedRevision, actorId, actionId,
                "legacy:" + expectedRevision + ":" + actorId + ":" + actionId,
                List.of());
    }

    public BattleCommand {
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must be >= 0");
        if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("actorId must not be blank");
        if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("actionId must not be blank");
        if (commandId == null || commandId.isBlank()) throw new IllegalArgumentException("commandId must not be blank");
        if (targetIds == null) throw new IllegalArgumentException("targetIds must not be null");
        for (String targetId : targetIds) {
            if (targetId == null || targetId.isBlank()) throw new IllegalArgumentException("targetIds must not contain blanks");
        }
        targetIds = List.copyOf(targetIds);
    }
}
