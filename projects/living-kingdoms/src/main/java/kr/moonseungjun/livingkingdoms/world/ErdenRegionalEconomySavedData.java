package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Persistent stocks, consumption, local trade and capital exports for the six regional villages. */
public final class ErdenRegionalEconomySavedData extends SavedData {
    public static final int SCHEMA_VERSION = 1;

    public record ResourceStock(String resource, long amount) {
        private static final Codec<ResourceStock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("resource").forGetter(ResourceStock::resource),
                Codec.LONG.fieldOf("amount").forGetter(ResourceStock::amount)
        ).apply(instance, ResourceStock::new));

        public ResourceStock {
            resource = resource == null ? "" : resource;
            amount = Math.max(0L, amount);
        }
    }

    public record SettlementState(
            String id,
            int x,
            int z,
            String industry,
            List<ResourceStock> stocks,
            long lastProcessedDay,
            long lastExportDay,
            long totalProduced,
            long totalConsumed,
            long totalTradedOut,
            long totalTradedIn,
            long totalExported,
            long shortageDays) {
        private static final Codec<SettlementState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(SettlementState::id),
                Codec.INT.fieldOf("x").forGetter(SettlementState::x),
                Codec.INT.fieldOf("z").forGetter(SettlementState::z),
                Codec.STRING.fieldOf("industry").forGetter(SettlementState::industry),
                ResourceStock.CODEC.listOf().optionalFieldOf("stocks", List.of()).forGetter(SettlementState::stocks),
                Codec.LONG.optionalFieldOf("last_processed_day", -1L).forGetter(SettlementState::lastProcessedDay),
                Codec.LONG.optionalFieldOf("last_export_day", -1L).forGetter(SettlementState::lastExportDay),
                Codec.LONG.optionalFieldOf("total_produced", 0L).forGetter(SettlementState::totalProduced),
                Codec.LONG.optionalFieldOf("total_consumed", 0L).forGetter(SettlementState::totalConsumed),
                Codec.LONG.optionalFieldOf("total_traded_out", 0L).forGetter(SettlementState::totalTradedOut),
                Codec.LONG.optionalFieldOf("total_traded_in", 0L).forGetter(SettlementState::totalTradedIn),
                Codec.LONG.optionalFieldOf("total_exported", 0L).forGetter(SettlementState::totalExported),
                Codec.LONG.optionalFieldOf("shortage_days", 0L).forGetter(SettlementState::shortageDays)
        ).apply(instance, SettlementState::new));

        public SettlementState {
            id = id == null ? "" : id;
            industry = industry == null ? "unknown" : industry;
            stocks = List.copyOf(stocks);
            totalProduced = Math.max(0L, totalProduced);
            totalConsumed = Math.max(0L, totalConsumed);
            totalTradedOut = Math.max(0L, totalTradedOut);
            totalTradedIn = Math.max(0L, totalTradedIn);
            totalExported = Math.max(0L, totalExported);
            shortageDays = Math.max(0L, shortageDays);
        }

        public long stock(String resource) {
            for (ResourceStock stock : stocks) {
                if (stock.resource().equals(resource)) return stock.amount();
            }
            return 0L;
        }

        public SettlementState withStock(String resource, long amount) {
            Map<String, Long> values = new LinkedHashMap<>();
            for (ResourceStock stock : stocks) values.put(stock.resource(), stock.amount());
            if (amount <= 0L) values.remove(resource);
            else values.put(resource, amount);
            List<ResourceStock> replacement = values.entrySet().stream()
                    .map(entry -> new ResourceStock(entry.getKey(), entry.getValue()))
                    .toList();
            return copy(replacement, lastProcessedDay, lastExportDay,
                    totalProduced, totalConsumed, totalTradedOut, totalTradedIn,
                    totalExported, shortageDays);
        }

        public SettlementState addStock(String resource, long delta) {
            return withStock(resource, Math.max(0L, stock(resource) + delta));
        }

        public SettlementState recordDay(long day, long produced, long consumed, boolean shortage) {
            return copy(stocks, day, lastExportDay,
                    totalProduced + Math.max(0L, produced),
                    totalConsumed + Math.max(0L, consumed),
                    totalTradedOut, totalTradedIn, totalExported,
                    shortageDays + (shortage ? 1L : 0L));
        }

        public SettlementState recordTradeOut(String resource, long amount) {
            long safe = Math.min(stock(resource), Math.max(0L, amount));
            SettlementState reduced = addStock(resource, -safe);
            return reduced.copy(reduced.stocks, reduced.lastProcessedDay, reduced.lastExportDay,
                    reduced.totalProduced, reduced.totalConsumed,
                    reduced.totalTradedOut + safe, reduced.totalTradedIn,
                    reduced.totalExported, reduced.shortageDays);
        }

        public SettlementState recordTradeIn(String resource, long amount) {
            long safe = Math.max(0L, amount);
            SettlementState increased = addStock(resource, safe);
            return increased.copy(increased.stocks, increased.lastProcessedDay, increased.lastExportDay,
                    increased.totalProduced, increased.totalConsumed,
                    increased.totalTradedOut, increased.totalTradedIn + safe,
                    increased.totalExported, increased.shortageDays);
        }

        public SettlementState recordExport(String resource, long amount, long day) {
            long safe = Math.min(stock(resource), Math.max(0L, amount));
            SettlementState reduced = addStock(resource, -safe);
            return reduced.copy(reduced.stocks, reduced.lastProcessedDay, day,
                    reduced.totalProduced, reduced.totalConsumed,
                    reduced.totalTradedOut, reduced.totalTradedIn,
                    reduced.totalExported + safe, reduced.shortageDays);
        }

        public SettlementState markExportDay(long day) {
            return copy(stocks, lastProcessedDay, day,
                    totalProduced, totalConsumed, totalTradedOut, totalTradedIn,
                    totalExported, shortageDays);
        }

        private SettlementState copy(
                List<ResourceStock> replacementStocks,
                long processedDay,
                long exportDay,
                long produced,
                long consumed,
                long tradedOut,
                long tradedIn,
                long exported,
                long shortages) {
            return new SettlementState(
                    id, x, z, industry, replacementStocks,
                    processedDay, exportDay, produced, consumed,
                    tradedOut, tradedIn, exported, shortages);
        }
    }

    public record TradeShipment(
            String id,
            String sourceId,
            String targetId,
            String resource,
            long amount,
            long departureTick,
            long arrivalTick,
            String status,
            int routeMetres) {
        private static final Codec<TradeShipment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(TradeShipment::id),
                Codec.STRING.fieldOf("source_id").forGetter(TradeShipment::sourceId),
                Codec.STRING.fieldOf("target_id").forGetter(TradeShipment::targetId),
                Codec.STRING.fieldOf("resource").forGetter(TradeShipment::resource),
                Codec.LONG.fieldOf("amount").forGetter(TradeShipment::amount),
                Codec.LONG.fieldOf("departure_tick").forGetter(TradeShipment::departureTick),
                Codec.LONG.fieldOf("arrival_tick").forGetter(TradeShipment::arrivalTick),
                Codec.STRING.fieldOf("status").forGetter(TradeShipment::status),
                Codec.INT.fieldOf("route_metres").forGetter(TradeShipment::routeMetres)
        ).apply(instance, TradeShipment::new));

        public TradeShipment {
            id = id == null ? "" : id;
            sourceId = sourceId == null ? "" : sourceId;
            targetId = targetId == null ? "" : targetId;
            resource = resource == null ? "" : resource;
            amount = Math.max(0L, amount);
            arrivalTick = Math.max(departureTick, arrivalTick);
            status = status == null || status.isBlank() ? "in_transit" : status;
            routeMetres = Math.max(1, routeMetres);
        }

        public boolean terminal() {
            return status.equals("arrived") || status.equals("failed");
        }

        public TradeShipment withStatus(String replacement) {
            return new TradeShipment(
                    id, sourceId, targetId, resource, amount,
                    departureTick, arrivalTick, replacement, routeMetres);
        }
    }

    private static final Codec<ErdenRegionalEconomySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("economy_revision", 0).forGetter(data -> data.economyRevision),
            SettlementState.CODEC.listOf().optionalFieldOf("settlements", List.of())
                    .forGetter(data -> List.copyOf(data.settlements)),
            TradeShipment.CODEC.listOf().optionalFieldOf("trade_shipments", List.of())
                    .forGetter(data -> List.copyOf(data.tradeShipments)),
            Codec.STRING.listOf().optionalFieldOf("materialized_storages", List.of())
                    .forGetter(data -> List.copyOf(data.materializedStorages)),
            Codec.LONG.optionalFieldOf("next_serial", 0L).forGetter(data -> data.nextSerial)
    ).apply(instance, ErdenRegionalEconomySavedData::new));

    public static final SavedDataType<ErdenRegionalEconomySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_regional_economy"),
            level -> new ErdenRegionalEconomySavedData(),
            level -> CODEC
    );

    private int economyRevision;
    private final List<SettlementState> settlements;
    private final List<TradeShipment> tradeShipments;
    private final Set<String> materializedStorages;
    private long nextSerial;

    public ErdenRegionalEconomySavedData() {
        this(0, List.of(), List.of(), List.of(), 0L);
    }

    private ErdenRegionalEconomySavedData(
            int economyRevision,
            List<SettlementState> settlements,
            List<TradeShipment> tradeShipments,
            List<String> materializedStorages,
            long nextSerial) {
        this.economyRevision = Math.max(0, economyRevision);
        this.settlements = new ArrayList<>(settlements);
        this.tradeShipments = new ArrayList<>(tradeShipments);
        this.materializedStorages = new LinkedHashSet<>(materializedStorages);
        this.nextSerial = Math.max(0L, nextSerial);
    }

    public boolean hasEconomy(int revision, int expectedSettlements) {
        return economyRevision == revision && settlements.size() == expectedSettlements;
    }

    public void initialize(int revision, List<SettlementState> replacement) {
        economyRevision = revision;
        settlements.clear();
        settlements.addAll(replacement);
        tradeShipments.clear();
        materializedStorages.clear();
        nextSerial = 0L;
        setDirty();
    }

    public List<SettlementState> settlements() {
        return List.copyOf(settlements);
    }

    public List<TradeShipment> tradeShipments() {
        return List.copyOf(tradeShipments);
    }

    public SettlementState settlement(String id) {
        for (SettlementState state : settlements) if (state.id().equals(id)) return state;
        return null;
    }

    public void replaceSettlement(SettlementState replacement) {
        for (int index = 0; index < settlements.size(); index++) {
            if (!settlements.get(index).id().equals(replacement.id())) continue;
            if (!settlements.get(index).equals(replacement)) {
                settlements.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public String nextTradeId(long day) {
        nextSerial++;
        setDirty();
        return "erden_regional_trade_%06d_%04d".formatted(Math.max(0L, day), nextSerial);
    }

    public void addTrade(TradeShipment shipment) {
        tradeShipments.add(shipment);
        setDirty();
    }

    public void replaceTrade(TradeShipment replacement) {
        for (int index = 0; index < tradeShipments.size(); index++) {
            if (!tradeShipments.get(index).id().equals(replacement.id())) continue;
            if (!tradeShipments.get(index).equals(replacement)) {
                tradeShipments.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public void pruneTrades(long minimumArrivalTick) {
        if (tradeShipments.removeIf(shipment -> shipment.terminal()
                && shipment.arrivalTick() < minimumArrivalTick)) {
            setDirty();
        }
    }

    public boolean storageMaterialized(String settlementId) {
        return materializedStorages.contains(settlementId);
    }

    public void markStorageMaterialized(String settlementId) {
        if (materializedStorages.add(settlementId)) setDirty();
    }

    public int materializedStorageCount() {
        return materializedStorages.size();
    }

    public long totalProduced() {
        return settlements.stream().mapToLong(SettlementState::totalProduced).sum();
    }

    public long totalConsumed() {
        return settlements.stream().mapToLong(SettlementState::totalConsumed).sum();
    }

    public long totalTradedOut() {
        return settlements.stream().mapToLong(SettlementState::totalTradedOut).sum();
    }

    public long totalTradedIn() {
        return settlements.stream().mapToLong(SettlementState::totalTradedIn).sum();
    }

    public long totalExported() {
        return settlements.stream().mapToLong(SettlementState::totalExported).sum();
    }

    public long totalShortageDays() {
        return settlements.stream().mapToLong(SettlementState::shortageDays).sum();
    }

    public int activeTradeCount() {
        int total = 0;
        for (TradeShipment shipment : tradeShipments) if (shipment.status().equals("in_transit")) total++;
        return total;
    }
}
