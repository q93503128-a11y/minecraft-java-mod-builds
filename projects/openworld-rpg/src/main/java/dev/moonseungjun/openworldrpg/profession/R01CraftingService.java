package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-owned R01 settlement alchemy/cooking transaction authority.
 *
 * <p>It intentionally contains no screen or spatial substitute. Live calls fail closed until the
 * corresponding Alderford service has a production spatial binding.</p>
 */
public final class R01CraftingService {
    private static final String MATERIAL_RECEIPT_PREFIX =
            "openworld_rpg:craft_material/";

    private R01CraftingService() {
    }

    public static R01CraftingState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01CraftingAttachments.STATE,
                R01CraftingState.initial()
        );
    }

    public static ProfessionMasteryState masteryState(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                ProfessionMasteryAttachments.STATE,
                ProfessionMasteryState.initial()
        );
    }

    public static CraftResult craftSettlement(
            ServerPlayer player,
            R01CraftingRecipe recipe,
            int quantity
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(recipe, "recipe");
        if (quantity <= 0) {
            return new CraftResult(
                    CraftStatus.INVALID_QUANTITY,
                    0,
                    Optional.empty()
            );
        }

        reconcilePending(player);
        if (state(player).pending().isPresent()) {
            return new CraftResult(
                    CraftStatus.RECOVERY_WAITING_FOR_MATERIALS,
                    0,
                    Optional.of(
                            state(player).pending().orElseThrow().transactionId()
                    )
            );
        }

        if (R01AlderfordRuntimeBindingRegistry.productionService(
                recipe.serviceId()
        ).isEmpty()) {
            return new CraftResult(
                    CraftStatus.SERVICE_NOT_PRODUCTION,
                    0,
                    Optional.empty()
            );
        }

        Map<String, Integer> costs;
        long goldCost;
        try {
            costs = recipe.materialCostsFor(quantity);
            goldCost = recipe.goldCostFor(quantity);
        } catch (ArithmeticException exception) {
            return new CraftResult(
                    CraftStatus.INVALID_QUANTITY,
                    0,
                    Optional.empty()
            );
        }

        if (!PlayerInventoryService.canConsumeMaterials(
                player,
                costs,
                true
        )) {
            return new CraftResult(
                    CraftStatus.INSUFFICIENT_MATERIALS,
                    0,
                    Optional.empty()
            );
        }
        if (goldCost > 0L
                && PlayerCurrencyService.state(player).gold() < goldCost) {
            return new CraftResult(
                    CraftStatus.INSUFFICIENT_GOLD,
                    0,
                    Optional.empty()
            );
        }
        if (!canAcceptCompleteOutput(player, recipe, quantity)) {
            return new CraftResult(
                    CraftStatus.INVENTORY_FULL,
                    0,
                    Optional.empty()
            );
        }

        R01CraftingState.BeginResult begin = state(player).begin(
                player.getUUID().toString(),
                recipe.id(),
                quantity,
                goldCost
        );
        if (!begin.created()) {
            throw new IllegalStateException(
                    "R01 crafting retained an unreconciled pending transaction."
            );
        }
        replaceCraftState(player, begin.state());
        return executePending(player, begin.craft(), false);
    }

    public static CraftResult craftMaxSettlement(
            ServerPlayer player,
            R01CraftingRecipe recipe
    ) {
        int maximum = maxCraftableSettlement(player, recipe);
        if (maximum <= 0) {
            return craftSettlement(player, recipe, 1);
        }
        return craftSettlement(player, recipe, maximum);
    }

    public static int maxCraftableSettlement(
            ServerPlayer player,
            R01CraftingRecipe recipe
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(recipe, "recipe");
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                recipe.serviceId()
        ).isEmpty()) {
            return 0;
        }

        long maximum = Integer.MAX_VALUE;
        PlayerInventoryState inventory = PlayerInventoryService.state(player);
        for (Map.Entry<String, Integer> cost : recipe.unitMaterialCosts().entrySet()) {
            long available = inventory.materialPouch()
                    .getOrDefault(cost.getKey(), 0);
            available += inventory.materialVault()
                    .getOrDefault(cost.getKey(), 0);
            maximum = Math.min(maximum, available / cost.getValue());
        }
        if (recipe.unitGoldFee() > 0L) {
            maximum = Math.min(
                    maximum,
                    Math.max(0L, PlayerCurrencyService.state(player).gold())
                            / recipe.unitGoldFee()
            );
        }
        maximum = Math.min(
                maximum,
                backpackRoomFor(player, recipe.outputUnit())
        );
        return (int) Math.max(0L, maximum);
    }

    public static void reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01CraftingState.PendingCraft pending =
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

    private static CraftResult executePending(
            ServerPlayer player,
            R01CraftingState.PendingCraft pending,
            boolean recovery
    ) {
        R01CraftingRecipe recipe = R01CraftingRecipe.byId(
                pending.recipeId()
        ).orElseThrow(() -> new IllegalStateException(
                "Unknown persisted R01 recipe: " + pending.recipeId()
        ));

        String goldTransactionId = pending.transactionId() + "/gold";
        String materialTransactionId = materialTransactionId(
                pending.transactionId()
        );
        boolean goldDone = pending.goldCost() == 0L
                || PlayerCurrencyService.state(player)
                        .hasAppliedDebit(goldTransactionId);
        boolean materialsDone = PlayerInventoryService.state(player)
                .deliveryCompleted(materialTransactionId);

        Map<String, Integer> materialCosts =
                recipe.materialCostsFor(pending.quantity());

        if (!goldDone && !materialsDone) {
            if (pending.goldCost() > 0L
                    && PlayerCurrencyService.state(player).gold()
                    < pending.goldCost()) {
                replaceCraftState(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
                return new CraftResult(
                        CraftStatus.INSUFFICIENT_GOLD,
                        0,
                        Optional.empty()
                );
            }
            if (!PlayerInventoryService.canConsumeMaterials(
                    player,
                    materialCosts,
                    true
            )) {
                replaceCraftState(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
                return new CraftResult(
                        CraftStatus.INSUFFICIENT_MATERIALS,
                        0,
                        Optional.empty()
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
                            "Craft materials committed before an unavailable Gold debit."
                    );
                }
                replaceCraftState(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
                return new CraftResult(
                        CraftStatus.INSUFFICIENT_GOLD,
                        0,
                        Optional.empty()
                );
            }
            goldDone = true;
        }

        PlayerInventoryState.MaterialsConsumeOnceResult materialResult =
                PlayerInventoryService.consumeMaterialsOnce(
                        player,
                        materialTransactionId,
                        materialCosts,
                        true
                );
        if (!materialResult.consumed()) {
            return new CraftResult(
                    CraftStatus.RECOVERY_WAITING_FOR_MATERIALS,
                    0,
                    Optional.of(pending.transactionId())
            );
        }

        boolean pendingClaim = false;
        List<ProjectInventoryItem> chunks = outputChunks(
                recipe,
                pending.quantity()
        );
        for (int i = 0; i < chunks.size(); i++) {
            String outputId = outputTransactionId(
                    pending.transactionId(),
                    i
            );
            ProjectInventoryState inventory =
                    PlayerInventoryService.state(player);
            if (inventory.pendingReward(outputId).isPresent()) {
                PlayerInventoryState.DeliveryResult claimed =
                        PlayerInventoryService.claimPending(
                                player,
                                outputId
                        );
                if (claimed.status()
                        == PlayerInventoryState.DeliveryStatus.STILL_PENDING) {
                    pendingClaim = true;
                }
                continue;
            }
            if (inventory.deliveryCompleted(outputId)) {
                continue;
            }

            PlayerInventoryState.BackpackDeliveryResult backpack =
                    PlayerInventoryService.deliverBackpackOnce(
                            player,
                            outputId,
                            chunks.get(i)
                    );
            if (backpack.delivered()) {
                continue;
            }

            PlayerInventoryState.DeliveryResult fallback =
                    PlayerInventoryService.deliverImportantOnce(
                            player,
                            outputId,
                            chunks.get(i)
                    );
            if (fallback.status()
                    == PlayerInventoryState.DeliveryStatus.PENDING
                    || fallback.status()
                    == PlayerInventoryState.DeliveryStatus.STILL_PENDING) {
                pendingClaim = true;
            }
        }

        awardRecipeInsight(player, recipe);
        replaceCraftState(
                player,
                state(player).clearPending(pending.transactionId())
        );

        PlayerInventoryService.forgetCompletedDeliveryReceipt(
                player,
                materialTransactionId
        );
        for (int i = 0; i < chunks.size(); i++) {
            String outputId = outputTransactionId(
                    pending.transactionId(),
                    i
            );
            if (PlayerInventoryService.state(player)
                    .pendingReward(outputId).isEmpty()) {
                PlayerInventoryService.forgetCompletedDeliveryReceipt(
                        player,
                        outputId
                );
            }
        }

        return new CraftResult(
                pendingClaim
                        ? CraftStatus.CRAFTED_PENDING_CLAIM
                        : recovery
                                ? CraftStatus.CRAFTED_RECOVERED
                                : CraftStatus.CRAFTED,
                pending.quantity(),
                Optional.of(pending.transactionId())
        );
    }

    private static void awardRecipeInsight(
            ServerPlayer player,
            R01CraftingRecipe recipe
    ) {
        ProfessionMasteryState current = masteryState(player);
        ProfessionMasteryState next = current.awardInsightOnce(
                recipe.profession(),
                recipe.insightFlag()
        );
        if (!current.equals(next)) {
            player.setAttached(ProfessionMasteryAttachments.STATE, next);
        }
    }

    private static boolean canAcceptCompleteOutput(
            ServerPlayer player,
            R01CraftingRecipe recipe,
            int quantity
    ) {
        if (quantity > backpackRoomFor(player, recipe.outputUnit())) {
            return false;
        }
        var simulated = PlayerInventoryService.state(player).backpack();
        for (ProjectInventoryItem chunk : outputChunks(recipe, quantity)) {
            var inserted = simulated.insert(chunk);
            if (inserted.remainder().isPresent()) {
                return false;
            }
            simulated = inserted.state();
        }
        return true;
    }

    private static int backpackRoomFor(
            ServerPlayer player,
            ProjectInventoryItem unit
    ) {
        var backpack = PlayerInventoryService.state(player).backpack();
        long room = (long) (backpack.capacity() - backpack.usedSlots())
                * unit.stackCap();
        for (var entry : backpack.occupied()) {
            if (entry.item().canMerge(unit)) {
                room += entry.item().stackCap()
                        - entry.item().quantity();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, room);
    }

    private static List<ProjectInventoryItem> outputChunks(
            R01CraftingRecipe recipe,
            int quantity
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Craft output quantity must be positive."
            );
        }
        int stackCap = recipe.outputUnit().stackCap();
        int remaining = quantity;
        ArrayList<ProjectInventoryItem> chunks = new ArrayList<>();
        while (remaining > 0) {
            int chunk = Math.min(stackCap, remaining);
            chunks.add(recipe.outputChunk(chunk));
            remaining -= chunk;
        }
        return List.copyOf(chunks);
    }

    private static String materialTransactionId(String craftTransactionId) {
        return MATERIAL_RECEIPT_PREFIX
                + craftTransactionId.substring(
                        craftTransactionId.indexOf(':') + 1
                );
    }

    private static String outputTransactionId(
            String craftTransactionId,
            int chunk
    ) {
        return craftTransactionId + "/output/" + chunk;
    }

    private static void replaceCraftState(
            ServerPlayer player,
            R01CraftingState next
    ) {
        R01CraftingState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(R01CraftingAttachments.STATE, next);
        }
    }

    public enum CraftStatus {
        CRAFTED,
        CRAFTED_RECOVERED,
        CRAFTED_PENDING_CLAIM,
        SERVICE_NOT_PRODUCTION,
        INVALID_QUANTITY,
        INSUFFICIENT_GOLD,
        INSUFFICIENT_MATERIALS,
        INVENTORY_FULL,
        RECOVERY_WAITING_FOR_MATERIALS;

        public boolean successful() {
            return this == CRAFTED
                    || this == CRAFTED_RECOVERED
                    || this == CRAFTED_PENDING_CLAIM;
        }
    }

    public record CraftResult(
            CraftStatus status,
            int quantity,
            Optional<String> transactionId
    ) {
        public CraftResult {
            Objects.requireNonNull(status, "status");
            if (quantity < 0) {
                throw new IllegalArgumentException(
                        "Craft result quantity must be non-negative."
                );
            }
            transactionId = Objects.requireNonNull(
                    transactionId,
                    "transactionId"
            );
            if (status.successful() && quantity <= 0) {
                throw new IllegalArgumentException(
                        "Successful craft must report positive quantity."
                );
            }
        }
    }
}
