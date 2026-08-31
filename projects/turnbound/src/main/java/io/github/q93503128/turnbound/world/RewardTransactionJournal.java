package io.github.q93503128.turnbound.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Durable write-ahead record for a battle reward settlement that has not yet reached the canonical campaign save. */
public final class RewardTransactionJournal {
    public enum Recovery { NONE, APPLIED, STALE }

    public record Pending(String transactionId, CampaignProgressStore.Snapshot snapshot) {
        public Pending {
            if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("Missing reward transaction id");
            if (snapshot == null) throw new IllegalArgumentException("Missing reward transaction snapshot");
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RewardTransactionJournal() {}

    public static void prepare(Path primary, String transactionId, CampaignProgressStore.Snapshot snapshot) throws IOException {
        if (primary == null) throw new IllegalArgumentException("Missing campaign save path");
        Pending pending = new Pending(transactionId, snapshot);
        Path journal = journalPath(primary);
        Path parent = journal.getParent();
        if (parent == null) throw new IOException("TURNBOUND reward journal has no parent directory");
        Files.createDirectories(parent);

        JsonObject root = new JsonObject();
        root.addProperty("transactionId", pending.transactionId());
        root.add("snapshot", JsonParser.parseString(CampaignSaveCodec.encode(pending.snapshot())));
        Path temp = journal.resolveSibling(journal.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            try {
                Files.move(temp, journal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, journal, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static Optional<Pending> load(Path primary) throws IOException {
        Path journal = journalPath(primary);
        if (!Files.exists(journal)) return Optional.empty();
        String raw = Files.readString(journal, StandardCharsets.UTF_8);
        try {
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            if (!root.has("transactionId") || !root.has("snapshot") || !root.get("snapshot").isJsonObject()) {
                throw new IllegalStateException("Incomplete TURNBOUND reward journal");
            }
            return Optional.of(new Pending(root.get("transactionId").getAsString(),
                    CampaignSaveCodec.decode(root.getAsJsonObject("snapshot").toString())));
        } catch (RuntimeException ex) {
            Path quarantined = quarantine(primary);
            throw new IOException("TURNBOUND reward journal is unreadable; preserved at " + quarantined, ex);
        }
    }

    public static Recovery recover(Path primary, UUID playerId) throws IOException {
        Optional<Pending> loaded = load(primary);
        if (loaded.isEmpty()) return Recovery.NONE;
        Pending pending = loaded.get();
        CampaignProgressStore.Snapshot current = CampaignProgressStore.snapshot(playerId);
        if (RewardGrantService.transactionCommitted(current, pending.transactionId())) {
            clearBestEffort(primary);
            return Recovery.STALE;
        }

        CampaignProgressStore.restore(playerId, pending.snapshot());
        CampaignProgressStore.markDirty(playerId);
        try {
            CampaignSaveFiles.save(primary, pending.snapshot());
            CampaignProgressStore.markClean(playerId);
        } catch (IOException ex) {
            CampaignProgressStore.restore(playerId, current);
            CampaignProgressStore.markDirty(playerId);
            throw ex;
        }
        clearBestEffort(primary);
        return Recovery.APPLIED;
    }

    public static void clear(Path primary) throws IOException {
        Files.deleteIfExists(journalPath(primary));
    }

    static Path journalPath(Path primary) {
        return primary.resolveSibling(primary.getFileName() + ".reward-pending.json");
    }

    private static Path quarantine(Path primary) throws IOException {
        Path journal = journalPath(primary);
        Path target = journal.resolveSibling(journal.getFileName() + ".corrupt-" + Instant.now().toEpochMilli());
        Files.move(journal, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static void clearBestEffort(Path primary) {
        try { clear(primary); } catch (IOException ignored) { }
    }
}
