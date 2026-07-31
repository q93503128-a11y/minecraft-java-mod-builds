package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StarterNpcProgressSavedData extends SavedData {
    private static final Codec<StarterNpcProgressSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .optionalFieldOf("met_npcs", Map.of())
                    .forGetter(StarterNpcProgressSavedData::encodedProgress)
    ).apply(instance, StarterNpcProgressSavedData::new));

    public static final SavedDataType<StarterNpcProgressSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "starter_npc_progress"),
            level -> new StarterNpcProgressSavedData(),
            level -> CODEC
    );

    private final Map<String, Set<String>> metNpcs;

    public StarterNpcProgressSavedData() {
        this(Map.of());
    }

    private StarterNpcProgressSavedData(Map<String, List<String>> encoded) {
        this.metNpcs = new LinkedHashMap<>();
        encoded.forEach((playerId, npcIds) -> this.metNpcs.put(playerId, new LinkedHashSet<>(npcIds)));
    }

    public boolean markMet(UUID playerId, String npcId) {
        boolean added = metNpcs.computeIfAbsent(playerId.toString(), ignored -> new LinkedHashSet<>()).add(npcId);
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean hasMet(UUID playerId, String npcId) {
        return metNpcs.getOrDefault(playerId.toString(), Set.of()).contains(npcId);
    }

    private Map<String, List<String>> encodedProgress() {
        Map<String, List<String>> encoded = new LinkedHashMap<>();
        metNpcs.forEach((playerId, npcIds) -> encoded.put(playerId, new ArrayList<>(npcIds)));
        return encoded;
    }
}
