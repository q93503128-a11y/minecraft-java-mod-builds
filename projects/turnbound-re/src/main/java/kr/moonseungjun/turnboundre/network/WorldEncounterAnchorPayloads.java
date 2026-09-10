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
import java.util.UUID;

/** Server-authored wire contracts for one in-world encounter anchor preview/start. */
public final class WorldEncounterAnchorPayloads {
    private static final int MAX_WIRE_CHARS = 16_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private WorldEncounterAnchorPayloads() {}

    public record PreviewView(
            UUID anchorEntityId,
            String locator,
            String encounterId,
            int difficulty,
            List<String> enemySources,
            List<String> rewardKinds,
            boolean repeatable,
            int partySize,
            String resultCode,
            String resultDetail
    ) {
        public PreviewView {
            if (anchorEntityId == null) throw new IllegalArgumentException("anchorEntityId required");
            if (locator == null || locator.isBlank()) throw new IllegalArgumentException("locator required");
            if (encounterId == null || encounterId.isBlank()) throw new IllegalArgumentException("encounterId required");
            if (difficulty < 1) throw new IllegalArgumentException("difficulty must be >= 1");
            enemySources = enemySources == null ? List.of() : List.copyOf(enemySources);
            rewardKinds = rewardKinds == null ? List.of() : List.copyOf(rewardKinds);
            if (enemySources.isEmpty()) throw new IllegalArgumentException("enemySources required");
            if (partySize < 0 || partySize > 4) throw new IllegalArgumentException("partySize must be 0..4");
            resultCode = resultCode == null ? "" : resultCode;
            resultDetail = resultDetail == null ? "" : resultDetail;
        }
    }

    public record StartRequest(UUID anchorEntityId, String locator, String encounterId) {
        public StartRequest {
            if (anchorEntityId == null) throw new IllegalArgumentException("anchorEntityId required");
            if (locator == null || locator.isBlank()) throw new IllegalArgumentException("locator required");
            if (encounterId == null || encounterId.isBlank()) throw new IllegalArgumentException("encounterId required");
        }
    }

    public record AnchorPreviewS2C(String wire) implements CustomPacketPayload {
        public static final Type<AnchorPreviewS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "world_anchor_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorPreviewS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AnchorPreviewS2C::wire, AnchorPreviewS2C::new);

        public AnchorPreviewS2C { wire = checkedWire(wire); }

        public static AnchorPreviewS2C from(PreviewView view) {
            return new AnchorPreviewS2C(
                    pack(view.anchorEntityId().toString()) + "|"
                            + pack(view.locator()) + "|"
                            + pack(view.encounterId()) + "|"
                            + view.difficulty() + "|"
                            + packList(view.enemySources()) + "|"
                            + packList(view.rewardKinds()) + "|"
                            + view.repeatable() + "|"
                            + view.partySize() + "|"
                            + pack(view.resultCode()) + "|"
                            + pack(view.resultDetail()));
        }

        public PreviewView decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 10) throw new IllegalArgumentException("invalid anchor preview wire");
            return new PreviewView(
                    UUID.fromString(unpack(parts[0])),
                    unpack(parts[1]),
                    unpack(parts[2]),
                    Integer.parseInt(parts[3]),
                    unpackList(parts[4]),
                    unpackList(parts[5]),
                    Boolean.parseBoolean(parts[6]),
                    Integer.parseInt(parts[7]),
                    unpack(parts[8]),
                    unpack(parts[9]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StartAnchorEncounterC2S(String wire) implements CustomPacketPayload {
        public static final Type<StartAnchorEncounterC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "world_anchor_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StartAnchorEncounterC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, StartAnchorEncounterC2S::wire, StartAnchorEncounterC2S::new);

        public StartAnchorEncounterC2S { wire = checkedWire(wire); }

        public static StartAnchorEncounterC2S of(UUID anchorEntityId, String locator, String encounterId) {
            return new StartAnchorEncounterC2S(
                    pack(anchorEntityId.toString()) + "|" + pack(locator) + "|" + pack(encounterId));
        }

        public StartRequest decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 3) throw new IllegalArgumentException("invalid anchor start wire");
            return new StartRequest(UUID.fromString(unpack(parts[0])), unpack(parts[1]), unpack(parts[2]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AnchorRejectedS2C(String wire) implements CustomPacketPayload {
        public static final Type<AnchorRejectedS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "world_anchor_rejected"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorRejectedS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AnchorRejectedS2C::wire, AnchorRejectedS2C::new);

        public AnchorRejectedS2C { wire = checkedWire(wire); }
        public static AnchorRejectedS2C of(String code, String detail) {
            return new AnchorRejectedS2C(pack(code) + "|" + pack(detail));
        }
        public String code() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException("invalid anchor rejection wire");
            return unpack(parts[0]);
        }
        public String detail() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException("invalid anchor rejection wire");
            return unpack(parts[1]);
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static String packList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(WorldEncounterAnchorPayloads::pack).reduce((a, b) -> a + "," + b).orElse("");
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
        if (wire == null || wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("invalid anchor wire length");
        return wire;
    }
}
