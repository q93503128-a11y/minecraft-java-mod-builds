package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.EquipmentDefinition;
import kr.moonseungjun.turnboundre.progression.EquipmentProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Server-authored equipment presentation plus client intent. Material balances are never client-authored. */
public final class EquipmentNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 32_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private EquipmentNetworkPayloads() {}

    public record BonusView(int hpPercent, int atkPercent, int defPercent, int poisePercent) {
        public static final BonusView ZERO = new BonusView(0, 0, 0, 0);
        public BonusView {
            if (hpPercent < 0 || atkPercent < 0 || defPercent < 0 || poisePercent < 0) {
                throw new IllegalArgumentException("equipment bonus must be >= 0");
            }
        }
    }

    public record EquipmentView(
            String id,
            String ingredientItem,
            int level,
            int maxLevel,
            String equippedCharacterId,
            int materialOwned,
            BonusView currentBonus,
            long nextCoinCost,
            int nextMaterialCount,
            BonusView nextBonus,
            String action,
            String blockCode
    ) {
        public EquipmentView {
            if (id == null || id.isBlank() || ingredientItem == null || ingredientItem.isBlank()) {
                throw new IllegalArgumentException("equipment ids required");
            }
            if (level < 0 || maxLevel < 1 || level > maxLevel || materialOwned < 0 || nextCoinCost < 0 || nextMaterialCount < 0) {
                throw new IllegalArgumentException("invalid equipment view numbers");
            }
            equippedCharacterId = equippedCharacterId == null ? "" : equippedCharacterId;
            if (currentBonus == null || nextBonus == null) throw new IllegalArgumentException("equipment bonus views required");
            action = action == null ? "" : action;
            blockCode = blockCode == null ? "" : blockCode;
        }
        public boolean owned() { return level > 0; }
        public boolean canForge() { return ("CRAFT".equals(action) || "UPGRADE".equals(action)) && blockCode.isBlank(); }
    }

    public record Snapshot(boolean forgeAvailable, List<EquipmentView> equipment, String resultCode, String resultDetail) {
        public Snapshot {
            equipment = equipment == null ? List.of() : List.copyOf(equipment);
            resultCode = resultCode == null ? "" : resultCode;
            resultDetail = resultDetail == null ? "" : resultDetail;
        }
        public Optional<EquipmentView> equipment(String id) {
            return equipment.stream().filter(value -> value.id().equals(id)).findFirst();
        }
    }

    public record DecodedAction(
            String operation,
            String characterId,
            String equipmentId,
            int expectedLevel,
            String expectedEquippedId
    ) {
        public DecodedAction {
            operation = operation == null ? "" : operation;
            characterId = characterId == null ? "" : characterId;
            equipmentId = equipmentId == null ? "" : equipmentId;
            expectedEquippedId = expectedEquippedId == null ? "" : expectedEquippedId;
            if (expectedLevel < 0) throw new IllegalArgumentException("expectedLevel must be >= 0");
        }
    }

    public record ActionC2S(String wire) implements CustomPacketPayload {
        public static final Type<ActionC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "equipment_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ActionC2S::wire, ActionC2S::new);

        public ActionC2S { wire = checkedWire(wire); }

        public static ActionC2S of(String operation, String characterId, String equipmentId, int expectedLevel, String expectedEquippedId) {
            return new ActionC2S(pack(operation) + "|" + pack(characterId) + "|" + pack(equipmentId)
                    + "|" + expectedLevel + "|" + pack(expectedEquippedId));
        }

        public DecodedAction decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 5) throw new IllegalArgumentException("invalid equipment action wire");
            return new DecodedAction(unpack(parts[0]), unpack(parts[1]), unpack(parts[2]),
                    Integer.parseInt(parts[3]), unpack(parts[4]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SnapshotS2C(String wire) implements CustomPacketPayload {
        public static final Type<SnapshotS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "equipment_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SnapshotS2C::wire, SnapshotS2C::new);

        public SnapshotS2C { wire = checkedWire(wire); }

        public static SnapshotS2C from(
                PlayerProgress progress,
                DefinitionRegistry definitions,
                Map<String, Integer> materialCounts,
                boolean forgeAvailable,
                String resultCode,
                String resultDetail
        ) {
            if (progress == null || definitions == null || materialCounts == null) {
                throw new IllegalArgumentException("progress/definitions/materialCounts required");
            }
            List<EquipmentView> views = new ArrayList<>();
            for (EquipmentDefinition definition : definitions.equipment().values()) {
                EquipmentProgress owned = progress.equipment().get(definition.id());
                int level = owned == null ? 0 : owned.level();
                EquipmentDefinition.Tier currentTier = level == 0 ? null : definition.tier(level);
                if (level > 0 && currentTier == null) {
                    throw new IllegalArgumentException("persisted equipment tier missing from current definitions: " + definition.id() + "@" + level);
                }
                EquipmentDefinition.Tier nextTier = definition.tier(level + 1);
                String action = level == 0 ? "CRAFT" : nextTier == null ? "MAX" : "UPGRADE";
                int materialOwned = Math.max(0, materialCounts.getOrDefault(definition.ingredientItem(), 0));
                String block = "";
                if (nextTier != null) {
                    if (!forgeAvailable) block = "FORGE_UNAVAILABLE";
                    else if (progress.coin() < nextTier.coinCost()) block = "INSUFFICIENT_COIN";
                    else if (materialOwned < nextTier.materialCount()) block = "INSUFFICIENT_MATERIAL";
                } else {
                    block = "MAX_LEVEL";
                }
                String equippedCharacter = progress.equippedEquipment().entrySet().stream()
                        .filter(entry -> entry.getValue().equals(definition.id()))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse("");
                views.add(new EquipmentView(
                        definition.id(), definition.ingredientItem(), level, definition.maxLevel(), equippedCharacter,
                        materialOwned,
                        currentTier == null ? BonusView.ZERO : bonus(currentTier.bonus()),
                        nextTier == null ? 0L : nextTier.coinCost(),
                        nextTier == null ? 0 : nextTier.materialCount(),
                        nextTier == null ? (currentTier == null ? BonusView.ZERO : bonus(currentTier.bonus())) : bonus(nextTier.bonus()),
                        action, block));
            }
            views.sort(Comparator.comparing(EquipmentView::id));
            return of(new Snapshot(forgeAvailable, views, resultCode, resultDetail));
        }

        public static SnapshotS2C of(Snapshot snapshot) {
            return new SnapshotS2C((snapshot.forgeAvailable() ? "1" : "0") + "|"
                    + packViews(snapshot.equipment()) + "|" + pack(snapshot.resultCode()) + "|" + pack(snapshot.resultDetail()));
        }

        public Snapshot decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 4) throw new IllegalArgumentException("invalid equipment snapshot wire");
            return new Snapshot("1".equals(parts[0]), unpackViews(parts[1]), unpack(parts[2]), unpack(parts[3]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static BonusView bonus(EquipmentDefinition.Bonus bonus) {
        return new BonusView(bonus.hpPercent(), bonus.atkPercent(), bonus.defPercent(), bonus.poisePercent());
    }

    private static String packViews(List<EquipmentView> values) {
        if (values == null || values.isEmpty()) return "";
        List<String> packed = new ArrayList<>();
        for (EquipmentView value : values) {
            packed.add(String.join(",",
                    pack(value.id()), pack(value.ingredientItem()), Integer.toString(value.level()), Integer.toString(value.maxLevel()),
                    pack(value.equippedCharacterId()), Integer.toString(value.materialOwned()), packBonus(value.currentBonus()),
                    Long.toString(value.nextCoinCost()), Integer.toString(value.nextMaterialCount()), packBonus(value.nextBonus()),
                    pack(value.action()), pack(value.blockCode())));
        }
        return String.join(";", packed);
    }

    private static List<EquipmentView> unpackViews(String wire) {
        if (wire == null || wire.isEmpty()) return List.of();
        List<EquipmentView> values = new ArrayList<>();
        for (String packed : wire.split(";", -1)) {
            String[] p = packed.split(",", -1);
            if (p.length != 12) throw new IllegalArgumentException("invalid equipment view wire");
            values.add(new EquipmentView(unpack(p[0]), unpack(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]),
                    unpack(p[4]), Integer.parseInt(p[5]), unpackBonus(p[6]), Long.parseLong(p[7]), Integer.parseInt(p[8]),
                    unpackBonus(p[9]), unpack(p[10]), unpack(p[11])));
        }
        return List.copyOf(values);
    }

    private static String packBonus(BonusView bonus) {
        return bonus.hpPercent() + ":" + bonus.atkPercent() + ":" + bonus.defPercent() + ":" + bonus.poisePercent();
    }

    private static BonusView unpackBonus(String wire) {
        String[] p = wire.split(":", -1);
        if (p.length != 4) throw new IllegalArgumentException("invalid equipment bonus wire");
        return new BonusView(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    private static String pack(String value) {
        String safe = value == null ? "" : value;
        return B64E.encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String unpack(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(B64D.decode(value), StandardCharsets.UTF_8);
    }

    private static String checkedWire(String wire) {
        if (wire == null || wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("wire too large");
        return wire;
    }
}
