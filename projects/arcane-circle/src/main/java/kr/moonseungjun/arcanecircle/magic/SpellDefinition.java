package kr.moonseungjun.arcanecircle.magic;

import java.util.List;

public record SpellDefinition(
        String id,
        String name,
        int circle,
        int manaCost,
        int cooldownTicks,
        double range,
        double power,
        School school,
        Acquisition acquisition,
        String description,
        List<String> fusionSources
) {
    public enum School {
        ARCANE("비전"), FIRE("화염"), FROST("서리"), WIND("풍류"), WARD("수호"), LIFE("생명"), SPACE("공간");

        private final String displayName;
        School(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Acquisition {
        PRIMER("기초 마도서"), DIRECT("전승 주문"), FUSION("융합 연구");

        private final String displayName;
        Acquisition(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
