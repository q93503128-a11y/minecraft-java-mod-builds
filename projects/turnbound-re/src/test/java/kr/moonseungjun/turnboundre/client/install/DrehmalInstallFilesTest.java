package kr.moonseungjun.turnboundre.client.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DrehmalInstallFilesTest {
    @TempDir
    Path temp;

    @Test
    void distributionMarkerRequiresModrinthAndExplicitAutoInstall() throws Exception {
        Path config = temp.resolve("config");
        Files.createDirectories(config);
        Path marker = config.resolve(DrehmalInstallFiles.DISTRIBUTION_MARKER);

        Files.writeString(marker, "distribution=modrinth\nautoInstallDrehmal=true\n", StandardCharsets.UTF_8);
        assertTrue(DrehmalInstallFiles.isModrinthDistribution(temp));

        Files.writeString(marker, "distribution=modrinth\nautoInstallDrehmal=false\n", StandardCharsets.UTF_8);
        assertFalse(DrehmalInstallFiles.isModrinthDistribution(temp));

        Files.writeString(marker, "distribution=prism\nautoInstallDrehmal=true\n", StandardCharsets.UTF_8);
        assertFalse(DrehmalInstallFiles.isModrinthDistribution(temp));
    }

    @Test
    void profileMarkerAcceptsOnlyExactPinnedProfileAfterTrim() throws Exception {
        Path world = temp.resolve("world");
        Files.createDirectories(world);
        Files.writeString(
                DrehmalInstallFiles.profileMarker(world),
                "  " + DrehmalInstallFiles.PROFILE_ID + "\n",
                StandardCharsets.UTF_8);
        assertTrue(DrehmalInstallFiles.profileMarkerMatches(world));

        Files.writeString(
                DrehmalInstallFiles.profileMarker(world),
                DrehmalInstallFiles.PROFILE_ID + "-other\n",
                StandardCharsets.UTF_8);
        assertFalse(DrehmalInstallFiles.profileMarkerMatches(world));
    }


    @Test
    void worldReadyRequiresProfileAndAllThreeCompatibilityMigrations() throws Exception {
        Path world = DrehmalInstallFiles.worldDirectory(temp);
        Files.createDirectories(world.resolve("datapacks"));
        Files.writeString(world.resolve("level.dat"), "fixture", StandardCharsets.UTF_8);
        DrehmalInstallFiles.writeProfileMarker(world);

        Path datapack = Drehmal26_2DatapackMigrator.datapack(world);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(datapack))) {
            output.putNextEntry(new ZipEntry("fixture.txt"));
            output.write("fixture".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        Path resourcePack = DrehmalInstallFiles.worldResourcePack(world);
        Files.createDirectories(resourcePack.getParent());
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(resourcePack))) {
            output.putNextEntry(new ZipEntry("assets/example/keep.json"));
            output.write("{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        assertFalse(DrehmalInstallFiles.worldReady(temp));

        Files.writeString(
                Drehmal26_2DatapackMigrator.compatibilityMarker(world),
                Drehmal26_2DatapackMigrator.MIGRATION_ID + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                Drehmal26_2SavedDataMigrator.compatibilityMarker(world),
                Drehmal26_2SavedDataMigrator.MIGRATION_ID + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                Drehmal26_2ResourcePackMigrator.compatibilityMarker(world),
                Drehmal26_2ResourcePackMigrator.MIGRATION_ID + "\n",
                StandardCharsets.UTF_8);

        assertTrue(DrehmalInstallFiles.migrationReady(world));
        assertTrue(DrehmalInstallFiles.resourcePackReady(world));
        assertTrue(DrehmalInstallFiles.worldReady(temp));

        Files.delete(Drehmal26_2SavedDataMigrator.compatibilityMarker(world));
        assertFalse(DrehmalInstallFiles.migrationReady(world));
        assertFalse(DrehmalInstallFiles.worldReady(temp));
    }

    @Test
    void directoryHashMatchesOfficialRecursiveHexDigestAlgorithm() throws Exception {
        Path root = temp.resolve("hash-root");
        Path child = root.resolve("b-dir");
        Files.createDirectories(child);
        Files.writeString(root.resolve("a.txt"), "alpha", StandardCharsets.UTF_8);
        Files.writeString(child.resolve("inside.txt"), "inside", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("z.txt"), "omega", StandardCharsets.UTF_8);

        MessageDigest childDigest = MessageDigest.getInstance("SHA-256");
        childDigest.update("inside".getBytes(StandardCharsets.UTF_8));
        String childHex = HexFormat.of().formatHex(childDigest.digest());

        MessageDigest rootDigest = MessageDigest.getInstance("SHA-256");
        rootDigest.update("alpha".getBytes(StandardCharsets.UTF_8));
        rootDigest.update(childHex.getBytes(StandardCharsets.UTF_8));
        rootDigest.update("omega".getBytes(StandardCharsets.UTF_8));
        String expected = HexFormat.of().formatHex(rootDigest.digest());

        assertEquals(expected, DrehmalInstallFiles.directoryHash(root));
    }

    @Test
    void mergedZipExtractionRejectsTraversal() throws Exception {
        Path archive = temp.resolve("unsafe.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("../escape.txt"));
            output.write("nope".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        Path destination = temp.resolve("world");
        assertThrows(IOException.class, () -> DrehmalInstallFiles.expandZipMerged(archive, destination));
        assertFalse(Files.exists(temp.resolve("escape.txt")));
    }
}
