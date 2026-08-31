package kr.moonseungjun.survivalascension.infrastructure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class InfrastructureData extends SavedData {
    private static final int CURRENT_SCHEMA = 2;
    private static final Codec<Map<String, Integer>> FUNDING_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    public static final SavedDataType<InfrastructureData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "infrastructure_v1"),
            InfrastructureData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    FUNDING_CODEC.optionalFieldOf("funding", Map.of()).forGetter(InfrastructureData::fundingSnapshot),
                    Codec.INT.optionalFieldOf("schema_version", 1).forGetter(InfrastructureData::schemaVersion)
            ).apply(instance, InfrastructureData::new))
    );

    private final Map<String, Integer> funding = new HashMap<>();
    private int schemaVersion = CURRENT_SCHEMA;

    public InfrastructureData() {}

    private InfrastructureData(Map<String, Integer> funding, int schemaVersion) {
        funding.forEach((key, value) -> this.funding.put(key, Math.max(0, value)));
        this.schemaVersion = Math.max(1, schemaVersion);
    }

    private Map<String, Integer> fundingSnapshot() { return Map.copyOf(funding); }
    private int schemaVersion() { return schemaVersion; }

    public static InfrastructureData get(MinecraftServer server) {
        InfrastructureData data = server.getDataStorage().computeIfAbsent(TYPE);
        data.migrateLegacyFundingIfNeeded();
        return data;
    }

    public static InfrastructureData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    public int contributed(InfrastructureProject project, int requirementIndex) {
        return contributed(project, project.requirements().get(requirementIndex));
    }

    public int contributed(InfrastructureProject project, InfrastructureProject.Requirement requirement) {
        return funding.getOrDefault(stableKey(project, requirement), 0);
    }

    public int remaining(InfrastructureProject project, int requirementIndex) {
        InfrastructureProject.Requirement requirement = project.requirements().get(requirementIndex);
        return Math.max(0, requirement.amount() - contributed(project, requirement));
    }

    public int addContribution(InfrastructureProject project, int requirementIndex, int amount) {
        if (amount <= 0) return 0;
        InfrastructureProject.Requirement requirement = project.requirements().get(requirementIndex);
        String key = stableKey(project, requirement);
        int before = funding.getOrDefault(key, 0);
        int after = Math.min(requirement.amount(), before + amount);
        if (after != before) {
            funding.put(key, after);
            setDirty();
        }
        return after - before;
    }

    public boolean isComplete(InfrastructureProject project) {
        for (int i = 0; i < project.requirements().size(); i++) {
            if (remaining(project, i) > 0) return false;
        }
        return true;
    }

    private void migrateLegacyFundingIfNeeded() {
        if (schemaVersion >= CURRENT_SCHEMA) return;

        boolean changed = false;
        for (InfrastructureProject project : InfrastructureProject.values()) {
            Set<Integer> oldIndices = new HashSet<>();
            for (InfrastructureProject.Requirement requirement : project.requirements()) {
                oldIndices.addAll(requirement.legacyIndices());
                String targetKey = stableKey(project, requirement);
                if (funding.containsKey(targetKey)) continue;

                int migrated = 0;
                for (int legacyIndex : requirement.legacyIndices()) {
                    migrated += funding.getOrDefault(legacyKey(project, legacyIndex), 0);
                }
                migrated = Math.min(requirement.amount(), Math.max(0, migrated));
                if (migrated > 0) {
                    funding.put(targetKey, migrated);
                    changed = true;
                }
            }
            for (int legacyIndex : oldIndices) {
                changed |= funding.remove(legacyKey(project, legacyIndex)) != null;
            }
        }

        schemaVersion = CURRENT_SCHEMA;
        setDirty();
    }

    private static String stableKey(InfrastructureProject project, InfrastructureProject.Requirement requirement) {
        return project.id() + ":req:" + requirement.key();
    }

    private static String legacyKey(InfrastructureProject project, int requirementIndex) {
        return project.id() + ":" + requirementIndex;
    }
}
