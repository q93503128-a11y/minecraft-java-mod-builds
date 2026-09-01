package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;
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
            String encoded = cue(state, event);
            if (encoded != null) cues.add(encoded);
        }
        if (!cues.isEmpty()) PacketDistributor.sendToPlayer(player, new AudioCuePayload(String.join("\n", cues)));
    }

    private static String cue(BattleState state, BattleEvent event) {
        String type = event.type();
        return switch (type) {
            case "ACTION" -> encode("skill", "SKILL", 2, event);
            case "DAMAGE" -> encode(heavy(state, event) ? "hit_heavy" : "hit_light", "IMPACT", heavy(state, event) ? 3 : 1, event);
            case "REACTION_DAMAGE" -> encode("reaction_hit", "REACTION", 3, event);
            case "DOT" -> encode("dot_tick", "IMPACT", 1, event);
            case "HEAL", "REACTION_HEAL" -> encode("heal", "SUPPORT", "REACTION_HEAL".equals(type) ? 2 : 1, event);
            case "BARRIER" -> encode("barrier", "SUPPORT", 2, event);
            case "REVIVE", "SELF_REVIVE" -> encode("revive", "SYSTEM", 3, event);
            case "DOWN" -> encode("down", "IMPACT", 3, event);
            case "BOSS_PHASE" -> encode("boss_phase", "SYSTEM", 3, event);
            case "SPAWN" -> encode("spawn", "SYSTEM", 2, event);
            default -> null;
        };
    }

    private static boolean heavy(BattleState state, BattleEvent event) {
        CombatantState target = state.find(event.targetId());
        return target != null && event.value() >= Math.max(1, (int)Math.floor(target.maxHp() * 0.18));
    }

    private static String encode(String id, String group, int priority, BattleEvent event) {
        return safe(id) + "|" + safe(group) + "|" + priority + "|" + safe(event.sourceId()) + "|"
                + safe(event.targetId()) + "|" + safe(event.detail()) + "|" + event.value();
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.replace('|', '/').replace('\n', ' ');
    }
}
