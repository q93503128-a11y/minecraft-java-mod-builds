package kr.moonseungjun.survivalascension.endgame;

import kr.moonseungjun.survivalascension.elite.EliteMobSystem;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.registry.AscensionItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Field-Mythic endgame loop: meaningful contributors build deterministic netherite and tempering progress.
 * Expensive work is event-driven; no world/player scan runs per tick.
 */
public final class MythicEndgameRewardService {
    private static final String DAMAGE_LEDGER_KEY = "survivalascension_mythic_endgame_damage";
    private static final String CLAIMED_KEY = "survivalascension_mythic_endgame_claimed";
    private static final String PLAYER_ENDGAME_KEY = "survivalascension_endgame";
    private static final String ALLOY_PROGRESS_KEY = "mythic_alloy_progress";
    private static final String TEMPERING_PROGRESS_KEY = "mythic_tempering_progress";

    private static final int DAMAGE_SCALE = 100;
    private static final int ALLOY_KILLS_PER_SCRAP = 3;
    private static final int TEMPERING_KILLS_PER_SEAL = 4;
    private static final float MIN_CONTRIBUTION_HEALTH = 6.0F;
    private static final float MIN_CONTRIBUTION_SHARE = 0.05F;

    private MythicEndgameRewardService() {}

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || EliteMobSystem.rankId(mob) != 3) return;
        if (event.getHealthDamage() <= 0.0F) return;

        ServerPlayer player = contributingPlayer(event.getSource());
        if (player == null) return;

        CompoundTag entityData = mob.getPersistentData();
        CompoundTag ledger = entityData.getCompoundOrEmpty(DAMAGE_LEDGER_KEY);
        String key = player.getUUID().toString();
        int added = Math.max(1, Math.round(event.getHealthDamage() * DAMAGE_SCALE));
        long next = (long) ledger.getIntOr(key, 0) + added;
        ledger.putInt(key, (int) Math.min(Integer.MAX_VALUE, next));
        entityData.put(DAMAGE_LEDGER_KEY, ledger);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || EliteMobSystem.rankId(mob) != 3) return;
        if (!(mob.level() instanceof ServerLevel level)) return;

        CompoundTag entityData = mob.getPersistentData();
        if (entityData.getBooleanOr(CLAIMED_KEY, false)) return;
        entityData.putBoolean(CLAIMED_KEY, true);

        int threshold = Math.max(
                Math.round(MIN_CONTRIBUTION_HEALTH * DAMAGE_SCALE),
                Math.round(mob.getMaxHealth() * MIN_CONTRIBUTION_SHARE * DAMAGE_SCALE)
        );

        Set<UUID> qualified = new HashSet<>();
        CompoundTag ledger = entityData.getCompoundOrEmpty(DAMAGE_LEDGER_KEY);
        for (String key : ledger.keySet()) {
            if (ledger.getIntOr(key, 0) < threshold) continue;
            try {
                qualified.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
            }
        }

        ServerPlayer killer = contributingPlayer(event.getSource());
        if (killer != null) qualified.add(killer.getUUID());
        if (qualified.isEmpty()) return;

        MinecraftServer server = level.getServer();
        MythicEndgamePendingData pending = MythicEndgamePendingData.get(server);
        for (UUID playerId : qualified) {
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            if (online != null) applyQualifiedKills(online, 1, false);
            else pending.add(playerId, 1);
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int pending = MythicEndgamePendingData.get(player.getServer()).take(player.getUUID());
        if (pending > 0) applyQualifiedKills(player, pending, true);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack seal = event.getItemStack();
        if (!seal.is(AscensionItems.TEMPERING_SEAL.get())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        InteractionHand targetHand = event.getHand() == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        ItemStack target = player.getItemInHand(targetHand);
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.literal("§6[담금질 인장] §f반대 손에 강화할 장비를 들고 허공에서 사용하세요."));
            return;
        }

        Integer storedCost = target.get(DataComponents.REPAIR_COST);
        int repairCost = storedCost == null ? 0 : storedCost;
        if (repairCost <= 0) {
            player.sendSystemMessage(Component.literal("§6[담금질 인장] §f이 장비에는 지울 누적 모루 부담이 없습니다."));
            return;
        }

        target.set(DataComponents.REPAIR_COST, 0);
        if (!player.isCreative()) seal.shrink(1);
        player.sendSystemMessage(Component.literal("§6[담금질 인장] §a누적 모루 부담을 지웠습니다. §f인챈트와 장비 옵션은 그대로 유지됩니다."));
    }

    private static ServerPlayer contributingPlayer(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer direct) return direct;
        return AscensionAffixes.rangedProjectileOwner(source.getDirectEntity());
    }

    private static void applyQualifiedKills(ServerPlayer player, int killCount, boolean restored) {
        if (killCount <= 0) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        CompoundTag endgame = persisted.getCompoundOrEmpty(PLAYER_ENDGAME_KEY);

        int alloyTotal = Math.max(0, endgame.getIntOr(ALLOY_PROGRESS_KEY, 0)) + killCount;
        int temperingTotal = Math.max(0, endgame.getIntOr(TEMPERING_PROGRESS_KEY, 0)) + killCount;
        int scrapRewards = alloyTotal / ALLOY_KILLS_PER_SCRAP;
        int sealRewards = temperingTotal / TEMPERING_KILLS_PER_SEAL;
        int alloyProgress = alloyTotal % ALLOY_KILLS_PER_SCRAP;
        int temperingProgress = temperingTotal % TEMPERING_KILLS_PER_SEAL;

        endgame.putInt(ALLOY_PROGRESS_KEY, alloyProgress);
        endgame.putInt(TEMPERING_PROGRESS_KEY, temperingProgress);
        persisted.put(PLAYER_ENDGAME_KEY, endgame);
        root.put(Player.PERSISTED_NBT_TAG, persisted);

        if (scrapRewards > 0) giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, scrapRewards));
        if (sealRewards > 0) giveOrDrop(player, new ItemStack(AscensionItems.TEMPERING_SEAL.get(), sealRewards));

        String restoredText = restored ? " §7· 이탈 중 전투 기여 " + killCount + "회 정산" : "";
        String rewardText = (scrapRewards > 0 ? " §8· §6네더라이트 파편 +" + scrapRewards : "")
                + (sealRewards > 0 ? " §8· §6담금질 인장 +" + sealRewards : "");
        player.sendSystemMessage(Component.literal(
                "§6[신화 사냥] §f유효 기여 인정" + restoredText + rewardText
                        + " §7· 고대 합금 " + alloyProgress + "/" + ALLOY_KILLS_PER_SCRAP
                        + " · 담금질 " + temperingProgress + "/" + TEMPERING_KILLS_PER_SEAL
        ));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
