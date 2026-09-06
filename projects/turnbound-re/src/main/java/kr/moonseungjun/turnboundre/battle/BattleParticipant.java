package kr.moonseungjun.turnboundre.battle;

public record BattleParticipant(
        String id,
        BattleTeam team,
        int participantOrdinal,
        int speed,
        int maxHp,
        int attack,
        int defense,
        int poiseMax
) {
    public BattleParticipant {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("participant id must not be blank");
        if (team == null) throw new IllegalArgumentException(id + ": team must not be null");
        if (participantOrdinal < 0) throw new IllegalArgumentException(id + ": participantOrdinal must be >= 0");
        if (speed < 0) throw new IllegalArgumentException(id + ": speed must be >= 0");
        if (maxHp <= 0) throw new IllegalArgumentException(id + ": maxHp must be > 0");
        if (attack < 0) throw new IllegalArgumentException(id + ": attack must be >= 0");
        if (defense < 0) throw new IllegalArgumentException(id + ": defense must be >= 0");
        if (poiseMax <= 0) throw new IllegalArgumentException(id + ": poiseMax must be > 0");
    }
}
