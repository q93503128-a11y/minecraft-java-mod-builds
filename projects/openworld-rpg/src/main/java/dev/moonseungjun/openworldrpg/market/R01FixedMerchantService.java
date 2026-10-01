package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryService;
import dev.moonseungjun.openworldrpg.inventory.PlayerInventoryState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.recovery.R01NourishmentMeal;
import dev.moonseungjun.openworldrpg.recovery.RecoveryConsumable;
import dev.moonseungjun.openworldrpg.world.structure.R01AlderfordRuntimeBindingRegistry;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared server transaction for Alderford's unlimited fixed consumable stock.
 *
 * <p>Normal purchases require legal Backpack space before Gold mutation. A persisted pending
 * transaction exists only to make the debit/delivery boundary reconnect-safe.</p>
 */
public final class R01FixedMerchantService {
    public static final String LYSA_SERVICE_ID =
            "openworld_rpg:service/alderford/greenwater_remedies";
    public static final String BRIN_SERVICE_ID =
            "openworld_rpg:service/alderford/copper_kettle";

    private R01FixedMerchantService() {
    }

    public static R01FixedMerchantState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                R01FixedMerchantAttachments.STATE,
                R01FixedMerchantState.initial()
        );
    }

    public static PurchaseResult purchase(
            ServerPlayer player,
            Offer offer
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(offer, "offer");

        reconcilePending(player);
        if (R01AlderfordRuntimeBindingRegistry.productionService(
                offer.serviceId()
        ).isEmpty()) {
            return new PurchaseResult(
                    PurchaseStatus.SERVICE_NOT_PRODUCTION,
                    Optional.empty()
            );
        }

        ProjectInventoryItem item = offer.item();
        if (PlayerCurrencyService.state(player).gold() < offer.priceGold()) {
            return new PurchaseResult(
                    PurchaseStatus.INSUFFICIENT_GOLD,
                    Optional.empty()
            );
        }
        if (!PlayerInventoryService.canAcceptBackpack(player, item)) {
            return new PurchaseResult(
                    PurchaseStatus.INVENTORY_FULL,
                    Optional.empty()
            );
        }

        R01FixedMerchantState.BeginResult begin = state(player).begin(
                player.getUUID().toString(),
                offer.id()
        );
        if (!begin.created()) {
            throw new IllegalStateException(
                    "Fixed merchant retained an unreconciled pending purchase."
            );
        }
        replace(player, begin.state());

        return executePending(
                player,
                begin.purchase(),
                false
        );
    }

    public static void reconcilePending(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01FixedMerchantState.PendingPurchase pending =
                state(player).pending().orElse(null);
        if (pending == null) {
            return;
        }
        executePending(player, pending, true);
    }

    private static PurchaseResult executePending(
            ServerPlayer player,
            R01FixedMerchantState.PendingPurchase pending,
            boolean recovery
    ) {
        Offer offer = Offer.byId(pending.offerId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown persisted fixed-merchant offer: "
                                + pending.offerId()
                ));

        boolean alreadyDebited =
                PlayerCurrencyService.state(player)
                        .hasAppliedDebit(pending.transactionId());

        if (!alreadyDebited
                && PlayerCurrencyService.state(player).gold()
                        < offer.priceGold()) {
            replace(
                    player,
                    state(player).clearPending(
                            pending.transactionId()
                    )
            );
            return new PurchaseResult(
                    PurchaseStatus.INSUFFICIENT_GOLD,
                    Optional.empty()
            );
        }

        if (!alreadyDebited
                && !PlayerInventoryService.canAcceptBackpack(
                        player,
                        offer.item()
                )) {
            replace(
                    player,
                    state(player).clearPending(
                            pending.transactionId()
                    )
            );
            return new PurchaseResult(
                    PurchaseStatus.INVENTORY_FULL,
                    Optional.empty()
            );
        }

        var debit = PlayerCurrencyService.debitOnce(
                player,
                pending.transactionId(),
                offer.priceGold()
        );
        if (!debit.success()) {
            replace(
                    player,
                    state(player).clearPending(
                            pending.transactionId()
                    )
            );
            return new PurchaseResult(
                    PurchaseStatus.INSUFFICIENT_GOLD,
                    Optional.empty()
            );
        }

        PlayerInventoryState.BackpackDeliveryResult delivery =
                PlayerInventoryService.deliverBackpackOnce(
                        player,
                        pending.transactionId(),
                        offer.item()
                );

        PurchaseStatus status =
                recovery
                        ? PurchaseStatus.PURCHASED_RECOVERED
                        : PurchaseStatus.PURCHASED;
        if (!delivery.delivered()) {
            PlayerInventoryState.DeliveryResult fallback =
                    PlayerInventoryService.deliverImportantOnce(
                            player,
                            pending.transactionId(),
                            offer.item()
                    );
            status = switch (fallback.status()) {
                case DELIVERED, ALREADY_COMPLETED ->
                        PurchaseStatus.PURCHASED_RECOVERED;
                case PENDING, STILL_PENDING ->
                        PurchaseStatus.PURCHASED_RECOVERY_PENDING;
            };
        }

        replace(
                player,
                state(player).clearPending(
                        pending.transactionId()
                )
        );
        return new PurchaseResult(
                status,
                Optional.of(pending.transactionId())
        );
    }

    private static void replace(
            ServerPlayer player,
            R01FixedMerchantState next
    ) {
        R01FixedMerchantState current = state(player);
        if (!current.equals(next)) {
            player.setAttached(
                    R01FixedMerchantAttachments.STATE,
                    next
            );
        }
    }

    public enum Offer {
        HEALING_POTION(
                "openworld_rpg:offer/lysa/healing_potion",
                LYSA_SERVICE_ID,
                30L,
                ProjectInventoryItem.recovery(
                        "openworld_rpg:healing_potion",
                        RecoveryConsumable.HEALING_POTION,
                        1,
                        0L
                )
        ),
        FOCUS_DRAUGHT(
                "openworld_rpg:offer/lysa/focus_draught",
                LYSA_SERVICE_ID,
                35L,
                ProjectInventoryItem.recovery(
                        "openworld_rpg:focus_draught",
                        RecoveryConsumable.FOCUS_DRAUGHT,
                        1,
                        0L
                )
        ),
        CLEANSING_TONIC(
                "openworld_rpg:offer/lysa/cleansing_tonic",
                LYSA_SERVICE_ID,
                40L,
                ProjectInventoryItem.recovery(
                        "openworld_rpg:cleansing_tonic",
                        RecoveryConsumable.CLEANSING_TONIC,
                        1,
                        0L
                )
        ),
        HERBED_LOUXIA_ROAST(
                "openworld_rpg:offer/brin/herbed_louxia_roast",
                BRIN_SERVICE_ID,
                25L,
                mealItem(R01NourishmentMeal.HERBED_LOUXIA_ROAST)
        ),
        TRAIL_SKEWERS(
                "openworld_rpg:offer/brin/trail_skewers",
                BRIN_SERVICE_ID,
                20L,
                mealItem(R01NourishmentMeal.TRAIL_SKEWERS)
        ),
        GLOW_BROTH(
                "openworld_rpg:offer/brin/glow_broth",
                BRIN_SERVICE_ID,
                25L,
                mealItem(R01NourishmentMeal.GLOW_BROTH)
        );

        private final String id;
        private final String serviceId;
        private final long priceGold;
        private final ProjectInventoryItem item;

        Offer(
                String id,
                String serviceId,
                long priceGold,
                ProjectInventoryItem item
        ) {
            this.id = id;
            this.serviceId = serviceId;
            this.priceGold = priceGold;
            this.item = item;
        }

        public String id() {
            return id;
        }

        public String serviceId() {
            return serviceId;
        }

        public long priceGold() {
            return priceGold;
        }

        public ProjectInventoryItem item() {
            return item;
        }

        public static Optional<Offer> byId(String id) {
            Objects.requireNonNull(id, "id");
            return Arrays.stream(values())
                    .filter(value -> value.id.equals(id))
                    .findFirst();
        }

        private static ProjectInventoryItem mealItem(
                R01NourishmentMeal meal
        ) {
            return ProjectInventoryItem.ordinary(
                    meal.itemId(),
                    1,
                    50,
                    0L
            );
        }
    }

    public enum PurchaseStatus {
        PURCHASED,
        PURCHASED_RECOVERED,
        PURCHASED_RECOVERY_PENDING,
        SERVICE_NOT_PRODUCTION,
        INSUFFICIENT_GOLD,
        INVENTORY_FULL;

        public boolean successful() {
            return this == PURCHASED
                    || this == PURCHASED_RECOVERED
                    || this == PURCHASED_RECOVERY_PENDING;
        }
    }

    public record PurchaseResult(
            PurchaseStatus status,
            Optional<String> transactionId
    ) {
        public PurchaseResult {
            Objects.requireNonNull(status, "status");
            transactionId = Objects.requireNonNull(
                    transactionId,
                    "transactionId"
            );
            if (status.successful() != transactionId.isPresent()) {
                throw new IllegalArgumentException(
                        "Fixed-merchant purchase status/id mismatch."
                );
            }
        }
    }
}
