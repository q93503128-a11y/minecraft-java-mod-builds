package io.github.q93503128.turnbound.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Version-pinned local migration for Drehmal: APOTHEOSIS 2.2.2f's world resource pack.
 *
 * <p>Minecraft 26.2 rejects uppercase characters in resource-location paths. Drehmal's 1.20.1 pack uses the
 * legacy parent id {@code minecraft:item/spawn_egg_2D}; this migrator preserves the authored spawn-egg model by
 * lowercasing that resource path (and its model entry when present) in the installed copy only.</p>
 */
final class Drehmal26_2ResourcePackMigrator {
    static final int COMPAT_VERSION = 3;
    static final String TARGET_VERSION = "26.2";
    static final String RESOURCE_PACK_RELATIVE = "resources.zip";
    static final String MARKER_FILE = ".turnbound_drehmal_resources_26_2_compat";
    static final String BACKUP_SUFFIX = ".turnbound-1_20_1-backup";

    static final String LEGACY_SPAWN_EGG_MODEL = "assets/minecraft/models/item/spawn_egg_2D.json";
    static final String MODERN_SPAWN_EGG_MODEL = "assets/minecraft/models/item/spawn_egg_2d.json";
    static final String LEGACY_SPAWN_EGG_PARENT = "minecraft:item/spawn_egg_2D";
    static final String MODERN_SPAWN_EGG_PARENT = "minecraft:item/spawn_egg_2d";
    private static final String VANILLA_GENERATED_PARENT = "minecraft:item/generated";

    record Report(
            boolean changed,
            int referencesRewritten,
            boolean modelRenamed,
            String sourceHash,
            String migratedHash
    ) {}

    private record Validation(int rewrittenReferences, boolean modernModelPresent) {}

    private Drehmal26_2ResourcePackMigrator() {}

    static boolean isCurrent(Path world) {
        if (world == null) return false;
        Path marker = world.resolve(MARKER_FILE);
        Path pack = world.resolve(RESOURCE_PACK_RELATIVE);
        if (!Files.isRegularFile(marker) || !Files.isRegularFile(pack)) return false;
        try {
            String text = Files.readString(marker, StandardCharsets.UTF_8);
            return text.contains("compatVersion=" + COMPAT_VERSION)
                    && text.contains("target=" + TARGET_VERSION)
                    && archiveLooksMigrated(pack);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean archiveLooksMigrated(Path archive) {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            if (zip.getEntry(LEGACY_SPAWN_EGG_MODEL) != null) return false;
            for (String name : List.of(
                    "assets/minecraft/models/item/bee_spawn_egg.json",
                    "assets/minecraft/models/item/bat_spawn_egg.json",
                    "assets/minecraft/models/item/zombie_spawn_egg.json")) {
                ZipEntry entry = zip.getEntry(name);
                if (entry == null) continue;
                try (InputStream stream = zip.getInputStream(entry)) {
                    String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    if (text.contains(LEGACY_SPAWN_EGG_PARENT)) return false;
                }
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    static Report migrate(Path world) throws IOException {
        if (world == null) throw new IOException("Drehmal world path is missing");
        Path source = world.resolve(RESOURCE_PACK_RELATIVE);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Drehmal resource pack is missing: " + RESOURCE_PACK_RELATIVE);
        }

        if (isCurrent(world)) {
            Validation validation = validateArchive(source);
            String hash = DrehmalInstallFiles.sha256(source);
            return new Report(false, validation.rewrittenReferences(), false, hash, hash);
        }

        String sourceHash = DrehmalInstallFiles.sha256(source);
        Validation compatible = tryValidate(source);
        if (compatible != null) {
            writeMarker(world, sourceHash, sourceHash, compatible.rewrittenReferences(), false);
            return new Report(false, compatible.rewrittenReferences(), false, sourceHash, sourceHash);
        }

        Path backup = source.resolveSibling(source.getFileName() + BACKUP_SUFFIX);
        if (!Files.exists(backup)) {
            Files.copy(source, backup);
        } else if (!Files.isRegularFile(backup)) {
            throw new IOException("Drehmal resource-pack backup path is not a file");
        }

        Path temp = source.resolveSibling(source.getFileName() + ".turnbound-26_2.tmp");
        Files.deleteIfExists(temp);

        try {
            RewriteResult rewrite = rewriteArchive(source, temp);
            validateArchive(temp);
            String migratedHash = DrehmalInstallFiles.sha256(temp);
            atomicReplace(temp, source);
            writeMarker(world, sourceHash, migratedHash, rewrite.referencesRewritten(), rewrite.modelRenamed());
            return new Report(
                    true,
                    rewrite.referencesRewritten(),
                    rewrite.modelRenamed(),
                    sourceHash,
                    migratedHash);
        } catch (Throwable failure) {
            Files.deleteIfExists(temp);
            if (failure instanceof IOException io) throw io;
            throw new IOException("Drehmal 26.2 resource-pack migration failed", failure);
        }
    }

    private record RewriteResult(int referencesRewritten, boolean modelRenamed) {}

    private static RewriteResult rewriteArchive(Path source, Path temp) throws IOException {
        Files.createDirectories(temp.getParent());
        try (ZipFile input = new ZipFile(source.toFile());
             OutputStream raw = Files.newOutputStream(temp);
             ZipOutputStream output = new ZipOutputStream(raw, StandardCharsets.UTF_8)) {

            boolean hasCustomSpawnEggModel = input.getEntry(LEGACY_SPAWN_EGG_MODEL) != null
                    || input.getEntry(MODERN_SPAWN_EGG_MODEL) != null;
            String replacementParent = hasCustomSpawnEggModel
                    ? MODERN_SPAWN_EGG_PARENT
                    : VANILLA_GENERATED_PARENT;

            int references = 0;
            boolean renamed = false;
            var entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String outputName = entry.getName();
                if (LEGACY_SPAWN_EGG_MODEL.equals(outputName)) {
                    outputName = MODERN_SPAWN_EGG_MODEL;
                    renamed = true;
                }

                ZipEntry outEntry = new ZipEntry(outputName);
                if (entry.getTime() >= 0L) outEntry.setTime(entry.getTime());
                output.putNextEntry(outEntry);
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    try (InputStream stream = input.getInputStream(entry)) {
                        bytes = stream.readAllBytes();
                    }
                    if (entry.getName().endsWith(".json")) {
                        String text = new String(bytes, StandardCharsets.UTF_8);
                        int count = occurrences(text, LEGACY_SPAWN_EGG_PARENT);
                        if (count > 0) {
                            text = text.replace(LEGACY_SPAWN_EGG_PARENT, replacementParent);
                            bytes = text.getBytes(StandardCharsets.UTF_8);
                            references += count;
                        }
                    }
                    output.write(bytes);
                }
                output.closeEntry();
            }
            return new RewriteResult(references, renamed);
        }
    }

    private static int occurrences(String text, String needle) {
        if (text == null || text.isEmpty() || needle == null || needle.isEmpty()) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int at = text.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + needle.length();
        }
    }

    private static Validation tryValidate(Path archive) {
        try {
            return validateArchive(archive);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static Validation validateArchive(Path archive) throws IOException {
        int modernReferences = 0;
        boolean modernModel = false;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            if (zip.getEntry("pack.mcmeta") == null) {
                throw new IOException("Drehmal resource pack is missing pack.mcmeta");
            }
            if (zip.getEntry(LEGACY_SPAWN_EGG_MODEL) != null) {
                throw new IOException("Legacy uppercase spawn-egg model path remains");
            }
            modernModel = zip.getEntry(MODERN_SPAWN_EGG_MODEL) != null;

            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".json")) continue;
                byte[] bytes;
                try (InputStream stream = zip.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                String text = new String(bytes, StandardCharsets.UTF_8);
                if (text.contains(LEGACY_SPAWN_EGG_PARENT)) {
                    throw new IOException("Legacy uppercase spawn-egg parent remains: " + entry.getName());
                }
                modernReferences += occurrences(text, MODERN_SPAWN_EGG_PARENT);
            }
        }
        return new Validation(modernReferences, modernModel);
    }

    private static void writeMarker(
            Path world,
            String sourceHash,
            String migratedHash,
            int referencesRewritten,
            boolean modelRenamed
    ) throws IOException {
        Path marker = world.resolve(MARKER_FILE);
        Path temp = marker.resolveSibling(marker.getFileName() + ".tmp");
        String text = "compatVersion=" + COMPAT_VERSION + System.lineSeparator()
                + "target=" + TARGET_VERSION + System.lineSeparator()
                + "sourceHash=" + sourceHash + System.lineSeparator()
                + "migratedHash=" + migratedHash + System.lineSeparator()
                + "referencesRewritten=" + referencesRewritten + System.lineSeparator()
                + "modelRenamed=" + modelRenamed + System.lineSeparator();
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
            throw new IOException("Atomic file replacement is not supported for Drehmal resource-pack migration", error);
        }
    }
}
