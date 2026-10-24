package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.core.RaidCoreRewardService;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Narrow hook at the exact vanilla RAID_WIN criterion trigger inside raid victory processing. */
@Mixin(Raid.class)
public abstract class RaidMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancements/triggers/PlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;)V"
            )
    )
    private void augmentationSurvival$rewardRaidVictory(PlayerTrigger trigger, ServerPlayer player) {
        trigger.trigger(player);
        if (trigger == CriteriaTriggers.RAID_WIN) {
            RaidCoreRewardService.reward(player);
        }
    }
}
