package kr.moonseungjun.turnboundre.battle;

public record BattleEvent(long revision, String type, String actorId, String detail) {
    public BattleEvent {
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("event type must not be blank");
        actorId = actorId == null ? "" : actorId;
        detail = detail == null ? "" : detail;
    }
}
