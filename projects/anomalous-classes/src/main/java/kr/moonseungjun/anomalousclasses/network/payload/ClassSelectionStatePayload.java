package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClassSelectionStatePayload(
        int currentClassIndex,
        int revision,
        int[] levels,
        int[] skillPoints,
        boolean[] played,
        boolean combatLocked,
        Result result) implements CustomPacketPayload {

    public static final Type<ClassSelectionStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "class_selection_state"));
    public static final StreamCodec<ByteBuf, ClassSelectionStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> payload.encode(buffer),
            ClassSelectionStatePayload::decode);

    public ClassSelectionStatePayload {
        levels = levels.clone();
        skillPoints = skillPoints.clone();
        played = played.clone();
        if (levels.length != ClassId.values().length || skillPoints.length != ClassId.values().length || played.length != ClassId.values().length) {
            throw new IllegalArgumentException("Class state arrays must match the supported class count");
        }
        result = result == null ? Result.STATUS : result;
    }

    @Override
    public int[] levels() {
        return levels.clone();
    }

    @Override
    public int[] skillPoints() {
        return skillPoints.clone();
    }

    @Override
    public boolean[] played() {
        return played.clone();
    }

    public static ClassSelectionStatePayload from(PlayerClassProfile profile, boolean combatLocked, Result result) {
        int[] levels = new int[ClassId.values().length];
        int[] skillPoints = new int[ClassId.values().length];
        boolean[] played = new boolean[ClassId.values().length];
        for (ClassId id : ClassId.values()) {
            ClassProgress progress = profile.progress(id);
            levels[id.networkIndex()] = progress.level();
            skillPoints[id.networkIndex()] = progress.skillPoints();
            played[id.networkIndex()] = progress.played();
        }
        return new ClassSelectionStatePayload(
                profile.currentClass().map(ClassId::networkIndex).orElse(-1),
                profile.revision(), levels, skillPoints, played, combatLocked, result);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(ByteBuf buffer) {
        ByteBufCodecs.VAR_INT.encode(buffer, currentClassIndex);
        ByteBufCodecs.VAR_INT.encode(buffer, revision);
        for (int index = 0; index < levels.length; index++) {
            ByteBufCodecs.VAR_INT.encode(buffer, levels[index]);
            ByteBufCodecs.VAR_INT.encode(buffer, skillPoints[index]);
            ByteBufCodecs.BOOL.encode(buffer, played[index]);
        }
        ByteBufCodecs.BOOL.encode(buffer, combatLocked);
        ByteBufCodecs.VAR_INT.encode(buffer, result.ordinal());
    }

    private static ClassSelectionStatePayload decode(ByteBuf buffer) {
        int currentClassIndex = ByteBufCodecs.VAR_INT.decode(buffer);
        int revision = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
        int count = ClassId.values().length;
        int[] levels = new int[count];
        int[] skillPoints = new int[count];
        boolean[] played = new boolean[count];
        for (int index = 0; index < count; index++) {
            levels[index] = Math.max(1, ByteBufCodecs.VAR_INT.decode(buffer));
            skillPoints[index] = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
            played[index] = ByteBufCodecs.BOOL.decode(buffer);
        }
        boolean combatLocked = ByteBufCodecs.BOOL.decode(buffer);
        int resultIndex = ByteBufCodecs.VAR_INT.decode(buffer);
        Result[] results = Result.values();
        Result result = resultIndex >= 0 && resultIndex < results.length ? results[resultIndex] : Result.STATUS;
        if (currentClassIndex < -1 || currentClassIndex >= count) currentClassIndex = -1;
        return new ClassSelectionStatePayload(currentClassIndex, revision, levels, skillPoints, played, combatLocked, result);
    }

    public enum Result {
        OPEN,
        STATUS,
        SAVED,
        ALREADY_CURRENT,
        COMBAT_LOCKED,
        STALE_REQUEST,
        INVALID_CLASS
    }
}
