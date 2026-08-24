package kr.moonseungjun.frontiersettlement.content;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;

/**
 * Frontier military body that preserves the proven Iron Golem combat/goal implementation while
 * owning a distinct entity type so the client can present it as a human soldier. It deliberately
 * carries no server-side weapon ItemStack; visible equipment is presentation-only in the renderer.
 */
public final class FrontierSoldierEntity extends IronGolem {
    public FrontierSoldierEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
    }
}
