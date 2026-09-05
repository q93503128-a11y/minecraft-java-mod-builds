package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FractureShrineTargetPayload(boolean active, boolean exact, int x, int z) implements CustomPacketPayload {
    public static final Type<FractureShrineTargetPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "fracture_shrine_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FractureShrineTargetPayload> CODEC = StreamCodec.of(
            (buf, value) -> { buf.writeBoolean(value.active()); buf.writeBoolean(value.exact()); buf.writeInt(value.x()); buf.writeInt(value.z()); },
            buf -> new FractureShrineTargetPayload(buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readInt()));
    public static FractureShrineTargetPayload target(boolean exact, int x, int z) { return new FractureShrineTargetPayload(true, exact, x, z); }
    public static FractureShrineTargetPayload clear() { return new FractureShrineTargetPayload(false, false, 0, 0); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
