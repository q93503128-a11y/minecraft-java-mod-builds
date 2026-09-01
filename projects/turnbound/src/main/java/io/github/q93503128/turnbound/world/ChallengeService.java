package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.content.ChallengeCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-side one-time Challenge completion/reward authority for criteria fully specified by v0.4. */
public final class ChallengeService {
    private static final String MARK_KEY = "__turnbound_challenges";

    private ChallengeService() {}

    public static Set<String> completed(UUID playerId) {
        return Set.copyOf(CampaignProgressStore.snapshot(playerId).quests().marks().getOrDefault(MARK_KEY, Set.of()));
    }

    /** Returns the exact challenges this victory would newly complete without mutating campaign state. */
    public static List<String> preview(UUID playerId, String encounterId, BattleState state, BattleOutcome outcome) {
        if (playerId == null || state == null || outcome != BattleOutcome.ALLY_VICTORY) return List.of();
        if (V04Catalogs.tutorialBridge(encounterId)) return List.of();
        Metrics metrics = metrics(state);
        Set<String> already = new LinkedHashSet<>(completed(playerId));
        List<String> earned = new ArrayList<>();

        awardIf(earned, already, "CH03_UNDER_12_ALLY_ACTIONS", metrics.allyActions() < 12);
        awardIf(earned, already, "CH04_UNDER_20_ALLY_ACTIONS", metrics.allyActions() < 20);
        awardIf(earned, already, "CH05_REVIVE_AND_WIN", metrics.revives() >= 1);
        awardIf(earned, already, "CH10_HEAL_2000_ONE_BATTLE", metrics.healing() >= 2_000);
        awardIf(earned, already, "CH11_FINISH_LOW_HP", metrics.finishedUnderTenPercent());
        awardIf(earned, already, "CH12_KILL_E003_BEFORE_EXPLOSION", metrics.killedE003BeforeExplosion());
        awardIf(earned, already, "CH13_SURVIVE_E003_EXPLOSION", metrics.e003ExplosionOccurred());
        awardIf(earned, already, "CH14_ELITE_NO_REVIVE", metrics.elitePresent() && metrics.revives() == 0);
        awardIf(earned, already, "CH15_HARD_B01", "HARD_B01".equals(encounterId));
        awardIf(earned, already, "CH16_HARD_B02", "HARD_B02".equals(encounterId));
        awardIf(earned, already, "CH17_HARD_B03", "HARD_B03".equals(encounterId));
        awardIf(earned, already, "CH18_HARD_B04", "HARD_B04".equals(encounterId));
        awardIf(earned, already, "CH19_HARD_B05", "HARD_B05".equals(encounterId));
        awardIf(earned, already, "CH20_RIFT_F30", "RIFT_F30".equals(encounterId));
        return List.copyOf(earned);
    }

    public static List<String> evaluateAndCommit(UUID playerId, String encounterId, BattleState state, BattleOutcome outcome) {
        List<String> earned = preview(playerId, encounterId, state, outcome);
        if (earned.isEmpty()) return List.of();
        Set<String> completed = new LinkedHashSet<>(completed(playerId));
        completed.addAll(earned);
        applyRewards(playerId, completed, earned);
        return earned;
    }

    private static void awardIf(List<String> earned, Set<String> already, String id, boolean condition) {
        if (!condition || already.contains(id) || !ChallengeCatalog.get(id).autoEvaluable()) return;
        already.add(id);
        earned.add(id);
    }

    private static void applyRewards(UUID playerId, Set<String> completed, List<String> earned) {
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        PlayerProfile profile = PlayerProfile.restore(snapshot.profile());
        for (String id : earned) {
            ChallengeCatalog.Challenge challenge = ChallengeCatalog.get(id);
            profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, challenge.crystal());
            profile.grant(PlayerProfile.Currency.GOLD, challenge.gold());
        }

        QuestProgress.Snapshot oldQuest = snapshot.quests();
        Map<String, Set<String>> marks = new LinkedHashMap<>();
        oldQuest.marks().forEach((key, values) -> marks.put(key, new LinkedHashSet<>(values)));
        marks.put(MARK_KEY, new LinkedHashSet<>(completed));
        QuestProgress.Snapshot quests = new QuestProgress.Snapshot(oldQuest.completed(), oldQuest.tracked(), oldQuest.unlockFlags(),
                oldQuest.rewardTokens(), oldQuest.counters(), marks);

        CampaignProgressStore.restore(playerId, new CampaignProgressStore.Snapshot(
                profile.snapshot(), snapshot.characters(), snapshot.growth(), snapshot.equipment(), quests, snapshot.activeParty(),
                snapshot.clearedEncounters(), snapshot.orphanedCharacterIds(), snapshot.orphanedEquipmentIds()));
        CampaignProgressStore.markDirty(playerId);
    }

    private static Metrics metrics(BattleState state) {
        int allyActions = 0;
        int revives = 0;
        int healing = 0;
        boolean explosion = false;
        boolean killedBeforeExplosion = false;
        Set<String> explodedE003 = new LinkedHashSet<>();

        for (BattleEvent event : state.events()) {
            if ("TURN_END".equals(event.type()) && regularAlly(state, event.sourceId())) allyActions++;
            if ("REVIVE".equals(event.type()) || "SELF_REVIVE".equals(event.type())) revives++;
            if (("HEAL".equals(event.type()) || "REACTION_HEAL".equals(event.type())) && regularAlly(state, event.sourceId())) {
                healing += Math.max(0, event.value());
            }
            if ("ACTION".equals(event.type()) && "e003_explode".equals(event.detail())) {
                explosion = true;
                explodedE003.add(event.sourceId());
            }
            if ("DOWN".equals(event.type())) {
                CombatantState target = state.find(event.targetId());
                if (target != null && "E003".equals(target.definition().id()) && !explodedE003.contains(target.instanceId())) {
                    killedBeforeExplosion = true;
                }
            }
        }

        boolean lowHp = state.living(CombatantSide.ALLY).stream()
                .filter(unit -> !unit.definition().summon())
                .anyMatch(unit -> unit.hp() * 10L < unit.maxHp());
        boolean elite = state.combatants().stream()
                .anyMatch(unit -> unit.side() == CombatantSide.ENEMY && unit.definition().id().startsWith("EL"));
        return new Metrics(allyActions, revives, healing, lowHp, killedBeforeExplosion, explosion, elite);
    }

    private static boolean regularAlly(BattleState state, String instanceId) {
        CombatantState unit = state.find(instanceId);
        return unit != null && unit.side() == CombatantSide.ALLY && !unit.definition().summon();
    }

    private record Metrics(int allyActions, int revives, int healing, boolean finishedUnderTenPercent,
                           boolean killedE003BeforeExplosion, boolean e003ExplosionOccurred, boolean elitePresent) {}
}
