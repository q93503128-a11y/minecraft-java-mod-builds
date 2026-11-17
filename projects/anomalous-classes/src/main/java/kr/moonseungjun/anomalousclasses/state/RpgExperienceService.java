package kr.moonseungjun.anomalousclasses.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Relaxed, survival-first progression. Ordinary hunting and resource gathering
 * are unrestricted; only trivial repeat-position and one-time-award duplication
 * are suppressed.
 */
public final class RpgExperienceService {
    private static final long REPEAT_MINE_TICKS = 2_400L;
    private static final int MAX_RECENT_MINES = 2_048;
    private static final Map<UUID, LinkedHashMap<String, Long>> MINED_POSITIONS = new java.util.HashMap<>();

    private RpgExperienceService() {}

    public static void award(ServerPlayer player, long xp) {
        if (!eligible(player) || xp <= 0) return;
        ClassProfileSavedData saved = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = saved.profile(player.getUUID());
        ClassId active = profile.currentClass().orElse(null);
        if (active == null) return;
        ClassProgress previous = profile.progress(active);
        RpgXpCurve.Progress next = RpgXpCurve.advance(previous.level(), previous.experience(),
                previous.skillPoints(), xp);
        if (next.level() == previous.level() && next.experience() == previous.experience()) return;
        profile.updateProgress(active, previous.withLevelAndExperience(
                next.level(), next.experience(), next.skillPoints()));
        saved.setDirty();
        if (next.levelsGained() > 0) {
            player.displayClientMessage(Component.translatable("message.anomalousclasses.rpg.level_up",
                    next.level(), next.levelsGained() * RpgXpCurve.SKILL_POINTS_PER_LEVEL), true);
        }
    }

    /** Player-wide milestone: switching classes does not duplicate first discoveries. */
    public static void awardFirst(ServerPlayer player, String key, long xp) {
        if (!eligible(player) || xp <= 0 || key == null || key.length() > 256) return;
        ClassProfileSavedData saved = ClassProfileSavedData.get(player.getServer());
        PlayerClassProfile profile = saved.profile(player.getUUID());
        if (profile.currentClass().isEmpty() || !profile.recordRpgMilestone(key)) return;
        saved.setDirty();
        award(player, xp);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob victim)
                || event.getEntity() instanceof PuppetEntity || victim.level().isClientSide()) return;
        MinecraftServer server = victim.level().getServer();
        if (server == null || event.getSource().getEntity() == null) return;
        if (MentalDominatorSavedData.get(server).controlled(victim.getUUID()) != null
                || ParasiteSavedData.get(server).ownerOf(victim.getUUID()) != null) return;
        Entity attacker = event.getSource().getEntity();
        ServerPlayer owner = ownerOf(server, attacker);
        if (owner == null || owner.getUUID().equals(victim.getUUID())
                || owner.isAlliedTo(victim)) return;
        long base = victim instanceof Enemy ? 100L : 16L;
        long healthReward = Math.min(1_400L, (long) Math.ceil(victim.getMaxHealth()
                * (victim instanceof Enemy ? 7.0 : 1.0)));
        award(owner, Math.min(1_600L, base + healthReward));
    }

    private static ServerPlayer ownerOf(MinecraftServer server, Entity attacker) {
        if (attacker instanceof ServerPlayer owner) return owner;
        UUID id = null;
        ClassId required = null;
        if (attacker instanceof PuppetEntity puppet) {
            id = puppet.puppetOwner();
            required = ClassId.PUPPETEER;
        } else {
            MentalDominatorSavedData.ControlledRecord mental =
                    MentalDominatorSavedData.get(server).controlled(attacker.getUUID());
            if (mental != null && mental.linkState() == MentalDominatorSavedData.LinkState.ACTIVE) {
                id = mental.ownerId();
                required = ClassId.MENTAL_DOMINATOR;
            } else if (ParasiteSavedData.get(server).ownerOf(attacker.getUUID()) != null) {
                id = ParasiteSavedData.get(server).ownerOf(attacker.getUUID());
                required = ClassId.PARASITE_MORPHER;
            } else if (attacker instanceof TamableAnimal tame) {
                id = tame.getOwnerUUID();
            }
        }
        if (id == null) return null;
        ServerPlayer owner = server.getPlayerList().getPlayer(id);
        if (owner == null || !owner.isAlive() || owner.level() != attacker.level()) return null;
        if (required != null && ClassProfileSavedData.get(server).profile(id)
                .currentClass().filter(required::equals).isEmpty()) return null;
        return owner;
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        int amount = mineReward(event.getState());
        if (amount <= 0 || !eligible(player)) return;
        long now = player.serverLevel().getGameTime();
        String key = player.serverLevel().dimension().location() + ":" + event.getPos().asLong();
        LinkedHashMap<String, Long> recent = MINED_POSITIONS.computeIfAbsent(player.getUUID(),
                ignored -> new LinkedHashMap<>(32, .75f, true) {
                    @Override protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                        return size() > MAX_RECENT_MINES;
                    }
                });
        Long previous = recent.get(key);
        if (previous != null && now >= previous && now - previous < REPEAT_MINE_TICKS) return;
        recent.put(key, now);
        award(player, amount);
    }

    private static int mineReward(BlockState state) {
        if (state.is(Blocks.ANCIENT_DEBRIS)) return 450;
        if (state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)) return 220;
        if (state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)) return 95;
        if (state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COPPER_ORES)) return 65;
        if (state.is(BlockTags.COAL_ORES) || state.is(Blocks.NETHER_QUARTZ_ORE)) return 40;
        if (state.is(BlockTags.LOGS)) return 7;
        return 0;
    }

    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getCrafting().isEmpty()) return;
        var item = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        String path = item.getPath();
        long reward = (path.contains("pickaxe") || path.contains("sword") || path.contains("axe")
                || path.contains("helmet") || path.contains("chestplate")
                || path.contains("leggings") || path.contains("boots")
                || item.getNamespace().equals("anomalousclasses")) ? 350L : 100L;
        awardFirst(player, "craft:" + item, reward);
    }

    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var id = event.getAdvancement().id();
        if (id.getPath().startsWith("recipes/")) return;
        long reward = id.getNamespace().equals("minecraft") ? 1_200L : 850L;
        awardFirst(player, "advancement:" + id, reward);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 200 != 0
                || !eligible(player)) return;
        String dimension = player.serverLevel().dimension().location().toString();
        player.serverLevel().getBiome(player.blockPosition()).unwrapKey().ifPresent(biome -> {
            String biomeKey = "biome:" + dimension + ":" + biome.location();
            ClassProfileSavedData data = ClassProfileSavedData.get(player.getServer());
            PlayerClassProfile profile = data.profile(player.getUUID());
            if (profile.currentClass().isEmpty()) return;
            // The login/spawn position is the starting point, not a new discovery.
            if (profile.rpgMilestones().stream().noneMatch(key -> key.startsWith("biome:"))) {
                profile.recordRpgMilestone("dimension:" + dimension);
                profile.recordRpgMilestone(biomeKey);
                data.setDirty();
                return;
            }
            awardFirst(player, "dimension:" + dimension, 800L);
            awardFirst(player, biomeKey, 600L);
        });
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MINED_POSITIONS.remove(event.getEntity().getUUID());
    }

    private static boolean eligible(ServerPlayer player) {
        return player.getServer() != null && !player.isCreative() && !player.isSpectator()
                && player.isAlive();
    }
}
