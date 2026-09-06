package kr.moonseungjun.turnboundre.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PureBattleHarness {
    public record Fighter(String id, int speed, int hp) {}
    public record BattleEvent(long revision, String type, String actor, int value) {}

    private PureBattleHarness() {}

    public static List<BattleEvent> deterministicProbe(long seed, List<Fighter> fighters) {
        return deterministicProbe(seed, fighters, 1);
    }

    public static List<BattleEvent> deterministicProbe(long seed, List<Fighter> fighters, int rounds) {
        if (fighters == null || fighters.isEmpty()) throw new IllegalArgumentException("fighters must not be empty");
        if (rounds < 1) throw new IllegalArgumentException("rounds must be >= 1");
        for (Fighter fighter : fighters) {
            if (fighter == null || fighter.id() == null || fighter.id().isBlank()) throw new IllegalArgumentException("fighter id must not be blank");
            if (fighter.speed() < 0) throw new IllegalArgumentException(fighter.id() + ": speed must be >= 0");
            if (fighter.hp() <= 0) throw new IllegalArgumentException(fighter.id() + ": hp must be > 0");
        }

        List<Fighter> order = new ArrayList<>(fighters);
        order.sort((a, b) -> {
            int speed = Integer.compare(b.speed(), a.speed());
            return speed != 0 ? speed : a.id().compareTo(b.id());
        });
        Random random = new Random(seed);
        List<BattleEvent> events = new ArrayList<>(Math.multiplyExact(order.size(), rounds));
        long revision = 0;
        for (int round = 0; round < rounds; round++) {
            for (Fighter fighter : order) {
                int variance = random.nextInt(5);
                events.add(new BattleEvent(++revision, "INITIATIVE_PROBE", fighter.id(), fighter.speed() + variance));
            }
        }
        return List.copyOf(events);
    }
}
