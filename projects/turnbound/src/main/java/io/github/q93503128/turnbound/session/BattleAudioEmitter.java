package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.network.AudioCuePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Converts authoritative battle events into presentation-only semantic audio cues. */
final class BattleAudioEmitter {
    private BattleAudioEmitter() {}

    static void emit(ServerPlayer player, BattleState state, int eventStart) {
        if (player == null || state == null) return;
        List<BattleEvent> events = state.events();
        if (eventStart < 0 || eventStart >= events.size()) return;
        List<String> cues = new ArrayList<>();
        for (int i = eventStart; i < events.size(); i++) {
            BattleEvent event = events.get(i);
            String encoded = BattleAudioCueRouting.cue(state, event);
            if (encoded != null) cues.add(encoded);
        }
        if (!cues.isEmpty()) PacketDistributor.sendToPlayer(player, new AudioCuePayload(String.join("\n", cues)));
    }

}
