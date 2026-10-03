package dev.moonseungjun.openworldrpg.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

public record R01CraftingState(
        int schemaVersion,
        long nextCraftSerial,
        Optional<PendingCraft> pending
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01CraftingState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("r01_crafting_schema_version")
                            .forGetter(R01CraftingState::schemaVersion),
                    Codec.LONG.fieldOf("next_craft_serial")
                            .forGetter(R01CraftingState::nextCraftSerial),
                    PendingCraft.CODEC.optionalFieldOf("pending_craft")
                            .forGetter(R01CraftingState::pending)
            ).apply(instance, R01CraftingState::new));

    public R01CraftingState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported R01 crafting schema.");
        }
        if (nextCraftSerial < 0L) {
            throw new IllegalArgumentException("nextCraftSerial must be non-negative.");
        }
        pending = Objects.requireNonNull(pending, "pending");
    }

    public static R01CraftingState initial() {
        return new R01CraftingState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty()
        );
    }

    public BeginResult begin(
            String playerUuid,
            String recipeId,
            int quantity,
            long goldCost
    ) {
        return begin(
                playerUuid,
                recipeId,
                quantity,
                goldCost,
                CraftContext.SETTLEMENT
        );
    }

    public BeginResult begin(
            String playerUuid,
            String recipeId,
            int quantity,
            long goldCost,
            CraftContext context
    ) {
        java.util.UUID.fromString(playerUuid);
        requireId(recipeId);
        context = Objects.requireNonNull(context, "context");
        if (quantity <= 0 || goldCost < 0L) {
            throw new IllegalArgumentException(
                    "Craft quantity must be positive and Gold cost non-negative."
            );
        }
        if (pending.isPresent()) {
            return new BeginResult(this, pending.orElseThrow(), false);
        }

        String transactionId = "openworld_rpg:craft/"
                + playerUuid + "/" + nextCraftSerial;
        PendingCraft craft = new PendingCraft(
                transactionId,
                recipeId,
                quantity,
                goldCost,
                context
        );
        return new BeginResult(
                new R01CraftingState(
                        schemaVersion,
                        Math.addExact(nextCraftSerial, 1L),
                        Optional.of(craft)
                ),
                craft,
                true
        );
    }

    public R01CraftingState clearPending(String transactionId) {
        requireId(transactionId);
        if (pending.isEmpty()) {
            return this;
        }
        if (!pending.orElseThrow().transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "R01 crafting pending transaction mismatch."
            );
        }
        return new R01CraftingState(
                schemaVersion,
                nextCraftSerial,
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

    public record PendingCraft(
            String transactionId,
            String recipeId,
            int quantity,
            long goldCost,
            CraftContext context
    ) {
        public static final Codec<PendingCraft> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingCraft::transactionId),
                        Codec.STRING.fieldOf("recipe_id")
                                .forGetter(PendingCraft::recipeId),
                        Codec.INT.fieldOf("quantity")
                                .forGetter(PendingCraft::quantity),
                        Codec.LONG.fieldOf("gold_cost")
                                .forGetter(PendingCraft::goldCost),
                        CraftContext.CODEC.optionalFieldOf(
                                "craft_context",
                                CraftContext.SETTLEMENT
                        ).forGetter(PendingCraft::context)
                ).apply(instance, PendingCraft::new));

        public PendingCraft(
                String transactionId,
                String recipeId,
                int quantity,
                long goldCost
        ) {
            this(
                    transactionId,
                    recipeId,
                    quantity,
                    goldCost,
                    CraftContext.SETTLEMENT
            );
        }

        public PendingCraft {
            requireId(transactionId);
            requireId(recipeId);
            if (quantity <= 0 || goldCost < 0L) {
                throw new IllegalArgumentException(
                        "Pending craft quantity/cost is invalid."
                );
            }
            context = Objects.requireNonNull(context, "context");
        }
    }

    public enum CraftContext {
        SETTLEMENT,
        CAMP;

        public static final Codec<CraftContext> CODEC =
                Codec.STRING.xmap(
                        value -> switch (value) {
                            case "settlement" -> SETTLEMENT;
                            case "camp" -> CAMP;
                            default -> throw new IllegalArgumentException(
                                    "Unknown R01 craft context: " + value
                            );
                        },
                        value -> switch (value) {
                            case SETTLEMENT -> "settlement";
                            case CAMP -> "camp";
                        }
                );

        public boolean mayUseMaterialVault() {
            return this == SETTLEMENT;
        }
    }

    public record BeginResult(
            R01CraftingState state,
            PendingCraft craft,
            boolean created
    ) {
        public BeginResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(craft, "craft");
        }
    }
}
