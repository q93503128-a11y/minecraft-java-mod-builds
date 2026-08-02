package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageProgressionSystem {
    public static final int MAX_BUILDING_LEVEL = 5;
    public static final int MAX_PERSONAL_RANK = 5;
    public static final int STARTING_COINS = 120;

    private static final Map<UUID, Integer> CLAIM_DAYS = new LinkedHashMap<>();
    private static final Map<UUID, Long> TRAINING_READY_AT = new LinkedHashMap<>();
    private static final Map<UUID, Integer> COINS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> FORGE_RANKS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> SKILL_RANKS = new LinkedHashMap<>();
    private static final EnumMap<Building, Integer> DURABILITY = new EnumMap<>(Building.class);

    private static VillageProgressionData savedData;
    private static int supplies = 180;
    private static int wallLevel;
    private static int smithyLevel;
    private static int infirmaryLevel;
    private static int storehouseLevel;
    private static int barracksLevel;
    private static int skillHallLevel;
    private static boolean gameOver;

    private VillageProgressionSystem() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageProgressionData.TYPE);
        supplies = savedData.supplies();
        wallLevel = savedData.wallLevel();
        smithyLevel = savedData.smithyLevel();
        infirmaryLevel = savedData.infirmaryLevel();
        storehouseLevel = savedData.storehouseLevel();
        barracksLevel = savedData.barracksLevel();
        skillHallLevel = savedData.skillHallLevel();
        gameOver = savedData.gameOver();

        CLAIM_DAYS.clear();
        CLAIM_DAYS.putAll(savedData.claimDays());
        COINS.clear();
        COINS.putAll(savedData.coins());
        FORGE_RANKS.clear();
        FORGE_RANKS.putAll(savedData.forgeRanks());
        SKILL_RANKS.clear();
        SKILL_RANKS.putAll(savedData.skillRanks());
        TRAINING_READY_AT.clear();

        DURABILITY.clear();
        Map<String, Integer> loadedDurability = savedData.buildingDurability();
        for (Building building : Building.values()) {
            int loaded = loadedDurability.getOrDefault(building.id(), maxDurability(building));
            DURABILITY.put(building, Math.max(0, Math.min(maxDurability(building), loaded)));
        }
        persist();
    }

    public static synchronized void registerPlayer(ServerPlayer player) {
        boolean changed = false;
        if (!COINS.containsKey(player.getUUID())) {
            COINS.put(player.getUUID(), STARTING_COINS);
            changed = true;
        }
        if (!FORGE_RANKS.containsKey(player.getUUID())) {
            FORGE_RANKS.put(player.getUUID(), 0);
            changed = true;
        }
        if (!SKILL_RANKS.containsKey(player.getUUID())) {
            SKILL_RANKS.put(player.getUUID(), 0);
            changed = true;
        }
        if (changed) {
            persist();
        }
    }

    public static void handleBuildingInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !VillageCouncilState.isInsideVillage(player)) {
            return;
        }
        BlockPos villageCenter = VillageCouncilState.villageCenter().orElse(null);
        if (villageCenter == null) {
            return;
        }
        Building building = VillageFortressBuildings.buildingAtTerminal(level, villageCenter, event.getPos());
        if (building == null) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        VillageUiService.openBuilding(player, building);
    }

    public static synchronized String status() {
        return "보급품 " + supplies
                + " | 북문 Lv." + wallLevel
                + " | 대장간 Lv." + smithyLevel
                + " | 연구소 Lv." + skillHallLevel
                + " | 의무소 Lv." + infirmaryLevel
                + " | 보급소 Lv." + storehouseLevel
                + " | 병영 Lv." + barracksLevel;
    }

    public static synchronized String status(ServerPlayer player) {
        return status() + " | 내 수호 주화 " + coins(player);
    }

    public static synchronized int supplies() {
        return supplies;
    }

    public static synchronized int coins(ServerPlayer player) {
        return COINS.getOrDefault(player.getUUID(), STARTING_COINS);
    }

    public static synchronized int forgeRank(ServerPlayer player) {
        return FORGE_RANKS.getOrDefault(player.getUUID(), 0);
    }

    public static synchronized int skillRank(ServerPlayer player) {
        return SKILL_RANKS.getOrDefault(player.getUUID(), 0);
    }

    public static synchronized int wallLevel() {
        return wallLevel;
    }

    public static synchronized int smithyLevel() {
        return smithyLevel;
    }

    public static synchronized int armoryLevel() {
        return smithyLevel;
    }

    public static synchronized int infirmaryLevel() {
        return infirmaryLevel;
    }

    public static synchronized int storehouseLevel() {
        return storehouseLevel;
    }

    public static synchronized int barracksLevel() {
        return barracksLevel;
    }

    public static synchronized int skillHallLevel() {
        return skillHallLevel;
    }

    public static synchronized boolean isGameOver() {
        return gameOver;
    }

    public static synchronized int level(Building building) {
        return switch (building) {
            case WALLS -> wallLevel;
            case SMITHY -> smithyLevel;
            case SKILL_HALL -> skillHallLevel;
            case INFIRMARY -> infirmaryLevel;
            case STOREHOUSE -> storehouseLevel;
            case BARRACKS -> barracksLevel;
            case TOWN_HALL -> 0;
        };
    }

    public static synchronized int maxDurability(Building building) {
        int level = level(building);
        return switch (building) {
            case WALLS -> 1200 + level * 350;
            case TOWN_HALL -> 800;
            case SMITHY -> 560 + level * 120;
            case SKILL_HALL -> 520 + level * 110;
            case INFIRMARY -> 520 + level * 110;
            case STOREHOUSE -> 560 + level * 120;
            case BARRACKS -> 620 + level * 130;
        };
    }

    public static synchronized int durability(Building building) {
        return DURABILITY.getOrDefault(building, maxDurability(building));
    }

    public static synchronized boolean isOperational(Building building) {
        return durability(building) > 0;
    }

    public static synchronized String durabilityText(Building building) {
        return durability(building) + " / " + maxDurability(building);
    }

    public static synchronized float smithyDamageMultiplier(ServerPlayer player) {
        return 1.0f + smithyLevel * 0.04f + forgeRank(player) * 0.12f;
    }

    public static synchronized float learnedSkillDamageMultiplier(ServerPlayer player) {
        return 1.0f + skillRank(player) * 0.08f;
    }

    public static synchronized float wallDamageMultiplier() {
        if (!isOperational(Building.WALLS)) {
            return 1.0f;
        }
        return Math.max(0.62f, 0.94f - wallLevel * 0.064f);
    }

    public static synchronized int skillDurationBonusTicks(ServerPlayer player) {
        return barracksLevel * 40 + skillRank(player) * 60;
    }

    public static synchronized int skillCooldownReductionSeconds(ServerPlayer player) {
        return barracksLevel * 2 + skillRank(player);
    }

    public static synchronized int raidRewardMultiplierPercent() {
        return 100 + storehouseLevel * 15 + barracksLevel * 5;
    }

    public static synchronized void addSupplies(MinecraftServer server, int amount, String reason) {
        int granted = Math.max(0, amount);
        supplies += granted;
        persist();
        if (granted > 0) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[공동 보급품] §f+" + granted + " | " + reason + " | 현재 " + supplies),
                    false);
        }
    }

    public static synchronized void addCoins(ServerPlayer player, int amount, String reason) {
        int granted = Math.max(0, amount);
        if (granted <= 0) {
            return;
        }
        COINS.put(player.getUUID(), coins(player) + granted);
        persist();
        player.sendSystemMessage(Component.literal("§e+" + granted + " 수호 주화 §7(" + reason + ")"));
    }

    public static void awardRaidCoins(MinecraftServer server, int amount) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            addCoins(player, amount, "습격 방어 보상");
        }
    }

    public static synchronized String claimDailyBread(ServerPlayer player) {
        if (!isOperational(Building.STOREHOUSE)) {
            return "상점·보급소가 파괴되어 오늘의 식량을 받을 수 없습니다.";
        }
        int day = VillageCouncilState.currentDay();
        int lastClaimed = CLAIM_DAYS.getOrDefault(player.getUUID(), 0);
        if (lastClaimed >= day) {
            return "오늘의 빵 보급은 이미 받았습니다.";
        }
        int count = 3 + storehouseLevel * 2;
        ItemStack bread = Items.BREAD.getDefaultInstance();
        bread.setCount(count);
        giveOrDrop(player, bread);
        CLAIM_DAYS.put(player.getUUID(), day);
        persist();
        return "오늘의 빵 " + count + "개를 받았습니다.";
    }

    public static synchronized void grantDailyBreadOnLogin(ServerPlayer player) {
        int day = VillageCouncilState.currentDay();
        if (CLAIM_DAYS.getOrDefault(player.getUUID(), 0) >= day
                || !isOperational(Building.STOREHOUSE)) {
            return;
        }
        player.sendSystemMessage(Component.literal("§6[일일 식량] §f" + claimDailyBread(player)));
    }

    public static synchronized String buyArrows(ServerPlayer player) {
        if (!isOperational(Building.STOREHOUSE)) {
            return "상점·보급소가 파괴되어 상점을 이용할 수 없습니다.";
        }
        int count = 16 + storehouseLevel * 4;
        int cost = 14;
        if (!spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 화살 " + count + "개 가격: " + cost;
        }
        ItemStack arrows = Items.ARROW.getDefaultInstance();
        arrows.setCount(count);
        giveOrDrop(player, arrows);
        return "화살 " + count + "개 구매 완료 | 남은 주화 " + coins(player);
    }

    public static synchronized String buyFood(ServerPlayer player) {
        if (!isOperational(Building.STOREHOUSE)) {
            return "상점·보급소가 파괴되어 상점을 이용할 수 없습니다.";
        }
        int count = 5 + storehouseLevel * 2;
        int cost = 18;
        if (!spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 전투 식량 가격: " + cost;
        }
        ItemStack food = Items.COOKED_BEEF.getDefaultInstance();
        food.setCount(count);
        giveOrDrop(player, food);
        return "전투 식량 " + count + "개 구매 완료 | 남은 주화 " + coins(player);
    }

    public static synchronized String improveForgeRank(ServerPlayer player) {
        if (!isOperational(Building.SMITHY)) {
            return "대장간이 파괴되어 장비 강화를 할 수 없습니다.";
        }
        int current = forgeRank(player);
        if (current >= MAX_PERSONAL_RANK) {
            return "대장간 장비 강화가 최고 단계입니다.";
        }
        int cost = 80 + current * 100;
        if (!spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + coins(player);
        }
        FORGE_RANKS.put(player.getUUID(), current + 1);
        persist();
        return "장비 강화 단계 " + (current + 1) + " 달성 | 공격력 보너스 상승";
    }

    public static synchronized String learnNextSkill(ServerPlayer player) {
        if (!isOperational(Building.SKILL_HALL)) {
            return "기술·마법 연구소가 파괴되어 능력을 배울 수 없습니다.";
        }
        int current = skillRank(player);
        if (current >= MAX_PERSONAL_RANK) {
            return "전투·마법 능력이 최고 단계입니다.";
        }
        int cost = 100 + current * 120;
        if (!spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + coins(player);
        }
        SKILL_RANKS.put(player.getUUID(), current + 1);
        persist();
        return "전투·마법 능력 단계 " + (current + 1) + " 습득 | 역할 스킬과 공격력 강화";
    }

    public static synchronized String train(ServerPlayer player) {
        if (!isOperational(Building.BARRACKS)) {
            return "병영이 파괴되어 훈련할 수 없습니다.";
        }
        long now = System.currentTimeMillis();
        long readyAt = TRAINING_READY_AT.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 999L) / 1000L);
            return "다음 훈련까지 " + seconds + "초 남았습니다.";
        }
        int xp = 30 + barracksLevel * 18;
        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(player, xp);
        TRAINING_READY_AT.put(player.getUUID(), now + 180_000L);
        return "병영 훈련 완료 | XP " + result.awardedExperience()
                + " | 현재 레벨 " + result.current().level();
    }

    public static synchronized String useInfirmary(ServerPlayer player) {
        if (!isOperational(Building.INFIRMARY)) {
            return "의무소가 파괴되어 치료할 수 없습니다.";
        }
        int level = infirmaryLevel;
        float heal = 8.0f + level * 4.0f;
        float before = player.getHealth();
        player.heal(heal);
        if (level >= 1) clearTreatmentEffects(player);
        if (level >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * (6 + level * 2),
                    level >= 5 ? 1 : 0, false, true, true));
        }
        if (level >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 35,
                    Math.min(2, level - 3), false, true, true));
        }
        if (level >= 5) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 8, 0, false, true, true));
        }
        float restored = Math.max(0.0f, player.getHealth() - before);
        return "응급 치료 완료 | 체력 " + Math.round(restored / 2.0f) + "칸 회복"
                + (level >= 1 ? " · 해로운 상태 제거" : "")
                + (level >= 3 ? " · 보호막 지급" : "");
    }

    public static synchronized int respawnDelayTicks() {
        int seconds = isOperational(Building.INFIRMARY) ? Math.max(10, 20 - infirmaryLevel * 2) : 20;
        return seconds * 20;
    }

    private static void clearTreatmentEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.SLOWNESS);
    }

    public static synchronized String upgrade(ServerPlayer player, Building building) {
        if (building == Building.TOWN_HALL) {
            return "마을 회관은 직접 업그레이드하지 않습니다.";
        }
        if (VillageRaidSystem.isRaidLocked()) {
            return "습격 중에는 업그레이드할 수 없습니다.";
        }
        if (!isOperational(building)) {
            return "파괴된 건물은 먼저 수리해야 합니다.";
        }

        int current = level(building);
        if (current >= MAX_BUILDING_LEVEL) {
            return building.displayName() + "은(는) 최고 레벨입니다.";
        }
        int cost = upgradeCost(current);
        if (supplies < cost) {
            return "공동 보급품 부족: 필요 " + cost + ", 현재 " + supplies;
        }

        supplies -= cost;
        setLevel(building, current + 1);
        DURABILITY.put(building, maxDurability(building));
        persist();
        if (player.level() instanceof ServerLevel level) {
            VillageWorldSystem.applyUpgradeVisual(level, building, current + 1);
        }
        return building.displayName() + " Lv." + (current + 1)
                + " 업그레이드 완료 | 보급품 " + supplies;
    }

    public static synchronized String repair(ServerPlayer player, Building building) {
        if (VillageRaidSystem.isRaidLocked()) {
            return "습격 중에는 수리할 수 없습니다.";
        }
        int current = durability(building);
        int maximum = maxDurability(building);
        if (current >= maximum) {
            return building.displayName() + "은(는) 이미 완전한 상태입니다.";
        }
        int missing = maximum - current;
        int cost = Math.max(20, (missing + 7) / 8);
        if (supplies < cost) {
            return "수리비 부족: 보급품 " + cost + " 필요, 현재 " + supplies;
        }
        supplies -= cost;
        DURABILITY.put(building, maximum);
        persist();
        if (player.level() instanceof ServerLevel level) {
            VillageWorldSystem.rebuildStructure(level, building);
        }
        return building.displayName() + " 수리 완료 | 보급품 " + supplies;
    }

    public static synchronized void damageBuilding(MinecraftServer server, Building building, int damage) {
        if (gameOver || damage <= 0 || !isOperational(building)) {
            return;
        }
        int previous = durability(building);
        int next = Math.max(0, previous - damage);
        DURABILITY.put(building, next);
        persist();
        VillageStructureHud.showDamage(server, building, next, maxDurability(building));

        if (next == 0) {
            VillageWorldSystem.destroyStructure(server.overworld(), building);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§c[시설 파괴] §f" + building.displayName()
                            + "이(가) 무너졌습니다. 잔해는 남으며 수리 전까지 이용할 수 없습니다."),
                    false);
        }

        if (allCoreBuildingsDestroyed()) {
            gameOver = true;
            persist();
            VillageRaidSystem.triggerGameOver(server);
        }
    }

    public static void healRaidParty(MinecraftServer server, boolean victory) {
        int level = infirmaryLevel();
        if (!isOperational(Building.INFIRMARY) || (level <= 0 && !victory)) return;
        float heal = victory ? 10.0f + level * 4.0f : 2.0f + level * 2.0f;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!VillageCouncilState.isInsideVillage(player)) continue;
            player.heal(heal);
            if (level >= 1) clearTreatmentEffects(player);
            if (level >= 2) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * (5 + level),
                        level >= 5 ? 1 : 0, false, true, true));
            }
            if (victory && level >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 45,
                        Math.min(2, level - 3), false, true, true));
            }
        }
    }

    public static synchronized void restoreFacilitiesForMigration() {
        gameOver = false;
        supplies = Math.max(100, supplies);
        DURABILITY.clear();
        for (Building building : Building.values()) {
            DURABILITY.put(building, maxDurability(building));
        }
        persist();
    }

    public static synchronized void resetForRestart(MinecraftServer server, boolean fromStart) {
        gameOver = false;
        if (fromStart) {
            supplies = 180;
            wallLevel = 0;
            smithyLevel = 0;
            infirmaryLevel = 0;
            storehouseLevel = 0;
            barracksLevel = 0;
            skillHallLevel = 0;
            CLAIM_DAYS.clear();
            FORGE_RANKS.clear();
            SKILL_RANKS.clear();
            COINS.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                COINS.put(player.getUUID(), STARTING_COINS);
                FORGE_RANKS.put(player.getUUID(), 0);
                SKILL_RANKS.put(player.getUUID(), 0);
            }
        } else {
            supplies = Math.max(100, supplies);
        }

        DURABILITY.clear();
        for (Building building : Building.values()) {
            DURABILITY.put(building, maxDurability(building));
        }
        persist();
        VillageCouncilState.restartGameDay(server, fromStart);
        VillageWorldSystem.forceRebuild(server);
        VillageRaidSystem.resetAfterRestart(server);
    }

    public static int upgradeCost(int currentLevel) {
        return 120 + Math.max(0, currentLevel) * 140;
    }

    public static synchronized boolean spendCoins(ServerPlayer player, int amount) {
        int current = coins(player);
        int cost = Math.max(0, amount);
        if (current < cost) {
            return false;
        }
        COINS.put(player.getUUID(), current - cost);
        persist();
        return true;
    }

    private static boolean allCoreBuildingsDestroyed() {
        for (Building building : Building.values()) {
            if (building != Building.WALLS && isOperational(building)) {
                return false;
            }
        }
        return true;
    }

    private static void setLevel(Building building, int value) {
        int level = Math.max(0, Math.min(MAX_BUILDING_LEVEL, value));
        switch (building) {
            case WALLS -> wallLevel = level;
            case SMITHY -> smithyLevel = level;
            case SKILL_HALL -> skillHallLevel = level;
            case INFIRMARY -> infirmaryLevel = level;
            case STOREHOUSE -> storehouseLevel = level;
            case BARRACKS -> barracksLevel = level;
            case TOWN_HALL -> {
            }
        }
    }

    private static void persist() {
        if (savedData == null) {
            return;
        }
        Map<String, Integer> encodedDurability = new LinkedHashMap<>();
        DURABILITY.forEach((building, hp) -> encodedDurability.put(building.id(), hp));
        savedData.replaceState(
                supplies,
                wallLevel,
                smithyLevel,
                infirmaryLevel,
                storehouseLevel,
                barracksLevel,
                skillHallLevel,
                CLAIM_DAYS,
                COINS,
                FORGE_RANKS,
                SKILL_RANKS,
                encodedDurability,
                gameOver);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public enum Building {
        TOWN_HALL("town_hall", "마을 회관", Blocks.BELL),
        WALLS("walls", "북문·성벽", Blocks.STONECUTTER),
        SMITHY("smithy", "대장간", Blocks.SMITHING_TABLE),
        SKILL_HALL("skill_hall", "기술·마법 연구소", Blocks.ENCHANTING_TABLE),
        INFIRMARY("infirmary", "의무소", Blocks.BREWING_STAND),
        STOREHOUSE("storehouse", "상점·보급소", Blocks.BARREL),
        BARRACKS("barracks", "병영·훈련장", Blocks.TARGET);

        private final String id;
        private final String displayName;
        private final Block terminal;

        Building(String id, String displayName, Block terminal) {
            this.id = id;
            this.displayName = displayName;
            this.terminal = terminal;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public static Building fromTerminal(Block block) {
            for (Building building : values()) {
                if (building.terminal == block) {
                    return building;
                }
            }
            return null;
        }

        public static Building fromId(String id) {
            for (Building building : values()) {
                if (building.id.equals(id)) {
                    return building;
                }
            }
            return null;
        }
    }
}
