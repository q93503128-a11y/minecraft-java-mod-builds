package kr.moonseungjun.riftfrontier.network;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import kr.moonseungjun.riftfrontier.combat.AttackTimeline;
import kr.moonseungjun.riftfrontier.combat.MinecraftBossCombatAdapter;
import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Small server-authoritative semantic snapshot for boss presentation.
 *
 * <p>This payload carries no animation, VFX, sound or damage cadence. Clients resolve authored
 * presentation assets from these semantics and the server tick. The server's PresentationFrame remains
 * derived from the exact AttackExecution snapshot used by damage.</p>
 */
public record BossPresentationPayload(
    int entityId,
    long serverGameTick,
    boolean active,
    int bossPhase,
    String patternId,
    String attackPhase,
    long phaseTick,
    long phaseTicksRemaining,
    double phaseProgress,
    String presentationCue,
    String delivery,
    List<String> counterplay,
    boolean hitWindowOpen
) implements CustomPacketPayload {
    public static final Type<BossPresentationPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Riftfrontier.MOD_ID, "boss_presentation")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BossPresentationPayload> STREAM_CODEC =
        StreamCodec.ofMember(BossPresentationPayload::encode, BossPresentationPayload::decode);

    private static final int MAX_COUNTERPLAY = 16;

    public BossPresentationPayload {
        if (entityId < 0) throw new IllegalArgumentException("entityId must be >= 0");
        if (serverGameTick < 0) throw new IllegalArgumentException("serverGameTick must be >= 0");
        patternId = Objects.requireNonNull(patternId, "patternId");
        attackPhase = Objects.requireNonNull(attackPhase, "attackPhase");
        presentationCue = Objects.requireNonNull(presentationCue, "presentationCue");
        delivery = Objects.requireNonNull(delivery, "delivery");
        counterplay = List.copyOf(Objects.requireNonNull(counterplay, "counterplay"));
        if (counterplay.size() > MAX_COUNTERPLAY) throw new IllegalArgumentException("too many counterplay semantics");
        if (!Double.isFinite(phaseProgress) || phaseProgress < 0.0D || phaseProgress > 1.0D) {
            throw new IllegalArgumentException("phaseProgress must be finite and between 0 and 1");
        }
        if (phaseTick < 0 || phaseTicksRemaining < 0) {
            throw new IllegalArgumentException("phase timing must be >= 0");
        }
        if (active) {
            if (bossPhase <= 0) throw new IllegalArgumentException("active presentation requires a positive boss phase");
            ContentId.parse(patternId);
            AttackTimeline.Phase phase = AttackTimeline.Phase.valueOf(attackPhase);
            if (phase == AttackTimeline.Phase.COMPLETE) throw new IllegalArgumentException("COMPLETE is not an active presentation phase");
            if (hitWindowOpen != (phase == AttackTimeline.Phase.ACTIVE)) {
                throw new IllegalArgumentException("hitWindowOpen must exactly match ACTIVE phase");
            }
        } else if (bossPhase != 0 || !patternId.isEmpty() || !attackPhase.isEmpty() || hitWindowOpen) {
            throw new IllegalArgumentException("inactive presentation must use the canonical clear state");
        }
    }

    public static BossPresentationPayload fromFrame(
        int entityId,
        long serverGameTick,
        MinecraftBossCombatAdapter.PresentationFrame frame
    ) {
        Objects.requireNonNull(frame, "frame");
        return new BossPresentationPayload(
            entityId,
            serverGameTick,
            true,
            frame.bossPhase(),
            frame.patternId().toString(),
            frame.attackPhase().name(),
            frame.phaseTick(),
            frame.phaseTicksRemaining(),
            frame.phaseProgress(),
            frame.presentationCue(),
            frame.delivery(),
            frame.counterplay().stream().sorted().toList(),
            frame.hitWindowOpen()
        );
    }

    public static BossPresentationPayload clear(int entityId, long serverGameTick) {
        return new BossPresentationPayload(entityId, serverGameTick, false, 0, "", "", 0, 0, 0.0D, "", "", List.of(), false);
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarLong(serverGameTick);
        buf.writeBoolean(active);
        buf.writeVarInt(bossPhase);
        buf.writeUtf(patternId);
        buf.writeUtf(attackPhase);
        buf.writeVarLong(phaseTick);
        buf.writeVarLong(phaseTicksRemaining);
        buf.writeDouble(phaseProgress);
        buf.writeUtf(presentationCue);
        buf.writeUtf(delivery);
        buf.writeVarInt(counterplay.size());
        counterplay.forEach(buf::writeUtf);
        buf.writeBoolean(hitWindowOpen);
    }

    private static BossPresentationPayload decode(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        long serverGameTick = buf.readVarLong();
        boolean active = buf.readBoolean();
        int bossPhase = buf.readVarInt();
        String patternId = buf.readUtf();
        String attackPhase = buf.readUtf();
        long phaseTick = buf.readVarLong();
        long phaseTicksRemaining = buf.readVarLong();
        double phaseProgress = buf.readDouble();
        String presentationCue = buf.readUtf();
        String delivery = buf.readUtf();
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_COUNTERPLAY) throw new IllegalArgumentException("invalid counterplay count: " + count);
        List<String> counterplay = new ArrayList<>(count);
        for (int i = 0; i < count; i++) counterplay.add(buf.readUtf());
        boolean hitWindowOpen = buf.readBoolean();
        return new BossPresentationPayload(
            entityId, serverGameTick, active, bossPhase, patternId, attackPhase,
            phaseTick, phaseTicksRemaining, phaseProgress, presentationCue, delivery, counterplay, hitWindowOpen
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
