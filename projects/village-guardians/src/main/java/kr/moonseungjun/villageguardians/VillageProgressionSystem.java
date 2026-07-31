package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageProgressionSystem {
    public static final int MAX_BUILDING_LEVEL = 5;

    private static final Map<UUID, Integer> CLAIM_DAYS = new LinkedHashMap<>();
    private static final Map<UUID, Long> TRAINING_READY_AT = new LinkedHashMap<>();

    private static VillageProgressionData savedData;
    private static int supplies = 180;
    private static int wallLevel;
    private static int armoryLevel;
    private static int infirmaryLevel;
    private static int storehouseLevel;
    private static int barracksLevel;

    private VillageProgressionSystem() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageProgressionData.TYPE);
        supplies = savedData.supplies();
        wallLevel = savedData.wallLevel();
        armoryLevel = savedData.armoryLevel();
        infirmaryLevel = savedData.infirmaryLevel();
        storehouseLevel = savedData.storehouseLevel();
        barracksLevel = savedData.barracksLevel();
        CLAIM_DAYS.clear();
        CLAIM_DAYS.putAll(savedData.claimDays());
        TRAINING_READY_AT.clear();
    }

    public static void handleBuildingInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || !VillageCouncilState.isInsideVillage(player)) {
            return;
        }

        Block block = player.level().getBlockState(event.getPos()).getBlock();
        Building building = Building.fromTerminal(block);
        if (building == null) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        String message = player.isShiftKeyDown()
                ? useShiftAction(player, building)
                : useNormalAction(player, building);
        player.sendSystemMessage(Component.literal(message));
    }

    public static synchronized String status() {
        return "§6[마을 발전] §f보급품 " + supplies
                + " | 성벽 " + wallLevel
                + " | 무기고 " + armoryLevel
                + " | 의무소 " + infirmaryLevel
                + " | 창고 " + storehouseLevel
                + " | 병영 " + barracksLevel;
    }

    public static synchronized int supplies() {
        return supplies;
    }

    public static synchronized int wallLevel() {
        return wallLevel;
    }

    public static synchronized int armoryLevel() {
        return armoryLevel;
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

    public static synchronized float armoryDamageMultiplier() {
        return 1.0f + armoryLevel * 0.08f;
    }

    public static synchronized float wallDamageMultiplier() {
        return Math.max(0.65f, 1.0f - wallLevel * 0.06f);
    }

    public static synchronized int skillDurationBonusTicks() {
        return barracksLevel * 40;
    }

    public static synchronized int skillCooldownReductionSeconds() {
        return barracksLevel * 2;
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
                    Component.literal("§6[보급품] §f+" + granted + " | " + reason + " | 현재 " + supplies),
                    false);
        }
    }

    public static void healRaidParty(MinecraftServer server, boolean victory) {
        int level = infirmaryLevel();
        if (level <= 0 && !victory) {
            return;
        }
        float heal = victory ? 6.0f + level * 4.0f : level * 2.0f;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (VillageCouncilState.isInsideVillage(player)) {
                player.heal(heal);
            }
        }
    }

    private static String useShiftAction(ServerPlayer player, Building building) {
        if (building == Building.TOWN_HALL) {
            return VillageCouncilState.proposeAdvanceTime(player);
        }
        return upgrade(player, building);
    }

    private static String useNormalAction(ServerPlayer player, Building building) {
        return switch (building) {
            case TOWN_HALL -> VillageCouncilState.status(player.level().getServer(), player)
                    + "\n" + status()
                    + "\n" + VillageRaidSystem.status();
            case STOREHOUSE -> claimDailyRations(player);
            case ARMORY -> "§6[무기고] §f레벨 " + armoryLevel()
                    + " | 마을 안에서 공격력 +" + (armoryLevel() * 8) + "%"
                    + " | 웅크리고 사용하면 업그레이드";
            case INFIRMARY -> useInfirmary(player);
            case BARRACKS -> train(player);
            case WALLS -> "§6[성벽 관리소] §f레벨 " + wallLevel()
                    + " | 마을 안에서 받는 피해 " + Math.round(wallDamageMultiplier() * 100.0f) + "%"
                    + " | 웅크리고 사용하면 업그레이드";
        };
    }

    private static synchronized String claimDailyRations(ServerPlayer player) {
        int day = VillageCouncilState.currentDay();
        if (CLAIM_DAYS.getOrDefault(player.getUUID(), 0) >= day) {
            return "§6[창고] §f오늘의 전투 보급을 이미 받았습니다.";
        }
        int cost = Math.max(4, 10 - storehouseLevel);
        if (supplies < cost) {
            return "§c[창고] 보급품이 부족합니다. 필요 " + cost + ", 현재 " + supplies;
        }

        supplies -= cost;
        CLAIM_DAYS.put(player.getUUID(), day);
        ItemStack food = Items.COOKED_BEEF.getDefaultInstance();
        food.setCount(6 + storehouseLevel * 2);
        ItemStack arrows = Items.ARROW.getDefaultInstance();
        arrows.setCount(12 + storehouseLevel * 6);
        giveOrDrop(player, food);
        giveOrDrop(player, arrows);
        persist();
        return "§a[창고] §f제 " + day + "일 전투 보급 지급 완료. 보급품 " + supplies + " 남음.";
    }

    private static String useInfirmary(ServerPlayer player) {
        int level = infirmaryLevel();
        float heal = 4.0f + level * 4.0f;
        player.heal(heal);
        return "§d[의무소] §f체력을 " + Math.round(heal / 2.0f) + "칸 회복했습니다."
                + " | 레벨 " + level + " | 웅크리고 사용하면 업그레이드";
    }

    private static String train(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long readyAt = TRAINING_READY_AT.getOrDefault(player.getUUID(), 0L);
        if (readyAt > now) {
            long seconds = Math.max(1L, (readyAt - now + 999L) / 1000L);
            return "§e[병영] §f다음 훈련까지 " + seconds + "초 남았습니다.";
        }

        int xp = 25 + barracksLevel() * 15;
        VillageCouncilState.ExperienceResult result = VillageCouncilState.grantExperience(player, xp);
        TRAINING_READY_AT.put(player.getUUID(), now + 180_000L);
        return "§a[병영 훈련] §fRPG XP " + result.awardedExperience()
                + " 획득 | 현재 레벨 " + result.current().level()
                + " | 웅크리고 사용하면 업그레이드";
    }

    private static synchronized String upgrade(ServerPlayer player, Building building) {
        if (!VillageCouncilState.isMayor(player)) {
            return "§c건물 업그레이드는 촌장만 승인할 수 있습니다.";
        }

        int current = level(building);
        if (current >= MAX_BUILDING_LEVEL) {
            return "§e" + building.displayName + "은(는) 최고 레벨입니다.";
        }

        int cost = upgradeCost(current);
        if (supplies < cost) {
            return "§c보급품 부족: " + building.displayName + " 업그레이드에 " + cost
                    + " 필요, 현재 " + supplies;
        }

        supplies -= cost;
        int next = current + 1;
        setLevel(building, next);
        persist();

        if (player.level() instanceof ServerLevel level) {
            VillageWorldSystem.applyUpgradeVisual(level, building, next);
        }
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§a[마을 발전] §f" + building.displayName + " 레벨 " + next
                            + " 달성 | 보급품 " + supplies + " 남음"),
                    false);
        }
        return building.displayName + " 업그레이드 완료";
    }

    public static int upgradeCost(int currentLevel) {
        return 120 + Math.max(0, currentLevel) * 140;
    }

    private static int level(Building building) {
        return switch (building) {
            case WALLS -> wallLevel;
            case ARMORY -> armoryLevel;
            case INFIRMARY -> infirmaryLevel;
            case STOREHOUSE -> storehouseLevel;
            case BARRACKS -> barracksLevel;
            case TOWN_HALL -> 0;
        };
    }

    private static void setLevel(Building building, int value) {
        int level = Math.max(0, Math.min(MAX_BUILDING_LEVEL, value));
        switch (building) {
            case WALLS -> wallLevel = level;
            case ARMORY -> armoryLevel = level;
            case INFIRMARY -> infirmaryLevel = level;
            case STOREHOUSE -> storehouseLevel = level;
            case BARRACKS -> barracksLevel = level;
            case TOWN_HALL -> {
            }
        }
    }

    private static void persist() {
        if (savedData != null) {
            savedData.replaceState(
                    supplies,
                    wallLevel,
                    armoryLevel,
                    infirmaryLevel,
                    storehouseLevel,
                    barracksLevel,
                    CLAIM_DAYS);
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public enum Building {
        TOWN_HALL("회관", Blocks.BELL),
        WALLS("성벽", Blocks.STONECUTTER),
        ARMORY("무기고", Blocks.SMITHING_TABLE),
        INFIRMARY("의무소", Blocks.BREWING_STAND),
        STOREHOUSE("창고", Blocks.BARREL),
        BARRACKS("병영", Blocks.TARGET);

        private final String displayName;
        private final Block terminal;

        Building(String displayName, Block terminal) {
            this.displayName = displayName;
            this.terminal = terminal;
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
    }
}
