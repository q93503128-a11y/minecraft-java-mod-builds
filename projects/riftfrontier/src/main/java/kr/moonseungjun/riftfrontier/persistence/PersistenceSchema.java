package kr.moonseungjun.riftfrontier.persistence;

/** Stable root contract for every authoritative Riftfrontier saved-data payload. */
public final class PersistenceSchema {
    public static final int CURRENT = 3;
    public static final String VERSION_KEY = "riftfrontier_schema_version";

    private PersistenceSchema() {}

    public static void requireSupported(int version) {
        if (version != CURRENT) {
            throw new IllegalArgumentException("Unsupported Riftfrontier persistence schema " + version + "; expected " + CURRENT);
        }
    }
}
