package dev.moonseungjun.fishinggame.fishing;

import java.util.Set;

public enum FishingHotspot {
    LAKESIDE_REEDS(
            FishingLocation.LAKESIDE,
            "서쪽 얕은 물",
            "작은 민물고기와 잉어류가 잘 모입니다",
            Set.of("bluegill", "crucian", "carp")
    ),
    LAKESIDE_ROCKS(
            FishingLocation.LAKESIDE,
            "바위 그늘",
            "송어와 배스 같은 활동적인 물고기가 자주 붙습니다",
            Set.of("perch", "trout", "largemouth")
    ),
    LAKESIDE_DEEP(
            FishingLocation.LAKESIDE,
            "깊은 물골",
            "큰 메기와 희귀 잉어를 노리기 좋습니다",
            Set.of("catfish", "golden_carp")
    ),
    COAST_BREAKWATER(
            FishingLocation.COAST,
            "방파제 안쪽",
            "고등어와 참돔이 자주 모입니다",
            Set.of("mackerel", "sea_bream")
    ),
    COAST_CHANNEL(
            FishingLocation.COAST,
            "항로 중앙",
            "회유하는 연어를 노리기 좋습니다",
            Set.of("salmon")
    ),
    COAST_OUTER(
            FishingLocation.COAST,
            "외해 끝부두",
            "큰 참치가 접근할 가능성이 높아집니다",
            Set.of("tuna")
    ),
    DEEP_LIGHTS(
            FishingLocation.DEEP_SEA,
            "유도등 수역",
            "빛에 반응하는 심해어가 잘 모입니다",
            Set.of("angler")
    ),
    DEEP_TRENCH(
            FishingLocation.DEEP_SEA,
            "심해 골",
            "긴 심해어가 지나가는 길목입니다",
            Set.of("oarfish")
    ),
    DEEP_ANCIENT(
            FishingLocation.DEEP_SEA,
            "고대 해구",
            "아주 큰 고대종을 노릴 수 있는 수역입니다",
            Set.of("ancient_sturgeon")
    );

    private static final double PREFERRED_MULTIPLIER = 1.70;
    private static final double OTHER_MULTIPLIER = 0.92;

    private final FishingLocation location;
    private final String displayName;
    private final String hint;
    private final Set<String> preferredSpecies;

    FishingHotspot(
            FishingLocation location,
            String displayName,
            String hint,
            Set<String> preferredSpecies
    ) {
        this.location = location;
        this.displayName = displayName;
        this.hint = hint;
        this.preferredSpecies = Set.copyOf(preferredSpecies);
    }

    public FishingLocation location() {
        return location;
    }

    public String displayName() {
        return displayName;
    }

    public String hint() {
        return hint;
    }

    public boolean prefers(String speciesId) {
        return preferredSpecies.contains(speciesId);
    }

    public double weightMultiplier(FishSpecies species) {
        if (species.location() != location) return 1.0;
        return prefers(species.id()) ? PREFERRED_MULTIPLIER : OTHER_MULTIPLIER;
    }

    public static FishingHotspot at(FishingLocation location, double x, double z) {
        return switch (location) {
            case LAKESIDE -> x < -6.0
                    ? LAKESIDE_REEDS
                    : (x > 6.0 ? LAKESIDE_ROCKS : LAKESIDE_DEEP);
            case COAST -> x < -8.0
                    ? COAST_BREAKWATER
                    : (x > 8.0 ? COAST_OUTER : COAST_CHANNEL);
            case DEEP_SEA -> x < -10.0
                    ? DEEP_LIGHTS
                    : (x > 10.0 ? DEEP_ANCIENT : DEEP_TRENCH);
        };
    }

    public static FishingHotspot primaryForSpecies(String speciesId) {
        for (FishingHotspot hotspot : values()) {
            if (hotspot.prefers(speciesId)) return hotspot;
        }
        return LAKESIDE_DEEP;
    }
}
