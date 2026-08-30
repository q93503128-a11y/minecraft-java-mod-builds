package io.github.q93503128.turnbound.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Crash-resistant file layer: temp write, last-good backup, backup recovery and corruption quarantine. */
public final class CampaignSaveFiles {
    public record LoadResult(CampaignProgressStore.Snapshot snapshot, boolean recoveredBackup) {}

    private CampaignSaveFiles() {}

    public static Optional<LoadResult> load(Path primary) throws IOException {
        Path backup = backup(primary);
        Exception primaryFailure = null;

        if (Files.exists(primary)) {
            try {
                return Optional.of(new LoadResult(read(primary), false));
            } catch (Exception ex) {
                primaryFailure = ex;
            }
        }

        if (Files.exists(backup)) {
            try {
                return Optional.of(new LoadResult(read(backup), true));
            } catch (Exception backupFailure) {
                IOException combined = new IOException("TURNBOUND campaign save and backup are unreadable", backupFailure);
                if (primaryFailure != null) combined.addSuppressed(primaryFailure);
                throw combined;
            }
        }

        if (primaryFailure != null) throw new IOException("TURNBOUND campaign save is unreadable and no backup exists", primaryFailure);
        return Optional.empty();
    }

    public static void save(Path primary, CampaignProgressStore.Snapshot snapshot) throws IOException {
        Path parent = primary.getParent();
        if (parent == null) throw new IOException("TURNBOUND campaign save has no parent directory");
        Files.createDirectories(parent);

        Path temp = primary.resolveSibling(primary.getFileName() + ".tmp");
        Files.writeString(temp, CampaignSaveCodec.encode(snapshot), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        if (Files.exists(primary)) Files.copy(primary, backup(primary), StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(temp, primary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, primary, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static List<Path> quarantine(Path primary) throws IOException {
        List<Path> moved = new ArrayList<>();
        String suffix = ".corrupt-" + Instant.now().toEpochMilli();
        quarantineOne(primary, primary.resolveSibling(primary.getFileName() + suffix), moved);
        Path backup = backup(primary);
        quarantineOne(backup, backup.resolveSibling(backup.getFileName() + suffix), moved);
        return List.copyOf(moved);
    }

    /** Preserve a healthy backup by isolating only the unreadable primary before recovery is persisted. */
    public static Path quarantinePrimary(Path primary) throws IOException {
        if (!Files.exists(primary)) return primary;
        Path target = primary.resolveSibling(primary.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
        Files.move(primary, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    static Path backup(Path primary) {
        return primary.resolveSibling(primary.getFileName() + ".bak");
    }

    private static CampaignProgressStore.Snapshot read(Path path) throws IOException {
        return CampaignSaveCodec.decode(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static void quarantineOne(Path source, Path target, List<Path> moved) throws IOException {
        if (!Files.exists(source)) return;
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        moved.add(target);
    }
}
