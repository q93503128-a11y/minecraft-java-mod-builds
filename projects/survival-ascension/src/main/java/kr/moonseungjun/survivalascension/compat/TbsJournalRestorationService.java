package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One-time migration for players whose native TBS Archivist's Journal was removed by the old
 * compatibility guard. New players are left entirely to TBS's own onboarding flow.
 */
public final class TbsJournalRestorationService {
    private static final Identifier TBS_ARCHIVISTS_JOURNAL = Identifier.fromNamespaceAndPath("tbos", "archivists_journal");
    private static final String RESTORED_TAG = "survivalascension.tbs_journal_restored_v1";
    private static final int RESTORE_DELAY_TICKS = 80;
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private TbsJournalRestorationService() {}

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModList.get().isLoaded("tbos") || player.getTags().contains(RESTORED_TAG)) return;
        if (!ExpeditionData.get(player).tbsJournalGuardChecked(player)) return;
        READY_AT.put(player.getUUID(), player.level().getGameTime() + RESTORE_DELAY_TICKS);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        READY_AT.remove(event.getEntity().getUUID());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) return;
        Long ready = READY_AT.get(player.getUUID());
        if (ready == null || player.level().getGameTime() < ready) return;
        READY_AT.remove(player.getUUID());

        if (!ModList.get().isLoaded("tbos") || player.getTags().contains(RESTORED_TAG)) return;
        if (!ExpeditionData.get(player).tbsJournalGuardChecked(player)) return;

        if (!hasJournal(player)) {
            Item item = BuiltInRegistries.ITEM.getValue(TBS_ARCHIVISTS_JOURNAL);
            if (item == null) return;
            ItemStack restored = new ItemStack(item);
            if (!player.addItem(restored)) {
                player.drop(restored, false);
            }
            player.sendSystemMessage(Component.literal("[Survival Ascension] 기존 버전에서 제거되었던 기록관의 일지를 복구했습니다."));
        }
        player.addTag(RESTORED_TAG);
    }

    private static boolean hasJournal(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && TBS_ARCHIVISTS_JOURNAL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }
}
