package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Data-driven authored-world region graph. World geometry is deliberately referenced by locator ids
 * so production coordinates/art can remain behind the World Asset Gate.
 */
public record RegionDefinition(
        String id,
        String kind,
        String dimension,
        List<String> exits,
        List<EncounterAnchor> encounterAnchors
) {
    public record EncounterAnchor(
            String id,
            String encounter,
            String locator,
            boolean repeatable
    ) {
        public static final Codec<EncounterAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(EncounterAnchor::id),
                Codec.STRING.fieldOf("encounter").forGetter(EncounterAnchor::encounter),
                Codec.STRING.fieldOf("locator").forGetter(EncounterAnchor::locator),
                Codec.BOOL.optionalFieldOf("repeatable", true).forGetter(EncounterAnchor::repeatable)
        ).apply(instance, EncounterAnchor::new));
    }

    public static final Codec<RegionDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(RegionDefinition::id),
            Codec.STRING.fieldOf("kind").forGetter(RegionDefinition::kind),
            Codec.STRING.fieldOf("dimension").forGetter(RegionDefinition::dimension),
            Codec.STRING.listOf().optionalFieldOf("exits", List.of()).forGetter(RegionDefinition::exits),
            EncounterAnchor.CODEC.listOf().optionalFieldOf("encounterAnchors", List.of()).forGetter(RegionDefinition::encounterAnchors)
    ).apply(instance, RegionDefinition::new));

    public RegionDefinition {
        exits = exits == null ? List.of() : List.copyOf(exits);
        encounterAnchors = encounterAnchors == null ? List.of() : List.copyOf(encounterAnchors);
    }
}
