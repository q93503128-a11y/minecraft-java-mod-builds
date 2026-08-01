
package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AcademyOfferCatalog {
    public enum Kind { PRIMER, SPELLBOOK, STAFF }
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
