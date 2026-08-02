#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"
RES = ROOT / "src/main/resources"


def path(relative: str) -> Path:
    return ROOT / relative


def replace_once(relative: str, old: str, new: str) -> None:
    target = path(relative)
    text = target.read_text(encoding="utf-8")
    if old not in text:
        if new in text:
            return
        raise RuntimeError(f"missing patch anchor in {relative}: {old[:100]!r}")
    if text.count(old) != 1:
        raise RuntimeError(f"ambiguous patch anchor in {relative}: {text.count(old)} matches")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all(relative: str, old: str, new: str, minimum: int = 1) -> None:
    target = path(relative)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count == 0 and new in text:
        return
    if count < minimum:
        raise RuntimeError(f"not enough patch anchors in {relative}: {old!r} ({count})")
    target.write_text(text.replace(old, new), encoding="utf-8")


def write(relative: str, content: str) -> None:
    target = path(relative)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


properties = path("gradle.properties").read_text(encoding="utf-8")
if "mod_version=0.12.1-alpha.6" in properties:
    required = [
        JAVA / "magic/MageGearService.java",
        JAVA / "client/ArcaneClient.java",
        ROOT / "src/main/resources/assets/arcanecircle/items/mage_hat.json",
    ]
    missing = [str(value) for value in required if not value.exists()]
    if missing:
        raise RuntimeError(f"alpha.6 version exists but files are missing: {missing}")
    print("Arcane Circle v0.12.1-alpha.6 gameplay migration already applied")
    raise SystemExit(0)

replace_once("gradle.properties", "mod_version=0.12.1-alpha.5", "mod_version=0.12.1-alpha.6")
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    'public static final String VERSION = "0.12.1-alpha.5";',
    'public static final String VERSION = "0.12.1-alpha.6";',
)
replace_once(
    "src/main/resources/data/arcanecircle/spell_catalog/index.json",
    '"version": "0.12.1-alpha.5"',
    '"version": "0.12.1-alpha.6"',
)

# Number-key chord casting replaces the X modifier entirely.
write("src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneClient.java", '''package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.BeginCastPayload;
import kr.moonseungjun.arcanecircle.network.CommitFusionPayload;
import kr.moonseungjun.arcanecircle.network.QueueFusionPayload;
import kr.moonseungjun.arcanecircle.network.ReleaseCastPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;

public final class ArcaneClient {
    private static final KeyMapping GRIMOIRE_KEY = new KeyMapping(
            "key.arcanecircle.grimoire", InputConstants.KEY_C, KeyMapping.Category.MISC);
    private static final KeyMapping[] SLOT_KEYS = {
            new KeyMapping("key.arcanecircle.slot_1", InputConstants.KEY_1, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_2", InputConstants.KEY_2, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_3", InputConstants.KEY_3, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_4", InputConstants.KEY_4, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_5", InputConstants.KEY_5, KeyMapping.Category.MISC)
    };
    private static final boolean[] SLOT_WAS_DOWN = new boolean[5];
    private static final boolean[] FUSION_QUEUED = new boolean[5];
    private static int primarySlot = -1;
    private static boolean fusionChord;
    private static int protectedSelectedSlot = -1;
    private static boolean numberInputActive;

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        for (KeyMapping key : SLOT_KEYS) event.register(key);
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) return;
        protectedSelectedSlot = minecraft.player.getInventory().getSelectedSlot();
        numberInputActive = false;
        for (KeyMapping vanilla : minecraft.options.keyHotbarSlots) {
            numberInputActive |= vanilla.isDown();
            vanilla.setDown(false);
            while (vanilla.consumeClick()) {}
        }
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetInput();
            ArcaneClientState.reset();
            drainClicks();
            return;
        }
        if (numberInputActive && protectedSelectedSlot >= 0
                && minecraft.player.getInventory().getSelectedSlot() != protectedSelectedSlot) {
            minecraft.player.getInventory().setSelectedSlot(protectedSelectedSlot);
        }
        if (minecraft.gui.screen() != null) {
            while (GRIMOIRE_KEY.consumeClick()) {}
            drainSlotClicks();
            if (primarySlot >= 0 || fusionChord) {
                ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            }
            resetCastChord();
            return;
        }
        while (GRIMOIRE_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("atlas"));
        }

        boolean[] down = new boolean[SLOT_KEYS.length];
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) down[slot] = SLOT_KEYS[slot].isDown();

        // Process new presses first. This lets a secondary key join the chord even on the
        // same tick that the primary key is released.
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            if (!down[slot] || SLOT_WAS_DOWN[slot]) continue;
            if (primarySlot < 0) {
                primarySlot = slot;
                fusionChord = false;
                Arrays.fill(FUSION_QUEUED, false);
                ClientPacketDistributor.sendToServer(new BeginCastPayload(slot));
            } else if (slot != primarySlot) {
                if (!fusionChord) {
                    ClientPacketDistributor.sendToServer(new QueueFusionPayload(primarySlot));
                    FUSION_QUEUED[primarySlot] = true;
                    fusionChord = true;
                }
                if (!FUSION_QUEUED[slot]) {
                    ClientPacketDistributor.sendToServer(new QueueFusionPayload(slot));
                    FUSION_QUEUED[slot] = true;
                }
            }
        }

        boolean primaryReleased = primarySlot >= 0
                && !down[primarySlot] && SLOT_WAS_DOWN[primarySlot];
        if (primaryReleased) {
            if (fusionChord) ClientPacketDistributor.sendToServer(new CommitFusionPayload(0));
            else ClientPacketDistributor.sendToServer(new ReleaseCastPayload(primarySlot));
            resetCastChord();
        }

        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            SLOT_WAS_DOWN[slot] = down[slot];
            while (SLOT_KEYS[slot].consumeClick()) {}
        }
    }

    private static void resetCastChord() {
        primarySlot = -1;
        fusionChord = false;
        Arrays.fill(FUSION_QUEUED, false);
    }

    private static void resetInput() {
        resetCastChord();
        protectedSelectedSlot = -1;
        numberInputActive = false;
        Arrays.fill(SLOT_WAS_DOWN, false);
    }

    private static void drainClicks() {
        while (GRIMOIRE_KEY.consumeClick()) {}
        drainSlotClicks();
    }

    private static void drainSlotClicks() {
        for (KeyMapping key : SLOT_KEYS) while (key.consumeClick()) {}
    }
}
''')

replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    "import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;",
    "import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;\nimport kr.moonseungjun.arcanecircle.magic.MageGearService;",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    '"§71~5를 길게 눌러 회로를 그리고, 완성된 뒤 키를 놓아 시전합니다. X+숫자는 융합입니다."',
    '"§71~5를 길게 눌러 회로를 전개합니다. 누른 채 다른 숫자 주문을 더하면 융합되고, 처음 누른 키를 놓아 시전합니다."',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    "        SpellCastingService.tickCharge(player);\n        MagicWorldService.tick(player);",
    "        SpellCastingService.tickCharge(player);\n        MagicWorldService.tick(player);\n        if (player.tickCount % 10 == 0) MageGearService.tick(player);",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java",
    "        ArcaneNoticeService.clear(event.getEntity().getUUID());",
    "        ArcaneNoticeService.clear(event.getEntity().getUUID());\n        MageGearService.clear(event.getEntity().getUUID());",
)

# Immediate fusion rejection and no X wording.
replace_all(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    "X를 놓아 시전하세요.",
    "처음 누른 주문 키를 놓아 시전하세요.",
)
replace_all(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    "초 유지 후 X를 놓아 시전",
    "초 유지 후 처음 누른 키를 놓아 시전",
)
replace_all(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    "% · 완성 전에 X를 놓았습니다.",
    "% · 완성 전에 처음 누른 키를 놓았습니다.",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java",
    '''            MagicPlayerData.CastPreparation fusion = data(player).prepareFusion(player, queue.ingredients);
            if (fusion.accepted()) {
                WorldMagicService.charge(player, result, true, queue.ingredients, fusion.range(), 0.0);
            }
            String extension = SpellCatalog.canExtend(queue.ingredients) ? " §8· 세 번째 회로 추가 가능" : "";
            ArcaneNoticeService.push(player, Component.literal("§5[융합 전개] §d" + names + " §f→ §e"
                    + result.name() + " §7· " + String.format("%.1f", queue.requiredTicks / 20.0)
                    + "초 유지 후 처음 누른 키를 놓아 시전" + extension));''',
    '''            MagicPlayerData.CastPreparation fusion = data(player).prepareFusion(player, queue.ingredients);
            String extension = SpellCatalog.canExtend(queue.ingredients) ? " · 세 번째 주문 추가 가능" : "";
            if (!fusion.accepted()) {
                queue.resultId = "";
                queue.chargeStartedAt = -1L;
                queue.requiredTicks = 0;
                WorldMagicService.stop(player);
                fail(player, result.name() + " 융합 불가 · " + fusion.message() + extension);
                return;
            }
            WorldMagicService.charge(player, result, true, queue.ingredients, fusion.range(), 0.0);
            ArcaneNoticeService.push(player, Component.literal("§5[융합 전개] §d" + names + " §f→ §e"
                    + result.name() + " §7· " + String.format("%.1f", queue.requiredTicks / 20.0)
                    + "초 유지 후 처음 누른 키를 놓아 시전" + extension));''',
)

# Gear contributes broad secondary stats, while fusion is guaranteed stronger than its strongest source.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        StaffProfile staff = ModItems.equipped(player);
        int maxMana = Math.max(1, state.baseMaxMana() + staff.maxManaBonus());
        if (state.mana > maxMana) {
            state.mana = maxMana;
            setDirty();
        }
        double regen = state.baseRegenPerHalfSecond() * staff.regenMultiplier();
        return new EffectiveStats(maxMana, regen, staff);''',
    '''        StaffProfile staff = ModItems.equipped(player);
        MageGearService.GearStats gear = MageGearService.stats(player);
        int maxMana = Math.max(1, state.baseMaxMana() + staff.maxManaBonus() + gear.maxManaBonus());
        if (state.mana > maxMana) {
            state.mana = maxMana;
            setDirty();
        }
        double regen = state.baseRegenPerHalfSecond() * staff.regenMultiplier() * gear.regenMultiplier();
        return new EffectiveStats(maxMana, regen, staff);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    "        StaffProfile staff = ModItems.equipped(player);\n        kr.moonseungjun.arcanecircle.world.ArcaneWorldData world =",
    "        StaffProfile staff = ModItems.equipped(player);\n        MageGearService.GearStats gear = MageGearService.stats(player);\n        kr.moonseungjun.arcanecircle.world.ArcaneWorldData world =",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/MagicPlayerData.java",
    '''        int manaCost = Math.max(1, (int) Math.ceil(spell.manaCost() * circleMana * masteryMana
                * staff.manaCostMultiplier() * facultyMana));
        int cooldown = Math.max(8, (int) Math.round(spell.cooldownTicks() * circleCooldown * masteryCooldown
                * staff.cooldownMultiplier() * facultyCooldown));
        double range = spell.range() * circleRange * masteryRange * staff.rangeMultiplier() * facultyRange;
        double power = spell.power() * circlePower * masteryPower * staff.powerFor(spell.school()) * facultyPower;''',
    '''        int manaCost = Math.max(1, (int) Math.ceil(spell.manaCost() * circleMana * masteryMana
                * staff.manaCostMultiplier() * gear.manaCostMultiplier() * facultyMana));
        int cooldown = Math.max(8, (int) Math.round(spell.cooldownTicks() * circleCooldown * masteryCooldown
                * staff.cooldownMultiplier() * gear.cooldownMultiplier() * facultyCooldown));
        double range = spell.range() * circleRange * masteryRange * staff.rangeMultiplier()
                * gear.rangeMultiplier() * facultyRange;
        double power = spell.power() * circlePower * masteryPower * staff.powerFor(spell.school())
                * gear.powerMultiplier() * facultyPower;
        if (fusion) {
            double strongestIngredient = 0.0;
            for (String ingredientId : ingredients) {
                SpellDefinition ingredient = SpellCatalog.spell(ingredientId).orElse(null);
                if (ingredient == null) continue;
                int ingredientGap = Math.max(0, state.circle - ingredient.circle());
                int ingredientTier = SpellCatalog.masteryTier(state.mastery(ingredient.id()));
                double ingredientPower = ingredient.power()
                        * (1.0 + ingredientGap * 0.10)
                        * (1.0 + ingredientTier * 0.04)
                        * staff.powerFor(ingredient.school())
                        * gear.powerMultiplier();
                strongestIngredient = Math.max(strongestIngredient, ingredientPower);
            }
            double fusionFloor = strongestIngredient * (ingredients.size() >= 3 ? 1.45 : 1.25);
            power = Math.max(power, fusionFloor);
        }''',
)

# Register functional wearable gear using the vanilla leather equipment asset.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    "import net.minecraft.world.item.ItemStack;",
    "import net.minecraft.world.item.Item;\nimport net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.item.equipment.ArmorMaterials;\nimport net.minecraft.world.item.equipment.ArmorType;",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    '''    public static final DeferredItem<ArcaneStaffItem> ARCHMAGE_STAFF = registerStaff("archmage_staff", ARCHMAGE_PROFILE, Rarity.EPIC);

    public static final DeferredItem<BeginnerGrimoireItem> BEGINNER_GRIMOIRE = ITEMS.registerItem(''',
    '''    public static final DeferredItem<ArcaneStaffItem> ARCHMAGE_STAFF = registerStaff("archmage_staff", ARCHMAGE_PROFILE, Rarity.EPIC);

    public static final DeferredItem<Item> MAGE_HAT = ITEMS.registerItem("mage_hat",
            properties -> new Item(properties.rarity(Rarity.UNCOMMON)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.HELMET)));
    public static final DeferredItem<Item> MAGE_ROBE = ITEMS.registerItem("mage_robe",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.CHESTPLATE)));
    public static final DeferredItem<Item> MAGE_ROBE_HEM = ITEMS.registerItem("mage_robe_hem",
            properties -> new Item(properties.rarity(Rarity.RARE)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.LEGGINGS)));
    public static final DeferredItem<Item> MAGE_BOOTS = ITEMS.registerItem("mage_boots",
            properties -> new Item(properties.rarity(Rarity.UNCOMMON)
                    .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.BOOTS)));

    public static final DeferredItem<BeginnerGrimoireItem> BEGINNER_GRIMOIRE = ITEMS.registerItem(''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    '''            for (DeferredItem<ArcaneStaffItem> item : all()) event.accept(item.get());
            event.accept(BEGINNER_GRIMOIRE.get());''',
    '''            for (DeferredItem<ArcaneStaffItem> item : all()) event.accept(item.get());
            event.accept(MAGE_HAT.get());
            event.accept(MAGE_ROBE.get());
            event.accept(MAGE_BOOTS.get());
            event.accept(BEGINNER_GRIMOIRE.get());''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java",
    '''    public static StaffProfile profile(String id) {
        return PROFILE_BY_ID.getOrDefault(id, StaffProfile.NONE);
    }
''',
    '''    public static StaffProfile profile(String id) {
        return PROFILE_BY_ID.getOrDefault(id, StaffProfile.NONE);
    }

    public static DeferredItem<? extends Item> gearItem(String id) {
        return switch (id) {
            case "mage_hat" -> MAGE_HAT;
            case "mage_robe" -> MAGE_ROBE;
            case "mage_boots" -> MAGE_BOOTS;
            default -> MAGE_HAT;
        };
    }
''',
)

# Gear is sold through the same Arcana economy.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/AcademyOfferCatalog.java",
    "public enum Kind { PRIMER, SPELLBOOK, STAFF }",
    "public enum Kind { PRIMER, SPELLBOOK, STAFF, GEAR }",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/AcademyOfferCatalog.java",
    '''        cached = List.copyOf(result);''',
    '''        result.add(new Offer("gear:mage_hat", "비전 모자", "MP·회복·마력 효율에 특화된 모자.",
                2, 1800L, Kind.GEAR, "mage_hat"));
        result.add(new Offer("gear:mage_boots", "유랑 마도화", "이동·도약·사거리·재사용 속도에 특화된 신발.",
                2, 2400L, Kind.GEAR, "mage_boots"));
        result.add(new Offer("gear:mage_robe", "중층 마도 로브", "몸과 바지 슬롯을 함께 사용하며 생존력과 주문 위력을 높입니다.",
                3, 7200L, Kind.GEAR, "mage_robe"));
        cached = List.copyOf(result);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java",
    "import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;",
    "import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;\nimport kr.moonseungjun.arcanecircle.magic.CombatGrowthService;",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java",
    '''            case STAFF -> new ItemStack(ModItems.staffItem(offer.targetId()).get());
        };''',
    '''            case STAFF -> new ItemStack(ModItems.staffItem(offer.targetId()).get());
            case GEAR -> new ItemStack(ModItems.gearItem(offer.targetId()).get());
        };''',
)
replace_all(
    "src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEconomyService.java",
    "player.sendSystemMessage(Component.literal(",
    "ArcaneNoticeService.push(player, Component.literal(",
    minimum=5,
)

# Server-authored notices are visible over the open grimoire, and locked fusions explain why.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''            boolean ready = formula.ingredients().stream().allMatch(id -> ArcaneClientState.cooldownRemainingTicks(id) <= 0);
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF25344B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), ready ? accent : 0xFFB75B68);''',
    '''            int playerCircle = ArcaneClientState.integer("circle", 1);
            boolean circleReady = result.circle() <= playerCircle;
            boolean learned = formula.ingredients().stream().allMatch(id -> ArcaneClientState.known().contains(id));
            boolean cooldownReady = formula.ingredients().stream()
                    .allMatch(id -> ArcaneClientState.cooldownRemainingTicks(id) <= 0);
            boolean ready = circleReady && learned && cooldownReady;
            g.fill(r.x(), r.y(), r.right(), r.bottom(), inside(mouseX, mouseY, r) ? 0xFF25344B : 0xFF111927);
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), ready ? accent : 0xFFB75B68);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''            String readiness = ready ? "재료 주문 쿨타임 준비 완료" : "재료 주문 쿨타임 대기 중";
            g.text(font, Component.literal(fit(readiness, r.w() - 10)), r.x() + 6, r.y() + 43,
                    ready ? 0xFF76D5A5 : 0xFFE07882);''',
    '''            String readiness = !circleReady
                    ? "융합 불가 · 필요 " + result.circle() + "써클 / 현재 " + playerCircle + "써클"
                    : !learned ? "융합 불가 · 재료 주문 미습득"
                    : !cooldownReady ? "융합 불가 · 재료 주문 쿨타임 대기 중"
                    : "융합 가능 · 재료 주문 준비 완료";
            g.text(font, Component.literal(fit(readiness, r.w() - 10)), r.x() + 6, r.y() + 43,
                    ready ? 0xFF76D5A5 : 0xFFE07882);''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        if (notice.isBlank() || System.currentTimeMillis() > noticeUntil) return;
        int w = Math.min(l.panelW() - 30, Math.max(110, font.width(notice) + 20)); int x = l.cx() - w / 2;
        g.fill(x, l.top() + 48, x + w, l.top() + 67, 0xF0181324); g.fill(x, l.top() + 48, x + w, l.top() + 50, 0xFFFFD36B);
        g.centeredText(font, Component.literal(notice), l.cx(), l.top() + 54, 0xFFFFE8B4);
    }''',
    '''    private void drawNotice(GuiGraphicsExtractor g, Layout l) {
        String serverNotice = ArcaneClientState.noticeText();
        String shown = !serverNotice.isBlank() ? serverNotice
                : (!notice.isBlank() && System.currentTimeMillis() <= noticeUntil ? notice : "");
        if (shown.isBlank()) return;
        int w = Math.min(l.panelW() - 30, Math.max(110, font.width(shown) + 20)); int x = l.cx() - w / 2;
        g.fill(x, l.top() + 48, x + w, l.top() + 67, 0xF0181324); g.fill(x, l.top() + 48, x + w, l.top() + 50, 0xFFFFD36B);
        g.centeredText(font, Component.literal(fit(shown, w - 12)), l.cx(), l.top() + 54, 0xFFFFE8B4);
    }''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''        infoPanel(g, c.x() + 268, c.y() + 28, Math.max(150, c.w() - 268), "장비", List.of(
                ArcaneClientState.text("staff", "맨손"),
                "소속 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).displayName(),
                "저단계 주문 자동 단축", "건축 허용"));''',
    '''        infoPanel(g, c.x() + 268, c.y() + 28, Math.max(150, c.w() - 268), "장비 / 소속", List.of(
                ArcaneClientState.text("staff", "맨손"),
                ArcaneClientState.text("gear_hat", "모자 없음"),
                ArcaneClientState.text("gear_robe", "로브 없음"),
                ArcaneClientState.text("gear_boots", "마도화 없음"),
                "소속 " + MagicTradition.parse(ArcaneClientState.text("tradition", "UNBOUND")).displayName()));''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java",
    '''        g.fill(x, y, x + w, y + 82, 0xFF101827); g.fill(x, y, x + w, y + 2, 0xFF745797);''',
    '''        int panelHeight = Math.max(82, 34 + lines.size() * 13);
        g.fill(x, y, x + w, y + panelHeight, 0xFF101827); g.fill(x, y, x + w, y + 2, 0xFF745797);''',
)

replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java",
    "import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;",
    "import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;\nimport kr.moonseungjun.arcanecircle.magic.MageGearService;",
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java",
    '''        StaffProfile staff = stats.staff();
        ArcaneQuestData.QuestStatus quest =''',
    '''        StaffProfile staff = stats.staff();
        MageGearService.GearStats gear = MageGearService.stats(player);
        ArcaneQuestData.QuestStatus quest =''',
)
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java",
    '''                + ";staff_regen=" + permille(staff.regenMultiplier())
                + ";" + "marks="''',
    '''                + ";staff_regen=" + permille(staff.regenMultiplier())
                + ";gear_hat=" + MageGearService.hatName(player)
                + ";gear_robe=" + MageGearService.robeName(player)
                + ";gear_boots=" + MageGearService.bootsName(player)
                + ";gear_mana=" + gear.maxManaBonus()
                + ";gear_regen=" + permille(gear.regenMultiplier())
                + ";" + "marks="''',
)

# A 1st-circle safety spell should be brief, not a long-duration flight substitute.
replace_once(
    "src/main/java/kr/moonseungjun/arcanecircle/magic/ExpandedSpellEffects.java",
    'case "feather_fall" -> featherFall(player, 720);',
    'case "feather_fall" -> featherFall(player, 120);',
)

# Remove the obsolete X key label and add gear names.
lang_path = RES / "assets/arcanecircle/lang/ko_kr.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.pop("key.arcanecircle.fusion_modifier", None)
lang.update({
    "item.arcanecircle.mage_hat": "비전 모자",
    "item.arcanecircle.mage_robe": "중층 마도 로브",
    "item.arcanecircle.mage_robe_hem": "중층 마도 로브 자락",
    "item.arcanecircle.mage_boots": "유랑 마도화",
})
lang_path.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

for item, texture in {
    "mage_hat": "minecraft:item/leather_helmet",
    "mage_robe": "minecraft:item/leather_chestplate",
    "mage_robe_hem": "minecraft:item/leather_leggings",
    "mage_boots": "minecraft:item/leather_boots",
}.items():
    write(f"src/main/resources/assets/arcanecircle/models/item/{item}.json",
          json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": texture}}, separators=(",", ":")) + "\n")
    write(f"src/main/resources/assets/arcanecircle/items/{item}.json",
          json.dumps({"model": {"type": "minecraft:model", "model": f"arcanecircle:item/{item}"}}, separators=(",", ":")) + "\n")

# Record design guarantees without changing existing IDs or save codecs.
index_path = RES / "data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index.update({
    "fusion_input": "number_key_chord",
    "fusion_power_floor": "125_percent_of_strongest_source_or_145_percent_for_triple",
    "mage_gear": ["hat", "two_slot_robe", "boots"],
    "feather_fall_ticks": 120,
})
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

print("Arcane Circle v0.12.1-alpha.6 fusion, notices, gear and duration migration applied")
