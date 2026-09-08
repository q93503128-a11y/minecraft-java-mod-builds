package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.progression.ProgressionRules;
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
import java.util.Optional;

/** Server-authored progression presentation and party-edit wire contracts. */
public final class ProgressionNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 48_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private ProgressionNetworkPayloads() {}

    public record CharacterView(
            String id,
            boolean owned,
            int originStar,
            int currentStar,
            int level,
            int levelCap,
            int squadCost,
            List<String> roles,
            int hp,
            int atk,
            int def,
            int spd,
            int poise,
            List<String> affinities,
            String basicAction,
            List<String> skills,
            String burst
    ) {
        public CharacterView {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("character id required");
            if (originStar < 1 || originStar > 5) throw new IllegalArgumentException("originStar must be 1..5");
            if (currentStar < originStar || currentStar > 6) throw new IllegalArgumentException("currentStar invalid");
            if (level < 0 || level > levelCap) throw new IllegalArgumentException("level invalid");
            if (owned && level < 1) throw new IllegalArgumentException("owned character requires level >= 1");
            if (squadCost < 0) throw new IllegalArgumentException("squadCost must be >= 0");
            roles = roles == null ? List.of() : List.copyOf(roles);
            affinities = affinities == null ? List.of() : List.copyOf(affinities);
            skills = skills == null ? List.of() : List.copyOf(skills);
            basicAction = basicAction == null ? "" : basicAction;
            burst = burst == null ? "" : burst;
        }
    }

    public record Snapshot(
            long coin,
            long essence,
            int partyCapacity,
            List<String> party,
            List<CharacterView> characters,
            String resultCode,
            String resultDetail
    ) {
        public Snapshot {
            if (coin < 0 || essence < 0 || partyCapacity < 1) throw new IllegalArgumentException("invalid progression snapshot");
            party = party == null ? List.of() : List.copyOf(party);
            characters = characters == null ? List.of() : List.copyOf(characters);
            resultCode = resultCode == null ? "" : resultCode;
            resultDetail = resultDetail == null ? "" : resultDetail;
        }

        public Optional<CharacterView> character(String id) {
            return characters.stream().filter(character -> character.id().equals(id)).findFirst();
        }
    }

    public record DecodedSetParty(List<String> expectedParty, List<String> requestedParty) {
        public DecodedSetParty {
            expectedParty = expectedParty == null ? List.of() : List.copyOf(expectedParty);
            requestedParty = requestedParty == null ? List.of() : List.copyOf(requestedParty);
        }
    }

    public record RequestProgressC2S(String wire) implements CustomPacketPayload {
        public static final Type<RequestProgressC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "progress_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestProgressC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, RequestProgressC2S::wire, RequestProgressC2S::new);

        public RequestProgressC2S {
            wire = checkedWire(wire);
            if (!"request".equals(wire)) throw new IllegalArgumentException("invalid progression request");
        }

        public RequestProgressC2S() { this("request"); }
    }

    public record SetPartyC2S(String wire) implements CustomPacketPayload {
        public static final Type<SetPartyC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "set_party"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetPartyC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SetPartyC2S::wire, SetPartyC2S::new);

        public SetPartyC2S { wire = checkedWire(wire); }

        public static SetPartyC2S of(List<String> expectedParty, List<String> requestedParty) {
            return new SetPartyC2S(packList(expectedParty) + "|" + packList(requestedParty));
        }

        public DecodedSetParty decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException("invalid set-party wire");
            return new DecodedSetParty(unpackList(parts[0]), unpackList(parts[1]));
        }
    }

    public record ProgressSnapshotS2C(String wire) implements CustomPacketPayload {
        public static final Type<ProgressSnapshotS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "progress_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSnapshotS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ProgressSnapshotS2C::wire, ProgressSnapshotS2C::new);

        public ProgressSnapshotS2C { wire = checkedWire(wire); }

        public static ProgressSnapshotS2C from(
                PlayerProgress progress,
                DefinitionRegistry definitions,
                String resultCode,
                String resultDetail
        ) {
            if (progress == null || definitions == null) throw new IllegalArgumentException("progress/definitions required");
            List<CharacterView> characters = new ArrayList<>();
            for (CharacterDefinition definition : definitions.characters().values()) {
                CharacterProgress owned = progress.characters().get(definition.id());
                CharacterDefinition.Stats stats;
                int currentStar;
                int level;
                int cap;
                if (owned != null) {
                    stats = ProgressionRules.stats(definition, owned);
                    currentStar = owned.currentStar();
                    level = owned.level();
                    cap = ProgressionRules.levelCap(currentStar);
                } else {
                    stats = definition.baseStats();
                    currentStar = definition.originStar();
                    level = 0;
                    cap = ProgressionRules.levelCap(currentStar);
                }
                List<String> affinities = definition.affinities().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .toList();
                characters.add(new CharacterView(
                        definition.id(),
                        owned != null,
                        definition.originStar(),
                        currentStar,
                        level,
                        cap,
                        definition.squadCost(),
                        definition.roles(),
                        stats.hp(), stats.atk(), stats.def(), stats.spd(), stats.poise(),
                        affinities,
                        definition.basicAction(),
                        definition.skills(),
                        definition.burst()));
            }
            characters.sort(Comparator
                    .comparing(CharacterView::owned).reversed()
                    .thenComparing(CharacterView::originStar, Comparator.reverseOrder())
                    .thenComparing(CharacterView::id));
            return of(new Snapshot(progress.coin(), progress.essence(), progress.partyCapacity(), progress.party(),
                    characters, resultCode, resultDetail));
        }

        public static ProgressSnapshotS2C of(Snapshot snapshot) {
            String wire = snapshot.coin()
                    + "|" + snapshot.essence()
                    + "|" + snapshot.partyCapacity()
                    + "|" + packList(snapshot.party())
                    + "|" + packCharacters(snapshot.characters())
                    + "|" + pack(snapshot.resultCode())
                    + "|" + pack(snapshot.resultDetail());
            return new ProgressSnapshotS2C(wire);
        }

        public Snapshot decode() {
            String[] parts = wire.split("\\|", -1);
            if (parts.length != 7) throw new IllegalArgumentException("invalid progress snapshot wire");
            return new Snapshot(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Integer.parseInt(parts[2]),
                    unpackList(parts[3]),
                    unpackCharacters(parts[4]),
                    unpack(parts[5]),
                    unpack(parts[6]));
        }
    }

    private static String packCharacters(List<CharacterView> characters) {
        if (characters == null || characters.isEmpty()) return "";
        List<String> out = new ArrayList<>();
        for (CharacterView c : characters) {
            out.add(String.join(",",
                    pack(c.id()),
                    c.owned() ? "1" : "0",
                    Integer.toString(c.originStar()),
                    Integer.toString(c.currentStar()),
                    Integer.toString(c.level()),
                    Integer.toString(c.levelCap()),
                    Integer.toString(c.squadCost()),
                    packList(c.roles()),
                    Integer.toString(c.hp()),
                    Integer.toString(c.atk()),
                    Integer.toString(c.def()),
                    Integer.toString(c.spd()),
                    Integer.toString(c.poise()),
                    packList(c.affinities()),
                    pack(c.basicAction()),
                    packList(c.skills()),
                    pack(c.burst())));
        }
        return String.join(";", out);
    }

    private static List<CharacterView> unpackCharacters(String wire) {
        if (wire == null || wire.isEmpty()) return List.of();
        List<CharacterView> out = new ArrayList<>();
        for (String packedCharacter : wire.split(";", -1)) {
            String[] p = packedCharacter.split(",", -1);
            if (p.length != 17) throw new IllegalArgumentException("invalid character view wire");
            out.add(new CharacterView(
                    unpack(p[0]),
                    "1".equals(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]),
                    Integer.parseInt(p[5]),
                    Integer.parseInt(p[6]),
                    unpackList(p[7]),
                    Integer.parseInt(p[8]),
                    Integer.parseInt(p[9]),
                    Integer.parseInt(p[10]),
                    Integer.parseInt(p[11]),
                    Integer.parseInt(p[12]),
                    unpackList(p[13]),
                    unpack(p[14]),
                    unpackList(p[15]),
                    unpack(p[16])));
        }
        return List.copyOf(out);
    }

    private static String packList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        return values.stream().map(ProgressionNetworkPayloads::pack).reduce((a, b) -> a + "~" + b).orElse("");
    }

    private static List<String> unpackList(String value) {
        if (value == null || value.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : value.split("~", -1)) out.add(unpack(part));
        return List.copyOf(out);
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
