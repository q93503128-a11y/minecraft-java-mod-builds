
package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AcademyOfferCatalog {
    public enum Kind { PRIMER, SPELLBOOK, STAFF, GEAR }
    public record Offer(String id, String displayName, String description, int circle, long basePrice,
                        Kind kind, String targetId) {}

    private static List<Offer> cached;
    private AcademyOfferCatalog() {}

    public static List<Offer> offers() {
        if (cached != null) return cached;
        List<Offer> result = new ArrayList<>();
        result.add(new Offer("primer", "초심자 마도서", "1써클 기초 주문 5종을 각인합니다.",
                1, SpellCatalog.arcaneMarkPrice(1), Kind.PRIMER, "beginner_grimoire"));
        for (SpellDefinition spell : SpellCatalog.bookSpells()) {
            result.add(new Offer("spell:" + spell.id(), "주문서: " + spell.name(), spell.description(),
                    spell.circle(), SpellCatalog.arcaneMarkPrice(spell.circle()), Kind.SPELLBOOK, spell.id()));
        }
        long[] staffPrices = {0, 300, 650, 1200, 4000, 12000, 36000, 180000, 1200000};
        for (int index = 0; index < ModItems.profiles().size(); index++) {
            var profile = ModItems.profiles().get(index);
            if (index == 0) continue;
            int circle = Math.min(9, index + 1);
            result.add(new Offer("staff:" + profile.id(), profile.displayName(), profile.summary(),
                    circle, staffPrices[index], Kind.STAFF, profile.id()));
        }
        result.add(new Offer("gear:mage_hat", "비전 모자", "MP·회복·마력 효율에 특화된 모자.",
                2, 1800L, Kind.GEAR, "mage_hat"));
        result.add(new Offer("gear:mage_boots", "유랑 마도화", "이동·도약·사거리·재사용 속도에 특화된 신발.",
                2, 2400L, Kind.GEAR, "mage_boots"));
        result.add(new Offer("gear:mage_robe", "중층 마도 로브", "몸과 바지 슬롯을 함께 사용하며 생존력과 주문 위력을 높입니다.",
                3, 7200L, Kind.GEAR, "mage_robe"));
        result.add(new Offer("gear:sage_hat", "현자의 모자", "고위 마력 운용과 회복 효율을 크게 높이는 2단계 모자.",
                5, 55_000L, Kind.GEAR, "sage_hat"));
        result.add(new Offer("gear:skywalker_boots", "천공 마도화", "높은 점프와 체공, 빠른 이동을 제공하는 2단계 신발.",
                5, 75_000L, Kind.GEAR, "skywalker_boots"));
        result.add(new Offer("gear:sage_robe", "현자의 로브", "몸·바지 두 칸을 사용하며 마력·방어·주문 효율을 크게 증폭합니다.",
                6, 160_000L, Kind.GEAR, "sage_robe"));
        result.add(new Offer("gear:archmage_crown", "대마도사 관", "극한의 마력량과 회복·효율을 제공하는 최상위 모자.",
                8, 1_100_000L, Kind.GEAR, "archmage_crown"));
        result.add(new Offer("gear:froststep_boots", "빙결 보행화", "장시간 체공하고 물 위에 얼음 길을 만드는 최상위 신발.",
                8, 1_400_000L, Kind.GEAR, "froststep_boots"));
        result.add(new Offer("gear:archmage_robe", "대마도사 예복", "몸·바지 두 칸을 대가로 생존력과 주문 효율을 압도적으로 높입니다.",
                9, 3_200_000L, Kind.GEAR, "archmage_robe"));
        cached = List.copyOf(result);
        return cached;
    }

    public static Optional<Offer> offer(String id) {
        return offers().stream().filter(value -> value.id().equals(id)).findFirst();
    }

    public static List<Offer> forCircle(int circle) {
        return offers().stream().filter(value -> circle <= 0 || value.circle() == circle).toList();
    }
}
