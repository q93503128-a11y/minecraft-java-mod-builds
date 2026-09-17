package kr.moonseungjun.turnboundre.client.install;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** Pure file/verification contract shared by the Modrinth first-run installer and unit tests. */
public final class DrehmalInstallFiles {
    public static final String WORLD_VERSION = "2.2.2f";
    public static final String PROFILE_ID = "turnbound_re:drehmal_apotheosis_2_2_2f";
    public static final String WORLD_FOLDER_NAME = "TURNBOUND RE - Drehmal APOTHEOSIS 2.2.2f";
    public static final String PROFILE_MARKER_FILE = ".turnbound_re_profile";
    public static final String DISTRIBUTION_MARKER = "turnbound_re_distribution.properties";
    public static final String EXPECTED_MAP_HASH = "2e6232dc3e97c77eaa006b09e3ee09b246c46e3df68359dcc1495ce19d1e8053";
    public static final long MIN_FREE_BYTES = 12L * 1024L * 1024L * 1024L;

    public static final DownloadSpec SHARD_1 = new DownloadSpec(
            "shard_1.zip",
            URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_1.zip"),
            1_484_178_230L,
            "378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358");
    public static final DownloadSpec SHARD_2 = new DownloadSpec(
            "shard_2.zip",
            URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_2.zip"),
            1_641_289_974L,
            "2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331");
    public static final DownloadSpec SHARD_3 = new DownloadSpec(
            "shard_3.zip",
            URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_3.zip"),
            861_863_079L,
            "8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67");
    public static final DownloadSpec RESOURCE_PACK = new DownloadSpec(
            "resources.zip",
            URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/resources.zip"),
            190_255_507L,
            null);
    public static final List<DownloadSpec> MAP_SHARDS = List.of(SHARD_1, SHARD_2, SHARD_3);

    public record DownloadSpec(String fileName, URI uri, long expectedSize, String sha256) {
        public DownloadSpec {
            if (fileName == null || fileName.isBlank() || uri == null || expectedSize <= 0) {
                throw new IllegalArgumentException("download specification fields required");
            }
            if (sha256 != null && !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("download SHA-256 must be lowercase hex");
            }
        }
    }

    private DrehmalInstallFiles() {}

    public static Path distributionMarker(Path gameDirectory) {
        return gameDirectory.resolve("config").resolve(DISTRIBUTION_MARKER);
    }

    public static boolean isModrinthDistribution(Path gameDirectory) {
        if (gameDirectory == null) return false;
        Path marker = distributionMarker(gameDirectory);
        if (!Files.isRegularFile(marker)) return false;
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(marker, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return "modrinth".equalsIgnoreCase(properties.getProperty("distribution", "").trim())
                    && Boolean.parseBoolean(properties.getProperty("autoInstallDrehmal", "false").trim());
        } catch (IOException ignored) {
            return false;
        }
    }

    public static Path worldDirectory(Path gameDirectory) {
        return gameDirectory.resolve("saves").resolve(WORLD_FOLDER_NAME);
    }

    public static Path profileMarker(Path worldDirectory) {
        return worldDirectory.resolve(PROFILE_MARKER_FILE);
    }

    /** Minecraft 26.1+ world resource packs live under the world's resourcepacks directory. */
    public static Path worldResourcePack(Path worldDirectory) {
        return worldDirectory.resolve("resourcepacks").resolve("resources.zip");
    }

    public static Path cacheDirectory(Path gameDirectory) {
        return gameDirectory.resolve(".turnbound-re-cache").resolve(WORLD_VERSION);
    }

    public static boolean profileMarkerMatches(Path worldDirectory) {
        if (worldDirectory == null) return false;
        Path marker = profileMarker(worldDirectory);
        try {
            return Files.isRegularFile(marker)
                    && PROFILE_ID.equals(Files.readString(marker, StandardCharsets.UTF_8).trim());
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean isReadableZip(Path path) {
        if (path == null || !Files.isRegularFile(path)) return false;
        try (ZipFile ignored = new ZipFile(path.toFile())) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean resourcePackReady(Path worldDirectory) {
        Path resourcePack = worldResourcePack(worldDirectory);
        try {
            return Files.isRegularFile(resourcePack)
                    && Files.size(resourcePack) == RESOURCE_PACK.expectedSize()
                    && isReadableZip(resourcePack);
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean worldReady(Path gameDirectory) {
        if (gameDirectory == null) return false;
        Path world = worldDirectory(gameDirectory);
        return Files.isRegularFile(world.resolve("level.dat"))
                && profileMarkerMatches(world)
                && Drehmal26_2DatapackMigrator.compatibilityMarkerMatches(world)
                && resourcePackReady(world);
    }

    public static boolean verifiedDownload(Path path, DownloadSpec spec) throws IOException {
        if (path == null || spec == null || !Files.isRegularFile(path)) return false;
        if (Files.size(path) != spec.expectedSize()) return false;
        if (!isReadableZip(path)) return false;
        return spec.sha256() == null || spec.sha256().equals(sha256(path));
    }

    public static String sha256(Path file) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Reproduces the official Drehmal installer directory hash: sorted entries, raw file bytes, and recursive
     * directory digest hex strings fed into each parent digest.
     */
    public static String directoryHash(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("directory required for Drehmal hash");
        }
        MessageDigest digest = newDigest();
        List<Path> entries;
        try (var stream = Files.list(directory)) {
            entries = stream
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        byte[] buffer = new byte[1024 * 1024];
        for (Path entry : entries) {
            if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                digest.update(directoryHash(entry).getBytes(StandardCharsets.UTF_8));
            } else if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                try (InputStream input = Files.newInputStream(entry)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        if (read > 0) digest.update(buffer, 0, read);
                    }
                }
            } else {
                throw new IOException("unsupported filesystem entry in Drehmal world: " + entry.getFileName());
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static void expandZipMerged(Path archive, Path destination) throws IOException {
        if (!isReadableZip(archive)) throw new IOException("archive is not a readable ZIP: " + archive.getFileName());
        Path root = destination.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.isBlank() || name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
                    throw new IOException("unsafe ZIP entry rejected: " + entry.getName());
                }
                Path target = root.resolve(name).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException("unsafe ZIP entry rejected: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    public static void writeProfileMarker(Path worldDirectory) throws IOException {
        Files.writeString(profileMarker(worldDirectory), PROFILE_ID + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
