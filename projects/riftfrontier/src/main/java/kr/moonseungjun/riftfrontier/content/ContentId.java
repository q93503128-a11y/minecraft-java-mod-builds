package kr.moonseungjun.riftfrontier.content;

import java.util.Objects;
import java.util.regex.Pattern;

public record ContentId(String namespace, String path) implements Comparable<ContentId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z][a-z0-9_.-]{0,63}");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public ContentId {
        namespace = Objects.requireNonNull(namespace, "namespace");
        path = Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) throw new IllegalArgumentException("Invalid namespace: " + namespace);
        if (!PATH.matcher(path).matches() || path.startsWith("/") || path.endsWith("/") || path.contains("//")) throw new IllegalArgumentException("Invalid path: " + path);
    }

    public static ContentId rift(String path) { return new ContentId("riftfrontier", path); }

    public static ContentId parse(String value) {
        int split = value.indexOf(':');
        if (split <= 0 || split == value.length() - 1 || value.indexOf(':', split + 1) >= 0) throw new IllegalArgumentException("Expected namespace:path, got: " + value);
        return new ContentId(value.substring(0, split), value.substring(split + 1));
    }

    @Override public String toString() { return namespace + ':' + path; }
    @Override public int compareTo(ContentId other) { return toString().compareTo(other.toString()); }
}
