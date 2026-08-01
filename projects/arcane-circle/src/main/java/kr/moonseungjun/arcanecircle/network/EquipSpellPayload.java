package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EquipSpellPayload(String spellId, int slot) implements CustomPacketPayload {
    public static final Type<EquipSpellPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "equip_spell"));
    public static final StreamCodec<ByteBuf, EquipSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EquipSpellPayload::spellId,
            ByteBufCodecs.VAR_INT, EquipSpellPayload::slot,
            EquipSpellPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
