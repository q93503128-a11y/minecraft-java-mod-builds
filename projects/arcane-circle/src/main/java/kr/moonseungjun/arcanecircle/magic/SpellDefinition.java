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
        SigilAnchor sigilAnchor,
        String description,
        List<String> fusionSources
) {
    /**
     * Copy-source spells used to be authored as BODY/0-range self magic even after their runtime
     * became target-based. Keep the saved/catalog schema stable while exposing the corrected
     * effective targeting contract to every caster, VFX and UI caller.
     */
    public double range() {
        return switch (id) {
            case "simulacrum" -> 28.0;
            case "clone" -> 32.0;
            default -> range;
        };
    }

    public SigilAnchor sigilAnchor() {
        return switch (id) {
            case "simulacrum", "clone" -> SigilAnchor.TARGET;
            default -> sigilAnchor;
        };
    }

    /** Short authored identity/lore text belongs on normal browsing surfaces. */
    public String description() {
        return description;
    }

    /** Detailed, testable mechanics belong in the dedicated effect compendium. */
    public String effectSummary() {
        String firstCircle = FirstCircleSpellSummary.summary(id);
        if (!firstCircle.isBlank()) return firstCircle;
        String sixthCircle = SixthCircleSpellSummary.summary(id);
        if (!sixthCircle.isBlank()) return sixthCircle;
        String effect = SpellEffectSummary.summary(this);
        return effect == null ? "" : effect;
    }

    public enum School {
        ARCANE("비전"), FIRE("화염"), FROST("서리"), WIND("풍류"), WARD("수호"), LIFE("생명"), SPACE("공간");

        private final String displayName;
        School(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Acquisition {
        PRIMER("초심자 마도서"), BOOK("주문서"), FUSION("융합 연구");

        private final String displayName;
        Acquisition(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    /** Where the charge-stage magic circle is stabilized before release. */
    public enum SigilAnchor {
        FRONT("전방 전개"),
        FEET("발밑 전개"),
        BODY("신체 전개"),
        GROUND_SELF("시전자 지면"),
        GROUND_TARGET("조준 지면"),
        TARGET("대상 결속");

        private final String displayName;
        SigilAnchor(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
}
