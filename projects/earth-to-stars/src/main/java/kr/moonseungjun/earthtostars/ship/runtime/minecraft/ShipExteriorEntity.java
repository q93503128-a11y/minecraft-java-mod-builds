package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.content.EarthToStarsItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The starter craft's visible hull, interaction target and actual passenger vehicle.
 * Persistent authority remains in ShipState/SavedData; this runtime exterior is recreated as needed.
 */
public final class ShipExteriorEntity extends Display.ItemDisplay {
    public ShipExteriorEntity(EntityType<? extends ShipExteriorEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public void setVisualItem(Item item) {
        var slot = getSlot(0);
        if (slot == null || !slot.set(new ItemStack(item))) {
            throw new IllegalStateException("ship exterior item slot is unavailable");
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(EarthToStarsItems.PROPELLANT_CELL.get())) {
            return ShipRuntimeManager.serviceSupply(
                    serverPlayer, this, hand, ShipSystemsManager.SupplyType.PROPELLANT);
        }
        if (held.is(EarthToStarsItems.OXYGEN_CARTRIDGE.get())) {
            return ShipRuntimeManager.serviceSupply(
                    serverPlayer, this, hand, ShipSystemsManager.SupplyType.OXYGEN);
        }
        if (player.isShiftKeyDown()) {
            return ShipRuntimeManager.retireCraft(serverPlayer, this)
                    ? InteractionResult.SUCCESS_SERVER
                    : InteractionResult.FAIL;
        }
        return ShipRuntimeManager.boardAndControl(serverPlayer, this)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return false;
    }
}
