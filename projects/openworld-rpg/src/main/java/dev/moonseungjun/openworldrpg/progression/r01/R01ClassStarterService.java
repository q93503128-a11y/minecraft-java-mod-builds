package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentLoadoutState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative first root-class selection plus its one-time starter equipment transfer. */
public final class R01ClassStarterService {
    private static final String STASH_OPENING_SWORD_TRANSACTION =
            "openworld_rpg:r01/first_class/stash_heartland_arming_sword";
    private static final String STASH_OPENING_BUCKLER_TRANSACTION =
            "openworld_rpg:r01/first_class/stash_watch_buckler";

    private R01ClassStarterService() {
    }

    public static R01PlayerState selectFirstRootClass(
            ServerPlayer player,
            RootClass rootClass
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rootClass, "rootClass");

        R01PlayerState state = R01PlayerStateService.state(player);
        if (state.opening().firstRootClassSelected()) {
            throw new IllegalStateException("First root class has already been selected.");
        }

        PlayerProgressionService.selectClass(player, rootClass);
        applyStarterEquipment(player, rootClass);
        R01PlayerStateService.markFirstRootClassSelected(player);
        return R01PlayerStateService.markStarterPackageClaimed(player);
    }

    /**
     * Repairs an interrupted first-class transfer on reconnect.
     *
     * <p>Displaced starter gear is delivered through Backpack → Personal Storage → Pending Reward,
     * so a disconnect cannot silently delete the opening Sword/Buckler or create a sellable copy.</p>
     */
    public static boolean reconcileInterruptedGrant(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState state = R01PlayerStateService.state(player);
        if (state.opening().starterPackageClaimed()) {
            return false;
        }

        RootClass rootClass = PlayerProgressionService.state(player)
                .activeClass()
                .orElse(null);
        if (rootClass == null) {
            return false;
        }

        applyStarterEquipment(player, rootClass);
        if (!state.opening().firstRootClassSelected()) {
            R01PlayerStateService.markFirstRootClassSelected(player);
        }
        R01PlayerStateService.markStarterPackageClaimed(player);
        return true;
    }

    private static void applyStarterEquipment(
            ServerPlayer player,
            RootClass rootClass
    ) {
        EquippedCombatItem starter = R01StarterEquipment.firstClassWeapon(rootClass)
                .orElse(null);
        if (starter == null) {
            return;
        }

        PlayerEquipmentLoadoutState loadout = PlayerEquipmentService.state(player);
        EquippedCombatItem currentMain = loadout.item(ProjectEquipmentSlot.MAIN_WEAPON)
                .orElse(null);
        if (currentMain != null && !currentMain.itemId().equals(starter.itemId())) {
            stashKnownOpeningItem(
                    player,
                    currentMain,
                    "openworld_rpg:heartland_arming_sword",
                    STASH_OPENING_SWORD_TRANSACTION
            );
            PlayerEquipmentService.unequip(player, ProjectEquipmentSlot.MAIN_WEAPON);
        }

        if (starter.weaponFamily().orElseThrow().usesBothHands()) {
            EquippedCombatItem currentOffhand = PlayerEquipmentService.state(player)
                    .item(ProjectEquipmentSlot.OFF_HAND)
                    .orElse(null);
            if (currentOffhand != null) {
                stashKnownOpeningItem(
                        player,
                        currentOffhand,
                        "openworld_rpg:watch_buckler",
                        STASH_OPENING_BUCKLER_TRANSACTION
                );
                PlayerEquipmentService.unequip(player, ProjectEquipmentSlot.OFF_HAND);
            }
        }

        PlayerEquipmentService.equip(player, starter);
    }

    private static void stashKnownOpeningItem(
            ServerPlayer player,
            EquippedCombatItem item,
            String expectedItemId,
            String transactionId
    ) {
        if (!item.itemId().equals(expectedItemId)) {
            throw new IllegalStateException(
                    "First-class starter transfer found unexpected equipped item: "
                            + item.itemId()
            );
        }

        PlayerInventoryService.deliverImportantOnce(
                player,
                transactionId,
                ProjectInventoryItem.starterEquipment(item)
        );
    }
}
