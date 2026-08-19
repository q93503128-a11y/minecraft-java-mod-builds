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
        VillageUiService.openQuickChat(player);
    }

    public static void openStatus(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        RpgProgress progress = VillageCouncilState.progressOf(player.getUUID());
        String body = "전투 상태  Lv." + progress.level() + " · " + progress.experience()
                + "/" + progress.experienceToNextLevel() + " XP · 장착 장비 최고 +"
                + VillageEquipmentRaritySystem.bestEquippedEnhancement(player) + " · 개인 연구 +"
                + VillageProgressionSystem.skillRank(player) + "\n"
                + "직업  " + (role == null ? "미배치" : role.displayName())
                + (role == null ? "" : " · " + VillageRoleSkillSystem.loadoutSummary(player)) + "\n"
                + "재화  주화 " + VillageProgressionSystem.coins(player)
                + " · 공동 보급품 " + VillageProgressionSystem.supplies() + "\n"
                + "마을  제 " + VillageCouncilState.currentDay() + "일 "
                + VillageCouncilState.currentPhase().koreanName() + " · " + VillageRaidSystem.status() + "\n"
                + "단축키  설정 > 조작 > 마을 지키기에서 현재 지정 키 확인·변경";
        String relicLabel = "획득 유물 보기|보유 " + VillageRelicSystem.ownedCount(player)
                + " / " + VillageRelicSystem.Relic.values().length + " · 누적 효과 확인";
        send(player, "status", "수호자 상태", body,
                List.of("open_relic_collection"), List.of(relicLabel));
    }

    public static void openRelicCollection(ServerPlayer player) {
        VillageRelicSystem.openCollection(player);
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
                    + VillageSkillTreeSystem.nodeStatus(player, node) + "|" + node.pointCost());
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

    public static void openRoleSkillResearch(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role == null) {
            player.sendSystemMessage(Component.literal("§c마을 회관에서 직업을 먼저 배치하세요."));
            return;
        }
        if (!VillageLocationRules.isNearSkillHall(player)) {
            player.sendSystemMessage(Component.literal("§c직업 기술 습득은 기술 연구소 연구대 근처에서만 가능합니다."));
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRoleSkillSystem.ActiveSkill skill : VillageRoleSkillSystem.skillsFor(role)) {
            int slot = VillageRoleSkillSystem.equippedSlot(player, skill);
            actions.add("research_skill_unlock:" + skill.id());
            labels.add(String.join("|", "skill", skill.id(), skill.displayName(), skill.description(),
                    Integer.toString(skill.requiredLevel()), Integer.toString(skill.coinCost()),
                    VillageRoleSkillSystem.skillStatus(player, skill), Integer.toString(slot)));
        }
        String summary = "Lv." + VillageCouncilState.levelOf(player.getUUID())
                + " · 주화 " + VillageProgressionSystem.coins(player)
                + " · 습득은 연구소 · 습득 후 {SKILL1}/{SKILL2} 장착 변경";
        send(player, "role_skills", "직업 기술 연구",
                role.id() + "|" + role.displayName() + "|" + summary, actions, labels);
    }

    public static void openWaveIntel(ServerPlayer player) {
        List<String> actions = new ArrayList<>(); List<String> labels = new ArrayList<>();
        if (VillageCouncilState.currentPhase() == VillageTimePhase.DAY) {
            for (VillageWaveIntelSystem.WavePreview preview : VillageWaveIntelSystem.previews(player)) {
                actions.add("wave_info:" + preview.wave()); labels.add(preview.title() + "|" + preview.detail());
            }
        }
        send(player, "wave_intel", "다음 밤 적 정찰", VillageWaveIntelSystem.report(player), actions, labels);
    }

    public static void openEquipmentShop(ServerPlayer player) {
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int day = VillageCouncilState.currentDay();
        for (VillageEquipmentShop.Offer offer : VillageEquipmentShop.currentOffers(day)) {
            String status = VillageEquipmentShop.status(player, offer);
            boolean available = "available".equals(status);
            actions.add("gear:" + offer.id());
            labels.add(String.join("|", "shop", offer.category().name().toLowerCase(),
                    "[" + offer.rarity().displayName() + "] " + offer.displayName(),
                    "주화 " + offer.cost(), offer.effect() + " · 오늘 입고",
                    available ? "구매 가능" : status, available ? "available" : "locked"));
        }
        int arrows = 16 + VillageProgressionSystem.storehouseLevel() * 4;
        addShop(actions, labels, "claim_bread", "consumable", "오늘의 배급 식량", "무료",
                "하루 1회 기본 허기 회복 식량 · 접속 시 자동 지급, 놓쳤다면 여기서 수령", "하루 1회", true);
        addShop(actions, labels, "buy_arrows", "consumable", "화살 " + arrows + "개", "주화 14",
                "원거리 전투 보급", "구매 가능", true);
        for (VillageConsumableSystem.Consumable consumable : VillageConsumableSystem.catalog()) {
            boolean available = VillageConsumableSystem.unlocked(consumable);
            addShop(actions, labels, "consumable:" + consumable.id(), "consumable",
                    consumable.displayName() + " ×" + VillageConsumableSystem.bundleCount(consumable),
                    "주화 " + VillageConsumableSystem.effectiveCost(consumable), consumable.description(),
                    VillageConsumableSystem.status(consumable), available);
        }
        actions.add("open_item_sell");
        labels.add("shop_utility|보유품 선택 판매");
        actions.add("sell_loot");
        labels.add("shop_utility|판매용 잡템 일괄 정산");
        send(player, "equipment_shop", "상점",
                "제 " + day + "일 오늘의 입고품 · 주화 " + VillageProgressionSystem.coins(player)
                        + " · 장비 재고는 매일 교체", actions, labels);
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

    public static void openForgeEnhancement(ServerPlayer player) {
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
            openResult(player, "장비 강화", "장비 강화는 대장간 단말기 근처에서만 가능합니다.", "open_forge_enhancement");
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageEquipmentRaritySystem.EnhancementCandidate candidate
                : VillageEquipmentRaritySystem.enhancementCandidates(player)) {
            actions.add("forge_enhance:" + candidate.slot());
            String status = candidate.current() >= candidate.maximum()
                    ? "현재 대장간 최대 강화" : "다음 강화 주화 " + candidate.cost();
            labels.add(candidate.name() + "|" + candidate.rarity() + " · 강화 +" + candidate.current()
                    + " / +" + candidate.maximum() + " · " + status
                    + "\n현재 수치: " + candidate.currentEffect()
                    + "\n강화 후 수치: " + candidate.nextEffect());
        }
        send(player, "building", "장비 강화",
                "강화할 장비를 직접 선택합니다. 대장간 레벨과 방어 일수가 장기 강화 한도를 열며 장비 계열별 최종 상한이 다릅니다.",
                actions, labels);
    }

    public static void openItemSell(ServerPlayer player) {
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
            openResult(player, "보유품 판매", "보유품 판매는 상점 단말기 근처에서만 가능합니다.", "open_equipment_shop");
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageTradingSystem.SellCandidate candidate : VillageTradingSystem.sellCandidates(player)) {
            actions.add("sell_item:" + candidate.slot());
            labels.add(candidate.name() + " ×" + candidate.count() + "|판매가 주화 "
                    + candidate.totalValue() + " · 개당 " + candidate.unitValue());
        }
        send(player, "building", "보유품 판매",
                "판매할 아이템을 하나씩 선택합니다. 장비·소모품·판매용 잡템은 판매할 수 있습니다.", actions, labels);
    }

    public static void openFusion(ServerPlayer player) {
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
            player.sendSystemMessage(Component.literal("§c장비 합성은 대장간 단말기 근처에서만 가능합니다."));
            return;
        }
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageEquipmentRaritySystem.FusionCandidate candidate
                : VillageEquipmentRaritySystem.fusionCandidates(player)) {
            actions.add("fusion_pick:" + candidate.slot());
            labels.add(String.join("|", "fusion", Integer.toString(candidate.slot()), candidate.group(),
                    candidate.name(), candidate.rarity(), candidate.itemId()));
        }
        send(player, "equipment_fusion", "장비 합성",
                "같은 종류·같은 등급 장비 세 개를 선택해 다음 등급 하나로 합성합니다.", actions, labels);
    }

    public static void openSkillTest(ServerPlayer player) {
        if (VillageSkillTestSystem.isEnabled(player)) {
            sendSkillTestSkillManager(player, "외부 시험장 활성화");
            return;
        }
        if (!VillageLocationRules.isNearSkillHall(player)) {
            openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                    "open_role_skill_research");
            return;
        }
        openSkillTestPassword(player, "기술 시험관 접근 코드를 입력하세요.");
    }

    private static void openSkillTestPassword(ServerPlayer player, String message) {
        send(player, "skill_test_password", "기술 시험관 인증", message,
                List.of(), List.of());
    }

    public static void openSkillTestRoleManager(ServerPlayer player) {
        String mode = prepareSkillTest(player);
        if (mode != null) sendSkillTestRoleManager(player, mode);
    }

    public static void openSkillTestSkillManager(ServerPlayer player) {
        String mode = prepareSkillTest(player);
        if (mode != null) sendSkillTestSkillManager(player, mode);
    }

    private static String prepareSkillTest(ServerPlayer player) {
        if (!VillageSkillTestSystem.isEnabled(player)) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                openResult(player, "기술 시험", "기술 시험 시작은 기술 연구소 연구대 근처에서만 가능합니다.",
                        "open_role_skill_research");
            } else {
                openSkillTestPassword(player, "인증 후 시험장 관리 기능을 사용할 수 있습니다.");
            }
            return null;
        }
        return "외부 시험장 활성화";
    }

    private static void sendSkillTestRoleManager(ServerPlayer player, String mode) {
        VillageRole selected = VillageSkillTestSystem.selectedRole(player);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRole candidate : VillageRole.values()) {
            actions.add("test_role:" + candidate.id());
            labels.add((candidate == selected ? "선택됨 · " : "") + candidate.displayName()
                    + "|" + candidate.overview()
                    + "\n시험 전용 직업만 변경하며 실제 직업·성장·저장값은 바뀌지 않습니다.");
        }
        add(actions, labels,
                "open_skill_test_skills", "스킬 관리함 열기|현재 시험 직업의 {SKILL1}/{SKILL2} 기술 장착 화면",
                "test_exit", "시험 종료·복귀|시험 데이터를 정리하고 원래 위치로 복귀");
        String body = mode
                + "\n현재 시험 직업: " + selected.displayName()
                + "\n금색 바닥 직업 관리함입니다. 청금석 바닥 관리함에서는 스킬을 장착합니다.";
        send(player, "skill_test_role", "시험 직업 관리함", body, actions, labels);
    }

    private static void sendSkillTestSkillManager(ServerPlayer player, String mode) {
        VillageRole role = VillageSkillTestSystem.selectedRole(player);
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (VillageRoleSkillSystem.ActiveSkill skill : VillageRoleSkillSystem.skillsFor(role)) {
            actions.add("test_equip:" + skill.id() + ":0");
            labels.add("{SKILL1} · " + skill.displayName() + "|" + skill.description()
                    + "\n선택하면 {SKILL1} 슬롯에 장착되고 창이 자동으로 닫힙니다.");
            actions.add("test_equip:" + skill.id() + ":1");
            labels.add("{SKILL2} · " + skill.displayName() + "|" + skill.description()
                    + "\n선택하면 {SKILL2} 슬롯에 장착되고 창이 자동으로 닫힙니다.");
        }
        add(actions, labels,
                "open_skill_test_roles", "직업 관리함 열기|시험할 직업을 변경",
                "test_spawn", "시험 표적 재배치|체력·밀림 저항이 다른 표적 6개 생성",
                "test_clear", "시험 표적 정리|현재 내가 만든 시험 표적 제거",
                "test_exit", "시험 종료·복귀|표적과 임시 장착을 정리하고 원래 위치로 복귀");
        String body = mode
                + "\n현재 시험 직업: " + role.displayName()
                + "\n현재 임시 장착: " + VillageSkillTestSystem.loadoutSummary(player)
                + "\n기술 선택 후 창이 닫히면 {SKILL1}/{SKILL2}를 눌러 실제 시전합니다. K로 다시 엽니다.";
        send(player, "skill_test_skill", "시험 스킬 관리함", body, actions, labels);
    }

    public static void openResult(ServerPlayer player, String title, String result, String returnAction) {
        send(player, "result", title, result,
                returnAction == null || returnAction.isBlank() ? List.of() : List.of(returnAction),
                returnAction == null || returnAction.isBlank() ? List.of() : List.of("확인"));
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
            String detail = "Lv." + level + "/" + VillageDefenseResearchSystem.MAX_LEVEL
                    + "\n현재 수치: " + branch.description(level)
                    + (level >= VillageDefenseResearchSystem.MAX_LEVEL ? "\n최고 단계"
                    : "\n강화 후 수치: " + branch.description(level + 1)
                    + "\n다음 단계 비용: 주화 " + cost);
            labels.add(branch.displayName() + "|" + detail);
        }
        send(player, "building", "마을 방어 연구", "용병·포탑·전리품 운용을 연구합니다.", actions, labels);
    }

    public static boolean handleAction(ServerPlayer player, String action) {
        if (action == null || action.isBlank() || action.equals("facility_info")) return true;
        MinecraftServer server = player.level().getServer();
        if (server == null) return true;

        if (action.startsWith("use_skill:")) {
            int slot;
            try { slot = Integer.parseInt(action.substring(10)); }
            catch (NumberFormatException ignored) { slot = 0; }
            player.sendSystemMessage(Component.literal("§b" + VillageRpgSystem.useRoleSkill(player, slot)));
            return true;
        }

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
            openResult(player, repair ? "시설 수리 결과" : "시설 강화 결과", result, "open_dashboard");
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
            openResult(player, "장비 구매 결과",
                    VillageEquipmentShop.purchase(player, action.substring(5)), "open_equipment_shop");
            return true;
        }
        if (action.startsWith("consumable:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                player.sendSystemMessage(Component.literal("§c전투 소모품 구매는 창고 단말기 근처에서만 가능합니다."));
            } else {
                openResult(player, "전투 소모품 구매 결과",
                        VillageConsumableSystem.purchase(player, action.substring("consumable:".length())),
                        "open_equipment_shop");
            }
            return true;
        }
        if (action.startsWith("fusion_combine:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
                player.sendSystemMessage(Component.literal("§c장비 합성은 대장간 단말기 근처에서만 가능합니다."));
                return true;
            }
            String[] raw = action.substring(15).split(",", -1);
            if (raw.length == 3) {
                try {
                    String result = VillageEquipmentRaritySystem.combineSelected(player,
                            Integer.parseInt(raw[0]), Integer.parseInt(raw[1]), Integer.parseInt(raw[2]));
                    openResult(player, "장비 합성 결과", result, "open_fusion");
                } catch (NumberFormatException ignored) {
                    player.sendSystemMessage(Component.literal("§c합성할 장비 선택값이 올바르지 않습니다."));
                }
            }
            return true;
        }
        if (action.startsWith("forge_enhance:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.SMITHY)) {
                openResult(player, "장비 강화 결과", "장비 강화는 대장간 단말기 근처에서만 가능합니다.",
                        "open_forge_enhancement");
                return true;
            }
            try {
                int slot = Integer.parseInt(action.substring(14));
                openResult(player, "장비 강화 결과",
                        VillageEquipmentRaritySystem.enhanceSelected(player, slot), "open_forge_enhancement");
            } catch (NumberFormatException ignored) {
                openResult(player, "장비 강화 결과", "강화할 장비 선택값이 올바르지 않습니다.",
                        "open_forge_enhancement");
            }
            return true;
        }
        if (action.startsWith("sell_item:")) {
            try {
                int slot = Integer.parseInt(action.substring(10));
                openResult(player, "보유품 판매 결과", VillageTradingSystem.sellSelected(player, slot),
                        "open_item_sell");
            } catch (NumberFormatException ignored) {
                openResult(player, "보유품 판매 결과", "판매할 아이템 선택값이 올바르지 않습니다.",
                        "open_item_sell");
            }
            return true;
        }
        if (action.startsWith("research_skill_unlock:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c직업 기술 습득은 기술 연구소에서만 가능합니다."));
                return true;
            }
            player.sendSystemMessage(Component.literal("§b"
                    + VillageRoleSkillSystem.unlockSkill(player, action.substring(22))));
            openRoleSkillResearch(player);
            return true;
        }
        if (action.startsWith("research_skill_equip:")) {
            String[] parts = action.split(":", 3);
            if (parts.length == 3) {
                int slot;
                try { slot = Integer.parseInt(parts[2]); }
                catch (NumberFormatException ignored) { slot = 0; }
                player.sendSystemMessage(Component.literal("§b"
                        + VillageRoleSkillSystem.equipSkill(player, parts[1], slot)));
                openRoleSkillResearch(player);
            }
            return true;
        }
        if (action.startsWith("hire_mercenary:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
                player.sendSystemMessage(Component.literal("§c용병 고용은 병영 단말기 근처에서만 가능합니다."));
                return true;
            }
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(16));
            openResult(player, "용병 고용 결과", VillageMercenarySystem.hire(player, kind),
                    "open_mercenary_command");
            return true;
        }
        if (action.startsWith("defense_research:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c방어 연구는 기술 연구소에서만 가능합니다."));
            } else {
                VillageDefenseResearchSystem.Branch branch = VillageDefenseResearchSystem.Branch.fromId(action.substring(17));
                openResult(player, "방어 연구 결과", VillageDefenseResearchSystem.upgrade(player, branch),
                        "open_defense_research");
            }
            return true;
        }
        if (action.startsWith("skill_test_password:")) {
            String code = action.substring("skill_test_password:".length()).trim();
            if (!"1557".equals(code)) {
                openSkillTestPassword(player, "접근 코드가 올바르지 않습니다. 다시 입력하세요.");
                return true;
            }
            if (!VillageLocationRules.isNearSkillHall(player)) {
                openResult(player, "기술 시험", "기술 연구소 연구대 근처에서만 인증할 수 있습니다.",
                        "open_role_skill_research");
                return true;
            }
            String mode = VillageSkillTestSystem.enable(player);
            if (!VillageSkillTestSystem.isEnabled(player)) {
                openResult(player, "기술 시험", mode, "open_role_skill_research");
            } else {
                sendSkillTestRoleManager(player, mode);
            }
            return true;
        }

        if (action.startsWith("test_role:")) {
            player.sendSystemMessage(Component.literal("§b"
                    + VillageSkillTestSystem.selectRole(player, action.substring(10))));
            openSkillTestRoleManager(player);
            return true;
        }
        if (action.startsWith("test_equip:")) {
            String[] parts = action.split(":", 3);
            int slot = 0;
            if (parts.length == 3) {
                try { slot = Integer.parseInt(parts[2]); }
                catch (NumberFormatException ignored) { slot = 0; }
                player.sendSystemMessage(Component.literal("§b"
                        + VillageSkillTestSystem.equip(player, parts[1], slot)));
            }
            return true;
        }
        if (action.startsWith("relic_select:")) {
            String result = VillageRelicSystem.select(player, action.substring(13));
            player.sendSystemMessage(Component.literal("§d" + result));
            if (VillageRelicSystem.hasPendingChoice(player)) VillageRelicSystem.openChoice(player);
            else VillageRelicSystem.openCollection(player);
            return true;
        }

        switch (action) {
            case "open_dashboard", "open_mayor" -> openDashboard(player);
            case "open_manual", "open_caller_menu" -> openCaller(player);
            case "open_status" -> openStatus(player);
            case "open_relic_collection" -> openRelicCollection(player);
            case "open_personal_progress" -> openPersonalProgress(player);
            case "open_skill_tree" -> openSkillTree(player);
            case "open_role_progress_current" -> {
                if (VillageSkillTestSystem.isEnabled(player)) openSkillTestSkillManager(player);
                else openRoleProgress(player);
            }
            case "open_role_skill_research" -> openRoleSkillResearch(player);
            case "open_forge_enhancement" -> openForgeEnhancement(player);
            case "open_fusion" -> openFusion(player);
            case "open_wave_intel" -> openWaveIntel(player);
            case "open_equipment_shop" -> openEquipmentShop(player);
            case "open_item_sell" -> openItemSell(player);
            case "open_mercenary_command" -> openMercenaryCommand(player);
            case "open_defense_research" -> openDefenseResearch(player);
            case "open_skill_test" -> openSkillTest(player);
            case "open_skill_test_roles" -> openSkillTestRoleManager(player);
            case "open_skill_test_skills" -> openSkillTestSkillManager(player);
            case "test_spawn" -> {
                player.sendSystemMessage(Component.literal("§b" + VillageSkillTestSystem.spawnTargets(player)));
                openSkillTestSkillManager(player);
            }
            case "test_clear" -> {
                player.sendSystemMessage(Component.literal("§b" + VillageSkillTestSystem.clearTargets(player)));
                openSkillTestSkillManager(player);
            }
            case "test_exit" -> openResult(player, "기술 시험", VillageSkillTestSystem.disable(player),
                    VillageCouncilState.roleOf(player.getUUID()).isPresent()
                            ? "open_role_skill_research" : "open_dashboard");
            case "forge_upgrade", "smithy_forge_upgrade" -> openForgeEnhancement(player);
            case "forge_combine" -> openFusion(player);
            case "buy_arrows" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c화살 구매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    openResult(player, "화살 구매 결과", VillageProgressionSystem.buyArrows(player),
                            "open_equipment_shop");
                }
            }
            case "claim_bread" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c배급 식량 수령은 창고 단말기 근처에서만 가능합니다."));
                } else {
                    openResult(player, "배급 식량", VillageProgressionSystem.claimDailyBread(player),
                            "open_equipment_shop");
                }
            }
            case "sell_loot" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c전리품 판매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    openResult(player, "잡템 정산 결과", VillageTradingSystem.sellMonsterDrops(player),
                            "open_equipment_shop");
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
            case SMITHY -> add(actions, labels,
                    "open_forge_enhancement", "장비 선택 강화|보유한 등급 장비를 골라 개별 강화",
                    "open_fusion", "장비 3개 합성|같은 종류·같은 등급·같은 강화 단계 세 개를 상위 등급으로 합성");
            case SKILL_HALL -> add(actions, labels,
                    "open_role_skill_research", "직업 기술 연구|현재 직업의 기술 습득과 {SKILL1}/{SKILL2} 장착만 관리",
                    "open_defense_research", "마을 방어 연구|용병·포탑·전리품 연구 트리",
                    "open_skill_test", "외부 기술 시험장|야외 시험장으로 이동해 {SKILL1}/{SKILL2}에 기술을 임시 장착하고 실제 모션 시험");
            case INFIRMARY -> { }
            case BARRACKS -> add(actions, labels,
                    "open_wave_intel", "다음 밤 적 정찰|웨이브별 병과·수량·특성·보스 편성 확인",
                    "open_mercenary_command", "용병 고용·성장|병과를 선택해 지속 용병 배치");
            case STOREHOUSE, TOWN_HALL -> { }
        }
    }

    private static String localDescription(ServerPlayer player, VillageProgressionSystem.Building building) {
        return switch (building) {
            case WALLS -> "현장에서는 정찰만 확인합니다. 수리·강화·포탑 건설은 회관에서 진행합니다.";
            case SMITHY -> "등급 장비를 하나씩 선택해 강화하고, 같은 종류·등급·강화 단계 장비 세 개를 합성합니다.";
            case SKILL_HALL -> "직업 기술과 용병·포탑 방어 연구를 담당합니다. 연구소 레벨마다 기술 위력·지속시간이 +5% 상승하고 재사용 효율도 개선됩니다.";
            case INFIRMARY -> "낮 동안 마을 안 플레이어의 체력을 항상 완전히 회복하고, 레벨별 전투 버프를 제공합니다.";
            case BARRACKS -> "다음 밤 적 정찰과 용병 고용, 모든 경험치 획득량 증가 패시브를 담당합니다. 현재 XP +"
                    + (VillageProgressionSystem.experienceMultiplierPercent() - 100) + "%";
            case STOREHOUSE -> "일일 배급 식량·화살·전투 소모품·장비 구매와 전리품 판매를 담당합니다.";
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
            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 기술 위력 +" + (safe * 5) + "% · 지속 +" + (safe * 5) + "% · 재사용 효율 +" + safe + "초 · 마을 방어 연구";
            case INFIRMARY -> "최대 내구도 " + (520 + safe * 110) + " · 낮 동안 체력 완전 회복"
                    + (safe >= 1 ? " · 피해 저항" : "")
                    + (safe >= 2 ? " · 이동 속도" : "")
                    + (safe >= 3 ? " · 공격력" : "")
                    + (safe >= 4 ? " · 재생" : "")
                    + (safe >= 5 ? " · 보호막" : "");
            case STOREHOUSE -> "최대 내구도 " + (560 + safe * 120) + " · 일일 배급·전투 소모품·상품·전리품 정산";
            case BARRACKS -> "최대 내구도 " + (620 + safe * 130) + " · 모든 XP +" + (safe * 10)
                    + "% · 기본 용병 정원 " + (1 + safe / 2);
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
                || screenId.equals("caller") || screenId.equals("relic_choice")
                || screenId.equals("wave_intel") || screenId.equals("skill_test");
    }
}
