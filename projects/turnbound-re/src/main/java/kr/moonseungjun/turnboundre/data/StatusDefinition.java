package kr.moonseungjun.turnboundre.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/** Canonical reloadable status definition. */
public record StatusDefinition(
        String id,
        String polarity,
        String durationUnit,
        int baseDuration,
        int maxStacks,
        String refreshRule,
        List<String> dispelTags,
        List<Hook> hooks
) {
    public record Effect(String type, double value) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(Effect::type),
                Codec.DOUBLE.optionalFieldOf("value", 0.0D).forGetter(Effect::value)
        ).apply(instance, Effect::new));
    }

    public record Hook(String when, Effect effect) {
        public static final Codec<Hook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("when").forGetter(Hook::when),
                Effect.CODEC.fieldOf("effect").forGetter(Hook::effect)
        ).apply(instance, Hook::new));
    }

    public static final Codec<StatusDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(StatusDefinition::id),
            Codec.STRING.fieldOf("polarity").forGetter(StatusDefinition::polarity),
            Codec.STRING.fieldOf("durationUnit").forGetter(StatusDefinition::durationUnit),
            Codec.INT.fieldOf("baseDuration").forGetter(StatusDefinition::baseDuration),
            Codec.INT.fieldOf("maxStacks").forGetter(StatusDefinition::maxStacks),
            Codec.STRING.fieldOf("refreshRule").forGetter(StatusDefinition::refreshRule),
            Codec.STRING.listOf().fieldOf("dispelTags").forGetter(StatusDefinition::dispelTags),
            Hook.CODEC.listOf().optionalFieldOf("hooks", List.of()).forGetter(StatusDefinition::hooks)
    ).apply(instance, StatusDefinition::new));

    public StatusDefinition {
        dispelTags = dispelTags == null ? List.of() : List.copyOf(dispelTags);
        hooks = hooks == null ? List.of() : List.copyOf(hooks);
    }

    /** Compatibility constructor for the earlier M1 stack-only status contract. */
    @Deprecated
    public StatusDefinition(
            String id, String polarity, String durationUnit, int maxStacks, String refreshRule,
            List<String> dispelTags, List<String> legacyHooks
    ) {
        this(id, polarity, durationUnit, 1, maxStacks, refreshRule, dispelTags,
                legacyHooks == null ? List.of() : legacyHooks.stream()
                        .map(hook -> new Hook("LEGACY", new Effect(hook, 0.0D)))
                        .toList());
    }
}
