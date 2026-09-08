package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Definition-bound visual metadata kept separate from dynamic progression state. */
public final class CharacterPresentationNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 32_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private CharacterPresentationNetworkPayloads() {}

    public record CatalogS2C(String wire) implements CustomPacketPayload {
        public static final Type<CatalogS2C> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "character_presentation_catalog"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CatalogS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, CatalogS2C::wire, CatalogS2C::new);

        public CatalogS2C {
            if (wire == null || wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("visual catalog wire too large");
        }

        public static CatalogS2C from(DefinitionRegistry definitions) {
            if (definitions == null) throw new IllegalArgumentException("definitions required");
            Map<String, String> sources = new LinkedHashMap<>();
            definitions.characters().values().stream()
                    .sorted(java.util.Comparator.comparing(CharacterDefinition::id))
                    .forEach(character -> sources.put(character.id(), character.sourceEntity()));
            return of(sources);
        }

        public static CatalogS2C of(Map<String, String> sources) {
            if (sources == null || sources.isEmpty()) return new CatalogS2C("");
            String wire = sources.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> pack(entry.getKey()) + "," + pack(entry.getValue()))
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
            return new CatalogS2C(wire);
        }

        public Map<String, String> decode() {
            if (wire.isEmpty()) return Map.of();
            Map<String, String> out = new LinkedHashMap<>();
            for (String entry : wire.split(";", -1)) {
                String[] parts = entry.split(",", -1);
                if (parts.length != 2) throw new IllegalArgumentException("invalid character visual catalog entry");
                String characterId = unpack(parts[0]);
                String sourceEntity = unpack(parts[1]);
                if (characterId.isBlank() || out.putIfAbsent(characterId, sourceEntity) != null) {
                    throw new IllegalArgumentException("duplicate/blank character visual catalog id");
                }
            }
            return Map.copyOf(out);
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
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
