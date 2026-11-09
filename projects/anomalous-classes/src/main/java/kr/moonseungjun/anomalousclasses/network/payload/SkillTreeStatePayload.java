package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import kr.moonseungjun.anomalousclasses.state.SkillTreeCatalog;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillTreeStatePayload(int classIndex, int revision, int skillPoints, int[] ranks,
                                    boolean combatLocked, Result result) implements CustomPacketPayload {
    public static final Type<SkillTreeStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "skill_tree_state"));
    public static final StreamCodec<ByteBuf, SkillTreeStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.classIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.revision);
                ByteBufCodecs.VAR_INT.encode(buffer, value.skillPoints);
                ByteBufCodecs.VAR_INT.encode(buffer, value.ranks.length);
                for (int rank : value.ranks) ByteBufCodecs.VAR_INT.encode(buffer, rank);
                ByteBufCodecs.BOOL.encode(buffer, value.combatLocked);
                ByteBufCodecs.VAR_INT.encode(buffer, value.result.ordinal());
            }, buffer -> {
                int classIndex = ByteBufCodecs.VAR_INT.decode(buffer);
                int revision = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int skillPoints = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int count = Math.max(0, Math.min(16, ByteBufCodecs.VAR_INT.decode(buffer)));
                int[] ranks = new int[count];
                for (int index = 0; index < count; index++) ranks[index] = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                boolean combatLocked = ByteBufCodecs.BOOL.decode(buffer);
                int resultIndex = ByteBufCodecs.VAR_INT.decode(buffer);
                Result[] results = Result.values();
                Result result = resultIndex >= 0 && resultIndex < results.length ? results[resultIndex] : Result.STATUS;
                if (classIndex < -1 || classIndex >= ClassId.values().length) classIndex = -1;
                return new SkillTreeStatePayload(classIndex, revision, skillPoints, ranks, combatLocked, result);
            });

    public SkillTreeStatePayload {
        ranks = ranks == null ? new int[0] : ranks.clone();
        if (ranks.length > 16) throw new IllegalArgumentException("Skill tree state is too large");
        skillPoints = Math.max(0, skillPoints);
        result = result == null ? Result.STATUS : result;
    }
    @Override public int[] ranks() { return ranks.clone(); }
    public static SkillTreeStatePayload from(PlayerClassProfile profile, ClassId id, boolean locked, Result result) {
        ClassProgress progress = profile.progress(id);
        int[] ranks = new int[SkillTreeCatalog.nodes(id).size()];
        int[] stored = progress.skillRanks();
        for (int index = 0; index < Math.min(ranks.length, stored.length); index++) {
            ranks[index] = Math.min(SkillTreeCatalog.nodes(id).get(index).maxRank(), stored[index]);
        }
        return new SkillTreeStatePayload(id.networkIndex(), profile.revision(), progress.skillPoints(), ranks, locked, result);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum Result { OPEN, STATUS, UPDATED, INSUFFICIENT_SP, MAX_RANK, NO_BREAKPOINT, COMBAT_LOCKED, STALE_REQUEST, INVALID_CLASS, INVALID_NODE, INVALID_ACTION, RESET }
}
