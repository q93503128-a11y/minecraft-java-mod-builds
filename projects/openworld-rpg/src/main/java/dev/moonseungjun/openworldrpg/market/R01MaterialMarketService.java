package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.progression.r01.R01RiverbankRemediesService;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Reconnect-safe Nessa Material Pouch selling authority. */
public final class R01MaterialMarketService {
    private R01MaterialMarketService() {
    }

    public static R01MaterialMarketState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01MaterialMarketAttachments.STATE,
                R01MaterialMarketState.initial()
        );
    }

    public static SaleResult sellMaterial(
            ServerPlayer player,
            String materialId,
            int quantity,
            boolean signatureConfirmed
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(materialId, "materialId");
        reconcilePending(player);
        if (state(player).pending().isPresent()) {
            return new SaleResult(
                    SaleStatus.RECOVERY_BLOCKED,
                    Optional.empty()
            );
        }
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                R01NessaMarketRules.MARKET_SERVICE_ID
        ).isEmpty()) {
            return new SaleResult(
                    SaleStatus.SERVICE_NOT_PRODUCTION,
                    Optional.empty()
            );
        }
        if (quantity <= 0) {
            return new SaleResult(
                    SaleStatus.INVALID_QUANTITY,
                    Optional.empty()
            );
        }

        var unitValue = R01MaterialMarketRules.sellValue(materialId);
        if (unitValue.isEmpty()) {
            return new SaleResult(
                    SaleStatus.UNKNOWN_MATERIAL,
                    Optional.empty()
            );
        }
        if (R01MaterialMarketRules.requiresSignatureConfirmation(materialId)
                && !signatureConfirmed) {
            return new SaleResult(
                    SaleStatus.CONFIRMATION_REQUIRED,
                    Optional.empty()
            );
        }
        if (R01RiverbankRemediesService.sellablePouchCount(
                player,
                materialId
        ) < quantity) {
            return new SaleResult(
                    SaleStatus.INSUFFICIENT_MATERIALS,
                    Optional.empty()
            );
        }

        long gold;
        try {
            gold = Math.multiplyExact(unitValue.getAsLong(), quantity);
        } catch (ArithmeticException exception) {
            return new SaleResult(
                    SaleStatus.INVALID_QUANTITY,
                    Optional.empty()
            );
        }
        return beginAndExecute(
                player,
                Map.of(materialId, quantity),
                gold,
                false,
                false
        );
    }

    public static SaleResult sellAllEligible(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        reconcilePending(player);
        if (state(player).pending().isPresent()) {
            return new SaleResult(
                    SaleStatus.RECOVERY_BLOCKED,
                    Optional.empty()
            );
        }
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                R01NessaMarketRules.MARKET_SERVICE_ID
        ).isEmpty()) {
            return new SaleResult(
                    SaleStatus.SERVICE_NOT_PRODUCTION,
                    Optional.empty()
            );
        }

        Map<String, Integer> materials = new HashMap<>();
        long gold = 0L;
        for (Map.Entry<String, Long> entry
                : R01MaterialMarketRules.sellValues().entrySet()) {
            String materialId = entry.getKey();
            if (!R01MaterialMarketRules.bulkEligible(materialId)) {
                continue;
            }
            int quantity = R01RiverbankRemediesService.sellablePouchCount(
                    player,
                    materialId
            );
            if (quantity <= 0) {
                continue;
            }
            materials.put(materialId, quantity);
            gold = Math.addExact(
                    gold,
                    Math.multiplyExact(entry.getValue(), quantity)
            );
        }
        if (materials.isEmpty()) {
            return new SaleResult(
                    SaleStatus.NOTHING_TO_SELL,
                    Optional.empty()
            );
        }
        return beginAndExecute(
                player,
                Map.copyOf(materials),
                gold,
                true,
                false
        );
    }

    public static SaleResult reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01MaterialMarketState.PendingSale pending =
                state(player).pending().orElse(null);
        if (pending == null) {
            return new SaleResult(
                    SaleStatus.NOTHING_TO_RECONCILE,
                    Optional.empty()
            );
        }
        return executePending(player, pending, true);
    }

    private static SaleResult beginAndExecute(
            ServerPlayer player,
            Map<String, Integer> materials,
            long gold,
            boolean bulk,
            boolean recovery
    ) {
        R01MaterialMarketState.BeginResult begin =
                state(player).begin(
                        player.getUUID().toString(),
                        materials,
                        gold,
                        bulk
                );
        if (!begin.created()) {
            return new SaleResult(
                    SaleStatus.RECOVERY_BLOCKED,
                    Optional.empty()
            );
        }
        replace(player, begin.state());
        return executePending(player, begin.sale(), recovery);
    }

    private static SaleResult executePending(
            ServerPlayer player,
            R01MaterialMarketState.PendingSale pending,
            boolean recovery
    ) {
        PlayerInventoryState.MaterialsConsumeOnceResult consumed =
                PlayerInventoryService.consumeMaterialsOnce(
                        player,
                        pending.transactionId() + "/materials",
                        pending.materials(),
                        false,
                        R01RiverbankRemediesService.protectedPouchCounts(player)
                );
        if (!consumed.consumed()) {
            if (!recovery) {
                replace(
                        player,
                        state(player).clearPending(
                                pending.transactionId()
                        )
                );
            }
            return new SaleResult(
                    SaleStatus.INSUFFICIENT_MATERIALS,
                    Optional.empty()
            );
        }

        PlayerCurrencyService.creditOnce(
                player,
                pending.transactionId() + "/gold",
                pending.gold()
        );
        replace(
                player,
                state(player).clearPending(pending.transactionId())
        );
        return new SaleResult(
                recovery ? SaleStatus.SOLD_RECOVERED : SaleStatus.SOLD,
                Optional.of(pending)
        );
    }

    private static void replace(
            ServerPlayer player,
            R01MaterialMarketState next
    ) {
        R01MaterialMarketState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(R01MaterialMarketAttachments.STATE, next);
        }
    }

    public enum SaleStatus {
        SOLD,
        SOLD_RECOVERED,
        SERVICE_NOT_PRODUCTION,
        INVALID_QUANTITY,
        UNKNOWN_MATERIAL,
        CONFIRMATION_REQUIRED,
        INSUFFICIENT_MATERIALS,
        NOTHING_TO_SELL,
        NOTHING_TO_RECONCILE,
        RECOVERY_BLOCKED;

        public boolean successful() {
            return this == SOLD || this == SOLD_RECOVERED;
        }
    }

    public record SaleResult(
            SaleStatus status,
            Optional<R01MaterialMarketState.PendingSale> sale
    ) {
        public SaleResult {
            Objects.requireNonNull(status, "status");
            sale = Objects.requireNonNull(sale, "sale");
            if (status.successful() != sale.isPresent()) {
                throw new IllegalArgumentException(
                        "Material-sale result/status mismatch."
                );
            }
        }
    }
}
