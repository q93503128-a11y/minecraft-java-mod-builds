package kr.moonseungjun.arcanecircle.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArcaneCircleWorldData extends SavedData {
    private record CircleEntry(long pos, List<Integer> runes) {
        private static final Codec<CircleEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(CircleEntry::pos),
                Codec.INT.listOf().fieldOf("runes").forGetter(CircleEntry::runes)
        ).apply(instance, CircleEntry::new));
    }

    public static final SavedDataType<ArcaneCircleWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "circles"),
            ArcaneCircleWorldData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    CircleEntry.CODEC.listOf().optionalFieldOf("circles", List.of()).forGetter(ArcaneCircleWorldData::entries)
            ).apply(instance, ArcaneCircleWorldData::new))
    );

    private final Map<Long, List<Integer>> circles = new HashMap<>();

    public ArcaneCircleWorldData() {}

    private ArcaneCircleWorldData(List<CircleEntry> entries) {
        for (CircleEntry entry : entries) {
            circles.put(entry.pos(), new ArrayList<>(entry.runes().stream().limit(3).toList()));
        }
    }

    public static ArcaneCircleWorldData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Integer> runes(BlockPos pos) {
        return List.copyOf(circles.getOrDefault(pos.asLong(), List.of()));
    }

    public int addRune(BlockPos pos, int runeCode) {
        List<Integer> runes = circles.computeIfAbsent(pos.asLong(), ignored -> new ArrayList<>(3));
        if (runes.size() >= 3) return -1;
        runes.add(runeCode);
        setDirty();
        return runes.size();
    }

    public List<Integer> clear(BlockPos pos) {
        List<Integer> removed = circles.remove(pos.asLong());
        if (removed == null) return List.of();
        setDirty();
        return List.copyOf(removed);
    }

    public void remove(BlockPos pos) {
        if (circles.remove(pos.asLong()) != null) setDirty();
    }

    private List<CircleEntry> entries() {
        return circles.entrySet().stream().map(entry -> new CircleEntry(entry.getKey(), List.copyOf(entry.getValue()))).toList();
    }
}
