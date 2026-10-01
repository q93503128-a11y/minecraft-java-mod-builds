package dev.moonseungjun.openworldrpg.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

public record R01FixedMerchantState(
        int schemaVersion,
        long nextPurchaseSerial,
        Optional<PendingPurchase> pending
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01FixedMerchantState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("fixed_merchant_schema_version")
                            .forGetter(R01FixedMerchantState::schemaVersion),
                    Codec.LONG.fieldOf("next_purchase_serial")
                            .forGetter(R01FixedMerchantState::nextPurchaseSerial),
                    PendingPurchase.CODEC.optionalFieldOf("pending_purchase")
                            .forGetter(R01FixedMerchantState::pending)
            ).apply(instance, R01FixedMerchantState::new));

    public R01FixedMerchantState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported fixed-merchant schema.");
        }
        if (nextPurchaseSerial < 0L) {
            throw new IllegalArgumentException("nextPurchaseSerial must be non-negative.");
        }
        pending = Objects.requireNonNull(pending, "pending");
    }

    public static R01FixedMerchantState initial() {
        return new R01FixedMerchantState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty()
        );
    }

    public BeginResult begin(String playerUuid, String offerId) {
        java.util.UUID.fromString(playerUuid);
        requireId(offerId);
        if (pending.isPresent()) {
            return new BeginResult(this, pending.orElseThrow(), false);
        }
        String transactionId = "openworld_rpg:fixed_purchase/"
                + playerUuid + "/" + nextPurchaseSerial;
        PendingPurchase purchase = new PendingPurchase(
                transactionId,
                offerId
        );
        return new BeginResult(
                new R01FixedMerchantState(
                        schemaVersion,
                        Math.addExact(nextPurchaseSerial, 1L),
                        Optional.of(purchase)
                ),
                purchase,
                true
        );
    }

    public R01FixedMerchantState clearPending(String transactionId) {
        requireId(transactionId);
        if (pending.isEmpty()) {
            return this;
        }
        if (!pending.orElseThrow().transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Fixed-merchant pending transaction mismatch."
            );
        }
        return new R01FixedMerchantState(
                schemaVersion,
                nextPurchaseSerial,
                Optional.empty()
        );
    }

    private static void requireId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected namespaced id.");
        }
    }

    public record PendingPurchase(
            String transactionId,
            String offerId
    ) {
        public static final Codec<PendingPurchase> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingPurchase::transactionId),
                        Codec.STRING.fieldOf("offer_id")
                                .forGetter(PendingPurchase::offerId)
                ).apply(instance, PendingPurchase::new));

        public PendingPurchase {
            requireId(transactionId);
            requireId(offerId);
        }
    }

    public record BeginResult(
            R01FixedMerchantState state,
            PendingPurchase purchase,
            boolean created
    ) {
        public BeginResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(purchase, "purchase");
        }
    }
}
