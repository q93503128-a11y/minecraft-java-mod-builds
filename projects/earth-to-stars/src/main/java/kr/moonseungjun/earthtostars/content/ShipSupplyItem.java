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

    public ShipSystemsManager.SupplyType supplyType() {
        return supplyType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.earth_to_stars.supply.use_on_ship"), true);
        }
        return InteractionResult.FAIL;
    }
}
