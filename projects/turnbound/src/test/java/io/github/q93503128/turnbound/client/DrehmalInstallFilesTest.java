package io.github.q93503128.turnbound.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrehmalInstallFilesTest {
    @TempDir Path temp;

    @Test
    void officialDirectoryHashMatchesUpstreamRecursiveAlgorithm() throws Exception {
        Files.writeString(temp.resolve("a.txt"), "alpha");
        Files.createDirectories(temp.resolve("sub"));
        Files.writeString(temp.resolve("sub").resolve("b.txt"), "beta");

        assertEquals(
                "584072e00e4c5cabb6b346f3d53a4173b2ce78b591aee3059eac1ac789cc7a4c",
                DrehmalInstallFiles.officialDirectoryHash(temp));
    }

    @Test
    void zipEntriesCanNeverEscapeInstallationRoot() throws Exception {
        Path safe = DrehmalInstallFiles.resolveZipEntry(temp, "region/r.0.0.mca");
        assertTrue(safe.startsWith(temp.toAbsolutePath().normalize()));
        assertThrows(Exception.class, () -> DrehmalInstallFiles.resolveZipEntry(temp, "../outside.txt"));
    }

    @Test
    void pinnedFirstInstallDownloadSizeMatchesOfficialReleaseAssets() {
        assertEquals(3_987_331_283L, DrehmalBootstrapManifest.MAP_COMPRESSED_BYTES);
        assertEquals(4_177_586_790L, DrehmalBootstrapManifest.FIRST_INSTALL_DOWNLOAD_BYTES);
        assertEquals("2.2.2f", DrehmalBootstrapManifest.VERSION);
    }
}
