package dev.moonseungjun.augmentationsurvival.menu;

import dev.moonseungjun.augmentationsurvival.acquisition.AugmentOfferService;
import dev.moonseungjun.augmentationsurvival.acquisition.CoreUpgradeService;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.device.AugmentDeviceBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Server-backed menu state for the augmentation workstations.
 *
 * <p>The menu intentionally keeps progression cores in the player's inventory. Choosing a tier is
 * the UI equivalent of inserting one core; the server only removes it when the final selection
 * commits successfully.</p>
 */
public final class AugmentDeviceMenu extends AbstractContainerMenu {
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_READY = 1;
    public static final int STATUS_NO_CORE = 2;
    public static final int STATUS_FULLY_MAXED = 3;
    public static final int STATUS_ACQUIRED = 4;
    public static final int STATUS_INVALID = 5;
    public static final int STATUS_UPGRADED = 6;
    public static final int STATUS_NOT_ENOUGH_CORES = 7;

    private static final int DATA_STATUS = 0;
    private static final int DATA_TIER = 1;
    private static final int DATA_CANDIDATE_0 = 2;
    private static final int DATA_CANDIDATE_1 = 3;
    private static final int DATA_CANDIDATE_2 = 4;
    private static final int DATA_SELECTION_COUNT = 5;
    private static final int DATA_SELECTED_MASK = 6;
    private static final int DATA_LAST_OUTPUT_COUNT = 7;
    private static final int DATA_COUNT = 8;

    private static final int BUTTON_BEGIN_TIER = 0;
    private static final int BUTTON_SELECT_CANDIDATE = 100;
    private static final int BUTTON_UPGRADE_ONE = 200;
    private static final int BUTTON_UPGRADE_ALL = 220;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final boolean advanced;
    private String pendingToken;

    public AugmentDeviceMenu(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access,
            boolean advanced
    ) {
        this(
                containerId,
                inventory,
                access,
                advanced,
                new SimpleContainerData(DATA_COUNT)
        );
    }

    private AugmentDeviceMenu(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access,
            boolean advanced,
            ContainerData data
    ) {
        super(advanced ? AugmentDeviceMenus.ADVANCED : AugmentDeviceMenus.BASIC, containerId);
        this.access = access;
        this.advanced = advanced;
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addDataSlots(data);
        resetViewState();
    }

    public static AugmentDeviceMenu clientBasic(int containerId, Inventory inventory) {
        return new AugmentDeviceMenu(
                containerId,
                inventory,
                ContainerLevelAccess.NULL,
                false
        );
    }

    public static AugmentDeviceMenu clientAdvanced(int containerId, Inventory inventory) {
        return new AugmentDeviceMenu(
                containerId,
                inventory,
                ContainerLevelAccess.NULL,
                true
        );
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!isRecognizedButton(buttonId)) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        if (buttonId >= BUTTON_BEGIN_TIER
                && buttonId < BUTTON_BEGIN_TIER + AugmentTier.values().length) {
            beginOffer(serverPlayer, AugmentTier.values()[buttonId - BUTTON_BEGIN_TIER]);
            return true;
        }
        if (buttonId >= BUTTON_SELECT_CANDIDATE
                && buttonId < BUTTON_SELECT_CANDIDATE + 3) {
            selectCandidate(serverPlayer, buttonId - BUTTON_SELECT_CANDIDATE);
            return true;
        }
        if (advanced
                && buttonId >= BUTTON_UPGRADE_ONE
                && buttonId < BUTTON_UPGRADE_ONE + AugmentTier.values().length - 1) {
            upgrade(serverPlayer, AugmentTier.values()[buttonId - BUTTON_UPGRADE_ONE], false);
            return true;
        }
        if (advanced
                && buttonId >= BUTTON_UPGRADE_ALL
                && buttonId < BUTTON_UPGRADE_ALL + AugmentTier.values().length - 1) {
            upgrade(serverPlayer, AugmentTier.values()[buttonId - BUTTON_UPGRADE_ALL], true);
            return true;
        }
        return true;
    }

    private void beginOffer(ServerPlayer player, AugmentTier tier) {
        if (pendingToken != null) {
            AugmentOfferService.cancelOffer(player, pendingToken);
            pendingToken = null;
        }
        AugmentOfferService.BeginResult result = AugmentOfferService.beginOffer(player, tier);
        resetViewState();
        data.set(DATA_TIER, tier.ordinal());

        switch (result.status()) {
            case READY -> {
                pendingToken = result.token();
                data.set(DATA_STATUS, STATUS_READY);
                data.set(DATA_SELECTION_COUNT, result.selectionCount());
                for (int index = 0; index < result.candidateIds().size() && index < 3; index++) {
                    data.set(DATA_CANDIDATE_0 + index, AugmentCatalog.indexOf(result.candidateIds().get(index)));
                }
            }
            case NO_CORE -> data.set(DATA_STATUS, STATUS_NO_CORE);
            case FULLY_MAXED -> data.set(DATA_STATUS, STATUS_FULLY_MAXED);
        }
        broadcastChanges();
    }

    private void selectCandidate(ServerPlayer player, int candidateSlot) {
        if (pendingToken == null || data.get(DATA_STATUS) != STATUS_READY) {
            data.set(DATA_STATUS, STATUS_INVALID);
            broadcastChanges();
            return;
        }
        int catalogIndex = data.get(DATA_CANDIDATE_0 + candidateSlot);
        if (catalogIndex < 0) {
            data.set(DATA_STATUS, STATUS_INVALID);
            broadcastChanges();
            return;
        }

        int mask = data.get(DATA_SELECTED_MASK);
        int bit = 1 << candidateSlot;
        if ((mask & bit) != 0) {
            data.set(DATA_SELECTED_MASK, mask & ~bit);
            broadcastChanges();
            return;
        }
        mask |= bit;
        data.set(DATA_SELECTED_MASK, mask);

        int required = data.get(DATA_SELECTION_COUNT);
        if (Integer.bitCount(mask) < required) {
            broadcastChanges();
            return;
        }

        List<String> selected = new ArrayList<>(required);
        for (int index = 0; index < 3; index++) {
            if ((mask & (1 << index)) == 0) {
                continue;
            }
            int selectedCatalogIndex = data.get(DATA_CANDIDATE_0 + index);
            if (selectedCatalogIndex >= 0) {
                selected.add(AugmentCatalog.definitionAt(selectedCatalogIndex).id());
            }
        }

        AugmentOfferService.CommitResult result = AugmentOfferService.commitOffer(
                player,
                pendingToken,
                selected
        );
        if (result.status() == AugmentOfferService.CommitStatus.SUCCESS) {
            pendingToken = null;
            data.set(DATA_STATUS, STATUS_ACQUIRED);
        } else {
            data.set(DATA_STATUS, STATUS_INVALID);
        }
        broadcastChanges();
    }

    private void upgrade(ServerPlayer player, AugmentTier sourceTier, boolean all) {
        if (pendingToken != null) {
            AugmentOfferService.cancelOffer(player, pendingToken);
            pendingToken = null;
        }
        CoreUpgradeService.UpgradeResult result = all
                ? CoreUpgradeService.upgradeAll(player, sourceTier)
                : CoreUpgradeService.upgradeOne(player, sourceTier);
        resetViewState();
        data.set(DATA_TIER, sourceTier.ordinal());
        data.set(DATA_LAST_OUTPUT_COUNT, result.outputCount());
        data.set(
                DATA_STATUS,
                result.status() == CoreUpgradeService.UpgradeStatus.SUCCESS
                        ? STATUS_UPGRADED
                        : STATUS_NOT_ENOUGH_CORES
        );
        broadcastChanges();
    }

    private boolean isRecognizedButton(int buttonId) {
        if (buttonId >= BUTTON_BEGIN_TIER
                && buttonId < BUTTON_BEGIN_TIER + AugmentTier.values().length) {
            return true;
        }
        if (buttonId >= BUTTON_SELECT_CANDIDATE
                && buttonId < BUTTON_SELECT_CANDIDATE + 3) {
            return true;
        }
        if (!advanced) {
            return false;
        }
        return (buttonId >= BUTTON_UPGRADE_ONE
                && buttonId < BUTTON_UPGRADE_ONE + AugmentTier.values().length - 1)
                || (buttonId >= BUTTON_UPGRADE_ALL
                && buttonId < BUTTON_UPGRADE_ALL + AugmentTier.values().length - 1);
    }

    private void resetViewState() {
        data.set(DATA_STATUS, STATUS_IDLE);
        data.set(DATA_TIER, -1);
        data.set(DATA_CANDIDATE_0, -1);
        data.set(DATA_CANDIDATE_1, -1);
        data.set(DATA_CANDIDATE_2, -1);
        data.set(DATA_SELECTION_COUNT, 0);
        data.set(DATA_SELECTED_MASK, 0);
        data.set(DATA_LAST_OUTPUT_COUNT, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                advanced
                        ? AugmentDeviceBlocks.ADVANCED_AUGMENT_DEVICE
                        : AugmentDeviceBlocks.AUGMENT_DEVICE
        );
    }

    @Override
    public void removed(Player player) {
        if (player instanceof ServerPlayer serverPlayer && pendingToken != null) {
            AugmentOfferService.cancelOffer(serverPlayer, pendingToken);
            pendingToken = null;
        }
        super.removed(player);
    }

    public boolean advanced() {
        return advanced;
    }

    public int status() {
        return data.get(DATA_STATUS);
    }

    public int tierOrdinal() {
        return data.get(DATA_TIER);
    }

    public int selectionCount() {
        return data.get(DATA_SELECTION_COUNT);
    }

    public int selectedMask() {
        return data.get(DATA_SELECTED_MASK);
    }

    public int lastOutputCount() {
        return data.get(DATA_LAST_OUTPUT_COUNT);
    }

    public AugmentDefinition candidate(int slot) {
        if (slot < 0 || slot >= 3) {
            throw new IllegalArgumentException("Candidate slot out of range: " + slot);
        }
        int index = data.get(DATA_CANDIDATE_0 + slot);
        return index < 0 ? null : AugmentCatalog.definitionAt(index);
    }

    public static int beginButtonId(AugmentTier tier) {
        return BUTTON_BEGIN_TIER + tier.ordinal();
    }

    public static int candidateButtonId(int slot) {
        if (slot < 0 || slot >= 3) {
            throw new IllegalArgumentException("Candidate slot out of range: " + slot);
        }
        return BUTTON_SELECT_CANDIDATE + slot;
    }

    public static int upgradeOneButtonId(AugmentTier tier) {
        return BUTTON_UPGRADE_ONE + tier.ordinal();
    }

    public static int upgradeAllButtonId(AugmentTier tier) {
        return BUTTON_UPGRADE_ALL + tier.ordinal();
    }
}
