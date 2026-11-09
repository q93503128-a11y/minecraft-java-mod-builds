package io.github.q93503128.turnbound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleSessionAuthority;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Receive world-owned presentation state; client never decides authoritative rules. */
public final class TurnboundClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Turnbound.WORLD_MODE_PACKET,
                (client, handler, payload, sender) -> {
                    boolean worldEnabled = payload.readBoolean();
                    client.execute(() -> TurnboundClientWorldState.setEnabled(worldEnabled));
                });
        ClientPlayNetworking.registerGlobalReceiver(TurnboundBattleBridge.SNAPSHOT,
                (client, handler, payload, sender) -> {
                    try {
                        if (!payload.readBoolean()) {
                            client.execute(() -> TurnboundClientBattleView.clear());
                            return;
                        }
                        long sessionId = payload.readLong();
                        long revision = payload.readLong();
                        String actor = payload.readString(128);
                        int outcomeId = payload.readVarInt();
                        if (outcomeId < 0 || outcomeId >= BattleOutcome.values().length) return;
                        int count = payload.readVarInt();
                        if (count < 0 || count > 16) return;
                        List<TurnboundClientBattleView.Unit> units = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            String id = payload.readString(128);
                            String name = payload.readString(128);
                            boolean ally = payload.readBoolean();
                            boolean summon = payload.readBoolean();
                            int hp = payload.readInt();
                            int maxHp = payload.readInt();
                            int barrier = payload.readInt();
                            long gauge = payload.readLong();
                            boolean downed = payload.readBoolean();
                            int skillCount = payload.readVarInt();
                            if (skillCount < 0 || skillCount > 20) return;
                            List<TurnboundClientBattleView.Skill> skills = new ArrayList<>(skillCount);
                            for (int j = 0; j < skillCount; j++) {
                                skills.add(new TurnboundClientBattleView.Skill(
                                        payload.readString(128), payload.readString(128),
                                        payload.readVarInt()));
                            }
                            units.add(new TurnboundClientBattleView.Unit(id, name, ally, summon,
                                    hp, maxHp, barrier, gauge, downed, List.copyOf(skills)));
                        }
                        if (payload.isReadable()) return;
                        var snapshot = new TurnboundClientBattleView.Snapshot(
                                sessionId, revision, actor, BattleOutcome.values()[outcomeId], units);
                        client.execute(() -> TurnboundClientBattleView.update(snapshot));
                    } catch (RuntimeException malformed) {
                        // Malformed server packet is discarded; never update view with partial data.
                    }
                });

        ClientPlayNetworking.registerGlobalReceiver(TurnboundBattleBridge.RECEIPT,
                (client, handler, payload, sender) -> {
                    try {
                        long sessionId = payload.readLong();
                        int status = payload.readVarInt();
                        if (payload.isReadable() || status < 0
                                || status >= BattleSessionAuthority.Status.values().length) return;
                        var receipt = new TurnboundClientBattleView.Receipt(
                                sessionId, BattleSessionAuthority.Status.values()[status]);
                        client.execute(() -> TurnboundClientBattleView.receipt(receipt));
                    } catch (RuntimeException malformed) {
                        // Do not update client state on invalid transport.
                    }
                });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TurnboundClientWorldState.setEnabled(false);
            TurnboundClientBattleView.clear();
        });
    }
}
