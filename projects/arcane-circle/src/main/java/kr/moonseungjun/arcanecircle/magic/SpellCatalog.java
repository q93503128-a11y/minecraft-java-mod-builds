package kr.moonseungjun.arcanecircle.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.DIRECT;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.FUSION;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.PRIMER;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.ARCANE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.FIRE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.FROST;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.LIFE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.SPACE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.WARD;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.WIND;

public final class SpellCatalog {
    private static final Map<String, SpellDefinition> SPELLS = new LinkedHashMap<>();
    private static final List<FusionFormula> FUSIONS = new ArrayList<>();
    private static boolean bootstrapped;

    private SpellCatalog() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        add("arcane_dart", "마력탄", 1, 14, 40, 10.0, 4.0, ARCANE, PRIMER,
                "응축한 마력을 직선으로 쏘는 모든 전투술의 기초.");
        add("ember", "불씨", 1, 16, 60, 9.0, 3.0, FIRE, PRIMER,
                "작은 화염핵을 붙여 대상을 태운다.");
        add("frost_needle", "서리침", 1, 18, 70, 10.0, 3.0, FROST, PRIMER,
                "냉기를 바늘처럼 쏘아 이동과 체온을 빼앗는다.");
        add("gale_step", "질풍보", 1, 20, 100, 6.0, 1.0, WIND, PRIMER,
                "발밑의 공기를 터뜨려 시선 방향으로 빠르게 이동한다.");
        add("lesser_ward", "소형 방벽", 1, 24, 160, 0.0, 4.0, WARD, PRIMER,
                "얇은 마력막으로 짧은 시간 피해를 흡수한다.");

        add("mend", "치유", 2, 35, 200, 0.0, 6.0, LIFE, DIRECT,
                "치유학파에서 전승되는 2써클 생명 회복술.");
        add("blink", "단거리 전이", 2, 40, 240, 8.0, 1.0, SPACE, DIRECT,
                "공간을 접어 짧은 거리를 즉시 건넌다.");
        addFusion("flame_lance", "화염창", 2, 32, 100, 14.0, 8.0, FIRE,
                "마력탄의 관통 구조에 불씨를 결합한 고속 화염술.", "arcane_dart", "ember");
        addFusion("ice_shackles", "빙결 사슬", 2, 36, 140, 12.0, 5.0, FROST,
                "서리침을 방벽 구조로 고정해 대상을 묶는다.", "frost_needle", "lesser_ward");
        addFusion("wind_blade", "풍인", 2, 30, 90, 13.0, 6.0, WIND,
                "질풍보의 압력을 마력탄 형태로 잘라 날린다.", "gale_step", "arcane_dart");

        add("greater_ward", "중형 방벽", 3, 60, 360, 0.0, 8.0, WARD, DIRECT,
                "고대 방벽술 계보에서 직접 전승되는 3써클 수호술.");
        addFusion("fireball", "화염구", 3, 70, 200, 16.0, 12.0, FIRE,
                "화염창을 불씨로 재점화해 폭발성 화염 영역을 만든다.", "flame_lance", "ember");
        addFusion("frost_nova", "서리 폭발", 3, 72, 240, 8.0, 8.0, FROST,
                "빙결 사슬을 전방위로 풀어 주변을 동결한다.", "ice_shackles", "frost_needle");
        addFusion("chain_bolt", "연쇄 마력뢰", 3, 68, 180, 14.0, 7.0, ARCANE,
                "풍인의 궤도 제어를 마력탄에 결합해 여러 대상에 연쇄한다.", "wind_blade", "arcane_dart");
        addFusion("rift_step", "균열 도약", 3, 65, 280, 16.0, 1.0, SPACE,
                "단거리 전이와 질풍보를 겹쳐 더 먼 공간을 안정적으로 건넌다.", "blink", "gale_step");

        addFusion("triune_barrage", "삼상 마력포", 3, 82, 260, 15.0, 11.0, ARCANE,
                "비전·화염·서리의 세 핵을 한 궤도에 겹쳐 폭발시키는 삼중 융합술.",
                "arcane_dart", "ember", "frost_needle");
        addFusion("tempest_aegis", "폭풍 성벽", 3, 78, 300, 7.0, 9.0, WARD,
                "질풍의 압력과 방벽 회로를 겹쳐 적을 밀어내는 이동식 성벽을 만든다.",
                "gale_step", "lesser_ward", "arcane_dart");
        addFusion("phoenix_field", "불사조 성역", 3, 90, 420, 7.0, 10.0, FIRE,
                "불씨·치유·중형 방벽을 삼중 결속해 아군을 살리고 적을 태우는 영역을 연다.",
                "ember", "mend", "greater_ward");
    }

    private static void add(String id, String name, int circle, int mana, int cooldown, double range,
                            double power, SpellDefinition.School school, SpellDefinition.Acquisition acquisition,
                            String description) {
        SPELLS.put(id, new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, acquisition, description, List.of()));
    }

    private static void addFusion(String id, String name, int circle, int mana, int cooldown, double range,
                                  double power, SpellDefinition.School school, String description,
                                  String... ingredients) {
        List<String> sources = List.of(ingredients);
        SpellDefinition spell = new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, FUSION, description, sources);
        SPELLS.put(id, spell);
        FUSIONS.add(new FusionFormula(id, sources));
    }

    public static Map<String, SpellDefinition> spells() {
        bootstrap();
        return Collections.unmodifiableMap(SPELLS);
    }

    public static List<FusionFormula> fusions() {
        bootstrap();
        return List.copyOf(FUSIONS);
    }

    public static Optional<SpellDefinition> spell(String id) {
        bootstrap();
        return Optional.ofNullable(SPELLS.get(id));
    }

    public static Optional<FusionFormula> fusionFor(List<String> ingredients) {
        bootstrap();
        List<String> normalized = normalized(ingredients);
        return FUSIONS.stream().filter(formula -> formula.normalizedIngredients().equals(normalized)).findFirst();
    }

    public static Optional<FusionFormula> fusionFor(String... ingredients) {
        return fusionFor(List.of(ingredients));
    }

    /** Returns every formula that can still be completed from the offered partial multiset. */
    public static List<FusionFormula> candidatesFor(List<String> ingredients) {
        bootstrap();
        List<String> offered = normalized(ingredients);
        if (offered.isEmpty() || offered.size() > 3) return List.of();
        return FUSIONS.stream()
                .filter(formula -> offered.size() <= formula.ingredients().size())
                .filter(formula -> isSortedMultisetSubset(offered, formula.normalizedIngredients()))
                .toList();
    }

    public static boolean canExtend(List<String> ingredients) {
        int size = normalized(ingredients).size();
        return candidatesFor(ingredients).stream().anyMatch(formula -> formula.ingredients().size() > size);
    }

    public static boolean isFusionResult(String spellId) {
        return spell(spellId).map(spell -> spell.acquisition() == FUSION).orElse(false);
    }

    public static int masteryRequired(String resultId) {
        SpellDefinition spell = spell(resultId).orElse(null);
        if (spell == null) return 4;
        int ingredients = Math.max(2, spell.fusionSources().size());
        if (spell.circle() <= 2) return ingredients == 2 ? 4 : 6;
        return ingredients == 2 ? 7 : 10;
    }

    public static List<String> starterKnownSpells() {
        bootstrap();
        return SPELLS.values().stream()
                .filter(spell -> spell.acquisition() != FUSION)
                .map(SpellDefinition::id)
                .toList();
    }

    public static List<String> starterSlots() {
        return List.of("arcane_dart", "ember", "frost_needle", "gale_step", "lesser_ward");
    }

    private static List<String> normalized(List<String> ingredients) {
        if (ingredients == null) return List.of();
        return ingredients.stream().filter(value -> value != null && !value.isBlank()).sorted().toList();
    }

    private static boolean isSortedMultisetSubset(List<String> offered, List<String> formula) {
        int offeredIndex = 0;
        int formulaIndex = 0;
        while (offeredIndex < offered.size() && formulaIndex < formula.size()) {
            int compare = offered.get(offeredIndex).compareTo(formula.get(formulaIndex));
            if (compare == 0) {
                offeredIndex++;
                formulaIndex++;
            } else if (compare > 0) {
                formulaIndex++;
            } else {
                return false;
            }
        }
        return offeredIndex == offered.size();
    }

    public record FusionFormula(String result, List<String> ingredients) {
        public FusionFormula {
            ingredients = List.copyOf(ingredients);
        }

        public List<String> normalizedIngredients() {
            return normalized(ingredients);
        }

        public boolean matches(List<String> offered) {
            return normalizedIngredients().equals(normalized(offered));
        }
    }
}
