package io.github.q93503128.turnbound;

import java.util.List;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleSessionAuthority;

/** Render-thread-owned, immutable view copied from server packets. Not an authority store. */
public final class TurnboundClientBattleView {
    public record Skill(String id, String name, int cooldown) {}
    public record Unit(String id, String name, boolean ally, boolean summon,
                       int hp, int maxHp, int barrier, long gauge, boolean downed,
                       List<Skill> ownedSkills) {}
    public record Snapshot(long sessionId, long revision, String currentActorId,
                           BattleOutcome outcome, List<Unit> units) {
        public Snapshot {
            units = List.copyOf(units);
        }
    }
    public record Receipt(long sessionId, BattleSessionAuthority.Status status) {}
    private static Snapshot current;
    private static Receipt lastReceipt;

    private TurnboundClientBattleView() {}

    public static Snapshot snapshot() { return current; }
    public static Receipt receipt() { return lastReceipt; }
    public static void update(Snapshot value) { current = value; }
    public static void receipt(Receipt value) { lastReceipt = value; }
    public static void clear() { current = null; lastReceipt = null; }
}
