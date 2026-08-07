from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")

def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")

def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one literal match, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))

def regex_once(path: Path, pattern: str, repl: str, flags: int = re.S) -> None:
    text = read(path)
    updated, count = re.subn(pattern, repl, text, count=1, flags=flags)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one regex match, found {count}: {pattern[:160]!r}")
    write(path, updated)

replace_once(ROOT / "gradle.properties", "mod_version=0.18.1-alpha.1", "mod_version=0.18.2-alpha.1")

equipment = SRC / "VillageEquipmentShop.java"
regex_once(
    equipment,
    r'''    public static float outgoingMultiplier\(ServerPlayer player, boolean projectile\) \{.*?\n    \}\n\n    public static float incomingMultiplier''',
    '''    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        float named = projectile ? bonusFor(player.getMainHandItem(), true) : bonusFor(player.getMainHandItem(), false);
        if (projectile) named = Math.max(named, bonusFor(player.getOffhandItem(), true));
        float rarity = projectile
                ? Math.max(VillageEquipmentRaritySystem.projectileMultiplier(player.getMainHandItem()),
                VillageEquipmentRaritySystem.projectileMultiplier(player.getOffhandItem()))
                : VillageEquipmentRaritySystem.meleeMultiplier(player.getMainHandItem());
        return named * rarity;
    }

    public static float incomingMultiplier'''
)
regex_once(
    equipment,
    r'''    public static float incomingMultiplier\(ServerPlayer player\) \{.*?\n    \}\n\n    public static float roleSkillMultiplier''',
    '''    public static float incomingMultiplier(ServerPlayer player) {
        float reduction = equippedOffers(player).stream()
                .map(Offer::damageReduction)
                .reduce(0.0f, Float::sum);
        float rarityMultiplier = VillageEquipmentRaritySystem.incomingMultiplier(player);
        return Math.max(0.52f, (1.0f - Math.min(0.42f, reduction)) * rarityMultiplier);
    }

    public static float roleSkillMultiplier'''
)
regex_once(
    equipment,
    r'''    public static float roleSkillMultiplier\(ServerPlayer player\) \{.*?\n    \}\n\n    public static int cooldownReductionSeconds''',
    '''    public static float roleSkillMultiplier(ServerPlayer player) {
        float named = 1.0f;
        for (Offer offer : equippedOffers(player)) named *= offer.skillMultiplier();
        return named * VillageEquipmentRaritySystem.skillMultiplier(player);
    }

    public static int cooldownReductionSeconds'''
)

identity_java = dedent(r'''
package kr.moonseungjun.villageguardians;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class VillageEquipmentIdentity {
    private static final String KEY_MARKER = "villageguardians_equipment";
    private static final String KEY_RARITY = "villageguardians_rarity";
    private static final String KEY_ENHANCEMENT = "villageguardians_enhancement";
    private static final String KEY_OFFER = "villageguardians_offer";

    private VillageEquipmentIdentity() {}

    public static void stampRarity(ItemStack stack, String rarity, int enhancement) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = tagCopy(stack);
        tag.putBoolean(KEY_MARKER, true);
        tag.putString(KEY_RARITY, rarity == null ? "" : rarity);
        tag.putInt(KEY_ENHANCEMENT, Math.max(0, enhancement));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void stampOffer(ItemStack stack, String offerId) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = tagCopy(stack);
        tag.putBoolean(KEY_MARKER, true);
        tag.putString(KEY_OFFER, offerId == null ? "" : offerId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean stamped(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(KEY_MARKER, false);
    }

    public static String rarity(ItemStack stack) {
        if (!stamped(stack)) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr(KEY_RARITY, "");
    }

    public static int enhancement(ItemStack stack) {
        if (!stamped(stack)) return -1;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? -1 : Math.max(0, data.copyTag().getIntOr(KEY_ENHANCEMENT, 0));
    }

    public static String offer(ItemStack stack) {
        if (!stamped(stack)) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr(KEY_OFFER, "");
    }

    public static boolean canReadLegacyName(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stamped(stack)) return false;
        Integer repairCost = stack.get(DataComponents.REPAIR_COST);
        return repairCost == null || repairCost <= 0;
    }

    private static CompoundTag tagCopy(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        return existing == null ? new CompoundTag() : existing.copyTag();
    }
}
''').lstrip()
write(SRC / "VillageEquipmentIdentity.java", identity_java)

rarity = SRC / "VillageEquipmentRaritySystem.java"
replace_once(
    rarity,
    '''        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("[" + rarity.displayName() + "] " + name + suffix)
                        .withStyle(rarity.formatting()));
''',
    '''        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal("[" + rarity.displayName() + "] " + name + suffix)
                        .withStyle(rarity.formatting()));
        VillageEquipmentIdentity.stampRarity(stack, rarity.name(), enhancement);
'''
)
regex_once(
    rarity,
    r'''    public static Rarity rarityOf\(ItemStack stack\) \{.*?\n    \}\n\n    public static int enhancementLevel''',
    '''    public static Rarity rarityOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String stamped = VillageEquipmentIdentity.rarity(stack);
        if (!stamped.isBlank()) {
            try { return Rarity.valueOf(stamped); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        if (!VillageEquipmentIdentity.canReadLegacyName(stack)) return null;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return null;
        String plain = ChatFormatting.stripFormatting(name.getString());
        for (Rarity rarity : Rarity.values()) {
            if (plain.startsWith("[" + rarity.displayName() + "] ")) return rarity;
        }
        return null;
    }

    public static int enhancementLevel'''
)
regex_once(
    rarity,
    r'''    public static int enhancementLevel\(ItemStack stack\) \{.*?\n    \}\n\n    public static String baseDisplayName''',
    '''    public static int enhancementLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int stamped = VillageEquipmentIdentity.enhancement(stack);
        if (stamped >= 0) return stamped;
        if (!VillageEquipmentIdentity.canReadLegacyName(stack)) return 0;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) return 0;
        String plain = ChatFormatting.stripFormatting(name.getString());
        int marker = plain.lastIndexOf(" +");
        if (marker < 0 || marker + 2 >= plain.length()) return 0;
        String raw = plain.substring(marker + 2);
        for (int index = 0; index < raw.length(); index++) {
            if (!Character.isDigit(raw.charAt(index))) return 0;
        }
        try { return Math.max(0, Integer.parseInt(raw)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    public static String baseDisplayName'''
)
replace_once(
    equipment,
    '''        public ItemStack createStack() {
            return VillageEquipmentRaritySystem.createNamed(item, rarity(), displayName);
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == item
                    && displayName.equals(VillageEquipmentRaritySystem.baseDisplayName(stack));
        }
''',
    '''        public ItemStack createStack() {
            ItemStack stack = VillageEquipmentRaritySystem.createNamed(item, rarity(), displayName);
            VillageEquipmentIdentity.stampOffer(stack, id);
            return stack;
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty() || stack.getItem() != item) return false;
            String stampedOffer = VillageEquipmentIdentity.offer(stack);
            if (!stampedOffer.isBlank()) return id.equals(stampedOffer);
            return VillageEquipmentIdentity.canReadLegacyName(stack)
                    && displayName.equals(VillageEquipmentRaritySystem.baseDisplayName(stack));
        }
'''
)

loot = SRC / "VillageRaidLootSystem.java"
loot_replacements = {
'put(VillageEnemyArchetypeSystem.Archetype.GRUNT, Items.BONE, "금 간 전열병 송곳니", ChatFormatting.GRAY);':
'put(VillageEnemyArchetypeSystem.Archetype.GRUNT, Items.BONE, "금 간 전열병 송곳니", ChatFormatting.GRAY, 3);',
'put(VillageEnemyArchetypeSystem.Archetype.RUSHER, Items.FLINT, "척후병의 닳은 단검 조각", ChatFormatting.GRAY);':
'put(VillageEnemyArchetypeSystem.Archetype.RUSHER, Items.FLINT, "척후병의 닳은 단검 조각", ChatFormatting.GRAY, 4);',
'put(VillageEnemyArchetypeSystem.Archetype.BULWARK, Items.IRON_NUGGET, "찌그러진 방패 고리", ChatFormatting.WHITE);':
'put(VillageEnemyArchetypeSystem.Archetype.BULWARK, Items.IRON_NUGGET, "찌그러진 방패 고리", ChatFormatting.WHITE, 6);',
'put(VillageEnemyArchetypeSystem.Archetype.SAPPER, Items.GUNPOWDER, "폭파병 화약 주머니", ChatFormatting.GOLD);':
'put(VillageEnemyArchetypeSystem.Archetype.SAPPER, Items.GUNPOWDER, "폭파병 화약 주머니", ChatFormatting.GOLD, 7);',
'put(VillageEnemyArchetypeSystem.Archetype.MARKSMAN, Items.FEATHER, "사수의 찢긴 깃", ChatFormatting.WHITE);':
'put(VillageEnemyArchetypeSystem.Archetype.MARKSMAN, Items.FEATHER, "사수의 찢긴 깃", ChatFormatting.WHITE, 6);',
'put(VillageEnemyArchetypeSystem.Archetype.SHIELDBREAKER, Items.IRON_NUGGET, "파쇄병 도끼날 파편", ChatFormatting.DARK_GRAY);':
'put(VillageEnemyArchetypeSystem.Archetype.SHIELDBREAKER, Items.IRON_NUGGET, "파쇄병 도끼날 파편", ChatFormatting.DARK_GRAY, 8);',
'put(VillageEnemyArchetypeSystem.Archetype.HEXER, Items.SPIDER_EYE, "응고된 저주 마력낭", ChatFormatting.DARK_PURPLE);':
'put(VillageEnemyArchetypeSystem.Archetype.HEXER, Items.SPIDER_EYE, "응고된 저주 마력낭", ChatFormatting.DARK_PURPLE, 9);',
'put(VillageEnemyArchetypeSystem.Archetype.WAR_CHANTER, Items.GOAT_HORN, "전쟁 고수의 갈라진 뿔", ChatFormatting.GOLD);':
'put(VillageEnemyArchetypeSystem.Archetype.WAR_CHANTER, Items.GOAT_HORN, "전쟁 고수의 갈라진 뿔", ChatFormatting.GOLD, 10);',
'put(VillageEnemyArchetypeSystem.Archetype.NECROMANCER, Items.COAL, "사령술사의 검은 뼛가루", ChatFormatting.DARK_PURPLE);':
'put(VillageEnemyArchetypeSystem.Archetype.NECROMANCER, Items.COAL, "사령술사의 검은 뼛가루", ChatFormatting.DARK_PURPLE, 12);',
'put(VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER, Items.AMETHYST_SHARD, "탑 사냥꾼의 조준 렌즈", ChatFormatting.AQUA);':
'put(VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER, Items.AMETHYST_SHARD, "탑 사냥꾼의 조준 렌즈", ChatFormatting.AQUA, 14);',
'put(VillageEnemyArchetypeSystem.Archetype.SIEGE_BEAST, Items.HEAVY_CORE, "공성 야수의 파쇄핵", ChatFormatting.LIGHT_PURPLE);':
'put(VillageEnemyArchetypeSystem.Archetype.SIEGE_BEAST, Items.HEAVY_CORE, "공성 야수의 파쇄핵", ChatFormatting.LIGHT_PURPLE, 28);',
'put(VillageEnemyArchetypeSystem.Archetype.IRON_WARLORD, Items.NETHERITE_SCRAP, "철의 전쟁군주 휘장", ChatFormatting.GOLD);':
'put(VillageEnemyArchetypeSystem.Archetype.IRON_WARLORD, Items.NETHERITE_SCRAP, "철의 전쟁군주 휘장", ChatFormatting.GOLD, 34);',
'put(VillageEnemyArchetypeSystem.Archetype.PLAGUE_ARCHON, Items.ENDER_PEARL, "역병 대주교의 뒤틀린 심장", ChatFormatting.DARK_PURPLE);':
'put(VillageEnemyArchetypeSystem.Archetype.PLAGUE_ARCHON, Items.ENDER_PEARL, "역병 대주교의 뒤틀린 심장", ChatFormatting.DARK_PURPLE, 38);',
'put(VillageEnemyArchetypeSystem.Archetype.DREAD_KNIGHT, Items.ECHO_SHARD, "공포 기사의 암흑 갑편", ChatFormatting.DARK_AQUA);':
'put(VillageEnemyArchetypeSystem.Archetype.DREAD_KNIGHT, Items.ECHO_SHARD, "공포 기사의 암흑 갑편", ChatFormatting.DARK_AQUA, 42);',
}
for old, new in loot_replacements.items():
    replace_once(loot, old, new)

replace_once(
    loot,
    '''    private static void put(VillageEnemyArchetypeSystem.Archetype type, Item item,
                            String name, ChatFormatting color) {
        SALE_LOOT.put(type, new SaleLoot(item, name, color));
    }
''',
    '''    private static void put(VillageEnemyArchetypeSystem.Archetype type, Item item,
                            String name, ChatFormatting color, int value) {
        SALE_LOOT.put(type, new SaleLoot(item, name, color, Math.max(1, value)));
    }

    public static int saleValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom == null) return 0;
        String plain = ChatFormatting.stripFormatting(custom.getString());
        for (SaleLoot loot : SALE_LOOT.values()) {
            if (("[판매용] " + loot.name()).equals(plain)) return loot.value();
        }
        return 0;
    }
'''
)
replace_once(
    loot,
    '''    private record SaleLoot(Item item, String name, ChatFormatting color) {}
''',
    '''    private record SaleLoot(Item item, String name, ChatFormatting color, int value) {}
'''
)

trading = SRC / "VillageTradingSystem.java"
replace_once(
    trading,
    '''    private static boolean isSaleOnlyLoot(ItemStack stack) {
        String name = plainName(stack);
        return name.startsWith("[판매용] ")
                || (stack.get(DataComponents.CUSTOM_NAME) == null && LEGACY_PRICES.containsKey(stack.getItem()));
    }
''',
    '''    private static boolean isSaleOnlyLoot(ItemStack stack) {
        if (VillageRaidLootSystem.saleValue(stack) > 0) return true;
        String name = plainName(stack);
        return NAMED_PRICES.containsKey(name)
                || (stack.get(DataComponents.CUSTOM_NAME) == null && LEGACY_PRICES.containsKey(stack.getItem()));
    }
'''
)
replace_once(
    trading,
    '''        String name = plainName(stack);
        Integer named = NAMED_PRICES.get(name);
''',
    '''        int raidValue = VillageRaidLootSystem.saleValue(stack);
        if (raidValue > 0) return raidValue;
        String name = plainName(stack);
        Integer named = NAMED_PRICES.get(name);
'''
)

relic = SRC / "VillageRelicSystem.java"
replace_once(relic, '    private static final String SEP = "\\u001F";\n',
             '    private static final String SEP = "\\u001F";\n    private static final String OFFER_SEP = ";";\n')
regex_once(
    relic,
    r'''    public static synchronized void offerToParty\(MinecraftServer server\) \{.*?\n    \}\n\n    public static synchronized void openChoice''',
    '''    public static synchronized void offerToParty(MinecraftServer server) {
        int day = VillageCouncilState.currentDay();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<Relic> choices = choicesFor(player, day);
            if (choices.isEmpty()) continue;
            String encoded = choices.stream().map(Relic::id)
                    .reduce((first, second) -> first + "," + second).orElse("");
            String previous = PENDING.getOrDefault(player.getUUID(), "");
            PENDING.put(player.getUUID(), previous.isBlank() ? encoded : previous + OFFER_SEP + encoded);
            persist();
            if (previous.isBlank()) openChoice(player);
        }
    }

    public static synchronized void openChoice'''
)
regex_once(
    relic,
    r'''    public static synchronized String select\(ServerPlayer player, String id\) \{.*?\n    \}\n\n    public static synchronized boolean has''',
    '''    public static synchronized String select(ServerPlayer player, String id) {
        Relic relic = Relic.fromId(id);
        if (relic == null) return "알 수 없는 유물입니다.";
        List<Relic> choices = pendingChoices(player);
        if (!choices.contains(relic)) return "현재 제시된 유물이 아닙니다.";
        int mask = OWNED.getOrDefault(player.getUUID(), 0);
        OWNED.put(player.getUUID(), sanitizeMask(mask | relic.bit()));
        consumePendingOffer(player.getUUID());
        persist();
        return relic.displayName() + " 획득 · " + relic.description();
    }

    public static synchronized boolean hasPendingChoice(ServerPlayer player) {
        return player != null && !pendingChoices(player).isEmpty();
    }

    public static synchronized boolean has'''
)
regex_once(
    relic,
    r'''    private static List<Relic> choicesFor\(ServerPlayer player, int day\) \{.*?\n    \}\n\n    private static List<Relic> pendingChoices\(ServerPlayer player\) \{.*?\n    \}\n''',
    '''    private static List<Relic> choicesFor(ServerPlayer player, int day) {
        List<Relic> available = new ArrayList<>();
        int mask = OWNED.getOrDefault(player.getUUID(), 0);
        java.util.Set<Relic> reserved = pendingRelics(player.getUUID());
        for (Relic relic : Relic.values()) {
            if ((mask & relic.bit()) == 0 && !reserved.contains(relic)) available.add(relic);
        }
        if (available.isEmpty()) return List.of();
        List<Relic> result = new ArrayList<>();
        int seed = player.getUUID().hashCode() * 31 + day * 17
                + Integer.bitCount(mask) * 13 + reserved.size() * 19;
        while (!available.isEmpty() && result.size() < 3) {
            int index = Math.floorMod(seed + result.size() * 37, available.size());
            result.add(available.remove(index));
        }
        return result;
    }

    private static List<Relic> pendingChoices(ServerPlayer player) {
        if (player == null) return List.of();
        String raw = PENDING.getOrDefault(player.getUUID(), "");
        if (raw.isBlank()) return List.of();
        String first = raw.split(OFFER_SEP, 2)[0];
        List<Relic> result = new ArrayList<>();
        for (String id : first.split(",")) {
            Relic relic = Relic.fromId(id);
            if (relic != null) result.add(relic);
        }
        return result;
    }

    private static java.util.Set<Relic> pendingRelics(UUID playerId) {
        java.util.Set<Relic> result = java.util.EnumSet.noneOf(Relic.class);
        String raw = PENDING.getOrDefault(playerId, "");
        if (raw.isBlank()) return result;
        for (String offer : raw.split(OFFER_SEP)) {
            for (String id : offer.split(",")) {
                Relic relic = Relic.fromId(id);
                if (relic != null) result.add(relic);
            }
        }
        return result;
    }

    private static void consumePendingOffer(UUID playerId) {
        String raw = PENDING.getOrDefault(playerId, "");
        if (raw.isBlank()) {
            PENDING.remove(playerId);
            return;
        }
        int separator = raw.indexOf(OFFER_SEP);
        if (separator < 0 || separator + OFFER_SEP.length() >= raw.length()) {
            PENDING.remove(playerId);
        } else {
            PENDING.put(playerId, raw.substring(separator + OFFER_SEP.length()));
        }
    }
'''
)

ui = SRC / "VillageUiController.java"
replace_once(
    ui,
    '''        if (action.startsWith("relic_select:")) {
            String result = VillageRelicSystem.select(player, action.substring(13));
            player.sendSystemMessage(Component.literal("§d" + result));
            VillageRelicSystem.openCollection(player);
            return true;
        }
''',
    '''        if (action.startsWith("relic_select:")) {
            String result = VillageRelicSystem.select(player, action.substring(13));
            player.sendSystemMessage(Component.literal("§d" + result));
            if (VillageRelicSystem.hasPendingChoice(player)) VillageRelicSystem.openChoice(player);
            else VillageRelicSystem.openCollection(player);
            return true;
        }
'''
)

respawn = SRC / "VillageRespawnSystem.java"
replace_once(
    respawn,
    'import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n',
    'import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;\n'
)
regex_once(
    respawn,
    r'''    public static boolean handleIncomingDamage\(LivingIncomingDamageEvent event\) \{.*?\n    \}\n\n    public static void tick''',
    '''    public static boolean handleFinalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        if (isDowned(player)) {
            event.setNewDamage(0.0f);
            return true;
        }
        float effectiveHealth = player.getHealth() + Math.max(0.0f, player.getAbsorptionAmount());
        if (event.getNewDamage() < effectiveHealth) return false;

        MinecraftServer server = player.level().getServer();
        if (server == null) return false;

        event.setNewDamage(0.0f);
        player.setAbsorptionAmount(0.0f);
        player.setHealth(1.0f);
        player.setRemainingFireTicks(0);
        player.setDeltaMovement(Vec3.ZERO);
        player.setGameMode(GameType.SPECTATOR);
        int delay = VillageProgressionSystem.respawnDelayTicks();
        RESPAWN_AT.put(player.getUUID(), server.overworld().getGameTime() + delay);
        player.sendSystemMessage(Component.literal(
                "§c[전투 불능] §f" + (delay / 20)
                        + "초 후 마을 광장에서 부활합니다. 적은 시설 공격을 계속합니다."));
        return true;
    }

    public static void tick'''
)

guardians = SRC / "VillageGuardians.java"
replace_once(
    guardians,
    'import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\n',
    'import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;\nimport net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\n'
)
replace_once(
    guardians,
    '''        VillageRpgSystem.handleIncomingDamage(event);
        VillageRoleAbilitySystem.handleIncomingDamage(event);
        VillageRespawnSystem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    public void onLivingDrops''',
    '''        VillageRpgSystem.handleIncomingDamage(event);
        VillageRoleAbilitySystem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    public void onFinalDamage(LivingDamageEvent.Pre event) {
        VillageRespawnSystem.handleFinalDamage(event);
    }

    @SubscribeEvent
    public void onLivingDrops'''
)

raid = SRC / "VillageRaidSystem.java"
replace_once(
    raid,
    'import net.minecraft.world.phys.Vec3;\n',
    'import net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.Vec3;\n'
)
replace_once(
    raid,
    '''    public static void resetTransientState(MinecraftServer server) {
        clearState();
        ensureRaidTeam(server);
''',
    '''    public static void resetTransientState(MinecraftServer server) {
        discardTaggedRaidEnemies(server);
        clearState();
        ensureRaidTeam(server);
'''
)
replace_once(
    raid,
    '''            Entity entity = server.overworld().getEntity(uuid);
            if (entity != null && !entity.isAlive()) {
                releaseEnemy(server, uuid, entity);
                iterator.remove();
            }
''',
    '''            Entity entity = server.overworld().getEntity(uuid);
            if (entity == null) {
                releaseEnemy(server, uuid, null);
                iterator.remove();
            } else if (!entity.isAlive()) {
                releaseEnemy(server, uuid, entity);
                iterator.remove();
            }
'''
)
replace_once(
    raid,
    '''    private static void discardEnemies(MinecraftServer server) {
''',
    '''    public static boolean shouldDiscardStaleRaidEnemy(Mob mob) {
        return mob != null
                && mob.getTags().contains(RAID_ENEMY_TAG)
                && !ACTIVE_ENEMIES.contains(mob.getUUID())
                && !VillageWorldSystem.isAllowedGameMob(mob);
    }

    private static void discardTaggedRaidEnemies(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 160.0;
        AABB area = new AABB(center).inflate(radius, 128.0, radius);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area,
                entity -> entity.getTags().contains(RAID_ENEMY_TAG))) {
            releaseEnemy(server, mob.getUUID(), mob);
            mob.discard();
        }
    }

    private static void discardEnemies(MinecraftServer server) {
'''
)
replace_once(
    guardians,
    '''        if (VillageMercenarySystem.recognize(mob)) return;
        if (VillageSkillTestSystem.recognize(mob)) return;
        if (VillageDefenseSystem.recognizeDefenseMob(mob)) return;
        if (VillageWorldSystem.isAllowedGameMob(mob)) return;
        if (!mob.isPersistenceRequired()) event.setCanceled(true);
''',
    '''        if (VillageMercenarySystem.recognize(mob)) return;
        if (VillageSkillTestSystem.recognize(mob)) return;
        if (VillageDefenseSystem.recognizeDefenseMob(mob)) return;
        if (VillageRaidSystem.shouldDiscardStaleRaidEnemy(mob)) {
            event.setCanceled(true);
            mob.discard();
            return;
        }
        if (VillageWorldSystem.isAllowedGameMob(mob)) return;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 96.0;
        if (mob.blockPosition().distSqr(center) > radius * radius) return;
        if (!mob.isPersistenceRequired()) event.setCanceled(true);
'''
)
replace_once(
    guardians,
    'import net.minecraft.server.level.ServerLevel;\n',
    'import net.minecraft.core.BlockPos;\nimport net.minecraft.server.level.ServerLevel;\n'
)

purge = SRC / "VillageGlobalMobPurgeSystem.java"
replace_once(
    purge,
    '        AABB loadedBattleWorld = new AABB(center).inflate(2048, 256, 2048);\n',
    '        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 96.0;\n        AABB loadedBattleWorld = new AABB(center).inflate(radius, 128, radius);\n'
)

effect_entity = SRC / "VillageSkillEffectEntity.java"
replace_once(
    effect_entity,
    '''        Entity owner = ownerEntity();
        if (followsOwner() && owner != null && owner.isAlive()) {
''',
    '''        Entity owner = ownerEntity();
        if ("arcanist_tornado".equals(kind()) && owner != null && owner.isAlive()) {
            Vec3 look = owner.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
            if (horizontal.lengthSqr() > 1.0E-6) setDirection(horizontal.normalize());
        }
        if (followsOwner() && owner != null && owner.isAlive()) {
'''
)
replace_once(
    effect_entity,
    '''                    "vanguard_slam_charge", "ranger_rapid", "ranger_focus",
                    "ranger_energy_charge", "luminar_heal_cast", "luminar_cleanse_cast",
                    "luminar_healing_field", "luminar_miracle_cast",
''',
    '''                    "vanguard_slam_charge", "ranger_rapid", "ranger_focus",
                    "ranger_energy_charge", "luminar_heal_cast", "luminar_cleanse_cast",
                    "luminar_miracle_cast",
'''
)

abilities = SRC / "VillageRoleAbilitySystem.java"
replace_once(
    abilities,
    '''                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(0.24));
''',
    '''                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(1.20));
'''
)

role_skills = SRC / "VillageRoleSkillSystem.java"
replace_once(
    role_skills,
    '''    private static int effectiveCooldownSeconds(
            ServerPlayer player, VillageRole role, ActiveSkill skill) {
        return Math.max(7,
                skill.baseCooldownSeconds()
''',
    '''    private static int effectiveCooldownSeconds(
            ServerPlayer player, VillageRole role, ActiveSkill skill) {
        int minimum = Math.max(2, Math.round(skill.baseCooldownSeconds() * 0.20f));
        return Math.max(minimum,
                skill.baseCooldownSeconds()
'''
)

snapshot_java = dedent(r'''
package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageMercenarySnapshotData extends SavedData {
    private static final Codec<VillageMercenarySnapshotData> CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .xmap(VillageMercenarySnapshotData::new, VillageMercenarySnapshotData::entries);

    public static final SavedDataType<VillageMercenarySnapshotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_mercenary_night_snapshot"),
            level -> new VillageMercenarySnapshotData(),
            level -> CODEC);

    private Map<String, String> entries;

    public VillageMercenarySnapshotData() {
        this(Map.of());
    }

    private VillageMercenarySnapshotData(Map<String, String> entries) {
        this.entries = new LinkedHashMap<>(entries);
    }

    public Map<String, String> entries() {
        return new LinkedHashMap<>(entries);
    }

    public void replace(Map<String, String> updated) {
        entries = new LinkedHashMap<>(updated);
        setDirty();
    }
}
''').lstrip()
write(SRC / "VillageMercenarySnapshotData.java", snapshot_java)

merc = SRC / "VillageMercenarySystem.java"
replace_once(
    merc,
    '''    private static VillageMercenaryData savedData;
    private static final List<MercenarySnapshot> NIGHT_SNAPSHOT = new ArrayList<>();
''',
    '''    private static VillageMercenaryData savedData;
    private static VillageMercenarySnapshotData snapshotData;
    private static final List<MercenarySnapshot> NIGHT_SNAPSHOT = new ArrayList<>();
'''
)
replace_once(
    merc,
    '''        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenaryData.TYPE);
        CLASSES.clear();
''',
    '''        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenaryData.TYPE);
        snapshotData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenarySnapshotData.TYPE);
        CLASSES.clear();
'''
)
replace_once(
    merc,
    '''        sanitize();
        persist();
        NIGHT_SNAPSHOT.clear();
        tickCounter = 0;
''',
    '''        sanitize();
        persist();
        loadNightSnapshot();
        tickCounter = 0;
'''
)
replace_once(
    merc,
    '''    public static synchronized void captureNightSnapshot(MinecraftServer server) {
        NIGHT_SNAPSHOT.clear();
        CLASSES.forEach((uuid, kind) -> NIGHT_SNAPSHOT.add(new MercenarySnapshot(
                kind, LEVELS.getOrDefault(uuid, 1), KILLS.getOrDefault(uuid, 0))));
    }
''',
    '''    public static synchronized void captureNightSnapshot(MinecraftServer server) {
        NIGHT_SNAPSHOT.clear();
        CLASSES.forEach((uuid, kind) -> NIGHT_SNAPSHOT.add(new MercenarySnapshot(
                kind, LEVELS.getOrDefault(uuid, 1), KILLS.getOrDefault(uuid, 0))));
        persistNightSnapshot();
    }
'''
)
replace_once(
    merc,
    '''    public static synchronized void resetForNewGame(MinecraftServer server) {
        discardCurrent(server); CLASSES.clear(); LEVELS.clear(); KILLS.clear(); NIGHT_SNAPSHOT.clear();
        tickCounter = 0; persist();
    }
''',
    '''    public static synchronized void resetForNewGame(MinecraftServer server) {
        discardCurrent(server); CLASSES.clear(); LEVELS.clear(); KILLS.clear(); NIGHT_SNAPSHOT.clear();
        tickCounter = 0; persist(); persistNightSnapshot();
    }
'''
)
replace_once(
    merc,
    '''    private record MercenarySnapshot(MercenaryClass kind, int level, int kills) {}

    private static BlockPos safeSpawn''',
    '''    private static void loadNightSnapshot() {
        NIGHT_SNAPSHOT.clear();
        if (snapshotData == null) return;
        snapshotData.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String[] parts = entry.getValue().split("\\\\|", 3);
                    if (parts.length != 3) return;
                    MercenaryClass kind = MercenaryClass.fromId(parts[0]);
                    if (kind == null) return;
                    try {
                        int level = Math.max(1, Math.min(5, Integer.parseInt(parts[1])));
                        int kills = Math.max(0, Integer.parseInt(parts[2]));
                        NIGHT_SNAPSHOT.add(new MercenarySnapshot(kind, level, kills));
                    } catch (NumberFormatException ignored) {
                    }
                });
    }

    private static void persistNightSnapshot() {
        if (snapshotData == null) return;
        Map<String, String> encoded = new LinkedHashMap<>();
        for (int index = 0; index < NIGHT_SNAPSHOT.size(); index++) {
            MercenarySnapshot snapshot = NIGHT_SNAPSHOT.get(index);
            encoded.put(String.format(Locale.ROOT, "%04d", index),
                    snapshot.kind().id() + "|" + snapshot.level() + "|" + snapshot.kills());
        }
        snapshotData.replace(encoded);
    }

    private record MercenarySnapshot(MercenaryClass kind, int level, int kills) {}

    private static BlockPos safeSpawn'''
)

skill_test = SRC / "VillageSkillTestSystem.java"
replace_once(
    skill_test,
    '''    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.getUUID());
    }

    public static String enable''',
    '''    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.getUUID());
    }

    public static boolean recoverStrandedAfterRestart(ServerPlayer player) {
        if (player == null || isEnabled(player) || !(player.level() instanceof ServerLevel)) return false;
        BlockPos arena = arenaCenter();
        if (arena == null) return false;
        BlockPos pos = player.blockPosition();
        if (Math.abs(pos.getX() - arena.getX()) > ARENA_RADIUS + 4
                || Math.abs(pos.getZ() - arena.getZ()) > ARENA_RADIUS + 4
                || Math.abs(pos.getY() - arena.getY()) > 18) return false;
        String result = VillageWorldSystem.returnToVillage(player);
        player.sendSystemMessage(Component.literal(
                "§e[시험장 복구] §f서버 재시작으로 종료된 시험 모드를 감지해 마을로 복귀했습니다. " + result));
        return true;
    }

    public static String enable'''
)
replace_once(
    guardians,
    '''            VillageWorldSystem.ensureFortifiedVillage(player);
            VillageStarterKit.grantOnLogin(player);
''',
    '''            VillageWorldSystem.ensureFortifiedVillage(player);
            VillageSkillTestSystem.recoverStrandedAfterRestart(player);
            VillageStarterKit.grantOnLogin(player);
'''
)

local_actions = SRC / "VillageLocalActionSystem.java"
replace_once(
    local_actions,
    '''            case "train" -> {
                VillageUiController.openResult(player, "병영 훈련",
                        "전투 훈련은 패시브로 변경되었습니다. 현재 모든 경험치 획득량 +"
                                + (VillageProgressionSystem.experienceMultiplierPercent() - 100) + "%",
                        "open_dashboard");
                return true;
            }
            default -> { return false; }
''',
    '''            case "train" -> {
                VillageUiController.openResult(player, "병영 훈련",
                        "전투 훈련은 패시브로 변경되었습니다. 현재 모든 경험치 획득량 +"
                                + (VillageProgressionSystem.experienceMultiplierPercent() - 100) + "%",
                        "open_dashboard");
                return true;
            }
            case "hire_mercenary" -> {
                VillageUiController.openResult(player, "용병 고용",
                        "구형 단일 용병 호출은 제거되었습니다. 병영에서 현재 4개 병과 중 하나를 선택하세요.",
                        "open_mercenary_command");
                return true;
            }
            default -> { return false; }
'''
)

council = SRC / "VillageCouncilState.java"
replace_once(
    council,
    '''    public static synchronized String vote(ServerPlayer player, boolean yes) {
''',
    '''    public static synchronized void onPlayerListChanged(MinecraftServer server) {
        if (server != null && activeProposal != null) evaluateProposal(server);
    }

    public static synchronized String vote(ServerPlayer player, boolean yes) {
'''
)
replace_once(
    guardians,
    '''    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
''',
    '''    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var server = event.getEntity().level().getServer();
        if (server != null) VillageCouncilState.onPlayerListChanged(server);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
'''
)

enemy = SRC / "VillageEnemyArchetypeSystem.java"
text = read(enemy)
text, c1 = re.subn(r'if \(globalTicks % cadence != 0\) return;', 'if (!abilityReady(mob, globalTicks, cadence)) return;', text)
text, c2 = re.subn(r'if \(globalTicks % (\d+) != 0\) return;', r'if (!abilityReady(mob, globalTicks, \1)) return;', text)
if c1 < 1 or c2 < 1:
    raise RuntimeError(f"{enemy}: expected cadence and fixed ability checks, got {c1}, {c2}")
write(enemy, text)
replace_once(
    enemy,
    '''    private static void damageAndDebuffPlayers(
''',
    '''    private static boolean abilityReady(Mob mob, int globalTicks, int cadence) {
        int safeCadence = Math.max(1, cadence);
        int phase = Math.floorMod(mob.getUUID().hashCode(), safeCadence);
        return Math.floorMod(globalTicks + phase, safeCadence) == 0;
    }

    private static void damageAndDebuffPlayers(
'''
)
replace_once(
    enemy,
    '''            player.hurtServer(level, level.damageSources().magic(),
                    damage + VillageCouncilState.currentDay() * 0.22f);
''',
    '''            float endlessBonus = Math.min(7.0f, VillageCouncilState.currentDay() * 0.22f);
            player.hurtServer(level, level.damageSources().magic(), damage + endlessBonus);
'''
)

relic_screen = SRC / "VillageRelicScreen.java"
replace_once(relic_screen, '        int summaryLines = Math.min(4, summary.size());\n',
             '        int summaryLines = Math.min(7, summary.size());\n')
replace_once(relic_screen, '        int panelWidth = Math.min(820, Math.max(300, width - 16));\n',
             '        int panelWidth = Math.max(120, Math.min(820, width - 16));\n')

verify = TOOLS / "verify_jar.py"
text = read(verify).replace("Ten regular enemy roles, four bosses and eight wave traits are present",
                            "Ten regular enemy roles, four bosses and twelve wave traits are present")
write(verify, text)

old_test = TOOLS / "test_v01718_bow_shortcuts.py"
text = read(old_test).replace("v0.17.19 version is active", "v0.18.2 version is active")
write(old_test, text)

test_v0182 = dedent(r'''
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def text(name):
    return (SRC / name).read_text(encoding="utf-8")

def require(condition, message):
    if not condition:
        raise AssertionError(message)
    print("[PASS]", message)

props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
require("mod_version=0.18.2-alpha.1" in props, "v0.18.2-alpha.1 version is active")

shop = text("VillageEquipmentShop.java")
rpg = text("VillageRpgSystem.java")
roles = text("VillageRoleSkillSystem.java")
require("VillageRelicSystem.projectileMultiplier" not in re.search(
    r"public static float outgoingMultiplier.*?public static float incomingMultiplier", shop, re.S).group(0),
    "Equipment outgoing multiplier no longer double-applies relics")
require("VillageRelicSystem.incomingMultiplier" not in re.search(
    r"public static float incomingMultiplier.*?public static float roleSkillMultiplier", shop, re.S).group(0),
    "Equipment incoming multiplier no longer double-applies relics")
require("VillageRelicSystem.skillMultiplier" not in re.search(
    r"public static float roleSkillMultiplier.*?public static int cooldownReductionSeconds", shop, re.S).group(0),
    "Equipment role skill multiplier no longer double-applies relics")
require("VillageRelicSystem.projectileMultiplier(attacker)" in rpg
        and "VillageRelicSystem.meleeMultiplier(attacker)" in rpg
        and "VillageRelicSystem.incomingMultiplier(defender)" in rpg,
        "Final RPG layer applies relic combat multipliers exactly once")
require("VillageRelicSystem.skillMultiplier(player)" in roles,
        "Final role-skill layer retains one relic skill multiplier")

loot = text("VillageRaidLootSystem.java")
trading = text("VillageTradingSystem.java")
require("public static int saleValue(ItemStack stack)" in loot and loot.count(", ChatFormatting.") >= 14,
        "All current raid sale loot shares one value source")
require("VillageRaidLootSystem.saleValue(stack)" in trading
        and "name.startsWith(\"[판매용] \")" not in re.search(
            r"private static boolean isSaleOnlyLoot.*?private static int unitValue", trading, re.S).group(0),
        "Bulk sale only deletes loot that has a real sale value")

relic = text("VillageRelicSystem.java")
controller = text("VillageUiController.java")
require('private static final String OFFER_SEP = ";"' in relic
        and "consumePendingOffer" in relic and "pendingRelics" in relic,
        "Multiple boss relic rewards use a persistent-compatible queue")
require("hasPendingChoice(player)" in controller and "openChoice(player)" in controller,
        "Relic UI advances through queued boss rewards one by one")

respawn = text("VillageRespawnSystem.java")
guardians = text("VillageGuardians.java")
require("LivingDamageEvent.Pre" in respawn and "event.getNewDamage()" in respawn
        and "LivingIncomingDamageEvent" not in respawn,
        "Downing uses final pre-health damage after armor and effects")
require("onFinalDamage(LivingDamageEvent.Pre event)" in guardians,
        "Final-damage downing event is registered")

raid = text("VillageRaidSystem.java")
require("discardTaggedRaidEnemies(server)" in raid
        and "if (entity == null)" in raid and "shouldDiscardStaleRaidEnemy" in raid,
        "Raid restart and unloaded-enemy state cannot leave ghost UUIDs")
require("VillageRaidSystem.shouldDiscardStaleRaidEnemy(mob)" in guardians,
        "Stale persisted raid mobs are discarded when they reload")

effect = text("VillageSkillEffectEntity.java")
ability = text("VillageRoleAbilitySystem.java")
follow = re.search(r"private boolean followsOwner\(\).*?\n    \}", effect, re.S).group(0)
require('"luminar_healing_field"' not in follow,
        "Healing sanctuary visual remains at the same fixed center as gameplay")
require('"arcanist_tornado".equals(kind())' in effect and "scale(1.20)" in ability,
        "Tornado visual and gameplay share live aim and equivalent travel speed")

require("Math.round(skill.baseCooldownSeconds() * 0.20f)" in roles,
        "Skill cooldown uses a per-skill 20 percent floor instead of universal 7 seconds")

merc = text("VillageMercenarySystem.java")
require("VillageMercenarySnapshotData" in merc and "persistNightSnapshot()" in merc,
        "Night-start mercenary snapshot survives server restart")
require((SRC / "VillageMercenarySnapshotData.java").exists(),
        "Mercenary night snapshot has dedicated SavedData")

skilltest = text("VillageSkillTestSystem.java")
require("recoverStrandedAfterRestart" in skilltest
        and "VillageSkillTestSystem.recoverStrandedAfterRestart(player)" in guardians,
        "Restarted players cannot remain stranded in a dead skill-test session")

local = text("VillageLocalActionSystem.java")
require('case "hire_mercenary"' in local and "구형 단일 용병 호출은 제거" in local,
        "Legacy generic mercenary action can no longer spawn obsolete golems")

council = text("VillageCouncilState.java")
require("onPlayerListChanged" in council and "PlayerLoggedOutEvent" in guardians,
        "Time vote is re-evaluated when a player disconnects")

purge = text("VillageGlobalMobPurgeSystem.java")
require("inflate(2048" not in purge and "BATTLEFIELD_RADIUS + 96.0" in purge,
        "Natural mob purge is bounded to the managed battlefield")
require("mob.blockPosition().distSqr(center) > radius * radius" in guardians,
        "Natural spawn suppression no longer affects the whole overworld")

enemy = text("VillageEnemyArchetypeSystem.java")
ability_block = re.search(r"public static void tickAbility.*?public static void onStructureHit", enemy, re.S).group(0)
require("abilityReady(mob, globalTicks" in enemy and "globalTicks %" not in ability_block,
        "Enemy special abilities use per-entity phase offsets")
require("Math.min(7.0f, VillageCouncilState.currentDay() * 0.22f)" in enemy,
        "Endless unavoidable magic damage has a bounded day bonus")

identity = text("VillageEquipmentIdentity.java")
rarity = text("VillageEquipmentRaritySystem.java")
require("DataComponents.CUSTOM_DATA" in identity and "DataComponents.REPAIR_COST" in identity,
        "Generated equipment owns persistent identity and rejects anvil-name spoofing")
require("VillageEquipmentIdentity.stampRarity" in rarity,
        "Rarity and enhancement writes stamp game-owned equipment identity")

screen = text("VillageRelicScreen.java")
require("Math.max(120, Math.min(820, width - 16))" in screen and "Math.min(7, summary.size())" in screen,
        "Relic collection remains bounded on narrow logical resolutions")

print("Village Guardians v0.18.2 runtime stability contracts passed.")
''').lstrip()
write(TOOLS / "test_v0182_runtime_stability.py", test_v0182)

gitignore = ROOT / ".gitignore"
ignore_text = read(gitignore) if gitignore.exists() else ""
for entry in ("tools/__pycache__/\n", "*.pyc\n"):
    if entry.strip() not in ignore_text:
        if ignore_text and not ignore_text.endswith("\n"):
            ignore_text += "\n"
        ignore_text += entry
write(gitignore, ignore_text)

print("Applied Village Guardians v0.18.2 runtime stability overhaul.")
