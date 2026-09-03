package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.SignatureTrialEvaluator;
import io.github.q93503128.turnbound.content.SignatureTrialCatalog;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;

import java.util.UUID;

/**
 * Server-authoritative Signature Trial progression facade for the future Hall/menu/world entry flow.
 *
 * <p>The service intentionally reports canon-blocked Trials instead of inventing encounter rosters.
 * Reward settlement accepts only an evaluator result that is both objectively clear and canon-ready.</p>
 */
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
            return owned && endgameUnlocked && level == 60 && currentStar == 6 && characterQuestComplete && !firstClearClaimed;
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
        boolean canonReady = false; // v0.4 currently has only roster gaps or the P08 contradiction.
        String reason = progressionBlock(endgame, level, growth);
        if (reason.isBlank() && !canonReady) reason = spec.unresolvedReason();
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
        if (!endgame) return "B05 클리어 후 Signature Trial이 해금됩니다.";
        if (!growth.characterQuestComplete()) return "해당 캐릭터 개인 퀘스트를 먼저 완료해야 합니다.";
        if (growth.currentStar() != 6 || level.level() != 60) return "Signature Trial은 Lv60 / ★6이 필요합니다.";
        if (growth.signatureTrialCleared()) return "Signature Trial 첫 클리어 보상을 이미 획득했습니다.";
        return "";
    }
}
