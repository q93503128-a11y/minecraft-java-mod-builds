#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one marker, found {count}")
    write(path, text.replace(old, new, 1))


# ---------------------------------------------------------------------------
# Version
# ---------------------------------------------------------------------------
props = ROOT / "gradle.properties"
replace_once(props, "mod_version=0.17.19-alpha.1", "mod_version=0.18.0-alpha.1", "version")

# ---------------------------------------------------------------------------
# Client key source of truth: Z / X skills, V quick communication.
# X is intentionally allowed even though vanilla uses X only as the toolbar
# modifier with number keys. Exact v0.17.19 defaults migrate once, while custom
# non-conflicting bindings remain untouched.
# ---------------------------------------------------------------------------
write(JAVA / "VillageClientKeys.java", r'''package kr.moonseungjun.villageguardians;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientKeys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "controls"));

    private static final KeyMapping ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_Z);
    private static final KeyMapping ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_X);
    private static final KeyMapping QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_V);
    private static final KeyMapping STATUS = key("status", GLFW.GLFW_KEY_H);
    private static final KeyMapping GROWTH = key("personal_progress", GLFW.GLFW_KEY_J);
    private static final KeyMapping ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K);

    // X is deliberately omitted: vanilla's toolbar save/restore needs X + number,
    // while this mod consumes the standalone X click for skill slot 2.
    private static final Set<Integer> VANILLA_RESERVED = Set.of(
            GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D,
            GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL,
            GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_T,
            GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_SLASH, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_ENTER,
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F1, GLFW.GLFW_KEY_F2,
            GLFW.GLFW_KEY_F3, GLFW.GLFW_KEY_F4, GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_F11,
            GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
            GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7,
            GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9);

    private static boolean tickListenerRegistered;
    private static boolean bindingsChecked;

    private VillageClientKeys() {}

    private static KeyMapping key(String id, int key) {
        return new KeyMapping("key.villageguardians." + id, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : mappings()) event.register(mapping);
        if (!tickListenerRegistered) {
            tickListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageClientKeys::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) migrateBindings(minecraft);
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.gui.screen() != null) {
            for (KeyMapping mapping : mappings()) drain(mapping);
            return;
        }
        consume(ROLE_SKILL_ONE, "use_skill:0");
        consume(ROLE_SKILL_TWO, "use_skill:1");
        consume(QUICK_COMMUNICATION, "open_quick_chat");
        consume(STATUS, "open_status");
        consume(GROWTH, "open_skill_tree");
        consume(ROLE_PROGRESS, "open_role_progress_current");
    }

    public static String skillOneKeyName() { return keyName(ROLE_SKILL_ONE); }
    public static String skillTwoKeyName() { return keyName(ROLE_SKILL_TWO); }
    public static String quickCommunicationKeyName() { return keyName(QUICK_COMMUNICATION); }
    public static String statusKeyName() { return keyName(STATUS); }
    public static String growthKeyName() { return keyName(GROWTH); }
    public static String roleProgressKeyName() { return keyName(ROLE_PROGRESS); }

    public static String compactSummary() {
        return quickCommunicationKeyName() + " 통신 · "
                + skillOneKeyName() + "/" + skillTwoKeyName() + " 기술";
    }

    public static String resolveTokens(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        return value
                .replace("{SKILL1}", skillOneKeyName())
                .replace("{SKILL2}", skillTwoKeyName())
                .replace("{QUICK}", quickCommunicationKeyName())
                .replace("{STATUS}", statusKeyName())
                .replace("{GROWTH}", growthKeyName())
                .replace("{ROLE}", roleProgressKeyName());
    }

    private static List<KeyMapping> mappings() {
        return List.of(ROLE_SKILL_ONE, ROLE_SKILL_TWO, QUICK_COMMUNICATION,
                STATUS, GROWTH, ROLE_PROGRESS);
    }

    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private static void migrateBindings(Minecraft minecraft) {
        if (bindingsChecked) return;
        bindingsChecked = true;

        boolean oldDefaults = keyValue(ROLE_SKILL_ONE) == GLFW.GLFW_KEY_Z
                && keyValue(ROLE_SKILL_TWO) == GLFW.GLFW_KEY_V
                && keyValue(QUICK_COMMUNICATION) == GLFW.GLFW_KEY_B
                && keyValue(STATUS) == GLFW.GLFW_KEY_H
                && keyValue(GROWTH) == GLFW.GLFW_KEY_J
                && keyValue(ROLE_PROGRESS) == GLFW.GLFW_KEY_K;

        Set<Integer> used = new HashSet<>();
        boolean unsafe = false;
        for (KeyMapping mapping : mappings()) {
            int value = keyValue(mapping);
            if (value <= 0 || VANILLA_RESERVED.contains(value) || !used.add(value)) unsafe = true;
        }
        if (!oldDefaults && !unsafe) return;

        set(ROLE_SKILL_ONE, GLFW.GLFW_KEY_Z);
        set(ROLE_SKILL_TWO, GLFW.GLFW_KEY_X);
        set(QUICK_COMMUNICATION, GLFW.GLFW_KEY_V);
        set(STATUS, GLFW.GLFW_KEY_H);
        set(GROWTH, GLFW.GLFW_KEY_J);
        set(ROLE_PROGRESS, GLFW.GLFW_KEY_K);
        KeyMapping.resetMapping();
        minecraft.options.save();
    }

    private static int keyValue(KeyMapping mapping) { return mapping.getKey().getValue(); }

    private static void set(KeyMapping mapping, int key) {
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
    }

    private static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) { }
    }

    private static void consume(KeyMapping mapping, String action) {
        while (mapping.consumeClick()) {
            ClientPacketDistributor.sendToServer(new VillageNetwork.VillageUiActionPayload(action));
        }
    }
}
''')

# Resolve key tokens before every UI screen is constructed.
write(JAVA / "VillageClientUi.java", r'''package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageClientUi {
    private VillageClientUi() {}

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(VillageNetwork.OpenVillageUiPayload.TYPE,
                (payload, context) -> {
                    VillageNetwork.OpenVillageUiPayload resolved = resolve(payload);
                    Minecraft.getInstance().gui.setScreen(
                            switch (resolved.screenId()) {
                                case "skill_tree" -> new VillageSkillTreeScreen(resolved);
                                case "town_hall" -> new VillageTownHallScreen(resolved);
                                case "role_progress", "role_skills" -> new VillageRoleProgressScreen(resolved);
                                case "quick_chat" -> new VillageQuickChatScreen(resolved);
                                case "status" -> new VillageStatusScreen(resolved);
                                case "skill_test_role", "skill_test_skill" -> new VillageSkillTestScreen(resolved);
                                case "skill_test_password" -> new VillageSkillTestPasswordScreen(resolved);
                                case "wave_intel", "skill_test", "game_over" -> new VillageFacilityScreen(resolved);
                                case "equipment_shop" -> new VillageShopScreen(resolved);
                                case "equipment_fusion" -> new VillageFusionScreen(resolved);
                                case "result" -> new VillageResultScreen(resolved);
                                case "building", "management", "funding", "tower_control", "tower_detail", "caller", "relic_choice" ->
                                        new VillageFacilityScreen(resolved);
                                default -> new VillageUiScreen(resolved);
                            });
                });
        event.register(VillageNetwork.SkillMotionPayload.TYPE,
                (payload, context) -> VillageSkillEffectClient.acceptMotion(payload));
        event.register(VillageNetwork.SkillHudPayload.TYPE,
                (payload, context) -> VillageSkillHudOverlay.accept(payload));
        event.register(VillageNetwork.PlayerStatusPayload.TYPE,
                (payload, context) -> VillageInventoryPanel.updateStatus(payload));
    }

    private static VillageNetwork.OpenVillageUiPayload resolve(
            VillageNetwork.OpenVillageUiPayload payload) {
        return new VillageNetwork.OpenVillageUiPayload(
                payload.screenId(),
                VillageClientKeys.resolveTokens(payload.title()),
                VillageClientKeys.resolveTokens(payload.body()),
                payload.actions(),
                VillageClientKeys.resolveTokens(payload.labels()));
    }
}
''')

# Skill HUD token resolution.
hud = JAVA / "VillageSkillHudOverlay.java"
replace_once(
    hud,
    '        text = payload == null || payload.text() == null ? "" : payload.text();\n',
    '        text = payload == null || payload.text() == null ? ""\n'
    '                : VillageClientKeys.resolveTokens(payload.text());\n',
    "skill hud key token resolution",
)

# Dynamic header text in the role skill screen.
role_screen = JAVA / "VillageRoleProgressScreen.java"
replace_once(
    role_screen,
    '            graphics.text(font, fit("기술 습득과 Z/X 장착", Math.max(80, closeX - 18)),\n',
    '            graphics.text(font, fit("기술 습득과 " + VillageClientKeys.skillOneKeyName()\n'
    '                            + "/" + VillageClientKeys.skillTwoKeyName() + " 장착",\n'
    '                    Math.max(80, closeX - 18)),\n',
    "role screen dynamic keys",
)

# ---------------------------------------------------------------------------
# Equipment breadth: 24 named offers, generic per-offer skill/cooldown/defense
# fields, and all named focus bonuses actually wired into role skills.
# ---------------------------------------------------------------------------
write(JAVA / "VillageEquipmentShop.java", r'''package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class VillageEquipmentShop {
    private VillageEquipmentShop() {}

    public static List<Offer> offers() { return List.of(Offer.values()); }

    public static List<Offer> offers(Category category) {
        return Arrays.stream(Offer.values()).filter(offer -> offer.category() == category).toList();
    }

    public static List<Offer> currentOffers(int day) {
        int safeDay = Math.max(1, day);
        List<Offer> result = new ArrayList<>();
        result.addAll(rotatingOffers(Category.EQUIPMENT, safeDay, 4));
        result.addAll(rotatingOffers(Category.ARMOR, safeDay, 3));
        return List.copyOf(result);
    }

    public static boolean isStocked(Offer offer, int day) {
        return offer != null && currentOffers(day).contains(offer);
    }

    private static List<Offer> rotatingOffers(Category category, int day, int maximum) {
        List<Offer> eligible = Arrays.stream(Offer.values())
                .filter(offer -> offer.category() == category && offer.requiredDay() <= day)
                .toList();
        if (eligible.isEmpty()) return List.of();
        int count = Math.min(maximum, eligible.size());
        int start = Math.floorMod(day * 3 - 3 + category.ordinal() * 5, eligible.size());
        List<Offer> selected = new ArrayList<>();
        for (int index = 0; index < count; index++) selected.add(eligible.get((start + index) % eligible.size()));
        return List.copyOf(selected);
    }

    public static String purchase(ServerPlayer player, String offerId) {
        Offer offer = Offer.parse(offerId).orElse(null);
        if (offer == null) return "알 수 없는 장비 상품입니다.";
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.STOREHOUSE)) {
            return "상점이 파괴되어 장비를 구매할 수 없습니다.";
        }
        int day = VillageCouncilState.currentDay();
        if (!isStocked(offer, day)) return "오늘 입고된 상품이 아닙니다. 상점 목록을 다시 확인하세요.";
        if (!VillageProgressionSystem.spendCoins(player, offer.cost())) {
            return "수호 주화가 부족합니다. 필요 " + offer.cost();
        }
        ItemStack stack = offer.createStack();
        if (!player.addItem(stack)) player.drop(stack, false);
        return offer.displayName() + " 구매 완료 | 남은 주화 " + VillageProgressionSystem.coins(player);
    }

    public static String status(ServerPlayer player, Offer offer) {
        if (!isStocked(offer, VillageCouncilState.currentDay())) return "오늘 미입고";
        if (VillageProgressionSystem.coins(player) < offer.cost()) return "주화 " + offer.cost() + " 필요";
        return "available";
    }

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        float named = projectile ? bonusFor(player.getMainHandItem(), true) : bonusFor(player.getMainHandItem(), false);
        if (projectile) named = Math.max(named, bonusFor(player.getOffhandItem(), true));
        float rarity = projectile
                ? Math.max(VillageEquipmentRaritySystem.projectileMultiplier(player.getMainHandItem()),
                VillageEquipmentRaritySystem.projectileMultiplier(player.getOffhandItem()))
                : VillageEquipmentRaritySystem.meleeMultiplier(player.getMainHandItem());
        float relic = projectile ? VillageRelicSystem.projectileMultiplier(player)
                : VillageRelicSystem.meleeMultiplier(player);
        return named * rarity * relic;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = equippedOffers(player).stream()
                .map(Offer::damageReduction)
                .reduce(0.0f, Float::sum);
        float rarityMultiplier = VillageEquipmentRaritySystem.incomingMultiplier(player);
        return Math.max(0.52f, (1.0f - Math.min(0.42f, reduction)) * rarityMultiplier
                * VillageRelicSystem.incomingMultiplier(player));
    }

    public static float roleSkillMultiplier(ServerPlayer player) {
        float named = 1.0f;
        for (Offer offer : equippedOffers(player)) named *= offer.skillMultiplier();
        return named * VillageEquipmentRaritySystem.skillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player);
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        int result = 0;
        for (Offer offer : equippedOffers(player)) result += offer.cooldownReductionSeconds();
        return Math.min(4, result);
    }

    private static Set<Offer> equippedOffers(ServerPlayer player) {
        EnumSet<Offer> result = EnumSet.noneOf(Offer.class);
        collect(result, player.getMainHandItem());
        collect(result, player.getOffhandItem());
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            collect(result, player.getItemBySlot(slot));
        }
        return result;
    }

    private static void collect(Set<Offer> output, ItemStack stack) {
        for (Offer offer : Offer.values()) if (offer.matches(stack)) output.add(offer);
    }

    private static float bonusFor(ItemStack stack, boolean projectile) {
        Offer offer = Arrays.stream(Offer.values()).filter(value -> value.matches(stack)).findFirst().orElse(null);
        if (offer == null) return 1.0f;
        return projectile ? offer.projectileMultiplier() : offer.meleeMultiplier();
    }

    public enum Category {
        EQUIPMENT("장비"), ARMOR("방어구"), OTHER("기타");
        private final String displayName;
        Category(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Offer {
        WATCH_SWORD("watch_sword", "파수대 철검", Category.EQUIPMENT, Items.IRON_SWORD, 1, 90,
                "근접 피해 +8%", 1.08f, 1.00f, 1.00f, 0.00f, 0),
        HUNTER_BOW("hunter_bow", "성루 사냥활", Category.EQUIPMENT, Items.BOW, 2, 140,
                "원거리 피해 +10%", 1.00f, 1.10f, 1.00f, 0.00f, 0),
        WARD_SHIELD("ward_shield", "수호 문양 방패", Category.ARMOR, Items.SHIELD, 2, 170,
                "받는 피해 5% 감소", 1.00f, 1.00f, 1.00f, 0.05f, 0),
        SENTINEL_AXE("sentinel_axe", "문지기 파쇄도끼", Category.EQUIPMENT, Items.IRON_AXE, 3, 230,
                "근접 피해 +13%", 1.13f, 1.00f, 1.00f, 0.00f, 0),
        TWINSTRING_BOW("twinstring_bow", "쌍현 전투궁", Category.EQUIPMENT, Items.BOW, 4, 315,
                "원거리 피해 +15% · 기술 재사용 -1초", 1.00f, 1.15f, 1.00f, 0.00f, 1),
        BULWARK_HELM("bulwark_helm", "성루 방벽투구", Category.ARMOR, Items.DIAMOND_HELMET, 4, 350,
                "받는 피해 6% 감소", 1.00f, 1.00f, 1.00f, 0.06f, 0),
        VETERAN_BLADE("veteran_blade", "노련한 수호검", Category.EQUIPMENT, Items.DIAMOND_SWORD, 4, 330,
                "근접 피해 +17%", 1.17f, 1.00f, 1.00f, 0.00f, 0),
        SIEGE_CROSSBOW("siege_crossbow", "공성 파쇄쇠뇌", Category.EQUIPMENT, Items.CROSSBOW, 5, 390,
                "원거리 피해 +20%", 1.00f, 1.20f, 1.00f, 0.00f, 0),
        WIND_BLADE("wind_blade", "질풍 호위검", Category.EQUIPMENT, Items.DIAMOND_SWORD, 5, 430,
                "근접 피해 +15% · 기술 효과 +5% · 재사용 -1초", 1.15f, 1.00f, 1.05f, 0.00f, 1),
        MARCH_BOOTS("march_boots", "진군자의 전투화", Category.ARMOR, Items.DIAMOND_BOOTS, 5, 420,
                "받는 피해 4% 감소 · 기술 재사용 -1초", 1.00f, 1.00f, 1.00f, 0.04f, 1),
        ARCANE_FOCUS("arcane_focus", "비전 집중봉", Category.EQUIPMENT, Items.BLAZE_ROD, 5, 420,
                "직업 기술 피해·치유 +18%", 1.00f, 1.00f, 1.18f, 0.00f, 0),
        BASTION_CHEST("bastion_chest", "성채 수호 흉갑", Category.ARMOR, Items.DIAMOND_CHESTPLATE, 6, 560,
                "받는 피해 8% 감소", 1.00f, 1.00f, 1.00f, 0.08f, 0),
        EAGLE_CROSSBOW("eagle_crossbow", "독수리 추격쇠뇌", Category.EQUIPMENT, Items.CROSSBOW, 7, 650,
                "원거리 피해 +23% · 기술 재사용 -1초", 1.00f, 1.23f, 1.00f, 0.00f, 1),
        FROST_FOCUS("frost_focus", "서리결정 지휘봉", Category.EQUIPMENT, Items.BLAZE_ROD, 7, 680,
                "직업 기술 효과 +22% · 재사용 -1초", 1.00f, 1.00f, 1.22f, 0.00f, 1),
        RUNE_LEGGINGS("rune_leggings", "룬각인 전투각반", Category.ARMOR, Items.DIAMOND_LEGGINGS, 7, 710,
                "받는 피해 7% 감소 · 기술 효과 +8%", 1.00f, 1.00f, 1.08f, 0.07f, 0),
        EXECUTIONER_AXE("executioner_axe", "처형대장의 흑도끼", Category.EQUIPMENT, Items.NETHERITE_AXE, 8, 840,
                "근접 피해 +25%", 1.25f, 1.00f, 1.00f, 0.00f, 0),
        TITAN_SHIELD("titan_shield", "거신의 성문방패", Category.ARMOR, Items.SHIELD, 8, 820,
                "받는 피해 11% 감소", 1.00f, 1.00f, 1.00f, 0.11f, 0),
        AEGIS_CHEST("aegis_chest", "최후 방벽 흉갑", Category.ARMOR, Items.NETHERITE_CHESTPLATE, 9, 900,
                "받는 피해 10% 감소", 1.00f, 1.00f, 1.00f, 0.10f, 0),
        DAWN_BLADE("dawn_blade", "새벽 절단검", Category.EQUIPMENT, Items.NETHERITE_SWORD, 10, 980,
                "근접 피해 +28%", 1.28f, 1.00f, 1.00f, 0.00f, 0),
        STAR_BOW("star_bow", "별빛 장궁", Category.EQUIPMENT, Items.BOW, 10, 980,
                "원거리 피해 +28%", 1.00f, 1.28f, 1.00f, 0.00f, 0),
        DAWN_SCEPTER("dawn_scepter", "여명 성광홀", Category.EQUIPMENT, Items.BLAZE_ROD, 11, 1160,
                "직업 기술 효과 +30% · 재사용 -2초", 1.00f, 1.00f, 1.30f, 0.00f, 2),
        RIFT_LONGBOW("rift_longbow", "균열 관통장궁", Category.EQUIPMENT, Items.BOW, 12, 1250,
                "원거리 피해 +33% · 기술 효과 +5%", 1.00f, 1.33f, 1.05f, 0.00f, 0),
        PHOENIX_CHEST("phoenix_chest", "불사조 수호흉갑", Category.ARMOR, Items.NETHERITE_CHESTPLATE, 12, 1320,
                "받는 피해 13% 감소 · 기술 효과 +8%", 1.00f, 1.00f, 1.08f, 0.13f, 0),
        WAR_CROWN("war_crown", "끝없는 전쟁왕관", Category.ARMOR, Items.NETHERITE_HELMET, 14, 1580,
                "받는 피해 10% 감소 · 기술 효과 +12% · 재사용 -1초", 1.00f, 1.00f, 1.12f, 0.10f, 1);

        private final String id;
        private final String displayName;
        private final Category category;
        private final Item item;
        private final int requiredDay;
        private final int cost;
        private final String effect;
        private final float meleeMultiplier;
        private final float projectileMultiplier;
        private final float skillMultiplier;
        private final float damageReduction;
        private final int cooldownReductionSeconds;

        Offer(String id, String displayName, Category category, Item item, int requiredDay, int cost,
              String effect, float meleeMultiplier, float projectileMultiplier,
              float skillMultiplier, float damageReduction, int cooldownReductionSeconds) {
            this.id = id;
            this.displayName = displayName;
            this.category = category;
            this.item = item;
            this.requiredDay = requiredDay;
            this.cost = cost;
            this.effect = effect;
            this.meleeMultiplier = meleeMultiplier;
            this.projectileMultiplier = projectileMultiplier;
            this.skillMultiplier = skillMultiplier;
            this.damageReduction = damageReduction;
            this.cooldownReductionSeconds = cooldownReductionSeconds;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Category category() { return category; }
        public int requiredDay() { return requiredDay; }
        @Deprecated public int requiredLevel() { return 0; }
        public int cost() { return cost; }
        public String effect() { return effect; }
        public float meleeMultiplier() { return meleeMultiplier; }
        public float projectileMultiplier() { return projectileMultiplier; }
        public float skillMultiplier() { return skillMultiplier; }
        public float damageReduction() { return damageReduction; }
        public int cooldownReductionSeconds() { return cooldownReductionSeconds; }

        public VillageEquipmentRaritySystem.Rarity rarity() {
            if (requiredDay >= 9) return VillageEquipmentRaritySystem.Rarity.LEGENDARY;
            if (requiredDay >= 6) return VillageEquipmentRaritySystem.Rarity.EPIC;
            if (requiredDay >= 4) return VillageEquipmentRaritySystem.Rarity.RARE;
            if (requiredDay >= 2) return VillageEquipmentRaritySystem.Rarity.UNCOMMON;
            return VillageEquipmentRaritySystem.Rarity.COMMON;
        }

        public ItemStack createStack() {
            return VillageEquipmentRaritySystem.createNamed(item, rarity(), displayName);
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == item
                    && displayName.equals(VillageEquipmentRaritySystem.baseDisplayName(stack));
        }

        public static Optional<Offer> parse(String value) {
            if (value == null) return Optional.empty();
            String normalized = value.toLowerCase(Locale.ROOT);
            return Arrays.stream(values()).filter(offer -> offer.id.equals(normalized)).findFirst();
        }
    }
}
''')

# ---------------------------------------------------------------------------
# Ten relics, including cooldown and mixed-playstyle choices.
# ---------------------------------------------------------------------------
write(JAVA / "VillageRelicSystem.java", r'''package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VillageRelicSystem {
    private static final String SEP = "\u001F";
    private static final Map<UUID, Integer> OWNED = new LinkedHashMap<>();
    private static final Map<UUID, String> PENDING = new LinkedHashMap<>();
    private static VillageRelicData savedData;

    private VillageRelicSystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageRelicData.TYPE);
        OWNED.clear(); PENDING.clear();
        savedData.owned().forEach((key, value) -> parseUuid(key, uuid -> OWNED.put(uuid, value)));
        savedData.pending().forEach((key, value) -> parseUuid(key, uuid -> PENDING.put(uuid, value)));
        persist();
    }

    public static synchronized void offerToParty(MinecraftServer server) {
        int day = VillageCouncilState.currentDay();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<Relic> choices = choicesFor(player, day);
            if (choices.isEmpty()) continue;
            PENDING.put(player.getUUID(), choices.stream().map(Relic::id).reduce((a, b) -> a + "," + b).orElse(""));
            persist();
            openChoice(player);
        }
    }

    public static synchronized void openChoice(ServerPlayer player) {
        List<Relic> choices = pendingChoices(player);
        if (choices.isEmpty()) return;
        List<String> actions = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Relic relic : choices) {
            actions.add("relic_select:" + relic.id());
            labels.add(relic.displayName() + "|" + relic.description());
        }
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                "relic_choice", "보스 유물 선택",
                "보스를 쓰러뜨렸습니다. 세 유물 중 하나를 선택하면 이 플레이어에게 영구 적용됩니다.",
                String.join(SEP, actions), String.join(SEP, labels)));
    }

    public static synchronized String select(ServerPlayer player, String id) {
        Relic relic = Relic.fromId(id);
        if (relic == null) return "알 수 없는 유물입니다.";
        List<Relic> choices = pendingChoices(player);
        if (!choices.contains(relic)) return "현재 제시된 유물이 아닙니다.";
        int mask = OWNED.getOrDefault(player.getUUID(), 0);
        OWNED.put(player.getUUID(), mask | relic.bit());
        PENDING.remove(player.getUUID());
        persist();
        return relic.displayName() + " 획득 · " + relic.description();
    }

    public static synchronized boolean has(ServerPlayer player, Relic relic) {
        return player != null && relic != null && (OWNED.getOrDefault(player.getUUID(), 0) & relic.bit()) != 0;
    }

    public static synchronized void resetForNewGame() { OWNED.clear(); PENDING.clear(); persist(); }

    public static float meleeMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.WAR_SIGIL)) result *= 1.08f;
        if (has(player, Relic.EXECUTION_EDGE)) result *= 1.06f;
        if (has(player, Relic.BLOOD_CHALICE)) result *= 1.05f;
        if (has(player, Relic.STORM_FEATHER)) result *= 1.03f;
        return result;
    }

    public static float projectileMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.HUNTERS_EYE)) result *= 1.11f;
        if (has(player, Relic.WAR_SIGIL)) result *= 1.04f;
        if (has(player, Relic.STORM_FEATHER)) result *= 1.07f;
        return result;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.WARD_STONE)) result *= 0.91f;
        if (has(player, Relic.LAST_LIGHT)) result *= 0.95f;
        if (has(player, Relic.BASTION_CORE)) result *= 0.94f;
        if (has(player, Relic.STORM_FEATHER)) result *= 0.98f;
        return result;
    }

    public static float skillMultiplier(ServerPlayer player) {
        float result = 1.0f;
        if (has(player, Relic.ARCANE_HEART)) result *= 1.12f;
        if (has(player, Relic.LAST_LIGHT)) result *= 1.05f;
        if (has(player, Relic.DAWN_PRISM)) result *= 1.08f;
        return result;
    }

    public static int cooldownReductionSeconds(ServerPlayer player) {
        return has(player, Relic.CHRONO_SHARD) ? 2 : 0;
    }

    public static float vanguardLifeStealBonus(ServerPlayer player) {
        return has(player, Relic.BLOOD_CHALICE) ? 0.025f : 0.0f;
    }

    public static synchronized String summary(ServerPlayer player) {
        List<String> names = new ArrayList<>();
        for (Relic relic : Relic.values()) if (has(player, relic)) names.add(relic.displayName());
        return names.isEmpty() ? "없음" : String.join(" · ", names);
    }

    private static List<Relic> choicesFor(ServerPlayer player, int day) {
        List<Relic> available = new ArrayList<>();
        int mask = OWNED.getOrDefault(player.getUUID(), 0);
        for (Relic relic : Relic.values()) if ((mask & relic.bit()) == 0) available.add(relic);
        if (available.isEmpty()) return List.of();
        List<Relic> result = new ArrayList<>();
        int seed = player.getUUID().hashCode() * 31 + day * 17 + Integer.bitCount(mask) * 13;
        while (!available.isEmpty() && result.size() < 3) {
            int index = Math.floorMod(seed + result.size() * 37, available.size());
            result.add(available.remove(index));
        }
        return result;
    }

    private static List<Relic> pendingChoices(ServerPlayer player) {
        String raw = PENDING.getOrDefault(player.getUUID(), "");
        List<Relic> result = new ArrayList<>();
        for (String id : raw.split(",")) {
            Relic relic = Relic.fromId(id);
            if (relic != null) result.add(relic);
        }
        return result;
    }

    private static void persist() {
        if (savedData == null) return;
        Map<String, Integer> owned = new LinkedHashMap<>();
        OWNED.forEach((uuid, value) -> owned.put(uuid.toString(), value));
        Map<String, String> pending = new LinkedHashMap<>();
        PENDING.forEach((uuid, value) -> pending.put(uuid.toString(), value));
        savedData.replace(owned, pending);
    }

    private static void parseUuid(String value, java.util.function.Consumer<UUID> consumer) {
        try { consumer.accept(UUID.fromString(value)); }
        catch (IllegalArgumentException ignored) { }
    }

    public enum Relic {
        WAR_SIGIL("war_sigil", "전쟁의 인장", "근접 피해 +8%, 원거리 피해 +4%"),
        HUNTERS_EYE("hunters_eye", "추적자의 눈", "원거리 피해 +11%"),
        WARD_STONE("ward_stone", "수호석", "받는 피해 9% 감소"),
        ARCANE_HEART("arcane_heart", "비전 심장", "직업 기술 피해·치유 +12%"),
        EXECUTION_EDGE("execution_edge", "처형의 칼날", "근접 피해 +6% 및 마무리 전투 강화"),
        LAST_LIGHT("last_light", "마지막 등불", "받는 피해 5% 감소, 기술 효과 +5%"),
        CHRONO_SHARD("chrono_shard", "시간균열 파편", "모든 직업 기술 재사용 대기시간 2초 감소"),
        BLOOD_CHALICE("blood_chalice", "붉은 성배", "근접 피해 +5%, 선봉검사 흡혈 추가 강화"),
        BASTION_CORE("bastion_core", "성채의 심핵", "받는 피해 6% 감소"),
        DAWN_PRISM("dawn_prism", "여명의 프리즘", "직업 기술 피해·치유 +8%"),
        STORM_FEATHER("storm_feather", "폭풍매의 깃", "원거리 피해 +7%, 근접 피해 +3%, 받는 피해 2% 감소");

        private final String id;
        private final String displayName;
        private final String description;

        Relic(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }
        public int bit() { return 1 << ordinal(); }

        public static Relic fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Relic relic : values()) if (relic.id.equals(normalized)) return relic;
            return null;
        }
    }
}
''')

# ---------------------------------------------------------------------------
# Twelve deterministic wave traits.
# ---------------------------------------------------------------------------
write(JAVA / "VillageWaveTrait.java", r'''package kr.moonseungjun.villageguardians;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public enum VillageWaveTrait {
    STANDARD("standard", "정규 진군", "균형 잡힌 병력이 순서대로 밀고 들어옵니다.",
            "특정 약점이 없습니다. 성벽과 플레이어 화력을 고르게 운용하세요.",
            1.00f, 0, 0, 0, 1.00f, 1.00f),
    SWARM("swarm", "물량 공세", "체력이 낮은 돌격병과 척후병이 대량으로 합류합니다.",
            "화염탑·빙결탑·광역 기술이 특히 효과적입니다.",
            1.42f, -1, 0, 1, 0.90f, 0.82f),
    IRONCLAD("ironclad", "철갑 대열", "방패병과 중장갑 병력이 느리지만 단단하게 전진합니다.",
            "노포의 관통 분기, 마법 피해와 방어 약화가 유리합니다.",
            0.84f, 2, 0, -1, 0.92f, 1.24f),
    SIEGE("siege", "공성 대열", "폭파병과 성벽 파쇄병이 시설을 집중 공격합니다.",
            "우선 표적을 빠르게 제거하고 북문에 용병을 집중하세요.",
            0.96f, 1, 1, 0, 1.72f, 1.08f),
    HUNTERS("hunters", "사냥꾼 부대", "사수와 탑 사냥꾼이 후방에서 포탑과 수호자를 노립니다.",
            "성루사수와 근접 돌격으로 원거리 대열을 먼저 끊으세요.",
            0.92f, 0, 1, 1, 1.12f, 0.98f),
    HEXED("hexed", "저주 의식", "주술사와 사령술사가 약화·회복·증원을 반복합니다.",
            "주술사를 우선 처치하고 정화 기술과 비전탑을 준비하세요.",
            0.90f, 1, 0, 0, 1.18f, 1.10f),
    FRENZY("frenzy", "광란 돌격", "모든 적의 이동과 공격이 빨라집니다.",
            "빙결·둔화와 철벽수호자의 저항 기술이 핵심입니다.",
            1.08f, 0, 1, 2, 1.28f, 0.94f),
    REGENERATING("regenerating", "불사 행렬", "적이 지속적으로 회복하고 치유병이 전열을 유지합니다.",
            "한 대상을 집중 공격하고 화염·처형 효과로 회복을 끊으세요.",
            0.88f, 1, 0, 0, 1.08f, 1.16f),
    PHALANX("phalanx", "방진 행군", "방패병·파쇄병·전쟁 고수가 밀집 대형으로 전진합니다.",
            "광역 마법과 측후방 공격으로 대형을 무너뜨리세요.",
            0.82f, 2, 0, -1, 1.24f, 1.30f),
    BLOOD_MOON("blood_moon", "혈월 습격", "광전사들이 강한 공격력과 재생을 지닌 채 몰려옵니다.",
            "짧은 시간에 집중 화력을 쏟아 회복 전에 마무리하세요.",
            1.12f, 0, 2, 1, 1.30f, 1.02f),
    STORMFRONT("stormfront", "폭풍 전선", "고속 원거리 병력과 주술사가 끊임없이 진형을 바꿉니다.",
            "엄폐와 추적 기술을 활용하고 후방 사수를 먼저 제거하세요.",
            0.98f, 0, 1, 2, 1.18f, 1.04f),
    RIFTED("rifted", "균열 군세", "사령술사와 정예병이 보호막을 두르고 혼합 편성으로 진군합니다.",
            "정화·폭발·관통 피해를 함께 운용해 보호막과 지원병을 동시에 끊으세요.",
            0.86f, 1, 1, 1, 1.38f, 1.22f);

    private static final int LONG_EFFECT_TICKS = 20 * 60 * 30;

    private final String id;
    private final String displayName;
    private final String description;
    private final String counterHint;
    private final float countMultiplier;
    private final int healthBonus;
    private final int strengthBonus;
    private final int speedBonus;
    private final float structureDamageMultiplier;
    private final float healthScale;

    VillageWaveTrait(String id, String displayName, String description, String counterHint,
                     float countMultiplier, int healthBonus, int strengthBonus, int speedBonus,
                     float structureDamageMultiplier, float healthScale) {
        this.id = id; this.displayName = displayName; this.description = description;
        this.counterHint = counterHint; this.countMultiplier = countMultiplier;
        this.healthBonus = healthBonus; this.strengthBonus = strengthBonus; this.speedBonus = speedBonus;
        this.structureDamageMultiplier = structureDamageMultiplier; this.healthScale = healthScale;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public String counterHint() { return counterHint; }
    public float structureDamageMultiplier() { return structureDamageMultiplier; }
    public float healthScale() { return healthScale; }

    public int adjustedCount(int baseCount) {
        return Math.max(1, Math.round(Math.max(1, baseCount) * countMultiplier));
    }

    public void applyLongEffects(Mob mob) {
        if (healthBonus > 0) mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_EFFECT_TICKS, healthBonus - 1));
        if (strengthBonus > 0) mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG_EFFECT_TICKS, strengthBonus - 1));
        if (speedBonus > 0) mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG_EFFECT_TICKS, speedBonus - 1));
        if (this == REGENERATING || this == BLOOD_MOON) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, LONG_EFFECT_TICKS, this == BLOOD_MOON ? 1 : 0));
        }
        if (this == IRONCLAD || this == PHALANX) {
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, this == PHALANX ? 1 : 0));
        }
        if (this == STORMFRONT) mob.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, LONG_EFFECT_TICKS, 1));
        if (this == RIFTED) mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG_EFFECT_TICKS, 2));
    }

    public static VillageWaveTrait select(int day, int wave) {
        if (day <= 1 && wave <= 1) return STANDARD;
        List<VillageWaveTrait> unlocked = new ArrayList<>();
        unlocked.add(STANDARD);
        if (day >= 2) unlocked.add(SWARM);
        if (day >= 3) unlocked.add(IRONCLAD);
        if (day >= 4) unlocked.add(SIEGE);
        if (day >= 5) unlocked.add(HUNTERS);
        if (day >= 6) unlocked.add(HEXED);
        if (day >= 7) unlocked.add(FRENZY);
        if (day >= 8) unlocked.add(REGENERATING);
        if (day >= 9) unlocked.add(PHALANX);
        if (day >= 10) unlocked.add(BLOOD_MOON);
        if (day >= 11) unlocked.add(STORMFRONT);
        if (day >= 12) unlocked.add(RIFTED);
        int index = Math.floorMod(day * 37 + wave * 19 + day * wave * 3, unlocked.size());
        return unlocked.get(index);
    }

    public static VillageWaveTrait fromId(String id) {
        if (id == null) return STANDARD;
        for (VillageWaveTrait trait : values()) if (trait.id.equals(id)) return trait;
        return STANDARD;
    }
}
''')

# New boss-aspect system: 6 persistent-for-wave combat identities multiplied
# across the existing 4 boss bodies, yielding 24 mechanically distinct boss
# combinations without save-schema changes.
write(JAVA / "VillageBossAspectSystem.java", r'''package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VillageBossAspectSystem {
    private static final int LONG = 20 * 60 * 30;
    private static final Map<UUID, Aspect> ACTIVE = new HashMap<>();

    private VillageBossAspectSystem() {}

    public static void reset() { ACTIVE.clear(); }
    public static void forget(UUID id) { if (id != null) ACTIVE.remove(id); }

    public static Aspect preview(int day, int wave, int bossIndex) {
        Aspect[] values = Aspect.values();
        return values[Math.floorMod(day * 31 + wave * 17 + bossIndex * 13, values.length)];
    }

    public static void configure(ServerLevel level, Mob mob, int day, int wave, int bossIndex) {
        Aspect aspect = preview(day, wave, bossIndex);
        ACTIVE.put(mob.getUUID(), aspect);
        Component base = mob.getCustomName();
        mob.setCustomName(Component.literal("§4[" + aspect.displayName() + "] §f"
                + (base == null ? "우두머리" : base.getString())));
        switch (aspect) {
            case BERSERKER -> {
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG, 1));
            }
            case BULWARK -> {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG, 4));
            }
            case BLOODBOUND -> mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, LONG, 1));
            case STORMCALLER -> mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG, 1));
            case WARLEADER -> mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG, 1));
            case WALLBREAKER -> mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG, 1));
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 1.15f, 0.65f + aspect.ordinal() * 0.06f);
    }

    public static void tick(ServerLevel level, MinecraftServer server, Mob mob, int globalTicks) {
        Aspect aspect = ACTIVE.get(mob.getUUID());
        if (aspect == null || !mob.isAlive()) return;
        switch (aspect) {
            case BERSERKER -> {
                if (globalTicks % 70 != 0) return;
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 3));
            }
            case BULWARK -> {
                if (globalTicks % 150 != 0) return;
                mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 4));
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 8.0, 10, mob.getUUID())) {
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
                }
            }
            case BLOODBOUND -> {
                if (globalTicks % 100 != 0) return;
                float healed = 0.0f;
                for (ServerPlayer player : nearbyPlayers(server, mob, 11.0)) {
                    player.hurtServer(level, level.damageSources().magic(), 3.5f + VillageCouncilState.currentDay() * 0.16f);
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                    healed += 4.0f;
                }
                if (healed > 0.0f) mob.heal(Math.min(22.0f, healed));
            }
            case STORMCALLER -> {
                if (globalTicks % 80 != 0) return;
                ServerPlayer target = nearbyPlayers(server, mob, 18.0).stream()
                        .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if (target == null) return;
                Vec3 strike = target.position().add(
                        (mob.getRandom().nextDouble() - 0.5) * 2.0, 0.0,
                        (mob.getRandom().nextDouble() - 0.5) * 2.0);
                var lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                if (lightning != null) {
                    lightning.setVisualOnly(true);
                    lightning.setPos(strike.x, strike.y, strike.z);
                    level.addFreshEntity(lightning);
                }
                target.hurtServer(level, level.damageSources().magic(),
                        4.5f + VillageCouncilState.currentDay() * 0.20f);
            }
            case WARLEADER -> {
                if (globalTicks % 120 != 0) return;
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 13.0, 18, mob.getUUID())) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0));
                }
            }
            case WALLBREAKER -> { }
        }
    }

    public static float structureMultiplier(Mob mob) {
        return ACTIVE.get(mob.getUUID()) == Aspect.WALLBREAKER ? 1.55f : 1.0f;
    }

    public static String previewText(int day, int wave, int bossIndex) {
        Aspect aspect = preview(day, wave, bossIndex);
        return aspect.displayName() + " · " + aspect.description();
    }

    private static java.util.List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == mob.level() && player.isAlive()
                        && !player.isSpectator() && player.distanceToSqr(mob) <= squared)
                .toList();
    }

    public enum Aspect {
        BERSERKER("광전", "짧은 주기로 공격력과 이동 속도가 폭증합니다."),
        BULWARK("철벽", "자신과 주변 병력에게 보호막과 저항을 반복 부여합니다."),
        BLOODBOUND("혈계", "주변 수호자의 생명력을 흡수해 스스로 회복합니다."),
        STORMCALLER("뇌광", "주변 수호자 위치에 마법 번개를 반복 호출합니다."),
        WARLEADER("군령", "주변 적 병력의 공격력과 이동 속도를 강화합니다."),
        WALLBREAKER("파성", "시설에 가하는 피해가 크게 증가합니다.");

        private final String displayName;
        private final String description;
        Aspect(String displayName, String description) {
            this.displayName = displayName; this.description = description;
        }
        public String displayName() { return displayName; }
        public String description() { return description; }

        public static Aspect fromName(String name) {
            if (name == null) return null;
            String normalized = name.toUpperCase(Locale.ROOT);
            for (Aspect aspect : values()) if (aspect.name().equals(normalized)) return aspect;
            return null;
        }
    }
}
''')

# ---------------------------------------------------------------------------
# Integrate wave traits and boss aspects.
# ---------------------------------------------------------------------------
enemy = JAVA / "VillageEnemyArchetypeSystem.java"
replace_once(
    enemy,
    '''        if (trait == VillageWaveTrait.REGENERATING) {
            if (day >= 9 && slot % 6 == 0) return Archetype.NECROMANCER;
            return slot % 3 == 0 ? Archetype.HEXER : Archetype.BULWARK;
        }

''',
    '''        if (trait == VillageWaveTrait.REGENERATING) {
            if (day >= 9 && slot % 6 == 0) return Archetype.NECROMANCER;
            return slot % 3 == 0 ? Archetype.HEXER : Archetype.BULWARK;
        }
        if (trait == VillageWaveTrait.PHALANX) {
            if (slot % 6 == 0) return Archetype.WAR_CHANTER;
            return slot % 3 == 0 ? Archetype.SHIELDBREAKER : Archetype.BULWARK;
        }
        if (trait == VillageWaveTrait.BLOOD_MOON) {
            return slot % 5 == 0 ? Archetype.WAR_CHANTER : Archetype.RUSHER;
        }
        if (trait == VillageWaveTrait.STORMFRONT) {
            if (day >= 11 && slot % 5 == 0) return Archetype.TOWER_HUNTER;
            return slot % 3 == 0 ? Archetype.HEXER : Archetype.MARKSMAN;
        }
        if (trait == VillageWaveTrait.RIFTED) {
            if (slot % 6 == 0) return Archetype.NECROMANCER;
            return slot % 3 == 0 ? Archetype.HEXER : Archetype.SHIELDBREAKER;
        }

''',
    "new wave trait rosters",
)

raid = JAVA / "VillageRaidSystem.java"
replace_once(
    raid,
    '''            VillageEnemyArchetypeSystem.configure(
                    level, mob, spawned.archetype(), currentTrait, day, wave, boss);
            mob.addTag(RAID_ENEMY_TAG);
''',
    '''            VillageEnemyArchetypeSystem.configure(
                    level, mob, spawned.archetype(), currentTrait, day, wave, boss);
            if (boss) VillageBossAspectSystem.configure(level, mob, day, wave, index);
            mob.addTag(RAID_ENEMY_TAG);
''',
    "boss aspect configure",
)
replace_once(
    raid,
    '''            VillageEnemyArchetypeSystem.tickAbility(
                    level, server, mob, archetype, currentTrait, abilityTicks);

''',
    '''            VillageEnemyArchetypeSystem.tickAbility(
                    level, server, mob, archetype, currentTrait, abilityTicks);
            if (VillageEnemyArchetypeSystem.isBoss(archetype)) {
                VillageBossAspectSystem.tick(level, server, mob, abilityTicks);
            }

''',
    "boss aspect tick",
)
replace_once(
    raid,
    '''                float multiplier = currentTrait.structureDamageMultiplier()
                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageEnemyArchetypeSystem.structureDamageMultiplier(archetype);
''',
    '''                float multiplier = currentTrait.structureDamageMultiplier()
                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageEnemyArchetypeSystem.structureDamageMultiplier(archetype)
                        * VillageBossAspectSystem.structureMultiplier(mob);
''',
    "boss aspect structure damage",
)
replace_once(
    raid,
    '''    private static void releaseEnemy(MinecraftServer server, UUID uuid, Entity entity) {
        ACTIVE_ARCHETYPES.remove(uuid);
''',
    '''    private static void releaseEnemy(MinecraftServer server, UUID uuid, Entity entity) {
        ACTIVE_ARCHETYPES.remove(uuid);
        VillageBossAspectSystem.forget(uuid);
''',
    "boss aspect cleanup",
)
replace_once(
    raid,
    '''    private static void clearState() {
        ACTIVE_ENEMIES.clear();
        ACTIVE_ARCHETYPES.clear();
''',
    '''    private static void clearState() {
        ACTIVE_ENEMIES.clear();
        ACTIVE_ARCHETYPES.clear();
        VillageBossAspectSystem.reset();
''',
    "boss aspect reset",
)

intel = JAVA / "VillageWaveIntelSystem.java"
replace_once(
    intel,
    '''            Map<VillageEnemyArchetypeSystem.Archetype, Integer> roster = new LinkedHashMap<>();
            for (int index = 0; index < count; index++) {
                boolean boss = index < bosses;
                VillageEnemyArchetypeSystem.Archetype archetype =
                        VillageEnemyArchetypeSystem.previewArchetype(day, wave, index, boss, trait);
                roster.merge(archetype, 1, Integer::sum);
            }
            List<String> lines = new ArrayList<>();
''',
    '''            Map<VillageEnemyArchetypeSystem.Archetype, Integer> roster = new LinkedHashMap<>();
            List<String> bossLines = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                boolean boss = index < bosses;
                VillageEnemyArchetypeSystem.Archetype archetype =
                        VillageEnemyArchetypeSystem.previewArchetype(day, wave, index, boss, trait);
                roster.merge(archetype, 1, Integer::sum);
                if (boss) bossLines.add(archetype.displayName() + " · "
                        + VillageBossAspectSystem.previewText(day, wave, index));
            }
            List<String> lines = new ArrayList<>();
''',
    "boss preview collection",
)
replace_once(
    intel,
    '''            String detail = "예상 총 " + count + "명" + (bosses > 0 ? " · 보스 " + bosses + "명" : "")
                    + "\\n특성: " + trait.description() + "\\n대응: " + trait.counterHint()
                    + "\\n병력:\\n- " + String.join("\\n- ", lines);
''',
    '''            String detail = "예상 총 " + count + "명" + (bosses > 0 ? " · 보스 " + bosses + "명" : "")
                    + "\\n특성: " + trait.description() + "\\n대응: " + trait.counterHint()
                    + (bossLines.isEmpty() ? "" : "\\n보스 변이:\\n- " + String.join("\\n- ", bossLines))
                    + "\\n병력:\\n- " + String.join("\\n- ", lines);
''',
    "boss preview detail",
)

# ---------------------------------------------------------------------------
# Role-skill system: real named equipment/relic multipliers, cooldown gear,
# tokenized key labels, and no stale Z/V descriptions.
# ---------------------------------------------------------------------------
role = JAVA / "VillageRoleSkillSystem.java"
replace_once(
    role,
    '''        return "Z: " + first + " | V: " + second;
''',
    '''        return "{SKILL1}: " + first + " | {SKILL2}: " + second;
''',
    "role loadout key tokens",
)
replace_once(
    role,
    '''        String key = slot == 0 ? "§bZ" : "§dX";
''',
    '''        String key = slot == 0 ? "§b{SKILL1}" : "§d{SKILL2}";
''',
    "role hud key tokens",
)
replace_once(
    role,
    '''                    ? "시험 슬롯 " + (slot == 0 ? "Z" : "V") + "이 비어 있습니다. 시험 관리함에서 기술을 장착하세요."
''',
    '''                    ? "시험 기술 슬롯 " + (slot + 1) + "이 비어 있습니다. 시험 관리함에서 기술을 장착하세요."
''',
    "test empty slot message",
)
replace_once(
    role,
    '''        float power = powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageEquipmentRaritySystem.skillMultiplier(player);
''',
    '''        float power = powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageEquipmentShop.roleSkillMultiplier(player);
''',
    "named equipment role skill multiplier",
)
replace_once(
    role,
    '''                        - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role));
''',
    '''                        - VillageSkillTreeSystem.mobilityCooldownReductionSeconds(player)
                        - roleTreeCooldownReductionSeconds(player, role)
                        - VillageEquipmentShop.cooldownReductionSeconds(player)
                        - VillageRelicSystem.cooldownReductionSeconds(player));
''',
    "equipment relic cooldown",
)

# Skill-test manager uses tokenized defaults; payload resolver converts them to
# actual mappings. Keep chat text generic when it is not a payload.
test_system = JAVA / "VillageSkillTestSystem.java"
text = read(test_system)
text = text.replace("Z/V", "{SKILL1}/{SKILL2}")
text = text.replace('return "Z: " + first + " | V: " + second;',
                    'return "{SKILL1}: " + first + " | {SKILL2}: " + second;')
text = text.replace('safeSlot == 0 ? "Z" : "V"', 'safeSlot == 0 ? "{SKILL1}" : "{SKILL2}"')
write(test_system, text)

controller = JAVA / "VillageUiController.java"
text = read(controller)
text = text.replace("Z/V", "{SKILL1}/{SKILL2}")
text = text.replace('"Z · "', '"{SKILL1} · "')
text = text.replace('"V · "', '"{SKILL2} · "')
text = text.replace("Z 슬롯", "{SKILL1} 슬롯")
text = text.replace("V 슬롯", "{SKILL2} 슬롯")
text = text.replace("Z/X", "{SKILL1}/{SKILL2}")
write(controller, text)

# Client-only visible hints should always show current mappings.
for name in ["VillageSkillTestScreen.java", "VillageFacilityScreen.java"]:
    path = JAVA / name
    text = read(path)
    text = text.replace('"Z/X"', 'VillageClientKeys.skillOneKeyName() + "/" + VillageClientKeys.skillTwoKeyName()')
    text = text.replace('"Z/V"', 'VillageClientKeys.skillOneKeyName() + "/" + VillageClientKeys.skillTwoKeyName()')
    write(path, text)

# ---------------------------------------------------------------------------
# Current content audit.
# ---------------------------------------------------------------------------
write(ROOT / "CONTENT-AUDIT-v0.18.0.md", """# Village Guardians v0.18.0 콘텐츠 감사

## 확장 후 수량

- 직업 5종, 액티브 기술 20종, 직업 패시브 5종
- 공통 성장 노드 50개, 직업 성장 노드 75개
- 상점 고유 장비 24종: 공격·마법 장비 14종, 방어 장비 10종
- 장비 등급 5단계, 개별 강화 최대 +5, 동일 장비 3개 등급 합성
- 일반 적 병과 10종, 기본 보스 4종
- 보스 변이 6종: 기본 보스와 조합되어 총 24개 보스 조합
- 웨이브 특성 12종
- 판매용 전리품 14종
- 포탑 4종, 포탑 전문 분기 12개
- 용병 4병과, 유물 11종

## 실제 폭 증가 내용

장비는 이름만 늘린 것이 아니라 근접·원거리·기술 위력·피해 감소·재사용 대기시간을
서로 다른 조합으로 제공합니다. 기존 비전 집중봉의 고유 기술 배율이 실제 직업 기술 계산에
빠져 있던 문제도 함께 수정했습니다.

보스는 광전·철벽·혈계·뇌광·군령·파성의 여섯 변이를 얻습니다. 각 변이는 가속,
보호막, 흡혈, 번개, 병력 강화, 시설 피해 증가 중 서로 다른 전투 행동을 사용합니다.
기본 보스 4종과 결합되어 24개 조합이 결정론적으로 등장합니다.

웨이브 특성은 기존 8종에서 방진 행군, 혈월 습격, 폭풍 전선, 균열 군세를 추가해
12종이 되었습니다. 신규 특성은 병과 구성과 장기 효과가 서로 다릅니다.

## 캠페인 길이

고정 마지막 날은 없으며 무한 진행입니다. 12일차까지 새 웨이브 특성이 순차 해금되고,
14일차까지 새 상점 장비가 추가됩니다. 20일 이후에도 보스 변이·웨이브 특성·장비 조합이
순환하므로 이전보다 반복 조합 수가 크게 늘었습니다.
""")

print("Applied v0.18.0 core key and content expansion patch")
