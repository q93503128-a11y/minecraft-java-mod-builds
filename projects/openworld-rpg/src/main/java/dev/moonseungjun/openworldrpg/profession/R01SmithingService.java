package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentAffixCatalogLoader;
import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentMaterializer;
import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentParameterizedAffixLoader;
import dev.moonseungjun.openworldrpg.equipment.R01OrdinaryEquipmentBaseCatalogLoader;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.progression.r01.R01RiverbankRemediesService;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative Daren Holt ordinary-forge transaction.
 *
 * <p>Only the twelve canon-closed Refined/Superior R01 recipes live here. Reforge and signature
 * Mythic crafting remain separate transactions and are not approximated by this service.</p>
 */
public final class R01SmithingService {
    private static final String MATERIAL_RECEIPT_PREFIX =
            "openworld_rpg:smith_material/";
    private static final String FIRST_REFINED_INSIGHT =
            "openworld_rpg:profession_insight/smithing/first_refined";
    private static final String SECOND_DISTINCT_REFINED_INSIGHT =
            "openworld_rpg:profession_insight/smithing/second_distinct_refined";
    private static final String FIRST_SUPERIOR_INSIGHT =
            "openworld_rpg:profession_insight/smithing/first_superior";

    private R01SmithingService() {
    }

    public static R01SmithingState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01SmithingAttachments.STATE,
                R01SmithingState.initial()
        );
    }

    public static CraftResult craftSettlement(
            ServerPlayer player,
            R01SmithingRecipe recipe
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(recipe, "recipe");

        reconcilePending(player);
        reconcileSuperiorRecipeUnlock(player);
        if (state(player).pending().isPresent()) {
            return new CraftResult(
                    CraftStatus.RECOVERY_WAITING,
                    state(player).pending()
                            .map(R01SmithingState.PendingSmith::transactionId)
                            .orElse(null)
            );
        }

        if (R01AlderfordRuntimeBindingRegistry.productionService(
                R01SmithingRecipe.FORGE_SERVICE_ID
        ).isEmpty()) {
            return new CraftResult(
                    CraftStatus.SERVICE_NOT_PRODUCTION,
                    null
            );
        }

        if (recipe.requiresVerdantDiscovery()
                && !state(player).superiorRecipesUnlocked()) {
            return new CraftResult(
                    CraftStatus.RECIPE_LOCKED,
                    null
            );
        }

        if (!PlayerInventoryService.canConsumeMaterials(
                player,
                recipe.materialCosts(),
                true,
                R01RiverbankRemediesService.protectedPouchCounts(player)
        )) {
            return new CraftResult(
                    CraftStatus.INSUFFICIENT_MATERIALS,
                    null
            );
        }
        if (PlayerCurrencyService.state(player).gold()
                < recipe.goldCost()) {
            return new CraftResult(
                    CraftStatus.INSUFFICIENT_GOLD,
                    null
            );
        }

        ProjectInventoryItem output = materialize(
                recipe,
                player.getRandom().nextLong()
        );
        if (!PlayerInventoryService.canAcceptBackpack(
                player,
                output
        )) {
            return new CraftResult(
                    CraftStatus.INVENTORY_FULL,
                    null
            );
        }

        R01SmithingState.BeginResult begin = state(player).begin(
                player.getUUID().toString(),
                recipe,
                output
        );
        if (!begin.created()) {
            throw new IllegalStateException(
                    "R01 smithing retained an unreconciled pending transaction."
            );
        }
        replaceState(player, begin.state());
        return executePending(player, begin.craft(), false);
    }

    public static void recordVerdantCrystalEncounter(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        replaceState(
                player,
                state(player).unlockSuperiorRecipes()
        );
    }

    public static void reconcileSuperiorRecipeUnlock(
            ServerPlayer player
    ) {
        Objects.requireNonNull(player, "player");
        if (state(player).superiorRecipesUnlocked()) {
            return;
        }

        boolean gatheringDiscovery =
                R01GatheringService.state(player)
                        .discoveryFlags()
                        .contains(
                                R01GatheringRules.discoveryFlag(
                                        R01GatheringRules.VERDANT_CRYSTAL
                                )
                        );
        PlayerInventoryState inventory =
                PlayerInventoryService.state(player);
        boolean currentlyOwned =
                inventory.materialPouch()
                                .getOrDefault(
                                        R01GatheringRules.VERDANT_CRYSTAL,
                                        0
                                )
                        + inventory.materialVault()
                                .getOrDefault(
                                        R01GatheringRules.VERDANT_CRYSTAL,
                                        0
                                )
                        > 0;
        if (gatheringDiscovery || currentlyOwned) {
            recordVerdantCrystalEncounter(player);
        }
    }

    public static void reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        reconcileSuperiorRecipeUnlock(player);
        R01SmithingState.PendingSmith pending =
                state(player).pending().orElse(null);
        if (pending == null) {
            PlayerInventoryService.clearCompletedDeliveryIdsWithPrefix(
                    player,
                    MATERIAL_RECEIPT_PREFIX
            );
            return;
        }
        executePending(player, pending, true);
    }

    static ProjectInventoryItem materialize(
            R01SmithingRecipe recipe,
            long seed
    ) {
        Objects.requireNonNull(recipe, "recipe");
        var bases = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();
        var staticCatalog =
                OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var parameterized =
                OrdinaryEquipmentParameterizedAffixLoader.loadBundled();

        Set<String> blockers = bases.runtimeBlockers(
                recipe.baseId(),
                staticCatalog,
                parameterized
        );
        if (!blockers.isEmpty()) {
            throw new IllegalStateException(
                    "R01 smithing base has runtime affix blockers: "
                            + blockers
            );
        }

        var eligible = bases.eligibleAffixes(
                recipe.baseId(),
                staticCatalog,
                parameterized
        );
        boolean fixedEligible = eligible.stream()
                .anyMatch(value ->
                        value.id().equals(recipe.fixedAffixId())
                );
        if (!fixedEligible) {
            throw new IllegalStateException(
                    "R01 smithing fixed affix is not eligible for base: "
                            + recipe.id()
            );
        }

        var profile = bases.materializerProfile(
                recipe.baseId(),
                recipe.resolvedSlot()
        );
        return OrdinaryEquipmentMaterializer.materialize(
                new OrdinaryEquipmentMaterializer.MaterializationRequest(
                        profile,
                        recipe.grade(),
                        recipe.itemLevel(),
                        eligible,
                        seed,
                        recipe.unitSellValue(),
                        List.of(recipe.fixedAffixId())
                ),
                staticCatalog
        ).inventoryItem();
    }

    private static CraftResult executePending(
            ServerPlayer player,
            R01SmithingState.PendingSmith pending,
            boolean recovery
    ) {
        R01SmithingRecipe recipe = R01SmithingRecipe.byId(
                pending.recipeId()
        ).orElseThrow(() -> new IllegalStateException(
                "Unknown persisted R01 smithing recipe: "
                        + pending.recipeId()
        ));

        String goldTransactionId =
                pending.transactionId() + "/gold";
        String materialTransactionId =
                materialTransactionId(pending.transactionId());
        String outputTransactionId =
                pending.transactionId() + "/output";

        boolean goldDone = pending.goldCost() == 0L
                || PlayerCurrencyService.state(player)
                        .hasAppliedDebit(goldTransactionId);
        boolean materialsDone = PlayerInventoryService.state(player)
                .deliveryCompleted(materialTransactionId);

        if (!goldDone && !materialsDone) {
            if (PlayerCurrencyService.state(player).gold()
                    < pending.goldCost()) {
                replaceState(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
                return new CraftResult(
                        CraftStatus.INSUFFICIENT_GOLD,
                        null
                );
            }
            if (!PlayerInventoryService.canConsumeMaterials(
                    player,
                    recipe.materialCosts(),
                    true,
                    R01RiverbankRemediesService
                            .protectedPouchCounts(player)
            )) {
                replaceState(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
                return new CraftResult(
                        CraftStatus.INSUFFICIENT_MATERIALS,
                        null
                );
            }
        }

        if (!goldDone && pending.goldCost() > 0L) {
            var debit = PlayerCurrencyService.debitOnce(
                    player,
                    goldTransactionId,
                    pending.goldCost()
            );
            if (!debit.success()) {
                if (materialsDone) {
                    throw new IllegalStateException(
                            "Smithing materials committed before unavailable Gold debit."
                    );
                }
                replaceState(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
                return new CraftResult(
                        CraftStatus.INSUFFICIENT_GOLD,
                        null
                );
            }
        }

        PlayerInventoryState.MaterialsConsumeOnceResult materialResult =
                PlayerInventoryService.consumeMaterialsOnce(
                        player,
                        materialTransactionId,
                        recipe.materialCosts(),
                        true,
                        R01RiverbankRemediesService
                                .protectedPouchCounts(player)
                );
        if (!materialResult.consumed()) {
            return new CraftResult(
                    CraftStatus.RECOVERY_WAITING,
                    pending.transactionId()
            );
        }

        boolean pendingClaim = false;
        PlayerInventoryState inventory =
                PlayerInventoryService.state(player);
        if (inventory.pendingReward(outputTransactionId).isPresent()) {
            var claim = PlayerInventoryService.claimPending(
                    player,
                    outputTransactionId
            );
            pendingClaim = claim.status()
                    == PlayerInventoryState.DeliveryStatus.STILL_PENDING;
        } else if (!inventory.deliveryCompleted(outputTransactionId)) {
            var backpack = PlayerInventoryService.deliverBackpackOnce(
                    player,
                    outputTransactionId,
                    pending.output()
            );
            if (!backpack.delivered()) {
                var fallback =
                        PlayerInventoryService.deliverImportantOnce(
                                player,
                                outputTransactionId,
                                pending.output()
                        );
                pendingClaim = fallback.status()
                        == PlayerInventoryState.DeliveryStatus.PENDING
                        || fallback.status()
                        == PlayerInventoryState.DeliveryStatus.STILL_PENDING;
            }
        }

        awardSmithingInsight(player, recipe);
        replaceState(
                player,
                state(player).clearPending(
                        pending.transactionId()
                )
        );
        PlayerInventoryService.forgetCompletedDeliveryReceipt(
                player,
                materialTransactionId
        );
        if (PlayerInventoryService.state(player)
                .pendingReward(outputTransactionId).isEmpty()) {
            PlayerInventoryService.forgetCompletedDeliveryReceipt(
                    player,
                    outputTransactionId
            );
        }

        return new CraftResult(
                pendingClaim
                        ? CraftStatus.CRAFTED_PENDING_CLAIM
                        : recovery
                                ? CraftStatus.CRAFTED_RECOVERED
                                : CraftStatus.CRAFTED,
                pending.transactionId()
        );
    }

    private static void awardSmithingInsight(
            ServerPlayer player,
            R01SmithingRecipe recipe
    ) {
        ProfessionMasteryState mastery =
                R01CraftingService.masteryState(player);

        if (recipe.grade()
                == dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade
                        .REFINED) {
            R01SmithingState current = state(player);
            int distinctBefore = current.refinedBaseCrafts().size();
            R01SmithingState next =
                    current.recordRefinedBaseCraft(
                            recipe.baseId()
                    );
            boolean newDistinct = !current.equals(next);
            if (newDistinct) {
                replaceState(player, next);
                if (distinctBefore == 0) {
                    mastery = mastery.awardInsightOnce(
                            ProfessionMasteryState.Profession.SMITHING,
                            FIRST_REFINED_INSIGHT
                    );
                } else if (distinctBefore == 1) {
                    mastery = mastery.awardInsightOnce(
                            ProfessionMasteryState.Profession.SMITHING,
                            SECOND_DISTINCT_REFINED_INSIGHT
                    );
                }
            }
        } else if (recipe.grade()
                == dev.moonseungjun.openworldrpg.inventory.ProjectItemGrade
                        .SUPERIOR) {
            mastery = mastery.awardInsightOnce(
                    ProfessionMasteryState.Profession.SMITHING,
                    FIRST_SUPERIOR_INSIGHT
            );
        }

        if (!R01CraftingService.masteryState(player).equals(mastery)) {
            player.setAttached(
                    ProfessionMasteryAttachments.STATE,
                    mastery
            );
        }
    }

    private static String materialTransactionId(
            String smithTransactionId
    ) {
        return MATERIAL_RECEIPT_PREFIX
                + smithTransactionId.substring(
                        smithTransactionId.indexOf(':') + 1
                );
    }

    private static R01SmithingState replaceState(
            ServerPlayer player,
            R01SmithingState next
    ) {
        R01SmithingState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(
                    R01SmithingAttachments.STATE,
                    next
            );
        }
        return next;
    }

    public enum CraftStatus {
        CRAFTED,
        CRAFTED_RECOVERED,
        CRAFTED_PENDING_CLAIM,
        SERVICE_NOT_PRODUCTION,
        RECIPE_LOCKED,
        INSUFFICIENT_GOLD,
        INSUFFICIENT_MATERIALS,
        INVENTORY_FULL,
        RECOVERY_WAITING;

        public boolean successful() {
            return this == CRAFTED
                    || this == CRAFTED_RECOVERED
                    || this == CRAFTED_PENDING_CLAIM;
        }
    }

    public record CraftResult(
            CraftStatus status,
            String transactionId
    ) {
        public CraftResult {
            Objects.requireNonNull(status, "status");
            if (status.successful()
                    && (transactionId == null
                            || transactionId.isBlank())) {
                throw new IllegalArgumentException(
                        "Successful smithing result requires transaction id."
                );
            }
        }
    }
}
