package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.stream.Collectors;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.GraphSkillTreeCatalog;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillTreeStatePayload(int classIndex, int revision, int skillPoints, int spentPoints,
                                    int[] purchasedNodeIndexes, boolean combatLocked, Result result)
        implements CustomPacketPayload {
    private static final int MAX_NODE_INDEXES = 108;
    public static final Type<SkillTreeStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AnomalousClasses.MOD_ID, "skill_tree_state"));
    public static final StreamCodec<ByteBuf, SkillTreeStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.classIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.revision);
                ByteBufCodecs.VAR_INT.encode(buffer, value.skillPoints);
                ByteBufCodecs.VAR_INT.encode(buffer, value.spentPoints);
                ByteBufCodecs.VAR_INT.encode(buffer, value.purchasedNodeIndexes.length);
                for (int index : value.purchasedNodeIndexes) ByteBufCodecs.VAR_INT.encode(buffer, index);
                ByteBufCodecs.BOOL.encode(buffer, value.combatLocked);
                ByteBufCodecs.VAR_INT.encode(buffer, value.result.ordinal());
            }, buffer -> {
                int classIndex = ByteBufCodecs.VAR_INT.decode(buffer);
                int revision = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int skillPoints = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int spentPoints = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int count = Math.max(0, Math.min(MAX_NODE_INDEXES, ByteBufCodecs.VAR_INT.decode(buffer)));
                int[] owned = new int[count];
                for (int index = 0; index < count; index++) owned[index] = ByteBufCodecs.VAR_INT.decode(buffer);
                boolean combatLocked = ByteBufCodecs.BOOL.decode(buffer);
                int resultIndex = ByteBufCodecs.VAR_INT.decode(buffer);
                Result[] results = Result.values();
                Result result = resultIndex >= 0 && resultIndex < results.length ? results[resultIndex] : Result.STATUS;
                if (classIndex < -1 || classIndex >= ClassId.values().length) classIndex = -1;
                return new SkillTreeStatePayload(classIndex, revision, skillPoints, spentPoints,
                        owned, combatLocked, result);
            });

    public SkillTreeStatePayload {
        purchasedNodeIndexes = purchasedNodeIndexes == null ? new int[0] : purchasedNodeIndexes.clone();
        if (purchasedNodeIndexes.length > MAX_NODE_INDEXES) throw new IllegalArgumentException("Skill tree state is too large");
        skillPoints = Math.max(0, skillPoints);
        spentPoints = Math.max(0, spentPoints);
        result = result == null ? Result.STATUS : result;
    }

    @Override public int[] purchasedNodeIndexes() { return purchasedNodeIndexes.clone(); }

    public Set<String> purchasedNodeIds(ClassId id) {
        GraphSkillTreeCatalog.Tree tree = GraphSkillTreeCatalog.tree(id);
        return java.util.Arrays.stream(purchasedNodeIndexes).filter(index -> index >= 0 && index < tree.purchaseNodes().size())
                .mapToObj(index -> tree.purchaseNodes().get(index).id()).collect(Collectors.toUnmodifiableSet());
    }

    public static SkillTreeStatePayload from(PlayerClassProfile profile, ClassId id, boolean locked, Result result) {
        ClassProgress progress = profile.progress(id);
        return new SkillTreeStatePayload(id.networkIndex(), profile.revision(), progress.skillPoints(),
                GraphSkillTreeCatalog.spent(id, progress.purchasedNodes()),
                GraphSkillTreeCatalog.purchasedIndexes(id, progress.purchasedNodes()), locked, result);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum Result { OPEN, STATUS, UPDATED, INSUFFICIENT_SP, ALREADY_OWNED, MISSING_REQUIREMENT,
        NOT_CONNECTED, COMBAT_LOCKED, STALE_REQUEST, INVALID_CLASS, INVALID_NODE, INVALID_ACTION, RESET, MIGRATION_FAILED }
}
