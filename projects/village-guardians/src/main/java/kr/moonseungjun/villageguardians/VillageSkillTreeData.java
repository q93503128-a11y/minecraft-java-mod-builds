package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class VillageSkillTreeData extends SavedData {
    private static final Codec<VillageSkillTreeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("unlocked_masks", Map.of())
                    .forGetter(data -> data.unlockedMasks)
    ).apply(instance, VillageSkillTreeData::new));

    public static final SavedDataType<VillageSkillTreeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_skill_tree"),
            level -> new VillageSkillTreeData(),
            level -> CODEC);

    private Map<String, Integer> unlockedMasks;

    public VillageSkillTreeData() {
        this(Map.of());
    }

    private VillageSkillTreeData(Map<String, Integer> unlockedMasks) {
        this.unlockedMasks = new LinkedHashMap<>();
        unlockedMasks.forEach((uuid, mask) -> this.unlockedMasks.put(uuid, sanitizeMask(mask)));
    }

    public Map<UUID, Integer> masks() {
        Map<UUID, Integer> parsed = new LinkedHashMap<>();
        unlockedMasks.forEach((uuidText, mask) -> {
            try {
                parsed.put(UUID.fromString(uuidText), sanitizeMask(mask));
            } catch (IllegalArgumentException ignored) {
                // Ignore only the damaged player entry.
            }
        });
        return parsed;
    }

    public void replace(Map<UUID, Integer> masks) {
        unlockedMasks = new LinkedHashMap<>();
        masks.forEach((uuid, mask) -> unlockedMasks.put(uuid.toString(), sanitizeMask(mask)));
        setDirty();
    }

    private static int sanitizeMask(int value) {
        int nodeCount = VillageSkillTreeSystem.Node.values().length;
        int allowedBits = nodeCount >= Integer.SIZE - 1 ? Integer.MAX_VALUE : (1 << nodeCount) - 1;
        return Math.max(0, value) & allowedBits;
    }
}
