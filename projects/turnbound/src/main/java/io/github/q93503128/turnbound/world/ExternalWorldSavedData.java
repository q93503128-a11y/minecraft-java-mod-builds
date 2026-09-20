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
        data.onboardingEntries.addAll(DrehmalOnboardingFlags.decode(onboarding));
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
        return DrehmalOnboardingFlags.contains(onboardingEntries, playerId, flag);
    }

    public Set<String> onboardingFlags(UUID playerId) {
        return DrehmalOnboardingFlags.forPlayer(onboardingEntries, playerId);
    }

    public void markOnboardingFlag(UUID playerId, String flag) {
        if (DrehmalOnboardingFlags.add(onboardingEntries, playerId, flag)) setDirty();
    }
}
