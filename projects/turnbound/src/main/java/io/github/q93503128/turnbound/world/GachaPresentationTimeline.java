package io.github.q93503128.turnbound.world;

/**
 * Timing contract for the 2D summon presentation.
 * Low-rarity pulls remain quick, while four- and five-star results get a noticeably longer anticipation/reveal hold.
 */
public final class GachaPresentationTimeline {
    public enum Phase { SIGNAL, SILHOUETTE, REVEAL, NAME, COMPLETE }

    public static final int SIGNAL_TICKS = 8;
    public static final int SILHOUETTE_TICKS = 8;
    public static final int REVEAL_TICK = SIGNAL_TICKS + SILHOUETTE_TICKS;
    public static final int NAME_TICK = REVEAL_TICK + 7;
    public static final int SLOT_TICKS = 40;

    public record Timing(int signalTicks, int silhouetteTicks, int revealTicks, int nameTicks) {
        public Timing {
            if (signalTicks < 1 || silhouetteTicks < 1 || revealTicks < 1 || nameTicks < 1) {
                throw new IllegalArgumentException("Summon timing phases must be positive");
            }
        }

        public int revealTick() { return signalTicks + silhouetteTicks; }
        public int nameTick() { return revealTick() + revealTicks; }
        public int slotTicks() { return nameTick() + nameTicks; }
    }

    private GachaPresentationTimeline() {}

    public static Timing timing(int stars, boolean newlyOwned) {
        int clamped = Math.max(1, Math.min(5, stars));
        int newHold = newlyOwned ? switch (clamped) {
            case 5 -> 4;
            case 4 -> 3;
            default -> 2;
        } : 0;
        return switch (clamped) {
            case 5 -> new Timing(14, 14, 12, 24 + newHold);
            case 4 -> new Timing(10, 10, 9, 17 + newHold);
            case 3 -> new Timing(7, 7, 7, 12 + newHold);
            case 2 -> new Timing(6, 6, 6, 10 + newHold);
            default -> new Timing(5, 5, 6, 8 + newHold);
        };
    }

    public static Phase phase(int slotTick, int stars, boolean newlyOwned) {
        Timing timing = timing(stars, newlyOwned);
        int tick = Math.max(0, slotTick);
        if (tick < timing.signalTicks()) return Phase.SIGNAL;
        if (tick < timing.revealTick()) return Phase.SILHOUETTE;
        if (tick < timing.nameTick()) return Phase.REVEAL;
        if (tick < timing.slotTicks()) return Phase.NAME;
        return Phase.COMPLETE;
    }

    public static int slotTicks(int stars, boolean newlyOwned) {
        return timing(stars, newlyOwned).slotTicks();
    }

    public static int revealTick(int stars, boolean newlyOwned) {
        return timing(stars, newlyOwned).revealTick();
    }

    public static int nameTick(int stars, boolean newlyOwned) {
        return timing(stars, newlyOwned).nameTick();
    }

    public static Phase phase(int slotTick) {
        return phase(slotTick, 4, false);
    }

    public static int intensity(int stars) {
        return switch (Math.max(1, Math.min(5, stars))) {
            case 5 -> 4;
            case 4 -> 3;
            case 3 -> 2;
            case 2 -> 1;
            default -> 0;
        };
    }

    public static int summaryTicks(int pullCount) {
        return pullCount <= 1 ? 38 : 86;
    }

    public static int revealTicks(Iterable<GachaPresentationPlan.Reveal> reveals) {
        int total = 0;
        if (reveals != null) {
            for (GachaPresentationPlan.Reveal reveal : reveals) {
                if (reveal != null) total += slotTicks(reveal.stars(), reveal.newlyOwned());
            }
        }
        return total;
    }

    public static int totalTicks(Iterable<GachaPresentationPlan.Reveal> reveals, int pullCount) {
        return revealTicks(reveals) + summaryTicks(pullCount);
    }

    public static int totalTicks(int revealCount, int pullCount) {
        return Math.max(1, revealCount) * SLOT_TICKS + summaryTicks(pullCount);
    }
}
