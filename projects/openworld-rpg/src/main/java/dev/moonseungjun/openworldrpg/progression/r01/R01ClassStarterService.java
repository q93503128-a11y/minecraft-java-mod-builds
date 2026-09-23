package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.EquippedCombatItem;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative first root-class selection plus its one-time starter weapon projection. */
public final class R01ClassStarterService {
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
     * Repairs an interrupted first-class grant after reconnect without granting another sellable
     * copy. This is safe because equipped combat-state projection is slot-idempotent.
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

        if (starter.weaponFamily().orElseThrow().usesBothHands()) {
            PlayerEquipmentService.unequip(player, ProjectEquipmentSlot.OFF_HAND);
        }
        PlayerEquipmentService.equip(player, starter);
    }
}
