package kr.moonseungjun.anomalousclasses.network.payload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-computed assembly and maintenance quotes; never a client authority. */
public record PuppetWorkshopStatePayload(BlockPos station, boolean craftable, int result,
                                         List<PuppetView> puppets) implements CustomPacketPayload {
    public static final int OPEN = 0, CRAFTED = 1, REJECTED = 2,
            REPAIRED = 3, EXPANDED = 4, DISMANTLED = 5;
    public record PuppetView(int slot, UUID id, int revision, int size, int condition,
                             boolean broken, boolean canRepair, boolean canExpand, boolean canDismantle,
                             Map<String, Integer> repairCost, Map<String, Integer> expandCost,
                             Map<String, Integer> refund) {
        public PuppetView {
            repairCost = Map.copyOf(repairCost);
            expandCost = Map.copyOf(expandCost);
            refund = Map.copyOf(refund);
        }
    }

    public PuppetWorkshopStatePayload {
        puppets = List.copyOf(puppets);
        if (puppets.size() > 36) throw new IllegalArgumentException("Too many workbench items");
    }

    public static final Type<PuppetWorkshopStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "puppet_workbench_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PuppetWorkshopStatePayload> STREAM_CODEC =
            StreamCodec.of((buffer, value) -> {
                buffer.writeBlockPos(value.station);
                buffer.writeBoolean(value.craftable);
                buffer.writeVarInt(value.result);
                buffer.writeVarInt(value.puppets.size());
                for (PuppetView puppet : value.puppets) {
                    buffer.writeVarInt(puppet.slot);
                    buffer.writeUUID(puppet.id);
                    buffer.writeVarInt(puppet.revision);
                    buffer.writeVarInt(puppet.size);
                    buffer.writeVarInt(puppet.condition);
                    buffer.writeBoolean(puppet.broken);
                    buffer.writeBoolean(puppet.canRepair);
                    buffer.writeBoolean(puppet.canExpand);
                    buffer.writeBoolean(puppet.canDismantle);
                    writeBill(buffer, puppet.repairCost);
                    writeBill(buffer, puppet.expandCost);
                    writeBill(buffer, puppet.refund);
                }
            }, buffer -> {
                BlockPos station = buffer.readBlockPos();
                boolean craftable = buffer.readBoolean();
                int result = buffer.readVarInt();
                int count = buffer.readVarInt();
                if (count < 0 || count > 36) throw new IllegalArgumentException("Invalid workbench item count");
                java.util.ArrayList<PuppetView> puppets = new java.util.ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int slot = buffer.readVarInt();
                    UUID id = buffer.readUUID();
                    int revision = buffer.readVarInt();
                    int size = buffer.readVarInt();
                    int condition = buffer.readVarInt();
                    boolean broken = buffer.readBoolean();
                    boolean repairable = buffer.readBoolean();
                    boolean expandable = buffer.readBoolean();
                    boolean dismantleable = buffer.readBoolean();
                    Map<String, Integer> repair = readBill(buffer);
                    Map<String, Integer> expand = readBill(buffer);
                    Map<String, Integer> refund = readBill(buffer);
                    puppets.add(new PuppetView(slot, id, revision, size, condition,
                            broken, repairable, expandable, dismantleable, repair, expand, refund));
                }
                return new PuppetWorkshopStatePayload(station, craftable, result, puppets);
            });

    private static void writeBill(RegistryFriendlyByteBuf buffer, Map<String, Integer> bill) {
        buffer.writeVarInt(bill.size());
        bill.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            buffer.writeUtf(entry.getKey(), 128);
            buffer.writeVarInt(entry.getValue());
        });
    }

    private static Map<String, Integer> readBill(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 20) throw new IllegalArgumentException("Invalid material count");
        Map<String, Integer> bill = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String material = buffer.readUtf(128);
            int amount = buffer.readVarInt();
            if (amount < 1 || bill.putIfAbsent(material, amount) != null)
                throw new IllegalArgumentException("Invalid material entry");
        }
        return Map.copyOf(bill);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
