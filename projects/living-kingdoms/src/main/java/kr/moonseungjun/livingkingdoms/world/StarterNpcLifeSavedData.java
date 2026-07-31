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

/** Prevents named citizens from silently respawning after the player kills them. */
public final class StarterNpcLifeSavedData extends SavedData {
    private static final Codec<StarterNpcLifeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("dead_npcs", List.of())
                    .forGetter(data -> List.copyOf(data.deadNpcIds))
    ).apply(instance, StarterNpcLifeSavedData::new));

    public static final SavedDataType<StarterNpcLifeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "starter_npc_life"),
            level -> new StarterNpcLifeSavedData(),
            level -> CODEC
    );

    private final Set<String> deadNpcIds;

    public StarterNpcLifeSavedData() {
        this(List.of());
    }

    private StarterNpcLifeSavedData(List<String> deadNpcIds) {
        this.deadNpcIds = new LinkedHashSet<>(deadNpcIds);
    }

    public boolean isDead(String npcId) {
        return deadNpcIds.contains(npcId);
    }

    public void markDead(String npcId) {
        if (deadNpcIds.add(npcId)) {
            setDirty();
        }
    }
}
