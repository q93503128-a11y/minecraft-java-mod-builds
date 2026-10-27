package dev.moonseungjun.augmentationsurvival.augment.resource;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Exact final ordinary-drop multiplier for the Mythic Abundance augment. */
public final class AbundanceService {
    private static final String ABUNDANCE_ID = id("abundance");
    public static final TagKey<Item> ABUNDANCE_EXCLUDED = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "abundance_excluded")
    );

    private AbundanceService() {
    }

    public static void initialize() {
        AugmentCatalog.require(ABUNDANCE_ID);
        AugmentationSurvival.LOGGER.info(
                "Loaded abundance contract: exact direct block, fishing and normal-mob final-drop duplication"
        );
    }

    /**
     * Called from the exact Block.getDrops overload that already carries the direct breaker. The returned
     * stacks have already passed the block's loot-table/Silk/Fortune logic, so compatible results are copied
     * without any re-roll.
     */
    public static List<ItemStack> applyDirectBlockDrops(ServerPlayer player, List<ItemStack> drops) {
        if (player == null
                || drops == null
                || drops.isEmpty()
                || ResourceHarvestService.isDerivedBreak()
                || !hasAbundance(player)) {
            return drops;
        }
        List<ItemStack> result = new ArrayList<>(drops.size() * 2);
        result.addAll(drops);
        for (ItemStack stack : drops) {
            if (eligibleOrdinaryDrop(stack)) {
                result.add(stack.copy());
            }
        }
        return result;
    }

    /** Multiplies only the exact final fishing loot-list entries. Loot tables are not re-rolled. */
    public static ObjectArrayList<ItemStack> applyFishingResult(
            ServerPlayer player,
            ObjectArrayList<ItemStack> drops
    ) {
        if (!hasAbundance(player) || drops.isEmpty()) {
            return drops;
        }
        int originalSize = drops.size();
        for (int index = 0; index < originalSize; index++) {
            ItemStack stack = drops.get(index);
            if (eligibleOrdinaryDrop(stack)) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    /**
     * Wraps only a directly killed normal mob's final loot-table consumer. This shares the same exact
     * provenance boundary as Loot Scoop, so nearby entities/items and equipment/custom death drops are
     * never captured by position.
     */
    public static Consumer<ItemStack> wrapDirectMobLoot(
            LivingEntity victim,
            DamageSource source,
            boolean playerKilled,
            Consumer<ItemStack> downstream
    ) {
        if (!playerKilled
                || victim instanceof Player
                || CompanionAugmentService.isDominated(victim)
                || CombatExpansionService.isConfiguredBoss(victim)
                || !(source.getEntity() instanceof ServerPlayer player)
                || !CombatProvenanceContext.resolve(source).isRootDirect()
                || !hasAbundance(player)) {
            return downstream;
        }

        return stack -> {
            downstream.accept(stack);
            if (eligibleOrdinaryDrop(stack)) {
                downstream.accept(stack.copy());
            }
        };
    }

    public static boolean eligibleOrdinaryDrop(ItemStack stack) {
        if (stack == null
                || stack.isEmpty()
                || stack.getMaxStackSize() <= 1
                || stack.isDamageableItem()
                || stack.is(ABUNDANCE_EXCLUDED)
                || stack.get(DataComponents.CUSTOM_DATA) != null
                || stack.get(DataComponents.CUSTOM_NAME) != null
                || stack.get(DataComponents.LORE) != null
                || isCore(stack)) {
            return false;
        }
        return true;
    }

    private static boolean isCore(ItemStack stack) {
        for (AugmentTier tier : AugmentTier.values()) {
            if (stack.is(AugmentCoreItems.item(tier))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAbundance(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = Math.max(0, Math.min(
                state.level(ABUNDANCE_ID),
                AugmentCatalog.require(ABUNDANCE_ID).maxLevel()
        ));
        return level > 0;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
