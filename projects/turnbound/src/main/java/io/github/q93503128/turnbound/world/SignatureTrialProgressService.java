package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SignatureTrialEvaluator;
import io.github.q93503128.turnbound.content.SignatureTrialCatalog;
import io.github.q93503128.turnbound.content.SignatureTrialEncounterAuthoring;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;

import java.util.UUID;

/** Server-authoritative Signature Equipment Trial progression. Awakening is a separate v1 growth axis. */
public final class SignatureTrialProgressService {
    public record Status(
            String characterId,
            String title,
            boolean owned,
            boolean endgameUnlocked,
            int level,
            int currentStar,
            boolean characterQuestComplete,
            boolean firstClearClaimed,
            boolean encounterCanonReady,
            String objective,
            String blockReason
    ) {
        public boolean progressionReady() {
            return owned && endgameUnlocked && level == GrowthRulesV1.maxLevel()
                    && characterQuestComplete && !firstClearClaimed;
        }

        public boolean canEnter() { return progressionReady() && encounterCanonReady; }
    }

    private SignatureTrialProgressService() {}

    public static Status status(UUID playerId, String characterId) {
        if (playerId == null) throw new IllegalArgumentException("Missing player id");
        SignatureTrialCatalog.Spec spec = SignatureTrialCatalog.forCharacter(characterId);
        boolean owned = CampaignProgressStore.ownedCharacters(playerId).contains(characterId);
        boolean endgame = CampaignContentUnlocks.signatureActual(playerId);
        if (!owned) {
            return new Status(characterId, spec.title(), false, endgame, 0, 0,
                    false, false, false, spec.objective(), "캐릭터를 보유하지 않았습니다.");
        }

        CharacterProgression.State level = CampaignProgressStore.character(playerId, characterId);
        CharacterGrowthRules.State growth = CampaignProgressStore.growth(playerId, characterId);
        SignatureTrialEncounterAuthoring.Validation encounter = SignatureTrialEncounterAuthoring.readiness(characterId);
        boolean canonReady = encounter.ready();
        String reason = progressionBlock(endgame, level, growth);
        if (reason.isBlank() && !canonReady) reason = encounter.blockReason();
        return new Status(characterId, spec.title(), true, endgame, level.level(), growth.currentStar(),
                growth.characterQuestComplete(), growth.signatureTrialCleared(), canonReady,
                spec.objective(), reason);
    }

    public static EquipmentInventory.Item settle(UUID playerId, SignatureTrialEvaluator.Evaluation evaluation) {
        if (playerId == null || evaluation == null) throw new IllegalArgumentException("Missing Signature Trial settlement data");
        Status status = status(playerId, evaluation.characterId());
        if (!status.progressionReady()) {
            throw new IllegalStateException(status.blockReason().isBlank()
                    ? "Signature Trial progression prerequisites are not complete" : status.blockReason());
        }
        if (!evaluation.objectiveMet()) {
            throw new IllegalStateException("Signature Trial objective is not complete: " + evaluation.detail());
        }
        if (!evaluation.settlementEligible() || !status.encounterCanonReady()) {
            throw new IllegalStateException("Signature Trial reward is canon-blocked: " + status.blockReason());
        }
        return CampaignProgressStore.completeSignatureTrial(playerId, evaluation.characterId());
    }

    private static String progressionBlock(boolean endgame, CharacterProgression.State level, CharacterGrowthRules.State growth) {
        if (!endgame) return "메인 여정을 더 진행하면 전용 장비 시련이 열립니다.";
        if (!growth.characterQuestComplete()) return "해당 캐릭터 개인 퀘스트를 먼저 완료해야 합니다.";
        if (level.level() != GrowthRulesV1.maxLevel()) return "전용 장비 시련은 Lv60이 필요합니다.";
        if (growth.signatureTrialCleared()) return "전용 장비 시련의 첫 보상을 이미 획득했습니다.";
        return "";
    }
}
