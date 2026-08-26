package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded server-side field knowledge from first-time external hostile kills.
 * It never owns items, currency, population, construction, logistics or companion entities.
 */
public final class SettlementThreatKnowledgeData extends SavedData {
    private static final int MAX_RECORDED_THREAT_TYPES = 64;

    public static final SavedDataType<SettlementThreatKnowledgeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "threat_knowledge"),
            SettlementThreatKnowledgeData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("defeated_external_threats", List.of())
                            .forGetter(data -> data.defeatedExternalThreats)
            ).apply(instance, SettlementThreatKnowledgeData::new))
    );

    private List<String> defeatedExternalThreats;

    public SettlementThreatKnowledgeData() {
        this(List.of());
    }

    public SettlementThreatKnowledgeData(List<String> defeatedExternalThreats) {
        this.defeatedExternalThreats = boundedCopy(defeatedExternalThreats);
    }

    public static SettlementThreatKnowledgeData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<String> defeatedExternalThreats() {
        return defeatedExternalThreats;
    }

    public int threatLevel() {
        return Math.min(SettlementExplorationBenefitService.MAX_THREAT_LEVEL, defeatedExternalThreats.size());
    }

    public boolean recordExternalThreat(String id) {
        if (id == null || id.isBlank() || defeatedExternalThreats.contains(id)
                || defeatedExternalThreats.size() >= MAX_RECORDED_THREAT_TYPES) return false;
        List<String> next = new ArrayList<>(defeatedExternalThreats);
        next.add(id);
        defeatedExternalThreats = List.copyOf(next);
        setDirty();
        return true;
    }

    private static List<String> boundedCopy(List<String> source) {
        if (source == null || source.isEmpty()) return List.of();
        List<String> result = new ArrayList<>(Math.min(source.size(), MAX_RECORDED_THREAT_TYPES));
        for (String value : source) {
            if (value == null || value.isBlank() || result.contains(value)) continue;
            result.add(value);
            if (result.size() >= MAX_RECORDED_THREAT_TYPES) break;
        }
        return List.copyOf(result);
    }
}
