package dev.moonseungjun.openworldrpg.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record R01NessaMarketState(
        int schemaVersion,
        Optional<Cycle> currentCycle
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01NessaMarketState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("nessa_market_schema_version")
                            .forGetter(R01NessaMarketState::schemaVersion),
                    Cycle.CODEC.optionalFieldOf("current_cycle")
                            .forGetter(R01NessaMarketState::currentCycle)
            ).apply(instance, R01NessaMarketState::new));

    public R01NessaMarketState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported Nessa market schema.");
        }
        currentCycle = Objects.requireNonNull(currentCycle, "currentCycle");
    }

    public static R01NessaMarketState initial() {
        return new R01NessaMarketState(
                CURRENT_SCHEMA_VERSION,
                Optional.empty()
        );
    }

    public R01NessaMarketState ensureCycle(
            long worldSeed,
            String playerUuid,
            long cycleIndex
    ) {
        if (currentCycle.isPresent()
                && currentCycle.orElseThrow().cycleIndex() == cycleIndex) {
            return this;
        }
        return new R01NessaMarketState(
                schemaVersion,
                Optional.of(new Cycle(
                        cycleIndex,
                        R01NessaMarketRules.generateCycle(
                                worldSeed,
                                playerUuid,
                                cycleIndex
                        ),
                        Set.of()
                ))
        );
    }

    public Optional<PurchasePlan> purchasePlan(
            String playerUuid,
            long expectedCycleIndex,
            int slotIndex
    ) {
        java.util.UUID.fromString(playerUuid);
        Cycle cycle = currentCycle.orElse(null);
        if (cycle == null
                || cycle.cycleIndex() != expectedCycleIndex
                || cycle.soldSlots().contains(slotIndex)) {
            return Optional.empty();
        }
        R01NessaMarketRules.StockItem item = cycle.item(slotIndex);
        return Optional.of(new PurchasePlan(
                transactionId(playerUuid, expectedCycleIndex, slotIndex),
                expectedCycleIndex,
                slotIndex,
                item
        ));
    }

    public R01NessaMarketState markSold(
            String playerUuid,
            long cycleIndex,
            int slotIndex,
            String transactionId
    ) {
        PurchasePlan plan = purchasePlan(
                playerUuid,
                cycleIndex,
                slotIndex
        ).orElse(null);
        if (plan == null) {
            Cycle cycle = currentCycle.orElse(null);
            if (cycle != null
                    && cycle.cycleIndex() == cycleIndex
                    && cycle.soldSlots().contains(slotIndex)
                    && transactionId.equals(
                            transactionId(playerUuid, cycleIndex, slotIndex)
                    )) {
                return this;
            }
            throw new IllegalStateException(
                    "Nessa purchase cannot commit against stale/sold stock."
            );
        }
        if (!plan.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Nessa purchase transaction id mismatch."
            );
        }

        Set<Integer> nextSold = new HashSet<>(
                currentCycle.orElseThrow().soldSlots()
        );
        nextSold.add(slotIndex);
        Cycle cycle = currentCycle.orElseThrow();
        return new R01NessaMarketState(
                schemaVersion,
                Optional.of(new Cycle(
                        cycle.cycleIndex(),
                        cycle.items(),
                        Set.copyOf(nextSold)
                ))
        );
    }

    public static String transactionId(
            String playerUuid,
            long cycleIndex,
            int slotIndex
    ) {
        java.util.UUID.fromString(playerUuid);
        if (cycleIndex < 0L
                || slotIndex < 1
                || slotIndex > R01NessaMarketRules.STOCK_SLOTS) {
            throw new IllegalArgumentException("Invalid Nessa purchase identity.");
        }
        return "openworld_rpg:nessa_purchase/"
                + playerUuid + "/" + cycleIndex + "/" + slotIndex;
    }

    public record Cycle(
            long cycleIndex,
            List<R01NessaMarketRules.StockItem> items,
            Set<Integer> soldSlots
    ) {
        private static final Codec<Set<Integer>> SOLD_CODEC =
                Codec.INT.listOf().xmap(
                        Set::copyOf,
                        value -> value.stream().sorted().toList()
                );

        public static final Codec<Cycle> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("cycle_index")
                                .forGetter(Cycle::cycleIndex),
                        R01NessaMarketRules.StockItem.CODEC.listOf()
                                .fieldOf("items")
                                .forGetter(Cycle::items),
                        SOLD_CODEC.fieldOf("sold_slots")
                                .forGetter(Cycle::soldSlots)
                ).apply(instance, Cycle::new));

        public Cycle {
            if (cycleIndex < 0L) {
                throw new IllegalArgumentException("Cycle index must be non-negative.");
            }
            items = List.copyOf(Objects.requireNonNull(items, "items"));
            soldSlots = Set.copyOf(Objects.requireNonNull(soldSlots, "soldSlots"));
            if (items.size() != R01NessaMarketRules.STOCK_SLOTS) {
                throw new IllegalArgumentException("Nessa cycle must have exactly five slots.");
            }
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).slotIndex() != i + 1) {
                    throw new IllegalArgumentException("Nessa stock slot order mismatch.");
                }
            }
            for (int slot : soldSlots) {
                if (slot < 1 || slot > R01NessaMarketRules.STOCK_SLOTS) {
                    throw new IllegalArgumentException("Invalid sold Nessa slot.");
                }
            }
        }

        public R01NessaMarketRules.StockItem item(int slotIndex) {
            if (slotIndex < 1 || slotIndex > items.size()) {
                throw new IllegalArgumentException("Invalid Nessa slot.");
            }
            return items.get(slotIndex - 1);
        }
    }

    public record PurchasePlan(
            String transactionId,
            long cycleIndex,
            int slotIndex,
            R01NessaMarketRules.StockItem item
    ) {
        public PurchasePlan {
            Objects.requireNonNull(transactionId, "transactionId");
            Objects.requireNonNull(item, "item");
        }
    }
}
