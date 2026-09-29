package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineProjectSkillAccess;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces donor active-spell resolution with the server-published project class skill slots.
 */
@Mixin(
        targets = "net.spell_engine.internals.container.SpellContainerSource",
        remap = false
)
public abstract class SpellEngineProjectSkillExposureMixin {
    @Inject(
            method = "update",
            at = @At("TAIL"),
            remap = false
    )
    private static void openworldRpg$publishProjectClassSkills(
            @Coerce Object player,
            CallbackInfo ci
    ) {
        if (player instanceof Player actualPlayer) {
            SpellEngineProjectSkillAccess.rewriteActiveSpells(
                    actualPlayer
            );
        }
    }
}
