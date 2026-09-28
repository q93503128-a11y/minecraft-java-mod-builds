package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentAffixCatalogLoader;
import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentMaterializer;
import dev.moonseungjun.openworldrpg.equipment.OrdinaryEquipmentParameterizedAffixLoader;
import dev.moonseungjun.openworldrpg.equipment.R01OrdinaryEquipmentBaseCatalogLoader;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Exact bridge from a persisted Nessa stock slot into the shared ordinary-equipment materializer.
 *
 * <p>It reports canonical runtime blockers instead of silently deleting unsupported affixes.</p>
 */
public final class R01NessaEquipmentMaterialization {
    private R01NessaEquipmentMaterialization() {
    }

    public static Resolution resolve(
            R01NessaMarketRules.StockItem stock
    ) {
        Objects.requireNonNull(stock, "stock");

        var bases = R01OrdinaryEquipmentBaseCatalogLoader.loadBundled();
        var staticCatalog = OrdinaryEquipmentAffixCatalogLoader.loadBundled();
        var parameterized =
                OrdinaryEquipmentParameterizedAffixLoader.loadBundled();

        Set<String> blockers = bases.runtimeBlockers(
                stock.baseId(),
                staticCatalog,
                parameterized
        );
        if (!blockers.isEmpty()) {
            return new Resolution(
                    ResolutionStatus.RUNTIME_AFFIX_BLOCKED,
                    blockers,
                    Optional.empty()
            );
        }

        var profile = bases.materializerProfile(
                stock.baseId(),
                stock.equipmentSlot()
        );
        var eligible = bases.eligibleAffixes(
                stock.baseId(),
                staticCatalog,
                parameterized
        );
        OrdinaryEquipmentMaterializer.MaterializedEquipment materialized =
                OrdinaryEquipmentMaterializer.materialize(
                        new OrdinaryEquipmentMaterializer.MaterializationRequest(
                                profile,
                                stock.grade(),
                                stock.itemLevel(),
                                eligible,
                                stock.affixSeed(),
                                R01NessaMarketRules.sellBackGold(
                                        stock.priceGold()
                                )
                        ),
                        staticCatalog
                );

        return new Resolution(
                ResolutionStatus.READY,
                Set.of(),
                Optional.of(materialized.inventoryItem())
        );
    }

    public enum ResolutionStatus {
        READY,
        RUNTIME_AFFIX_BLOCKED
    }

    public record Resolution(
            ResolutionStatus status,
            Set<String> blockers,
            Optional<ProjectInventoryItem> item
    ) {
        public Resolution {
            Objects.requireNonNull(status, "status");
            blockers = Set.copyOf(
                    Objects.requireNonNull(blockers, "blockers")
            );
            item = Objects.requireNonNull(item, "item");
            if (status == ResolutionStatus.READY) {
                if (item.isEmpty() || !blockers.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Ready Nessa materialization requires exactly one item and no blockers."
                    );
                }
            } else if (item.isPresent() || blockers.isEmpty()) {
                throw new IllegalArgumentException(
                        "Blocked Nessa materialization requires blockers and no item."
                );
            }
        }
    }
}
