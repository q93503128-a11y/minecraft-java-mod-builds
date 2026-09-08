package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Compact server-authored wire contracts for the in-world Expedition Journal. */
public final class ExpeditionNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 24_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private ExpeditionNetworkPayloads() {}

    public record EncounterView(String id, int difficulty, int enemyCount, boolean repeatable) {
        public EncounterView {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("encounter id required");
            if (difficulty < 1 || enemyCount < 1) throw new IllegalArgumentException("invalid encounter summary");
        }
    }

    public record JournalView(List<String> party, List<EncounterView> encounters, String resultCode, String resultDetail) {
        public JournalView {
            party = party == null ? List.of() : List.copyOf(party);
            encounters = encounters == null ? List.of() : List.copyOf(encounters);
            resultCode = resultCode == null ? "" : resultCode;
            resultDetail = resultDetail == null ? "" : resultDetail;
        }
    }

    public record RequestJournalC2S(String wire) implements CustomPacketPayload {
        public static final Type<RequestJournalC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "expedition_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestJournalC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RequestJournalC2S::wire, RequestJournalC2S::new);
        public RequestJournalC2S {
            wire = checkedWire(wire);
            if (!"request".equals(wire)) throw new IllegalArgumentException("invalid expedition request");
        }
        public RequestJournalC2S() { this("request"); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StartEncounterC2S(String wire) implements CustomPacketPayload {
        public static final Type<StartEncounterC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "expedition_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartEncounterC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, StartEncounterC2S::wire, StartEncounterC2S::new);
        public StartEncounterC2S { wire = checkedWire(wire); }
        public static StartEncounterC2S of(String encounterId) { return new StartEncounterC2S(pack(encounterId)); }
        public String encounterId() { return unpack(wire); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record JournalSnapshotS2C(String wire) implements CustomPacketPayload {
        public static final Type<JournalSnapshotS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "expedition_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, JournalSnapshotS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, JournalSnapshotS2C::wire, JournalSnapshotS2C::new);
        public JournalSnapshotS2C { wire = checkedWire(wire); }

        public static JournalSnapshotS2C of(JournalView view) {
            List<String> rows = view.encounters().stream()
                    .map(encounter -> encounter.id() + "\t" + encounter.difficulty() + "\t" + encounter.enemyCount() + "\t" + encounter.repeatable())
                    .toList();
            return new JournalSnapshotS2C(
                    packList(view.party()) + "|" + packList(rows) + "|" + pack(view.resultCode()) + "|" + pack(view.resultDetail()));
        }

        public JournalView decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 4) throw new IllegalArgumentException("invalid expedition snapshot wire");
            List<EncounterView> encounters = new ArrayList<>();
            for (String row : unpackList(parts[1])) {
                String[] fields = row.split("\\t", -1);
                if (fields.length != 4) throw new IllegalArgumentException("invalid expedition encounter row");
                encounters.add(new EncounterView(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Boolean.parseBoolean(fields[3])));
            }
            return new JournalView(unpackList(parts[0]), encounters, unpack(parts[2]), unpack(parts[3]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static String packList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(ExpeditionNetworkPayloads::pack).reduce((a, b) -> a + "," + b).orElse("");
    }

    private static List<String> unpackList(String value) {
        if (value == null || value.isBlank()) return List.of();
        String[] parts = value.split(",", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String part : parts) out.add(unpack(part));
        return List.copyOf(out);
    }

    private static String pack(String value) {
        String safe = value == null ? "" : value;
        return B64E.encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String unpack(String value) {
        if (value == null || value.isBlank()) return "";
        return new String(B64D.decode(value), StandardCharsets.UTF_8);
    }

    private static String checkedWire(String wire) {
        if (wire == null || wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("invalid expedition wire length");
        return wire;
    }
}
