package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.ClientPhaseState;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mirrors server Phase Shift collision rules for the local player's client prediction. */
@Mixin(Entity.class)
abstract class ClientPhaseShiftCollisionMixin {
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$clientPhaseCollision(
            Vec3 movement,
            CallbackInfoReturnable<Vec3> cir
    ) {
        Entity self = (Entity) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientPhaseState.phaseShiftActive() || minecraft.player == null || self != minecraft.player) {
            return;
        }
        double y = movement.y;
        if (y < 0.0D) {
            Vec3 vertical = Entity.collideBoundingBox(
                    self,
                    new Vec3(0.0D, y, 0.0D),
                    self.getBoundingBox(),
                    self.level(),
                    List.of()
            );
            y = vertical.y;
        }
        cir.setReturnValue(new Vec3(movement.x, y, movement.z));
    }
}
