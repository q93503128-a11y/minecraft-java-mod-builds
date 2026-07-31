package kr.moonseungjun.livingkingdoms.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent player skill points and unlocks. */
public final class SkillProgressionSavedData extends SavedData {
    private static final Codec<SkillState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("points", 0).forGetter(SkillState::points),
            Codec.INT.optionalFieldOf("level_milestone", 0).forGetter(SkillState::levelMilestone),
            Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(SkillState::unlocked)
    ).apply(instance, SkillState::new));

    private static final Codec<SkillProgressionSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, STATE_CODEC)
                    .optionalFieldOf("players", Map.of())
                    .forGetter(data -> Map.copyOf(data.states))
    ).apply(instance, SkillProgressionSavedData::new));

    public static final SavedDataType<SkillProgressionSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "skill_progression"),
            level -> new SkillProgressionSavedData(),
            level -> CODEC
    );

    private final Map<String, SkillState> states;

    public SkillProgressionSavedData() {
        this(Map.of());
    }

    private SkillProgressionSavedData(Map<String, SkillState> states) {
        this.states = new LinkedHashMap<>(states);
    }

    public SkillState state(UUID playerId, String speciesId) {
        String key = playerId.toString();
        SkillState existing = states.get(key);
        if (existing != null) return existing;
        SkillState created = new SkillState(SkillTreeCatalog.initialPoints(speciesId), 0, List.of());
        states.put(key, created);
        setDirty();
        return created;
    }

    public SkillState syncLevel(UUID playerId, String speciesId, int experienceLevel) {
        SkillState current = state(playerId, speciesId);
        int milestone = Math.max(0, experienceLevel / 5);
        if (milestone <= current.levelMilestone()) return current;
        SkillState updated = new SkillState(
                current.points() + milestone - current.levelMilestone(),
                milestone,
                current.unlocked()
        );
        states.put(playerId.toString(), updated);
        setDirty();
        return updated;
    }

    public UnlockResult unlock(UUID playerId, String speciesId, String skillId) {
        SkillState current = state(playerId, speciesId);
        SkillTreeCatalog.SkillNode node = SkillTreeCatalog.node(skillId);
        if (node == null) return new UnlockResult(false, "존재하지 않는 기술입니다.", current);
        if (current.unlocked().contains(skillId)) {
            return new UnlockResult(false, "이미 해금한 기술입니다.", current);
        }
        for (String prerequisite : node.prerequisites()) {
            if (!current.unlocked().contains(prerequisite)) {
                SkillTreeCatalog.SkillNode required = SkillTreeCatalog.node(prerequisite);
                return new UnlockResult(false,
                        "선행 기술 '" + (required == null ? prerequisite : required.title()) + "'이 필요합니다.", current);
            }
        }
        int cost = SkillTreeCatalog.effectiveCost(node, speciesId);
        if (current.points() < cost) {
            return new UnlockResult(false, "기술 점수가 부족합니다. 필요: " + cost, current);
        }
        List<String> unlocked = new ArrayList<>(current.unlocked());
        unlocked.add(skillId);
        SkillState updated = new SkillState(current.points() - cost, current.levelMilestone(), unlocked);
        states.put(playerId.toString(), updated);
        setDirty();
        return new UnlockResult(true, node.title() + " 기술을 해금했습니다.", updated);
    }

    public boolean has(UUID playerId, String speciesId, String skillId) {
        return state(playerId, speciesId).unlocked().contains(skillId);
    }

    public record SkillState(int points, int levelMilestone, List<String> unlocked) {
        public SkillState {
            points = Math.max(0, points);
            levelMilestone = Math.max(0, levelMilestone);
            unlocked = List.copyOf(unlocked);
        }
    }

    public record UnlockResult(boolean accepted, String message, SkillState state) {
    }
}
