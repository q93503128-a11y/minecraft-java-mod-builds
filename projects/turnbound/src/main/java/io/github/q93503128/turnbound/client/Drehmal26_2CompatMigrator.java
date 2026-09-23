package io.github.q93503128.turnbound.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Version-pinned local migration for Drehmal: APOTHEOSIS 2.2.2f's hi_drehmal datapack.
 *
 * <p>The official 1.20.1 map is verified before this runs. Only the installed user's datapack copy is rewritten.
 * Region/chunk/NBT files are never opened. The original datapack is retained beside the migrated copy as a backup,
 * and the live archive is replaced only after the temporary archive passes the 26.2 structural checks.</p>
 */
final class Drehmal26_2CompatMigrator {
    static final int COMPAT_VERSION = 2;
    static final String TARGET_VERSION = "26.2";
    static final String DATAPACK_RELATIVE = "datapacks/hi_drehmal.zip";
    static final String MARKER_FILE = ".turnbound_drehmal_26_2_compat";
    static final String BACKUP_SUFFIX = ".turnbound-1_20_1-backup";

    private static final int EXPECTED_BIOMES = 48;
    private static final int EXPECTED_DIMENSION_TYPES = 1;
    private static final double MAX_CREATURE_SPAWN_PROBABILITY = 0.9999999D;
    private static final String NULL_SOUND_ID = "minecraft:null";
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    record Report(
            boolean changed,
            int biomes,
            int dimensionTypes,
            String sourceHash,
            String migratedHash
    ) {}

    private record Validation(int biomes, int dimensionTypes) {}

    private Drehmal26_2CompatMigrator() {}

    static boolean isCurrent(Path world) {
        if (world == null) return false;
        Path marker = world.resolve(MARKER_FILE);
        Path datapack = world.resolve(DATAPACK_RELATIVE);
        if (!Files.isRegularFile(marker) || !Files.isRegularFile(datapack)) return false;
        try {
            String text = Files.readString(marker, StandardCharsets.UTF_8);
            return text.contains("compatVersion=" + COMPAT_VERSION)
                    && text.contains("target=" + TARGET_VERSION);
        } catch (IOException ignored) {
            return false;
        }
    }

    static Report migrate(Path world) throws IOException {
        if (world == null) throw new IOException("Drehmal world path is missing");
        Path source = world.resolve(DATAPACK_RELATIVE);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Drehmal datapack is missing: " + DATAPACK_RELATIVE);
        }

        if (isCurrent(world)) {
            Validation validation = validateArchive(source);
            String hash = DrehmalInstallFiles.sha256(source);
            return new Report(false, validation.biomes(), validation.dimensionTypes(), hash, hash);
        }

        String sourceHash = DrehmalInstallFiles.sha256(source);
        Validation alreadyCompatible = tryValidate(source);
        if (alreadyCompatible != null) {
            writeMarker(world, sourceHash, sourceHash, alreadyCompatible);
            return new Report(
                    false,
                    alreadyCompatible.biomes(),
                    alreadyCompatible.dimensionTypes(),
                    sourceHash,
                    sourceHash);
        }

        Path backup = source.resolveSibling(source.getFileName() + BACKUP_SUFFIX);
        if (!Files.exists(backup)) {
            Files.copy(source, backup);
        } else if (!Files.isRegularFile(backup)) {
            throw new IOException("Drehmal compatibility backup path is not a file");
        }

        Path temp = source.resolveSibling(source.getFileName() + ".turnbound-26_2.tmp");
        Files.deleteIfExists(temp);

        try {
            rewriteArchive(source, temp);
            Validation validation = validateArchive(temp);
            String migratedHash = DrehmalInstallFiles.sha256(temp);
            atomicReplace(temp, source);
            writeMarker(world, sourceHash, migratedHash, validation);
            return new Report(
                    true,
                    validation.biomes(),
                    validation.dimensionTypes(),
                    sourceHash,
                    migratedHash);
        } catch (Throwable failure) {
            Files.deleteIfExists(temp);
            if (failure instanceof IOException io) throw io;
            throw new IOException("Drehmal 26.2 compatibility migration failed", failure);
        }
    }

    private static Validation tryValidate(Path archive) {
        try {
            return validateArchive(archive);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void rewriteArchive(Path source, Path temp) throws IOException {
        Files.createDirectories(temp.getParent());
        try (ZipFile input = new ZipFile(source.toFile());
             OutputStream raw = Files.newOutputStream(temp);
             ZipOutputStream output = new ZipOutputStream(raw, StandardCharsets.UTF_8)) {
            var entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry outEntry = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0L) outEntry.setTime(entry.getTime());
                output.putNextEntry(outEntry);
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    try (InputStream stream = input.getInputStream(entry)) {
                        bytes = stream.readAllBytes();
                    }
                    if (isBiome(entry.getName())) {
                        bytes = migrateJson(bytes, Drehmal26_2CompatMigrator::migrateBiome);
                    } else if (isDimensionType(entry.getName())) {
                        bytes = migrateJson(bytes, Drehmal26_2CompatMigrator::migrateDimensionType);
                    }
                    output.write(bytes);
                }
                output.closeEntry();
            }
        }
    }

    private interface JsonMigration {
        void apply(JsonObject root) throws IOException;
    }

    private static byte[] migrateJson(byte[] source, JsonMigration migration) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IOException("Registry element is not a JSON object");
            JsonObject root = parsed.getAsJsonObject();
            migration.apply(root);
            return (JSON.toJson(root) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException error) {
            throw new IOException("Could not parse Drehmal registry JSON", error);
        }
    }

    private static void migrateDimensionType(JsonObject root) {
        if (!root.has("has_ender_dragon_fight")) {
            root.addProperty("has_ender_dragon_fight", false);
        }
    }

    private static void migrateBiome(JsonObject root) throws IOException {
        JsonElement carvers = root.get("carvers");
        if (carvers != null && carvers.isJsonObject()) {
            JsonArray flattened = new JsonArray();
            for (Map.Entry<String, JsonElement> entry : carvers.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value == null || value.isJsonNull()) continue;
                if (value.isJsonArray()) {
                    for (JsonElement element : value.getAsJsonArray()) {
                        flattened.add(element.deepCopy());
                    }
                } else if (value.isJsonPrimitive()) {
                    flattened.add(value.deepCopy());
                } else if (value.isJsonObject() && value.getAsJsonObject().isEmpty()) {
                    // Empty legacy generation-step object contributes no carvers.
                } else {
                    throw new IOException("Unsupported legacy biome carver value for step " + entry.getKey());
                }
            }
            root.add("carvers", flattened);
        }

        JsonElement probability = root.get("creature_spawn_probability");
        if (probability != null && probability.isJsonPrimitive()) {
            JsonPrimitive primitive = probability.getAsJsonPrimitive();
            if (primitive.isNumber() && primitive.getAsDouble() >= 1.0D) {
                root.addProperty("creature_spawn_probability", MAX_CREATURE_SPAWN_PROBABILITY);
            }
        }

        migrateBiomePresentation(root);
    }

    private static void migrateBiomePresentation(JsonObject root) {
        JsonObject effects = object(root.get("effects"));
        JsonObject attributes = object(root.get("attributes"));
        if (effects == null && attributes == null) return;

        boolean createdAttributes = attributes == null;
        if (attributes == null) attributes = new JsonObject();

        boolean changed = sanitizeAudioAttributes(attributes);

        if (effects != null) {
            changed |= moveColor(effects, attributes, "sky_color", "minecraft:visual/sky_color");
            changed |= moveColor(effects, attributes, "fog_color", "minecraft:visual/fog_color");
            changed |= moveColor(effects, attributes, "water_fog_color", "minecraft:visual/water_fog_color");

            JsonObject ambientSounds = object(attributes.get("minecraft:audio/ambient_sounds"));
            if (ambientSounds == null) ambientSounds = new JsonObject();
            boolean ambientChanged = false;

            JsonElement ambient = effects.get("ambient_sound");
            if (ambient != null && ambient.isJsonPrimitive()) {
                if (!ambientSounds.has("loop") && usableSoundReference(ambient)) {
                    ambientSounds.add("loop", ambient.deepCopy());
                    ambientChanged = true;
                }
                effects.remove("ambient_sound");
                changed = true;
            }

            JsonElement mood = effects.get("mood_sound");
            if (mood != null && mood.isJsonObject()) {
                if (!ambientSounds.has("mood") && usableSoundObject(mood.getAsJsonObject())) {
                    ambientSounds.add("mood", mood.deepCopy());
                    ambientChanged = true;
                }
                effects.remove("mood_sound");
                changed = true;
            }

            JsonElement additions = effects.get("additions_sound");
            if (additions != null && additions.isJsonObject()) {
                if (!ambientSounds.has("additions") && usableSoundObject(additions.getAsJsonObject())) {
                    ambientSounds.add("additions", additions.deepCopy());
                    ambientChanged = true;
                }
                effects.remove("additions_sound");
                changed = true;
            }

            if (ambientChanged) {
                attributes.add("minecraft:audio/ambient_sounds", ambientSounds);
            }

            JsonElement music = effects.get("music");
            if (music != null && music.isJsonObject()) {
                if (validMusic(music.getAsJsonObject())) {
                    JsonObject background = object(attributes.get("minecraft:audio/background_music"));
                    if (background == null) background = new JsonObject();
                    if (!background.has("default")) {
                        background.add("default", music.deepCopy());
                        attributes.add("minecraft:audio/background_music", background);
                    }
                }
                effects.remove("music");
                changed = true;
            }

            JsonElement particle = effects.get("particle");
            if (particle != null && particle.isJsonObject() && !attributes.has("minecraft:visual/ambient_particles")) {
                JsonObject legacy = particle.getAsJsonObject();
                JsonObject options = object(legacy.get("options"));
                JsonElement probability = legacy.get("probability");
                if (options != null && probability != null && probability.isJsonPrimitive()) {
                    JsonObject migrated = new JsonObject();
                    migrated.add("particle", options.deepCopy());
                    migrated.add("probability", probability.deepCopy());
                    JsonArray particles = new JsonArray();
                    particles.add(migrated);
                    attributes.add("minecraft:visual/ambient_particles", particles);
                    effects.remove("particle");
                    changed = true;
                }
            }
        }

        if (attributes.isEmpty()) {
            if (!createdAttributes) {
                root.remove("attributes");
            }
        } else if (changed || !createdAttributes) {
            root.add("attributes", attributes);
        }
    }

    private static boolean sanitizeAudioAttributes(JsonObject attributes) {
        boolean changed = false;

        JsonObject ambientSounds = object(attributes.get("minecraft:audio/ambient_sounds"));
        if (ambientSounds != null) {
            changed |= removeNullSound(ambientSounds, "loop");
            changed |= removeNullSound(ambientSounds, "mood");
            changed |= removeNullSound(ambientSounds, "additions");
            if (ambientSounds.isEmpty()) {
                attributes.remove("minecraft:audio/ambient_sounds");
                changed = true;
            }
        }

        JsonObject background = object(attributes.get("minecraft:audio/background_music"));
        if (background != null) {
            changed |= removeNullSound(background, "default");
            if (background.isEmpty()) {
                attributes.remove("minecraft:audio/background_music");
                changed = true;
            }
        }

        return changed;
    }

    private static boolean removeNullSound(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (!hasNullSoundReference(value)) return false;
        parent.remove(key);
        return true;
    }

    private static boolean hasNullAudioSentinel(JsonObject attributes) {
        JsonObject ambientSounds = object(attributes.get("minecraft:audio/ambient_sounds"));
        if (ambientSounds != null
                && (hasNullSoundReference(ambientSounds.get("loop"))
                || hasNullSoundReference(ambientSounds.get("mood"))
                || hasNullSoundReference(ambientSounds.get("additions")))) {
            return true;
        }

        JsonObject background = object(attributes.get("minecraft:audio/background_music"));
        return background != null && hasNullSoundReference(background.get("default"));
    }

    private static boolean hasNullSoundReference(JsonElement value) {
        if (value == null || value.isJsonNull()) return false;
        if (value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString()) {
            return NULL_SOUND_ID.equals(value.getAsString());
        }
        if (!value.isJsonObject()) return false;
        JsonElement sound = value.getAsJsonObject().get("sound");
        return sound != null
                && sound.isJsonPrimitive()
                && sound.getAsJsonPrimitive().isString()
                && NULL_SOUND_ID.equals(sound.getAsString());
    }

    private static boolean usableSoundReference(JsonElement sound) {
        return sound != null
                && sound.isJsonPrimitive()
                && sound.getAsJsonPrimitive().isString()
                && !NULL_SOUND_ID.equals(sound.getAsString());
    }

    private static boolean usableSoundObject(JsonObject value) {
        return value != null && usableSoundReference(value.get("sound"));
    }

    private static boolean validMusic(JsonObject music) {
        return music != null && usableSoundReference(music.get("sound"));
    }

    private static boolean moveColor(
            JsonObject effects,
            JsonObject attributes,
            String legacyKey,
            String attributeKey
    ) {
        if (attributes.has(attributeKey)) return false;
        JsonElement value = effects.get(legacyKey);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return false;
        int rgb = value.getAsInt() & 0xFFFFFF;
        attributes.addProperty(attributeKey, String.format("#%06x", rgb));
        effects.remove(legacyKey);
        return true;
    }

    private static JsonObject object(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static Validation validateArchive(Path archive) throws IOException {
        int biomes = 0;
        int dimensions = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            if (zip.getEntry("pack.mcmeta") == null) {
                throw new IOException("Drehmal datapack is missing pack.mcmeta");
            }

            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                if (!isBiome(entry.getName()) && !isDimensionType(entry.getName())) continue;

                JsonObject root;
                try (InputStream stream = zip.getInputStream(entry)) {
                    JsonElement parsed = JsonParser.parseReader(
                            new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
                    if (!parsed.isJsonObject()) {
                        throw new IOException("Registry element is not an object: " + entry.getName());
                    }
                    root = parsed.getAsJsonObject();
                } catch (RuntimeException error) {
                    throw new IOException("Invalid registry JSON: " + entry.getName(), error);
                }

                if (isBiome(entry.getName())) {
                    biomes++;
                    JsonElement carvers = root.get("carvers");
                    if (carvers != null && carvers.isJsonObject()) {
                        throw new IOException("Legacy object-form biome carvers remain: " + entry.getName());
                    }
                    JsonElement probability = root.get("creature_spawn_probability");
                    if (probability != null
                            && probability.isJsonPrimitive()
                            && probability.getAsJsonPrimitive().isNumber()
                            && probability.getAsDouble() >= 1.0D) {
                        throw new IOException("Out-of-range creature_spawn_probability remains: " + entry.getName());
                    }
                    JsonObject attributes = object(root.get("attributes"));
                    if (attributes != null && hasNullAudioSentinel(attributes)) {
                        throw new IOException("Invalid minecraft:null sound reference remains: " + entry.getName());
                    }
                } else {
                    dimensions++;
                    JsonElement dragon = root.get("has_ender_dragon_fight");
                    if (dragon == null
                            || !dragon.isJsonPrimitive()
                            || !dragon.getAsJsonPrimitive().isBoolean()) {
                        throw new IOException("Missing has_ender_dragon_fight: " + entry.getName());
                    }
                }
            }
        }

        if (biomes != EXPECTED_BIOMES || dimensions != EXPECTED_DIMENSION_TYPES) {
            throw new IOException(
                    "Unexpected Drehmal registry inventory: biomes=" + biomes + ", dimensionTypes=" + dimensions);
        }
        return new Validation(biomes, dimensions);
    }

    private static boolean isBiome(String name) {
        return name != null
                && name.startsWith("data/")
                && name.contains("/worldgen/biome/")
                && name.endsWith(".json");
    }

    private static boolean isDimensionType(String name) {
        return name != null
                && name.startsWith("data/")
                && name.contains("/dimension_type/")
                && name.endsWith(".json");
    }

    private static void writeMarker(
            Path world,
            String sourceHash,
            String migratedHash,
            Validation validation
    ) throws IOException {
        Path marker = world.resolve(MARKER_FILE);
        Path temp = marker.resolveSibling(marker.getFileName() + ".tmp");
        String text = "compatVersion=" + COMPAT_VERSION + System.lineSeparator()
                + "target=" + TARGET_VERSION + System.lineSeparator()
                + "sourceHash=" + sourceHash + System.lineSeparator()
                + "migratedHash=" + migratedHash + System.lineSeparator()
                + "biomes=" + validation.biomes() + System.lineSeparator()
                + "dimensionTypes=" + validation.dimensionTypes() + System.lineSeparator();
        Files.writeString(temp, text, StandardCharsets.UTF_8);
        atomicReplace(temp, marker);
    }

    private static void atomicReplace(Path source, Path destination) throws IOException {
        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            throw new IOException("Atomic file replacement is not supported for Drehmal compatibility migration", error);
        }
    }
}
