package kr.moonseungjun.villageguardians;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class VillageNetwork {
    private VillageNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToClient(OpenVillageUiPayload.TYPE, OpenVillageUiPayload.STREAM_CODEC);
        registrar.playToServer(VillageUiActionPayload.TYPE, VillageUiActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        VillageUiService.handleAction(player, payload.action());
                    }
                });
    }

    public static void open(ServerPlayer player, OpenVillageUiPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public record OpenVillageUiPayload(
            String screenId,
            String title,
            String body,
            String actions,
            String labels) implements CustomPacketPayload {
        public static final Type<OpenVillageUiPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "open_village_ui"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenVillageUiPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::screenId,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::title,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::body,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::actions,
                ByteBufCodecs.STRING_UTF8, OpenVillageUiPayload::labels,
                OpenVillageUiPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record VillageUiActionPayload(String action) implements CustomPacketPayload {
        public static final Type<VillageUiActionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_ui_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VillageUiActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, VillageUiActionPayload::action,
                VillageUiActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
