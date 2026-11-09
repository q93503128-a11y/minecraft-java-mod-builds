package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public record ProfileStatePayload(int classIndex, int revision, int level, long experience, int skillPoints,
                                 double maxHealth, double attackDamage, double movementSpeed, double armor)
        implements CustomPacketPayload {
    public static final Type<ProfileStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "profile_state"));
    public static final StreamCodec<ByteBuf, ProfileStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, value.classIndex);
                ByteBufCodecs.VAR_INT.encode(buffer, value.revision);
                ByteBufCodecs.VAR_INT.encode(buffer, value.level);
                ByteBufCodecs.VAR_LONG.encode(buffer, value.experience);
                ByteBufCodecs.VAR_INT.encode(buffer, value.skillPoints);
                ByteBufCodecs.DOUBLE.encode(buffer, value.maxHealth);
                ByteBufCodecs.DOUBLE.encode(buffer, value.attackDamage);
                ByteBufCodecs.DOUBLE.encode(buffer, value.movementSpeed);
                ByteBufCodecs.DOUBLE.encode(buffer, value.armor);
            }, buffer -> new ProfileStatePayload(
                    ByteBufCodecs.VAR_INT.decode(buffer), Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer)),
                    Math.max(1, ByteBufCodecs.VAR_INT.decode(buffer)), Math.max(0L, ByteBufCodecs.VAR_LONG.decode(buffer)),
                    Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer)),
                    finite(ByteBufCodecs.DOUBLE.decode(buffer)), finite(ByteBufCodecs.DOUBLE.decode(buffer)),
                    finite(ByteBufCodecs.DOUBLE.decode(buffer)), finite(ByteBufCodecs.DOUBLE.decode(buffer))));

    public static ProfileStatePayload from(ServerPlayer player, PlayerClassProfile profile, ClassId id) {
        ClassProgress progress = profile.progress(id);
        return new ProfileStatePayload(id.networkIndex(), profile.revision(), progress.level(), progress.experience(),
                progress.skillPoints(), player.getAttributeValue(Attributes.MAX_HEALTH),
                player.getAttributeValue(Attributes.ATTACK_DAMAGE), player.getAttributeValue(Attributes.MOVEMENT_SPEED),
                player.getAttributeValue(Attributes.ARMOR));
    }

    private static double finite(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(value, 1_000_000)) : 0; }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
