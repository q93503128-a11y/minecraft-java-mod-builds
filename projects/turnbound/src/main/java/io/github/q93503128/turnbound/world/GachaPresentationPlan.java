package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.progression.GachaService;

import java.util.ArrayList;
import java.util.List;

/** Pure reveal-order contract shared by the server presentation runtime and unit tests. */
public final class GachaPresentationPlan {
    private GachaPresentationPlan() {}

    public static List<String> revealCharacterIds(GachaService.BatchResult result) {
        if (result == null || result.pulls().isEmpty()) return List.of();
        List<String> newlyOwned = new ArrayList<>();
        for (GachaService.PullResult pull : result.pulls()) if (pull.newlyOwned()) newlyOwned.add(pull.characterId());
        if (!newlyOwned.isEmpty()) return List.copyOf(newlyOwned);

        GachaService.PullResult best = null;
        for (GachaService.PullResult pull : result.pulls()) {
            if (best == null || pull.nativeStars() > best.nativeStars()) best = pull;
        }
        return best == null ? List.of() : List.of(best.characterId());
    }
}
