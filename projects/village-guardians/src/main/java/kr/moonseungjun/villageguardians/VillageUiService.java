package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Shared multiplayer prompts and the small set of actions not owned by the current UI controller. */
public final class VillageUiService {
    private static final String SEP = "\u001F";

    private VillageUiService() {}

    public static void openQuickChat(ServerPlayer player) {
        send(player, "quick_chat", "수호단 통신",
                "필요한 신호를 선택해 접속 중인 수호단에게 전송합니다.",
                List.of("chat_ready", "chat_gate", "chat_repair", "chat_help"),
                List.of(
                        "준비 완료|다음 시간 진행 가능",
                        "북문 집결|성문 방어 지원 요청",
                        "시설 수리 요청|손상 시설 확인 요청",
                        "현재 위치 지원|내 위치로 전투 지원 요청"));
    }

    public static void openVoteForAll(MinecraftServer server, String proposerName) {
        String body = proposerName + " 님이 다음 시간 단계 진행을 제안했습니다.\n현재 제 "
                + VillageCouncilState.currentDay() + "일 " + VillageCouncilState.currentPhase().koreanName() + "입니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "vote", "시간 진행 투표", body,
                    List.of("vote_yes", "vote_no"), List.of("찬성|다음 시간 단계 진행", "반대|현재 시간 유지"));
        }
    }

    public static void openGameOverForAll(MinecraftServer server) {
        String body = "§c마을 회관이 파괴되어 방어에 실패했습니다.\n\n"
                + "§f전투 전 낮으로 돌아가면 시설 내구도와 용병을 야간 시작 시점으로 복구하고, "
                + "같은 웨이브 편성을 다시 상대합니다. 주화·보급품·획득 아이템은 되돌리지 않습니다.\n"
                + "§f처음부터 다시를 선택하면 마을·직업·레벨·성장·유물·용병·장비를 초기화합니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "game_over", "마을 방어 실패", body,
                    List.of("restart_previous", "restart_start"),
                    List.of("전투 전 낮으로 돌아가기|시설·용병 복구 · 같은 밤 재도전",
                            "처음부터 완전히 다시|마을·개인 성장·보유품 전체 초기화"));
        }
    }

    public static void openRepairSummaryForAll(MinecraftServer server) {
        String body = "§a야간 습격을 막아냈습니다.\n§f각 시설 단말기나 회관에서 손상 시설을 수리하고 강화할 수 있습니다.\n\n"
                + durabilitySummary();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "victory", "방어 성공", body,
                    List.of("open_quick_chat"), List.of("빠른 통신|수호단 신호 전송"));
        }
    }

    public static void handleAction(ServerPlayer player, String action) {
        MinecraftServer server = player == null ? null : player.level().getServer();
        if (server == null || action == null || action.isBlank()) return;
        switch (action) {
            case "open_manual", "open_caller_menu", "open_quick_chat" -> openQuickChat(player);
            case "return_village" -> player.sendSystemMessage(
                    Component.literal("§a" + VillageWorldSystem.returnToVillage(player)));
            case "advance_time" -> player.sendSystemMessage(
                    Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            case "vote_yes" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, true)));
            case "vote_no" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, false)));
            case "chat_ready" -> broadcastQuick(server, player, "준비 완료. 시간 진행 가능합니다.");
            case "chat_gate" -> broadcastQuick(server, player, "전원 북쪽 성문으로 집결!");
            case "chat_repair" -> broadcastQuick(server, player, "손상 시설 확인 후 현장 단말기에서 수리 바랍니다.");
            case "chat_help" -> broadcastQuick(server, player, "지원 요청! 좌표 "
                    + player.blockPosition().getX() + ", " + player.blockPosition().getY() + ", "
                    + player.blockPosition().getZ() + "로 모여 주세요.");
            case "restart_previous" -> restart(server, player, false);
            case "restart_start" -> restart(server, player, true);
            default -> player.sendSystemMessage(Component.literal("§c알 수 없는 마을 UI 동작입니다."));
        }
    }

    private static void restart(MinecraftServer server, ServerPlayer player, boolean fromStart) {
        if (!VillageProgressionSystem.isGameOver()) {
            player.sendSystemMessage(Component.literal(fromStart
                    ? "§c방어 실패 상태에서만 처음부터 다시 시작할 수 있습니다."
                    : "§c방어 실패 상태에서만 전투 전 낮으로 되돌릴 수 있습니다."));
            return;
        }
        VillageProgressionSystem.resetForRestart(server, fromStart);
    }

    private static void broadcastQuick(MinecraftServer server, ServerPlayer player, String text) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "§b[빠른 신호] §f" + player.getGameProfile().name() + ": " + text), false);
    }

    private static String durabilitySummary() {
        StringBuilder text = new StringBuilder();
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            text.append(building.displayName()).append(' ').append(VillageProgressionSystem.durabilityText(building))
                    .append(VillageProgressionSystem.isOperational(building) ? "" : " §c[파괴]").append('\n');
        }
        return text.toString();
    }

    private static void send(ServerPlayer player, String screenId, String title, String body,
                             List<String> actions, List<String> labels) {
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                screenId, title, body, String.join(SEP, actions), String.join(SEP, labels)));
    }
}
