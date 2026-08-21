package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistent rural production nodes and shipment escrow feeding the Erden capital. */
public final class ErdenKingdomSupplySavedData extends SavedData {
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

    public record NodeState(
            String id,
            int x,
            int z,
            String role,
            List<ResourceStock> stocks,
            long lastProducedDay,
            long totalProduced,
            long blockedDays) {
        private static final Codec<NodeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(NodeState::id),
                Codec.INT.fieldOf("x").forGetter(NodeState::x),
                Codec.INT.fieldOf("z").forGetter(NodeState::z),
                Codec.STRING.fieldOf("role").forGetter(NodeState::role),
                ResourceStock.CODEC.listOf().optionalFieldOf("stocks", List.of()).forGetter(NodeState::stocks),
                Codec.LONG.optionalFieldOf("last_produced_day", -1L).forGetter(NodeState::lastProducedDay),
                Codec.LONG.optionalFieldOf("total_produced", 0L).forGetter(NodeState::totalProduced),
                Codec.LONG.optionalFieldOf("blocked_days", 0L).forGetter(NodeState::blockedDays)
        ).apply(instance, NodeState::new));

        public NodeState {
            id = id == null ? "" : id;
            role = role == null ? "unknown" : role;
            stocks = List.copyOf(stocks);
            totalProduced = Math.max(0L, totalProduced);
            blockedDays = Math.max(0L, blockedDays);
        }

        public long stock(String resource) {
            for (ResourceStock entry : stocks) {
                if (entry.resource().equals(resource)) return entry.amount();
            }
            return 0L;
        }

        public NodeState withStock(String resource, long amount) {
            Map<String, Long> values = new LinkedHashMap<>();
            for (ResourceStock entry : stocks) values.put(entry.resource(), entry.amount());
            if (amount <= 0L) values.remove(resource);
            else values.put(resource, amount);
            List<ResourceStock> replacement = values.entrySet().stream()
                    .map(entry -> new ResourceStock(entry.getKey(), entry.getValue()))
                    .toList();
            return new NodeState(
                    id, x, z, role, replacement,
                    lastProducedDay, totalProduced, blockedDays);
        }

        public NodeState addStock(String resource, long delta) {
            return withStock(resource, Math.max(0L, stock(resource) + delta));
        }

        public NodeState produce(String resource, long amount, long day) {
            long safe = Math.max(0L, amount);
            NodeState stocked = addStock(resource, safe);
            return new NodeState(
                    stocked.id, stocked.x, stocked.z, stocked.role, stocked.stocks,
                    day, stocked.totalProduced + safe, stocked.blockedDays);
        }

        public NodeState markBlocked() {
            return new NodeState(
                    id, x, z, role, stocks,
                    lastProducedDay, totalProduced, blockedDays + 1L);
        }
    }

    public record ShipmentState(
            String id,
            String sourceId,
            String warehouseId,
            String resource,
            long amount,
            long departureTick,
            long arrivalTick,
            String status,
            String mode,
            int routeMetres,
            boolean openingConvoy) {
        private static final Codec<ShipmentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(ShipmentState::id),
                Codec.STRING.fieldOf("source_id").forGetter(ShipmentState::sourceId),
                Codec.STRING.fieldOf("warehouse_id").forGetter(ShipmentState::warehouseId),
                Codec.STRING.fieldOf("resource").forGetter(ShipmentState::resource),
                Codec.LONG.fieldOf("amount").forGetter(ShipmentState::amount),
                Codec.LONG.fieldOf("departure_tick").forGetter(ShipmentState::departureTick),
                Codec.LONG.fieldOf("arrival_tick").forGetter(ShipmentState::arrivalTick),
                Codec.STRING.fieldOf("status").forGetter(ShipmentState::status),
                Codec.STRING.fieldOf("mode").forGetter(ShipmentState::mode),
                Codec.INT.fieldOf("route_metres").forGetter(ShipmentState::routeMetres),
                Codec.BOOL.optionalFieldOf("opening_convoy", false).forGetter(ShipmentState::openingConvoy)
        ).apply(instance, ShipmentState::new));

        public ShipmentState {
            id = id == null ? "" : id;
            sourceId = sourceId == null ? "" : sourceId;
            warehouseId = warehouseId == null ? "" : warehouseId;
            resource = resource == null ? "" : resource;
            amount = Math.max(0L, amount);
            arrivalTick = Math.max(departureTick, arrivalTick);
            status = status == null || status.isBlank() ? "in_transit" : status;
            mode = mode == null || mode.isBlank() ? "wagon" : mode;
            routeMetres = Math.max(1, routeMetres);
        }

        public boolean terminal() {
            return status.equals("arrived") || status.equals("returned") || status.equals("failed");
        }

        public ShipmentState withStatus(String replacement) {
            return new ShipmentState(
                    id, sourceId, warehouseId, resource, amount,
                    departureTick, arrivalTick, replacement, mode, routeMetres, openingConvoy);
        }
    }

    private static final Codec<ErdenKingdomSupplySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("supply_revision", 0).forGetter(data -> data.supplyRevision),
            Codec.LONG.optionalFieldOf("last_processed_day", -1L).forGetter(data -> data.lastProcessedDay),
            NodeState.CODEC.listOf().optionalFieldOf("nodes", List.of()).forGetter(data -> List.copyOf(data.nodes)),
            ShipmentState.CODEC.listOf().optionalFieldOf("shipments", List.of()).forGetter(data -> List.copyOf(data.shipments)),
            Codec.LONG.optionalFieldOf("next_serial", 0L).forGetter(data -> data.nextSerial),
            Codec.LONG.optionalFieldOf("total_produced", 0L).forGetter(data -> data.totalProduced),
            Codec.LONG.optionalFieldOf("total_dispatched", 0L).forGetter(data -> data.totalDispatched),
            Codec.LONG.optionalFieldOf("total_received", 0L).forGetter(data -> data.totalReceived),
            Codec.LONG.optionalFieldOf("total_blocked", 0L).forGetter(data -> data.totalBlocked),
            Codec.LONG.optionalFieldOf("opening_convoys", 0L).forGetter(data -> data.openingConvoys)
    ).apply(instance, ErdenKingdomSupplySavedData::new));

    public static final SavedDataType<ErdenKingdomSupplySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_kingdom_supply"),
            level -> new ErdenKingdomSupplySavedData(),
            level -> CODEC
    );

    private int supplyRevision;
    private long lastProcessedDay;
    private final List<NodeState> nodes;
    private final List<ShipmentState> shipments;
    private long nextSerial;
    private long totalProduced;
    private long totalDispatched;
    private long totalReceived;
    private long totalBlocked;
    private long openingConvoys;

    public ErdenKingdomSupplySavedData() {
        this(0, -1L, List.of(), List.of(), 0L, 0L, 0L, 0L, 0L, 0L);
    }

    private ErdenKingdomSupplySavedData(
            int supplyRevision,
            long lastProcessedDay,
            List<NodeState> nodes,
            List<ShipmentState> shipments,
            long nextSerial,
            long totalProduced,
            long totalDispatched,
            long totalReceived,
            long totalBlocked,
            long openingConvoys) {
        this.supplyRevision = Math.max(0, supplyRevision);
        this.lastProcessedDay = lastProcessedDay;
        this.nodes = new ArrayList<>(nodes);
        this.shipments = new ArrayList<>(shipments);
        this.nextSerial = Math.max(0L, nextSerial);
        this.totalProduced = Math.max(0L, totalProduced);
        this.totalDispatched = Math.max(0L, totalDispatched);
        this.totalReceived = Math.max(0L, totalReceived);
        this.totalBlocked = Math.max(0L, totalBlocked);
        this.openingConvoys = Math.max(0L, openingConvoys);
    }

    public boolean hasSupply(int revision, int expectedNodes) {
        return supplyRevision == revision && nodes.size() == expectedNodes;
    }

    public void initialize(int revision, long previousDay, List<NodeState> replacementNodes) {
        supplyRevision = revision;
        lastProcessedDay = previousDay;
        nodes.clear();
        nodes.addAll(replacementNodes);
        shipments.clear();
        nextSerial = 0L;
        totalProduced = replacementNodes.stream().mapToLong(NodeState::totalProduced).sum();
        totalDispatched = 0L;
        totalReceived = 0L;
        totalBlocked = 0L;
        openingConvoys = 0L;
        setDirty();
    }

    public List<NodeState> nodes() {
        return List.copyOf(nodes);
    }

    public List<ShipmentState> shipments() {
        return List.copyOf(shipments);
    }

    public long lastProcessedDay() {
        return lastProcessedDay;
    }

    public String nextShipmentId(long day) {
        nextSerial++;
        setDirty();
        return "erden_supply_%06d_%04d".formatted(Math.max(0L, day), nextSerial);
    }

    public void replaceNode(NodeState replacement) {
        for (int index = 0; index < nodes.size(); index++) {
            if (!nodes.get(index).id().equals(replacement.id())) continue;
            if (!nodes.get(index).equals(replacement)) {
                nodes.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public void addShipment(ShipmentState shipment) {
        shipments.add(shipment);
        totalDispatched += shipment.amount();
        if (shipment.openingConvoy()) openingConvoys++;
        setDirty();
    }

    /** Adds production that originates outside the legacy 18 supply nodes and dispatches it atomically. */
    public void addProducedShipment(ShipmentState shipment) {
        shipments.add(shipment);
        totalProduced += shipment.amount();
        totalDispatched += shipment.amount();
        if (shipment.openingConvoy()) openingConvoys++;
        setDirty();
    }

    public void replaceShipment(ShipmentState replacement) {
        for (int index = 0; index < shipments.size(); index++) {
            if (!shipments.get(index).id().equals(replacement.id())) continue;
            if (!shipments.get(index).equals(replacement)) {
                shipments.set(index, replacement);
                setDirty();
            }
            return;
        }
    }

    public void recordArrival(long amount) {
        totalReceived += Math.max(0L, amount);
        setDirty();
    }

    public void recordBlocked() {
        totalBlocked++;
        setDirty();
    }

    public void markProcessedDay(long day, long produced) {
        if (day <= lastProcessedDay) return;
        lastProcessedDay = day;
        totalProduced += Math.max(0L, produced);
        setDirty();
    }

    public void pruneSettled(long minimumArrivalTick) {
        if (shipments.removeIf(shipment -> shipment.terminal()
                && shipment.arrivalTick() < minimumArrivalTick)) {
            setDirty();
        }
    }

    public long totalProduced() {
        return totalProduced;
    }

    public long totalDispatched() {
        return totalDispatched;
    }

    public long totalReceived() {
        return totalReceived;
    }

    public long totalBlocked() {
        return totalBlocked;
    }

    public long openingConvoys() {
        return openingConvoys;
    }
}
