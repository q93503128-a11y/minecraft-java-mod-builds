package kr.moonseungjun.turnboundre.client.install;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Local compatibility transform for the official Drehmal 2.2.2f resource pack.
 *
 * <p>Minecraft 26.2 rejects identifiers containing uppercase path characters. The 1.20.1 pack still points
 * several spawn-egg models at {@code minecraft:item/spawn_egg_2D}; only that stale identifier is rewritten.
 * The original downloaded archive is backed up inside the TURNBOUND-owned save before replacement.</p>
 */
public final class Drehmal26_2ResourcePackMigrator {
    public static final String MIGRATION_ID = "drehmal-2.2.2f-resourcepack-to-mc-26.2-r1";
    public static final String COMPATIBILITY_MARKER_FILE = ".turnbound_re_26_2_resources";

    private static final String BACKUP_RELATIVE = ".turnbound-re-backup/resources-2.2.2f-original.zip";
    private static final byte[] LEGACY_TOKEN = "spawn_egg_2D".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MODERN_TOKEN = "spawn_egg_2d".getBytes(StandardCharsets.UTF_8);

    public record MigrationReport(int jsonFilesChanged, Path backup) {}

    private Drehmal26_2ResourcePackMigrator() {}

    public static Path compatibilityMarker(Path worldDirectory) {
        return worldDirectory.resolve(COMPATIBILITY_MARKER_FILE);
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
                    && isArchiveCompatible(DrehmalInstallFiles.worldResourcePack(worldDirectory));
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean isArchiveCompatible(Path archive) {
        if (!isReadableZip(archive)) return false;
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    byte[] bytes = readAll(input);
                    if (contains(bytes, LEGACY_TOKEN)) return false;
                }
                input.closeEntry();
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static MigrationReport migrate(Path worldDirectory) throws IOException {
        if (worldDirectory == null || !Files.isDirectory(worldDirectory)) {
            throw new IOException("Drehmal world directory unavailable for resource-pack migration");
        }
        Path resourcePack = DrehmalInstallFiles.worldResourcePack(worldDirectory);
        if (!isReadableZip(resourcePack)) {
            throw new IOException("Drehmal world resource pack unavailable");
        }
        if (compatibilityMarkerMatches(worldDirectory)) {
            return new MigrationReport(0, backup(worldDirectory));
        }

        Path backup = backup(worldDirectory);
        if (!Files.isRegularFile(backup)) {
            Files.createDirectories(backup.getParent());
            Files.copy(resourcePack, backup, StandardCopyOption.COPY_ATTRIBUTES);
        } else if (!isReadableZip(backup)) {
            throw new IOException("Drehmal resource-pack backup is not a readable ZIP");
        }

        if (isArchiveCompatible(resourcePack)) {
            writeMarker(worldDirectory);
            return new MigrationReport(0, backup);
        }

        Path partial = resourcePack.resolveSibling(resourcePack.getFileName() + ".turnbound-26_2.part");
        Files.deleteIfExists(partial);
        int changed = 0;
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(resourcePack));
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
                    if (name.endsWith(".json") && contains(original, LEGACY_TOKEN)) {
                        migrated = replace(original, LEGACY_TOKEN, MODERN_TOKEN);
                        changed++;
                    }
                    output.write(migrated);
                }
                output.closeEntry();
                input.closeEntry();
            }
        } catch (Throwable failure) {
            Files.deleteIfExists(partial);
            if (failure instanceof IOException io) throw io;
            throw new IOException("Drehmal 26.2 resource-pack migration failed", failure);
        }

        if (!isArchiveCompatible(partial)) {
            Files.deleteIfExists(partial);
            throw new IOException("migrated Drehmal resource pack failed compatibility validation");
        }
        moveAtomicOrReplace(partial, resourcePack);
        writeMarker(worldDirectory);
        return new MigrationReport(changed, backup);
    }

    private static void writeMarker(Path worldDirectory) throws IOException {
        Files.writeString(
                compatibilityMarker(worldDirectory),
                MIGRATION_ID + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        if (haystack == null || needle == null || needle.length == 0 || haystack.length < needle.length) return false;
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static byte[] replace(byte[] source, byte[] from, byte[] to) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(source.length);
        int cursor = 0;
        while (cursor < source.length) {
            boolean match = cursor <= source.length - from.length;
            if (match) {
                for (int i = 0; i < from.length; i++) {
                    if (source[cursor + i] != from[i]) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                output.write(to);
                cursor += from.length;
            } else {
                output.write(source[cursor]);
                cursor++;
            }
        }
        return output.toByteArray();
    }

    private static String normalizeEntryName(String name) throws IOException {
        if (name == null) throw new IOException("ZIP entry name missing");
        String normalized = name.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.contains("../")) {
            throw new IOException("unsafe resource-pack ZIP entry rejected: " + name);
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
