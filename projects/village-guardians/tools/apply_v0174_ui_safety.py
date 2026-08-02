from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one match in {path}, found {count}: {old[:100]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all(path: Path, old: str, new: str, expected: int) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"Expected {expected} matches in {path}, found {count}: {old!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


# Version.
replace_once(ROOT / "gradle.properties", "mod_version=0.17.3-alpha.1", "mod_version=0.17.4-alpha.1")
replace_once(
    ROOT / "tools/test_runtime_safety.py",
    'assert "PANEL = 0xFFF1E6CF" in facility_ui',
    'assert "PANEL = 0xFFE4D8BF" in facility_ui')

# Do not fire gameplay shortcuts while chat, inventory, or another screen owns keyboard input.
keys = JAVA / "VillageClientKeys.java"
replace_once(
    keys,
    '''    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        consume(minecraft, ROLE_SKILL_ONE, "use_skill:0");
        consume(minecraft, ROLE_SKILL_TWO, "use_skill:1");
        consume(minecraft, QUICK_COMMUNICATION, "open_quick_chat");
        consume(minecraft, STATUS, "open_status");
        consume(minecraft, GROWTH, "open_skill_tree");
        consume(minecraft, ROLE_PROGRESS, "open_role_progress_current");
        consume(minecraft, CALLER, "open_caller_menu");
    }

    private static void consume(Minecraft minecraft, KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            if (minecraft.player != null) {
                ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
            }
        }
    }
''',
    '''    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {
            drain(ROLE_SKILL_ONE);
            drain(ROLE_SKILL_TWO);
            drain(QUICK_COMMUNICATION);
            drain(STATUS);
            drain(GROWTH);
            drain(ROLE_PROGRESS);
            drain(CALLER);
            return;
        }
        consume(ROLE_SKILL_ONE, "use_skill:0");
        consume(ROLE_SKILL_TWO, "use_skill:1");
        consume(QUICK_COMMUNICATION, "open_quick_chat");
        consume(STATUS, "open_status");
        consume(GROWTH, "open_skill_tree");
        consume(ROLE_PROGRESS, "open_role_progress_current");
        consume(CALLER, "open_caller_menu");
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Discard clicks captured while another screen owns keyboard input.
        }
    }

    private static void consume(KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }
''')

# Enforce authoritative location checks in the first server-side router.
controller = JAVA / "VillageUiController.java"
replace_once(
    controller,
    '''        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
            if (!requireTownHall(player, "시설 수리와 강화는 마을 회관에서만 가능합니다.")) return true;
            boolean repair = action.startsWith("repair:");
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring(repair ? 7 : 8));
            if (building != null) {
                String result = repair ? VillageProgressionSystem.repair(player, building)
                        : VillageProgressionSystem.upgrade(player, building);
                player.sendSystemMessage(Component.literal("§6" + result));
                openDashboard(player);
            }
            return true;
        }
''',
    '''        if (action.startsWith("repair:") || action.startsWith("upgrade:")) {
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
''')
replace_once(
    controller,
    '''        if (action.startsWith("skill_node:")) {
            player.sendSystemMessage(Component.literal("§b" + VillageSkillTreeSystem.purchase(player, action.substring(11))));
            openSkillTree(player);
            return true;
        }
''',
    '''        if (action.startsWith("skill_node:")) {
            if (!VillageLocationRules.isNearSkillHall(player)) {
                player.sendSystemMessage(Component.literal("§c전술 발전은 기술 연구소에서만 가능합니다."));
                return true;
            }
            player.sendSystemMessage(Component.literal("§b" + VillageSkillTreeSystem.purchase(player, action.substring(11))));
            openSkillTree(player);
            return true;
        }
''')
replace_once(
    controller,
    '''        if (action.startsWith("role_node:")) {
            String[] parts = action.split(":", 3);
            if (parts.length == 3) VillageRole.parse(parts[1]).ifPresent(role -> {
                player.sendSystemMessage(Component.literal("§b" + VillageRoleSkillSystem.purchaseNode(player, role, parts[2])));
                openRoleProgress(player, role);
            });
            return true;
        }
''',
    '''        if (action.startsWith("role_node:")) {
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
''')
replace_once(
    controller,
    '''        if (action.startsWith("gear:")) {
            player.sendSystemMessage(Component.literal("§e" + VillageEquipmentShop.purchase(player, action.substring(5))));
            openEquipmentShop(player);
            return true;
        }
''',
    '''        if (action.startsWith("gear:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                player.sendSystemMessage(Component.literal("§c장비 구매는 창고 단말기 근처에서만 가능합니다."));
                return true;
            }
            player.sendSystemMessage(Component.literal("§e" + VillageEquipmentShop.purchase(player, action.substring(5))));
            openEquipmentShop(player);
            return true;
        }
''')
replace_once(
    controller,
    '''        if (action.startsWith("hire_mercenary:")) {
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(16));
            player.sendSystemMessage(Component.literal("§e" + VillageMercenarySystem.hire(player, kind)));
            openMercenaryCommand(player);
            return true;
        }
''',
    '''        if (action.startsWith("hire_mercenary:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
                player.sendSystemMessage(Component.literal("§c용병 고용은 병영 단말기 근처에서만 가능합니다."));
                return true;
            }
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(16));
            player.sendSystemMessage(Component.literal("§e" + VillageMercenarySystem.hire(player, kind)));
            openMercenaryCommand(player);
            return true;
        }
''')
replace_once(
    controller,
    '''            case "forge_combine" -> {
                player.sendSystemMessage(Component.literal("§e" + VillageEquipmentRaritySystem.combineFirstPair(player)));
                openBuilding(player, VillageProgressionSystem.Building.SMITHY);
            }
            case "buy_arrows" -> {
                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.buyArrows(player)));
                openEquipmentShop(player);
            }
            case "buy_food" -> {
                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.buyFood(player)));
                openEquipmentShop(player);
            }
            case "sell_loot" -> {
                player.sendSystemMessage(Component.literal("§e" + VillageTradingSystem.sellMonsterDrops(player)));
                openEquipmentShop(player);
            }
''',
    '''            case "forge_combine" -> {
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
''')

# Protect all legacy facility actions that reach the fallback service. The action must be
# deferred until after the authoritative location check; ordinary arguments are evaluated first.
service = JAVA / "VillageUiService.java"
replace_once(service, "import java.util.List;\n", "import java.util.List;\nimport java.util.function.Supplier;\n")
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.claimDailyBread(player), VillageProgressionSystem.Building.STOREHOUSE)",
    "actAndReopen(player, () -> VillageProgressionSystem.claimDailyBread(player), VillageProgressionSystem.Building.STOREHOUSE)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.buyArrows(player), VillageProgressionSystem.Building.STOREHOUSE)",
    "actAndReopen(player, () -> VillageProgressionSystem.buyArrows(player), VillageProgressionSystem.Building.STOREHOUSE)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.buyFood(player), VillageProgressionSystem.Building.STOREHOUSE)",
    "actAndReopen(player, () -> VillageProgressionSystem.buyFood(player), VillageProgressionSystem.Building.STOREHOUSE)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageTradingSystem.sellMonsterDrops(player), VillageProgressionSystem.Building.STOREHOUSE)",
    "actAndReopen(player, () -> VillageTradingSystem.sellMonsterDrops(player), VillageProgressionSystem.Building.STOREHOUSE)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.improveForgeRank(player), VillageProgressionSystem.Building.SMITHY)",
    "actAndReopen(player, () -> VillageProgressionSystem.improveForgeRank(player), VillageProgressionSystem.Building.SMITHY)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.learnNextSkill(player), VillageProgressionSystem.Building.SKILL_HALL)",
    "actAndReopen(player, () -> VillageProgressionSystem.learnNextSkill(player), VillageProgressionSystem.Building.SKILL_HALL)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.useInfirmary(player), VillageProgressionSystem.Building.INFIRMARY)",
    "actAndReopen(player, () -> VillageProgressionSystem.useInfirmary(player), VillageProgressionSystem.Building.INFIRMARY)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageProgressionSystem.train(player), VillageProgressionSystem.Building.BARRACKS)",
    "actAndReopen(player, () -> VillageProgressionSystem.train(player), VillageProgressionSystem.Building.BARRACKS)",
    1)
replace_all(
    service,
    "actAndReopen(player, VillageDefenseSystem.hireMercenary(player), VillageProgressionSystem.Building.BARRACKS)",
    "actAndReopen(player, () -> VillageDefenseSystem.hireMercenary(player), VillageProgressionSystem.Building.BARRACKS)",
    1)
replace_once(
    service,
    '''    private static void actAndReopen(ServerPlayer player, String result, VillageProgressionSystem.Building building) {
        player.sendSystemMessage(Component.literal("§e" + result));
        openBuilding(player, building);
    }
''',
    '''    private static void actAndReopen(ServerPlayer player, Supplier<String> action,
                                     VillageProgressionSystem.Building building) {
        if (!VillageLocationRules.isNear(player, building)) {
            player.sendSystemMessage(Component.literal(
                    "§c이 기능은 " + building.displayName() + " 단말기 근처에서만 사용할 수 있습니다."));
            return;
        }
        player.sendSystemMessage(Component.literal("§e" + action.get()));
        openBuilding(player, building);
    }
''')

local_actions = JAVA / "VillageLocalActionSystem.java"
replace_once(
    local_actions,
    '''            case "use_infirmary" -> {
                player.sendSystemMessage(Component.literal("§a" + VillageProgressionSystem.useInfirmary(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.INFIRMARY);
                return true;
            }
            case "train" -> {
                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.train(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.BARRACKS);
                return true;
            }
''',
    '''            case "use_infirmary" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.INFIRMARY)) {
                    player.sendSystemMessage(Component.literal("§c치료는 의무소 단말기 근처에서만 가능합니다."));
                    return true;
                }
                player.sendSystemMessage(Component.literal("§a" + VillageProgressionSystem.useInfirmary(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.INFIRMARY);
                return true;
            }
            case "train" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
                    player.sendSystemMessage(Component.literal("§c전투 훈련은 병영 단말기 근처에서만 가능합니다."));
                    return true;
                }
                player.sendSystemMessage(Component.literal("§e" + VillageProgressionSystem.train(player)));
                VillageUiController.openBuilding(player, VillageProgressionSystem.Building.BARRACKS);
                return true;
            }
''')

# Pixel-accurate ellipsis and a stacked fallback on exceptionally narrow town-hall layouts.
town = JAVA / "VillageTownHallScreen.java"
replace_once(town, "compact(role.name(), Math.max(10, w / 6))", "compact(role.name(), w - 22)")
replace_once(town, "compact(facility.name(), Math.max(10, w / 6))", "compact(facility.name(), w - 22)")
replace_once(town, "compact(state, Math.max(9, w / 6))", "compact(state, w - 22)")
replace_once(town, "compact(button.label(), Math.max(10, bound.width() / 6))", "compact(button.label(), bound.width() - 10)")
replace_once(
    town,
    '''    private Split split(Layout layout) {
        int left = layout.left() + 15;
        int right = layout.right() - 15;
        int top = layout.top() + 64;
        int bottom = layout.bottom() - 13;
        int listWidth = clamp((right - left) * 24 / 100, 125, 205);
        return new Split(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 8, top, right, bottom));
    }
''',
    '''    private Split split(Layout layout) {
        int left = layout.left() + 15;
        int right = layout.right() - 15;
        int top = layout.top() + 64;
        int bottom = layout.bottom() - 13;
        int contentWidth = right - left;
        if (contentWidth < 260) {
            int availableHeight = Math.max(1, bottom - top);
            int listHeight = clamp(availableHeight * 36 / 100, 72,
                    Math.max(72, availableHeight - 96));
            return new Split(new Pane(left, top, right, top + listHeight),
                    new Pane(left, top + listHeight + 7, right, bottom));
        }
        int listWidth = clamp(contentWidth * 24 / 100, 125, 205);
        return new Split(new Pane(left, top, left + listWidth, bottom),
                new Pane(left + listWidth + 8, top, right, bottom));
    }
''')
replace_once(
    town,
    '''    private String compact(String value, int max) {
        String normalized = plain(value).replace('\\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }
''',
    '''    private String compact(String value, int maxWidth) {
        String normalized = plain(value).replace('\\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        if (font.width(suffix) > maxWidth) return "";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
    }
''')

facility = JAVA / "VillageFacilityScreen.java"
replace_once(
    facility,
    '''        graphics.text(font, plain(payload.title()), left, layout.top() + 10, TEXT, false);
        graphics.text(font, subtitle(), left, layout.top() + 27, MUTED, false);
''',
    '''        int textWidth = Math.max(32, closeX - left - 8);
        graphics.text(font, compact(plain(payload.title()), textWidth), left, layout.top() + 10, TEXT, false);
        graphics.text(font, compact(subtitle(), textWidth), left, layout.top() + 27, MUTED, false);
''')
replace_once(facility, "compact(parts[0], Math.max(9, cardWidth / 6))", "compact(parts[0], cardWidth - 19)")
replace_once(
    facility,
    "compact(VillageActionDescriptions.executeLabel(actions[selectedIndex]), Math.max(9, buttonWidth / 6))",
    "compact(VillageActionDescriptions.executeLabel(actions[selectedIndex]), buttonWidth - 10)")
replace_once(
    facility,
    '''    private String compact(String value, int max) {
        String normalized = plain(value).replace('\\n', ' ');
        return normalized.length() <= max ? normalized
                : normalized.substring(0, Math.max(1, max - 1)) + "…";
    }
''',
    '''    private String compact(String value, int maxWidth) {
        String normalized = plain(value).replace('\\n', ' ');
        if (maxWidth <= 0) return "";
        if (font.width(normalized) <= maxWidth) return normalized;
        String suffix = "…";
        if (font.width(suffix) > maxWidth) return "";
        int end = normalized.length();
        while (end > 0 && font.width(normalized.substring(0, end) + suffix) > maxWidth) end--;
        return normalized.substring(0, end) + suffix;
    }
''')

# Keep the personal growth popover inside every viewport and use all available vertical space.
skill_tree = JAVA / "VillageSkillTreeScreen.java"
replace_once(
    skill_tree,
    '''        int bubbleWidth = Math.min(246, Math.max(174, viewport.width() / 3));
        List<FormattedCharSequence> lines = font.split(Component.literal(node.description()), bubbleWidth - 16);
        int lineCount = Math.min(3, lines.size());
        boolean purchasable = "습득 가능".equals(node.status());
        int bubbleHeight = 45 + lineCount * 11 + (purchasable ? 24 : 8);
''',
    '''        int bubbleWidth = fitPopoverWidth(viewport.width(), 164, 246);
        boolean purchasable = "습득 가능".equals(node.status());
        List<FormattedCharSequence> lines = font.split(Component.literal(node.description()),
                Math.max(40, bubbleWidth - 16));
        int baseHeight = 45 + (purchasable ? 24 : 8);
        int lineCount = Math.min(lines.size(), Math.max(0, (viewport.height() - baseHeight - 10) / 11));
        int bubbleHeight = baseHeight + lineCount * 11;
''')
replace_once(
    skill_tree,
    '''        int nodeX = screenX(viewport, node.worldX());
        int nodeY = screenY(viewport, node.worldY());
        int bubbleWidth = fitPopoverWidth(viewport.width(), 164, 246);
''',
    '''        int nodeX = screenX(viewport, node.worldX());
        int nodeY = screenY(viewport, node.worldY());
        int nodeHalf = scaledNodeSize() / 2;
        if (nodeX + nodeHalf < viewport.left() || nodeX - nodeHalf > viewport.right()
                || nodeY + nodeHalf < viewport.top() || nodeY - nodeHalf > viewport.bottom()) {
            return null;
        }
        int bubbleWidth = fitPopoverWidth(viewport.width(), 164, 246);
''')
replace_once(skill_tree, "        int buttonWidth = 68;\n", "        int buttonWidth = Math.max(1, Math.min(68, bubbleWidth - 14));\n")
replace_once(
    skill_tree,
    '''    private int screenX(Viewport viewport, double worldX) {
''',
    '''    private static int fitPopoverWidth(int viewportWidth, int preferredMinimum, int preferredMaximum) {
        int maximum = Math.max(1, viewportWidth - 10);
        int minimum = Math.min(preferredMinimum, maximum);
        int preferred = Math.max(minimum, Math.min(maximum, viewportWidth / 3));
        return Math.min(maximum, Math.min(preferredMaximum, preferred));
    }

    private int screenX(Viewport viewport, double worldX) {
''')

# Apply the same clipping rules to role growth, and switch skill cards to vertical popovers
# when neither side has enough room so the adjacent card is not covered.
role = JAVA / "VillageRoleProgressScreen.java"
replace_once(
    role,
    '''        int bubbleWidth = Math.min(264, Math.max(188, view.width() / 3));
        List<FormattedCharSequence> lines = font.split(Component.literal(node.description()), bubbleWidth - 16);
        int maxLines = Math.max(2, Math.min(7, (view.height() - 90) / 11));
        int lineCount = Math.min(maxLines, lines.size());
        boolean purchasable = "습득 가능".equals(node.status());
        int bubbleHeight = 45 + lineCount * 11 + (purchasable ? 24 : 8);
''',
    '''        int bubbleWidth = fitPopoverWidth(view.width(), 176, 264);
        boolean purchasable = "습득 가능".equals(node.status());
        List<FormattedCharSequence> lines = font.split(Component.literal(node.description()),
                Math.max(40, bubbleWidth - 16));
        int baseHeight = 45 + (purchasable ? 24 : 8);
        int lineCount = Math.min(lines.size(), Math.max(0, (view.height() - baseHeight - 10) / 11));
        int bubbleHeight = baseHeight + lineCount * 11;
''')
replace_once(
    role,
    '''        int nodeX = screenX(view, node.worldX());
        int nodeY = screenY(view, node.worldY());
        int bubbleWidth = fitPopoverWidth(view.width(), 176, 264);
''',
    '''        int nodeX = screenX(view, node.worldX());
        int nodeY = screenY(view, node.worldY());
        int nodeHalf = scaledNodeSize() / 2;
        if (nodeX + nodeHalf < view.left() || nodeX - nodeHalf > view.right()
                || nodeY + nodeHalf < view.top() || nodeY - nodeHalf > view.bottom()) {
            return null;
        }
        int bubbleWidth = fitPopoverWidth(view.width(), 176, 264);
''')
old_skill_bubble = '''    private SkillBubble skillBubble(Viewport view, SkillGrid grid) {
        if (selectedSkill < 0 || selectedSkill >= skills.size()) return null;
        SkillEntry skill = skills.get(selectedSkill);
        CardBounds card = skillCardBounds(view, grid, selectedSkill);
        int cardX = card.x() + card.size() / 2;
        int cardY = card.y() + card.size() / 2;
        int bubbleWidth = Math.min(270, Math.max(196, view.width() / 3));
        List<FormattedCharSequence> lines = font.split(Component.literal(skill.description()), bubbleWidth - 16);
        int maxLines = Math.max(2, Math.min(7, (view.height() - 90) / 11));
        int lineCount = Math.min(maxLines, lines.size());
        boolean learned = isLearned(skill);
        boolean unlockable = "습득 가능".equals(skill.status());
        int bubbleHeight = 45 + lineCount * 11 + ((learned || unlockable) ? 24 : 8);
        int x = card.x() + card.size() + 9;
        if (x + bubbleWidth > view.right() - 5) x = card.x() - bubbleWidth - 9;
        x = clamp(x, view.left() + 5, Math.max(view.left() + 5, view.right() - bubbleWidth - 5));
        int y = clamp(card.y() + 4, view.top() + 5,
                Math.max(view.top() + 5, view.bottom() - bubbleHeight - 5));
        int buttonHeight = 18;
        int buttonWidth = 66;
        int unlockWidth = 104;
        int buttonY = y + bubbleHeight - buttonHeight - 6;
        int secondX = x + bubbleWidth - buttonWidth - 7;
        int firstX = learned ? secondX - buttonWidth - 6 : x + bubbleWidth - unlockWidth - 7;
        return new SkillBubble(x, y, bubbleWidth, bubbleHeight, firstX, secondX,
                buttonY, buttonWidth, unlockWidth, buttonHeight, learned, unlockable,
                lines, lineCount, skill, cardX, cardY);
    }
'''
new_skill_bubble = '''    private SkillBubble skillBubble(Viewport view, SkillGrid grid) {
        if (selectedSkill < 0 || selectedSkill >= skills.size()) return null;
        SkillEntry skill = skills.get(selectedSkill);
        CardBounds card = skillCardBounds(view, grid, selectedSkill);
        if (card.y() + card.size() < view.top() || card.y() > view.bottom()) return null;
        int cardX = card.x() + card.size() / 2;
        int cardY = card.y() + card.size() / 2;
        int maximumWidth = Math.max(1, view.width() - 10);
        int preferredWidth = Math.min(maximumWidth, Math.min(270, Math.max(180, view.width() / 3)));
        int rightSpace = view.right() - 5 - (card.x() + card.size() + 9);
        int leftSpace = card.x() - 9 - (view.left() + 5);
        boolean placeRight = rightSpace >= leftSpace;
        int sideSpace = Math.max(rightSpace, leftSpace);
        boolean horizontalPlacement = sideSpace >= Math.min(160, preferredWidth);
        int bubbleWidth = horizontalPlacement ? Math.min(preferredWidth, sideSpace) : preferredWidth;
        boolean learned = isLearned(skill);
        boolean unlockable = "습득 가능".equals(skill.status());
        List<FormattedCharSequence> lines = font.split(Component.literal(skill.description()),
                Math.max(40, bubbleWidth - 16));
        int baseHeight = 45 + ((learned || unlockable) ? 24 : 8);
        int lineCount = Math.min(lines.size(), Math.max(0, (view.height() - baseHeight - 10) / 11));
        int bubbleHeight = baseHeight + lineCount * 11;

        int x;
        int y;
        if (horizontalPlacement) {
            x = placeRight ? card.x() + card.size() + 9 : card.x() - bubbleWidth - 9;
            y = clamp(card.y() + 4, view.top() + 5,
                    Math.max(view.top() + 5, view.bottom() - bubbleHeight - 5));
        } else {
            x = clamp(cardX - bubbleWidth / 2, view.left() + 5,
                    Math.max(view.left() + 5, view.right() - bubbleWidth - 5));
            int below = card.y() + card.size() + 8;
            int above = card.y() - bubbleHeight - 8;
            if (below + bubbleHeight <= view.bottom() - 5) y = below;
            else if (above >= view.top() + 5) y = above;
            else y = clamp(card.y() + 4, view.top() + 5,
                        Math.max(view.top() + 5, view.bottom() - bubbleHeight - 5));
        }

        int buttonHeight = 18;
        int buttonGap = 6;
        int buttonWidth = learned
                ? Math.max(1, Math.min(66, (bubbleWidth - 20 - buttonGap) / 2))
                : Math.max(1, Math.min(66, bubbleWidth - 14));
        int unlockWidth = Math.max(1, Math.min(104, bubbleWidth - 14));
        int buttonY = y + bubbleHeight - buttonHeight - 6;
        int secondX = x + bubbleWidth - buttonWidth - 7;
        int firstX = learned ? secondX - buttonWidth - buttonGap : x + bubbleWidth - unlockWidth - 7;
        return new SkillBubble(x, y, bubbleWidth, bubbleHeight, firstX, secondX,
                buttonY, buttonWidth, unlockWidth, buttonHeight, learned, unlockable,
                lines, lineCount, skill, cardX, cardY);
    }
'''
replace_once(role, old_skill_bubble, new_skill_bubble)
replace_once(
    role,
    '''    private SkillGrid skillGrid(Viewport view) {
''',
    '''    private static int fitPopoverWidth(int viewportWidth, int preferredMinimum, int preferredMaximum) {
        int maximum = Math.max(1, viewportWidth - 10);
        int minimum = Math.min(preferredMinimum, maximum);
        int preferred = Math.max(minimum, Math.min(maximum, viewportWidth / 3));
        return Math.min(maximum, Math.min(preferredMaximum, preferred));
    }

    private SkillGrid skillGrid(Viewport view) {
''')

# New contract focuses on the regressions found during the v0.17.3 source/JAR comparison.
test_path = ROOT / "tools/test_v0174_ui_safety.py"
test_path.write_text('''from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    controller = read("VillageUiController.java")
    service = read("VillageUiService.java")
    local = read("VillageLocalActionSystem.java")
    town = read("VillageTownHallScreen.java")
    facility = read("VillageFacilityScreen.java")
    common_tree = read("VillageSkillTreeScreen.java")
    role_tree = read("VillageRoleProgressScreen.java")

    assert "mod_version=0.17.4-alpha.1" in props
    assert "minecraft.gui.screen() != null" in keys
    assert "private static void drain(KeyMapping mapping)" in keys
    assert "minecraft.getConnection() == null" in keys

    skill_guard = controller.index('if (action.startsWith("skill_node:"))')
    role_guard = controller.index('if (action.startsWith("role_node:"))')
    assert "isNearSkillHall" in controller[skill_guard:skill_guard + 500]
    assert "isNearSkillHall" in controller[role_guard:role_guard + 600]
    assert "isNearTownHall(player) && !VillageLocationRules.isNear(player, building)" in controller
    assert "장비 합성은 대장간" in controller
    assert "장비 구매는 창고" in controller
    assert "용병 고용은 병영" in controller

    assert "Supplier<String> action" in service
    assert "action.get()" in service
    assert "() -> VillageProgressionSystem.learnNextSkill(player)" in service
    assert "치료는 의무소" in local and "전투 훈련은 병영" in local

    assert "font.width(normalized)" in town
    assert "font.width(normalized)" in facility
    assert "contentWidth < 260" in town
    assert "fitPopoverWidth(viewport.width(), 164, 246)" in common_tree
    assert "nodeX + nodeHalf < viewport.left()" in common_tree
    assert "fitPopoverWidth(view.width(), 176, 264)" in role_tree
    assert "horizontalPlacement" in role_tree
    assert "card.y() + card.size() < view.top()" in role_tree
    assert "below + bubbleHeight <= view.bottom() - 5" in role_tree

    print("Village Guardians v0.17.4 UI, location and shortcut contracts passed.")


if __name__ == "__main__":
    main()
''', encoding="utf-8")

print("Applied Village Guardians v0.17.4 UI safety and interaction consistency patch.")
