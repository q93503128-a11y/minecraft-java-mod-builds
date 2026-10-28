package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.movement.TerrainResistanceService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets vanilla movement efficiency mitigate the cobweb stuck vector on every axis. */
@Mixin(WebBlock.class)
public abstract class WebBlockTerrainResistanceMixin {
    @Redirect(
            method = "entityInside",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void augmentationSurvival$applyTerrainResistance(
            Entity entity,
            BlockState state,
            Vec3 vanillaMultiplier
    ) {
        Vec3 multiplier = entity instanceof Player player
                ? TerrainResistanceService.adjustCobwebMultiplier(player, vanillaMultiplier)
                : vanillaMultiplier;
        entity.makeStuckInBlock(state, multiplier);
    }
}
