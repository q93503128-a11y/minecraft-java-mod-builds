package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VanillaMobCoverageValidator {
    public enum Coverage { PLAYABLE, ENEMY_ONLY, EXCLUDED }
    private VanillaMobCoverageValidator() {}

    public static List<String> missing(Set<String> eligibleVanillaMobIds, Map<String, Coverage> coverage) {
        List<String> missing = new ArrayList<>();
        for (String id : eligibleVanillaMobIds) if (!coverage.containsKey(id)) missing.add(id);
        missing.sort(String::compareTo);
        return List.copyOf(missing);
    }
}
