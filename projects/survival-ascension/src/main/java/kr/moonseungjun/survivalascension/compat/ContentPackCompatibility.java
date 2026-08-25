package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Small, dependency-free compatibility seam for content supplied by the Survival Ascension pack.
 *
 * External mods are never linked by their implementation classes here. Common Minecraft/NeoForge
 * contracts plus Survival-owned data tags keep Survival Ascension loadable when optional content is
 * absent and let future compatible content participate without another hard-coded registry list.
 */
public final class ContentPackCompatibility {
    private static final TagKey<EntityType<?>> EXPEDITION_MAJOR_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );
    private static final Identifier TBS_ARCHIVISTS_JOURNAL = Identifier.fromNamespaceAndPath("tbos", "archivists_journal");
    private static final int TBS_JOURNAL_CHECK_DELAY_TICKS = 60;
    private static final Map<UUID, Long> TBS_JOURNAL_CHECK_READY = new HashMap<>();

    private ContentPackCompatibility() {}

    /** Hostile mobs remain the normal combat target; common-tagged bosses and Survival-tagged
     * major targets are included even when their implementation does not use Minecraft's Enemy marker. */
    public static boolean isCombatTarget(LivingEntity entity) {
        return entity instanceof Enemy
                || entity.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)
                || isMajorExpeditionTarget(entity);
    }

    /** Data-driven major-target contract. Optional content IDs live in datapack JSON, never here. */
    public static boolean isMajorExpeditionTarget(LivingEntity entity) {
        return entity.getType().builtInRegistryHolder().is(EXPEDITION_MAJOR_TARGETS);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!ModList.get().isLoaded("tbos") || ExpeditionData.get(player).tbsJournalGuardChecked(player)) return;
        TBS_JOURNAL_CHECK_READY.put(player.getUUID(), player.level().getGameTime() + TBS_JOURNAL_CHECK_DELAY_TICKS);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TBS_JOURNAL_CHECK_READY.remove(event.getEntity().getUUID());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) || player.tickCount % 5 != 0) return;
        Long ready = TBS_JOURNAL_CHECK_READY.get(player.getUUID());
        if (ready == null || player.level().getGameTime() < ready) return;
        TBS_JOURNAL_CHECK_READY.remove(player.getUUID());
        if (!ModList.get().isLoaded("tbos")) return;
        ExpeditionData data = ExpeditionData.get(player);
        if (data.tbsJournalGuardChecked(player)) return;
        removeOneInitialTbsJournal(player);
        data.markTbsJournalGuardChecked(player);
    }

    private static boolean removeOneInitialTbsJournal(net.minecraft.server.level.ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !TBS_ARCHIVISTS_JOURNAL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) continue;
            stack.shrink(1);
            inventory.setChanged();
            return true;
        }
        return false;
    }
}
