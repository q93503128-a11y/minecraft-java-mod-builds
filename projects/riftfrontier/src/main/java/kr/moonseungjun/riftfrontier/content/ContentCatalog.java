package kr.moonseungjun.riftfrontier.content;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable, deterministic index used by validation reports, saves and future client snapshots. */
public record ContentCatalog(int schemaVersion, List<Entry> entries, Map<CoreDefinition.Kind, Integer> counts, String fingerprint) {
    public record Entry(CoreDefinition.Kind kind, ContentId id) implements Comparable<Entry> {
        @Override public int compareTo(Entry other) {
            int kindOrder = Integer.compare(kind.ordinal(), other.kind.ordinal());
            return kindOrder != 0 ? kindOrder : id.compareTo(other.id);
        }
    }

    public ContentCatalog {
        entries = List.copyOf(entries);
        counts = Map.copyOf(counts);
    }

    public static ContentCatalog from(ContentRegistry registry) {
        List<Entry> entries = registry.all().stream()
            .map(definition -> new Entry(definition.kind(), definition.id()))
            .sorted()
            .toList();

        Map<CoreDefinition.Kind, Integer> counts = new EnumMap<>(CoreDefinition.Kind.class);
        for (CoreDefinition.Kind kind : CoreDefinition.Kind.values()) counts.put(kind, 0);
        for (Entry entry : entries) counts.merge(entry.kind(), 1, Integer::sum);

        return new ContentCatalog(ContentPackLoader.CURRENT_CONTENT_SCHEMA, entries, counts, fingerprint(entries));
    }

    private static String fingerprint(List<Entry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Entry entry : entries) {
                digest.update(entry.kind().name().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) ':');
                digest.update(entry.id().toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
