package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative progress for the blocking first-entry realm preparation screen. */
public record RealmBuildProgressPayload(
        String homelandId,
        String phase,
        int percent,
        String message,
        boolean complete,
        boolean failed
) implements CustomPacketPayload {
    public static final Type<RealmBuildProgressPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "realm_build_progress")
    );

    public static final StreamCodec<ByteBuf, RealmBuildProgressPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RealmBuildProgressPayload::homelandId,
            ByteBufCodecs.STRING_UTF8,
            RealmBuildProgressPayload::phase,
            ByteBufCodecs.VAR_INT,
            RealmBuildProgressPayload::percent,
            ByteBufCodecs.STRING_UTF8,
            RealmBuildProgressPayload::message,
            ByteBufCodecs.BOOL,
            RealmBuildProgressPayload::complete,
            ByteBufCodecs.BOOL,
            RealmBuildProgressPayload::failed,
            RealmBuildProgressPayload::new
    );

    public RealmBuildProgressPayload {
        homelandId = homelandId == null ? "unknown" : homelandId;
        phase = phase == null ? "preparing" : phase;
        percent = Math.max(0, Math.min(100, percent));
        message = message == null ? "왕국을 준비하고 있습니다." : message;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
