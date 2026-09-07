package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.network.chat.Component;

/**
 * Localized HUD copy for authoritative battle snapshot facts.
 * This class never predicts combat outcomes or target legality.
 */
public final class BattleHudPresentation {
    private BattleHudPresentation() {}

    public static Component currentTurnLabel() {
        return Component.translatable("hud.turnbound_re.state.now");
    }

    public static Component exposedLabel() {
        return Component.translatable("hud.turnbound_re.status.exposed");
    }

    public static Component intentLine(BattleNetworkPayloads.SnapshotParticipant enemy, String actionName) {
        if (!enemy.alive()) return Component.translatable("hud.turnbound_re.state.defeated");
        BattleNetworkPayloads.SnapshotIntent intent = enemy.intent();
        if (intent == null) return Component.translatable("hud.turnbound_re.intent.none");

        Component risk = Component.translatable(intentRiskKey(intent.risk()));
        Component type = Component.translatable(intentTypeKey(intent.type()));
        Component targeting = Component.translatable(intentTargetingKey(intent.targeting()));
        return Component.translatable(
                intent.breakCancelable() ? "hud.turnbound_re.intent.line_break" : "hud.turnbound_re.intent.line",
                risk,
                actionName,
                type,
                targeting);
    }

    public static Component statusLine(BattleNetworkPayloads.SnapshotParticipant participant) {
        if (!participant.alive()) return Component.translatable("hud.turnbound_re.state.defeated");
        if (participant.exposed()) return exposedLabel();
        if (participant.guard()) return Component.translatable("hud.turnbound_re.status.guard");
        if (participant.poiseGuard()) return Component.translatable("hud.turnbound_re.status.poise_guard");
        if (participant.statuses().isEmpty()) return Component.empty();

        BattleNetworkPayloads.SnapshotStatus status = participant.statuses().getFirst();
        Component name = statusName(status.id());
        boolean hasStacks = status.stacks() > 1;
        boolean hasTurns = status.remaining() > 0;
        if (hasStacks && hasTurns) {
            return Component.translatable("hud.turnbound_re.status.detail.stacks_turns", name, status.stacks(), status.remaining());
        }
        if (hasStacks) {
            return Component.translatable("hud.turnbound_re.status.detail.stacks", name, status.stacks());
        }
        if (hasTurns) {
            return Component.translatable("hud.turnbound_re.status.detail.turns", name, status.remaining());
        }
        return name;
    }

    static String intentRiskKey(String risk) {
        return switch (risk) {
            case "NORMAL" -> "hud.turnbound_re.intent.risk.normal";
            case "DANGEROUS" -> "hud.turnbound_re.intent.risk.dangerous";
            case "ULTIMATE" -> "hud.turnbound_re.intent.risk.ultimate";
            default -> "hud.turnbound_re.intent.unknown";
        };
    }

    static String intentTypeKey(String type) {
        return switch (type) {
            case "ATTACK" -> "hud.turnbound_re.intent.type.attack";
            case "DEFEND" -> "hud.turnbound_re.intent.type.defend";
            case "BUFF" -> "hud.turnbound_re.intent.type.buff";
            case "DEBUFF" -> "hud.turnbound_re.intent.type.debuff";
            case "SPECIAL" -> "hud.turnbound_re.intent.type.special";
            default -> "hud.turnbound_re.intent.unknown";
        };
    }

    static String intentTargetingKey(String targeting) {
        return switch (targeting) {
            case "SINGLE" -> "hud.turnbound_re.intent.target.single";
            case "ALL" -> "hud.turnbound_re.intent.target.all";
            case "SELF" -> "hud.turnbound_re.intent.target.self";
            case "RANDOM" -> "hud.turnbound_re.intent.target.random";
            default -> "hud.turnbound_re.intent.unknown";
        };
    }

    static String statusNameKey(String id) {
        String raw = stripNamespace(id);
        return switch (raw) {
            case "guard" -> "hud.turnbound_re.status.guard";
            case "exposed" -> "hud.turnbound_re.status.exposed";
            case "poise_guard" -> "hud.turnbound_re.status.poise_guard";
            case "burn" -> "hud.turnbound_re.status.burn";
            case "slow" -> "hud.turnbound_re.status.slow";
            case "atk_up" -> "hud.turnbound_re.status.atk_up";
            case "def_down" -> "hud.turnbound_re.status.def_down";
            case "venom" -> "hud.turnbound_re.status.venom";
            case "webbed" -> "hud.turnbound_re.status.webbed";
            case "evasion" -> "hud.turnbound_re.status.evasion";
            case "ward" -> "hud.turnbound_re.status.ward";
            case "volatile" -> "hud.turnbound_re.status.volatile";
            default -> "";
        };
    }

    private static Component statusName(String id) {
        String key = statusNameKey(id);
        return key.isBlank() ? Component.literal(humanizeId(id)) : Component.translatable(key);
    }

    private static String stripNamespace(String id) {
        if (id == null || id.isBlank()) return "";
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static String humanizeId(String id) {
        String raw = stripNamespace(id);
        if (raw.isBlank()) return "?";
        String[] words = raw.replace('-', '_').split("_+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase());
        }
        return out.isEmpty() ? "?" : out.toString();
    }
}
