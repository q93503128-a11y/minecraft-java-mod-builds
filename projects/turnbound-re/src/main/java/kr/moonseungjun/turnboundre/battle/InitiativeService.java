package kr.moonseungjun.turnboundre.battle;

import java.util.Comparator;
import java.util.List;

public final class InitiativeService {
    private static final Comparator<BattleParticipant> ORDER = Comparator
            .comparingInt(BattleParticipant::speed).reversed()
            .thenComparingInt(BattleParticipant::participantOrdinal);

    private InitiativeService() {}

    public static List<String> order(List<BattleParticipant> participants) {
        if (participants == null || participants.isEmpty()) throw new IllegalArgumentException("participants must not be empty");
        return participants.stream().sorted(ORDER).map(BattleParticipant::id).toList();
    }
}
