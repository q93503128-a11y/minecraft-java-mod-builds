package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentation-only enrichment for rewards that are settled by campaign supplemental services.
 * This never grants progression; it only makes the pre-close Result page match the authoritative settlement.
 */
public final class BattleResultPreview {
    public record View(BattleResultSummary summary, List<String> notices) {
        public View {
            summary = summary == null ? BattleResultSummary.none() : summary;
            notices = List.copyOf(notices == null ? List.of() : notices);
        }
    }

    private BattleResultPreview() {}

    public static View enrich(String encounterId, BattleResultSummary base) {
        if (base == null) base = BattleResultSummary.none();
        String canonical = CampaignProgressStore.canonicalEncounterId(encounterId == null ? "" : encounterId);
        if (!base.firstClear() || !canonical.matches("BATTLE_B0[1-5]")) {
            return new View(base, List.of());
        }

        String bossId = canonical.substring("BATTLE_".length());
        int crystal = "B01".equals(bossId) ? 3_000 : 1_200;
        int essence = V04Catalogs.bossFirstClearEssence(bossId);
        ArrayList<String> equipment = new ArrayList<>(base.equipmentRewards());
        ArrayList<String> notices = new ArrayList<>();

        switch (bossId) {
            case "B01" -> {
                equipment.add("T2 장비 선택권 ×1");
                notices.add("NEW · P08 라제 영입");
                notices.add("UNLOCK · Echo Archive");
            }
            case "B03", "B04" -> equipment.add("T3 장비 선택권 ×1");
            case "B05" -> equipment.add("T4 장비 선택권 ×1");
            default -> { }
        }

        BattleResultSummary enriched = new BattleResultSummary(
                base.xp(), base.gold(), crystal, essence, equipment, true, base.party());
        return new View(enriched, notices);
    }
}
