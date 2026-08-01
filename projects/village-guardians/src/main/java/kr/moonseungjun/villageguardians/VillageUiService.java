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
        String body = "§6제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "\n"
                + "§f공동 보급품: " + VillageProgressionSystem.supplies() + "\n"
                + "§f" + VillageRaidSystem.status() + "\n"
                + "§b" + VillageDefenseSystem.status(server.overworld()) + "\n\n"
                + "§e시설 내구도\n" + durabilitySummary()
                + "\n§7회관에서는 시설의 수리·강화만 관리합니다."
                + "\n§7구매·훈련·치료·연구·장비 강화는 각 시설에서 직접 이용하세요.";
        send(player, "dashboard", "마을 회관 · 시설 관리", body,
                List.of(
                        "manage:town_hall", "manage:walls", "manage:smithy",
                        "manage:skill_hall", "manage:storehouse", "manage:barracks",
                        "manage:infirmary", "open_status", "open_quick_chat"),
                List.of(
                        "회관 상태·수리", "성벽·방어탑 관리", "대장간 수리·강화",
                        "연구소 수리·강화", "보급소 수리·강화", "병영 수리·강화",
                        "의무소 수리·강화", "내 상태·역할", "회의·빠른 신호"));
    }

    public static void openQuickChat(ServerPlayer player) {
        String body = "§b마을 수호단 빠른 신호\n"
                + "§f호출기를 들고 우클릭하면 이 화면을 언제든 열 수 있습니다.\n"
                + "§f신호를 선택한 뒤 내용을 확인하고 실행하세요.\n\n"
                + "§7호출기에는 시설 관리나 시간 전환 기능이 없습니다.";
        send(player, "quick_chat", "빠른 신호", body,
                List.of("chat_ready", "chat_gate", "chat_repair", "chat_help"),
                List.of("준비 완료", "북문 집결", "수리 요청", "지원 요청"));
    }

    public static void openMayor(ServerPlayer player) {
        openDashboard(player);
    }

    public static void openManual(ServerPlayer player) {
        openQuickChat(player);
    }

    public static void openPlayerStatus(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        String body = "§6내 상태\n"
                + "§f" + VillageCouncilState.rpgStatus(player) + "\n"
                + "§f역할: " + (role == null ? "미선택" : role.displayName()) + "\n"
                + "§e수호 주화: " + VillageProgressionSystem.coins(player) + "\n"
                + "§f장비 강화: " + VillageProgressionSystem.forgeRank(player) + " / "
                + VillageProgressionSystem.MAX_PERSONAL_RANK + "\n"
                + "§f연구 능력: " + VillageProgressionSystem.skillRank(player) + " / "
                + VillageProgressionSystem.MAX_PERSONAL_RANK + "\n"
                + "§b스킬 포인트: " + VillageSkillTreeSystem.availablePoints(player)
                + " / " + VillageSkillTreeSystem.earnedPoints(player) + "\n\n"
                + "§d전투 기술\n§f" + VillageCombatTechniqueSystem.unlockSummary(player) + "\n\n"
                + "§a역할 스킬: 기본 R키"
                + "\n§7설정 → 조작키 → 마을 지키기에서 단축키를 변경할 수 있습니다."
                + "\n§7역할을 선택하면 상세 효과를 확인한 뒤 한 번 더 확정합니다.";
        send(player, "status", "상태·역할", body,
                List.of(
                        "open_skill_tree", "return_village",
                        "role_info:guard_captain", "role_info:ranger",
                        "role_info:engineer", "role_info:medic"),
                List.of(
                        "전술 발전 트리", "마을로 귀환",
                        "수비대장 상세", "성벽 궁수 상세",
                        "공병대장 상세", "의무관 상세"));
    }

    public static void openRolePreview(ServerPlayer player, VillageRole role) {
        boolean selected = VillageCouncilState.roleOf(player.getUUID()).orElse(null) == role;
        String body = "§6" + role.displayName() + "\n\n"
                + "§f" + role.overview() + "\n\n"
                + "§b상시 효과\n§f" + role.passive() + "\n\n"
                + "§dR키 역할 스킬\n§f" + role.active() + "\n\n"
                + "§e추천 위치\n§f" + role.recommended()
                + (selected ? "\n\n§a현재 선택한 역할입니다." : "\n\n§7선택 버튼을 누르면 확인 창이 한 번 더 열립니다.");
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (!selected) {
            actions.add("select_role:" + role.id());
            labels.add(role.displayName() + "으로 변경");
        }
        actions.add("open_status");
        labels.add("역할 목록으로 돌아가기");
        send(player, "role_preview", role.displayName(), body, actions, labels);
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
                + " | 획득 " + VillageSkillTreeSystem.earnedPoints(player)
                + " | 사용 " + VillageSkillTreeSystem.spentPoints(player)
                + " · 노드 선택 → 상세 확인 → 습득 확정";
        send(player, "skill_tree", "전술 발전 트리", body, actions, labels);
    }

    public static void openVoteForAll(MinecraftServer server, String proposerName) {
        String body = "§e" + proposerName + " 님이 중앙 종을 사용해 다음 시간 단계 투표를 열었습니다.\n"
                + "§f현재 제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "입니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "vote", "마을 시간 진행 투표", body,
                    List.of("vote_yes", "vote_no"), List.of("찬성", "반대"));
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
                ? "고정 핵심 시설"
                : "Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL;
        String body = "§6" + building.displayName() + " 관리\n"
                + "§f등급: " + levelText + "\n"
                + "§f내구도: " + current + " / " + maximum + "\n"
                + "§f공동 보급품: " + VillageProgressionSystem.supplies() + "\n\n"
                + "§b현재 효과\n§f" + managementEffect(building, level, server) + "\n\n"
                + (missing > 0
                ? "§e완전 수리 비용: 보급품 " + repairCost + "\n"
                : "§a현재 완전한 상태입니다.\n")
                + (building == VillageProgressionSystem.Building.TOWN_HALL
                ? "§7회관은 별도 레벨 강화 없이 수리만 가능합니다."
                : level >= VillageProgressionSystem.MAX_BUILDING_LEVEL
                ? "§a최고 강화 단계입니다."
                : "§e다음 강화 비용: 보급품 " + upgradeCost + "\n§f다음 효과: "
                + managementEffect(building, level + 1, server));

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
        labels.add("시설 관리 목록으로");
        send(player, "management", building.displayName() + " 관리", body, actions, labels);
    }

    public static void openBuilding(ServerPlayer player, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.TOWN_HALL) {
            openDashboard(player);
            return;
        }
        boolean usable = VillageProgressionSystem.isOperational(building);
        String body = "§6" + building.displayName() + "\n"
                + "§f이 화면은 해당 시설의 현장 기능만 제공합니다.\n"
                + "§7수리와 시설 강화는 회관의 시설 관리에서 진행합니다.\n\n"
                + "§f시설 레벨: " + VillageProgressionSystem.level(building) + " / "
                + VillageProgressionSystem.MAX_BUILDING_LEVEL + "\n"
                + "§f내구도: " + VillageProgressionSystem.durabilityText(building) + "\n"
                + "§e내 수호 주화: " + VillageProgressionSystem.coins(player) + "\n\n"
                + localDescription(player, building, usable);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (!usable) {
            actions.add("open_dashboard");
            labels.add("회관에서 수리하기");
        } else {
            fillLocalBuildingActions(player, building, actions, labels);
            actions.add("open_dashboard");
            labels.add("회관 시설 관리");
        }
        send(player, "building", building.displayName() + " · 현장 기능", body, actions, labels);
    }

    public static void openGameOverForAll(MinecraftServer server) {
        String body = "§c마을의 모든 핵심 건물이 파괴되었습니다.\n\n"
                + "§f이전 날부터 시작하면 현재 성장과 강화를 유지한 채 시설을 복구합니다.\n"
                + "§f처음부터 시작하면 마을 발전, 공동 보급품, 개인 강화 단계를 초기화합니다.\n"
                + "§7두 선택 모두 확인 창을 거치며 플레이어 소지품은 사라지지 않습니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "game_over", "마을 방어 실패", body,
                    List.of("restart_previous", "restart_start"),
                    List.of("이전 날부터 다시", "처음부터 다시"));
        }
    }

    public static void openRepairSummaryForAll(MinecraftServer server) {
        String body = "§a야간 습격을 막아냈습니다.\n"
                + "§f회관의 시설 관리 화면에서 손상된 시설을 수리하고 강화할 수 있습니다.\n\n"
                + durabilitySummary();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "victory", "방어 성공 · 정비 시간", body,
                    List.of("open_dashboard", "open_status"), List.of("시설 관리", "내 상태"));
        }
    }

    public static void handleAction(ServerPlayer player, String action) {
        MinecraftServer server = player.level().getServer();
        if (server == null || action == null || action.isBlank()) {
            return;
        }

        if (action.startsWith("manage:")) {
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring("manage:".length()));
            if (building != null) {
                openFacilityManagement(player, building);
            }
            return;
        }
        if (action.startsWith("building:")) {
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring("building:".length()));
            if (building != null) {
                openBuilding(player, building);
            }
            return;
        }
        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
            boolean repair = action.startsWith("repair:");
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
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
            VillageRole.parse(action.substring("role_info:".length()))
                    .ifPresent(role -> openRolePreview(player, role));
            return;
        }
        if (action.startsWith("select_role:")) {
            VillageRole.parse(action.substring("select_role:".length())).ifPresentOrElse(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageCouncilState.chooseRole(player, role)));
                openRolePreview(player, role);
            }, () -> player.sendSystemMessage(Component.literal("§c알 수 없는 역할입니다.")));
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
            case "advance_time" -> player.sendSystemMessage(Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            case "vote_yes" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, true)));
            case "vote_no" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, false)));
            case "chat_ready" -> broadcastQuick(server, player, "준비 완료. 시간 진행 가능합니다.");
            case "chat_gate" -> broadcastQuick(server, player, "전원 북쪽 성문으로 집결!");
            case "chat_repair" -> broadcastQuick(server, player, "손상 시설 확인 후 회관에서 수리 바랍니다.");
            case "chat_help" -> broadcastQuick(server, player, "지원 요청! 제 위치로 모여 주세요.");
            case "claim_bread" -> actAndReopen(player, VillageProgressionSystem.claimDailyBread(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_arrows" -> actAndReopen(player, VillageProgressionSystem.buyArrows(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_food" -> actAndReopen(player, VillageProgressionSystem.buyFood(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "sell_loot" -> actAndReopen(player, VillageTradingSystem.sellMonsterDrops(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "forge_upgrade" -> actAndReopen(player, VillageProgressionSystem.improveForgeRank(player), VillageProgressionSystem.Building.SMITHY);
            case "skill_learn" -> actAndReopen(player, VillageProgressionSystem.learnNextSkill(player), VillageProgressionSystem.Building.SKILL_HALL);
            case "use_skill" -> player.sendSystemMessage(Component.literal("§b" + VillageRpgSystem.useRoleSkill(player)));
            case "use_infirmary" -> actAndReopen(player, VillageProgressionSystem.useInfirmary(player), VillageProgressionSystem.Building.INFIRMARY);
            case "train" -> actAndReopen(player, VillageProgressionSystem.train(player), VillageProgressionSystem.Building.BARRACKS);
            case "hire_mercenary" -> actAndReopen(player, VillageDefenseSystem.hireMercenary(player), VillageProgressionSystem.Building.BARRACKS);
            case "defense_status" -> {
                player.sendSystemMessage(Component.literal("§b" + VillageDefenseSystem.status(server.overworld())));
                openBuilding(player, VillageProgressionSystem.Building.WALLS);
            }
            case "restart_previous" -> VillageProgressionSystem.resetForRestart(server, false);
            case "restart_start" -> VillageProgressionSystem.resetForRestart(server, true);
            default -> player.sendSystemMessage(Component.literal("§c알 수 없는 마을 UI 동작입니다."));
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
                        "open_skill_tree", "전술 발전 트리 열기",
                        "skill_learn", rank >= VillageProgressionSystem.MAX_PERSONAL_RANK
                                ? "연구 능력 최고 단계"
                                : "연구 능력 +" + (rank + 1) + " · 주화 " + cost);
            }
            case INFIRMARY -> add(actions, labels,
                    "use_infirmary", "즉시 치료받기");
            case STOREHOUSE -> {
                int arrows = 16 + VillageProgressionSystem.storehouseLevel() * 4;
                int food = 5 + VillageProgressionSystem.storehouseLevel() * 2;
                add(actions, labels,
                        "claim_bread", "오늘의 무료 빵 받기",
                        "buy_arrows", "화살 " + arrows + "개 · 주화 14",
                        "buy_food", "전투 식량 " + food + "개 · 주화 18",
                        "sell_loot", "몬스터 전리품 일괄 판매");
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
            return "§c시설이 파괴되어 기능을 사용할 수 없습니다. 회관에서 먼저 수리하세요.";
        }
        return switch (building) {
            case TOWN_HALL -> "§f시설의 수리와 강화 상태를 관리합니다.";
            case WALLS -> "§f성벽 레벨에 따라 네 모서리 방어탑이 자동 사격합니다.";
            case SMITHY -> "§f개인 장비 공격 보너스를 강화합니다. 현재 +" + VillageProgressionSystem.forgeRank(player);
            case SKILL_HALL -> "§f전술 발전 트리와 후반 전투 기술 연구를 담당합니다. 역할 스킬은 R키로 사용합니다.";
            case INFIRMARY -> "§f현재 체력을 즉시 회복합니다.";
            case STOREHOUSE -> "§f식량·화살 구매와 몬스터 전리품 판매를 담당합니다.";
            case BARRACKS -> "§f전투 훈련과 용병 고용을 담당합니다.";
        };
    }

    private static String managementEffect(
            VillageProgressionSystem.Building building,
            int level,
            MinecraftServer server) {
        int safeLevel = Math.max(0, Math.min(VillageProgressionSystem.MAX_BUILDING_LEVEL, level));
        return switch (building) {
            case TOWN_HALL -> "시설 관리와 마을 회의의 핵심 시설";
            case WALLS -> "최대 내구도 " + (1200 + safeLevel * 350)
                    + " · 마을 피해 감소 · 방어탑 화력 단계 " + safeLevel
                    + (server == null ? "" : " · " + VillageDefenseSystem.status(server.overworld()));
            case SMITHY -> "최대 내구도 " + (560 + safeLevel * 120)
                    + " · 장비 강화 피해 보정 " + (safeLevel * 4) + "%";
            case SKILL_HALL -> "최대 내구도 " + (520 + safeLevel * 110)
                    + " · 역할 스킬과 전투 기술 연구 기반 강화";
            case INFIRMARY -> "최대 내구도 " + (520 + safeLevel * 110)
                    + " · 즉시 치료량 " + Math.round((6.0f + safeLevel * 4.0f) / 2.0f) + "칸";
            case STOREHOUSE -> "최대 내구도 " + (560 + safeLevel * 120)
                    + " · 일일 빵 " + (3 + safeLevel * 2) + "개 · 습격 보상 증가";
            case BARRACKS -> "최대 내구도 " + (620 + safeLevel * 130)
                    + " · 훈련 XP " + (30 + safeLevel * 18)
                    + " · 기본 용병 정원 " + (1 + safeLevel / 2);
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

    private static void actAndReopen(
            ServerPlayer player,
            String result,
            VillageProgressionSystem.Building building) {
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
