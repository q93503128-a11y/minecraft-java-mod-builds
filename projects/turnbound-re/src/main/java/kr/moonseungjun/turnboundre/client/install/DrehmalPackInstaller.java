package kr.moonseungjun.turnboundre.client.install;

import kr.moonseungjun.turnboundre.TurnboundRe;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/** Background first-run installer used only by the official TURNBOUND Modrinth pack marker. */
public final class DrehmalPackInstaller {
    public enum Phase {
        IDLE,
        CHECKING,
        DOWNLOADING_MAP,
        ASSEMBLING_WORLD,
        VERIFYING_WORLD,
        DOWNLOADING_RESOURCES,
        FINALIZING,
        READY,
        FAILED
    }

    public record Snapshot(Phase phase, String detail, int current, int total, double progress, String failureMessage) {
        public Snapshot {
            if (phase == null) phase = Phase.IDLE;
            if (detail == null) detail = "";
            if (failureMessage == null) failureMessage = "";
            current = Math.max(0, current);
            total = Math.max(0, total);
            progress = Math.max(0.0D, Math.min(1.0D, progress));
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static volatile Snapshot snapshot = new Snapshot(Phase.IDLE, "", 0, 0, 0.0D, "");

    private DrehmalPackInstaller() {}

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static boolean running() {
        return RUNNING.get();
    }

    public static boolean distributionEnabled(Path gameDirectory) {
        return DrehmalInstallFiles.isModrinthDistribution(gameDirectory);
    }

    public static boolean ready(Path gameDirectory) {
        return DrehmalInstallFiles.worldReady(gameDirectory);
    }

    public static void start(Path gameDirectory) {
        if (gameDirectory == null || !distributionEnabled(gameDirectory)) return;
        if (ready(gameDirectory)) {
            snapshot = new Snapshot(Phase.READY, "", 1, 1, 1.0D, "");
            return;
        }
        if (!RUNNING.compareAndSet(false, true)) return;

        snapshot = new Snapshot(Phase.CHECKING, "", 0, 1, 0.0D, "");
        Thread worker = new Thread(() -> runInstall(gameDirectory), "turnbound-re-drehmal-installer");
        worker.setDaemon(true);
        worker.start();
    }

    public static void retry(Path gameDirectory) {
        if (RUNNING.get()) return;
        snapshot = new Snapshot(Phase.IDLE, "", 0, 0, 0.0D, "");
        start(gameDirectory);
    }

    private static void runInstall(Path gameDirectory) {
        try {
            install(gameDirectory);
            snapshot = new Snapshot(Phase.READY, "", 1, 1, 1.0D, "");
        } catch (Throwable failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            TurnboundRe.LOGGER.error("TURNBOUND Modrinth first-run world installation failed", failure);
            snapshot = new Snapshot(Phase.FAILED, "", 0, 0, 0.0D, friendlyFailure(failure));
        } finally {
            RUNNING.set(false);
        }
    }

    private static void install(Path gameDirectory) throws IOException, InterruptedException {
        if (!distributionEnabled(gameDirectory)) {
            throw new IOException("distribution marker unavailable");
        }

        Path world = DrehmalInstallFiles.worldDirectory(gameDirectory);
        Path cache = DrehmalInstallFiles.cacheDirectory(gameDirectory);
        Path installingFlag = cache.resolve("installing.flag");
        Path mapValidatedFlag = cache.resolve("map.validated");
        Files.createDirectories(cache);

        snapshot = new Snapshot(Phase.CHECKING, "월드 상태 확인", 0, 1, 0.1D, "");

        if (Files.isRegularFile(world.resolve("level.dat")) && DrehmalInstallFiles.profileMarkerMatches(world)) {
            ensureWorldResourcePack(cache, world);
            finishInstall(world, cache);
            return;
        }

        boolean resumeValidatedMap = Files.isRegularFile(installingFlag)
                && Files.isRegularFile(mapValidatedFlag)
                && Files.isRegularFile(world.resolve("level.dat"))
                && DrehmalInstallFiles.EXPECTED_MAP_HASH.equals(
                        Files.readString(mapValidatedFlag, StandardCharsets.UTF_8).trim());

        if (Files.exists(world) && !resumeValidatedMap) {
            if (Files.isRegularFile(installingFlag)) {
                // Only an interrupted TURNBOUND-owned install is rebuilt automatically.
                DrehmalInstallFiles.deleteRecursively(world);
            } else {
                snapshot = new Snapshot(Phase.VERIFYING_WORLD, "기존 월드 확인", 0, 1, 0.0D, "");
                String existingHash = DrehmalInstallFiles.directoryHash(world);
                if (!DrehmalInstallFiles.EXPECTED_MAP_HASH.equals(existingHash)) {
                    throw new IOException("existing save with TURNBOUND world name is not the pinned Drehmal map");
                }
                Files.writeString(installingFlag, "validated existing map\n", StandardCharsets.UTF_8);
                Files.writeString(mapValidatedFlag, DrehmalInstallFiles.EXPECTED_MAP_HASH + "\n", StandardCharsets.UTF_8);
                resumeValidatedMap = true;
            }
        }

        if (!resumeValidatedMap) {
            ensureFreeSpace(gameDirectory);
            Files.writeString(installingFlag, "installing " + DrehmalInstallFiles.WORLD_VERSION + "\n", StandardCharsets.UTF_8);

            Path[] shardFiles = new Path[DrehmalInstallFiles.MAP_SHARDS.size()];
            for (int i = 0; i < DrehmalInstallFiles.MAP_SHARDS.size(); i++) {
                DrehmalInstallFiles.DownloadSpec spec = DrehmalInstallFiles.MAP_SHARDS.get(i);
                shardFiles[i] = ensureDownload(
                        cache,
                        spec,
                        Phase.DOWNLOADING_MAP,
                        "지도 파일 " + (i + 1) + "/" + DrehmalInstallFiles.MAP_SHARDS.size(),
                        i + 1,
                        DrehmalInstallFiles.MAP_SHARDS.size());
            }

            Files.createDirectories(world);
            for (int i = 0; i < shardFiles.length; i++) {
                snapshot = new Snapshot(
                        Phase.ASSEMBLING_WORLD,
                        "월드 구성 " + (i + 1) + "/" + shardFiles.length,
                        i + 1,
                        shardFiles.length,
                        i / (double) shardFiles.length,
                        "");
                DrehmalInstallFiles.expandZipMerged(shardFiles[i], world);
                snapshot = new Snapshot(
                        Phase.ASSEMBLING_WORLD,
                        "월드 구성 " + (i + 1) + "/" + shardFiles.length,
                        i + 1,
                        shardFiles.length,
                        (i + 1) / (double) shardFiles.length,
                        "");
            }

            if (!Files.isRegularFile(world.resolve("level.dat"))) {
                throw new IOException("assembled Drehmal world is missing level.dat");
            }

            snapshot = new Snapshot(Phase.VERIFYING_WORLD, "월드 파일 검증", 0, 1, 0.15D, "");
            String mapHash = DrehmalInstallFiles.directoryHash(world);
            if (!DrehmalInstallFiles.EXPECTED_MAP_HASH.equals(mapHash)) {
                throw new IOException("assembled Drehmal map verification failed");
            }
            Files.writeString(mapValidatedFlag, DrehmalInstallFiles.EXPECTED_MAP_HASH + "\n", StandardCharsets.UTF_8);
            snapshot = new Snapshot(Phase.VERIFYING_WORLD, "월드 파일 검증", 1, 1, 1.0D, "");
        }

        ensureWorldResourcePack(cache, world);
        finishInstall(world, cache);
    }

    private static void ensureFreeSpace(Path gameDirectory) throws IOException {
        long usable = Files.getFileStore(gameDirectory).getUsableSpace();
        if (usable < DrehmalInstallFiles.MIN_FREE_BYTES) {
            throw new IOException("insufficient free space for TURNBOUND world installation");
        }
    }

    private static void ensureWorldResourcePack(Path cache, Path world) throws IOException, InterruptedException {
        if (DrehmalInstallFiles.resourcePackReady(world)) return;
        Path cached = ensureDownload(
                cache,
                DrehmalInstallFiles.RESOURCE_PACK,
                Phase.DOWNLOADING_RESOURCES,
                "월드 리소스",
                1,
                1);

        snapshot = new Snapshot(Phase.DOWNLOADING_RESOURCES, "월드 리소스 적용", 1, 1, 0.95D, "");
        Path destination = DrehmalInstallFiles.worldResourcePack(world);
        Files.createDirectories(destination.getParent());
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        Files.deleteIfExists(partial);
        Files.copy(cached, partial, StandardCopyOption.REPLACE_EXISTING);
        if (Files.size(partial) != DrehmalInstallFiles.RESOURCE_PACK.expectedSize()
                || !DrehmalInstallFiles.isReadableZip(partial)) {
            Files.deleteIfExists(partial);
            throw new IOException("world resource pack verification failed");
        }
        moveAtomicOrReplace(partial, destination);
    }

    private static void finishInstall(Path world, Path cache) throws IOException {
        snapshot = new Snapshot(Phase.FINALIZING, "TURNBOUND 월드 마무리", 0, 1, 0.4D, "");
        if (!Files.isRegularFile(world.resolve("level.dat")) || !DrehmalInstallFiles.resourcePackReady(world)) {
            throw new IOException("TURNBOUND world did not reach the ready state");
        }

        // This is the trust boundary used by the server-side external-world binder.
        DrehmalInstallFiles.writeProfileMarker(world);
        snapshot = new Snapshot(Phase.FINALIZING, "TURNBOUND 월드 마무리", 1, 1, 1.0D, "");

        try {
            DrehmalInstallFiles.deleteRecursively(cache);
        } catch (IOException cleanupFailure) {
            // The verified world is already complete; stale download cache must not turn success into a false failure.
            TurnboundRe.LOGGER.warn("TURNBOUND world is ready but installer cache cleanup failed", cleanupFailure);
        }
    }

    private static Path ensureDownload(
            Path cache,
            DrehmalInstallFiles.DownloadSpec spec,
            Phase phase,
            String detail,
            int current,
            int total
    ) throws IOException, InterruptedException {
        Files.createDirectories(cache);
        Path target = cache.resolve(spec.fileName());
        if (DrehmalInstallFiles.verifiedDownload(target, spec)) {
            snapshot = new Snapshot(phase, detail, current, total, 1.0D, "");
            return target;
        }

        Files.deleteIfExists(target);
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        Files.deleteIfExists(partial);

        HttpRequest request = HttpRequest.newBuilder(spec.uri())
                .timeout(Duration.ofHours(2))
                .header("User-Agent", "TURNBOUND-RE/0.1.0-alpha.1")
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (InputStream ignored = response.body()) {
                // Close the response body before surfacing the download failure.
            }
            throw new IOException("official download returned HTTP " + response.statusCode());
        }

        long copied = 0L;
        try (InputStream input = response.body(); var output = Files.newOutputStream(partial)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) continue;
                output.write(buffer, 0, read);
                copied += read;
                snapshot = new Snapshot(
                        phase,
                        detail,
                        current,
                        total,
                        Math.min(1.0D, copied / (double) spec.expectedSize()),
                        "");
            }
        } catch (IOException failure) {
            Files.deleteIfExists(partial);
            throw failure;
        }

        moveAtomicOrReplace(partial, target);
        if (!DrehmalInstallFiles.verifiedDownload(target, spec)) {
            Files.deleteIfExists(target);
            throw new IOException("download verification failed for " + spec.fileName());
        }
        snapshot = new Snapshot(phase, detail, current, total, 1.0D, "");
        return target;
    }

    private static void moveAtomicOrReplace(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String friendlyFailure(Throwable failure) {
        String message = failure == null || failure.getMessage() == null ? "" : failure.getMessage();
        String lower = message.toLowerCase();
        if (lower.contains("insufficient free space")) {
            return "저장 공간이 부족합니다. 최소 12 GiB의 여유 공간을 확보한 뒤 다시 시도하세요.";
        }
        if (lower.contains("existing save")) {
            return "같은 이름의 다른 월드가 있어 자동 설치를 중단했습니다. 기존 월드는 변경하지 않았습니다.";
        }
        if (lower.contains("http") || lower.contains("download")) {
            return "공식 월드 파일을 내려받지 못했습니다. 인터넷 연결을 확인한 뒤 다시 시도하세요.";
        }
        if (lower.contains("verification") || lower.contains("hash") || lower.contains("readable zip")) {
            return "받은 월드 파일의 검증에 실패했습니다. 다시 시도하면 손상된 파일만 다시 받습니다.";
        }
        return "월드를 준비하는 중 문제가 발생했습니다. 다시 시도해 주세요.";
    }
}
