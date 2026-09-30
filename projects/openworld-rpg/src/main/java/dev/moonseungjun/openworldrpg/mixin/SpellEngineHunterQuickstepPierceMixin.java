package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineHunterQuickstepProjectileBridge;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.spell_engine.entity.SpellProjectile", remap = false)
public abstract class SpellEngineHunterQuickstepPierceMixin {
    @Unique
    private boolean openworldRpg$quickstepPierceSpent;

    @Inject(method = "onHitEntity", at = @At("HEAD"), remap = false)
    private void openworldRpg$prepareQuickstepPierce(
            EntityHitResult hit,
            CallbackInfo ci
    ) {
        if (SpellEngineHunterQuickstepProjectileBridge.preparePierce(
                (Object) this,
                hit.getEntity(),
                openworldRpg$quickstepPierceSpent
        )) {
            openworldRpg$quickstepPierceSpent = true;
        }
    }
}
