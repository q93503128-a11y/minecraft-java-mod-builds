package io.github.q93503128.turnbound.session;

import java.util.List;

/** Server-authored victory reward preview rendered before the battle screen closes. */
public record BattleResultSummary(int xp, int gold, boolean firstClear, List<PartyXp> party) {
    public record PartyXp(
            String characterId,
            String name,
            int levelBefore,
            int xpBefore,
            int levelAfter,
            int xpAfter,
            int xpToNextAfter
    ) {}

    public BattleResultSummary {
        party = List.copyOf(party == null ? List.of() : party);
    }

    public static BattleResultSummary none() { return new BattleResultSummary(0, 0, false, List.of()); }
}
