package kr.moonseungjun.turnboundre.client.install;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Local compatibility transform for Drehmal APOTHEOSIS 2.2.2f's 1.20.1 datapack.
 *
 * <p>The official map is verified before this transform. The original datapack is backed up inside the
 * TURNBOUND-owned save, then only the registry JSON shapes that 26.2 rejects are migrated. No Drehmal bytes
 * are redistributed by TURNBOUND.</p>
 */
public final class Drehmal26_2DatapackMigrator {
    public static final String MIGRATION_ID = "drehmal-2.2.2f-to-mc-26.2-r1";
    public static final String COMPATIBILITY_MARKER_FILE = ".turnbound_re_26_2_compat";

    private static final String DATAPACK_RELATIVE = "datapacks/hi_drehmal.zip";
    private static final String BACKUP_RELATIVE = ".turnbound-re-backup/hi_drehmal-2.2.2f-original.zip";
    private static final double MAX_CREATURE_SPAWN_PROBABILITY = 0.9999999D;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public record MigrationReport(int biomeFilesChanged, int dimensionTypeFilesChanged, Path backup) {
        public int totalFilesChanged() {
            return biomeFilesChanged + dimensionTypeFilesChanged;
        }
    }

    private Drehmal26_2DatapackMigrator() {}

    public static Path compatibilityMarker(Path worldDirectory) {
        return worldDirectory.resolve(COMPATIBILITY_MARKER_FILE);
    }

    public static Path datapack(Path worldDirectory) {
        return worldDirectory.resolve(DATAPACK_RELATIVE);
    }

    public static Path backup(Path worldDirectory) {
        return worldDirectory.resolve(BACKUP_RELATIVE);
    }

    public static boolean compatibilityMarkerMatches(Path worldDirectory) {
        if (worldDirectory == null) return false;
        Path marker = compatibilityMarker(worldDirectory);
        try {
            return Files.isRegularFile(marker)
                    && MIGRATION_ID.equals(Files.readString(marker, StandardCharsets.UTF_8).trim())
                    && isReadableZip(datapack(worldDirectory));
        } catch (IOException ignored) {
            return false;
        }
    }

    public static MigrationReport migrate(Path worldDirectory) throws IOException {
        if (worldDirectory == null || !Files.isDirectory(worldDirectory)) {
            throw new IOException("Drehmal world directory unavailable for 26.2 migration");
        }
        Path datapack = datapack(worldDirectory);
        if (!isReadableZip(datapack)) {
            throw new IOException("Drehmal datapack hi_drehmal.zip unavailable");
        }
        if (compatibilityMarkerMatches(worldDirectory)) {
            return new MigrationReport(0, 0, backup(worldDirectory));
        }

        Path backup = backup(worldDirectory);
        if (!Files.isRegularFile(backup)) {
            Files.createDirectories(backup.getParent());
            Files.copy(datapack, backup, StandardCopyOption.COPY_ATTRIBUTES);
        } else if (!isReadableZip(backup)) {
            throw new IOException("Drehmal compatibility backup is not a readable ZIP");
        }

        Path partial = datapack.resolveSibling(datapack.getFileName() + ".turnbound-26_2.part");
        Files.deleteIfExists(partial);

        int biomeFilesChanged = 0;
        int dimensionTypeFilesChanged = 0;
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(datapack));
             ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(partial))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = normalizeEntryName(entry.getName());
                ZipEntry migratedEntry = new ZipEntry(name);
                if (entry.getTime() >= 0) migratedEntry.setTime(entry.getTime());
                output.putNextEntry(migratedEntry);

                if (!entry.isDirectory()) {
                    byte[] original = readAll(input);
                    byte[] migrated = original;
                    if (isBiomeJson(name)) {
                        migrated = migrateBiome(original);
                        if (!java.util.Arrays.equals(original, migrated)) biomeFilesChanged++;
                    } else if (isDimensionTypeJson(name)) {
                        migrated = migrateDimensionType(name, original);
                        if (!java.util.Arrays.equals(original, migrated)) dimensionTypeFilesChanged++;
                    }
                    output.write(migrated);
                }
                output.closeEntry();
                input.closeEntry();
            }
        } catch (Throwable failure) {
            Files.deleteIfExists(partial);
            if (failure instanceof IOException io) throw io;
            throw new IOException("Drehmal 26.2 datapack migration failed", failure);
        }

        if (!isReadableZip(partial)) {
            Files.deleteIfExists(partial);
            throw new IOException("migrated Drehmal datapack is not a readable ZIP");
        }
        try {
            validateMigratedDatapack(partial);
        } catch (IOException failure) {
            Files.deleteIfExists(partial);
            throw failure;
        }

        moveAtomicOrReplace(partial, datapack);
        Files.writeString(
                compatibilityMarker(worldDirectory),
                MIGRATION_ID + System.lineSeparator(),
                StandardCharsets.UTF_8);
        return new MigrationReport(biomeFilesChanged, dimensionTypeFilesChanged, backup);
    }

    static JsonObject migrateBiomeObject(JsonObject root) {
        JsonObject migrated = root.deepCopy();

        JsonElement carvers = migrated.get("carvers");
        if (carvers != null && carvers.isJsonObject()) {
            JsonArray flattened = new JsonArray();
            Set<String> seenStrings = new LinkedHashSet<>();
            for (var entry : carvers.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value == null || value.isJsonNull()) continue;
                if (value.isJsonArray()) {
                    for (JsonElement carver : value.getAsJsonArray()) {
                        addUniqueHolder(flattened, seenStrings, carver);
                    }
                } else {
                    addUniqueHolder(flattened, seenStrings, value);
                }
            }
            migrated.add("carvers", flattened);
        }

        JsonElement probability = migrated.get("creature_spawn_probability");
        if (probability != null && probability.isJsonPrimitive() && probability.getAsJsonPrimitive().isNumber()) {
            double value = probability.getAsDouble();
            double clamped = Math.max(0.0D, Math.min(MAX_CREATURE_SPAWN_PROBABILITY, value));
            if (Double.compare(value, clamped) != 0) {
                migrated.addProperty("creature_spawn_probability", clamped);
            }
        }

        return migrated;
    }

    static JsonObject migrateDimensionTypeObject(String entryName, JsonObject root) {
        JsonObject migrated = root.deepCopy();
        if (!migrated.has("has_ender_dragon_fight")) {
            boolean isVanillaEnd = entryName.endsWith("/dimension_type/the_end.json");
            migrated.addProperty("has_ender_dragon_fight", isVanillaEnd);
        }

        if (!migrated.has("skybox") && migrated.has("effects") && migrated.get("effects").isJsonPrimitive()) {
            String effects = migrated.get("effects").getAsString();
            if ("minecraft:the_end".equals(effects)) {
                migrated.addProperty("skybox", "end");
            } else if ("minecraft:the_nether".equals(effects)) {
                migrated.addProperty("skybox", "none");
            }
        }
        return migrated;
    }

    static void validateMigratedDatapack(Path archive) throws IOException {
        int biomes = 0;
        int dimensionTypes = 0;
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = normalizeEntryName(entry.getName());
                if (!entry.isDirectory() && (isBiomeJson(name) || isDimensionTypeJson(name))) {
                    byte[] bytes = readAll(input);
                    JsonElement parsed;
                    try {
                        parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
                    } catch (RuntimeException failure) {
                        throw new IOException("migrated registry JSON is invalid: " + name, failure);
                    }
                    if (!parsed.isJsonObject()) {
                        throw new IOException("migrated registry JSON root must be an object: " + name);
                    }
                    JsonObject object = parsed.getAsJsonObject();
                    if (isBiomeJson(name)) {
                        biomes++;
                        JsonElement carvers = object.get("carvers");
                        if (carvers != null && carvers.isJsonObject()) {
                            throw new IOException("legacy biome carver map remains after migration: " + name);
                        }
                        JsonElement probability = object.get("creature_spawn_probability");
                        if (probability != null && probability.isJsonPrimitive()
                                && probability.getAsJsonPrimitive().isNumber()) {
                            double value = probability.getAsDouble();
                            if (value < 0.0D || value > MAX_CREATURE_SPAWN_PROBABILITY) {
                                throw new IOException("biome creature spawn probability remains outside 26.2 range: " + name);
                            }
                        }
                    } else {
                        dimensionTypes++;
                        if (!object.has("has_ender_dragon_fight")
                                || !object.get("has_ender_dragon_fight").isJsonPrimitive()
                                || !object.getAsJsonPrimitive("has_ender_dragon_fight").isBoolean()) {
                            throw new IOException("dimension type missing 26.2 dragon-fight flag: " + name);
                        }
                    }
                }
                input.closeEntry();
            }
        }
        if (biomes == 0 || dimensionTypes == 0) {
            throw new IOException("Drehmal datapack missing expected biome or dimension-type registry data");
        }
    }

    private static void addUniqueHolder(JsonArray output, Set<String> seenStrings, JsonElement value) {
        if (value == null || value.isJsonNull()) return;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String id = value.getAsString();
            if (seenStrings.add(id)) output.add(id);
            return;
        }
        output.add(value.deepCopy());
    }

    private static byte[] migrateBiome(byte[] original) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IOException("biome JSON root must be an object");
            return GSON.toJson(migrateBiomeObject(parsed.getAsJsonObject())).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException failure) {
            throw new IOException("failed to migrate Drehmal biome JSON", failure);
        }
    }

    private static byte[] migrateDimensionType(String entryName, byte[] original) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(new String(original, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IOException("dimension type JSON root must be an object");
            return GSON.toJson(migrateDimensionTypeObject(entryName, parsed.getAsJsonObject())).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException failure) {
            throw new IOException("failed to migrate Drehmal dimension type JSON", failure);
        }
    }

    private static boolean isBiomeJson(String name) {
        return name.startsWith("data/")
                && name.contains("/worldgen/biome/")
                && name.endsWith(".json");
    }

    private static boolean isDimensionTypeJson(String name) {
        return name.startsWith("data/")
                && name.contains("/dimension_type/")
                && name.endsWith(".json");
    }

    private static String normalizeEntryName(String name) throws IOException {
        if (name == null) throw new IOException("ZIP entry name missing");
        String normalized = name.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("../")) {
            throw new IOException("unsafe datapack ZIP entry rejected: " + name);
        }
        return normalized;
    }

    private static byte[] readAll(ZipInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        input.transferTo(output);
        return output.toByteArray();
    }

    private static boolean isReadableZip(Path path) {
        if (path == null || !Files.isRegularFile(path)) return false;
        try (ZipFile ignored = new ZipFile(path.toFile())) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void moveAtomicOrReplace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
