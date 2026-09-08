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
import java.util.Locale;
import java.util.UUID;

/** Server-authored terminal battle presentation. Reward values are facts, never client-side rolls. */
public final class BattleResultNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 32_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private BattleResultNetworkPayloads() {}

    public record ShardView(String characterId, int amount, int total) {
        public ShardView {
            if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("characterId required");
            if (amount < 0 || total < 0 || total < amount) throw new IllegalArgumentException("invalid shard result");
        }
    }

    public record ResultView(
            UUID battleId,
            long revision,
            String outcome,
            long coinDelta,
            long essenceDelta,
            long coinTotal,
            long essenceTotal,
            List<ShardView> shards
    ) {
        public ResultView {
            if (battleId == null || revision < 0) throw new IllegalArgumentException("battleId/revision required");
            outcome = outcome == null ? "" : outcome.toUpperCase(Locale.ROOT);
            if (!("VICTORY".equals(outcome) || "DEFEAT".equals(outcome))) {
                throw new IllegalArgumentException("outcome must be VICTORY or DEFEAT");
            }
            if (coinDelta < 0 || essenceDelta < 0 || coinTotal < 0 || essenceTotal < 0) {
                throw new IllegalArgumentException("reward values must be >= 0");
            }
            shards = shards == null ? List.of() : List.copyOf(shards);
        }

        public boolean victory() { return "VICTORY".equals(outcome); }
        public boolean hasRewards() { return coinDelta > 0 || essenceDelta > 0 || !shards.isEmpty(); }
    }

    public record ResultS2C(String wire) implements CustomPacketPayload {
        public static final Type<ResultS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ResultS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ResultS2C::wire, ResultS2C::new);

        public ResultS2C {
            requireWire(wire);
        }

        public static ResultS2C from(ResultView view) {
            if (view == null) throw new IllegalArgumentException("view required");
            String shardWire = view.shards().stream()
                    .map(shard -> pack(shard.characterId()) + "," + shard.amount() + "," + shard.total())
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
            return new ResultS2C(view.battleId() + "|" + view.revision() + "|" + view.outcome()
                    + "|" + view.coinDelta() + "|" + view.essenceDelta()
                    + "|" + view.coinTotal() + "|" + view.essenceTotal() + "|" + shardWire);
        }

        public ResultView decode() {
            String[] fields = wire.split("\\|", -1);
            if (fields.length != 8) throw new IllegalArgumentException("invalid battle result wire");
            UUID battleId = UUID.fromString(fields[0]);
            long revision = Long.parseLong(fields[1]);
            String outcome = fields[2];
            long coinDelta = Long.parseLong(fields[3]);
            long essenceDelta = Long.parseLong(fields[4]);
            long coinTotal = Long.parseLong(fields[5]);
            long essenceTotal = Long.parseLong(fields[6]);
            List<ShardView> shards = new ArrayList<>();
            if (!fields[7].isEmpty()) {
                for (String entry : fields[7].split(";", -1)) {
                    String[] parts = entry.split(",", -1);
                    if (parts.length != 3) throw new IllegalArgumentException("invalid shard result entry");
                    shards.add(new ShardView(unpack(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                }
            }
            return new ResultView(battleId, revision, outcome, coinDelta, essenceDelta, coinTotal, essenceTotal, shards);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AcknowledgeResultC2S(String wire) implements CustomPacketPayload {
        public static final Type<AcknowledgeResultC2S> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "acknowledge_battle_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AcknowledgeResultC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AcknowledgeResultC2S::wire, AcknowledgeResultC2S::new);

        public AcknowledgeResultC2S { requireWire(wire); }

        public static AcknowledgeResultC2S from(ResultView view) {
            if (view == null) throw new IllegalArgumentException("view required");
            return new AcknowledgeResultC2S(view.battleId() + "|" + view.revision() + "|" + view.outcome());
        }

        public DecodedAcknowledgement decode() {
            String[] fields = wire.split("\\|", -1);
            if (fields.length != 3) throw new IllegalArgumentException("invalid result acknowledgement wire");
            return new DecodedAcknowledgement(UUID.fromString(fields[0]), Long.parseLong(fields[1]), fields[2]);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DecodedAcknowledgement(UUID battleId, long revision, String outcome) {
        public DecodedAcknowledgement {
            if (battleId == null || revision < 0) throw new IllegalArgumentException("battleId/revision required");
            outcome = outcome == null ? "" : outcome.toUpperCase(Locale.ROOT);
            if (!("VICTORY".equals(outcome) || "DEFEAT".equals(outcome))) {
                throw new IllegalArgumentException("invalid acknowledgement outcome");
            }
        }
    }

    public record ResultClosedS2C(String wire) implements CustomPacketPayload {
        public static final Type<ResultClosedS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_result_closed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ResultClosedS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ResultClosedS2C::wire, ResultClosedS2C::new);

        public ResultClosedS2C { requireWire(wire); }

        public static ResultClosedS2C of(UUID battleId, boolean accepted, String code) {
            if (battleId == null) throw new IllegalArgumentException("battleId required");
            return new ResultClosedS2C(battleId + "|" + (accepted ? "1" : "0") + "|" + pack(code));
        }

        public DecodedClose decode() {
            String[] fields = wire.split("\\|", -1);
            if (fields.length != 3) throw new IllegalArgumentException("invalid result close wire");
            if (!("0".equals(fields[1]) || "1".equals(fields[1]))) throw new IllegalArgumentException("invalid close accepted flag");
            return new DecodedClose(UUID.fromString(fields[0]), "1".equals(fields[1]), unpack(fields[2]));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DecodedClose(UUID battleId, boolean accepted, String code) {
        public DecodedClose {
            if (battleId == null) throw new IllegalArgumentException("battleId required");
            code = code == null ? "" : code;
        }
    }

    private static void requireWire(String wire) {
        if (wire == null || wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("battle result wire too large");
    }

    private static String pack(String value) {
        String safe = value == null ? "" : value;
        return B64E.encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String unpack(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(B64D.decode(value), StandardCharsets.UTF_8);
    }
}
