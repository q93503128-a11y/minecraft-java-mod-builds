package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Canonical character contract from docs/04_CHARACTER_SYSTEM.md and docs/13_DATA_SCHEMA.md. */
public record CharacterDefinition(
        String id,
        String sourceEntity,
        int originStar,
        int squadCost,
        List<String> roles,
        Stats baseStats,
        Growth growth,
        Map<String, Stats> ascensionFlat,
        Map<String, String> affinities,
        String basicAction,
        List<String> skills,
        String burst,
        List<String> passives,
        Availability availability,
        String presentationKey
) {
    public record Stats(int hp, int atk, int def, int spd, int poise) {
        public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("hp").forGetter(Stats::hp),
                Codec.INT.fieldOf("atk").forGetter(Stats::atk),
                Codec.INT.fieldOf("def").forGetter(Stats::def),
                Codec.INT.fieldOf("spd").forGetter(Stats::spd),
                Codec.INT.fieldOf("poise").forGetter(Stats::poise)
        ).apply(instance, Stats::new));
    }

    public record Growth(double hp, double atk, double def, double spd, double poise) {
        public static final Codec<Growth> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("hp").forGetter(Growth::hp),
                Codec.DOUBLE.fieldOf("atk").forGetter(Growth::atk),
                Codec.DOUBLE.fieldOf("def").forGetter(Growth::def),
                Codec.DOUBLE.fieldOf("spd").forGetter(Growth::spd),
                Codec.DOUBLE.fieldOf("poise").forGetter(Growth::poise)
        ).apply(instance, Growth::new));
    }

    public record Availability(String type, String sourceTag) {
        public static final Codec<Availability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Availability::type),
                Codec.STRING.optionalFieldOf("sourceTag", "").forGetter(Availability::sourceTag)
        ).apply(instance, Availability::new));
    }

    public static final Codec<CharacterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterDefinition::id),
            Codec.STRING.optionalFieldOf("sourceEntity", "").forGetter(CharacterDefinition::sourceEntity),
            Codec.INT.fieldOf("originStar").forGetter(CharacterDefinition::originStar),
            Codec.INT.fieldOf("squadCost").forGetter(CharacterDefinition::squadCost),
            Codec.STRING.listOf().fieldOf("roles").forGetter(CharacterDefinition::roles),
            Stats.CODEC.fieldOf("baseStats").forGetter(CharacterDefinition::baseStats),
            Growth.CODEC.fieldOf("growth").forGetter(CharacterDefinition::growth),
            Codec.unboundedMap(Codec.STRING, Stats.CODEC).optionalFieldOf("ascensionFlat", Map.of()).forGetter(CharacterDefinition::ascensionFlat),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("affinities").forGetter(CharacterDefinition::affinities),
            Codec.STRING.fieldOf("basicAction").forGetter(CharacterDefinition::basicAction),
            Codec.STRING.listOf().fieldOf("skills").forGetter(CharacterDefinition::skills),
            Codec.STRING.fieldOf("burst").forGetter(CharacterDefinition::burst),
            Codec.STRING.listOf().optionalFieldOf("passives", List.of()).forGetter(CharacterDefinition::passives),
            Availability.CODEC.fieldOf("availability").forGetter(CharacterDefinition::availability),
            Codec.STRING.fieldOf("presentationKey").forGetter(CharacterDefinition::presentationKey)
    ).apply(instance, CharacterDefinition::new));

    public CharacterDefinition {
        roles = roles == null ? List.of() : List.copyOf(roles);
        ascensionFlat = ascensionFlat == null ? Map.of() : Map.copyOf(ascensionFlat);
        affinities = affinities == null ? Map.of() : Map.copyOf(affinities);
        skills = skills == null ? List.of() : List.copyOf(skills);
        passives = passives == null ? List.of() : List.copyOf(passives);
    }

    /** All referenced action ids in deterministic presentation order. */
    public List<String> actions() {
        List<String> out = new ArrayList<>();
        out.add(basicAction);
        out.addAll(skills);
        out.add(burst);
        out.addAll(passives);
        return List.copyOf(out);
    }
}
