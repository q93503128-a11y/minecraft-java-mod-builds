package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.DexterityService;
import dev.moonseungjun.augmentationsurvival.augment.utility.EnchantmentResonanceService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeEnchantmentResonanceMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void augmentationSurvival$pushMiningOwner(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        DexterityService.pushBreakContext(this.player, pos);
        EnchantmentResonanceService.pushActor(this.player);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void augmentationSurvival$popMiningOwner(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        EnchantmentResonanceService.popActor();
        DexterityService.popBreakContext();
    }
}
