package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GachaService;

import java.util.ArrayList;
import java.util.List;

/**
 * Reveal-order contract shared by the server presentation runtime and unit tests.
 *
 * <p>A single pull always gets its reveal. A multi-pull reserves the longer 3D ceremony for four- and five-star
 * results; low-rarity/new results remain readable in the final summary instead of forcing ten full ceremonies.</p>
 */
public final class GachaPresentationPlan {
    public record Reveal(String characterId, int stars, boolean newlyOwned) {}

    private GachaPresentationPlan() {}

    public static List<Reveal> reveals(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        List<Integer> indices = spotlightIndices(result);
        List<Reveal> out = new ArrayList<>(indices.size());
        for (int index : indices) {
            GachaService.PullResult pull = result.pulls().get(index);
            out.add(new Reveal(pull.characterId(), pull.nativeStars(), pull.newlyOwned()));
        }
        return List.copyOf(out);
    }

    public static List<Integer> spotlightIndices(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        if (result.pulls().size() == 1) return List.of(0);

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < result.pulls().size(); i++) {
            if (result.pulls().get(i).nativeStars() >= 4) indices.add(i);
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
