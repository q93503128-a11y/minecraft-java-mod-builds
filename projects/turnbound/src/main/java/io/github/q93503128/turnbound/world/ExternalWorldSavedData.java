package io.github.q93503128.turnbound.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** World-owned bookkeeping for the external authored-world layer. */
public final class ExternalWorldSavedData extends SavedData {
    private static final Codec<ExternalWorldSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("initializedPlayers", List.of())
                    .forGetter(data -> data.initializedPlayers.stream().map(UUID::toString).toList()),
            Codec.STRING.listOf().optionalFieldOf("onboardingFlags", List.of())
                    .forGetter(data -> List.copyOf(data.onboardingEntries))
    ).apply(instance, ExternalWorldSavedData::fromStrings));

    public static final SavedDataType<ExternalWorldSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "external_world/binding"),
            ExternalWorldSavedData::new,
            CODEC,
            null);

    private final Set<UUID> initializedPlayers = new LinkedHashSet<>();
    private final Set<String> onboardingEntries = new LinkedHashSet<>();

    public ExternalWorldSavedData() {}

    private static ExternalWorldSavedData fromStrings(List<String> values, List<String> onboarding) {
        ExternalWorldSavedData data = new ExternalWorldSavedData();
        for (String value : values) {
            try {
                data.initializedPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // A corrupt foreign UUID entry must not make the whole world unloadable.
            }
        }
        for (String value : onboarding) {
            int split = value == null ? -1 : value.indexOf('|');
            if (split <= 0 || split >= value.length() - 1) continue;
            try {
                UUID playerId = UUID.fromString(value.substring(0, split));
                String flag = cleanFlag(value.substring(split + 1));
                if (!flag.isBlank()) data.onboardingEntries.add(entry(playerId, flag));
            } catch (IllegalArgumentException ignored) {
                // Malformed onboarding state is isolated instead of making the authored world unloadable.
            }
        }
        return data;
    }

    public static ExternalWorldSavedData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("server required");
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean initialized(UUID playerId) {
        return playerId != null && initializedPlayers.contains(playerId);
    }

    public void markInitialized(UUID playerId) {
        if (playerId != null && initializedPlayers.add(playerId)) setDirty();
    }

    public boolean onboardingFlag(UUID playerId, String flag) {
        String clean = cleanFlag(flag);
        return playerId != null && !clean.isBlank() && onboardingEntries.contains(entry(playerId, clean));
    }

    public Set<String> onboardingFlags(UUID playerId) {
        if (playerId == null) return Set.of();
        String prefix = playerId + "|";
        Set<String> out = new LinkedHashSet<>();
        for (String value : onboardingEntries) {
            if (value.startsWith(prefix) && value.length() > prefix.length()) {
                out.add(value.substring(prefix.length()));
            }
        }
        return Set.copyOf(out);
    }

    public void markOnboardingFlag(UUID playerId, String flag) {
        String clean = cleanFlag(flag);
        if (playerId != null && !clean.isBlank() && onboardingEntries.add(entry(playerId, clean))) setDirty();
    }

    private static String entry(UUID playerId, String flag) {
        return playerId + "|" + flag;
    }

    private static String cleanFlag(String flag) {
        String clean = flag == null ? "" : flag.trim();
        if (clean.isBlank() || clean.length() > 64 || clean.indexOf('|') >= 0) return "";
        return clean;
    }
}
