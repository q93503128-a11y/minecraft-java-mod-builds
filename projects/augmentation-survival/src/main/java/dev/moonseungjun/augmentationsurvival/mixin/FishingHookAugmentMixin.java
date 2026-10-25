package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.AbundanceService;
import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.resource.TreasureFishingService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server-owned fishing timing, exact treasure chance and abundance scheduling. */
@Mixin(FishingHook.class)
public abstract class FishingHookAugmentMixin {
    @Shadow private int timeUntilLured;
    @Shadow private int nibble;
    @Shadow private boolean openWater;
    @Shadow @Final private int luck;

    @Unique private int augmentationSurvival$beforeLured;
    @Unique private double augmentationSurvival$lureCredit;
    @Unique private boolean augmentationSurvival$hadNibble;

    @Inject(method = "tick", at = @At("HEAD"))
    private void augmentationSurvival$captureLureTimer(CallbackInfo ci) {
        augmentationSurvival$beforeLured = timeUntilLured;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$accelerateLureTimer(CallbackInfo ci) {
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        if (!(owner instanceof ServerPlayer player)
                || augmentationSurvival$beforeLured <= 0
                || timeUntilLured < 0
                || timeUntilLured >= augmentationSurvival$beforeLured) {
            return;
        }
        double reduction = ConsumptionAugmentService.fishingWaitReduction(player);
        if (!(reduction > 0.0D) || reduction >= 0.95D) {
            return;
        }
        int vanillaProgress = augmentationSurvival$beforeLured - timeUntilLured;
        augmentationSurvival$lureCredit += vanillaProgress * reduction / (1.0D - reduction);
        int extra = (int) Math.floor(augmentationSurvival$lureCredit);
        if (extra <= 0) {
            return;
        }
        int applied = Math.min(extra, timeUntilLured);
        timeUntilLured -= applied;
        augmentationSurvival$lureCredit -= applied;
    }

    @Redirect(
            method = "retrieve",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
            )
    )
    private ObjectArrayList<ItemStack> augmentationSurvival$exactTreasureChance(
            LootTable rootFishingTable,
            LootParams params,
            ItemStack rod
    ) {
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        if (owner instanceof ServerPlayer player
                && TreasureFishingService.shouldForceTreasure(
                        player,
                        (float) this.luck + player.getLuck(),
                        this.openWater,
                        player.getRandom()
                )) {
            ServerLevel level = (ServerLevel) ((FishingHook) (Object) this).level();
            LootTable treasureTable = level.getServer()
                    .reloadableRegistries()
                    .getLootTable(BuiltInLootTables.FISHING_TREASURE);
            return treasureTable.getRandomItems(params);
        }
        return rootFishingTable.getRandomItems(params);
    }

    @Inject(method = "retrieve", at = @At("HEAD"))
    private void augmentationSurvival$captureFishingResult(
            ItemStack rod,
            CallbackInfoReturnable<Integer> cir
    ) {
        augmentationSurvival$hadNibble = nibble > 0;
    }

    @Inject(method = "retrieve", at = @At("TAIL"))
    private void augmentationSurvival$scheduleFishingAbundance(
            ItemStack rod,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!augmentationSurvival$hadNibble) {
            return;
        }
        Player owner = ((FishingHook) (Object) this).getPlayerOwner();
        if (owner instanceof ServerPlayer player) {
            AbundanceService.scheduleFishing(player, ((FishingHook) (Object) this).position());
        }
        augmentationSurvival$hadNibble = false;
    }
}
