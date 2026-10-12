package io.github.q93503128.turnbound.world;

import java.util.List;

/**
 * Pure client-presentation state machine for summon reveals.
 *
 * <p>Timed stages may move only within the presentation of the current committed result. The HOLD stage has no
 * timer and therefore can never advance to another pull without an explicit player action. RNG, ownership and
 * rewards remain server-authoritative and are deliberately outside this class.</p>
 */
public final class GachaPresentationFlow {
    public enum Stage { RITUAL, OMEN, BREAK, SILHOUETTE, REVEAL, HOLD, SUMMARY }

    public record Pull(int stars, boolean newlyOwned) {
        public Pull {
            stars = Math.max(1, Math.min(5, stars));
        }
    }

    private static final int RITUAL_TICKS = 18;

    private final List<Pull> pulls;
    private final int highestStars;
    private Stage stage;
    private int stageTick;
    private int currentIndex;

    public GachaPresentationFlow(List<Pull> pulls) {
        this.pulls = List.copyOf(pulls == null ? List.of() : pulls);
        int highest = 1;
        for (Pull pull : this.pulls) highest = Math.max(highest, pull.stars());
        this.highestStars = Math.max(1, Math.min(5, highest));
        this.stage = this.pulls.isEmpty() ? Stage.SUMMARY : Stage.RITUAL;
    }

    public void tick() {
        if (stage == Stage.HOLD || stage == Stage.SUMMARY) return;
        stageTick++;
        if (stageTick < durationTicks()) return;
        stage = switch (stage) {
            case RITUAL -> Stage.OMEN;
            case OMEN -> Stage.BREAK;
            case BREAK -> Stage.SILHOUETTE;
            case SILHOUETTE -> Stage.REVEAL;
            case REVEAL -> Stage.HOLD;
            case HOLD, SUMMARY -> stage;
        };
        stageTick = 0;
    }

    /** Advances exactly one committed result. It is intentionally a no-op outside HOLD. */
    public void advance() {
        if (stage != Stage.HOLD) return;
        if (currentIndex + 1 >= pulls.size()) {
            stage = Stage.SUMMARY;
        } else {
            currentIndex++;
            stage = Stage.SILHOUETTE;
        }
        stageTick = 0;
    }

    /** Explicit skip always preserves the committed results and jumps only to their summary. */
    public void skip() {
        if (stage == Stage.SUMMARY) return;
        stage = Stage.SUMMARY;
        stageTick = 0;
    }

    public Stage stage() { return stage; }
    public int stageTick() { return stageTick; }
    public int currentIndex() { return currentIndex; }
    public int pullCount() { return pulls.size(); }
    public int highestStars() { return highestStars; }
    public Pull currentPull() { return pulls.isEmpty() ? null : pulls.get(currentIndex); }
    public boolean holding() { return stage == Stage.HOLD; }
    public boolean summary() { return stage == Stage.SUMMARY; }
    public boolean lastPull() { return pulls.isEmpty() || currentIndex >= pulls.size() - 1; }

    /** Five-star batches deliberately masquerade as four-star during the omen before the gold break. */
    public int omenStars() { return highestStars >= 5 ? 4 : highestStars; }

    public boolean fiveStarUpgrade() {
        return highestStars >= 5 && stage == Stage.BREAK && stageTick >= 5;
    }

    public int breakDisplayStars() {
        return fiveStarUpgrade() ? 5 : omenStars();
    }

    public float progress() {
        int duration = durationTicks();
        if (duration <= 0) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, stageTick / (float) duration));
    }

    public int durationTicks() {
        return switch (stage) {
            case RITUAL -> RITUAL_TICKS;
            case OMEN -> omenTicks(highestStars);
            case BREAK -> breakTicks(highestStars);
            case SILHOUETTE -> silhouetteTicks(currentStars());
            case REVEAL -> revealTicks(currentStars());
            case HOLD, SUMMARY -> 0;
        };
    }

    private int currentStars() {
        Pull pull = currentPull();
        return pull == null ? 1 : pull.stars();
    }

    private static int omenTicks(int stars) {
        return switch (stars) {
            case 5 -> 18;
            case 4 -> 15;
            case 3 -> 12;
            default -> 10;
        };
    }

    private static int breakTicks(int stars) {
        return switch (stars) {
            case 5 -> 13;
            case 4 -> 9;
            case 3 -> 7;
            default -> 6;
        };
    }

    private static int silhouetteTicks(int stars) {
        return switch (stars) {
            case 5 -> 12;
            case 4 -> 9;
            case 3 -> 7;
            case 2 -> 6;
            default -> 5;
        };
    }

    private static int revealTicks(int stars) {
        return switch (stars) {
            case 5 -> 13;
            case 4 -> 10;
            case 3 -> 8;
            case 2 -> 7;
            default -> 6;
        };
    }
}
