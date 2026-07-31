package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class VillageUiService {
    private static final String SEP = "\u001F";

    private VillageUiService() {
    }

    public static void openDashboard(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        boolean multiplayer = server.getPlayerList().getPlayerCount() > 1;
        String body = "§6제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "\n"
                + VillageCouncilState.status(server, player) + "\n"
                + VillageProgressionSystem.status(player) + "\n"
                + VillageRaidSystem.status() + "\n\n"
                + "§7북문 " + VillageProgressionSystem.durabilityText(VillageProgressionSystem.Building.WALLS)
                + " | 회관 " + VillageProgressionSystem.durabilityText(VillageProgressionSystem.Building.TOWN_HALL);
        send(player, "dashboard", "마을 수호단 운영", body,
                List.of("advance_time", "open_status", "chat_ready", "chat_gate", "chat_repair", "chat_help"),
                List.of(multiplayer ? "낮·밤 전환 투표" : "낮·밤 전환",
                        "내 상태·역할",
                        "준비 완료 신호",
                        "북문 집결 신호",
                        "수리 요청 신호",
                        "지원 요청 신호"));
    }

    public static void openMayor(ServerPlayer player) {
        openDashboard(player);
    }

    public static void openManual(ServerPlayer player) {
        openDashboard(player);
    }

    public static void openPlayerStatus(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        String body = "§6내 상태\n"
                + "§f" + VillageCouncilState.rpgStatus(player) + "\n"
                + "§f역할: " + (role == null ? "미선택" : role.displayName()) + "\n"
                + "§e수호 주화: " + VillageProgressionSystem.coins(player) + "\n"
                + "§f장비 강화: " + VillageProgressionSystem.forgeRank(player) + " / "
                + VillageProgressionSystem.MAX_PERSONAL_RANK + "\n"
                + "§f능력 습득: " + VillageProgressionSystem.skillRank(player) + " / "
                + VillageProgressionSystem.MAX_PERSONAL_RANK + "\n\n"
                + "§7역할은 언제든 바꿀 수 있으며 역할 스킬은 /vg skill 또는 상태창에서 사용합니다.";
        send(player, "status", "상태·역할", body,
                List.of("use_skill", "role:guard_captain", "role:builder", "role:quartermaster",
                        "role:scout", "role:steward", "role:medic"),
                List.of("역할 스킬 사용", "수비대장", "건축가", "보급관", "정찰병", "관리관", "의무병"));
    }

    public static void openVoteForAll(MinecraftServer server, String proposerName) {
        String body = "§e" + proposerName + " 님이 다음 시간 단계로 진행하는 투표를 열었습니다.\n"
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
                    List.of("open_dashboard", "open_status"), List.of("마을 운영", "내 상태"));
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

        if (action.startsWith("role:")) {
            VillageRole.parse(action.substring(5)).ifPresentOrElse(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageCouncilState.chooseRole(player, role)));
                openPlayerStatus(player);
            }, () -> player.sendSystemMessage(Component.literal("§c알 수 없는 역할입니다.")));
            return;
        }

        switch (action) {
            case "open_dashboard", "open_mayor", "open_manual" -> openDashboard(player);
            case "open_status" -> openPlayerStatus(player);
            case "advance_time" -> player.sendSystemMessage(Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            case "vote_yes" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, true)));
            case "vote_no" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, false)));
            case "chat_ready" -> broadcastQuick(server, player, "준비 완료. 시간 진행 가능합니다.");
            case "chat_gate" -> broadcastQuick(server, player, "전원 북쪽 성문으로 집결!");
            case "chat_repair" -> broadcastQuick(server, player, "손상 시설 확인 후 수리 바랍니다.");
            case "chat_help" -> broadcastQuick(server, player, "지원 요청! 제 위치로 모여 주세요.");
            case "claim_bread" -> actAndReopen(player, VillageProgressionSystem.claimDailyBread(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_arrows" -> actAndReopen(player, VillageProgressionSystem.buyArrows(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_food" -> actAndReopen(player, VillageProgressionSystem.buyFood(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "forge_upgrade" -> actAndReopen(player, VillageProgressionSystem.improveForgeRank(player), VillageProgressionSystem.Building.SMITHY);
            case "skill_learn" -> actAndReopen(player, VillageProgressionSystem.learnNextSkill(player), VillageProgressionSystem.Building.SKILL_HALL);
            case "use_skill" -> {
                player.sendSystemMessage(Component.literal("§b" + VillageRpgSystem.useRoleSkill(player)));
                openPlayerStatus(player);
            }
            case "use_infirmary" -> actAndReopen(player, VillageProgressionSystem.useInfirmary(player), VillageProgressionSystem.Building.INFIRMARY);
            case "train" -> actAndReopen(player, VillageProgressionSystem.train(player), VillageProgressionSystem.Building.BARRACKS);
            case "restart_previous" -> VillageProgressionSystem.resetForRestart(server, false);
            case "restart_start" -> VillageProgressionSystem.resetForRestart(server, true);
            default -> player.sendSystemMessage(Component.literal("§c알 수 없는 마을 UI 동작입니다."));
        }
    }

    private static void fillBuildingActions(VillageProgressionSystem.Building building, List<String> actions, List<String> labels) {
        switch (building) {
            case TOWN_HALL -> add(actions, labels, "open_dashboard", "마을 운영 화면", "open_status", "내 상태·역할", "advance_time", "시간 진행");
            case WALLS -> add(actions, labels, "repair:walls", "손상 수리", "upgrade:walls", "북문·성벽 강화");
            case SMITHY -> add(actions, labels, "forge_upgrade", "개인 장비 강화", "repair:smithy", "손상 수리", "upgrade:smithy", "대장간 증축");
            case SKILL_HALL -> add(actions, labels, "skill_learn", "새 능력 습득", "use_skill", "역할 스킬 사용", "repair:skill_hall", "손상 수리", "upgrade:skill_hall", "연구소 증축");
            case INFIRMARY -> add(actions, labels, "use_infirmary", "치료받기", "repair:infirmary", "손상 수리", "upgrade:infirmary", "의무소 증축");
            case STOREHOUSE -> add(actions, labels, "claim_bread", "오늘의 빵 받기", "buy_arrows", "화살 묶음 구매", "buy_food", "전투 식량 구매", "repair:storehouse", "손상 수리", "upgrade:storehouse", "상점·보급소 증축");
            case BARRACKS -> add(actions, labels, "train", "전투 훈련", "repair:barracks", "손상 수리", "upgrade:barracks", "병영 증축");
        }
    }

    private static String description(ServerPlayer player, VillageProgressionSystem.Building building, boolean usable) {
        if (!usable) return "§c시설이 완전히 파괴되었습니다. 잔해만 남아 있으며 수리 전까지 모든 기능이 잠깁니다.";
        return switch (building) {
            case TOWN_HALL -> "§f마을 현황, 빠른 신호, 낮·밤 전환과 개인 상태를 관리합니다.";
            case WALLS -> "§f적은 북쪽 성문으로 진입하며, 내구도가 0이면 북문이 무너집니다.";
            case SMITHY -> "§f아이템과 장비를 강화합니다. 내 강화 " + VillageProgressionSystem.forgeRank(player) + " / " + VillageProgressionSystem.MAX_PERSONAL_RANK;
            case SKILL_HALL -> "§f스킬·마법·특수능력을 배웁니다. 내 능력 " + VillageProgressionSystem.skillRank(player) + " / " + VillageProgressionSystem.MAX_PERSONAL_RANK;
            case INFIRMARY -> "§f즉시 치료와 웨이브 사이 회복을 담당합니다.";
            case STOREHOUSE -> "§f매일 빵 3개부터 지급하며 강화할수록 식량과 상점 물량이 늘어납니다.";
            case BARRACKS -> "§f훈련으로 XP를 얻고 역할 스킬 효율을 강화합니다.";
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
