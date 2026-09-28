package dev.moonseungjun.openworldrpg.housing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Shared Overworld ownership/reservation/permission authority for physical authored properties. */
public record R01HousingWorldState(
        int schemaVersion,
        Map<String, String> propertyOwners,
        Map<String, Reservation> reservations,
        Map<String, TransferReceipt> completedTransfers,
        Map<String, List<String>> trustedDecorators,
        Map<String, List<String>> privateStorageAccess
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Map<String, List<String>>> PERMISSION_MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf());

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
                            .forGetter(R01HousingWorldState::completedTransfers),
                    PERMISSION_MAP_CODEC
                            .optionalFieldOf("trusted_decorators", Map.of())
                            .forGetter(R01HousingWorldState::trustedDecorators),
                    PERMISSION_MAP_CODEC
                            .optionalFieldOf("private_storage_access", Map.of())
                            .forGetter(R01HousingWorldState::privateStorageAccess)
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
        trustedDecorators = immutablePermissionMap(
                trustedDecorators,
                "trustedDecorators"
        );
        privateStorageAccess = immutablePermissionMap(
                privateStorageAccess,
                "privateStorageAccess"
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
        validatePermissionMap(
                trustedDecorators,
                propertyOwners,
                "trusted decorator"
        );
        validatePermissionMap(
                privateStorageAccess,
                propertyOwners,
                "private storage"
        );
        for (Map.Entry<String, List<String>> entry
                : privateStorageAccess.entrySet()) {
            List<String> decorators = trustedDecorators.getOrDefault(
                    entry.getKey(),
                    List.of()
            );
            for (String playerUuid : entry.getValue()) {
                if (!decorators.contains(playerUuid)) {
                    throw new IllegalArgumentException(
                            "Private Home Storage access requires Trusted Decorator role."
                    );
                }
            }
        }
    }

    /** Backward-compatible constructor for the pre-permission schema shape. */
    public R01HousingWorldState(
            int schemaVersion,
            Map<String, String> propertyOwners,
            Map<String, Reservation> reservations,
            Map<String, TransferReceipt> completedTransfers
    ) {
        this(
                schemaVersion,
                propertyOwners,
                reservations,
                completedTransfers,
                Map.of(),
                Map.of()
        );
    }

    public static R01HousingWorldState initial() {
        return new R01HousingWorldState(
                CURRENT_SCHEMA_VERSION,
                Map.of(),
                Map.of(),
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

    public PropertyRole roleFor(String propertyId, String playerUuid) {
        requireStableId(propertyId);
        requireUuid(playerUuid);
        String owner = propertyOwners.get(propertyId);
        if (playerUuid.equals(owner)) {
            return PropertyRole.OWNER;
        }
        if (trustedDecorators
                .getOrDefault(propertyId, List.of())
                .contains(playerUuid)) {
            return PropertyRole.TRUSTED_DECORATOR;
        }
        return PropertyRole.GUEST;
    }

    public boolean canFurnish(String propertyId, String playerUuid) {
        PropertyRole role = roleFor(propertyId, playerUuid);
        return role == PropertyRole.OWNER
                || role == PropertyRole.TRUSTED_DECORATOR;
    }

    public boolean canAccessPrivateStorage(
            String propertyId,
            String playerUuid
    ) {
        PropertyRole role = roleFor(propertyId, playerUuid);
        if (role == PropertyRole.OWNER) {
            return true;
        }
        return privateStorageAccess
                .getOrDefault(propertyId, List.of())
                .contains(playerUuid);
    }

    public boolean canUseNonPrivateFurniture(
            String propertyId,
            String playerUuid
    ) {
        requireStableId(propertyId);
        requireUuid(playerUuid);
        return propertyOwners.containsKey(propertyId);
    }

    public R01HousingWorldState setTrustedDecorator(
            String propertyId,
            String ownerUuid,
            String targetUuid,
            boolean trusted
    ) {
        requireOwnedBy(propertyId, ownerUuid);
        requireUuid(targetUuid);
        if (ownerUuid.equals(targetUuid)) {
            throw new IllegalArgumentException(
                    "Property owner already has furnishing authority."
            );
        }

        Map<String, List<String>> nextDecorators =
                new HashMap<>(trustedDecorators);
        List<String> current = new ArrayList<>(
                nextDecorators.getOrDefault(propertyId, List.of())
        );
        if (trusted) {
            if (!current.contains(targetUuid)) {
                current.add(targetUuid);
            }
        } else {
            current.remove(targetUuid);
        }
        setPermissionList(nextDecorators, propertyId, current);

        Map<String, List<String>> nextStorage =
                new HashMap<>(privateStorageAccess);
        if (!trusted) {
            List<String> storage = new ArrayList<>(
                    nextStorage.getOrDefault(propertyId, List.of())
            );
            storage.remove(targetUuid);
            setPermissionList(nextStorage, propertyId, storage);
        }

        return copy(
                propertyOwners,
                reservations,
                completedTransfers,
                Map.copyOf(nextDecorators),
                Map.copyOf(nextStorage)
        );
    }

    public R01HousingWorldState setPrivateStorageAccess(
            String propertyId,
            String ownerUuid,
            String targetUuid,
            boolean allowed
    ) {
        requireOwnedBy(propertyId, ownerUuid);
        requireUuid(targetUuid);
        if (ownerUuid.equals(targetUuid)) {
            throw new IllegalArgumentException(
                    "Property owner already has private storage authority."
            );
        }
        if (allowed
                && !trustedDecorators
                        .getOrDefault(propertyId, List.of())
                        .contains(targetUuid)) {
            throw new IllegalStateException(
                    "Private storage access requires Trusted Decorator role."
            );
        }

        Map<String, List<String>> nextStorage =
                new HashMap<>(privateStorageAccess);
        List<String> current = new ArrayList<>(
                nextStorage.getOrDefault(propertyId, List.of())
        );
        if (allowed) {
            if (!current.contains(targetUuid)) {
                current.add(targetUuid);
            }
        } else {
            current.remove(targetUuid);
        }
        setPermissionList(nextStorage, propertyId, current);

        return copy(
                propertyOwners,
                reservations,
                completedTransfers,
                trustedDecorators,
                Map.copyOf(nextStorage)
        );
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
                copy(
                        propertyOwners,
                        Map.copyOf(next),
                        completedTransfers,
                        trustedDecorators,
                        privateStorageAccess
                ),
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
        return copy(
                propertyOwners,
                Map.copyOf(next),
                completedTransfers,
                trustedDecorators,
                privateStorageAccess
        );
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

        Map<String, List<String>> nextDecorators =
                new HashMap<>(trustedDecorators);
        Map<String, List<String>> nextStorage =
                new HashMap<>(privateStorageAccess);
        pending.oldPropertyId().ifPresent(oldPropertyId -> {
            nextDecorators.remove(oldPropertyId);
            nextStorage.remove(oldPropertyId);
        });
        /*
         * A vacant target must start from the authored empty/default permission state even if a
         * stale save once contained permissions for it.
         */
        nextDecorators.remove(pending.newPropertyId());
        nextStorage.remove(pending.newPropertyId());

        return copy(
                Map.copyOf(nextOwners),
                Map.copyOf(nextReservations),
                Map.copyOf(nextReceipts),
                Map.copyOf(nextDecorators),
                Map.copyOf(nextStorage)
        );
    }

    private R01HousingWorldState copy(
            Map<String, String> owners,
            Map<String, Reservation> nextReservations,
            Map<String, TransferReceipt> receipts,
            Map<String, List<String>> decorators,
            Map<String, List<String>> storageAccess
    ) {
        return new R01HousingWorldState(
                schemaVersion,
                owners,
                nextReservations,
                receipts,
                decorators,
                storageAccess
        );
    }

    private void requireOwnedBy(String propertyId, String ownerUuid) {
        requireStableId(propertyId);
        requireUuid(ownerUuid);
        String actualOwner = propertyOwners.get(propertyId);
        if (!ownerUuid.equals(actualOwner)) {
            throw new IllegalStateException(
                    "Housing permission mutation requires property owner."
            );
        }
    }

    private static Map<String, List<String>> immutablePermissionMap(
            Map<String, List<String>> source,
            String name
    ) {
        Objects.requireNonNull(source, name);
        Map<String, List<String>> result = new HashMap<>();
        source.forEach((propertyId, players) -> {
            requireStableId(propertyId);
            Objects.requireNonNull(players, name + " player list");
            LinkedHashSet<String> unique = new LinkedHashSet<>();
            for (String playerUuid : players) {
                requireUuid(playerUuid);
                unique.add(playerUuid);
            }
            if (!unique.isEmpty()) {
                result.put(propertyId, List.copyOf(unique));
            }
        });
        return Map.copyOf(result);
    }

    private static void validatePermissionMap(
            Map<String, List<String>> permissions,
            Map<String, String> owners,
            String label
    ) {
        for (Map.Entry<String, List<String>> entry : permissions.entrySet()) {
            String owner = owners.get(entry.getKey());
            if (owner == null) {
                throw new IllegalArgumentException(
                        label + " permissions require an owned property."
                );
            }
            if (entry.getValue().contains(owner)) {
                throw new IllegalArgumentException(
                        "Property owner must not be duplicated in " + label
                                + " permissions."
                );
            }
        }
    }

    private static void setPermissionList(
            Map<String, List<String>> map,
            String propertyId,
            List<String> values
    ) {
        LinkedHashSet<String> unique = new LinkedHashSet<>(values);
        if (unique.isEmpty()) {
            map.remove(propertyId);
        } else {
            map.put(propertyId, List.copyOf(unique));
        }
    }

    public enum PropertyRole {
        OWNER,
        TRUSTED_DECORATOR,
        GUEST
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
