package kr.moonseungjun.titanbreak.mixin;

import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobAiTimeScaleMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void titanbreak$scaleServerAi(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!ReflexFieldService.shouldAdvanceMobAi(mob)) ci.cancel();
    }
}
