package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.battle.BattleRewardContext;
import kr.moonseungjun.turnboundre.progression.BattleRewardSettlementService;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgressStore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * Minimal first-expedition quest hook derived from authoritative world state.
 * It deliberately stores no parallel quest progress: waypoint discovery and one-time encounter completion remain canon.
 */
public final class FirstExpeditionQuestService {
    public static final String HUB_WAYPOINT = "turnbound_re:hub_01/waypoint";
    public static final String REGION_WAYPOINT = "turnbound_re:region_01/waypoint";
    public static final String RIFT_ELITE_LOCATOR = "turnbound_re:region_01/rift_elite";

    public enum Stage {
        FIND_REGION_WAYPOINT,
        DEFEAT_RIFT_VANGUARD,
        COMPLETE
    }

    private final PlayerProgressStore progress;

    public FirstExpeditionQuestService(PlayerProgressStore progress) {
        if (progress == null) throw new IllegalArgumentException("progress required");
        this.progress = progress;
    }

    /** Pure stage projection used by runtime guidance and tests. */
    public static Stage stage(PlayerProgress state, Set<String> discoveredTravelLocators) {
        if (state == null || discoveredTravelLocators == null) throw new IllegalArgumentException("state/discoveries required");
        if (state.hasCompletedEncounterLocator(RIFT_ELITE_LOCATOR)) return Stage.COMPLETE;
        if (discoveredTravelLocators.contains(REGION_WAYPOINT)) return Stage.DEFEAT_RIFT_VANGUARD;
        return Stage.FIND_REGION_WAYPOINT;
    }

    /** Natural new-game entry. Hub discovery is already committed by the server before this guidance is sent. */
    public void onInitialHubArrival(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerProgress state = progress.getOrCreate(server, player.getUUID());
        Stage current = stage(state, FastTravelSavedData.get(server).discovered(player.getUUID()));
        if (current != Stage.COMPLETE) player.sendSystemMessage(objective(current), false);
    }

    /** Called only after the fast-travel service accepted a real physical waypoint interaction. */
    public void onFastTravelResult(ServerPlayer player, WorldFastTravelService.Result result) {
        if (player == null || result == null || result.code() != WorldFastTravelService.ResultCode.DISCOVERED) return;
        if (!HUB_WAYPOINT.equals(result.sourceLocator()) && !REGION_WAYPOINT.equals(result.sourceLocator())) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        PlayerProgress state = progress.getOrCreate(server, player.getUUID());
        Stage current = stage(state, FastTravelSavedData.get(server).discovered(player.getUUID()));
        if (current == Stage.COMPLETE) return;
        player.sendSystemMessage(objective(current), true);
    }

    /** Called after the one-shot reward/completion write succeeds. Failed persistence never advances guidance. */
    public void onSettlement(
            MinecraftServer server,
            BattleRewardContext context,
            BattleRewardSettlementService.Settlement settlement
    ) {
        if (server == null || context == null || settlement == null) return;
        if (!context.fromWorldAnchor() || context.worldAnchorRepeatable()) return;
        if (!RIFT_ELITE_LOCATOR.equals(context.worldAnchorLocator())) return;
        if (!settlement.state().hasCompletedEncounterLocator(RIFT_ELITE_LOCATOR)) return;

        ServerPlayer player = server.getPlayerList().getPlayer(settlement.ownerPlayerId());
        if (player != null) player.sendSystemMessage(objective(Stage.COMPLETE), false);
    }

    static Component objective(Stage stage) {
        return switch (stage) {
            case FIND_REGION_WAYPOINT -> Component.translatable("screen.turnbound_re.expedition.title")
                    .append(Component.literal(" · "))
                    .append(Component.translatable("travel.turnbound_re.region_01"));
            case DEFEAT_RIFT_VANGUARD -> Component.translatable("screen.turnbound_re.anchor.challenge")
                    .append(Component.literal(" · "))
                    .append(Component.translatable("encounter.turnbound_re.debug_rift_elite.name"));
            case COMPLETE -> Component.translatable("screen.turnbound_re.result.victory")
                    .append(Component.literal(" · "))
                    .append(Component.translatable("travel.turnbound_re.hub_01"));
        };
    }
}
