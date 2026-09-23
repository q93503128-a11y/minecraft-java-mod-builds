package io.github.q93503128.turnbound.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

final class DrehmalInstallFiles {
    private static final int BUFFER = 1024 * 256;

    private DrehmalInstallFiles() {}

    static String sha256(Path file) throws IOException {
        MessageDigest digest = sha256Digest();
        byte[] buffer = new byte[BUFFER];
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Matches the directory-hash algorithm published by the official Drehmal installer. */
    static String officialDirectoryHash(Path directory) throws IOException {
        return officialDirectoryHash(directory, null);
    }

    static String officialDirectoryHash(Path directory, BiConsumer<Long, Long> progress) throws IOException {
        long total = countFiles(directory);
        long[] done = {0L};
        return hashDirectory(directory, progress, done, total);
    }

    private static String hashDirectory(
            Path directory,
            BiConsumer<Long, Long> progress,
            long[] done,
            long total
    ) throws IOException {
        MessageDigest digest = sha256Digest();
        List<Path> entries;
        try (var stream = Files.list(directory)) {
            entries = stream.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
        }

        byte[] buffer = new byte[BUFFER];
        for (Path entry : entries) {
            if (Files.isRegularFile(entry)) {
                try (InputStream in = new BufferedInputStream(Files.newInputStream(entry))) {
                    int read;
                    while ((read = in.read(buffer)) >= 0) {
                        if (read > 0) digest.update(buffer, 0, read);
                    }
                }
                done[0]++;
                if (progress != null) progress.accept(done[0], total);
            } else if (Files.isDirectory(entry)) {
                String subHash = hashDirectory(entry, progress, done, total);
                digest.update(subHash.getBytes(StandardCharsets.UTF_8));
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static long countFiles(Path directory) throws IOException {
        long count = 0;
        try (var stream = Files.walk(directory)) {
            count = stream.filter(Files::isRegularFile).count();
        }
        return count;
    }

    static Path resolveZipEntry(Path root, String entryName) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(entryName).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IOException("Unsafe archive entry");
        }
        return resolved;
    }

    static long extractZip(Path archive, Path output, BiConsumer<Long, Long> progress, long completedBefore, long totalExpected)
            throws IOException {
        Files.createDirectories(output);
        long written = 0L;
        byte[] buffer = new byte[BUFFER];
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = resolveZipEntry(output, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    zip.closeEntry();
                    continue;
                }

                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (var out = Files.newOutputStream(target)) {
                    int read;
                    while ((read = zip.read(buffer)) >= 0) {
                        if (read <= 0) continue;
                        out.write(buffer, 0, read);
                        written += read;
                        if (progress != null) progress.accept(completedBefore + written, totalExpected);
                    }
                }
                zip.closeEntry();
            }
        }
        return written;
    }

    static boolean validResourcePack(Path archive) {
        if (archive == null || !Files.isRegularFile(archive)) return false;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            return zip.getEntry("pack.mcmeta") != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
