package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Whitelist-only non-combat gathering bonus for Dexterity. */
public final class DexterityService {
    private static final String DEXTERITY_ID = AugmentationSurvival.MOD_ID + ":dexterity";
    private static final TagKey<Block> GATHERING_WHITELIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "dexterity_gathering")
    );

    private DexterityService() {
    }

    public static void initialize() {
        AugmentCatalog.require(DEXTERITY_ID);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!state.is(GATHERING_WHITELIST)) {
                return;
            }
            AugmentationPlayerState augments = AugmentationPlayerStateService.state(serverPlayer);
            int augmentLevel = boundedLevel(augments);
            if (augmentLevel <= 0) {
                return;
            }
            double chance = FinalAugmentTuning.current().dexterityBonusChancePerLevel() * augmentLevel;
            if (serverPlayer.getRandom().nextDouble() >= chance) {
                return;
            }
            List<ItemStack> drops = Block.getDrops(
                    state,
                    level,
                    pos,
                    blockEntity,
                    serverPlayer,
                    serverPlayer.getMainHandItem()
            );
            for (ItemStack stack : drops) {
                if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                    ItemStack bonus = stack.copy();
                    bonus.setCount(1);
                    Block.popResource(level, pos, bonus);
                    return;
                }
            }
        });
    }

    private static int boundedLevel(AugmentationPlayerState state) {
        int level = state.level(DEXTERITY_ID);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(DEXTERITY_ID).maxLevel());
    }
}
