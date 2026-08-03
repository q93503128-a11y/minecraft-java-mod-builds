package kr.moonseungjun.arcanecircle.world;

import java.util.Arrays;
import java.util.List;

/** Static faction identity; live strongest-member data is supplied by ArcaneEncounterData. */
public final class FactionProfile {
    public record Entry(MagicTradition tradition, String representativeId, String representativeName,
                        int representativeCircle, MageSociety.Role role, String headquarters) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry(MagicTradition.ARCANE, "arcane_arden", "청람의 아르덴", 7,
                    MageSociety.Role.LICENSED, "왕국 마도연맹 북부 첨탑"),
            new Entry(MagicTradition.DIVINE, "divine_seraphine", "백은의 세라핀", 8,
                    MageSociety.Role.WARDEN, "백은 성약 성광원"),
            new Entry(MagicTradition.OCCULT, "occult_mirel", "녹월의 미렐", 7,
                    MageSociety.Role.SCHOLAR, "녹월 결사 지하 관측실"),
            new Entry(MagicTradition.PRIMAL, "primal_varkas", "재왕 바르카스", 9,
                    MageSociety.Role.VILLAIN, "재의 밀약 붕괴 제단")
    );

    private FactionProfile() {}

    public static List<Entry> entries() { return ENTRIES; }

    public static Entry of(MagicTradition tradition) {
        return ENTRIES.stream().filter(value -> value.tradition() == tradition).findFirst()
                .orElse(new Entry(MagicTradition.UNBOUND, "", "없음", 0,
                        MageSociety.Role.WANDERER, "정해진 본거지 없음"));
    }

    public static String namesFor(MagicTradition origin, MageSociety.Relation relation) {
        List<String> names = Arrays.stream(MagicTradition.values())
                .filter(value -> value != MagicTradition.UNBOUND && value != origin)
                .filter(value -> MageSociety.relation(origin, value) == relation)
                .map(MagicTradition::displayName).toList();
        return names.isEmpty() ? "없음" : String.join(" · ", names);
    }
}
