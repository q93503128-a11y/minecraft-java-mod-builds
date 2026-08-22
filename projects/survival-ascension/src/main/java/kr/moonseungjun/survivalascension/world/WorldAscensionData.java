package kr.moonseungjun.survivalascension.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldAscensionData extends SavedData {
    public static final int MAX_STAGE = 2;

    public static final SavedDataType<WorldAscensionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "world_ascension_v1"),
            WorldAscensionData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("stage", 0).forGetter(WorldAscensionData::stage)
            ).apply(instance, WorldAscensionData::new))
    );

    private int stage;

    public WorldAscensionData() {}

    private WorldAscensionData(int stage) {
        this.stage = Math.max(0, Math.min(MAX_STAGE, stage));
    }

    public static WorldAscensionData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public int stage() { return stage; }

    public boolean advanceTo(int requestedStage) {
        int next = Math.max(stage, Math.min(MAX_STAGE, requestedStage));
        if (next == stage) return false;
        stage = next;
        setDirty();
        return true;
    }

    public String stageName() {
        return switch (stage) {
            case 1 -> "전설 단계";
            case 2 -> "종말 단계";
            default -> "각성 단계";
        };
    }
}
