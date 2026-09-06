package kr.moonseungjun.turnboundre.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class PureBattleHarness {
    public record Fighter(String id, int speed, int hp) {}
    public record BattleEvent(long revision, String type, String actor, int value) {}

    private PureBattleHarness() {}

    public static List<BattleEvent> deterministicProbe(long seed, List<Fighter> fighters) {
        List<Fighter> order = new ArrayList<>(fighters);
        order.sort((a, b) -> {
            int speed = Integer.compare(b.speed(), a.speed());
            return speed != 0 ? speed : a.id().compareTo(b.id());
        });
        Random random = new Random(seed);
        List<BattleEvent> events = new ArrayList<>();
        long revision = 0;
        for (Fighter fighter : order) {
            int variance = random.nextInt(5);
            events.add(new BattleEvent(++revision, "INITIATIVE_PROBE", fighter.id(), fighter.speed() + variance));
        }
        return List.copyOf(events);
    }
}
