package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VillageRoleProgressData extends SavedData {
    private static final Codec<VillageRoleProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("tree_masks", Map.of())
                    .forGetter(data -> data.treeMasks),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("skill_masks", Map.of())
                    .forGetter(data -> data.skillMasks),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("equipped_skills", Map.of())
                    .forGetter(data -> data.equippedSkills)
    ).apply(instance, VillageRoleProgressData::new));

    public static final SavedDataType<VillageRoleProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_role_progress"),
            level -> new VillageRoleProgressData(),
            level -> CODEC);

    private Map<String, Integer> treeMasks;
    private Map<String, Integer> skillMasks;
    private Map<String, String> equippedSkills;

    public VillageRoleProgressData() {
        this(Map.of(), Map.of(), Map.of());
    }

    private VillageRoleProgressData(
            Map<String, Integer> treeMasks,
            Map<String, Integer> skillMasks,
            Map<String, String> equippedSkills) {
        this.treeMasks = sanitizeMasks(treeMasks, VillageRoleSkillSystem.RoleNode.values().length);
        this.skillMasks = sanitizeMasks(skillMasks, VillageRoleSkillSystem.ActiveSkill.maxRoleSkillCount());
        this.equippedSkills = new LinkedHashMap<>(equippedSkills);
    }

    public Map<String, Integer> treeMasks() {
        return new LinkedHashMap<>(treeMasks);
    }

    public Map<String, Integer> skillMasks() {
        return new LinkedHashMap<>(skillMasks);
    }

    public Map<String, String> equippedSkills() {
        return new LinkedHashMap<>(equippedSkills);
    }

    public void replace(
            Map<String, Integer> newTreeMasks,
            Map<String, Integer> newSkillMasks,
            Map<String, String> newEquippedSkills) {
        treeMasks = sanitizeMasks(newTreeMasks, VillageRoleSkillSystem.RoleNode.values().length);
        skillMasks = sanitizeMasks(newSkillMasks, VillageRoleSkillSystem.ActiveSkill.maxRoleSkillCount());
        equippedSkills = new LinkedHashMap<>(newEquippedSkills);
        setDirty();
    }

    private static Map<String, Integer> sanitizeMasks(Map<String, Integer> source, int bitCount) {
        int allowedBits = bitCount >= Integer.SIZE - 1 ? Integer.MAX_VALUE : (1 << bitCount) - 1;
        Map<String, Integer> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                result.put(key, Math.max(0, value) & allowedBits);
            }
        });
        return result;
    }
}
