package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.network.GachaPresentationPayload;
import io.github.q93503128.turnbound.progression.GachaService;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Resolves a summon with existing server authority, saves it, then emits a presentation-only result payload. */
public final class GachaPresentationService {
    private GachaPresentationService() {}

    public static boolean handle(ServerPlayer player, String action) {
        if (player == null || action == null || BattleSessionManager.exists(player)) return false;
        if (!action.equals("SUMMON1") && !action.equals("SUMMON10") && !action.equals("STARTER")) return false;
        try {
            GachaService.BatchResult result = switch (action) {
                case "SUMMON1" -> CampaignProgressStore.summonStandard(player.getUUID(), 1);
                case "SUMMON10" -> CampaignProgressStore.summonStandard(player.getUUID(), 10);
                case "STARTER" -> CampaignProgressStore.summonStarter(player.getUUID());
                default -> throw new IllegalStateException("Unsupported summon action");
            };
            CampaignPersistence.saveIfDirty(player);
            PacketDistributor.sendToPlayer(player, new GachaPresentationPayload(encode(action, result)));
            MetaNetwork.sync(player);
        } catch (RuntimeException ex) {
            player.sendSystemMessage(Component.literal("TURNBOUND · 소환 실패: " + ex.getMessage()));
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
                    .append(pull.pityAfter()).append('\n');
        }
        return out.toString();
    }
}
