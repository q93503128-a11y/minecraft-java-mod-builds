package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the current menu contract. Legacy VillageUiService remains available for
 * older save-compatible actions, but new entry points are routed here first.
 */
public final class VillageUiController {
    private static final String SEP = "\u001F";

    private VillageUiController() {}

    public static void openDashboard(ServerPlayer player) {
        if (!requireTownHall(player, "마을 회관 지휘대 근처에서만 시설을 관리할 수 있습니다.")) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        VillageRole currentRole = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        for (VillageRole role : VillageRole.values()) {
            actions.add("select_role:" + role.id());
            labels.add(String.join("|", "role", role.id(), role.displayName(), role.overview(), role.passive(),
                    role.active(), role.recommended(), currentRole == role ? "current" : "available",
                    currentRole == role ? VillageRoleSkillSystem.loadoutSummary(player) : ""));
        }
        for (VillageProgressionSystem.Building building : VillageProgressionSystem.Building.values()) {
            int level = VillageProgressionSystem.level(building);
            int current = VillageProgressionSystem.durability(building);
            int maximum = VillageProgressionSystem.maxDurability(building);
            int missing = Math.max(0, maximum - current);
            int repairCost = missing <= 0 ? 0 : Math.max(20, (missing + 7) / 8);
            boolean canUpgrade = building != VillageProgressionSystem.Building.TOWN_HALL
                    && level < VillageProgressionSystem.MAX_BUILDING_LEVEL;
            int upgradeCost = canUpgrade ? VillageProgressionSystem.upgradeCost(level) : 0;
            String nextEffect = canUpgrade ? managementEffect(building, level + 1, server) : "";
            String levelText = building == VillageProgressionSystem.Building.TOWN_HALL
                    ? "행정·보급" : "Lv." + level + " / " + VillageProgressionSystem.MAX_BUILDING_LEVEL;
            actions.add("facility:" + building.id());
            labels.add(String.join("|", "facility", building.id(), building.displayName(), levelText,
                    Integer.toString(current), Integer.toString(maximum), managementEffect(building, level, server),
                    nextEffect, Integer.toString(upgradeCost), Integer.toString(repairCost)));
        }
        String body = "제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName()
                + " · 공동 보급품 " + VillageProgressionSystem.supplies()
                + " · " + VillageRaidSystem.status();
        send(player, "town_hall", "마을 회관", body, actions, labels);
    }

    public static void openCaller(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        String body = "제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + "\n"
                + "직업 " + (role == null ? "미배치" : role.displayName()) + " · 주화 "
                + VillageProgressionSystem.coins(player) + " · 보급품 " + VillageProgressionSystem.supplies() + "\n"
                + VillageRaidSystem.status() + "\n\n"
                + "단축키: H 상태 · J 성장 · K 직업 성장 · U 호출기 · B 빠른 통신 · Z/X 기술";
        send(player, "caller", "마을 수호단 호출기", body,
                List.of("open_status", "open_skill_tree", "open_role_progress_current",
                        "open_wave_intel", "open_quick_chat", "return_village"),
                List.of(
                        "상태 (H)|현재 전투 상태와 재화 확인",
                        "성장 (J)|공용 전술 성장 트리 바로 열기",
                        "직업 성장 (K)|현재 직업의 세 갈래 성장 확인",
                        "다음 웨이브 정보|예상 병과·특성·보스 정찰",
                        "빠른 통신 (B)|접속 중인 수호단에게 즉시 신호",
                        "마을 귀환|전투 중이 아닐 때 중앙 광장으로 귀환"));
    }

    public static void openStatus(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
        String body = "전투 상태  Lv." + progress.level() + " · " + progress.experience()
                + "/" + progress.experienceToNextLevel() + " XP · 장비 숙련 +"
                + VillageProgressionSystem.forgeRank(player) + " · 개인 연구 +"
                + VillageProgressionSystem.skillRank(player) + "\n"
                + "직업  " + (role == null ? "미배치" : role.displayName())
                + (role == null ? "" : " · " + VillageRoleSkillSystem.loadoutSummary(player)) + "\n"
                + "재화  주화 " + VillageProgressionSystem.coins(player)
                + " · 공동 보급품 " + VillageProgressionSystem.supplies() + "\n"
                + "마을  제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + " · " + VillageRaidSystem.status() + "\n"
                + "H 상태 · J 성장 · K 직업 성장 · U 호출기 · B 통신 · Z/X 기술";
        send(player, "status", "수호자 상태", body, List.of(), List.of());
    }

    public static void openPersonalProgress(ServerPlayer player) {
        openSkillTree(player);
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
                + "P · 드래그 이동 · 휠 확대/축소";
        send(player, "skill_tree", "성장", body, actions, labels);
    }

    public static void openRoleProgress(ServerPlayer player) {
        VillageCouncilState.roleOf(player.getUUID()).ifPresentOrElse(
                role -> openRoleProgress(player, role),
                () -> player.sendSystemMessage(Component.literal("§c마을 회관에서 직업을 먼저 배치하세요.")));
    }

    public static void openRoleProgress(ServerPlayer player, VillageRole role) {
        VillageRole current = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (current != role) {
            player.sendSystemMessage(Component.literal("§c현재 직업의 성장 화면만 열 수 있습니다."));
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRoleSkillSystem.RoleNode node : VillageRoleSkillSystem.nodes()) {
            actions.add("role_node:" + role.id() + ":" + node.id());
            labels.add(String.join("|", "node", node.id(), node.branch().name().toLowerCase(),
                    Integer.toString(node.tier()), node.title(role), node.description(role),
                    Integer.toString(node.requiredLevel()), Integer.toString(node.coinCost()),
                    VillageRoleSkillSystem.nodeStatus(player, role, node)));
        }
        for (VillageRoleSkillSystem.ActiveSkill skill : VillageRoleSkillSystem.skillsFor(role)) {
            int slot = VillageRoleSkillSystem.equippedSlot(player, skill);
            actions.add("role_skill_unlock:" + skill.id());
            labels.add(String.join("|", "skill", skill.id(), skill.displayName(), skill.description(),
                    Integer.toString(skill.requiredLevel()), Integer.toString(skill.coinCost()),
                    VillageRoleSkillSystem.skillStatus(player, skill), Integer.toString(slot)));
        }
        String summary = "Lv." + VillageCouncilState.levelOf(player.getUUID())
                + " · 주화 " + VillageProgressionSystem.coins(player)
                + " · " + VillageRoleSkillSystem.loadoutSummary(player)
                + " · 성장 노드는 어디서나 · 기술 습득은 연구소 · 장착은 어디서나";
        send(player, "role_progress", role.displayName() + " 성장",
                role.id() + "|" + role.displayName() + "|" + summary, actions, labels);
    }

    public static void openWaveIntel(ServerPlayer player) {
        send(player, "wave_intel", "다음 웨이브 정보",
                VillageWaveIntelSystem.report(), List.of(), List.of());
    }

    public static void openEquipmentShop(ServerPlayer player) {
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageEquipmentShop.Offer offer : VillageEquipmentShop.offers()) {
            String status = VillageEquipmentShop.status(player, offer);
            boolean available = "available".equals(status);
            actions.add("gear:" + offer.id());
            labels.add(String.join("|", "shop", offer.category().name().toLowerCase(), offer.displayName(),
                    "주화 " + offer.cost(), offer.effect() + " · 제 " + offer.requiredDay() + "일부터 판매",
                    available ? "구매 가능" : status, available ? "available" : "locked"));
        }
        int arrows = 16 + VillageProgressionSystem.storehouseLevel() * 4;
        int food = 5 + VillageProgressionSystem.storehouseLevel() * 2;
        addShop(actions, labels, "buy_arrows", "other", "화살 " + arrows + "개", "주화 14",
                "원거리 전투 보급", "구매 가능", true);
        addShop(actions, labels, "buy_food", "other", "전투 식량 " + food + "개", "주화 18",
                "허기 회복용 익힌 소고기", "구매 가능", true);
        addShop(actions, labels, "sell_loot", "other", "몬스터 전리품 판매", "일괄 정산",
                "주 인벤토리의 판매 가능 전리품만 안전하게 정산", "판매 가능", true);
        send(player, "equipment_shop", "상점",
                "레벨 제한 없이 구매할 수 있으며 강한 장비는 방어 일수에 따라 입고됩니다.\n"
                        + "현재 제 " + VillageCouncilState.currentDay() + "일 · 주화 "
                        + VillageProgressionSystem.coins(player), actions, labels);
    }

    public static void openBuilding(ServerPlayer player, VillageProgressionSystem.Building building) {
        if (building == null) return;
        if (building == VillageProgressionSystem.Building.TOWN_HALL) {
            openDashboard(player);
            return;
        }
        if (building == VillageProgressionSystem.Building.STOREHOUSE) {
            openEquipmentShop(player);
            return;
        }
        boolean usable = VillageProgressionSystem.isOperational(building);
        String body = building.displayName() + " · Lv." + VillageProgressionSystem.level(building)
                + " · 내구도 " + VillageProgressionSystem.durabilityText(building) + "\n"
                + (usable ? localDescription(player, building)
                : "시설이 파괴됐습니다. 마을 회관의 시설 수리 탭에서 복구하세요.");
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (usable) fillLocalActions(player, building, actions, labels);
        send(player, "building", building.displayName(), body, actions, labels);
    }

    public static void openMercenaryCommand(ServerPlayer player) {
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
            player.sendSystemMessage(Component.literal("§c병영이 파괴되어 용병을 고용할 수 없습니다."));
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {
            actions.add("hire_mercenary:" + kind.id());
            labels.add(kind.displayName() + " · 주화 " + VillageMercenarySystem.hireCost(kind)
                    + "|" + kind.description());
        }
        send(player, "building", "용병 지휘", VillageMercenarySystem.status(player.level().getServer()), actions, labels);
    }

    public static void openDefenseResearch(ServerPlayer player) {
        if (!VillageLocationRules.isNearSkillHall(player)) {
            player.sendSystemMessage(Component.literal("§c방어 연구는 기술 연구소 연구대 근처에서만 가능합니다."));
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageDefenseResearchSystem.Branch branch : VillageDefenseResearchSystem.Branch.values()) {
            int level = VillageDefenseResearchSystem.level(branch);
            int cost = VillageDefenseResearchSystem.upgradeCost(branch);
            actions.add("defense_research:" + branch.id());
            labels.add(branch.displayName() + " Lv." + level + "/" + VillageDefenseResearchSystem.MAX_LEVEL
                    + (level >= VillageDefenseResearchSystem.MAX_LEVEL ? "" : " · 주화 " + cost)
                    + "|" + branch.description(level));
        }
        send(player, "building", "마을 방어 연구", "용병·포탑·전리품 운용을 연구합니다.", actions, labels);
    }

    public static boolean handleAction(ServerPlayer player, String action) {
        if (action == null || action.isBlank()) return true;
        MinecraftServer server = player.level().getServer();
        if (server == null) return true;

        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
            boolean repair = action.startsWith("repair:");
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring(repair ? 7 : 8));
            if (building == null) return true;
            if (!VillageLocationRules.isNearTownHall(player) && !VillageLocationRules.isNear(player, building)) {
                player.sendSystemMessage(Component.literal(
                        "§c시설 수리와 강화는 해당 시설 단말기 또는 마을 회관에서만 가능합니다."));
                return true;
            }
            String result = repair ? VillageProgressionSystem.repair(player, building)
                    : VillageProgressionSystem.upgrade(player, building);
            player.sendSystemMessage(Component.literal("§6" + result));
            openDashboard(player);
            return true;
        }
        if (action.startsWith("manage:") || action.startsWith("facility:")) {
            openDashboard(player);
            return true;
        }
        if (action.startsWith("select_role:")) {
            if (!requireTownHall(player, "직업 배치는 마을 회관에서만 가능합니다.")) return true;
            VillageRole.parse(action.substring(12)).ifPresentOrElse(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageCouncilState.chooseRole(player, role)));
                openDashboard(player);
            }, () -> player.sendSystemMessage(Component.literal("§c알 수 없는 직업입니다.")));
            return true;
        }
        if (action.startsWith("skill_node:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c전술 발전은 기술 연구소에서만 가능합니다."));
                return true;
            }
            player.sendSystemMessage(Component.literal("§b" + VillageSkillTreeSystem.purchase(player, action.substring(11))));
            openSkillTree(player);
            return true;
        }
        if (action.startsWith("role_node:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c직업 성장은 기술 연구소에서만 가능합니다."));
                return true;
            }
            String[] parts = action.split(":", 3);
            if (parts.length == 3) VillageRole.parse(parts[1]).ifPresent(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageRoleSkillSystem.purchaseNode(player, role, parts[2])));
                openRoleProgress(player, role);
            });
            return true;
        }
        if (action.startsWith("gear:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                player.sendSystemMessage(Component.literal("§c장비 구매는 창고 단말기 근처에서만 가능합니다."));
                return true;
            }
            player.sendSystemMessage(Component.literal("§e" + VillageEquipmentShop.purchase(player, action.substring(5))));
            openEquipmentShop(player);
            return true;
        }
        if (action.startsWith("hire_mercenary:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
                player.sendSystemMessage(Component.literal("§c용병 고용은 병영 단말기 근처에서만 가능합니다."));
                return true;
            }
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(16));
            player.sendSystemMessage(Component.literal("§e" + VillageMercenarySystem.hire(player, kind)));
            openMercenaryCommand(player);
            return true;
        }
        if (action.startsWith("defense_research:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c방어 연구는 기술 연구소에서만 가능합니다."));
            } else {
                VillageDefenseResearchSystem.Branch branch = VillageDefenseResearchSystem.Branch.fromId(action.substring(17));
                player.sendSystemMessage(Component.literal("§b" + VillageDefenseResearchSystem.upgrade(player, branch)));
                openDefenseResearch(player);
            }
            return true;
        }
        if (action.startsWith("relic_select:")) {
            player.sendSystemMessage(Component.literal("§d" + VillageRelicSystem.select(player, action.substring(13))));
            return true;
        }

        switch (action) {
            case "open_dashboard", "open_mayor" -> openDashboard(player);
            case "open_manual", "open_caller_menu" -> openCaller(player);
            case "open_status" -> openStatus(player);
            case "open_personal_progress" -> openPersonalProgress(player);
            case "open_skill_tree" -> openSkillTree(player);
            case "open_role_progress_current" -> openRoleProgress(player);
            case "open_wave_intel" -> openWaveIntel(player);
            case "open_equipment_shop" -> openEquipmentShop(player);
            case "open_mercenary_command" -> openMercenaryCommand(player);
            case "open_defense_research" -> openDefenseResearch(player);
            case "forge_upgrade" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
                    player.sendSystemMessage(Component.literal("§c장비 강화는 대장간 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.improveForgeRank(player)));
                    openBuilding(player, VillageProgressionSystem.Building.SMITHY);
                }
            }
            case "smithy_forge_upgrade" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
                    player.sendSystemMessage(Component.literal("§c장비 강화는 대장간 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.improveForgeRank(player)));
                    openBuilding(player, VillageProgressionSystem.Building.SMITHY);
                }
            }
            case "forge_combine" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
                    player.sendSystemMessage(Component.literal("§c장비 합성은 대장간 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§e" + VillageEquipmentRaritySystem.combineFirstPair(player)));
                    openBuilding(player, VillageProgressionSystem.Building.SMITHY);
                }
            }
            case "buy_arrows" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c화살 구매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.buyArrows(player)));
                    openEquipmentShop(player);
                }
            }
            case "buy_food" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c식량 구매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.buyFood(player)));
                    openEquipmentShop(player);
                }
            }
            case "sell_loot" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c전리품 판매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§e" + VillageTradingSystem.sellMonsterDrops(player)));
                    openEquipmentShop(player);
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private static void fillLocalActions(ServerPlayer player, VillageProgressionSystem.Building building,
                                         List<String> actions, List<String> labels) {
        switch (building) {
            case WALLS -> add(actions, labels,
                    "open_wave_intel", "다음 웨이브 정찰|예상 병과·특성·보스 확인");
            case SMITHY -> {
                int rank = VillageProgressionSystem.forgeRank(player);
                int cost = 80 + rank * 100;
                add(actions, labels,
                        "smithy_forge_upgrade", rank >= VillageProgressionSystem.MAX_PERSONAL_RANK
                                ? "장비 강화 최고 단계|개인 장비 피해 보정이 최대입니다."
                                : "장비 강화 +" + (rank + 1) + " · 주화 " + cost
                                + "|근접·원거리 장비 피해 보정을 강화",
                        "forge_combine", "동종 장비 합성|같은 종류·같은 등급 두 개를 무료로 상위 등급 합성");
            }
            case SKILL_HALL -> add(actions, labels,
                    "open_role_progress_current", "직업 기술 연구|기술 습득·장착과 직업 성장 확인",
                    "open_defense_research", "마을 방어 연구|용병·포탑·전리품 연구 트리");
            case INFIRMARY -> add(actions, labels,
                    "use_infirmary", "즉시 치료|시설 단계에 따라 체력 회복");
            case BARRACKS -> add(actions, labels,
                    "train", "전투 훈련|재사용 대기시간 후 경험치 획득",
                    "open_mercenary_command", "용병 고용·성장|병과를 선택해 지속 용병 배치");
            case STOREHOUSE, TOWN_HALL -> { }
        }
    }

    private static String localDescription(ServerPlayer player, VillageProgressionSystem.Building building) {
        return switch (building) {
            case WALLS -> "현장에서는 정찰만 확인합니다. 수리·강화·포탑 건설은 회관에서 진행합니다.";
            case SMITHY -> "개인 장비 피해 보정 강화와 같은 종류·같은 등급 장비의 무료 합성을 담당합니다. 현재 장비 강화 +"
                    + VillageProgressionSystem.forgeRank(player) + ".";
            case SKILL_HALL -> "직업 기술과 용병·포탑 방어 연구를 담당합니다.";
            case INFIRMARY -> "전투 중 입은 피해를 즉시 치료합니다.";
            case BARRACKS -> "용병 병과 고용과 훈련을 담당합니다.";
            case STOREHOUSE -> "장비·식량·화살 구매와 전리품 판매를 담당합니다.";
            case TOWN_HALL -> "직업 배치와 모든 시설 수리·강화·건설을 담당합니다.";
        };
    }

    private static String managementEffect(VillageProgressionSystem.Building building, int level, MinecraftServer server) {
        int safe = Math.max(0, Math.min(VillageProgressionSystem.MAX_BUILDING_LEVEL, level));
        return switch (building) {
            case TOWN_HALL -> "직업 배치·시설 수리·강화·공동 보급 조달";
            case WALLS -> "최대 내구도 " + (1200 + safe * 350) + " · 포탑 설치 단계 " + safe;
            case SMITHY -> "최대 내구도 " + (560 + safe * 120) + " · 마을 장비 공격 보정 +" + (safe * 4)
                    + "% · 개인 장비 강화·등급 합성";
            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 직업 기술·마을 방어 연구";
            case INFIRMARY -> "최대 내구도 " + (520 + safe * 110) + " · 즉시 치료량 "
                    + Math.round((6.0f + safe * 4.0f) / 2.0f) + "칸";
            case STOREHOUSE -> "최대 내구도 " + (560 + safe * 120) + " · 상품·전리품 정산 효율 강화";
            case BARRACKS -> "최대 내구도 " + (620 + safe * 130) + " · 훈련 XP " + (30 + safe * 18)
                    + " · 기본 용병 정원 " + (1 + safe / 2);
        };
    }

    private static boolean requireTownHall(ServerPlayer player, String message) {
        if (VillageLocationRules.isNearTownHall(player)) return true;
        player.sendSystemMessage(Component.literal("§c" + message));
        return false;
    }

    private static void addShop(List<String> actions, List<String> labels, String action, String category,
                                String name, String cost, String effect, String status, boolean available) {
        actions.add(action);
        labels.add(String.join("|", "shop", category, name, cost, effect, status,
                available ? "available" : "locked"));
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
