package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Prevents named citizens from silently respawning after death or while their entity is unloaded. */
public final class StarterNpcLifeSavedData extends SavedData {
    public static final int SPAWN_TRACKING_REVISION = 1;

    private static final Codec<StarterNpcLifeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("dead_npcs", List.of())
                    .forGetter(data -> List.copyOf(data.deadNpcIds)),
            Codec.STRING.listOf().optionalFieldOf("spawned_npcs", List.of())
                    .forGetter(data -> List.copyOf(data.spawnedNpcIds)),
            Codec.INT.optionalFieldOf("spawn_tracking_revision", 0)
                    .forGetter(data -> data.spawnTrackingRevision)
    ).apply(instance, StarterNpcLifeSavedData::new));

    public static final SavedDataType<StarterNpcLifeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "starter_npc_life"),
            level -> new StarterNpcLifeSavedData(),
            level -> CODEC
    );

    private final Set<String> deadNpcIds;
    private final Set<String> spawnedNpcIds;
    private int spawnTrackingRevision;

    public StarterNpcLifeSavedData() {
        this(List.of(), List.of(), SPAWN_TRACKING_REVISION);
    }

    private StarterNpcLifeSavedData(
            List<String> deadNpcIds,
            List<String> spawnedNpcIds,
            int spawnTrackingRevision) {
        this.deadNpcIds = new LinkedHashSet<>(deadNpcIds);
        this.spawnedNpcIds = new LinkedHashSet<>(spawnedNpcIds);
        this.spawnTrackingRevision = spawnTrackingRevision;
    }

    public boolean isDead(String npcId) {
        return deadNpcIds.contains(npcId);
    }

    public void markDead(String npcId) {
        if (deadNpcIds.add(npcId)) {
            setDirty();
        }
    }

    public boolean wasSpawned(String npcId) {
        return spawnedNpcIds.contains(npcId);
    }

    public void markSpawned(String npcId) {
        if (spawnedNpcIds.add(npcId)) {
            setDirty();
        }
    }

    public boolean requiresSpawnTrackingMigration() {
        return spawnTrackingRevision < SPAWN_TRACKING_REVISION;
    }

    public void migratePreviouslyManaged(List<String> aliveNpcIds) {
        boolean changed = spawnedNpcIds.addAll(aliveNpcIds);
        if (spawnTrackingRevision != SPAWN_TRACKING_REVISION) {
            spawnTrackingRevision = SPAWN_TRACKING_REVISION;
            changed = true;
        }
        if (changed) setDirty();
    }
}
