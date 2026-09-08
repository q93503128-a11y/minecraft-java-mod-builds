package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipSystemsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class ShipSupplyItem extends Item {
    private final ShipSystemsManager.SupplyType supplyType;

    public ShipSupplyItem(Properties properties, ShipSystemsManager.SupplyType supplyType) {
        super(properties);
        this.supplyType = supplyType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }

        ShipSystemsManager.SupplyLoadResult result = ShipSystemsManager.loadSupply(player, supplyType);
        return switch (result) {
            case LOADED -> {
                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                player.displayClientMessage(Component.translatable(
                        supplyType == ShipSystemsManager.SupplyType.PROPELLANT
                                ? "message.earth_to_stars.supply.propellant_loaded"
                                : "message.earth_to_stars.supply.oxygen_loaded"
                ), true);
                yield InteractionResult.SUCCESS_SERVER;
            }
            case TANK_FULL -> {
                player.displayClientMessage(Component.translatable(
                        supplyType == ShipSystemsManager.SupplyType.PROPELLANT
                                ? "message.earth_to_stars.supply.propellant_full"
                                : "message.earth_to_stars.supply.oxygen_full"
                ), true);
                yield InteractionResult.FAIL;
            }
            case NO_ACCESSIBLE_SHIP -> {
                player.displayClientMessage(Component.translatable("message.earth_to_stars.supply.no_ship"), true);
                yield InteractionResult.FAIL;
            }
        };
    }
}
