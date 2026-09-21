package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.integration.spellengine.ProjectSpellInputRules;
import java.lang.reflect.InvocationTargetException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Narrow client compatibility shim for the pinned Spell Engine 1.10.5 input contract.
 *
 * <p>Spell Engine intentionally allows a held hotbar key to start another cast after the previous
 * cast finishes. Arc Bolt is authored as one physical press -> one completed cast, so only this
 * project spell suppresses the donor's held-key replay. The donor's cast timeline, targeting,
 * animation, projectile delivery and networking remain untouched.</p>
 */
@Mixin(targets = "net.spell_engine.client.casting.ClientCastController", remap = false)
public abstract class SpellEngineSinglePressMixin {
    private static final String REACTION_CLASS =
            "net.spell_engine.client.casting.ClientCastController$Reaction";

    @Inject(method = "keyHeld", at = @At("HEAD"), cancellable = true, remap = false)
    private void openworldRpg$requireFreshPressForArcBolt(
            @Coerce Object option,
            boolean donorFreshForStart,
            boolean donorFreshForStop,
            CallbackInfoReturnable<Object> cir
    ) {
        /*
         * In pinned Spell Engine 1.10.5, SpellHotbar passes its START debounce state as the third
         * keyHeld argument. Once this physical hold has started a cast, that value stays false
         * until the key is released. The target method names the parameter freshForStop, so keep
         * this adapter comment tied to the exact admitted version rather than pretending it is a
         * stable public API.
         */
        String spellId = openworldRpg$optionId(option);
        if (!ProjectSpellInputRules.suppressHeldRepeat(spellId, donorFreshForStop)) {
            return;
        }
        cir.setReturnValue(openworldRpg$nothingReaction(option.getClass().getClassLoader()));
    }

    @Unique
    private static String openworldRpg$optionId(Object option) {
        try {
            return String.valueOf(option.getClass().getMethod("id").invoke(option));
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new IllegalStateException("Spell Engine option id accessor failed.", cause == null ? exception : cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Pinned Spell Engine option id contract is unavailable.", exception);
        }
    }

    @Unique
    private static Object openworldRpg$nothingReaction(ClassLoader loader) {
        try {
            Class<?> reaction = Class.forName(REACTION_CLASS, false, loader);
            return reaction.getField("NOTHING").get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Pinned Spell Engine no-op reaction contract is unavailable.", exception);
        }
    }
}
