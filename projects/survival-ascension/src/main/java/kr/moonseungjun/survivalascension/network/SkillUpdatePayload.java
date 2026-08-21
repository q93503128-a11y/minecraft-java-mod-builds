package kr.moonseungjun.survivalascension.network;

/* Payload/StreamCodec pattern adapted from Skill Proficiencies, Copyright (c) 2026 balovich-matje, MIT. */

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SkillUpdatePayload(String skillId, long fromTotalXp, long totalXp, int fromLevel, int level) implements CustomPacketPayload {
    public static final Type<SkillUpdatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "skill_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillUpdatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.skillId());
                buf.writeVarLong(payload.fromTotalXp());
                buf.writeVarLong(payload.totalXp());
                buf.writeVarInt(payload.fromLevel());
                buf.writeVarInt(payload.level());
            },
            buf -> new SkillUpdatePayload(buf.readUtf(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt())
    );
    public boolean levelUp() { return level > fromLevel; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
