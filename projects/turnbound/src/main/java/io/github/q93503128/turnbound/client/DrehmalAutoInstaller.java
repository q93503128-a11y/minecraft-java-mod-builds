package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * First-run downloader used only when the imported TURNBOUND one-click pack leaves its opt-in marker in config.
 *
 * <p>A standalone TURNBOUND JAR never starts a multi-gigabyte download by itself. Once the official world is
 * verified and installed, later TURNBOUND JAR replacements reuse that save and perform no map download.</p>
 */
public final class DrehmalAutoInstaller {
    private static final String OPT_IN_RELATIVE = "config/turnbound/one_click_drehmal.txt";
    private static final String PROFILE_MARKER = ".turnbound_world_profile";
    private static final String INSTALL_MARKER = ".turnbound_bootstrap_version";
    private static final String TEMP_WORLD_NAME = ".turnbound-drehmal-2.2.2f-installing";
    private static final String USER_AGENT =
            "TURNBOUND/0.1.0-alpha.17 (official Drehmal bootstrap; q93503128-a11y/minecraft-java-mod-builds)";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public enum Phase {
        IDLE,
        CHECKING,
        DOWNLOADING_MAP,
        EXTRACTING_MAP,
        VERIFYING_MAP,
        MIGRATING_MAP,
        DOWNLOADING_RESOURCES,
        MIGRATING_RESOURCES,
        FINALIZING,
        COMPLETE,
        FAILED
    }

    public record Snapshot(Phase phase, String title, String detail, int percent, String error) {
        public Snapshot {
            phase = phase == null ? Phase.IDLE : phase;
            title = title == null ? "" : title;
            detail = detail == null ? "" : detail;
            percent = Math.max(0, Math.min(100, percent));
            error = error == null ? "" : error;
        }

        public boolean installing() {
            return phase != Phase.IDLE && phase != Phase.COMPLETE && phase != Phase.FAILED;
        }
    }

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile Snapshot snapshot = new Snapshot(Phase.IDLE, "", "", 0, "");

    private DrehmalAutoInstaller() {}

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) return;

        Path gameDir = minecraft.gameDirectory.toPath();
        if (!optedIn(gameDir)) return;

        Optional<Path> existing = findInstalledWorld(gameDir);
        if (existing.isPresent()
                && resourcePackReady(existing.get())
                && Drehmal26_2CompatMigrator.isCurrent(existing.get())) {
            if (snapshot.phase() != Phase.COMPLETE) {
                snapshot = new Snapshot(Phase.COMPLETE, "TURNBOUND 준비 완료", "설치된 Drehmal 월드를 그대로 사용합니다.", 100, "");
            }
            return;
        }

        if (snapshot.phase() == Phase.FAILED) return;
        if (!STARTED.compareAndSet(false, true)) return;
        if (!(minecraft.gui.screen() instanceof TitleScreen parent)) {
            STARTED.set(false);
            return;
        }

        minecraft.gui.setScreen(new DrehmalInstallScreen(parent));
        Thread.ofVirtual().name("turnbound-drehmal-bootstrap").start(() -> install(gameDir));
    }

    static boolean optedIn(Path gameDir) {
        Path marker = gameDir.resolve(OPT_IN_RELATIVE);
        if (!Files.isRegularFile(marker)) return false;
        try {
            return Files.readString(marker, StandardCharsets.UTF_8)
                    .contains(DrehmalBootstrapManifest.PROFILE_ID);
        } catch (IOException ignored) {
            return false;
        }
    }

    static Optional<Path> findInstalledWorld(Path gameDir) {
        Path saves = gameDir.resolve("saves");
        if (!Files.isDirectory(saves)) return Optional.empty();
        try (var stream = Files.list(saves)) {
            return stream.filter(Files::isDirectory)
                    .filter(DrehmalAutoInstaller::validInstalledWorld)
                    .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private static boolean validInstalledWorld(Path world) {
        Path profile = world.resolve(PROFILE_MARKER);
        if (!Files.isRegularFile(world.resolve("level.dat")) || !Files.isRegularFile(profile)) return false;
        try {
            return DrehmalBootstrapManifest.PROFILE_ID.equals(
                    Files.readString(profile, StandardCharsets.UTF_8).trim());
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean resourcePackReady(Path world) {
        Path pack = world.resolve("resources.zip");
        return Files.isRegularFile(pack)
                && DrehmalInstallFiles.validResourcePack(pack)
                && Drehmal26_2ResourcePackMigrator.isCurrent(world);
    }

    private static void install(Path gameDir) {
        try {
            update(Phase.CHECKING, "TURNBOUND 첫 실행 준비", "Drehmal 공식 배포본을 확인하고 있습니다.", 0);
            Files.createDirectories(gameDir.resolve("saves"));

            Optional<Path> installedWorld = findInstalledWorld(gameDir);
            if (installedWorld.isPresent()) {
                ensureResourcePackOnly(gameDir, installedWorld.get());
                ensure26_2Compatibility(installedWorld.get());
                complete();
                return;
            }

            Path finalWorld = gameDir.resolve("saves").resolve(DrehmalBootstrapManifest.WORLD_NAME);
            if (Files.exists(finalWorld)) {
                throw new IOException("같은 이름의 기존 월드가 있어 자동 설치가 중단되었습니다. 기존 월드는 수정하지 않았습니다.");
            }

            FileStore store = Files.getFileStore(gameDir);
            if (store.getUsableSpace() < DrehmalBootstrapManifest.MIN_FREE_BYTES) {
                throw new IOException("첫 설치에는 여유 공간 약 10GB가 필요합니다.");
            }

            Path cache = gameDir.resolve(".turnbound-installer").resolve("drehmal-" + DrehmalBootstrapManifest.VERSION);
            Path downloads = cache.resolve("downloads");
            Path tempWorld = gameDir.resolve("saves").resolve(TEMP_WORLD_NAME);
            Files.createDirectories(downloads);
            DrehmalInstallFiles.deleteTree(tempWorld);
            Files.createDirectories(tempWorld);

            downloadMapShards(downloads);
            extractMap(downloads, tempWorld);
            validateMap(tempWorld);
            ensure26_2Compatibility(tempWorld);

            Path resourcePack = downloadResourcePack(downloads);
            Files.copy(resourcePack, tempWorld.resolve("resources.zip"), StandardCopyOption.REPLACE_EXISTING);
            ensureResourcePackCompatibility(tempWorld);

            update(Phase.FINALIZING, "TURNBOUND 월드 마무리", "검증된 월드를 게임에 등록하고 있습니다.", 96);
            Files.writeString(
                    tempWorld.resolve(PROFILE_MARKER),
                    DrehmalBootstrapManifest.PROFILE_ID + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    tempWorld.resolve(INSTALL_MARKER),
                    "version=" + DrehmalBootstrapManifest.VERSION + System.lineSeparator()
                            + "worldHash=" + DrehmalBootstrapManifest.WORLD_HASH + System.lineSeparator(),
                    StandardCharsets.UTF_8);

            moveDirectory(tempWorld, finalWorld);
            try {
                DrehmalInstallFiles.deleteTree(cache);
            } catch (IOException cleanupError) {
                Turnbound.LOGGER.warn("TURNBOUND could not clean first-run download cache", cleanupError);
            }
            complete();
        } catch (Throwable error) {
            Turnbound.LOGGER.error("TURNBOUND one-click Drehmal installation failed", error);
            String message = error.getMessage();
            if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
            snapshot = new Snapshot(
                    Phase.FAILED,
                    "월드 준비에 실패했습니다",
                    "인터넷 연결과 저장 공간을 확인한 뒤 게임을 다시 실행해 주세요.",
                    snapshot.percent(),
                    message);
        }
    }

    private static void ensureResourcePackOnly(Path gameDir, Path world) throws Exception {
        Path installed = world.resolve("resources.zip");
        if (Files.isRegularFile(installed) && DrehmalInstallFiles.validResourcePack(installed)) {
            ensureResourcePackCompatibility(world);
            return;
        }

        Path cache = gameDir.resolve(".turnbound-installer").resolve("drehmal-" + DrehmalBootstrapManifest.VERSION);
        Path downloads = cache.resolve("downloads");
        Files.createDirectories(downloads);
        Path pack = downloadResourcePack(downloads);
        Files.copy(pack, installed, StandardCopyOption.REPLACE_EXISTING);
        ensureResourcePackCompatibility(world);
        try {
            DrehmalInstallFiles.deleteTree(cache);
        } catch (IOException cleanupError) {
            Turnbound.LOGGER.warn("TURNBOUND could not clean resource-pack download cache", cleanupError);
        }
    }

    private static void downloadMapShards(Path downloads) throws Exception {
        long completed = 0L;
        long total = DrehmalBootstrapManifest.MAP_COMPRESSED_BYTES;
        List<DrehmalBootstrapManifest.RemoteFile> shards = DrehmalBootstrapManifest.SHARDS;
        for (int i = 0; i < shards.size(); i++) {
            var shard = shards.get(i);
            Path destination = downloads.resolve(shard.name());
            final long before = completed;
            final int index = i + 1;
            download(shard, destination, (current, ignored) -> {
                long all = before + current;
                int pct = percent(all, total);
                update(
                        Phase.DOWNLOADING_MAP,
                        "Drehmal 월드 다운로드",
                        index + "/" + shards.size() + " · " + humanBytes(current) + " / " + humanBytes(shard.size()),
                        pct);
            });
            completed += shard.size();
        }
    }

    private static void extractMap(Path downloads, Path tempWorld) throws Exception {
        long completed = 0L;
        for (int i = 0; i < DrehmalBootstrapManifest.SHARDS.size(); i++) {
            var shard = DrehmalBootstrapManifest.SHARDS.get(i);
            Path archive = downloads.resolve(shard.name());
            final long before = completed;
            final int index = i + 1;
            long written = DrehmalInstallFiles.extractZip(
                    archive,
                    tempWorld,
                    (current, total) -> update(
                            Phase.EXTRACTING_MAP,
                            "Drehmal 월드 구성",
                            index + "/" + DrehmalBootstrapManifest.SHARDS.size() + " · 압축 해제 중",
                            percent(current, total)),
                    before,
                    DrehmalBootstrapManifest.WORLD_UNCOMPRESSED_BYTES);
            completed += written;
        }
    }

    private static void validateMap(Path tempWorld) throws Exception {
        update(Phase.VERIFYING_MAP, "Drehmal 월드 검증", "공식 배포본과 일치하는지 확인하고 있습니다.", 0);
        String actual = DrehmalInstallFiles.officialDirectoryHash(tempWorld, (done, total) -> update(
                Phase.VERIFYING_MAP,
                "Drehmal 월드 검증",
                done + " / " + total + " 파일",
                total <= 0 ? 0 : percent(done, total)));
        if (!DrehmalBootstrapManifest.WORLD_HASH.equals(actual)) {
            throw new IOException("Drehmal 월드 검증값이 공식 2.2.2f와 일치하지 않습니다.");
        }
    }

    private static void ensure26_2Compatibility(Path world) throws Exception {
        update(
                Phase.MIGRATING_MAP,
                "Drehmal 월드 호환 준비",
                "기존 지형과 저장 데이터를 유지하며 Minecraft 26.2 형식으로 맞추고 있습니다.",
                92);
        Drehmal26_2CompatMigrator.Report report = Drehmal26_2CompatMigrator.migrate(world);
        Turnbound.LOGGER.info(
                "TURNBOUND Drehmal 26.2 compatibility: changed={}, biomes={}, dimensions={}, sourceHash={}, migratedHash={}",
                report.changed(),
                report.biomes(),
                report.dimensionTypes(),
                report.sourceHash(),
                report.migratedHash());
    }

    private static void ensureResourcePackCompatibility(Path world) throws Exception {
        update(
                Phase.MIGRATING_RESOURCES,
                "Drehmal 리소스 호환 준비",
                "원본 디자인을 유지하며 Minecraft 26.2 리소스 형식으로 맞추고 있습니다.",
                94);
        Drehmal26_2ResourcePackMigrator.Report report = Drehmal26_2ResourcePackMigrator.migrate(world);
        Turnbound.LOGGER.info(
                "TURNBOUND Drehmal resource compatibility: changed={}, references={}, modelRenamed={}, sourceHash={}, migratedHash={}",
                report.changed(),
                report.referencesRewritten(),
                report.modelRenamed(),
                report.sourceHash(),
                report.migratedHash());
    }

    private static Path downloadResourcePack(Path downloads) throws Exception {
        var remote = DrehmalBootstrapManifest.RESOURCE_PACK;
        Path destination = downloads.resolve(remote.name());
        download(remote, destination, (current, total) -> update(
                Phase.DOWNLOADING_RESOURCES,
                "Drehmal 리소스 준비",
                humanBytes(current) + " / " + humanBytes(total),
                percent(current, total)));
        if (!DrehmalInstallFiles.validResourcePack(destination)) {
            throw new IOException("Drehmal 리소스팩 파일이 올바르지 않습니다.");
        }
        return destination;
    }

    private static void download(
            DrehmalBootstrapManifest.RemoteFile remote,
            Path destination,
            java.util.function.BiConsumer<Long, Long> progress
    ) throws Exception {
        if (Files.isRegularFile(destination) && Files.size(destination) == remote.size()) {
            if (!remote.sha256().isBlank() && !remote.sha256().equals(DrehmalInstallFiles.sha256(destination))) {
                Files.delete(destination);
            } else {
                progress.accept(remote.size(), remote.size());
                return;
            }
        }

        Files.createDirectories(destination.getParent());
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        long existing = Files.isRegularFile(partial) ? Files.size(partial) : 0L;
        if (existing > remote.size()) {
            Files.delete(partial);
            existing = 0L;
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(remote.uri())
                .GET()
                .timeout(Duration.ofHours(12))
                .header("User-Agent", USER_AGENT);
        if (existing > 0) request.header("Range", "bytes=" + existing + "-");

        HttpResponse<InputStream> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        int code = response.statusCode();
        boolean append = existing > 0 && code == HttpURLConnection.HTTP_PARTIAL;
        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
            throw new IOException("다운로드 응답 오류: HTTP " + code);
        }
        if (!append) existing = 0L;

        var options = append
                ? new java.nio.file.OpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND}
                : new java.nio.file.OpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING};

        byte[] buffer = new byte[1024 * 256];
        long written = existing;
        progress.accept(written, remote.size());
        try (InputStream in = new BufferedInputStream(response.body());
             var out = Files.newOutputStream(partial, options)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read <= 0) continue;
                out.write(buffer, 0, read);
                written += read;
                progress.accept(written, remote.size());
            }
        }

        if (written != remote.size()) {
            throw new IOException("다운로드 크기가 예상과 다릅니다: " + remote.name());
        }
        if (!remote.sha256().isBlank()) {
            String actual = DrehmalInstallFiles.sha256(partial);
            if (!remote.sha256().equals(actual)) {
                Files.deleteIfExists(partial);
                throw new IOException("다운로드 검증에 실패했습니다: " + remote.name());
            }
        }

        Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void moveDirectory(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailed) {
            Files.move(source, destination);
        }
    }

    private static void complete() {
        snapshot = new Snapshot(
                Phase.COMPLETE,
                "TURNBOUND 준비 완료",
                "Drehmal 2.2.2f는 이 인스턴스에 저장되었습니다. 다음부터는 다시 다운로드하지 않습니다.",
                100,
                "");
    }

    private static void update(Phase phase, String title, String detail, int percent) {
        snapshot = new Snapshot(phase, title, detail, percent, "");
    }

    private static int percent(long value, long total) {
        if (total <= 0) return 0;
        return (int)Math.max(0, Math.min(100, Math.round(value * 100.0 / total)));
    }

    private static String humanBytes(long bytes) {
        double mib = bytes / (1024.0 * 1024.0);
        if (mib < 1024.0) return String.format(Locale.ROOT, "%.0f MB", mib);
        return String.format(Locale.ROOT, "%.2f GB", mib / 1024.0);
    }
}
