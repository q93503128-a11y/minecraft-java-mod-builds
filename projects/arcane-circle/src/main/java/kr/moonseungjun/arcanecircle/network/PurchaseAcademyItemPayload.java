
package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PurchaseAcademyItemPayload(String offerId) implements CustomPacketPayload {
    public static final Type<PurchaseAcademyItemPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "purchase_academy_item"));
    public static final StreamCodec<ByteBuf, PurchaseAcademyItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PurchaseAcademyItemPayload::offerId,
            PurchaseAcademyItemPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
