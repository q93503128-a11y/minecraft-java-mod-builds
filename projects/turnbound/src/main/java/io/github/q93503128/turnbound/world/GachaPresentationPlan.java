package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GachaService;

import java.util.ArrayList;
import java.util.List;

/** Pure reveal-order contract shared by the server presentation runtime and unit tests. */
public final class GachaPresentationPlan {
    public record Reveal(String characterId, int stars, boolean newlyOwned) {}

    private GachaPresentationPlan() {}

    public static List<Reveal> reveals(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        List<Reveal> newlyOwned = new ArrayList<>();
        for (GachaService.PullResult pull : result.pulls()) {
            if (pull.newlyOwned()) newlyOwned.add(new Reveal(pull.characterId(), pull.nativeStars(), true));
        }
        if (!newlyOwned.isEmpty()) return List.copyOf(newlyOwned);

        GachaService.PullResult best = null;
        for (GachaService.PullResult pull : result.pulls()) {
            if (best == null || pull.nativeStars() > best.nativeStars()) best = pull;
        }
        return best == null ? List.of() : List.of(new Reveal(best.characterId(), best.nativeStars(), false));
    }

    public static List<String> revealCharacterIds(GachaService.BatchResult result) {
        return reveals(result).stream().map(Reveal::characterId).toList();
    }
}
