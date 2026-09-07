package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Data-driven encounter roster and reward/scene references. */
public record EncounterDefinition(
        String id,
        int difficulty,
        List<EnemySlot> enemies,
        String rewardTable,
        String sceneKey,
        boolean repeatable
) {
    public record EnemySlot(String character, int level, int currentStar) {
        public static final Codec<EnemySlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("character").forGetter(EnemySlot::character),
                Codec.INT.fieldOf("level").forGetter(EnemySlot::level),
                Codec.INT.fieldOf("currentStar").forGetter(EnemySlot::currentStar)
        ).apply(instance, EnemySlot::new));
    }

    public static final Codec<EncounterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(EncounterDefinition::id),
            Codec.INT.fieldOf("difficulty").forGetter(EncounterDefinition::difficulty),
            EnemySlot.CODEC.listOf().fieldOf("enemies").forGetter(EncounterDefinition::enemies),
            Codec.STRING.fieldOf("rewardTable").forGetter(EncounterDefinition::rewardTable),
            Codec.STRING.fieldOf("sceneKey").forGetter(EncounterDefinition::sceneKey),
            Codec.BOOL.optionalFieldOf("repeatable", true).forGetter(EncounterDefinition::repeatable)
    ).apply(instance, EncounterDefinition::new));

    public EncounterDefinition {
        enemies = enemies == null ? List.of() : List.copyOf(enemies);
    }
}
