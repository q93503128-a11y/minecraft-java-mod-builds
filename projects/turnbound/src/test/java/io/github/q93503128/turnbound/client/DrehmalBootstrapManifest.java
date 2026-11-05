package io.github.q93503128.turnbound.client;

import java.net.URI;
import java.util.List;

/**
 * Pinned official Drehmal 2.2.2f sources used only by TURNBOUND's opt-in one-click test pack.
 *
 * <p>The map/resource-pack bytes are never vendored in TURNBOUND. The client downloads the official upstream release
 * directly on first install, verifies the assembled map against the official installer hash, then keeps the resulting
 * save in the instance for later JAR-only TURNBOUND updates.</p>
 */
final class DrehmalBootstrapManifest {
    static final String VERSION = "2.2.2f";
    static final String PROFILE_ID = "turnbound:drehmal_apotheosis_2_2_2f";
    static final String WORLD_NAME = "TURNBOUND - Drehmal APOTHEOSIS 2.2.2f";
    static final String WORLD_HASH = "2e6232dc3e97c77eaa006b09e3ee09b246c46e3df68359dcc1495ce19d1e8053";
    static final long WORLD_UNCOMPRESSED_BYTES = 5_139_218_299L;
    static final long MIN_FREE_BYTES = 10L * 1024L * 1024L * 1024L;

    record RemoteFile(String name, URI uri, long size, String sha256) {
        RemoteFile {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("blank file name");
            if (uri == null) throw new IllegalArgumentException("missing uri");
            if (size <= 0) throw new IllegalArgumentException("invalid file size");
            sha256 = sha256 == null ? "" : sha256.trim().toLowerCase();
        }
    }

    static final List<RemoteFile> SHARDS = List.of(
            new RemoteFile(
                    "shard_1.zip",
                    URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_1.zip"),
                    1_484_178_230L,
                    "378f8bea9c88371c44d3a6fd6c6d91a886f14b6dfce97e6e6e994789b979b358"),
            new RemoteFile(
                    "shard_2.zip",
                    URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_2.zip"),
                    1_641_289_974L,
                    "2d250f04259404d81a3e9bd83a5592f1c26545f45f9c8a22a2277c9e523c1331"),
            new RemoteFile(
                    "shard_3.zip",
                    URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/shard_3.zip"),
                    861_863_079L,
                    "8c892c77c7ab9ef03aac7b517e5448490f55690163085b38c380523dd2d29b67"));

    static final RemoteFile RESOURCE_PACK = new RemoteFile(
            "resources.zip",
            URI.create("https://github.com/Drehmal-Team/map/releases/download/v2.2.2f/resources.zip"),
            190_255_507L,
            "");

    static final long MAP_COMPRESSED_BYTES = SHARDS.stream().mapToLong(RemoteFile::size).sum();
    static final long FIRST_INSTALL_DOWNLOAD_BYTES = MAP_COMPRESSED_BYTES + RESOURCE_PACK.size();

    private DrehmalBootstrapManifest() {}
}
