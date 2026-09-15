package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Data-driven binding profile for an independently installed authored Minecraft world.
 *
 * <p>The profile stores only TURNBOUND semantic anchors and public integration coordinates. It never contains
 * copied world blocks, structures, datapack commands, textures or other third-party map assets.</p>
 */
public record ExternalWorldProfileDefinition(
        String id,
        String sourceVersion,
        String dimension,
        List<Anchor> anchors
) {
    public static final String FAST_TRAVEL = "FAST_TRAVEL";
    public static final String RESOURCE = "RESOURCE";
    public static final String ENCOUNTER = "ENCOUNTER";

    public record Anchor(
            String kind,
            String locator,
            int x,
            int y,
            int z,
            boolean enabled,
            String sourceNote
    ) {
        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("kind").forGetter(Anchor::kind),
                Codec.STRING.fieldOf("locator").forGetter(Anchor::locator),
                Codec.INT.fieldOf("x").forGetter(Anchor::x),
                Codec.INT.fieldOf("y").forGetter(Anchor::y),
                Codec.INT.fieldOf("z").forGetter(Anchor::z),
                Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Anchor::enabled),
                Codec.STRING.optionalFieldOf("sourceNote", "").forGetter(Anchor::sourceNote)
        ).apply(instance, Anchor::new));

        public Anchor {
            sourceNote = sourceNote == null ? "" : sourceNote;
        }
    }

    public static final Codec<ExternalWorldProfileDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ExternalWorldProfileDefinition::id),
            Codec.STRING.fieldOf("sourceVersion").forGetter(ExternalWorldProfileDefinition::sourceVersion),
            Codec.STRING.fieldOf("dimension").forGetter(ExternalWorldProfileDefinition::dimension),
            Anchor.CODEC.listOf().optionalFieldOf("anchors", List.of()).forGetter(ExternalWorldProfileDefinition::anchors)
    ).apply(instance, ExternalWorldProfileDefinition::new));

    public ExternalWorldProfileDefinition {
        anchors = anchors == null ? List.of() : List.copyOf(anchors);
    }
}
