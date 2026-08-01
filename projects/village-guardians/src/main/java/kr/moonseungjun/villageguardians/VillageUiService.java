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

        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        VillageRole currentRole = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        for (VillageRole role : VillageRole.values()) {
            actions.add("select_role:" + role.id());
            labels.add(String.join("|",
                    "role",
                    role.id(),
                    role.displayName(),
                    role.overview(),
                    role.passive(),
                    role.active(),
                    role.recommended(),
                    currentRole == role ? "current" : "available"));
        }

        for (VillageProgressionSystem.Building building
                : VillageProgressionSystem.Building.values()) {
            int level = VillageProgressionSystem.level(building);
            String levelText = building == VillageProgressionSystem.Building.TOWN_HALL
                    ? "핵심 시설"
                    : "Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL;
            actions.add("manage:" + building.id());
            labels.add(String.join("|",
                    "facility",
                    building.id(),
                    building.displayName(),
                    levelText,
                    Integer.toString(VillageProgressionSystem.durability(building)),
                    Integer.toString(VillageProgressionSystem.maxDurability(building)),
                    managementEffect(building, level, server)));
        }

        String body = "제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " · 공동 보급품 " + VillageProgressionSystem.supplies()
                + " · " + VillageRaidSystem.status();
        send(player, "town_hall", "마을 회관", body, actions, labels);
    }

    public static void openQuickChat(ServerPlayer player) {
        String body = "필요한 신호를 선택해 접속 중인 수호단에게 전송합니다.";
        send(player, "quick_chat", "수호단 통신", body,
                List.of("chat_ready", "chat_gate", "chat_repair", "chat_help"),
                List.of("준비 완료", "북문 집결", "시설 수리 요청", "현재 위치 지원 요청"));
    }

    public static void openMayor(ServerPlayer player) {
        openDashboard(player);
    }

    public static void openManual(ServerPlayer player) {
        openQuickChat(player);
    }

    public static void openPlayerStatus(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        String body = "§f" + VillageCouncilState.rpgStatus(player) + "\n"
                + "§f역할: " + (role == null ? "미선택" : role.displayName()) + "\n"
                + "§e수호 주화: " + VillageProgressionSystem.coins(player) + "\n"
                + "§b스킬 포인트: " + VillageSkillTreeSystem.availablePoints(player)
                + " / " + VillageSkillTreeSystem.earnedPoints(player) + "\n\n"
                + "§7역할 변경은 마을 회관에서만 가능합니다.";
        send(player, "inventory_actions", "수호자 메뉴", body,
                List.of("open_skill_tree", "return_village"),
                List.of("전술 발전 열기", "마을 광장으로 귀환"));
    }

    public static void openRolePreview(ServerPlayer player, VillageRole role) {
        openDashboard(player);
    }

    public static void openSkillTree(ServerPlayer player) {
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageSkillTreeSystem.Node node : VillageSkillTreeSystem.nodes()) {
            actions.add("skill_node:" + node.id());
            labels.add(node.title() + "|" + node.description() + "|"
                    + VillageSkillTreeSystem.nodeStatus(player, node));
        }
        String body = "사용 가능 " + VillageSkillTreeSystem.availablePoints(player)
                + "P · 획득 " + VillageSkillTreeSystem.earnedPoints(player)
                + "P · 사용 " + VillageSkillTreeSystem.spentPoints(player)
                + "P · 빈 공간 드래그로 이동";
        send(player, "skill_tree", "전술 발전", body, actions, labels);
    }

    public static void openVoteForAll(MinecraftServer server, String proposerName) {
        String body = proposerName + " 님이 다음 시간 단계 진행을 제안했습니다.\n"
                + "현재 제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "입니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "vote", "시간 진행 투표", body,
                    List.of("vote_yes", "vote_no"),
                    List.of("찬성", "반대"));
        }
    }

    public static void openFacilityManagement(
            ServerPlayer player,
            VillageProgressionSystem.Building building) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        int current = VillageProgressionSystem.durability(building);
        int maximum = VillageProgressionSystem.maxDurability(building);
        int level = VillageProgressionSystem.level(building);
        int missing = Math.max(0, maximum - current);
        int repairCost = Math.max(20, (missing + 7) / 8);
        int upgradeCost = VillageProgressionSystem.upgradeCost(level);
        boolean operational = VillageProgressionSystem.isOperational(building);

        String levelText = building == VillageProgressionSystem.Building.TOWN_HALL
                ? "핵심 시설"
                : "Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL;
        String body = "§f등급 " + levelText + "\n"
                + "§f내구도 " + current + " / " + maximum + "\n"
                + "§f공동 보급품 " + VillageProgressionSystem.supplies() + "\n\n"
                + "§b현재 효과\n§f" + managementEffect(building, level, server) + "\n\n"
                + (missing > 0
                ? "§e완전 수리 비용: 보급품 " + repairCost + "\n"
                : "§a현재 완전한 상태입니다.\n")
                + (building == VillageProgressionSystem.Building.TOWN_HALL
                ? "§7회관은 수리만 가능합니다."
                : level >= VillageProgressionSystem.MAX_BUILDING_LEVEL
                ? "§a최고 강화 단계입니다."
                : "§e다음 강화 비용: 보급품 " + upgradeCost + "\n"
                + "§f다음 효과: " + managementEffect(building, level + 1, server));

        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (missing > 0) {
            actions.add("repair:" + building.id());
            labels.add("완전 수리 · 보급품 " + repairCost);
        }
        if (building != VillageProgressionSystem.Building.TOWN_HALL
                && operational
                && level < VillageProgressionSystem.MAX_BUILDING_LEVEL) {
            actions.add("upgrade:" + building.id());
            labels.add("Lv." + (level + 1) + " 강화 · 보급품 " + upgradeCost);
        }
        actions.add("open_dashboard");
        labels.add("회관으로 돌아가기");
        send(player, "management", building.displayName() + " 관리",
                body, actions, labels);
    }

    public static void openBuilding(
            ServerPlayer player,
            VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.TOWN_HALL) {
            openDashboard(player);
            return;
        }

        boolean usable = VillageProgressionSystem.isOperational(building);
        String body = "§f시설 레벨 "
                + VillageProgressionSystem.level(building) + " / "
                + VillageProgressionSystem.MAX_BUILDING_LEVEL + "\n"
                + "§f내구도 " + VillageProgressionSystem.durabilityText(building) + "\n"
                + "§e내 수호 주화 " + VillageProgressionSystem.coins(player) + "\n\n"
                + localDescription(player, building, usable);

        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (!usable) {
            actions.add("open_dashboard");
            labels.add("회관에서 수리");
        } else {
            fillLocalBuildingActions(player, building, actions, labels);
            actions.add("open_dashboard");
            labels.add("회관 시설 관리");
        }
        send(player, "building", building.displayName(), body, actions, labels);
    }

    public static void openGameOverForAll(MinecraftServer server) {
        String body = "§c마을의 모든 핵심 건물이 파괴되었습니다.\n\n"
                + "§f이전 날부터 시작하면 현재 성장과 강화를 유지합니다.\n"
                + "§f처음부터 시작하면 마을 발전과 개인 성장을 초기화합니다.\n"
                + "§7플레이어 소지품은 사라지지 않습니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "game_over", "마을 방어 실패", body,
                    List.of("restart_previous", "restart_start"),
                    List.of("이전 날부터 다시", "처음부터 다시"));
        }
    }

    public static void openRepairSummaryForAll(MinecraftServer server) {
        String body = "§a야간 습격을 막아냈습니다.\n"
                + "§f회관에서 손상된 시설을 수리하고 강화할 수 있습니다.\n\n"
                + durabilitySummary();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "victory", "방어 성공", body,
                    List.of("open_dashboard", "open_skill_tree"),
                    List.of("회관 열기", "전술 발전 열기"));
        }
    }

    public static void handleAction(ServerPlayer player, String action) {
        MinecraftServer server = player.level().getServer();
        if (server == null || action == null || action.isBlank()) {
            return;
        }

        if (action.startsWith("manage:")) {
            VillageProgressionSystem.Building building =
                    VillageProgressionSystem.Building.fromId(
                            action.substring("manage:".length()));
            if (building != null) {
                openFacilityManagement(player, building);
            }
            return;
        }
        if (action.startsWith("building:")) {
            VillageProgressionSystem.Building building =
                    VillageProgressionSystem.Building.fromId(
                            action.substring("building:".length()));
            if (building != null) {
                openBuilding(player, building);
            }
            return;
        }
        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
            boolean repair = action.startsWith("repair:");
            VillageProgressionSystem.Building building =
                    VillageProgressionSystem.Building.fromId(
                            action.substring(repair ? 7 : 8));
            if (building != null) {
                String result = repair
                        ? VillageProgressionSystem.repair(player, building)
                        : VillageProgressionSystem.upgrade(player, building);
                player.sendSystemMessage(Component.literal("§6" + result));
                openFacilityManagement(player, building);
            }
            return;
        }
        if (action.startsWith("role_info:")) {
            openDashboard(player);
            return;
        }
        if (action.startsWith("select_role:")) {
            if (!VillageTownHallInteraction.isNearTownHall(player)) {
                player.sendSystemMessage(Component.literal(
                        "§c역할 변경은 마을 회관의 지휘대 근처에서만 가능합니다."));
                return;
            }
            VillageRole.parse(action.substring("select_role:".length()))
                    .ifPresentOrElse(role -> {
                        player.sendSystemMessage(Component.literal(
                                "§b" + VillageCouncilState.chooseRole(player, role)));
                        openDashboard(player);
                    }, () -> player.sendSystemMessage(
                            Component.literal("§c알 수 없는 역할입니다.")));
            return;
        }
        if (action.startsWith("skill_node:")) {
            String result = VillageSkillTreeSystem.purchase(
                    player,
                    action.substring("skill_node:".length()));
            player.sendSystemMessage(Component.literal("§b" + result));
            openSkillTree(player);
            return;
        }

        switch (action) {
            case "open_dashboard", "open_mayor" -> openDashboard(player);
            case "open_manual", "open_quick_chat" -> openQuickChat(player);
            case "open_status" -> openPlayerStatus(player);
            case "open_skill_tree" -> openSkillTree(player);
            case "return_village" -> player.sendSystemMessage(
                    Component.literal("§a" + VillageWorldSystem.returnToVillage(player)));
            case "advance_time" -> player.sendSystemMessage(
                    Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            case "vote_yes" -> player.sendSystemMessage(
                    Component.literal(VillageCouncilState.vote(player, true)));
            case "vote_no" -> player.sendSystemMessage(
                    Component.literal(VillageCouncilState.vote(player, false)));
            case "chat_ready" -> broadcastQuick(server, player,
                    "준비 완료. 시간 진행 가능합니다.");
            case "chat_gate" -> broadcastQuick(server, player,
                    "전원 북쪽 성문으로 집결!");
            case "chat_repair" -> broadcastQuick(server, player,
                    "손상 시설 확인 후 회관에서 수리 바랍니다.");
            case "chat_help" -> broadcastQuick(server, player,
                    "지원 요청! 제 위치로 모여 주세요.");
            case "claim_bread" -> actAndReopen(player,
                    VillageProgressionSystem.claimDailyBread(player),
                    VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_arrows" -> actAndReopen(player,
                    VillageProgressionSystem.buyArrows(player),
                    VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_food" -> actAndReopen(player,
                    VillageProgressionSystem.buyFood(player),
                    VillageProgressionSystem.Building.STOREHOUSE);
            case "sell_loot" -> actAndReopen(player,
                    VillageTradingSystem.sellMonsterDrops(player),
                    VillageProgressionSystem.Building.STOREHOUSE);
            case "forge_upgrade" -> actAndReopen(player,
                    VillageProgressionSystem.improveForgeRank(player),
                    VillageProgressionSystem.Building.SMITHY);
            case "skill_learn" -> actAndReopen(player,
                    VillageProgressionSystem.learnNextSkill(player),
                    VillageProgressionSystem.Building.SKILL_HALL);
            case "use_skill" -> player.sendSystemMessage(
                    Component.literal("§b" + VillageRpgSystem.useRoleSkill(player)));
            case "use_infirmary" -> actAndReopen(player,
                    VillageProgressionSystem.useInfirmary(player),
                    VillageProgressionSystem.Building.INFIRMARY);
            case "train" -> actAndReopen(player,
                    VillageProgressionSystem.train(player),
                    VillageProgressionSystem.Building.BARRACKS);
            case "hire_mercenary" -> actAndReopen(player,
                    VillageDefenseSystem.hireMercenary(player),
                    VillageProgressionSystem.Building.BARRACKS);
            case "defense_status" -> {
                player.sendSystemMessage(Component.literal(
                        "§b" + VillageDefenseSystem.status(server.overworld())));
                openBuilding(player, VillageProgressionSystem.Building.WALLS);
            }
            case "restart_previous" -> VillageProgressionSystem.resetForRestart(server, false);
            case "restart_start" -> VillageProgressionSystem.resetForRestart(server, true);
            default -> player.sendSystemMessage(
                    Component.literal("§c알 수 없는 마을 UI 동작입니다."));
        }
    }

    private static void fillLocalBuildingActions(
            ServerPlayer player,
            VillageProgressionSystem.Building building,
            List<String> actions,
            List<String> labels) {
        switch (building) {
            case TOWN_HALL -> {
            }
            case WALLS -> add(actions, labels,
                    "defense_status", "방어탑·용병 현황");
            case SMITHY -> {
                int rank = VillageProgressionSystem.forgeRank(player);
                int cost = 80 + rank * 100;
                add(actions, labels, "forge_upgrade",
                        rank >= VillageProgressionSystem.MAX_PERSONAL_RANK
                                ? "장비 강화 최고 단계"
                                : "장비 강화 +" + (rank + 1) + " · 주화 " + cost);
            }
            case SKILL_HALL -> {
                int rank = VillageProgressionSystem.skillRank(player);
                int cost = 100 + rank * 120;
                add(actions, labels,
                        "open_skill_tree", "전술 발전",
                        "skill_learn", rank >= VillageProgressionSystem.MAX_PERSONAL_RANK
                                ? "연구 능력 최고 단계"
                                : "연구 능력 +" + (rank + 1) + " · 주화 " + cost);
            }
            case INFIRMARY -> add(actions, labels,
                    "use_infirmary", "즉시 치료");
            case STOREHOUSE -> {
                int arrows = 16 + VillageProgressionSystem.storehouseLevel() * 4;
                int food = 5 + VillageProgressionSystem.storehouseLevel() * 2;
                add(actions, labels,
                        "claim_bread", "오늘의 무료 빵",
                        "buy_arrows", "화살 " + arrows + "개 · 주화 14",
                        "buy_food", "전투 식량 " + food + "개 · 주화 18",
                        "sell_loot", "몬스터 전리품 판매");
            }
            case BARRACKS -> {
                int xp = 30 + VillageProgressionSystem.barracksLevel() * 18;
                add(actions, labels,
                        "train", "전투 훈련 · XP " + xp,
                        "hire_mercenary", "용병 고용 · 철 24개");
            }
        }
    }

    private static String localDescription(
            ServerPlayer player,
            VillageProgressionSystem.Building building,
            boolean usable) {
        if (!usable) {
            return "§c시설이 파괴되어 기능을 사용할 수 없습니다. 회관에서 수리하세요.";
        }
        return switch (building) {
            case TOWN_HALL -> "§f역할 배치와 시설 수리·강화를 담당합니다.";
            case WALLS -> "§f성벽 단계에 따라 네 모서리 방어탑이 자동 사격합니다.";
            case SMITHY -> "§f개인 장비 공격 보너스를 강화합니다. 현재 +"
                    + VillageProgressionSystem.forgeRank(player);
            case SKILL_HALL -> "§f전술 발전과 후반 전투 기술 연구를 담당합니다.";
            case INFIRMARY -> "§f현재 체력을 즉시 회복합니다.";
            case STOREHOUSE -> "§f식량·화살 구매와 전리품 판매를 담당합니다.";
            case BARRACKS -> "§f전투 훈련과 용병 고용을 담당합니다.";
        };
    }

    private static String managementEffect(
            VillageProgressionSystem.Building building,
            int level,
            MinecraftServer server) {
        int safeLevel = Math.max(0,
                Math.min(VillageProgressionSystem.MAX_BUILDING_LEVEL, level));
        return switch (building) {
            case TOWN_HALL -> "역할 배치와 시설 관리의 핵심 시설";
            case WALLS -> "최대 내구도 " + (1200 + safeLevel * 350)
                    + " · 마을 피해 감소 · 방어탑 화력 " + safeLevel
                    + (server == null ? "" : " · "
                    + VillageDefenseSystem.status(server.overworld()));
            case SMITHY -> "최대 내구도 " + (560 + safeLevel * 120)
                    + " · 장비 강화 피해 보정 " + (safeLevel * 4) + "%";
            case SKILL_HALL -> "최대 내구도 " + (520 + safeLevel * 110)
                    + " · 역할 전술과 전투 기술 연구 기반 강화";
            case INFIRMARY -> "최대 내구도 " + (520 + safeLevel * 110)
                    + " · 즉시 치료량 "
                    + Math.round((6.0f + safeLevel * 4.0f) / 2.0f) + "칸";
            case STOREHOUSE -> "최대 내구도 " + (560 + safeLevel * 120)
                    + " · 일일 빵 " + (3 + safeLevel * 2) + "개 · 습격 보상 증가";
            case BARRACKS -> "최대 내구도 " + (620 + safeLevel * 130)
                    + " · 훈련 XP " + (30 + safeLevel * 18)
                    + " · 기본 용병 정원 " + (1 + safeLevel / 2);
        };
    }

    private static String durabilitySummary() {
        StringBuilder text = new StringBuilder();
        for (VillageProgressionSystem.Building building
                : VillageProgressionSystem.Building.values()) {
            text.append(building.displayName()).append(' ')
                    .append(VillageProgressionSystem.durabilityText(building))
                    .append(VillageProgressionSystem.isOperational(building)
                            ? "" : " §c[파괴]")
                    .append('\n');
        }
        return text.toString();
    }

    private static void actAndReopen(
            ServerPlayer player,
            String result,
            VillageProgressionSystem.Building building) {
        player.sendSystemMessage(Component.literal("§e" + result));
        openBuilding(player, building);
    }

    private static void broadcastQuick(
            MinecraftServer server,
            ServerPlayer player,
            String text) {
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§b[빠른 신호] §f"
                        + player.getGameProfile().name() + ": " + text), false);
    }

    private static void add(
            List<String> actions,
            List<String> labels,
            String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            actions.add(pairs[i]);
            labels.add(pairs[i + 1]);
        }
    }

    private static void send(
            ServerPlayer player,
            String screenId,
            String title,
            String body,
            List<String> actions,
            List<String> labels) {
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                screenId,
                title,
                body,
                String.join(SEP, actions),
                String.join(SEP, labels)));
    }
}
