package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.AbundanceService;
import dev.moonseungjun.augmentationsurvival.augment.resource.DexterityService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final direct-break loot boundary for Abundance plus the spawned-resource boundary for Dexterity. */
@Mixin(Block.class)
abstract class BlockAbundanceMixin {
    @Redirect(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;"
            )
    )
    private static List<ItemStack> augmentationSurvival$duplicateDirectBreakerDrops(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemInstance tool
    ) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, breaker, tool);
        if (!(breaker instanceof ServerPlayer player)) return drops;
        return AbundanceService.applyDirectBlockDrops(player, drops);
    }

    @Inject(
            method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$augmentDirectBlockDrop(
            Level level,
            BlockPos pos,
            ItemStack stack,
            CallbackInfo ci
    ) {
        DexterityService.duplicateActualGatheringDrop(level, pos, stack);
    }
}
