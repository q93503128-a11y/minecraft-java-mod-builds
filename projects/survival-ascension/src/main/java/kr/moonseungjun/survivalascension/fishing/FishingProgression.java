package kr.moonseungjun.survivalascension.fishing;

import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

// Catch-driven skill entry point; kept isolated from unrelated expedition action counters.
public final class FishingProgression {
    private FishingProgression() {}

    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        int oldLevel = SkillProgressData.get(player).level(player, SkillType.FISHING);
        int rawXp = xpForCatch(event);
        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);
        applyBonusCatch(player, event, oldLevel);
        preserveRod(player, event, oldLevel);
        announceMilestones(player, result);
    }

    private static int xpForCatch(ItemFishedEvent event) {
        int xp = 8;
        for (ItemStack stack : event.getDrops()) {
            if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
                xp += 4;
            } else if (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.NAME_TAG) || stack.is(Items.NAUTILUS_SHELL)
                    || stack.is(Items.SADDLE) || stack.is(Items.BOW) || stack.is(Items.FISHING_ROD)) {
                xp += 12;
            } else {
                xp += 2;
            }
        }
        return Math.max(8, Math.min(32, xp));
    }

    private static void applyBonusCatch(ServerPlayer player, ItemFishedEvent event, int level) {
        double chance = SkillTuning.fishingBonusCatchChance(level);
        if (chance <= 0.0D) return;
        for (ItemStack stack : event.getDrops()) {
            if (!isFishCatch(stack)) continue;
            if (player.getRandom().nextDouble() < chance) stack.grow(1);
        }
    }

    private static boolean isFishCatch(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level) {
        int damage = event.getRodDamage();
        if (damage <= 0) return;
        double chance = SkillTuning.fishingRodPreservationChance(level);
        if (chance > 0.0D && player.getRandom().nextDouble() < chance) {
            event.damageRodBy(Math.max(0, damage - 1));
        }
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 10% · 물고기 추가 어획 10%"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 25% · 추가 어획 25% · 이후 레벨마다 두 효과 증가"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 45% · 추가 어획 50%"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 방지 65% · 추가 어획 75%"));
        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f마모 방지 80% · 물고기 추가 어획 100%"));
    }
}
