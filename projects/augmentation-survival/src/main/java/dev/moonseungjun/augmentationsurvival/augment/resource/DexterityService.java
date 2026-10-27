package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.FinalAugmentTuning;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Whitelist-only non-combat gathering bonus for Dexterity. */
public final class DexterityService {
    private static final String DEXTERITY_ID = AugmentationSurvival.MOD_ID + ":dexterity";
    private static final TagKey<Block> GATHERING_WHITELIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "dexterity_gathering")
    );
    private static final ThreadLocal<Deque<BreakContext>> BREAK_CONTEXTS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private DexterityService() {
    }

    public static void initialize() {
        AugmentCatalog.require(DEXTERITY_ID);
    }

    /**
     * Captures the block before ServerPlayerGameMode destroys it and rolls Dexterity exactly once for
     * that break. A stack is used because compatible derived harvesting can nest destroyBlock calls.
     */
    public static void pushBreakContext(ServerPlayer player, BlockPos pos) {
        BlockState blockState = player.level().getBlockState(pos);
        boolean grantBonus = false;
        if (blockState.is(GATHERING_WHITELIST)) {
            AugmentationPlayerState augments = AugmentationPlayerStateService.state(player);
            int augmentLevel = boundedLevel(augments);
            if (augmentLevel > 0) {
                double chance = FinalAugmentTuning.current().dexterityBonusChancePerLevel() * augmentLevel;
                grantBonus = player.getRandom().nextDouble() < chance;
            }
        }
        BREAK_CONTEXTS.get().push(new BreakContext(player, pos.immutable(), grantBonus));
    }

    public static void popBreakContext() {
        Deque<BreakContext> contexts = BREAK_CONTEXTS.get();
        if (!contexts.isEmpty()) {
            contexts.pop();
        }
        if (contexts.isEmpty()) {
            BREAK_CONTEXTS.remove();
        }
    }

    /**
     * Called at the real Block.popResource boundary. This never re-rolls a loot table: if the one
     * Dexterity roll succeeded, exactly one copy of the first eligible stack that actually exists is
     * emitted. Because this runs before Abundance, the Mythic final-drop multiplier can correctly
     * multiply both the original drop and this compatible +1 addition.
     */
    public static void duplicateActualGatheringDrop(Level level, BlockPos pos, ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || !AbundanceService.eligibleOrdinaryDrop(stack)) {
            return;
        }
        Deque<BreakContext> contexts = BREAK_CONTEXTS.get();
        if (contexts.isEmpty()) {
            return;
        }
        BreakContext context = contexts.peek();
        if (!context.grantBonus
                || context.consumed
                || context.player.level() != serverLevel
                || !context.pos.equals(pos)) {
            return;
        }

        context.consumed = true;
        ItemStack bonus = stack.copy();
        bonus.setCount(1);
        Block.popResource(level, pos, bonus);
        PassiveProcReadabilityService.presentDexterityBonus(context.player, pos);
    }

    private static int boundedLevel(AugmentationPlayerState state) {
        int level = state.level(DEXTERITY_ID);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(DEXTERITY_ID).maxLevel());
    }

    private static final class BreakContext {
        private final ServerPlayer player;
        private final BlockPos pos;
        private final boolean grantBonus;
        private boolean consumed;

        private BreakContext(ServerPlayer player, BlockPos pos, boolean grantBonus) {
            this.player = player;
            this.pos = pos;
            this.grantBonus = grantBonus;
        }
    }
}
