package kr.moonseungjun.arcanecircle.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.BOOK;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.FUSION;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.PRIMER;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.ARCANE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.FIRE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.FROST;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.LIFE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.SPACE;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.WARD;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.WIND;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.BODY;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.FEET;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.FRONT;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.GROUND_SELF;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.GROUND_TARGET;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.SigilAnchor.TARGET;

public final class SpellCatalog {
    private static final Map<String, SpellDefinition> SPELLS = new LinkedHashMap<>();
    private static final List<FusionFormula> FUSIONS = new ArrayList<>();
    private static boolean bootstrapped;

    private SpellCatalog() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        // 1st Circle: the five foundational formulae contained in the beginner grimoire.
        add("arcane_dart", "마력탄", 1, 10, 30, 12.0, 4.0, ARCANE, PRIMER, FRONT,
                "응축한 순수 마력을 전방 마법진에서 직선으로 방출하는 모든 공격술의 기초.");
        add("ember", "불씨", 1, 12, 45, 10.0, 3.5, FIRE, PRIMER, FRONT,
                "작은 화염핵을 전방에 고정한 뒤 발사해 대상을 태운다.");
        add("frost_needle", "서리침", 1, 14, 55, 12.0, 3.5, FROST, PRIMER, FRONT,
                "냉기를 바늘처럼 세공해 이동과 체온을 빼앗는다.");
        add("gale_step", "질풍보", 1, 16, 80, 7.0, 1.0, WIND, PRIMER, FEET,
                "발밑 마법진의 공기를 폭발시켜 시선 방향으로 급가속한다.");
        add("lesser_ward", "소형 방벽", 1, 20, 120, 0.0, 4.0, WARD, PRIMER, BODY,
                "신체 둘레에 얇은 방벽 회로를 두어 짧은 시간 피해를 흡수한다.");

        // 2nd Circle: practical adventuring magic, normally acquired from spellbooks.
        add("mend", "치유", 2, 30, 180, 0.0, 7.0, LIFE, BOOK, BODY,
                "손상된 생체 마력 흐름을 봉합해 상처를 회복한다.");
        add("blink", "단거리 전이", 2, 34, 200, 10.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "공간 좌표를 짧게 접어 안전한 지점으로 즉시 이동한다.");
        add("stone_skin", "석화 피부", 2, 38, 260, 0.0, 6.0, WARD, BOOK, BODY,
                "피부 표면에 광물성 방호층을 형성해 물리 충격을 줄인다.");
        add("lightning_arc", "번개 갈래", 2, 42, 160, 13.0, 7.0, ARCANE, BOOK, FRONT,
                "전방 마법진에서 전류를 뽑아 가까운 적 사이로 두 번 갈라 보낸다.");
        add("mana_lance", "마나 랜스", 2, 45, 140, 16.0, 9.0, ARCANE, BOOK, FRONT,
                "압축한 비전력을 창 형태로 방출해 일렬의 적을 관통한다.");

        addFusion("flame_lance", "화염창", 2, 32, 100, 14.0, 8.0, FIRE, FRONT,
                "마력탄의 관통 구조에 불씨를 결합한 고속 화염술.", "arcane_dart", "ember");
        addFusion("ice_shackles", "빙결 사슬", 2, 36, 140, 12.0, 5.0, FROST, TARGET,
                "서리침을 방벽 구조로 고정해 대상을 묶는다.", "frost_needle", "lesser_ward");
        addFusion("wind_blade", "풍인", 2, 30, 90, 13.0, 6.0, WIND, FRONT,
                "질풍보의 압력을 마력탄 형태로 잘라 날린다.", "gale_step", "arcane_dart");

        // 3rd Circle: battlefield control and mature defensive magic.
        add("greater_ward", "중형 방벽", 3, 58, 300, 0.0, 10.0, WARD, BOOK, BODY,
                "다층 방벽을 겹쳐 상당한 피해를 흡수한다.");
        add("flame_wave", "화염 파도", 3, 62, 220, 12.0, 9.0, FIRE, BOOK, FRONT,
                "넓은 전방 마법진에서 부채꼴 화염파를 밀어낸다.");
        add("ice_lance", "빙창", 3, 64, 200, 16.0, 11.0, FROST, BOOK, FRONT,
                "거대한 얼음창으로 단일 대상을 꿰뚫고 깊게 동결한다.");
        add("arcane_sight", "비전 감응", 3, 50, 500, 18.0, 1.0, ARCANE, BOOK, BODY,
                "주변 생명체와 마력 흔적을 감지해 어둠과 엄폐 속 존재를 드러낸다.");
        add("levitation", "부유", 3, 55, 260, 0.0, 1.0, WIND, BOOK, FEET,
                "중력 방향을 완화해 짧게 상승하고 안전하게 낙하한다.");

        addFusion("fireball", "화염구", 3, 70, 200, 16.0, 12.0, FIRE, FRONT,
                "화염창을 불씨로 재점화해 폭발성 화염핵을 발사한다.", "flame_lance", "ember");
        addFusion("frost_nova", "서리 폭발", 3, 72, 240, 8.0, 8.0, FROST, GROUND_SELF,
                "빙결 사슬을 전방위로 풀어 주변을 동결한다.", "ice_shackles", "frost_needle");
        addFusion("chain_bolt", "연쇄 마력뢰", 3, 68, 180, 14.0, 7.0, ARCANE, FRONT,
                "풍인의 궤도 제어를 마력탄에 결합해 여러 대상에 연쇄한다.", "wind_blade", "arcane_dart");
        addFusion("rift_step", "균열 도약", 3, 65, 280, 16.0, 1.0, SPACE, GROUND_TARGET,
                "단거리 전이와 질풍보를 겹쳐 더 먼 공간을 안정적으로 건넌다.", "blink", "gale_step");
        addFusion("triune_barrage", "삼상 마력포", 3, 82, 260, 15.0, 11.0, ARCANE, FRONT,
                "비전·화염·서리의 세 핵을 한 궤도에 겹쳐 폭발시키는 삼중 융합술.",
                "arcane_dart", "ember", "frost_needle");
        addFusion("tempest_aegis", "폭풍 성벽", 3, 78, 300, 7.0, 9.0, WARD, GROUND_SELF,
                "질풍의 압력과 방벽 회로를 겹쳐 적을 밀어내는 이동식 성벽을 만든다.",
                "gale_step", "lesser_ward", "arcane_dart");
        addFusion("phoenix_field", "불사조 성역", 3, 90, 420, 7.0, 10.0, FIRE, GROUND_SELF,
                "불씨·치유·중형 방벽을 삼중 결속해 아군을 살리고 적을 태우는 영역을 연다.",
                "ember", "mend", "greater_ward");

        // 4th Circle: strategic area spells and long-distance spatial formulae.
        add("meteor_shard", "유성 파편", 4, 95, 360, 22.0, 16.0, FIRE, BOOK, GROUND_TARGET,
                "조준 지점 상공에 화염질량을 생성해 낙하시켜 넓게 폭발시킨다.");
        add("blizzard_field", "블리자드", 4, 100, 420, 14.0, 12.0, FROST, BOOK, GROUND_TARGET,
                "지정 지역의 열을 빼앗아 지속되는 눈보라와 동결 지대를 만든다.");
        add("thunder_prison", "뇌전 감옥", 4, 110, 400, 16.0, 14.0, ARCANE, BOOK, GROUND_TARGET,
                "번개 기둥을 다각형으로 세워 내부 적을 감전시키고 움직임을 봉쇄한다.");
        add("mass_mend", "광역 치유", 4, 105, 500, 9.0, 18.0, LIFE, BOOK, GROUND_SELF,
                "생명 회로를 넓게 펼쳐 자신과 아군, 길들인 동물을 함께 치유한다.");
        add("spatial_gate", "공간문", 4, 115, 460, 30.0, 1.0, SPACE, BOOK, GROUND_TARGET,
                "두 좌표 사이에 안정된 통로를 열어 장거리를 건넌다.");

        // 5th Circle: domain-scale magic. Costs and cooldowns deliberately jump sharply.
        add("inferno_domain", "업화 영역", 5, 170, 700, 16.0, 26.0, FIRE, BOOK, GROUND_TARGET,
                "넓은 영역을 고열의 마력장으로 덮어 적을 연속적으로 태운다.");
        add("absolute_zero", "절대영도", 5, 180, 760, 15.0, 24.0, FROST, BOOK, GROUND_TARGET,
                "지정 지역의 운동과 열을 강제로 정지시켜 적을 완전 동결한다.");
        add("tempest_domain", "천공 폭풍", 5, 175, 680, 17.0, 22.0, WIND, BOOK, GROUND_TARGET,
                "폭풍권을 형성해 적을 띄우고 밀어내며 반복적인 뇌격을 가한다.");
        add("aegis_citadel", "아이기스 성채", 5, 165, 800, 11.0, 22.0, WARD, BOOK, GROUND_SELF,
                "거대한 수호 결계를 세워 범위 안 아군에게 강력한 흡수막과 저항을 부여한다.");
        add("arcane_annihilation", "비전 소멸포", 5, 200, 900, 30.0, 40.0, ARCANE, BOOK, FRONT,
                "다중 전방 마법진을 직렬 정렬해 고밀도 비전 광선을 방출한다.");
    }

    private static void add(String id, String name, int circle, int mana, int cooldown, double range,
                            double power, SpellDefinition.School school, SpellDefinition.Acquisition acquisition,
                            SpellDefinition.SigilAnchor anchor, String description) {
        SPELLS.put(id, new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, acquisition, anchor, description, List.of()));
    }

    private static void addFusion(String id, String name, int circle, int mana, int cooldown, double range,
                                  double power, SpellDefinition.School school, SpellDefinition.SigilAnchor anchor,
                                  String description, String... ingredients) {
        List<String> sources = List.of(ingredients);
        SpellDefinition spell = new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, FUSION, anchor, description, sources);
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

    public static List<SpellDefinition> primerSpells() {
        bootstrap();
        return SPELLS.values().stream().filter(spell -> spell.acquisition() == PRIMER).toList();
    }

    public static List<SpellDefinition> bookSpells() {
        bootstrap();
        return SPELLS.values().stream().filter(spell -> spell.acquisition() == BOOK).toList();
    }

    public static String bookItemId(String spellId) {
        return "spellbook_" + spellId;
    }

    public static Optional<FusionFormula> fusionFor(List<String> ingredients) {
        bootstrap();
        List<String> normalized = normalized(ingredients);
        return FUSIONS.stream().filter(formula -> formula.normalizedIngredients().equals(normalized)).findFirst();
    }

    public static Optional<FusionFormula> fusionFor(String... ingredients) {
        return fusionFor(List.of(ingredients));
    }

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

    /** New profiles know no spells until the beginner grimoire is read. Kept for save migration callers. */
    public static List<String> starterKnownSpells() {
        return List.of();
    }

    public static List<String> starterSlots() {
        return List.of("", "", "", "", "");
    }

    public static int circleInsightThreshold(int circle) {
        return switch (circle) {
            case 2 -> 40;
            case 3 -> 140;
            case 4 -> 360;
            case 5 -> 800;
            default -> 0;
        };
    }

    public static int emeraldEquivalentPrice(int circle) {
        return switch (circle) {
            case 1 -> 12;
            case 2 -> 27;
            case 3 -> 72;
            case 4 -> 216;
            case 5 -> 576;
            default -> 12;
        };
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
