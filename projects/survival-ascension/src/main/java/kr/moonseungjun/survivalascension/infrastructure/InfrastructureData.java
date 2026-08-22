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
import java.util.Map;

public final class InfrastructureData extends SavedData {
    private static final Codec<Map<String, Integer>> FUNDING_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    public static final SavedDataType<InfrastructureData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "infrastructure_v1"),
            InfrastructureData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    FUNDING_CODEC.optionalFieldOf("funding", Map.of()).forGetter(InfrastructureData::fundingSnapshot)
            ).apply(instance, InfrastructureData::new))
    );

    private final Map<String, Integer> funding = new HashMap<>();

    public InfrastructureData() {}

    private InfrastructureData(Map<String, Integer> funding) {
        funding.forEach((key, value) -> this.funding.put(key, Math.max(0, value)));
    }

    private Map<String, Integer> fundingSnapshot() { return Map.copyOf(funding); }

    public static InfrastructureData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static InfrastructureData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    public int contributed(InfrastructureProject project, int requirementIndex) {
        return funding.getOrDefault(key(project, requirementIndex), 0);
    }

    public int remaining(InfrastructureProject project, int requirementIndex) {
        InfrastructureProject.Requirement requirement = project.requirements().get(requirementIndex);
        return Math.max(0, requirement.amount() - contributed(project, requirementIndex));
    }

    public int addContribution(InfrastructureProject project, int requirementIndex, int amount) {
        if (amount <= 0) return 0;
        InfrastructureProject.Requirement requirement = project.requirements().get(requirementIndex);
        String key = key(project, requirementIndex);
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

    private static String key(InfrastructureProject project, int requirementIndex) {
        return project.id() + ":" + requirementIndex;
    }
}
