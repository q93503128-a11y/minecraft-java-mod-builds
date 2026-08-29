package kr.moonseungjun.titanbreak.mixin;

import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMovementTimeScaleMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 titanbreak$scaleMovement(Vec3 movement) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide()) return movement;

        double scale = ReflexFieldService.movementScale(entity);
        if (scale >= 0.999D) return movement;
        return movement.scale(Math.max(0.0D, scale));
    }
}
