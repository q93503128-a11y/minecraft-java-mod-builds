package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageTowerProgressData extends SavedData {
    private static final Codec<VillageTowerProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("branches", Map.of())
                    .forGetter(data -> data.branches),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("ranks", Map.of())
                    .forGetter(data -> data.ranks)
    ).apply(instance, VillageTowerProgressData::new));

    public static final SavedDataType<VillageTowerProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_tower_progress"),
            level -> new VillageTowerProgressData(),
            level -> CODEC);

    private Map<String, String> branches;
    private Map<String, Integer> ranks;

    public VillageTowerProgressData() {
        this(Map.of(), Map.of());
    }

    private VillageTowerProgressData(Map<String, String> branches, Map<String, Integer> ranks) {
        this.branches = sanitizeBranches(branches);
        this.ranks = sanitizeRanks(ranks);
    }

    public Map<String, String> branches() { return new LinkedHashMap<>(branches); }
    public Map<String, Integer> ranks() { return new LinkedHashMap<>(ranks); }

    public void replace(Map<String, String> newBranches, Map<String, Integer> newRanks) {
        branches = sanitizeBranches(newBranches);
        ranks = sanitizeRanks(newRanks);
        setDirty();
    }

    private static Map<String, String> sanitizeBranches(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            VillageTowerSpecializationSystem.TowerKind kind = VillageTowerSpecializationSystem.TowerKind.fromId(key);
            VillageTowerSpecializationSystem.Branch branch = VillageTowerSpecializationSystem.Branch.fromId(value);
            if (kind != null && branch != null && branch.kind() == kind) {
                result.put(kind.id(), branch.id());
            }
        });
        return result;
    }

    private static Map<String, Integer> sanitizeRanks(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            VillageTowerSpecializationSystem.TowerKind kind = VillageTowerSpecializationSystem.TowerKind.fromId(key);
            if (kind != null && value != null) {
                result.put(kind.id(), Math.max(0, Math.min(VillageTowerSpecializationSystem.MAX_BRANCH_RANK, value)));
            }
        });
        return result;
    }
}
