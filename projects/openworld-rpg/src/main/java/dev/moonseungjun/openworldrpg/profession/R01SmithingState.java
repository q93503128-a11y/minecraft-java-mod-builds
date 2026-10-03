package dev.moonseungjun.openworldrpg.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record R01SmithingState(
        int schemaVersion,
        long nextCraftSerial,
        Optional<PendingSmith> pending,
        Set<String> refinedBaseCrafts,
        boolean superiorRecipesUnlocked
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Codec<Set<String>> STRING_SET_CODEC =
            Codec.STRING.listOf().xmap(
                    Set::copyOf,
                    value -> value.stream().sorted().toList()
            );

    public static final Codec<R01SmithingState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("r01_smithing_schema_version")
                            .forGetter(R01SmithingState::schemaVersion),
                    Codec.LONG.fieldOf("next_craft_serial")
                            .forGetter(R01SmithingState::nextCraftSerial),
                    PendingSmith.CODEC.optionalFieldOf("pending_smith")
                            .forGetter(R01SmithingState::pending),
                    STRING_SET_CODEC.optionalFieldOf(
                            "refined_base_crafts",
                            Set.of()
                    ).forGetter(R01SmithingState::refinedBaseCrafts),
                    Codec.BOOL.optionalFieldOf(
                            "superior_recipes_unlocked",
                            false
                    ).forGetter(R01SmithingState::superiorRecipesUnlocked)
            ).apply(instance, R01SmithingState::new));

    public R01SmithingState(
            int schemaVersion,
            long nextCraftSerial,
            Optional<PendingSmith> pending,
            Set<String> refinedBaseCrafts
    ) {
        this(
                schemaVersion,
                nextCraftSerial,
                pending,
                refinedBaseCrafts,
                false
        );
    }

    public R01SmithingState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 smithing schema."
            );
        }
        if (nextCraftSerial < 0L) {
            throw new IllegalArgumentException(
                    "Smithing serial must be non-negative."
            );
        }
        pending = Objects.requireNonNull(pending, "pending");
        refinedBaseCrafts = Set.copyOf(
                Objects.requireNonNull(
                        refinedBaseCrafts,
                        "refinedBaseCrafts"
                )
        );
        refinedBaseCrafts.forEach(R01SmithingState::requireId);
    }

    public static R01SmithingState initial() {
        return new R01SmithingState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty(),
                Set.of(),
                false
        );
    }

    public BeginResult begin(
            String playerUuid,
            R01SmithingRecipe recipe,
            ProjectInventoryItem output
    ) {
        java.util.UUID.fromString(playerUuid);
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(output, "output");
        if (pending.isPresent()) {
            return new BeginResult(
                    this,
                    pending.orElseThrow(),
                    false
            );
        }

        String transactionId = "openworld_rpg:smith/"
                + playerUuid + "/" + nextCraftSerial;
        PendingSmith craft = new PendingSmith(
                transactionId,
                recipe.id(),
                recipe.goldCost(),
                output
        );
        return new BeginResult(
                new R01SmithingState(
                        schemaVersion,
                        Math.addExact(nextCraftSerial, 1L),
                        Optional.of(craft),
                        refinedBaseCrafts,
                        superiorRecipesUnlocked
                ),
                craft,
                true
        );
    }

    public R01SmithingState recordRefinedBaseCraft(
            String baseId
    ) {
        requireId(baseId);
        if (refinedBaseCrafts.contains(baseId)) {
            return this;
        }
        Set<String> next = new HashSet<>(refinedBaseCrafts);
        next.add(baseId);
        return new R01SmithingState(
                schemaVersion,
                nextCraftSerial,
                pending,
                Set.copyOf(next),
                superiorRecipesUnlocked
        );
    }

    public R01SmithingState unlockSuperiorRecipes() {
        if (superiorRecipesUnlocked) {
            return this;
        }
        return new R01SmithingState(
                schemaVersion,
                nextCraftSerial,
                pending,
                refinedBaseCrafts,
                true
        );
    }

    public R01SmithingState clearPending(String transactionId) {
        requireId(transactionId);
        if (pending.isEmpty()) {
            return this;
        }
        if (!pending.orElseThrow().transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "R01 smithing pending transaction mismatch."
            );
        }
        return new R01SmithingState(
                schemaVersion,
                nextCraftSerial,
                Optional.empty(),
                refinedBaseCrafts,
                superiorRecipesUnlocked
        );
    }

    private static void requireId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected namespaced id."
            );
        }
    }

    public record PendingSmith(
            String transactionId,
            String recipeId,
            long goldCost,
            ProjectInventoryItem output
    ) {
        public static final Codec<PendingSmith> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingSmith::transactionId),
                        Codec.STRING.fieldOf("recipe_id")
                                .forGetter(PendingSmith::recipeId),
                        Codec.LONG.fieldOf("gold_cost")
                                .forGetter(PendingSmith::goldCost),
                        ProjectInventoryItem.CODEC.fieldOf("output")
                                .forGetter(PendingSmith::output)
                ).apply(instance, PendingSmith::new));

        public PendingSmith {
            requireId(transactionId);
            requireId(recipeId);
            if (goldCost < 0L) {
                throw new IllegalArgumentException(
                        "Smithing Gold cost cannot be negative."
                );
            }
            Objects.requireNonNull(output, "output");
            if (output.equipmentProjection().isEmpty()
                    || output.quantity() != 1) {
                throw new IllegalArgumentException(
                        "Smithing output must be one equipment item."
                );
            }
        }
    }

    public record BeginResult(
            R01SmithingState state,
            PendingSmith craft,
            boolean created
    ) {
        public BeginResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(craft, "craft");
        }
    }
}
