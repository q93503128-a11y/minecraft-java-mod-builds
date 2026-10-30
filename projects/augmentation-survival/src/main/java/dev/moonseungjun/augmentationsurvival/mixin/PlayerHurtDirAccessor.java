package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the player-only damage tilt yaw that backs LivingEntity#getHurtDir in 26.x. */
@Mixin(Player.class)
public interface PlayerHurtDirAccessor {
    @Accessor("hurtDir")
    void augmentationSurvival$setHurtDir(float hurtDir);
}
