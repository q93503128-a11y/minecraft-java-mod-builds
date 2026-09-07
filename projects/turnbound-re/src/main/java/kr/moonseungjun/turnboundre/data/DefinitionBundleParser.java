package kr.moonseungjun.turnboundre.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Pure deterministic parser/merger for reloadable definition bundle JSON resources. */
public final class DefinitionBundleParser {
    public record Parsed(DefinitionRegistry registry, String hash, List<String> resourceIds) {
        public Parsed {
            resourceIds = List.copyOf(resourceIds);
        }
    }

    private DefinitionBundleParser() {}

    public static Parsed parse(Map<String, String> resources) {
        if (resources == null) throw new IllegalArgumentException("resources must not be null");

        List<Map.Entry<String, String>> ordered = new ArrayList<>(resources.entrySet());
        ordered.sort(Map.Entry.comparingByKey(Comparator.naturalOrder()));

        List<ActionDefinition> actions = new ArrayList<>();
        List<CharacterDefinition> characters = new ArrayList<>();
        List<StatusDefinition> statuses = new ArrayList<>();
        List<EncounterDefinition> encounters = new ArrayList<>();
        List<RewardTableDefinition> rewards = new ArrayList<>();
        List<String> resourceIds = new ArrayList<>();
        MessageDigest digest = sha256();

        for (Map.Entry<String, String> entry : ordered) {
            String resourceId = requireText(entry.getKey(), "resource id");
            String json = requireText(entry.getValue(), "resource JSON for " + resourceId);
            DefinitionBundle bundle;
            try {
                bundle = DefinitionBundle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                        .getOrThrow(error -> new IllegalArgumentException(resourceId + ": " + error));
            } catch (RuntimeException error) {
                if (error instanceof IllegalArgumentException illegal) throw illegal;
                throw new IllegalArgumentException("Failed to decode TURNBOUND definitions from " + resourceId, error);
            }

            actions.addAll(bundle.actions());
            characters.addAll(bundle.characters());
            statuses.addAll(bundle.statuses());
            encounters.addAll(bundle.encounters());
            rewards.addAll(bundle.rewards());
            resourceIds.add(resourceId);
            updateDigest(digest, resourceId);
            updateDigest(digest, json);
        }

        DefinitionRegistry registry = DefinitionRegistry.create(actions, characters, statuses, encounters, rewards);
        return new Parsed(registry, HexFormat.of().formatHex(digest.digest()), resourceIds);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }
}
