package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.network.GachaPresentationPayload;
import io.github.q93503128.turnbound.progression.GachaService;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Resolves a summon with server authority, persists it, then emits a presentation-only 2D result payload. */
public final class GachaPresentationService {
    private GachaPresentationService() {}

    public static boolean handle(ServerPlayer player, String action) {
        if (player == null || action == null || BattleSessionManager.exists(player)) return false;
        if (!action.equals("SUMMON1") && !action.equals("SUMMON10") && !action.equals("STARTER")) return false;

        GachaService.BatchResult result;
        try {
            result = switch (action) {
                case "SUMMON1" -> CampaignProgressStore.summonStandard(player.getUUID(), 1);
                case "SUMMON10" -> CampaignProgressStore.summonStandard(player.getUUID(), 10);
                case "STARTER" -> CampaignProgressStore.summonStarter(player.getUUID());
                default -> throw new IllegalStateException("Unsupported summon action");
            };
            CampaignPersistence.saveIfDirty(player);
        } catch (RuntimeException ex) {
            Turnbound.LOGGER.warn("Summon action failed for {}", player.getUUID(), ex);
            player.sendSystemMessage(Component.literal("소환을 완료하지 못했습니다. 보유 재화와 이용 조건을 확인해 주세요."));
            MetaNetwork.sync(player);
            return true;
        }

        try {
            PacketDistributor.sendToPlayer(player, new GachaPresentationPayload(encode(action, result)));
            MetaNetwork.sync(player);
        } catch (RuntimeException ex) {
            Turnbound.LOGGER.warn("Summon presentation failed after result was committed for {}", player.getUUID(), ex);
            player.sendSystemMessage(Component.literal("소환 결과는 저장되었습니다. 연출을 표시하지 못했지만 소환 기록에서 결과를 확인할 수 있습니다."));
            MetaNetwork.sync(player);
        }
        return true;
    }

    private static String encode(String action, GachaService.BatchResult result) {
        StringBuilder out = new StringBuilder();
        out.append("H|").append(action).append('|').append(result.pulls().size()).append('|').append(result.crystalSpent()).append('\n');
        for (GachaService.PullResult pull : result.pulls()) {
            out.append("P|").append(pull.characterId()).append('|').append(pull.nativeStars()).append('|')
                    .append(pull.newlyOwned() ? 1 : 0).append('|').append(pull.starEssenceGranted()).append('|')
                    .append(pull.pityAfter()).append('|').append(pull.bonusLevelGranted()).append('|')
                    .append(pull.bonusLevelAfter()).append('\n');
        }
        return out.toString();
    }
}
