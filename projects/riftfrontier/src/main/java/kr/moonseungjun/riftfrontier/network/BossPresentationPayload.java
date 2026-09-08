package kr.moonseungjun.riftfrontier.network;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.BossPresentationSemanticState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Wire transport for a server-authoritative boss presentation semantic state. */
public record BossPresentationPayload(BossPresentationSemanticState state) implements CustomPacketPayload {
    public static final Type<BossPresentationPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "boss_presentation")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BossPresentationPayload> STREAM_CODEC =
        StreamCodec.ofMember(BossPresentationPayload::encode, BossPresentationPayload::decode);

    public BossPresentationPayload {
        state = Objects.requireNonNull(state, "state");
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(state.entityId());
        buf.writeVarLong(state.serverGameTick());
        buf.writeBoolean(state.active());
        buf.writeVarInt(state.bossPhase());
        buf.writeUtf(state.patternId());
        buf.writeUtf(state.attackPhase());
        buf.writeDouble(state.phaseProgress());
        buf.writeUtf(state.presentationCue());
        buf.writeUtf(state.delivery());
        buf.writeVarInt(state.counterplay().size());
        state.counterplay().forEach(buf::writeUtf);
        buf.writeBoolean(state.hitWindowOpen());
    }

    private static BossPresentationPayload decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        long serverGameTick = buf.readVarLong();
        boolean active = buf.readBoolean();
        int bossPhase = buf.readVarInt();
        String patternId = buf.readUtf();
        String attackPhase = buf.readUtf();
        double phaseProgress = buf.readDouble();
        String presentationCue = buf.readUtf();
        String delivery = buf.readUtf();
        int count = buf.readVarInt();
        if (count < 0 || count > BossPresentationSemanticState.MAX_COUNTERPLAY) {
            throw new IllegalArgumentException("invalid counterplay count: " + count);
        }
        List<String> counterplay = new ArrayList<>(count);
        for (int i = 0; i < count; i++) counterplay.add(buf.readUtf());
        boolean hitWindowOpen = buf.readBoolean();
        return new BossPresentationPayload(new BossPresentationSemanticState(
            entityId,
            serverGameTick,
            active,
            bossPhase,
            patternId,
            attackPhase,
            phaseProgress,
            presentationCue,
            delivery,
            counterplay,
            hitWindowOpen
        ));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
