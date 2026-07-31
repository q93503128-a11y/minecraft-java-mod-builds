package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class VillageUiService {
    private static final String SEP = "\u001F";

    private VillageUiService() {}

    public static void openManual(ServerPlayer player) {
        String body = "§6마을 수호대 작전 지침\n"
                + "§f낮에는 시설을 수리·강화하고 대장간과 스킬 습득소에서 성장합니다.\n"
                + "§f밤에는 북쪽 단일 성문으로 적이 접근합니다. 성문 내구도가 0이 되면 한 번에 무너집니다.\n"
                + "§f적은 안으로 들어와 핵심 건물을 공격하며, 파괴된 시설은 수리 전까지 사용할 수 없습니다.\n"
                + "§f모든 핵심 건물이 파괴되면 이전 날 또는 처음부터 다시 시작할 수 있습니다.\n"
                + "§e수호 주화§f는 적 처치와 방어 성공으로 얻고, 화살은 첫 지급 이후 창고 상점에서 구매합니다.\n"
                + "§f죽어도 소지품은 유지되며 장비 내구도는 감소하지 않습니다.";
        if (VillageCouncilState.isMayor(player)) {
            send(player, "manual", "마을 수호대 작전 설명서", body,
                    List.of("open_mayor"), List.of("촌장 호출기 열기"));
        } else {
            send(player, "manual", "마을 수호대 작전 설명서", body, List.of(), List.of());
        }
    }

    public static void openMayor(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !VillageCouncilState.isMayor(player)) {
            player.sendSystemMessage(Component.literal("§c촌장만 마을 호출기를 사용할 수 있습니다."));
            return;
        }
        boolean multiplayer = server.getPlayerList().getPlayerCount() > 1;
        String body = "§6제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "\n"
                + VillageCouncilState.status(server, player) + "\n"
                + VillageProgressionSystem.status(player) + "\n"
                + VillageRaidSystem.status() + "\n\n"
                + "§7성문 " + VillageProgressionSystem.durabilityText(VillageProgressionSystem.Building.WALLS)
                + " | 회관 " + VillageProgressionSystem.durabilityText(VillageProgressionSystem.Building.TOWN_HALL);
        send(player, "mayor", "촌장 전용 마을 호출기", body,
                List.of("advance_time", "chat_ready", "chat_gate", "chat_repair", "chat_help", "open_manual"),
                List.of(multiplayer ? "다음 단계 투표 열기" : "낮·밤 전환",
                        "준비 완료 신호", "성문 집결 신호", "수리 요청 신호", "지원 요청 신호", "작전 설명서"));
    }

    public static void openVoteForAll(MinecraftServer server) {
        String body = "§e촌장이 다음 시간 단계로 진행하는 안건을 올렸습니다.\n"
                + "§f현재 제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "입니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "vote", "마을 시간 진행 투표", body,
                    List.of("vote_yes", "vote_no"), List.of("찬성", "반대"));
        }
    }

    public static void openBuilding(ServerPlayer player, VillageProgressionSystem.Building building) {
        boolean usable = VillageProgressionSystem.isOperational(building);
        String body = "§6" + building.displayName() + "\n"
                + "§f시설 레벨: " + VillageProgressionSystem.level(building) + " / "
                + VillageProgressionSystem.MAX_BUILDING_LEVEL + "\n"
                + "§f내구도: " + VillageProgressionSystem.durabilityText(building) + "\n"
                + "§f공동 보급품: " + VillageProgressionSystem.supplies() + "\n"
                + "§e내 수호 주화: " + VillageProgressionSystem.coins(player) + "\n\n"
                + description(player, building, usable);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (!usable) {
            actions.add("repair:" + building.id());
            labels.add("수리비 지불하고 복구");
        } else {
            fillBuildingActions(building, actions, labels);
        }
        send(player, "building", building.displayName(), body, actions, labels);
    }

    public static void openGameOverForAll(MinecraftServer server) {
        String body = "§c마을의 모든 핵심 건물이 파괴되었습니다.\n\n"
                + "§f이전 날부터 시작하면 현재 성장과 강화를 유지한 채 시설을 복구합니다.\n"
                + "§f처음부터 시작하면 마을 발전, 공동 보급품, 개인 강화 단계를 초기화합니다.\n"
                + "§7어느 선택에서도 플레이어 소지품은 사라지지 않습니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "game_over", "마을 방어 실패", body,
                    List.of("restart_previous", "restart_start"),
                    List.of("이전 날부터 다시", "처음부터 다시"));
        }
    }

    public static void openRepairSummaryForAll(MinecraftServer server) {
        String body = "§a야간 습격을 막아냈습니다.\n"
                + "§f손상된 시설은 낮 동안 공동 보급품으로 수리할 수 있습니다.\n\n"
                + durabilitySummary();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "victory", "방어 성공 · 정비 시간", body,
                    List.of("open_mayor", "open_manual"), List.of("마을 현황", "수리 안내"));
        }
    }

    public static void handleAction(ServerPlayer player, String action) {
        MinecraftServer server = player.level().getServer();
        if (server == null || action == null || action.isBlank()) return;

        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
            boolean repair = action.startsWith("repair:");
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring(repair ? 7 : 8));
            if (building != null) {
                String result = repair
                        ? VillageProgressionSystem.repair(player, building)
                        : VillageProgressionSystem.upgrade(player, building);
                player.sendSystemMessage(Component.literal("§6" + result));
                openBuilding(player, building);
            }
            return;
        }

        switch (action) {
            case "open_manual" -> openManual(player);
            case "open_mayor" -> openMayor(player);
            case "advance_time" -> player.sendSystemMessage(Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            case "vote_yes" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, true)));
            case "vote_no" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, false)));
            case "chat_ready" -> broadcastQuick(server, player, "준비 완료. 시간 진행 가능합니다.");
            case "chat_gate" -> broadcastQuick(server, player, "전원 북쪽 성문으로 집결!");
            case "chat_repair" -> broadcastQuick(server, player, "손상 시설 확인 후 수리 바랍니다.");
            case "chat_help" -> broadcastQuick(server, player, "지원 요청! 제 위치로 모여 주세요.");
            case "buy_arrows" -> actAndReopen(player, VillageProgressionSystem.buyArrows(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_food" -> actAndReopen(player, VillageProgressionSystem.buyFood(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "forge_upgrade" -> actAndReopen(player, VillageProgressionSystem.improveForgeRank(player), VillageProgressionSystem.Building.SMITHY);
            case "skill_learn" -> actAndReopen(player, VillageProgressionSystem.learnNextSkill(player), VillageProgressionSystem.Building.SKILL_HALL);
            case "use_infirmary" -> actAndReopen(player, VillageProgressionSystem.useInfirmary(player), VillageProgressionSystem.Building.INFIRMARY);
            case "train" -> actAndReopen(player, VillageProgressionSystem.train(player), VillageProgressionSystem.Building.BARRACKS);
            case "restart_previous" -> VillageProgressionSystem.resetForRestart(server, false);
            case "restart_start" -> VillageProgressionSystem.resetForRestart(server, true);
            default -> player.sendSystemMessage(Component.literal("§c알 수 없는 마을 UI 동작입니다."));
        }
    }

    private static void fillBuildingActions(VillageProgressionSystem.Building building, List<String> actions, List<String> labels) {
        switch (building) {
            case TOWN_HALL -> add(actions, labels, "open_mayor", "마을 운영 화면", "advance_time", "시간 진행");
            case WALLS -> add(actions, labels, "repair:walls", "손상 수리", "upgrade:walls", "성문·성벽 강화");
            case SMITHY -> add(actions, labels, "forge_upgrade", "개인 장비 강화", "repair:smithy", "손상 수리", "upgrade:smithy", "대장간 증축");
            case SKILL_HALL -> add(actions, labels, "skill_learn", "새 전투 기술 습득", "repair:skill_hall", "손상 수리", "upgrade:skill_hall", "스킬관 증축");
            case INFIRMARY -> add(actions, labels, "use_infirmary", "치료받기", "repair:infirmary", "손상 수리", "upgrade:infirmary", "의무소 증축");
            case STOREHOUSE -> add(actions, labels, "buy_arrows", "화살 묶음 구매", "buy_food", "전투 식량 구매", "repair:storehouse", "손상 수리", "upgrade:storehouse", "창고·상점 증축");
            case BARRACKS -> add(actions, labels, "train", "전투 훈련", "repair:barracks", "손상 수리", "upgrade:barracks", "병영 증축");
        }
    }

    private static String description(ServerPlayer player, VillageProgressionSystem.Building building, boolean usable) {
        if (!usable) return "§c시설이 완전히 파괴되었습니다. 수리 전까지 모든 기능이 잠깁니다.";
        return switch (building) {
            case TOWN_HALL -> "§f중앙 종에서 마을 현황, 빠른 신호, 낮·밤 전환을 관리합니다.";
            case WALLS -> "§f적은 북쪽 성문만 공격하며, 내구도가 0이면 성문 구간이 한 번에 무너집니다.";
            case SMITHY -> "§f개인 장비 강화 " + VillageProgressionSystem.forgeRank(player) + " / " + VillageProgressionSystem.MAX_PERSONAL_RANK;
            case SKILL_HALL -> "§f전투 기술 " + VillageProgressionSystem.skillRank(player) + " / " + VillageProgressionSystem.MAX_PERSONAL_RANK;
            case INFIRMARY -> "§f즉시 치료와 웨이브 사이 회복을 담당합니다.";
            case STOREHOUSE -> "§f첫 지급 이후 화살과 식량은 수호 주화로 구매합니다.";
            case BARRACKS -> "§f훈련으로 RPG 경험치를 얻고 역할 스킬 효율을 강화합니다.";
        };
    }

    private static String durabilitySummary() {
        StringBuilder text = new StringBuilder();
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            text.append(building.displayName()).append(' ')
                    .append(VillageProgressionSystem.durabilityText(building))
                    .append(VillageProgressionSystem.isOperational(building) ? "" : " §c[파괴]")
                    .append('\n');
        }
        return text.toString();
    }

    private static void actAndReopen(ServerPlayer player, String result, VillageProgressionSystem.Building building) {
        player.sendSystemMessage(Component.literal("§e" + result));
        openBuilding(player, building);
    }

    private static void broadcastQuick(MinecraftServer server, ServerPlayer player, String text) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§b[빠른 신호] §f" + player.getGameProfile().name() + ": " + text), false);
    }

    private static void add(List<String> actions, List<String> labels, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            actions.add(pairs[i]);
            labels.add(pairs[i + 1]);
        }
    }

    private static void send(ServerPlayer player, String screenId, String title, String body,
                             List<String> actions, List<String> labels) {
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                screenId, title, body, String.join(SEP, actions), String.join(SEP, labels)));
    }
}
