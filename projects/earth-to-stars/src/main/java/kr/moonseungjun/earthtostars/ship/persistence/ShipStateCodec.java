package kr.moonseungjun.earthtostars.ship.persistence;

import kr.moonseungjun.earthtostars.ship.domain.CrewRole;
import kr.moonseungjun.earthtostars.ship.domain.ModuleCatalog;
import kr.moonseungjun.earthtostars.ship.domain.ModuleInstance;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ModuleSlotType;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ShipStateCodec {
    private static final int MAGIC = 0x45545331;
    public static final int CURRENT_SCHEMA = 1;
    private static final int MAX_COLLECTION_SIZE = 10_000;

    private ShipStateCodec() {
    }

    public static byte[] encode(ShipState state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(CURRENT_SCHEMA);
                writeUuid(out, state.shipId().value());
                writeUuid(out, state.ownerId());

                List<ModuleSlot> slots = state.slots().values().stream()
                        .sorted(Comparator.comparing(ModuleSlot::id))
                        .toList();
                out.writeInt(slots.size());
                for (ModuleSlot slot : slots) {
                    out.writeUTF(slot.id());
                    out.writeUTF(slot.type().name());
                    out.writeInt(slot.sizeClass());
                }

                List<Map.Entry<UUID, CrewRole>> crew = state.crewAssignments().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                        .toList();
                out.writeInt(crew.size());
                for (Map.Entry<UUID, CrewRole> entry : crew) {
                    writeUuid(out, entry.getKey());
                    out.writeUTF(entry.getValue().name());
                }

                List<ModuleInstance> modules = state.modules().values().stream()
                        .sorted(Comparator.comparing(module -> module.instanceId().toString()))
                        .toList();
                out.writeInt(modules.size());
                for (ModuleInstance module : modules) {
                    writeUuid(out, module.instanceId());
                    out.writeUTF(module.definitionId());
                    out.writeUTF(module.slotId());
                    out.writeDouble(module.condition());
                }
            }
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new IllegalStateException("in-memory ship encoding failed", impossible);
        }
    }

    public static ShipState decode(byte[] bytes, ModuleCatalog catalog) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != MAGIC) {
                throw new IllegalArgumentException("invalid EARTH TO STARS ship payload magic");
            }
            int schema = in.readInt();
            if (schema != CURRENT_SCHEMA) {
                throw new IllegalArgumentException("unsupported ship schema " + schema + "; expected " + CURRENT_SCHEMA);
            }

            ShipId shipId = new ShipId(readUuid(in));
            UUID ownerId = readUuid(in);

            int slotCount = readCount(in, "slots");
            List<ModuleSlot> slots = new ArrayList<>(slotCount);
            for (int i = 0; i < slotCount; i++) {
                slots.add(new ModuleSlot(in.readUTF(), ModuleSlotType.valueOf(in.readUTF()), in.readInt()));
            }

            int crewCount = readCount(in, "crew");
            Map<UUID, CrewRole> crew = new LinkedHashMap<>();
            for (int i = 0; i < crewCount; i++) {
                UUID playerId = readUuid(in);
                CrewRole role = CrewRole.valueOf(in.readUTF());
                if (crew.putIfAbsent(playerId, role) != null) {
                    throw new IllegalArgumentException("duplicate persisted crew member: " + playerId);
                }
            }

            int moduleCount = readCount(in, "modules");
            List<ModuleInstance> modules = new ArrayList<>(moduleCount);
            for (int i = 0; i < moduleCount; i++) {
                modules.add(new ModuleInstance(readUuid(in), in.readUTF(), in.readUTF(), in.readDouble()));
            }

            if (in.available() != 0) {
                throw new IllegalArgumentException("unexpected trailing bytes in ship payload");
            }
            return ShipState.restore(shipId, ownerId, slots, crew, modules, catalog);
        } catch (IOException | IllegalArgumentException exception) {
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("failed to decode ship state", exception);
        }
    }

    private static int readCount(DataInputStream in, String label) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_COLLECTION_SIZE) {
            throw new IllegalArgumentException("invalid " + label + " count: " + count);
        }
        return count;
    }

    private static void writeUuid(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }
}
