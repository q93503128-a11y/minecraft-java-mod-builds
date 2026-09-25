package dev.moonseungjun.openworldrpg.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent server-owned R01 inventory/storage authority.
 *
 * <p>General Backpack, Alderford Personal Storage, Material Pouch/Vault, Key Items and important
 * reward overflow live in one attachment so an important delivery is committed atomically across
 * its legal destinations.</p>
 */
public record PlayerInventoryState(
        int schemaVersion,
        ProjectBackpackState backpack,
        ProjectBackpackState personalStorage,
        Map<String, Integer> materialPouch,
        Map<String, Integer> materialVault,
        Set<String> keyItems,
        Map<String, PendingItemReward> pendingItemRewards,
        Set<String> completedDeliveryIds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final int MATERIAL_POUCH_CAP = 999;
    public static final int MATERIAL_VAULT_CAP = 9_999;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<PlayerInventoryState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("inventory_schema_version")
                            .forGetter(PlayerInventoryState::schemaVersion),
                    ProjectBackpackState.CODEC.fieldOf("backpack").forGetter(PlayerInventoryState::backpack),
                    ProjectBackpackState.CODEC.fieldOf("personal_storage").forGetter(PlayerInventoryState::personalStorage),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("material_pouch")
                            .forGetter(PlayerInventoryState::materialPouch),
                    Codec.unboundedMap(Codec.STRING, Codec.INT)
                            .fieldOf("material_vault")
                            .forGetter(PlayerInventoryState::materialVault),
                    STRING_SET_CODEC.fieldOf("key_items").forGetter(PlayerInventoryState::keyItems),
                    Codec.unboundedMap(Codec.STRING, PendingItemReward.CODEC)
                            .fieldOf("pending_item_rewards")
                            .forGetter(PlayerInventoryState::pendingItemRewards),
                    STRING_SET_CODEC
                            .fieldOf("completed_delivery_ids")
                            .forGetter(PlayerInventoryState::completedDeliveryIds)
            ).apply(instance, PlayerInventoryState::new)
    );

    public PlayerInventoryState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported inventory schema version: " + schemaVersion
            );
        }
        Objects.requireNonNull(backpack, "backpack");
        Objects.requireNonNull(personalStorage, "personalStorage");
        materialPouch = Map.copyOf(Objects.requireNonNull(materialPouch, "materialPouch"));
        materialVault = Map.copyOf(Objects.requireNonNull(materialVault, "materialVault"));
        keyItems = Set.copyOf(Objects.requireNonNull(keyItems, "keyItems"));
        pendingItemRewards = Map.copyOf(
                Objects.requireNonNull(pendingItemRewards, "pendingItemRewards")
        );
        completedDeliveryIds = Set.copyOf(
                Objects.requireNonNull(completedDeliveryIds, "completedDeliveryIds")
        );

        if (personalStorage.capacity() != 36) {
            throw new IllegalArgumentException(
                    "R01 Personal Storage must remain exactly 36 ordinary slots."
            );
        }
        validateCountMap(materialPouch, MATERIAL_POUCH_CAP, "materialPouch");
        validateCountMap(materialVault, MATERIAL_VAULT_CAP, "materialVault");
        keyItems.forEach(PlayerInventoryState::requireStableId);
        pendingItemRewards.keySet().forEach(PlayerInventoryState::requireStableId);
        completedDeliveryIds.forEach(PlayerInventoryState::requireStableId);
        for (String id : pendingItemRewards.keySet()) {
            if (completedDeliveryIds.contains(id)) {
                throw new IllegalArgumentException(
                        "Item delivery cannot be pending and completed: " + id
                );
            }
        }
    }

    public static PlayerInventoryState initial() {
        return new PlayerInventoryState(
                CURRENT_SCHEMA_VERSION,
                ProjectBackpackState.startingBackpack(),
                ProjectBackpackState.personalStorage(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Set.of()
        );
    }

    public boolean deliveryCompleted(String transactionId) {
        requireStableId(transactionId);
        return completedDeliveryIds.contains(transactionId);
    }

    public Optional<PendingItemReward> pendingReward(String transactionId) {
        requireStableId(transactionId);
        return Optional.ofNullable(pendingItemRewards.get(transactionId));
    }

    public DeliveryResult deliverImportantOnce(
            String transactionId,
            ProjectInventoryItem item
    ) {
        requireStableId(transactionId);
        Objects.requireNonNull(item, "item");

        if (completedDeliveryIds.contains(transactionId)) {
            return new DeliveryResult(this, DeliveryStatus.ALREADY_COMPLETED);
        }

        PendingItemReward existing = pendingItemRewards.get(transactionId);
        if (existing != null) {
            if (!existing.original().equals(item)) {
                throw new IllegalStateException(
                        "Cannot mutate payload of pending item delivery " + transactionId
                );
            }
            return claimPending(transactionId);
        }

        return deliverRemainder(
                transactionId,
                new PendingItemReward(item, item),
                true
        );
    }

    public DeliveryResult claimPending(String transactionId) {
        requireStableId(transactionId);
        PendingItemReward pending = pendingItemRewards.get(transactionId);
        if (pending == null) {
            if (completedDeliveryIds.contains(transactionId)) {
                return new DeliveryResult(this, DeliveryStatus.ALREADY_COMPLETED);
            }
            throw new IllegalStateException("No pending item reward: " + transactionId);
        }
        return deliverRemainder(transactionId, pending, false);
    }

    public MaterialInsertResult addMaterialToPouch(
            String materialId,
            int amount
    ) {
        requireStableId(materialId);
        if (amount <= 0) {
            throw new IllegalArgumentException("Material amount must be positive.");
        }

        int current = materialPouch.getOrDefault(materialId, 0);
        int room = MATERIAL_POUCH_CAP - current;
        int inserted = Math.min(room, amount);
        int overflow = amount - inserted;

        Map<String, Integer> nextPouch = new HashMap<>(materialPouch);
        if (inserted > 0) {
            nextPouch.put(materialId, current + inserted);
        }

        return new MaterialInsertResult(
                copy(backpack, personalStorage, nextPouch, materialVault, keyItems,
                        pendingItemRewards, completedDeliveryIds),
                inserted,
                overflow
        );
    }

    public boolean canAcceptMaterialInPouch(
            String materialId,
            int amount
    ) {
        requireStableId(materialId);
        if (amount <= 0) {
            throw new IllegalArgumentException("Material amount must be positive.");
        }
        int current = materialPouch.getOrDefault(materialId, 0);
        return amount <= MATERIAL_POUCH_CAP - current;
    }

    /**
     * Idempotent full-amount material delivery used by reconnect-safe field transactions.
     *
     * <p>Capacity failure is all-or-nothing. The caller owns receipt cleanup after its durable
     * source transaction has finalized.</p>
     */
    public MaterialDeliveryResult deliverMaterialToPouchOnce(
            String transactionId,
            String materialId,
            int amount
    ) {
        requireStableId(transactionId);
        requireStableId(materialId);
        if (amount <= 0) {
            throw new IllegalArgumentException("Material amount must be positive.");
        }
        if (completedDeliveryIds.contains(transactionId)) {
            return new MaterialDeliveryResult(
                    this,
                    MaterialDeliveryStatus.ALREADY_COMPLETED,
                    0
            );
        }
        if (!canAcceptMaterialInPouch(materialId, amount)) {
            return new MaterialDeliveryResult(
                    this,
                    MaterialDeliveryStatus.CAPACITY_BLOCKED,
                    0
            );
        }

        Map<String, Integer> nextPouch = new HashMap<>(materialPouch);
        nextPouch.put(materialId, nextPouch.getOrDefault(materialId, 0) + amount);
        Set<String> nextCompleted = new HashSet<>(completedDeliveryIds);
        nextCompleted.add(transactionId);

        return new MaterialDeliveryResult(
                copy(
                        backpack,
                        personalStorage,
                        Map.copyOf(nextPouch),
                        materialVault,
                        keyItems,
                        pendingItemRewards,
                        Set.copyOf(nextCompleted)
                ),
                MaterialDeliveryStatus.DELIVERED,
                amount
        );
    }

    public PlayerInventoryState forgetCompletedDeliveryReceipt(String transactionId) {
        requireStableId(transactionId);
        if (!completedDeliveryIds.contains(transactionId)) {
            return this;
        }
        Set<String> nextCompleted = new HashSet<>(completedDeliveryIds);
        nextCompleted.remove(transactionId);
        return copy(
                backpack,
                personalStorage,
                materialPouch,
                materialVault,
                keyItems,
                pendingItemRewards,
                Set.copyOf(nextCompleted)
        );
    }

    public PlayerInventoryState clearCompletedDeliveryIdsWithPrefix(String prefix) {
        if (prefix == null || prefix.isBlank() || prefix.indexOf(':') <= 0) {
            throw new IllegalArgumentException("Expected namespaced delivery-id prefix.");
        }
        Set<String> nextCompleted = new HashSet<>(completedDeliveryIds);
        boolean changed = nextCompleted.removeIf(id -> id.startsWith(prefix));
        if (!changed) {
            return this;
        }
        return copy(
                backpack,
                personalStorage,
                materialPouch,
                materialVault,
                keyItems,
                pendingItemRewards,
                Set.copyOf(nextCompleted)
        );
    }

    public MaterialConsumeResult consumeMaterial(
            String materialId,
            int amount,
            boolean settlementMayUseVault
    ) {
        requireStableId(materialId);
        if (amount <= 0) {
            throw new IllegalArgumentException("Material cost must be positive.");
        }

        int pouchAvailable = materialPouch.getOrDefault(materialId, 0);
        int vaultAvailable = settlementMayUseVault
                ? materialVault.getOrDefault(materialId, 0)
                : 0;
        if (pouchAvailable + vaultAvailable < amount) {
            return new MaterialConsumeResult(this, false, 0, 0);
        }

        int fromPouch = Math.min(pouchAvailable, amount);
        int remaining = amount - fromPouch;
        int fromVault = remaining;

        Map<String, Integer> nextPouch = new HashMap<>(materialPouch);
        setOrRemove(nextPouch, materialId, pouchAvailable - fromPouch);

        Map<String, Integer> nextVault = new HashMap<>(materialVault);
        if (fromVault > 0) {
            setOrRemove(
                    nextVault,
                    materialId,
                    materialVault.getOrDefault(materialId, 0) - fromVault
            );
        }

        PlayerInventoryState next = copy(
                backpack,
                personalStorage,
                Map.copyOf(nextPouch),
                Map.copyOf(nextVault),
                keyItems,
                pendingItemRewards,
                completedDeliveryIds
        );
        return new MaterialConsumeResult(next, true, fromPouch, fromVault);
    }

    public MaterialTransferResult depositMaterialToVault(
            String materialId,
            int requestedAmount
    ) {
        requireStableId(materialId);
        if (requestedAmount <= 0) {
            throw new IllegalArgumentException("requestedAmount must be positive.");
        }

        int pouchAvailable = materialPouch.getOrDefault(materialId, 0);
        int vaultCurrent = materialVault.getOrDefault(materialId, 0);
        int transferable = Math.min(
                Math.min(pouchAvailable, requestedAmount),
                MATERIAL_VAULT_CAP - vaultCurrent
        );
        if (transferable <= 0) {
            return new MaterialTransferResult(this, 0);
        }

        Map<String, Integer> nextPouch = new HashMap<>(materialPouch);
        setOrRemove(nextPouch, materialId, pouchAvailable - transferable);
        Map<String, Integer> nextVault = new HashMap<>(materialVault);
        nextVault.put(materialId, vaultCurrent + transferable);

        PlayerInventoryState next = copy(
                backpack,
                personalStorage,
                Map.copyOf(nextPouch),
                Map.copyOf(nextVault),
                keyItems,
                pendingItemRewards,
                completedDeliveryIds
        );
        return new MaterialTransferResult(next, transferable);
    }

    public PlayerInventoryState addKeyItem(String keyItemId) {
        requireStableId(keyItemId);
        if (keyItems.contains(keyItemId)) {
            return this;
        }
        Set<String> next = new HashSet<>(keyItems);
        next.add(keyItemId);
        return copy(
                backpack,
                personalStorage,
                materialPouch,
                materialVault,
                Set.copyOf(next),
                pendingItemRewards,
                completedDeliveryIds
        );
    }

    private DeliveryResult deliverRemainder(
            String transactionId,
            PendingItemReward pending,
            boolean newTransaction
    ) {
        ProjectBackpackState.InsertResult backpackInsert =
                backpack.insert(pending.remaining());
        ProjectBackpackState nextBackpack = backpackInsert.state();

        ProjectBackpackState nextStorage = personalStorage;
        Optional<ProjectInventoryItem> remaining = backpackInsert.remainder();
        if (remaining.isPresent()) {
            ProjectBackpackState.InsertResult storageInsert =
                    personalStorage.insert(remaining.orElseThrow());
            nextStorage = storageInsert.state();
            remaining = storageInsert.remainder();
        }

        Map<String, PendingItemReward> nextPending = new HashMap<>(pendingItemRewards);
        Set<String> nextCompleted = new HashSet<>(completedDeliveryIds);
        DeliveryStatus status;

        if (remaining.isEmpty()) {
            nextPending.remove(transactionId);
            nextCompleted.add(transactionId);
            status = DeliveryStatus.DELIVERED;
        } else {
            nextPending.put(
                    transactionId,
                    new PendingItemReward(
                            pending.original(),
                            remaining.orElseThrow()
                    )
            );
            status = newTransaction ? DeliveryStatus.PENDING : DeliveryStatus.STILL_PENDING;
        }

        PlayerInventoryState next = copy(
                nextBackpack,
                nextStorage,
                materialPouch,
                materialVault,
                keyItems,
                Map.copyOf(nextPending),
                Set.copyOf(nextCompleted)
        );
        return new DeliveryResult(next, status);
    }

    private PlayerInventoryState copy(
            ProjectBackpackState nextBackpack,
            ProjectBackpackState nextStorage,
            Map<String, Integer> nextPouch,
            Map<String, Integer> nextVault,
            Set<String> nextKeyItems,
            Map<String, PendingItemReward> nextPending,
            Set<String> nextCompleted
    ) {
        return new PlayerInventoryState(
                schemaVersion,
                nextBackpack,
                nextStorage,
                nextPouch,
                nextVault,
                nextKeyItems,
                nextPending,
                nextCompleted
        );
    }

    private static void setOrRemove(
            Map<String, Integer> values,
            String id,
            int amount
    ) {
        if (amount <= 0) {
            values.remove(id);
        } else {
            values.put(id, amount);
        }
    }

    private static void validateCountMap(
            Map<String, Integer> values,
            int cap,
            String name
    ) {
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            requireStableId(entry.getKey());
            if (entry.getValue() == null
                    || entry.getValue() <= 0
                    || entry.getValue() > cap) {
                throw new IllegalArgumentException(
                        name + " amount must be inside [1, " + cap + "] for " + entry.getKey()
                );
            }
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced id.");
        }
    }

    public record PendingItemReward(
            ProjectInventoryItem original,
            ProjectInventoryItem remaining
    ) {
        public static final Codec<PendingItemReward> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ProjectInventoryItem.CODEC.fieldOf("original").forGetter(PendingItemReward::original),
                        ProjectInventoryItem.CODEC.fieldOf("remaining").forGetter(PendingItemReward::remaining)
                ).apply(instance, PendingItemReward::new)
        );

        public PendingItemReward {
            Objects.requireNonNull(original, "original");
            Objects.requireNonNull(remaining, "remaining");
            if (!original.canMerge(remaining) && !original.equals(remaining)) {
                throw new IllegalArgumentException(
                        "Pending remainder must represent the same item payload."
                );
            }
            if (remaining.quantity() > original.quantity()) {
                throw new IllegalArgumentException(
                        "Pending remainder cannot exceed original reward quantity."
                );
            }
        }
    }

    public enum DeliveryStatus {
        DELIVERED,
        PENDING,
        STILL_PENDING,
        ALREADY_COMPLETED
    }

    public record DeliveryResult(
            PlayerInventoryState state,
            DeliveryStatus status
    ) {
        public DeliveryResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(status, "status");
        }
    }

    public record MaterialConsumeResult(
            PlayerInventoryState state,
            boolean consumed,
            int fromPouch,
            int fromVault
    ) {
        public MaterialConsumeResult {
            Objects.requireNonNull(state, "state");
            if (fromPouch < 0 || fromVault < 0) {
                throw new IllegalArgumentException("Material source counts must be non-negative.");
            }
            if (!consumed && (fromPouch != 0 || fromVault != 0)) {
                throw new IllegalArgumentException(
                        "Failed material transaction cannot consume partial cost."
                );
            }
        }
    }

    public record MaterialTransferResult(
            PlayerInventoryState state,
            int transferred
    ) {
        public MaterialTransferResult {
            Objects.requireNonNull(state, "state");
            if (transferred < 0) {
                throw new IllegalArgumentException("transferred must be non-negative.");
            }
        }
    }

    public enum MaterialDeliveryStatus {
        DELIVERED,
        ALREADY_COMPLETED,
        CAPACITY_BLOCKED
    }

    public record MaterialDeliveryResult(
            PlayerInventoryState state,
            MaterialDeliveryStatus status,
            int delivered
    ) {
        public MaterialDeliveryResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(status, "status");
            if (delivered < 0) {
                throw new IllegalArgumentException("delivered must be non-negative.");
            }
            if (status != MaterialDeliveryStatus.DELIVERED && delivered != 0) {
                throw new IllegalArgumentException(
                        "Non-delivered material transaction cannot report delivered quantity."
                );
            }
        }
    }

    public record MaterialInsertResult(
            PlayerInventoryState state,
            int inserted,
            int overflow
    ) {
        public MaterialInsertResult {
            Objects.requireNonNull(state, "state");
            if (inserted < 0 || overflow < 0) {
                throw new IllegalArgumentException("Material insert counts must be non-negative.");
            }
        }
    }
}
