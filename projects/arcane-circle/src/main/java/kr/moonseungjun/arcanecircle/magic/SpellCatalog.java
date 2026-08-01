package kr.moonseungjun.arcanecircle.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.Acquisition.*;
import static kr.moonseungjun.arcanecircle.magic.SpellDefinition.School.*;

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
    }

    private static void add(String id, String name, int circle, int mana, int cooldown, double range,
                            double power, SpellDefinition.School school, SpellDefinition.Acquisition acquisition,
                            String description) {
        SPELLS.put(id, new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, acquisition, description, List.of()));
    }

    private static void addFusion(String id, String name, int circle, int mana, int cooldown, double range,
                                  double power, SpellDefinition.School school, String description,
                                  String first, String second) {
        SpellDefinition spell = new SpellDefinition(id, name, circle, mana, cooldown, range, power,
                school, FUSION, description, List.of(first, second));
        SPELLS.put(id, spell);
        FUSIONS.add(new FusionFormula(id, first, second));
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

    public record FusionFormula(String result, String first, String second) {}
}
