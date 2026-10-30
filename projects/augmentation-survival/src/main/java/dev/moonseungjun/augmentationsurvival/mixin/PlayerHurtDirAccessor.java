package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the player-owned damage-tilt yaw so delayed installments can restore it after ServerPlayer damage handling. */
@Mixin(Player.class)
public interface PlayerHurtDirAccessor {
    @Accessor("hurtDir")
    void augmentationSurvival$setHurtDir(float hurtDir);
}
