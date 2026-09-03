package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.SignatureTrialCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;

import java.util.UUID;

/**
 * Encodes player-specific Signature Trial/Awakening state into the existing meta snapshot payload.
 *
 * <p>Rows use the {@code T|...} prefix so older meta parsers safely ignore them. The server remains authoritative:
 * the client only renders these values and cannot use them to bypass progression or canon gates.</p>
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

            out.append("T|").append(status.characterId()).append('|').append(safe(status.title())).append('|')
                    .append(status.owned() ? 1 : 0).append('|')
                    .append(status.endgameUnlocked() ? 1 : 0).append('|')
                    .append(status.level()).append('|').append(status.currentStar()).append('|')
                    .append(status.characterQuestComplete() ? 1 : 0).append('|')
                    .append(status.firstClearClaimed() ? 1 : 0).append('|')
                    .append(status.encounterCanonReady() ? 1 : 0).append('|')
                    .append(signatureGranted ? 1 : 0).append('|')
                    .append(signaturePending ? 1 : 0).append('|')
                    .append(awakeningCore).append('|')
                    .append(awakened ? 1 : 0).append('|')
                    .append(awakeningReady ? 1 : 0).append('|')
                    .append(safe(status.objective())).append('|')
                    .append(safe(status.blockReason())).append('\n');
        }
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
