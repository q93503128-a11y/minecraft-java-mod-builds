package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.registry.AscensionItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adds Survival Ascension completion materials to the late Fractured Archive without replacing TBOS loot.
 * Rewards are contribution-authoritative and event-driven: no dungeon scan, chest rewrite or external class link.
 *
 * TBOS 0.7 exposes an endless floor index. The bridge reads it reflectively only when a qualifying major target
 * dies, allowing deeper floors to become better endgame farms without hard-linking TBOS implementation classes.
 */
public final class FracturedArchiveRewardService {
    private static final ResourceKey<Level> FRACTURED_ARCHIVE = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath("tbos", "fractured_archive")
    );
    private static final TagKey<EntityType<?>> FINAL_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "fractured_archive_final_targets")
    );

    private static final String DAMAGE_LEDGER_KEY = "survivalascension_archive_reward_damage";
    private static final String CLAIMED_KEY = "survivalascension_archive_reward_claimed";
    private static final int DAMAGE_SCALE = 100;
    private static final float MIN_CONTRIBUTION_HEALTH = 6.0F;
    private static final float MIN_CONTRIBUTION_SHARE = 0.05F;
    private static final AtomicBoolean FLOOR_BRIDGE_WARNED = new AtomicBoolean();

    private FracturedArchiveRewardService() {}

    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F || !(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level) || !isArchive(level)) return;
        if (!ContentPackCompatibility.isMajorExpeditionTarget(mob)) return;

        ServerPlayer player = contributingPlayer(event.getSource(), level);
        if (player == null) return;

        CompoundTag data = mob.getPersistentData();
        CompoundTag ledger = data.getCompoundOrEmpty(DAMAGE_LEDGER_KEY);
        String key = player.getUUID().toString();
        long next = (long) ledger.getIntOr(key, 0) + Math.max(1, Math.round(event.getHealthDamage() * DAMAGE_SCALE));
        ledger.putInt(key, (int) Math.min(Integer.MAX_VALUE, next));
        data.put(DAMAGE_LEDGER_KEY, ledger);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level) || !isArchive(level)) return;
        if (!ContentPackCompatibility.isMajorExpeditionTarget(mob)) return;

        CompoundTag data = mob.getPersistentData();
        if (data.getBooleanOr(CLAIMED_KEY, false)) return;
        data.putBoolean(CLAIMED_KEY, true);

        int threshold = Math.max(
                Math.round(MIN_CONTRIBUTION_HEALTH * DAMAGE_SCALE),
                Math.round(mob.getMaxHealth() * MIN_CONTRIBUTION_SHARE * DAMAGE_SCALE)
        );
        Set<UUID> qualified = new HashSet<>();
        CompoundTag ledger = data.getCompoundOrEmpty(DAMAGE_LEDGER_KEY);
        for (String raw : ledger.keySet()) {
            if (ledger.getIntOr(raw, 0) < threshold) continue;
            try {
                qualified.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
            }
        }
        ServerPlayer killer = contributingPlayer(event.getSource(), level);
        if (killer != null) qualified.add(killer.getUUID());
        if (qualified.isEmpty()) return;

        boolean finalTarget = mob.getType().builtInRegistryHolder().is(FINAL_TARGETS);
        long floorIndex = archiveFloorIndex(level.getServer(), qualified.iterator().next());
        int displayFloor = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, floorIndex) + 1L);
        FracturedArchivePendingData pending = FracturedArchivePendingData.get(level.getServer());
        for (UUID id : qualified) {
            Reward reward = rollReward(level, finalTarget, floorIndex);
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) grant(player, reward, finalTarget, false, displayFloor);
            else pending.add(id, reward.stones(), reward.scraps());
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        FracturedArchivePendingData.Reward pending = FracturedArchivePendingData.get(level.getServer()).take(player.getUUID());
        if (pending.stones() <= 0 && pending.scraps() <= 0) return;
        grant(player, new Reward(pending.stones(), pending.scraps()), false, true, 0);
    }

    /**
     * Floor 1-2 remain useful for learning and enchant completion, but guaranteed Netherite starts only once
     * the party has pushed into floor 3+. Floor 5+ is the intended repeatable deep-farm band and gets one
     * additional stone from the final target instead of simply multiplying raw vanilla valuables.
     */
    private static Reward rollReward(ServerLevel level, boolean finalTarget, long floorIndex) {
        long floor = Math.max(0L, floorIndex);
        int stones;
        int scraps = 0;

        if (finalTarget) {
            stones = floor >= 4L ? 3 : floor >= 2L ? 2 : 1;
            if (floor >= 2L) {
                scraps = 1;
                float extraChance = floor >= 4L ? 0.35F : 0.15F;
                if (level.getRandom().nextFloat() < extraChance) scraps++;
            } else {
                float earlyChance = floor == 1L ? 0.40F : 0.20F;
                if (level.getRandom().nextFloat() < earlyChance) scraps = 1;
            }
        } else {
            stones = 1;
            float scrapChance = floor >= 4L ? 0.35F : floor >= 2L ? 0.20F : 0.0F;
            if (scrapChance > 0.0F && level.getRandom().nextFloat() < scrapChance) scraps = 1;
        }
        return new Reward(stones, scraps);
    }

    private static void grant(ServerPlayer player, Reward reward, boolean finalTarget, boolean restored, int displayFloor) {
        if (reward.stones() > 0) giveOrDrop(player, new ItemStack(AscensionItems.ENCHANTMENT_STONE.get(), reward.stones()));
        if (reward.scraps() > 0) giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, reward.scraps()));

        String scrapText = reward.scraps() > 0 ? " §7· 네더라이트 파편 §6+" + reward.scraps() : "";
        if (restored) {
            player.sendSystemMessage(Component.literal("§d[균열 기록고 정산] §f이탈 중 확보한 보상 · 마력 각인석 §d+"
                    + reward.stones() + scrapText));
            return;
        }
        String floorText = displayFloor > 0 ? " §7· " + displayFloor + "층" : "";
        player.sendSystemMessage(Component.literal(finalTarget
                ? "§5[균열 기록고 심층 보상] §f마력 각인석 §d+" + reward.stones() + scrapText + floorText
                : "§d[균열 기록고 보상] §f마력 각인석 §d+" + reward.stones() + scrapText + floorText));
    }

    /**
     * Soft optional TBOS bridge. 0.7.0 contract audited from ArchiveRunSavedData#get(server),
     * findByMember(UUID), Optional#get and ArchiveRun#floor(). Failure falls back to floor 1 rewards.
     */
    private static long archiveFloorIndex(MinecraftServer server, UUID playerId) {
        try {
            Class<?> savedDataClass = Class.forName("com.nightbeam.tbos.run.ArchiveRunSavedData");
            Object storage = savedDataClass.getMethod("get", MinecraftServer.class).invoke(null, server);
            Method findByMember = savedDataClass.getMethod("findByMember", UUID.class);
            Object optionalValue = findByMember.invoke(storage, playerId);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) return 0L;
            Object run = optional.get();
            Object value = run.getClass().getMethod("floor").invoke(run);
            return value instanceof Number number ? Math.max(0L, number.longValue()) : 0L;
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (FLOOR_BRIDGE_WARNED.compareAndSet(false, true)) {
                SurvivalAscension.LOGGER.warn("TBOS Archive floor reward scaling unavailable; using floor-1 fallback", exception);
            }
            return 0L;
        }
    }

    private static ServerPlayer contributingPlayer(DamageSource source, ServerLevel level) {
        if (source.getEntity() instanceof ServerPlayer direct) return direct;
        return AscensionAffixes.rangedProjectileOwner(source.getDirectEntity(), level);
    }

    private static boolean isArchive(ServerLevel level) {
        return level.dimension().equals(FRACTURED_ARCHIVE);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record Reward(int stones, int scraps) {}
}
