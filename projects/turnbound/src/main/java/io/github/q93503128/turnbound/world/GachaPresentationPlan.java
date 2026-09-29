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
        List<Reveal> highlights = new ArrayList<>();
        for (GachaService.PullResult pull : result.pulls()) {
            if (pull.newlyOwned() || pull.nativeStars() >= 4) {
                highlights.add(new Reveal(pull.characterId(), pull.nativeStars(), pull.newlyOwned()));
            }
        }
        if (!highlights.isEmpty()) return List.copyOf(highlights);

        GachaService.PullResult best = null;
        for (GachaService.PullResult pull : result.pulls()) {
            if (best == null || pull.nativeStars() > best.nativeStars()) best = pull;
        }
        return best == null ? List.of() : List.of(new Reveal(best.characterId(), best.nativeStars(), false));
    }

    public static List<Integer> spotlightIndices(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < result.pulls().size(); i++) {
            GachaService.PullResult pull = result.pulls().get(i);
            if (pull.newlyOwned() || pull.nativeStars() >= 4) indices.add(i);
        }
        if (!indices.isEmpty()) return List.copyOf(indices);

        int bestIndex = 0;
        for (int i = 1; i < result.pulls().size(); i++) {
            if (result.pulls().get(i).nativeStars() > result.pulls().get(bestIndex).nativeStars()) bestIndex = i;
        }
        return List.of(bestIndex);
    }

    public static List<String> revealCharacterIds(GachaService.BatchResult result) {
        return reveals(result).stream().map(Reveal::characterId).toList();
    }
}
