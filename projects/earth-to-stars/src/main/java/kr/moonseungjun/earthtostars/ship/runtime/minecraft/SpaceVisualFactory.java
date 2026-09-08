package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.ship.runtime.ShipTransform;
import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class SpaceVisualFactory {
    private SpaceVisualFactory() {
    }

    static Display.ItemDisplay create(ServerLevel level, Item item, ShipVec3 position, float yaw, float pitch) {
        EntityType<?> itemDisplayType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item_display"));
        if (itemDisplayType == null) {
            throw new IllegalStateException("minecraft:item_display is not registered");
        }
        Display.ItemDisplay display = new Display.ItemDisplay(itemDisplayType, level);
        display.setNoGravity(true);
        display.getSlot(0).set(new ItemStack(item));
        display.setPos(position.x(), position.y(), position.z());
        display.setYRot(yaw);
        display.setXRot(pitch);
        return display;
    }

    static void apply(Display.ItemDisplay display, ShipTransform transform) {
        display.setDeltaMovement(0.0D, 0.0D, 0.0D);
        display.setPos(transform.position().x(), transform.position().y(), transform.position().z());
        display.setYRot((float) transform.yawDegrees());
        display.setXRot((float) transform.pitchDegrees());
    }

    static void apply(Display.ItemDisplay display, ShipVec3 position, float yaw, float pitch) {
        display.setDeltaMovement(0.0D, 0.0D, 0.0D);
        display.setPos(position.x(), position.y(), position.z());
        display.setYRot(yaw);
        display.setXRot(pitch);
    }

    static void discard(Display.ItemDisplay display) {
        if (display != null && !display.isRemoved()) {
            display.discard();
        }
    }
}
