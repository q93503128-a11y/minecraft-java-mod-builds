package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GachaService;

import java.util.ArrayList;
import java.util.List;

/**
 * Reveal-order contract for the 2D summon presentation.
 * Every committed pull is represented in order. High-rarity spotlight metadata is compatibility-only and must never
 * decide whether a result is shown.
 */
public final class GachaPresentationPlan {
    public record Reveal(String characterId, int stars, boolean newlyOwned) {}

    private GachaPresentationPlan() {}

    public static List<Reveal> reveals(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        List<Reveal> out = new ArrayList<>(result.pulls().size());
        for (GachaService.PullResult pull : result.pulls()) {
            out.add(new Reveal(pull.characterId(), pull.nativeStars(), pull.newlyOwned()));
        }
        return List.copyOf(out);
    }

    /** Retained for old payload/test compatibility; this is never a reveal filter. */
    public static List<Integer> spotlightIndices(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < result.pulls().size(); i++) {
            if (result.pulls().get(i).nativeStars() >= 4) indices.add(i);
        }
        return List.copyOf(indices);
    }

    public static List<String> revealCharacterIds(GachaService.BatchResult result) {
        return reveals(result).stream().map(Reveal::characterId).toList();
    }
}
