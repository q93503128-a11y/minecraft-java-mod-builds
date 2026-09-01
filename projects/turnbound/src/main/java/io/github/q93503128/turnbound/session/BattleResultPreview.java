package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.content.ChallengeCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import io.github.q93503128.turnbound.world.ChallengeService;
import io.github.q93503128.turnbound.world.CharacterProgression;
import io.github.q93503128.turnbound.world.QuestResultPreview;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Presentation-only enrichment for rewards settled beside the base encounter reward.
 * This never mutates progression; it makes the pre-close Result page match the authoritative settlement.
 */
public final class BattleResultPreview {
    public record View(BattleResultSummary summary, List<String> notices) {
        public View {
            summary = summary == null ? BattleResultSummary.none() : summary;
            notices = List.copyOf(notices == null ? List.of() : notices);
        }
    }

    private BattleResultPreview() {}

    public static View enrich(UUID playerId, String encounterId, BattleState state, BattleResultSummary base) {
        if (base == null) base = BattleResultSummary.none();
        if (playerId == null || state == null || state.outcome() != BattleOutcome.ALLY_VICTORY) {
            return new View(base, List.of());
        }

        String canonical = CampaignProgressStore.canonicalEncounterId(encounterId == null ? "" : encounterId);
        int xp = base.xp();
        int gold = base.gold();
        int crystal = base.crystal();
        int essence = base.starEssence();
        ArrayList<String> equipment = new ArrayList<>(base.equipmentRewards());
        ArrayList<String> notices = new ArrayList<>();

        // Campaign boss supplemental rewards are committed after CampaignProgressStore.commit().
        if (base.firstClear() && canonical.matches("BATTLE_B0[1-5]")) {
            String bossId = canonical.substring("BATTLE_".length());
            crystal += "B01".equals(bossId) ? 3_000 : 1_200;
            essence += V04Catalogs.bossFirstClearEssence(bossId);
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
        }

        // MAIN quests can auto-complete from the same BATTLE_WIN/BOSS_WIN events during settlement.
        QuestResultPreview.Preview quest = QuestResultPreview.automaticMainRewards(playerId, canonical);
        xp += quest.xp();
        gold += quest.gold();
        crystal += quest.crystal();
        for (QuestResultPreview.Completion completion : quest.completions()) {
            notices.add("MAIN QUEST CLEAR · " + completion.name()
                    + " · Crystal +" + completion.crystal() + " · Gold +" + completion.gold()
                    + (completion.xp() > 0 ? " · XP +" + completion.xp() : ""));
        }

        // Challenge settlement happens in the same durable reward transaction.
        for (String challengeId : ChallengeService.preview(playerId, encounterId, state, state.outcome())) {
            ChallengeCatalog.Challenge challenge = ChallengeCatalog.get(challengeId);
            crystal += challenge.crystal();
            gold += challenge.gold();
            notices.add("CHALLENGE CLEAR · " + challenge.ordinal() + ". " + challenge.label()
                    + " · Crystal +" + challenge.crystal() + " · Gold +" + challenge.gold());
        }

        List<BattleResultSummary.PartyXp> party = recomputeParty(playerId, base.party(), xp);
        BattleResultSummary enriched = new BattleResultSummary(
                xp, gold, crystal, essence, equipment, base.firstClear(), party);
        return new View(enriched, notices);
    }

    private static List<BattleResultSummary.PartyXp> recomputeParty(
            UUID playerId, List<BattleResultSummary.PartyXp> baseParty, int totalXp) {
        ArrayList<BattleResultSummary.PartyXp> out = new ArrayList<>();
        for (BattleResultSummary.PartyXp member : baseParty) {
            int cap = CharacterGrowthRules.levelCap(CampaignProgressStore.growth(playerId, member.characterId()).currentStar());
            CharacterProgression.Gain gain = CharacterProgression.gain(
                    new CharacterProgression.State(member.levelBefore(), member.xpBefore()), totalXp, cap);
            out.add(new BattleResultSummary.PartyXp(
                    member.characterId(), member.name(), gain.before().level(), gain.before().xp(),
                    gain.after().level(), gain.after().xp(), gain.xpToNextAfter()));
        }
        return List.copyOf(out);
    }
}
