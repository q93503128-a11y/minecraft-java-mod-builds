package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets phase-shift ignore horizontal/upward block collision while preserving downward collision. */
@Mixin(Entity.class)
abstract class PhaseShiftCollisionMixin {
    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$phaseCollision(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayer player) || !AdvancedWorldAugmentService.isPhaseShiftActive(player)) {
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
