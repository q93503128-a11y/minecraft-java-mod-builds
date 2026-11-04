package dev.moonseungjun.augmentationsurvival.augment.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/** Persistent pending companion resurrection markers, including their entity snapshots. */
public record ReviveWorldState(Map<String, Grave> graves) {
    public static final Codec<ReviveWorldState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Grave.CODEC).optionalFieldOf("graves", Map.of())
                    .forGetter(ReviveWorldState::graves)
    ).apply(instance, ReviveWorldState::new));

    public ReviveWorldState {
        graves = Map.copyOf(graves);
    }

    public static ReviveWorldState initial() {
        return new ReviveWorldState(Map.of());
    }

    public record Grave(String owner, String type, CompoundTag snapshot, String dimension,
                        List<Double> position, long expiresAtTick) {
        public static final Codec<Grave> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("owner").forGetter(Grave::owner),
                Codec.STRING.fieldOf("type").forGetter(Grave::type),
                TagParser.LENIENT_CODEC.fieldOf("snapshot").forGetter(Grave::snapshot),
                Codec.STRING.fieldOf("dimension").forGetter(Grave::dimension),
                Codec.DOUBLE.listOf().fieldOf("position").forGetter(Grave::position),
                Codec.LONG.fieldOf("expires_at").forGetter(Grave::expiresAtTick)
        ).apply(instance, Grave::new));

        public Grave {
            position = List.copyOf(position);
        }
    }
}
