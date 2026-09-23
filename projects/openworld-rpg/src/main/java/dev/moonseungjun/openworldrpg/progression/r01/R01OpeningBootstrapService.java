package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.recovery.RecoveryBeltService;
import dev.moonseungjun.openworldrpg.recovery.RecoveryConsumable;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Idempotent first-character R01 opening grant.
 *
 * <p>This projects only already-closed server authority: combat equipment state, Gold and one loaded
 * Healing Potion. Physical inventory/model presentation remains owned by the later item/asset
 * binding and is not faked here.</p>
 */
public final class R01OpeningBootstrapService {
    public static final long STARTING_GOLD = 150L;
    public static final String OPENING_GOLD_TRANSACTION =
            "openworld_rpg:r01/opening_loadout/gold";
    public static final String OPENING_HEALING_DOSE_TRANSACTION =
            "openworld_rpg:r01/opening_loadout/recovery/healing_potion";

    private R01OpeningBootstrapService() {
    }

    public static boolean ensureOpeningLoadout(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState state = R01PlayerStateService.state(player);
        if (state.openingLoadoutClaimed()) {
            return false;
        }

        PlayerCurrencyService.creditOnce(
                player,
                OPENING_GOLD_TRANSACTION,
                STARTING_GOLD
        );
        PlayerEquipmentService.equip(player, R01StarterEquipment.openingSword());
        PlayerEquipmentService.equip(player, R01StarterEquipment.openingBuckler());
        RecoveryBeltService.loadCommittedReserveDoseOnce(
                player,
                OPENING_HEALING_DOSE_TRANSACTION,
                RecoveryConsumable.HEALING_POTION
        );

        R01PlayerStateService.markOpeningLoadoutClaimed(player);
        return true;
    }
}
