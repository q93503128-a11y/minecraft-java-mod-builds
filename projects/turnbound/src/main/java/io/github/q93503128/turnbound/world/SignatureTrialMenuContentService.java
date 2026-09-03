package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.AwakeningRouteRules;
import io.github.q93503128.turnbound.content.CharacterMenuCatalog;
import io.github.q93503128.turnbound.content.SignatureTrialCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;

import java.util.UUID;

/**
 * Encodes player-specific Signature Trial/Awakening state into the existing meta snapshot payload.
 *
 * <p>Rows use the {@code T|...} prefix so older meta parsers safely ignore them. The server remains authoritative:
 * the client only renders these values and cannot use them to bypass progression or canon gates.</p>
 *
 * <p>Every v0.4 character receives a row. P01~P08 use their authored Signature Trial status. F01~F04 explicitly
 * have no Signature Equipment/Trial in the character canon, so the server emits their unresolved Awakening route
 * as a canon-gap row instead of making the client synthesize progression state.</p>
 */
public final class SignatureTrialMenuContentService {
    private SignatureTrialMenuContentService() { }

    public static String encode(UUID playerId) {
        if (playerId == null) return "";
        var campaign = CampaignProgressStore.snapshot(playerId);
        var equipment = campaign.equipment();
        long awakeningCore = campaign.profile().awakeningCore();
        StringBuilder out = new StringBuilder();

        for (SignatureTrialCatalog.Spec spec : SignatureTrialCatalog.all()) {
            SignatureTrialProgressService.Status status = SignatureTrialProgressService.status(playerId, spec.characterId());
            String signatureId = V04Catalogs.signatureFor(spec.characterId()).id();
            boolean signatureInInventory = equipment.items().values().stream()
                    .anyMatch(item -> signatureId.equals(item.itemId()));
            boolean signaturePending = equipment.pendingRewards().stream()
                    .anyMatch(item -> signatureId.equals(item.itemId()));
            boolean signatureGranted = signatureInInventory || signaturePending;
            boolean awakened = status.owned() && campaign.growth().containsKey(spec.characterId())
                    && campaign.growth().get(spec.characterId()).awakened();
            boolean awakeningReady = status.owned() && !awakened && status.level() == 60 && status.currentStar() == 6
                    && status.firstClearClaimed() && awakeningCore > 0;

            append(out,
                    status.characterId(), status.title(), status.owned(), status.endgameUnlocked(),
                    status.level(), status.currentStar(), status.characterQuestComplete(), status.firstClearClaimed(),
                    status.encounterCanonReady(), signatureGranted, signaturePending, awakeningCore, awakened,
                    awakeningReady, status.objective(), status.blockReason());
        }

        for (CharacterMenuCatalog.Profile profile : CharacterMenuCatalog.all()) {
            String characterId = profile.id();
            if (!AwakeningRouteRules.canonGap(characterId)) continue;

            boolean owned = campaign.profile().ownedCharacters().contains(characterId);
            var levelState = campaign.characters().get(characterId);
            var growthState = campaign.growth().get(characterId);
            int level = owned && levelState != null ? levelState.level() : 0;
            int currentStar = owned && growthState != null ? growthState.currentStar() : 0;
            boolean awakened = owned && growthState != null && growthState.awakened();

            append(out,
                    characterId,
                    "없음 · 소재형 각성 경로",
                    owned,
                    CampaignContentUnlocks.endgame(playerId),
                    level,
                    currentStar,
                    true,
                    false,
                    false,
                    false,
                    false,
                    awakeningCore,
                    awakened,
                    false,
                    "전용 장비와 개인 퀘스트 없음 · 각성 효과만 정본에 정의됨",
                    AwakeningRouteRules.blockReason(characterId));
        }
        return out.toString();
    }

    private static void append(
            StringBuilder out,
            String characterId,
            String title,
            boolean owned,
            boolean endgameUnlocked,
            int level,
            int currentStar,
            boolean characterQuestComplete,
            boolean firstClearClaimed,
            boolean encounterCanonReady,
            boolean signatureGranted,
            boolean signaturePending,
            long awakeningCore,
            boolean awakened,
            boolean awakeningReady,
            String objective,
            String blockReason
    ) {
        out.append("T|").append(characterId).append('|').append(safe(title)).append('|')
                .append(owned ? 1 : 0).append('|')
                .append(endgameUnlocked ? 1 : 0).append('|')
                .append(level).append('|').append(currentStar).append('|')
                .append(characterQuestComplete ? 1 : 0).append('|')
                .append(firstClearClaimed ? 1 : 0).append('|')
                .append(encounterCanonReady ? 1 : 0).append('|')
                .append(signatureGranted ? 1 : 0).append('|')
                .append(signaturePending ? 1 : 0).append('|')
                .append(awakeningCore).append('|')
                .append(awakened ? 1 : 0).append('|')
                .append(awakeningReady ? 1 : 0).append('|')
                .append(safe(objective)).append('|')
                .append(safe(blockReason)).append('\n');
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
