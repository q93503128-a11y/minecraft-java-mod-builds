package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
    private static final EnumMap<Building, Integer> NIGHT_START_DURABILITY = new EnumMap<>(Building.class);
    private static int nightPlanDay;
    private static int nightPlanPlayers = 1;
    private static boolean retryPlanLocked;

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
        NIGHT_START_DURABILITY.clear();
        Map<String, Integer> loadedDurability = savedData.buildingDurability();
        nightPlanDay = Math.max(0, loadedDurability.getOrDefault("$night_plan_day", 0));
        nightPlanPlayers = Math.max(1, loadedDurability.getOrDefault("$night_plan_players", 1));
        retryPlanLocked = loadedDurability.getOrDefault("$retry_plan_locked", 0) > 0;
        for (Building building : Building.values()) {
            int loaded = loadedDurability.getOrDefault(building.id(), maxDurability(building));
            DURABILITY.put(building, Math.max(0, Math.min(maxDurability(building), loaded)));
            int snapshot = loadedDurability.getOrDefault("$night_" + building.id(), loaded);
            NIGHT_START_DURABILITY.put(building, Math.max(0, Math.min(maxDurability(building), snapshot)));
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

    public static synchronized void captureNightStartSnapshot(MinecraftServer server) {
        NIGHT_START_DURABILITY.clear();
        for (Building building : Building.values()) NIGHT_START_DURABILITY.put(building, durability(building));
        int day = VillageCouncilState.currentDay();
        if (!(retryPlanLocked && nightPlanDay == day)) {
            nightPlanDay = day;
            nightPlanPlayers = Math.max(1, server.getPlayerList().getPlayerCount());
        }
        retryPlanLocked = false;
        VillageMercenarySystem.captureNightSnapshot(server);
        persist();
    }
    public static synchronized int plannedRaidPlayerCount(MinecraftServer server) {
        return nightPlanDay == VillageCouncilState.currentDay() ? Math.max(1, nightPlanPlayers)
                : Math.max(1, server.getPlayerList().getPlayerCount());
    }
    public static synchronized int previewRaidPlayerCount(MinecraftServer server) {
        if (server == null) return Math.max(1, nightPlanPlayers);
        if (retryPlanLocked && nightPlanDay == VillageCouncilState.currentDay()) return Math.max(1, nightPlanPlayers);
        return Math.max(1, server.getPlayerList().getPlayerCount());
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
        return 1.0f + smithyLevel * 0.04f;
    }

    public static synchronized int experienceMultiplierPercent() {
        return isOperational(Building.BARRACKS) ? 100 + barracksLevel * 10 : 100;
    }

    public static synchronized float learnedSkillDamageMultiplier(ServerPlayer player) {
        return 1.0f + skillRank(player) * 0.08f;
    }

    public static synchronized float skillHallPowerMultiplier() {
        return isOperational(Building.SKILL_HALL) ? 1.0f + skillHallLevel * 0.05f : 1.0f;
    }

    public static synchronized float skillHallDurationMultiplier() {
        return isOperational(Building.SKILL_HALL) ? 1.0f + skillHallLevel * 0.05f : 1.0f;
    }

    public static synchronized float wallDamageMultiplier() {
        if (!isOperational(Building.WALLS)) {
            return 1.0f;
        }
        return Math.max(0.62f, 0.94f - wallLevel * 0.064f);
    }

    public static synchronized int skillDurationBonusTicks(ServerPlayer player) {
        return skillHallLevel * 30 + skillRank(player) * 60;
    }

    public static synchronized int skillCooldownReductionSeconds(ServerPlayer player) {
        int research = isOperational(Building.SKILL_HALL) ? skillHallLevel : 0;
        int barracksSupport = isOperational(Building.BARRACKS) ? barracksLevel / 2 : 0;
        return Math.min(7, research + barracksSupport + skillRank(player) / 2);
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
            return "오늘의 배급 식량은 이미 받았습니다.";
        }
        int count = 3 + storehouseLevel * 2;
        ItemStack bread = Items.BREAD.getDefaultInstance();
        bread.setCount(count);
        bread.set(DataComponents.CUSTOM_NAME,
                Component.literal("마을 배급 식량").withStyle(ChatFormatting.GOLD));
        giveOrDrop(player, bread);
        CLAIM_DAYS.put(player.getUUID(), day);
        persist();
        return "오늘의 배급 식량 " + count + "개를 받았습니다.";
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
        arrows.set(DataComponents.CUSTOM_NAME,
                Component.literal("수호 화살").withStyle(ChatFormatting.WHITE));
        giveOrDrop(player, arrows);
        return "화살 " + count + "개 구매 완료 | 남은 주화 " + coins(player);
    }

    public static synchronized String buyFood(ServerPlayer player) {
        return "유료 일반 식량은 일일 배급 식량으로 통합되었습니다. 상점의 전투 소모품을 이용하세요.";
    }

    public static synchronized String improveForgeRank(ServerPlayer player) {
        return "장비 강화는 대장간에서 강화할 장비를 직접 선택하는 방식으로 변경되었습니다.";
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
        return "병영 훈련은 패시브 효과입니다. 현재 모든 경험치 획득량 +"
                + (experienceMultiplierPercent() - 100) + "%";
    }

    public static synchronized String useInfirmary(ServerPlayer player) {
        if (!isOperational(Building.INFIRMARY)) return "의무소가 파괴되어 효과를 받을 수 없습니다.";
        if (isDaytime()) player.setHealth(player.getMaxHealth());
        applyInfirmaryBuffs(player);
        return "의무소 효과 적용 | 낮에는 체력이 완전히 회복되고 시설 단계에 따라 전투 버프를 받습니다.";
    }

    public static synchronized int respawnDelayTicks() {
        return 20 * 20;
    }

    public static void tickInfirmary(MinecraftServer server) {
        if (!isOperational(Building.INFIRMARY)) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || !VillageCouncilState.isInsideVillage(player)) continue;
            if (isDaytime()) player.setHealth(player.getMaxHealth());
            applyInfirmaryBuffs(player);
        }
    }

    private static boolean isDaytime() {
        return VillageCouncilState.currentPhase() == VillageTimePhase.DAY;
    }

    private static void applyInfirmaryBuffs(ServerPlayer player) {
        int level = infirmaryLevel();
        if (level >= 1) player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, false, true, true));
        if (level >= 2) player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, false, true, true));
        if (level >= 3) player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, false, true, true));
        if (level >= 4) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
        if (level >= 5) player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, false, true, true));
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

        if (building == Building.TOWN_HALL && next == 0 && !gameOver) {
            gameOver = true;
            persist();
            VillageRaidSystem.triggerGameOver(server);
        }
    }

    public static void healRaidParty(MinecraftServer server, boolean victory) {
        if (!isOperational(Building.INFIRMARY)) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (VillageCouncilState.isInsideVillage(player)) applyInfirmaryBuffs(player);
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
        VillageRaidSystem.resetAfterRestart(server);
        VillageSkillTestSystem.clearAll(server);
        if (fromStart) {
            supplies = 180; wallLevel = 0; smithyLevel = 0; infirmaryLevel = 0;
            storehouseLevel = 0; barracksLevel = 0; skillHallLevel = 0;
            CLAIM_DAYS.clear(); FORGE_RANKS.clear(); SKILL_RANKS.clear(); COINS.clear();
            NIGHT_START_DURABILITY.clear(); nightPlanDay = 0; nightPlanPlayers = 1; retryPlanLocked = false;
            VillageSkillTreeSystem.resetForNewGame();
            VillageRoleSkillSystem.resetForNewGame();
            VillageDefenseResearchSystem.resetForNewGame();
            VillageRelicSystem.resetForNewGame();
            VillageMercenarySystem.resetForNewGame(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                COINS.put(player.getUUID(), STARTING_COINS);
                FORGE_RANKS.put(player.getUUID(), 0);
                SKILL_RANKS.put(player.getUUID(), 0);
            }
        } else {
            retryPlanLocked = true;
            VillageMercenarySystem.restoreNightSnapshot(server);
        }
        DURABILITY.clear();
        for (Building building : Building.values()) {
            int restored = fromStart ? maxDurability(building)
                    : NIGHT_START_DURABILITY.getOrDefault(building, maxDurability(building));
            DURABILITY.put(building, Math.max(0, Math.min(maxDurability(building), restored)));
        }
        persist();
        VillageCouncilState.restartGameDay(server, fromStart);
        VillageWorldSystem.forceRebuild(server);
        if (fromStart) for (ServerPlayer player : server.getPlayerList().getPlayers())
            VillageStarterKit.resetForNewGame(player);
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
        return !isOperational(Building.TOWN_HALL);
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
        NIGHT_START_DURABILITY.forEach((building, hp) -> encodedDurability.put("$night_" + building.id(), hp));
        encodedDurability.put("$night_plan_day", nightPlanDay);
        encodedDurability.put("$night_plan_players", Math.max(1, nightPlanPlayers));
        encodedDurability.put("$retry_plan_locked", retryPlanLocked ? 1 : 0);
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
