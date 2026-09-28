package dev.moonseungjun.openworldrpg.housing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

/** Persistent personal residence state and durable in-flight move transaction. */
public record R01HousingPlayerState(
        int schemaVersion,
        Optional<String> residencePropertyId,
        Optional<R01HomeStorageState> homeStorage,
        long transactionSequence,
        Optional<PendingMove> pendingMove
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01HousingPlayerState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("housing_schema_version")
                            .forGetter(R01HousingPlayerState::schemaVersion),
                    Codec.STRING.optionalFieldOf("residence_property_id")
                            .forGetter(R01HousingPlayerState::residencePropertyId),
                    R01HomeStorageState.CODEC.optionalFieldOf("home_storage")
                            .forGetter(R01HousingPlayerState::homeStorage),
                    Codec.LONG.fieldOf("transaction_sequence")
                            .forGetter(R01HousingPlayerState::transactionSequence),
                    PendingMove.CODEC.optionalFieldOf("pending_move")
                            .forGetter(R01HousingPlayerState::pendingMove)
            ).apply(instance, R01HousingPlayerState::new));

    public R01HousingPlayerState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported housing schema version: " + schemaVersion
            );
        }
        residencePropertyId = Objects.requireNonNull(
                residencePropertyId,
                "residencePropertyId"
        );
        homeStorage = Objects.requireNonNull(homeStorage, "homeStorage");
        pendingMove = Objects.requireNonNull(pendingMove, "pendingMove");
        if (transactionSequence < 0L) {
            throw new IllegalArgumentException(
                    "Housing transaction sequence must be non-negative."
            );
        }
        if (residencePropertyId.isPresent() != homeStorage.isPresent()) {
            throw new IllegalArgumentException(
                    "Residence ownership and Home Storage must exist together."
            );
        }
        residencePropertyId.ifPresent(
                value -> requireStableId(value, "residencePropertyId")
        );
        pendingMove.ifPresent(value -> {
            if (value.sequence() != transactionSequence) {
                throw new IllegalArgumentException(
                        "Pending housing transaction sequence mismatch."
                );
            }
            if (!value.oldPropertyId().equals(residencePropertyId)) {
                throw new IllegalArgumentException(
                        "Pending housing move must start from current residence."
                );
            }
        });
    }

    public static R01HousingPlayerState initial() {
        return new R01HousingPlayerState(
                CURRENT_SCHEMA_VERSION,
                Optional.empty(),
                Optional.empty(),
                0L,
                Optional.empty()
        );
    }

    public R01HousingPlayerState prepareMove(
            String playerUuid,
            String targetPropertyId,
            int targetStorageCapacity,
            long purchasePrice,
            long saleCredit,
            long goldDelta
    ) {
        requireUuid(playerUuid);
        requireStableId(targetPropertyId, "targetPropertyId");
        if (pendingMove.isPresent()) {
            throw new IllegalStateException(
                    "Housing transaction already pending."
            );
        }
        if (residencePropertyId.filter(targetPropertyId::equals).isPresent()) {
            throw new IllegalArgumentException(
                    "Cannot move into the already-owned residence."
            );
        }
        if (targetStorageCapacity < R01HomeStorageState.MIN_CAPACITY
                || targetStorageCapacity > R01HomeStorageState.MAX_CAPACITY
                || purchasePrice <= 0L
                || saleCredit < 0L) {
            throw new IllegalArgumentException(
                    "Invalid housing move economics/storage."
            );
        }
        long sequence = Math.addExact(transactionSequence, 1L);
        String transactionId =
                "openworld_rpg:housing/"
                        + playerUuid
                        + "/"
                        + sequence;
        PendingMove pending = new PendingMove(
                transactionId,
                sequence,
                residencePropertyId,
                targetPropertyId,
                targetStorageCapacity,
                purchasePrice,
                saleCredit,
                goldDelta
        );
        return new R01HousingPlayerState(
                schemaVersion,
                residencePropertyId,
                homeStorage,
                sequence,
                Optional.of(pending)
        );
    }

    public R01HousingPlayerState cancelPending(String transactionId) {
        requireStableId(transactionId, "transactionId");
        PendingMove pending = pendingMove.orElse(null);
        if (pending == null) {
            return this;
        }
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Cannot cancel a different housing transaction."
            );
        }
        return new R01HousingPlayerState(
                schemaVersion,
                residencePropertyId,
                homeStorage,
                transactionSequence,
                Optional.empty()
        );
    }

    public R01HousingPlayerState commitPending(String transactionId) {
        requireStableId(transactionId, "transactionId");
        PendingMove pending = pendingMove.orElseThrow(
                () -> new IllegalStateException(
                        "No pending housing move to commit."
                )
        );
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Cannot commit a different housing transaction."
            );
        }

        R01HomeStorageState nextStorage = homeStorage
                .map(value -> value.resizeForMove(pending.targetStorageCapacity()))
                .orElseGet(
                        () -> R01HomeStorageState.empty(
                                pending.targetStorageCapacity()
                        )
                );
        return new R01HousingPlayerState(
                schemaVersion,
                Optional.of(pending.newPropertyId()),
                Optional.of(nextStorage),
                transactionSequence,
                Optional.empty()
        );
    }

    public record PendingMove(
            String transactionId,
            long sequence,
            Optional<String> oldPropertyId,
            String newPropertyId,
            int targetStorageCapacity,
            long purchasePrice,
            long saleCredit,
            long goldDelta
    ) {
        public static final Codec<PendingMove> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingMove::transactionId),
                        Codec.LONG.fieldOf("sequence")
                                .forGetter(PendingMove::sequence),
                        Codec.STRING.optionalFieldOf("old_property_id")
                                .forGetter(PendingMove::oldPropertyId),
                        Codec.STRING.fieldOf("new_property_id")
                                .forGetter(PendingMove::newPropertyId),
                        Codec.INT.fieldOf("target_storage_capacity")
                                .forGetter(PendingMove::targetStorageCapacity),
                        Codec.LONG.fieldOf("purchase_price")
                                .forGetter(PendingMove::purchasePrice),
                        Codec.LONG.fieldOf("sale_credit")
                                .forGetter(PendingMove::saleCredit),
                        Codec.LONG.fieldOf("gold_delta")
                                .forGetter(PendingMove::goldDelta)
                ).apply(instance, PendingMove::new));

        public PendingMove {
            requireStableId(transactionId, "transactionId");
            if (sequence <= 0L) {
                throw new IllegalArgumentException(
                        "Housing transaction sequence must be positive."
                );
            }
            oldPropertyId = Objects.requireNonNull(
                    oldPropertyId,
                    "oldPropertyId"
            );
            oldPropertyId.ifPresent(
                    value -> requireStableId(value, "oldPropertyId")
            );
            requireStableId(newPropertyId, "newPropertyId");
            if (oldPropertyId.filter(newPropertyId::equals).isPresent()) {
                throw new IllegalArgumentException(
                        "Housing move cannot target current property."
                );
            }
            if (targetStorageCapacity < R01HomeStorageState.MIN_CAPACITY
                    || targetStorageCapacity > R01HomeStorageState.MAX_CAPACITY
                    || purchasePrice <= 0L
                    || saleCredit < 0L) {
                throw new IllegalArgumentException(
                        "Invalid pending housing move."
                );
            }
        }

        public long requiredGold() {
            return Math.max(0L, goldDelta);
        }

        public long refundGold() {
            return Math.max(0L, -goldDelta);
        }
    }

    private static void requireStableId(String value, String name) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    name + " must be a stable namespaced id."
            );
        }
    }

    private static void requireUuid(String value) {
        try {
            java.util.UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid housing player UUID: " + value,
                    exception
            );
        }
    }
}
