package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses inherited anvil input/output/player state without widening vanilla fields. */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
    @Accessor("player")
    Player augmentationSurvival$getPlayer();

    @Accessor("inputSlots")
    Container augmentationSurvival$getInputSlots();

    @Accessor("resultSlots")
    ResultContainer augmentationSurvival$getResultSlots();
}
