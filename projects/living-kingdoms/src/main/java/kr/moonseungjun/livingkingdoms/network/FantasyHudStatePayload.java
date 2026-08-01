package kr.moonseungjun.livingkingdoms.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Compact server-authoritative civic status for the always-visible fantasy HUD. */
public record FantasyHudStatePayload(
        long silver,
        int renown,
        int wanted,
        String profession,
        int grainIndex,
        int metalIndex,
        int herbIndex,
        int laborIndex
) implements CustomPacketPayload {
    public static final Type<FantasyHudStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "fantasy_hud_state")
    );

    public static final StreamCodec<ByteBuf, FantasyHudStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            FantasyHudStatePayload::silver,
            ByteBufCodecs.VAR_INT,
            FantasyHudStatePayload::renown,
            ByteBufCodecs.VAR_INT,
            FantasyHudStatePayload::wanted,
            ByteBufCodecs.STRING_UTF8,
            FantasyHudStatePayload::profession,
            ByteBufCodecs.VAR_INT,
            FantasyHudStatePayload::grainIndex,
            ByteBufCodecs.VAR_INT,
            FantasyHudStatePayload::metalIndex,
            ByteBufCodecs.VAR_INT,
            FantasyHudStatePayload::herbIndex,
            ByteBufCodecs.VAR_INT,
            FantasyHudStatePayload::laborIndex,
            FantasyHudStatePayload::new
    );

    public FantasyHudStatePayload {
        silver = Math.max(0L, silver);
        renown = Math.max(0, renown);
        wanted = Math.max(0, wanted);
        profession = profession == null || profession.isBlank() ? "미등록" : profession;
        grainIndex = bounded(grainIndex);
        metalIndex = bounded(metalIndex);
        herbIndex = bounded(herbIndex);
        laborIndex = bounded(laborIndex);
    }

    private static int bounded(int value) {
        return Math.max(0, Math.min(999, value));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
