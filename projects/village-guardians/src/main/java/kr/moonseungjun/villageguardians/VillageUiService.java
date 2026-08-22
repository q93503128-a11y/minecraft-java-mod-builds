package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class VillageUiService {
    private static final String SEP = "\u001F";

    private VillageUiService() {}

    public static void openDashboard(ServerPlayer player) {
        if (!requireTownHall(player, "마을 회관 기능은 회관 지휘대 근처에서만 사용할 수 있습니다.")) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        VillageRole currentRole = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        for (VillageRole role : VillageRole.values()) {
            actions.add("select_role:" + role.id());
            labels.add(String.join("|",
                    "role", role.id(), role.displayName(), role.overview(), role.passive(),
                    role.active(), role.recommended(), currentRole == role ? "current" : "available",
                    currentRole == role ? VillageRoleSkillSystem.loadoutSummary(player) : ""));
        }
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            int level = VillageProgressionSystem.level(building);
            String levelText = building == VillageProgressionSystem.Building.TOWN_HALL
                    ? "행정·보급" : "Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL;
            String action = switch (building) {
                case TOWN_HALL -> "open_funding";
                case WALLS -> "open_tower_control";
                default -> "manage:" + building.id();
            };
            actions.add(action);
            labels.add(String.join("|",
                    "facility", building.id(), building.displayName(), levelText,
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

    public static void openCallerMenu(ServerPlayer player) {
        openQuickChat(player);
    }

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

    public static void openMayor(ServerPlayer player) { openDashboard(player); }
    public static void openManual(ServerPlayer player) { openCallerMenu(player); }

    public static void openPlayerStatus(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        String body = "§b전투 상태\n§f" + VillageCouncilState.rpgStatus(player) + "\n\n"
                + "§b직업\n§f" + (role == null ? "미배치" : role.displayName()) + "\n\n"
                + "§b보유 재화\n§e수호 주화 " + VillageProgressionSystem.coins(player)
                + "\n§6공동 보급품 " + VillageProgressionSystem.supplies() + "\n\n"
                + "§b장착 기술\n§f" + (role == null ? "없음" : VillageRoleSkillSystem.loadoutSummary(player));
        send(player, "status", "수호자 상태", body, List.of(), List.of());
    }

    public static void openSkillTree(ServerPlayer player) {
        if (!requireSkillHall(player, "전술 발전은 기술·마법 연구소의 연구대 근처에서만 가능합니다.")) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageSkillTreeSystem.Node node : VillageSkillTreeSystem.nodes()) {
            actions.add("skill_node:" + node.id());
            labels.add(node.title() + "|" + node.description() + "|"
                    + VillageSkillTreeSystem.nodeStatus(player, node));
        }
        String body = "사용 가능 " + VillageSkillTreeSystem.availablePoints(player)
                + "P · 획득 " + VillageSkillTreeSystem.earnedPoints(player)
                + "P · 드래그 이동 · 휠 확대/축소";
        send(player, "skill_tree", "전술 발전", body, actions, labels);
    }

    public static void openRoleProgress(ServerPlayer player, VillageRole role) {
        VillageRole current = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (current != role) {
            player.sendSystemMessage(Component.literal("§c현재 배치된 직업의 성장 화면만 열 수 있습니다."));
            return;
        }
        if (!requireSkillHall(player, "직업 성장과 기술 장착은 기술·마법 연구소의 연구대 근처에서만 가능합니다.")) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRoleSkillSystem.RoleNode node : VillageRoleSkillSystem.RoleNode.values()) {
            actions.add("role_node:" + role.id() + ":" + node.id());
            labels.add(String.join("|",
                    "node", node.id(), node.branch().name().toLowerCase(), Integer.toString(node.tier()),
                    node.title(role), node.description(role), Integer.toString(node.requiredLevel()),
                    Integer.toString(node.coinCost()), VillageRoleSkillSystem.nodeStatus(player, role, node)));
        }
        for (VillageRoleSkillSystem.ActiveSkill skill : VillageRoleSkillSystem.skillsFor(role)) {
            int slot = VillageRoleSkillSystem.equippedSlot(player, skill);
            actions.add("role_skill_unlock:" + skill.id());
            labels.add(String.join("|",
                    "skill", skill.id(), skill.displayName(), skill.description(),
                    Integer.toString(skill.requiredLevel()), Integer.toString(skill.coinCost()),
                    VillageRoleSkillSystem.skillStatus(player, skill), Integer.toString(slot)));
        }
        String summary = "Lv." + VillageCouncilState.levelOf(player.getUUID())
                + " · 주화 " + VillageProgressionSystem.coins(player)
                + " · " + VillageRoleSkillSystem.loadoutSummary(player);
        send(player, "role_progress", role.displayName() + " 성장",
                role.id() + "|" + role.displayName() + "|" + summary, actions, labels);
    }

    public static void openRolePreview(ServerPlayer player, VillageRole role) { openDashboard(player); }

    public static void openTowerControl(ServerPlayer player) {
        if (!requireTownHall(player, "방어 지휘는 마을 회관 지휘대 근처에서만 가능합니다.")) return;
        VillageSiegeCommandUi.open(player);
    }

    public static void openFunding(ServerPlayer player) {
        if (!requireTownHall(player, "공동 보급 조달은 마을 회관 지휘대 근처에서만 가능합니다.")) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageFundingSystem.Bundle bundle : VillageFundingSystem.bundles()) {
            actions.add("funding:" + bundle.id());
            labels.add(bundle.displayName() + " · 주화 " + bundle.coinCost()
                    + "|공동 보급품 +" + bundle.supplies());
        }
        add(actions, labels, "open_dashboard", "회관으로 돌아가기|시설 수리·강화 화면으로 복귀");
        send(player, "funding", "공동 보급 조달",
                "개인 수호 주화를 공동 보급품으로 전환합니다.\n"
                        + "현재 내 주화 " + VillageProgressionSystem.coins(player)
                        + " · 공동 보급품 " + VillageProgressionSystem.supplies(), actions, labels);
    }

    public static void openEquipmentShop(ServerPlayer player) {
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageEquipmentShop.Offer offer : VillageEquipmentShop.offers()) {
            actions.add("gear:" + offer.id());
            labels.add(offer.displayName() + " · Lv." + offer.requiredLevel()
                    + " · " + offer.cost() + "주화|" + offer.effect()
                    + " · " + VillageEquipmentShop.status(player, offer));
        }
        send(player, "equipment_shop", "성장 장비 상점",
                "바닐라 장비 외형을 유지하며 레벨과 방어 일수에 따라 강한 상품이 해금됩니다.\n"
                        + "현재 Lv." + VillageCouncilState.levelOf(player.getUUID())
                        + " · 제 " + VillageCouncilState.currentDay() + "일 · 주화 "
                        + VillageProgressionSystem.coins(player), actions, labels);
    }

    public static void openMercenaryRoster(ServerPlayer player) {
        if (!requireManagementAccess(player, VillageProgressionSystem.Building.BARRACKS,
                "용병 명부는 병영 또는 마을 회관에서만 관리할 수 있습니다.")) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {
            int cost = VillageMercenarySystem.hireCost(kind);
            actions.add("hire_mercenary:" + kind.id());
            labels.add("고용 · " + kind.displayName() + " · " + cost + "주화|" + kind.description());
        }
        for (VillageMercenarySystem.RosterEntry entry : VillageMercenarySystem.rosterEntries(server)) {
            if (!entry.loaded()) continue;
            actions.add("retire_mercenary:" + entry.uuid());
            labels.add("퇴역 · " + entry.kind().displayName() + " Lv." + entry.level()
                    + "|누적 훈련 진척 " + entry.kills() + " · 환불 없음");
        }
        String body = VillageMercenarySystem.status(server)
                + " · 병영 Lv." + VillageProgressionSystem.barracksLevel()
                + " · 퇴역은 현재 로드된 용병만 가능";
        send(player, "mercenary_roster", "용병 명부", body, actions, labels);
    }

    public static void openVoteForAll(MinecraftServer server, String proposerName) {
        String body = proposerName + " 님이 다음 시간 단계 진행을 제안했습니다.\n현재 제 "
                + VillageCouncilState.currentDay() + "일 " + VillageCouncilState.currentPhase().koreanName() + "입니다.";
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send(player, "vote", "시간 진행 투표", body,
                    List.of("vote_yes", "vote_no"), List.of("찬성|다음 시간 단계 진행", "반대|현재 시간 유지"));
        }
    }

    public static void openFacilityManagement(ServerPlayer player, VillageProgressionSystem.Building building) {
        if (!requireManagementAccess(player, building,
                "시설 수리와 강화는 해당 시설 단말기 또는 마을 회관에서만 가능합니다.")) return;
        if (building == VillageProgressionSystem.Building.TOWN_HALL) {
            openFunding(player);
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        int current = VillageProgressionSystem.durability(building);
        int maximum = VillageProgressionSystem.maxDurability(building);
        int level = VillageProgressionSystem.level(building);
        int missing = Math.max(0, maximum - current);
        int repairCost = Math.max(20, (missing + 7) / 8);
        int upgradeCost = VillageProgressionSystem.upgradeCost(level);
        boolean operational = VillageProgressionSystem.isOperational(building);
        String body = "§b현재 시설\n§f등급 Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL
                + "\n§f내구도 " + current + " / " + maximum
                + "\n§6공동 보급품 " + VillageProgressionSystem.supplies()
                + " §e· 내 주화 " + VillageProgressionSystem.coins(player) + "\n\n"
                + "§b현재 효과\n§f" + managementEffect(building, level, server) + "\n\n"
                + "§b다음 작업\n"
                + (missing > 0 ? "§e완전 수리: 공동 보급품 " + repairCost + "\n" : "§a내구도 완전\n")
                + (level >= VillageProgressionSystem.MAX_BUILDING_LEVEL ? "§a최고 강화 단계"
                : "§eLv." + (level + 1) + " 강화: 공동 보급품 " + upgradeCost
                + "\n§f" + managementEffect(building, level + 1, server))
                + "\n\n§7보급품 조달은 마을 회관에서 진행합니다.";
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (missing > 0) add(actions, labels,
                "repair:" + building.id(), "완전 수리 · 보급품 " + repairCost + "|내구도를 최대치로 복구");
        if (operational && level < VillageProgressionSystem.MAX_BUILDING_LEVEL) {
            add(actions, labels,
                    "upgrade:" + building.id(), "Lv." + (level + 1) + " 강화 · 보급품 " + upgradeCost
                            + "|최대 내구도와 시설 고유 효과 상승");
        }
        add(actions, labels,
                "open_building:" + building.id(), "시설 기능으로 돌아가기|훈련·치료·상점 등 현장 기능",
                "open_funding", "회관 보급품 조달|개인 수호 주화를 공동 보급품으로 전환");
        if (building == VillageProgressionSystem.Building.WALLS) {
            add(actions, labels, "open_tower_control", "방어탑 지휘|포탑 전문화와 방어망 확인");
        }
        send(player, "management", building.displayName() + " 관리", body, actions, labels);
    }

    public static void openBuilding(ServerPlayer player, VillageProgressionSystem.Building building) {
        if (building == VillageProgressionSystem.Building.TOWN_HALL) { openDashboard(player); return; }
        boolean usable = VillageProgressionSystem.isOperational(building);
        String body = "§b시설 상태\n§f레벨 " + VillageProgressionSystem.level(building) + " / "
                + VillageProgressionSystem.MAX_BUILDING_LEVEL + "\n§f내구도 "
                + VillageProgressionSystem.durabilityText(building) + "\n§e내 수호 주화 "
                + VillageProgressionSystem.coins(player) + " §6· 공동 보급품 "
                + VillageProgressionSystem.supplies() + "\n\n" + localDescription(player, building, usable);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        add(actions, labels, "manage:" + building.id(),
                usable ? "시설 수리·강화|이 단말기에서 내구도와 시설 레벨 관리"
                        : "시설 수리|파괴된 시설을 공동 보급품으로 복구");
        if (usable) fillLocalBuildingActions(player, building, actions, labels);
        send(player, "building", building.displayName(), body, actions, labels);
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
        if ("facility_info".equals(action)) return;
        MinecraftServer server = player.level().getServer();
        if (server == null || action == null || action.isBlank()) return;

        if (action.startsWith("manage:")) {
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(action.substring(7));
            if (building != null) openFacilityManagement(player, building);
            return;
        }
        if (action.startsWith("open_building:")) {
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(action.substring(14));
            if (building != null) openBuilding(player, building);
            return;
        }
        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
            boolean repair = action.startsWith("repair:");
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring(repair ? 7 : 8));
            if (building == null || !requireManagementAccess(player, building,
                    "시설 수리와 강화는 해당 시설 단말기 또는 마을 회관에서만 가능합니다.")) return;
            player.sendSystemMessage(Component.literal("§6" + (repair
                    ? VillageProgressionSystem.repair(player, building)
                    : VillageProgressionSystem.upgrade(player, building))));
            openFacilityManagement(player, building);
            return;
        }
        if (action.startsWith("select_role:")) {
            if (!requireTownHall(player, "직업 배치는 마을 회관 지휘대 근처에서만 가능합니다.")) return;
            VillageRole.parse(action.substring(12)).ifPresentOrElse(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageCouncilState.chooseRole(player, role)));
                openDashboard(player);
            }, () -> player.sendSystemMessage(Component.literal("§c알 수 없는 직업입니다.")));
            return;
        }
        if (action.equals("open_role_progress_current")) {
            VillageCouncilState.roleOf(player.getUUID()).ifPresentOrElse(
                    role -> openRoleProgress(player, role),
                    () -> player.sendSystemMessage(Component.literal("§c회관에서 직업을 먼저 배치하세요.")));
            return;
        }
        if (action.startsWith("open_role_progress:")) {
            VillageRole.parse(action.substring(19)).ifPresent(role -> openRoleProgress(player, role));
            return;
        }
        if (action.startsWith("role_node:")) {
            String[] parts = action.split(":", 3);
            if (parts.length == 3) VillageRole.parse(parts[1]).ifPresent(role -> {
                String result = VillageRoleSkillSystem.purchaseNode(player, role, parts[2]);
                player.sendSystemMessage(Component.literal("§b" + result));
                openRoleProgress(player, role);
            });
            return;
        }
        if (action.startsWith("role_skill_unlock:")) {
            if (!requireSkillHall(player, "직업 기술 습득은 기술 연구소에서만 가능합니다.")) return;
            String id = action.substring(18);
            String result = VillageRoleSkillSystem.unlockSkill(player, id);
            player.sendSystemMessage(Component.literal("§b" + result));
            VillageCouncilState.roleOf(player.getUUID()).ifPresent(role -> openRoleProgress(player, role));
            return;
        }
        if (action.startsWith("role_skill_equip:")) {
            String[] parts = action.split(":", 3);
            if (parts.length == 3) {
                int slot;
                try { slot = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) { slot = 0; }
                player.sendSystemMessage(Component.literal("§b" + VillageRoleSkillSystem.equipSkill(player, parts[1], slot)));
                VillageCouncilState.roleOf(player.getUUID()).ifPresent(role -> openRoleProgress(player, role));
            }
            return;
        }
        if (action.startsWith("skill_node:")) {
            if (!requireSkillHall(player, "전술 발전은 기술 연구소에서만 가능합니다.")) return;
            player.sendSystemMessage(Component.literal("§b" + VillageSkillTreeSystem.purchase(player, action.substring(11))));
            openSkillTree(player);
            return;
        }
        if (action.startsWith("hire_mercenary:")) {
            if (!requireManagementAccess(player, VillageProgressionSystem.Building.BARRACKS,
                    "용병 고용은 병영 또는 마을 회관에서만 가능합니다.")) return;
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(16));
            player.sendSystemMessage(Component.literal("§b" + VillageMercenarySystem.hire(player, kind)));
            openMercenaryRoster(player);
            return;
        }
        if (action.startsWith("retire_mercenary:")) {
            if (!requireManagementAccess(player, VillageProgressionSystem.Building.BARRACKS,
                    "용병 퇴역은 병영 또는 마을 회관에서만 가능합니다.")) return;
            try {
                UUID uuid = UUID.fromString(action.substring(17));
                player.sendSystemMessage(Component.literal("§e" + VillageMercenarySystem.retire(player, uuid)));
            } catch (IllegalArgumentException ignored) {
                player.sendSystemMessage(Component.literal("§c잘못된 용병 식별자입니다."));
            }
            openMercenaryRoster(player);
            return;
        }

        if (action.startsWith("gear:")) {
            player.sendSystemMessage(Component.literal("§e" + VillageEquipmentShop.purchase(player, action.substring(5))));
            openEquipmentShop(player);
            return;
        }
        if (action.startsWith("funding:")) {
            if (!requireTownHall(player, "보급 조달은 마을 회관에서만 가능합니다.")) return;
            player.sendSystemMessage(Component.literal("§6" + VillageFundingSystem.purchase(player, action.substring(8))));
            openFunding(player);
            return;
        }
        if (action.startsWith("tower_open:") || action.startsWith("tower_branch:")
                || action.startsWith("tower_upgrade:")) {
            // Stale client actions are compatibility redirects only; retired fixed-tower progression cannot mutate state.
            VillageSiegeCommandUi.open(player);
            return;
        }
        if (action.startsWith("use_skill:")) {
            int slot;
            try { slot = Integer.parseInt(action.substring(10)); } catch (NumberFormatException ignored) { slot = 0; }
            player.sendSystemMessage(Component.literal("§b" + VillageRpgSystem.useRoleSkill(player, slot)));
            return;
        }

        switch (action) {
            case "open_dashboard", "open_mayor" -> openDashboard(player);
            case "open_manual", "open_caller_menu" -> openQuickChat(player);
            case "open_quick_chat" -> openQuickChat(player);
            case "open_status" -> openPlayerStatus(player);
            case "open_skill_tree" -> openSkillTree(player);
            case "open_tower_control" -> openTowerControl(player);
            case "open_funding" -> openFunding(player);
            case "open_equipment_shop" -> openEquipmentShop(player);
            case "return_village" -> player.sendSystemMessage(Component.literal("§a" + VillageWorldSystem.returnToVillage(player)));
            case "advance_time" -> player.sendSystemMessage(Component.literal(VillageCouncilState.proposeAdvanceTime(player)));
            case "vote_yes" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, true)));
            case "vote_no" -> player.sendSystemMessage(Component.literal(VillageCouncilState.vote(player, false)));
            case "chat_ready" -> broadcastQuick(server, player, "준비 완료. 시간 진행 가능합니다.");
            case "chat_gate" -> broadcastQuick(server, player, "전원 북쪽 성문으로 집결!");
            case "chat_repair" -> broadcastQuick(server, player, "손상 시설 확인 후 현장 단말기에서 수리 바랍니다.");
            case "chat_help" -> broadcastQuick(server, player, "지원 요청! 좌표 "
                    + player.blockPosition().getX() + ", " + player.blockPosition().getY() + ", "
                    + player.blockPosition().getZ() + "로 모여 주세요.");
            case "claim_bread" -> actAndReopen(player, () -> VillageProgressionSystem.claimDailyBread(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "buy_arrows" -> actAndReopen(player, () -> VillageProgressionSystem.buyArrows(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "sell_loot" -> actAndReopen(player, () -> VillageTradingSystem.sellMonsterDrops(player), VillageProgressionSystem.Building.STOREHOUSE);
            case "forge_upgrade" -> actAndReopen(player, () -> VillageProgressionSystem.improveForgeRank(player), VillageProgressionSystem.Building.SMITHY);
            case "skill_learn" -> actAndReopen(player, () -> VillageProgressionSystem.learnNextSkill(player), VillageProgressionSystem.Building.SKILL_HALL);
            case "use_skill" -> player.sendSystemMessage(Component.literal("§b" + VillageRpgSystem.useRoleSkill(player, 0)));
            case "use_infirmary" -> actAndReopen(player, () -> VillageProgressionSystem.useInfirmary(player), VillageProgressionSystem.Building.INFIRMARY);
            case "train" -> actAndReopen(player, () -> VillageProgressionSystem.train(player), VillageProgressionSystem.Building.BARRACKS);
            case "open_mercenary_roster", "hire_mercenary" -> openMercenaryRoster(player);
            case "tower_status" -> {
                player.sendSystemMessage(Component.literal("§b" + VillageDefenseSystem.status(server.overworld())));
                openTowerControl(player);
            }
            case "wall_status_local" -> {
                player.sendSystemMessage(Component.literal("§b성벽 "
                        + VillageProgressionSystem.durabilityText(VillageProgressionSystem.Building.WALLS)
                        + " · 이 단말기에서 수리·강화할 수 있습니다."));
                openBuilding(player, VillageProgressionSystem.Building.WALLS);
            }
            case "restart_previous" -> {
                if (!VillageProgressionSystem.isGameOver())
                    player.sendSystemMessage(Component.literal("§c방어 실패 상태에서만 전투 전 낮으로 되돌릴 수 있습니다."));
                else VillageProgressionSystem.resetForRestart(server, false);
            }
            case "restart_start" -> {
                if (!VillageProgressionSystem.isGameOver())
                    player.sendSystemMessage(Component.literal("§c방어 실패 상태에서만 처음부터 다시 시작할 수 있습니다."));
                else VillageProgressionSystem.resetForRestart(server, true);
            }
            default -> player.sendSystemMessage(Component.literal("§c알 수 없는 마을 UI 동작입니다."));
        }
    }

    private static void fillLocalBuildingActions(ServerPlayer player, VillageProgressionSystem.Building building,
                                                  List<String> actions, List<String> labels) {
        switch (building) {
            case TOWN_HALL -> {
            }
            case WALLS -> add(actions, labels,
                    "wall_status_local", "성벽 상태 확인|현재 내구도와 포탑 설치 상태 확인");
            case SMITHY -> {
                int rank = VillageProgressionSystem.forgeRank(player);
                int cost = 80 + rank * 100;
                add(actions, labels, "forge_upgrade", rank >= VillageProgressionSystem.MAX_PERSONAL_RANK
                        ? "장비 강화 최고 단계|추가 강화 불가"
                        : "장비 강화 +" + (rank + 1) + " · 주화 " + cost + "|개인 공격 보너스 상승");
            }
            case SKILL_HALL -> {
                int rank = VillageProgressionSystem.skillRank(player);
                int cost = 100 + rank * 120;
                add(actions, labels,
                        "open_role_progress_current", "직업 성장·기술 장착|세 갈래 성장과 두 기술 슬롯 관리",
                        "open_skill_tree", "공용 전술 발전|공격·방어·지원·사격 전술 연구",
                        "skill_learn", rank >= VillageProgressionSystem.MAX_PERSONAL_RANK
                                ? "연구 능력 최고 단계|추가 연구 불가"
                                : "연구 능력 +" + (rank + 1) + " · 주화 " + cost + "|기술 피해와 재사용 효율 강화");
            }
            case INFIRMARY -> add(actions, labels, "use_infirmary", "즉시 치료받기|시설 단계에 따라 체력 회복");
            case STOREHOUSE -> {
                int arrows = 16 + VillageProgressionSystem.storehouseLevel() * 4;
                int food = 5 + VillageProgressionSystem.storehouseLevel() * 2;
                add(actions, labels,
                        "claim_bread", "오늘의 무료 빵 받기|하루 한 번 식량 보급",
                        "buy_arrows", "화살 " + arrows + "개 · 주화 14|원거리 전투 보급",
                                                "sell_loot", "몬스터 전리품 일괄 판매|판매 가능한 전리품을 주화로 교환",
                        "open_equipment_shop", "성장 장비 상점|레벨과 방어 일수별 장비 구매");
            }
            case BARRACKS -> add(actions, labels,
                    "train", "전투 훈련 · XP " + (30 + VillageProgressionSystem.barracksLevel() * 18) + "|3분 재사용 대기시간",
                    "open_mercenary_roster", "용병 명부 · " + VillageMercenarySystem.rosterCount() + " / "
                            + VillageMercenarySystem.capacity() + "|4병과 고용·현재 용병 확인·개별 퇴역");
        }
    }

    private static String localDescription(ServerPlayer player, VillageProgressionSystem.Building building, boolean usable) {
        if (!usable) return "§c시설이 파괴됐습니다. 첫 번째 기능인 시설 수리를 선택해 복구하세요.";
        return switch (building) {
            case TOWN_HALL -> "§f시설 관리와 직업 배치, 공동 보급 조달을 담당합니다.";
            case WALLS -> "§f성벽 상태와 방어탑을 관리합니다. 첫 번째 기능에서 성벽을 수리·강화할 수 있습니다.";
            case SMITHY -> "§f장비 공격 보너스를 강화합니다. 첫 번째 기능에서 대장간 자체도 수리·강화할 수 있습니다. 현재 개인 강화 +"
                    + VillageProgressionSystem.forgeRank(player);
            case SKILL_HALL -> "§f공용 전술, 직업 성장, 기술 습득과 장착을 담당합니다. 첫 번째 기능에서 연구소 레벨을 관리합니다.";
            case INFIRMARY -> "§f즉시 치료를 제공합니다. 첫 번째 기능에서 의무소의 회복량과 내구도를 강화할 수 있습니다.";
            case STOREHOUSE -> "§f식량·화살·성장 장비 구매와 전리품 판매를 담당합니다. 첫 번째 기능에서 보급 성능을 강화합니다.";
            case BARRACKS -> "§f전투 훈련과 영구 용병 고용을 담당합니다. 첫 번째 기능에서 훈련 보상과 용병 정원을 강화합니다.";
        };
    }

    private static String managementEffect(VillageProgressionSystem.Building building, int level, MinecraftServer server) {
        int safe = Math.max(0, Math.min(VillageProgressionSystem.MAX_BUILDING_LEVEL, level));
        return switch (building) {
            case TOWN_HALL -> "직업 배치·시설 지휘·개인 주화 기반 공동 보급 조달";
            case WALLS -> "최대 내구도 " + (1200 + safe * 350) + " · 방어탑 설치 단계 " + safe
                    + (server == null ? "" : " · " + VillageDefenseSystem.status(server.overworld()));
            case SMITHY -> "최대 내구도 " + (560 + safe * 120) + " · 장비 강화 피해 보정 " + (safe * 4) + "%";
            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 기술 위력/지속 +" + (safe * 5) + "% · 공용 전술·직업 연구";
            case INFIRMARY -> "최대 내구도 " + (520 + safe * 110) + " · 즉시 치료량 "
                    + Math.round((6.0f + safe * 4.0f) / 2.0f) + "칸";
            case STOREHOUSE -> "최대 내구도 " + (560 + safe * 120) + " · 일일 식량·성장 장비·습격 보상 강화";
            case BARRACKS -> "최대 내구도 " + (620 + safe * 130) + " · 훈련 XP " + (30 + safe * 18)
                    + " · 용병 정원 " + (1 + safe / 2 + VillageDefenseResearchSystem.mercenaryCapacityBonus());
        };
    }

    private static String durabilitySummary() {
        StringBuilder text = new StringBuilder();
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            text.append(building.displayName()).append(' ').append(VillageProgressionSystem.durabilityText(building))
                    .append(VillageProgressionSystem.isOperational(building) ? "" : " §c[파괴]").append('\n');
        }
        return text.toString();
    }


    private static void actAndReopen(ServerPlayer player, Supplier<String> action,
                                     VillageProgressionSystem.Building building) {
        if (!VillageLocationRules.isNear(player, building)) {
            player.sendSystemMessage(Component.literal(
                    "§c이 기능은 " + building.displayName() + " 단말기 근처에서만 사용할 수 있습니다."));
            return;
        }
        player.sendSystemMessage(Component.literal("§e" + action.get()));
        openBuilding(player, building);
    }

    private static void broadcastQuick(MinecraftServer server, ServerPlayer player, String text) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "§b[빠른 신호] §f" + player.getGameProfile().name() + ": " + text), false);
    }

    private static boolean requireTownHall(ServerPlayer player, String message) {
        if (VillageLocationRules.isNearTownHall(player)) return true;
        player.sendSystemMessage(Component.literal("§c" + message));
        return false;
    }

    private static boolean requireSkillHall(ServerPlayer player, String message) {
        if (VillageLocationRules.isNearSkillHall(player)) return true;
        player.sendSystemMessage(Component.literal("§c" + message));
        return false;
    }

    private static boolean requireManagementAccess(
            ServerPlayer player,
            VillageProgressionSystem.Building building,
            String message) {
        if (VillageLocationRules.isNearTownHall(player) || VillageLocationRules.isNear(player, building)) return true;
        player.sendSystemMessage(Component.literal("§c" + message));
        return false;
    }

    private static void add(List<String> actions, List<String> labels, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            actions.add(pairs[i]);
            labels.add(pairs[i + 1]);
        }
    }

    private static void send(ServerPlayer player, String screenId, String title, String body,
                             List<String> actions, List<String> labels) {
        List<String> outputActions = new ArrayList<>(actions);
        List<String> outputLabels = new ArrayList<>(labels);
        if (usesFacilityInformation(screenId) && !body.isBlank() && !outputActions.contains("facility_info")) {
            outputActions.add(0, "facility_info");
            outputLabels.add(0, "시설 정보|현재 단계·내구도·고유 효과 확인");
        }
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                screenId, title, body, String.join(SEP, outputActions), String.join(SEP, outputLabels)));
    }

    private static boolean usesFacilityInformation(String screenId) {
        return screenId.equals("building") || screenId.equals("management") || screenId.equals("funding")
                || screenId.equals("tower_control") || screenId.equals("tower_detail")
                || screenId.equals("caller") || screenId.equals("relic_choice");
    }
}
