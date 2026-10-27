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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
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

    private static final ThreadLocal<Deque<ServerPlayer>> MINING_ACTORS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private AbundanceService() {
    }

    public static void initialize() {
        AugmentCatalog.require(ABUNDANCE_ID);
        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> duplicateDirectMiningDrops(drops));
        AugmentationSurvival.LOGGER.info(
                "Loaded abundance contract: exact direct block, fishing and normal-mob final-drop duplication"
        );
    }

    /**
     * Tracks the server player whose direct destroyBlock call currently owns block-loot generation.
     * A stack is used because derived harvest helpers can nest destroyBlock calls on the same server thread.
     */
    public static void pushMiningActor(ServerPlayer player) {
        MINING_ACTORS.get().push(player);
    }

    public static void popMiningActor() {
        Deque<ServerPlayer> actors = MINING_ACTORS.get();
        if (!actors.isEmpty()) {
            actors.pop();
        }
        if (actors.isEmpty()) {
            MINING_ACTORS.remove();
        }
    }

    /**
     * Runs at Fabric's final runtime loot-list boundary. This duplicates the already-generated block stacks,
     * so Silk Touch, Fortune, data packs and external loot modifiers are preserved exactly instead of re-rolled.
     */
    private static void duplicateDirectMiningDrops(List<ItemStack> drops) {
        if (drops.isEmpty() || ResourceHarvestService.isDerivedBreak()) {
            return;
        }
        Deque<ServerPlayer> actors = MINING_ACTORS.get();
        if (actors.isEmpty()) {
            return;
        }
        ServerPlayer player = actors.peek();
        if (player == null || !hasAbundance(player)) {
            return;
        }
        int originalSize = drops.size();
        for (int index = 0; index < originalSize; index++) {
            ItemStack stack = drops.get(index);
            if (eligibleOrdinaryDrop(stack)) {
                drops.add(stack.copy());
            }
        }
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
