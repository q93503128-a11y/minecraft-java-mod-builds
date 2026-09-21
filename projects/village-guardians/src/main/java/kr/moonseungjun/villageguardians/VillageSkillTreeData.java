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
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                    .optionalFieldOf("unlocked_masks_v2", Map.of())
                    .forGetter(data -> data.unlockedMasks),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("unlocked_masks", Map.of())
                    .forGetter(data -> data.legacyMasks),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("spent_points_v2", Map.of())
                    .forGetter(data -> data.spentPoints),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("health_training_v1", Map.of())
                    .forGetter(data -> data.healthTraining),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("attack_training_v1", Map.of())
                    .forGetter(data -> data.attackTraining)
    ).apply(instance, VillageSkillTreeData::new));

    public static final SavedDataType<VillageSkillTreeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_skill_tree"),
            level -> new VillageSkillTreeData(),
            level -> CODEC);

    private Map<String, Long> unlockedMasks;
    private Map<String, Integer> legacyMasks;
    private Map<String, Integer> spentPoints;
    private Map<String, Integer> healthTraining;
    private Map<String, Integer> attackTraining;

    public VillageSkillTreeData() { this(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()); }

    private VillageSkillTreeData(Map<String, Long> masks, Map<String, Integer> legacy,
                                 Map<String, Integer> spent, Map<String, Integer> health,
                                 Map<String, Integer> attack) {
        unlockedMasks = new LinkedHashMap<>();
        masks.forEach((uuid, mask) -> unlockedMasks.put(uuid, sanitizeMask(mask)));
        legacyMasks = new LinkedHashMap<>(legacy);
        if (unlockedMasks.isEmpty()) {
            legacy.forEach((uuid, mask) -> unlockedMasks.put(uuid,
                    sanitizeMask(Integer.toUnsignedLong(mask))));
        }
        spentPoints = new LinkedHashMap<>();
        spent.forEach((uuid, value) -> spentPoints.put(uuid, Math.max(0, value)));
        healthTraining = new LinkedHashMap<>();
        health.forEach((uuid, value) -> healthTraining.put(uuid, Math.max(0, value)));
        attackTraining = new LinkedHashMap<>();
        attack.forEach((uuid, value) -> attackTraining.put(uuid, Math.max(0, value)));
    }

    public Map<UUID, Long> masks() {
        Map<UUID, Long> result = new LinkedHashMap<>();
        unlockedMasks.forEach((raw, mask) -> {
            try { result.put(UUID.fromString(raw), sanitizeMask(mask)); }
            catch (IllegalArgumentException ignored) { }
        });
        return result;
    }

    public Map<UUID, Integer> spentPoints() {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        spentPoints.forEach((raw, value) -> {
            try { result.put(UUID.fromString(raw), Math.max(0, value)); }
            catch (IllegalArgumentException ignored) { }
        });
        return result;
    }

    public Map<UUID, Integer> healthTraining() {
        return uuidIntMap(healthTraining);
    }

    public Map<UUID, Integer> attackTraining() {
        return uuidIntMap(attackTraining);
    }

    public void replace(Map<UUID, Long> masks, Map<UUID, Integer> spent,
                        Map<UUID, Integer> health, Map<UUID, Integer> attack) {
        unlockedMasks = new LinkedHashMap<>();
        masks.forEach((uuid, mask) -> unlockedMasks.put(uuid.toString(), sanitizeMask(mask)));
        legacyMasks = new LinkedHashMap<>();
        spentPoints = stringify(spent);
        healthTraining = stringify(health);
        attackTraining = stringify(attack);
        setDirty();
    }

    private static Map<UUID, Integer> uuidIntMap(Map<String, Integer> source) {
        Map<UUID, Integer> result = new LinkedHashMap<>();
        source.forEach((raw, value) -> {
            try { result.put(UUID.fromString(raw), Math.max(0, value)); }
            catch (IllegalArgumentException ignored) { }
        });
        return result;
    }

    private static Map<String, Integer> stringify(Map<UUID, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        source.forEach((uuid, value) -> result.put(uuid.toString(), Math.max(0, value)));
        return result;
    }

    private static long sanitizeMask(long value) {
        int count = VillageSkillTreeSystem.Node.values().length;
        long allowed = count >= Long.SIZE - 1 ? Long.MAX_VALUE : (1L << count) - 1L;
        return Math.max(0L, value) & allowed;
    }
}
