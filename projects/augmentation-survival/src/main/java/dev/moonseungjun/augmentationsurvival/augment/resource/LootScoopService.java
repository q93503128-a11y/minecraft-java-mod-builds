package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Exact death-loot provenance boundary for Loot Scoop. */
public final class LootScoopService {
    private static final String LOOT_SCOOP_ID = AugmentationSurvival.MOD_ID + ":loot_scoop";

    private LootScoopService() {
    }

    /**
     * Wraps only the victim's final entity-loot-table consumer. One roll controls the whole bundle,
     * so unrelated nearby drops, equipment drops, custom death drops and augment cores are never
     * copied accidentally.
     */
    public static Consumer<ItemStack> wrapDeathLoot(
            LivingEntity victim,
            DamageSource source,
            boolean playerKilled,
            Consumer<ItemStack> downstream
    ) {
        if (!playerKilled
                || victim instanceof Player
                || CompanionAugmentService.isDominated(victim)
                || CombatExpansionService.isConfiguredBoss(victim)
                || !(source.getEntity() instanceof ServerPlayer player)) {
            return downstream;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = Math.max(0, Math.min(
                state.level(LOOT_SCOOP_ID),
                AugmentCatalog.require(LOOT_SCOOP_ID).maxLevel()
        ));
        if (level <= 0
                || player.getRandom().nextDouble()
                >= ResourceHarvestTuning.current().lootScoopChancePerLevel() * level) {
            return downstream;
        }

        boolean[] presented = {false};
        return stack -> {
            downstream.accept(stack);
            if (!stack.isEmpty() && !isCore(stack)) {
                downstream.accept(stack.copy());
                if (!presented[0]) {
                    presented[0] = true;
                    PassiveProcReadabilityService.presentLootScoop(player, victim);
                }
            }
        };
    }

    private static boolean isCore(ItemStack stack) {
        for (AugmentTier tier : AugmentTier.values()) {
            if (stack.is(AugmentCoreItems.item(tier))) {
                return true;
            }
        }
        return false;
    }
}
