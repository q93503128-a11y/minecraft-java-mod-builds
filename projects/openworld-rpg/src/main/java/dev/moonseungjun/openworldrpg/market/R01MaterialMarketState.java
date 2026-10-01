package dev.moonseungjun.openworldrpg.market;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record R01MaterialMarketState(
        int schemaVersion,
        long nextSaleSerial,
        Optional<PendingSale> pending
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01MaterialMarketState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("r01_material_market_schema_version")
                            .forGetter(R01MaterialMarketState::schemaVersion),
                    Codec.LONG.fieldOf("next_sale_serial")
                            .forGetter(R01MaterialMarketState::nextSaleSerial),
                    PendingSale.CODEC.optionalFieldOf("pending_sale")
                            .forGetter(R01MaterialMarketState::pending)
            ).apply(instance, R01MaterialMarketState::new));

    public R01MaterialMarketState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 material-market schema."
            );
        }
        if (nextSaleSerial < 0L) {
            throw new IllegalArgumentException(
                    "nextSaleSerial must be non-negative."
            );
        }
        pending = Objects.requireNonNull(pending, "pending");
    }

    public static R01MaterialMarketState initial() {
        return new R01MaterialMarketState(
                CURRENT_SCHEMA_VERSION,
                0L,
                Optional.empty()
        );
    }

    public BeginResult begin(
            String playerUuid,
            Map<String, Integer> materials,
            long gold,
            boolean bulk
    ) {
        java.util.UUID.fromString(playerUuid);
        if (pending.isPresent()) {
            return new BeginResult(this, pending.orElseThrow(), false);
        }
        PendingSale sale = new PendingSale(
                "openworld_rpg:nessa_material_sale/"
                        + playerUuid + "/" + nextSaleSerial,
                materials,
                gold,
                bulk
        );
        return new BeginResult(
                new R01MaterialMarketState(
                        schemaVersion,
                        Math.addExact(nextSaleSerial, 1L),
                        Optional.of(sale)
                ),
                sale,
                true
        );
    }

    public R01MaterialMarketState clearPending(String transactionId) {
        requireId(transactionId);
        if (pending.isEmpty()) {
            return this;
        }
        if (!pending.orElseThrow().transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "R01 material sale transaction mismatch."
            );
        }
        return new R01MaterialMarketState(
                schemaVersion,
                nextSaleSerial,
                Optional.empty()
        );
    }

    public record PendingSale(
            String transactionId,
            Map<String, Integer> materials,
            long gold,
            boolean bulk
    ) {
        public static final Codec<PendingSale> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingSale::transactionId),
                        Codec.unboundedMap(Codec.STRING, Codec.INT)
                                .fieldOf("materials")
                                .forGetter(PendingSale::materials),
                        Codec.LONG.fieldOf("gold")
                                .forGetter(PendingSale::gold),
                        Codec.BOOL.fieldOf("bulk")
                                .forGetter(PendingSale::bulk)
                ).apply(instance, PendingSale::new));

        public PendingSale {
            requireId(transactionId);
            materials = Map.copyOf(
                    Objects.requireNonNull(materials, "materials")
            );
            if (materials.isEmpty() || gold <= 0L) {
                throw new IllegalArgumentException(
                        "Material sale must contain materials and positive Gold."
                );
            }
            for (Map.Entry<String, Integer> entry : materials.entrySet()) {
                requireId(entry.getKey());
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    throw new IllegalArgumentException(
                            "Material sale quantities must be positive."
                    );
                }
            }
        }
    }

    public record BeginResult(
            R01MaterialMarketState state,
            PendingSale sale,
            boolean created
    ) {
        public BeginResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(sale, "sale");
        }
    }

    private static void requireId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable namespaced id.");
        }
    }
}
