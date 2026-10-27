package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
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

/** Server-owned fishing timing, exact treasure chance and exact final-result abundance duplication. */
@Mixin(FishingHook.class)
public abstract class FishingHookAugmentMixin {
    @Shadow private int timeUntilLured;
    @Shadow private boolean openWater;
    @Shadow @Final private int luck;

    @Unique private int augmentationSurvival$beforeLured;
    @Unique private double augmentationSurvival$lureCredit;

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
    private ObjectArrayList<ItemStack> augmentationSurvival$exactTreasureAndAbundance(
            LootTable rootFishingTable,
            LootParams params,
            ItemStack rod
    ) {
        FishingHook hook = (FishingHook) (Object) this;
        Player owner = hook.getPlayerOwner();
        ServerPlayer player = owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        ObjectArrayList<ItemStack> result;
        if (player != null
                && TreasureFishingService.shouldForceTreasure(
                        player,
                        (float) this.luck + player.getLuck(),
                        this.openWater,
                        player.getRandom()
                )) {
            ServerLevel level = (ServerLevel) hook.level();
            LootTable treasureTable = level.getServer()
                    .reloadableRegistries()
                    .getLootTable(BuiltInLootTables.FISHING_TREASURE);
            result = treasureTable.getRandomItems(params);
            if (!result.isEmpty()) {
                PassiveProcReadabilityService.presentTreasureFishing(player, hook.position());
            }
        } else {
            result = rootFishingTable.getRandomItems(params);
        }
        return player == null ? result : AbundanceService.applyFishingResult(player, result);
    }
}
