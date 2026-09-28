package dev.moonseungjun.openworldrpg.housing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Shared Overworld ownership/reservation authority for physical authored properties. */
public record R01HousingWorldState(
        int schemaVersion,
        Map<String, String> propertyOwners,
        Map<String, Reservation> reservations,
        Map<String, TransferReceipt> completedTransfers
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01HousingWorldState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("housing_world_schema_version")
                            .forGetter(R01HousingWorldState::schemaVersion),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .fieldOf("property_owners")
                            .forGetter(R01HousingWorldState::propertyOwners),
                    Codec.unboundedMap(Codec.STRING, Reservation.CODEC)
                            .fieldOf("reservations")
                            .forGetter(R01HousingWorldState::reservations),
                    Codec.unboundedMap(Codec.STRING, TransferReceipt.CODEC)
                            .fieldOf("completed_transfers")
                            .forGetter(R01HousingWorldState::completedTransfers)
            ).apply(instance, R01HousingWorldState::new));

    public R01HousingWorldState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported housing-world schema: " + schemaVersion
            );
        }
        propertyOwners = Map.copyOf(
                Objects.requireNonNull(propertyOwners, "propertyOwners")
        );
        reservations = Map.copyOf(
                Objects.requireNonNull(reservations, "reservations")
        );
        completedTransfers = Map.copyOf(
                Objects.requireNonNull(completedTransfers, "completedTransfers")
        );
        propertyOwners.forEach((propertyId, playerUuid) -> {
            requireStableId(propertyId);
            requireUuid(playerUuid);
        });
        reservations.forEach((propertyId, reservation) -> {
            requireStableId(propertyId);
            Objects.requireNonNull(reservation, "reservation");
            if (!propertyId.equals(reservation.propertyId())) {
                throw new IllegalArgumentException(
                        "Housing reservation key/property mismatch."
                );
            }
        });
        completedTransfers.forEach((transactionId, receipt) -> {
            requireStableId(transactionId);
            Objects.requireNonNull(receipt, "transfer receipt");
            if (!transactionId.equals(receipt.transactionId())) {
                throw new IllegalArgumentException(
                        "Housing transfer receipt key mismatch."
                );
            }
        });
    }

    public static R01HousingWorldState initial() {
        return new R01HousingWorldState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public Optional<String> owner(String propertyId) {
        requireStableId(propertyId);
        return Optional.ofNullable(propertyOwners.get(propertyId));
    }

    public Optional<Reservation> reservation(String propertyId) {
        requireStableId(propertyId);
        return Optional.ofNullable(reservations.get(propertyId));
    }

    public Optional<TransferReceipt> completedTransfer(String transactionId) {
        requireStableId(transactionId);
        return Optional.ofNullable(completedTransfers.get(transactionId));
    }

    public ReserveResult reserve(
            String propertyId,
            String playerUuid,
            String transactionId
    ) {
        requireStableId(propertyId);
        requireUuid(playerUuid);
        requireStableId(transactionId);

        String owner = propertyOwners.get(propertyId);
        if (owner != null && !owner.equals(playerUuid)) {
            return new ReserveResult(this, ReserveStatus.OCCUPIED);
        }

        Reservation current = reservations.get(propertyId);
        if (current != null) {
            if (current.playerUuid().equals(playerUuid)
                    && current.transactionId().equals(transactionId)) {
                return new ReserveResult(
                        this,
                        ReserveStatus.ALREADY_RESERVED_BY_TRANSACTION
                );
            }
            return new ReserveResult(this, ReserveStatus.RESERVED);
        }

        Map<String, Reservation> next = new HashMap<>(reservations);
        next.put(
                propertyId,
                new Reservation(propertyId, playerUuid, transactionId)
        );
        return new ReserveResult(
                copy(propertyOwners, Map.copyOf(next), completedTransfers),
                ReserveStatus.RESERVED_NOW
        );
    }

    public R01HousingWorldState cancelReservation(
            String propertyId,
            String playerUuid,
            String transactionId
    ) {
        requireStableId(propertyId);
        requireUuid(playerUuid);
        requireStableId(transactionId);
        Reservation current = reservations.get(propertyId);
        if (current == null) {
            return this;
        }
        if (!current.playerUuid().equals(playerUuid)
                || !current.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Cannot cancel another housing reservation."
            );
        }
        Map<String, Reservation> next = new HashMap<>(reservations);
        next.remove(propertyId);
        return copy(propertyOwners, Map.copyOf(next), completedTransfers);
    }

    public R01HousingWorldState transferReserved(
            R01HousingPlayerState.PendingMove pending,
            String playerUuid
    ) {
        Objects.requireNonNull(pending, "pending");
        requireUuid(playerUuid);

        TransferReceipt completed = completedTransfers.get(
                pending.transactionId()
        );
        if (completed != null) {
            if (!completed.playerUuid().equals(playerUuid)
                    || !completed.newPropertyId().equals(
                            pending.newPropertyId()
                    )) {
                throw new IllegalStateException(
                        "Housing transaction receipt payload mismatch."
                );
            }
            return this;
        }

        Reservation reservation = reservations.get(pending.newPropertyId());
        if (reservation == null
                || !reservation.playerUuid().equals(playerUuid)
                || !reservation.transactionId().equals(
                        pending.transactionId()
                )) {
            throw new IllegalStateException(
                    "Housing ownership transfer requires its reservation."
            );
        }

        String targetOwner = propertyOwners.get(pending.newPropertyId());
        if (targetOwner != null && !targetOwner.equals(playerUuid)) {
            throw new IllegalStateException(
                    "Reserved housing target became owned by another player."
            );
        }

        pending.oldPropertyId().ifPresent(oldPropertyId -> {
            String oldOwner = propertyOwners.get(oldPropertyId);
            if (!playerUuid.equals(oldOwner)) {
                throw new IllegalStateException(
                        "Player no longer owns the old residence."
                );
            }
        });

        Map<String, String> nextOwners = new HashMap<>(propertyOwners);
        pending.oldPropertyId().ifPresent(nextOwners::remove);
        nextOwners.put(pending.newPropertyId(), playerUuid);

        Map<String, Reservation> nextReservations =
                new HashMap<>(reservations);
        nextReservations.remove(pending.newPropertyId());

        Map<String, TransferReceipt> nextReceipts =
                new HashMap<>(completedTransfers);
        nextReceipts.put(
                pending.transactionId(),
                new TransferReceipt(
                        pending.transactionId(),
                        playerUuid,
                        pending.oldPropertyId(),
                        pending.newPropertyId()
                )
        );

        return copy(
                Map.copyOf(nextOwners),
                Map.copyOf(nextReservations),
                Map.copyOf(nextReceipts)
        );
    }

    private R01HousingWorldState copy(
            Map<String, String> owners,
            Map<String, Reservation> nextReservations,
            Map<String, TransferReceipt> receipts
    ) {
        return new R01HousingWorldState(
                schemaVersion,
                owners,
                nextReservations,
                receipts
        );
    }

    public enum ReserveStatus {
        RESERVED_NOW,
        ALREADY_RESERVED_BY_TRANSACTION,
        OCCUPIED,
        RESERVED
    }

    public record ReserveResult(
            R01HousingWorldState state,
            ReserveStatus status
    ) {
        public ReserveResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(status, "status");
        }

        public boolean success() {
            return status == ReserveStatus.RESERVED_NOW
                    || status == ReserveStatus.ALREADY_RESERVED_BY_TRANSACTION;
        }
    }

    public record Reservation(
            String propertyId,
            String playerUuid,
            String transactionId
    ) {
        public static final Codec<Reservation> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("property_id")
                                .forGetter(Reservation::propertyId),
                        Codec.STRING.fieldOf("player_uuid")
                                .forGetter(Reservation::playerUuid),
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(Reservation::transactionId)
                ).apply(instance, Reservation::new));

        public Reservation {
            requireStableId(propertyId);
            requireUuid(playerUuid);
            requireStableId(transactionId);
        }
    }

    public record TransferReceipt(
            String transactionId,
            String playerUuid,
            Optional<String> oldPropertyId,
            String newPropertyId
    ) {
        public static final Codec<TransferReceipt> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(TransferReceipt::transactionId),
                        Codec.STRING.fieldOf("player_uuid")
                                .forGetter(TransferReceipt::playerUuid),
                        Codec.STRING.optionalFieldOf("old_property_id")
                                .forGetter(TransferReceipt::oldPropertyId),
                        Codec.STRING.fieldOf("new_property_id")
                                .forGetter(TransferReceipt::newPropertyId)
                ).apply(instance, TransferReceipt::new));

        public TransferReceipt {
            requireStableId(transactionId);
            requireUuid(playerUuid);
            oldPropertyId = Objects.requireNonNull(
                    oldPropertyId,
                    "oldPropertyId"
            );
            oldPropertyId.ifPresent(R01HousingWorldState::requireStableId);
            requireStableId(newPropertyId);
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced housing id."
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
