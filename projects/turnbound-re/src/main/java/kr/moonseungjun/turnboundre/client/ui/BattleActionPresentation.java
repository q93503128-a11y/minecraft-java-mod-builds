package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Localized presentation copy for server-published battle actions.
 * This class formats known snapshot facts only and never predicts combat results or target legality.
 */
public final class BattleActionPresentation {
    private BattleActionPresentation() {}

    public static Component slotLabel(BattleNetworkPayloads.SnapshotAction action) {
        return switch (action.slot()) {
            case "BASIC" -> Component.translatable("screen.turnbound_re.action.basic");
            case "GUARD" -> Component.translatable("screen.turnbound_re.action.guard");
            case "BURST" -> Component.translatable("screen.turnbound_re.action.burst");
            default -> action.slot().startsWith("SKILL_")
                    ? Component.translatable("screen.turnbound_re.action.skill", action.slot().substring("SKILL_".length()))
                    : Component.translatable("screen.turnbound_re.action.generic");
        };
    }

    public static Component targetRule(BattleNetworkPayloads.SnapshotAction action) {
        return Component.translatable(
                "screen.turnbound_re.tooltip.target",
                targetTeamLabel(action.targetTeam()),
                targetShapeLabel(action.targetShape()),
                action.targetCount());
    }

    public static Component disabledReason(BattleNetworkPayloads.SnapshotAction action) {
        return switch (action.disabledReason()) {
            case "ENERGY" -> Component.translatable("screen.turnbound_re.disabled.energy", action.energyCost());
            case "TARGETS" -> Component.translatable("screen.turnbound_re.disabled.targets", action.targetCount());
            default -> Component.translatable("screen.turnbound_re.disabled.locked");
        };
    }

    public static Component hudCost(BattleNetworkPayloads.SnapshotAction action) {
        if (!action.usable()) {
            return switch (action.disabledReason()) {
                case "ENERGY" -> Component.translatable("hud.turnbound_re.action.need_energy", action.energyCost());
                case "TARGETS" -> Component.translatable("hud.turnbound_re.action.no_target");
                default -> Component.translatable("hud.turnbound_re.action.locked");
            };
        }
        return action.energyCost() > 0
                ? Component.literal("E " + action.energyCost())
                : Component.translatable("hud.turnbound_re.action.ready");
    }

    public static Component selectedSummary(BattleNetworkPayloads.SnapshotAction action) {
        return Component.translatable("screen.turnbound_re.selected_summary", targetRule(action), action.energyCost());
    }

    public static List<FormattedCharSequence> tooltip(BattleNetworkPayloads.SnapshotAction action) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(actionName(action.id())).withStyle(ChatFormatting.WHITE));
        lines.add(targetRule(action).copy().withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.turnbound_re.tooltip.energy", action.energyCost())
                .withStyle(ChatFormatting.GRAY));

        boolean directPower = false;
        if (action.hpPower() > 0) {
            lines.add(Component.translatable("screen.turnbound_re.tooltip.hp_power", action.hpPower())
                    .withStyle(ChatFormatting.GRAY));
            directPower = true;
        }
        if (action.poisePower() > 0) {
            lines.add(Component.translatable("screen.turnbound_re.tooltip.poise_power", action.poisePower())
                    .withStyle(ChatFormatting.GRAY));
            directPower = true;
        }
        if (!directPower) {
            lines.add(Component.translatable("screen.turnbound_re.tooltip.no_direct_power")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!action.damageTag().isBlank()) {
            lines.add(Component.translatable("screen.turnbound_re.tooltip.damage_tag", damageTagLabel(action.damageTag()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!action.usable()) {
            lines.add(disabledReason(action).copy().withStyle(ChatFormatting.RED));
        }
        return lines.stream().map(Component::getVisualOrderText).toList();
    }

    public static String actionName(String actionId) {
        return humanizeId(actionId);
    }

    static String disabledReasonKey(String reason) {
        return switch (reason) {
            case "ENERGY" -> "screen.turnbound_re.disabled.energy";
            case "TARGETS" -> "screen.turnbound_re.disabled.targets";
            default -> "screen.turnbound_re.disabled.locked";
        };
    }

    private static Component targetTeamLabel(String team) {
        return Component.translatable(switch (team) {
            case "SELF" -> "screen.turnbound_re.target_team.self";
            case "ALLY" -> "screen.turnbound_re.target_team.ally";
            case "ENEMY" -> "screen.turnbound_re.target_team.enemy";
            case "ANY" -> "screen.turnbound_re.target_team.any";
            default -> "screen.turnbound_re.target_team.unknown";
        });
    }

    private static Component targetShapeLabel(String shape) {
        return Component.translatable(switch (shape) {
            case "SINGLE" -> "screen.turnbound_re.target_shape.single";
            case "MULTI" -> "screen.turnbound_re.target_shape.multi";
            default -> "screen.turnbound_re.target_shape.unknown";
        });
    }

    private static Component damageTagLabel(String tag) {
        return Component.translatable(switch (tag) {
            case "MELEE" -> "screen.turnbound_re.damage_tag.melee";
            case "PROJECTILE" -> "screen.turnbound_re.damage_tag.projectile";
            case "FIRE" -> "screen.turnbound_re.damage_tag.fire";
            case "BLAST" -> "screen.turnbound_re.damage_tag.blast";
            case "ARCANE" -> "screen.turnbound_re.damage_tag.arcane";
            case "VOID" -> "screen.turnbound_re.damage_tag.void";
            default -> "screen.turnbound_re.damage_tag.unknown";
        });
    }

    private static String humanizeId(String id) {
        if (id == null || id.isBlank()) return "?";
        int colon = id.indexOf(':');
        String raw = colon >= 0 ? id.substring(colon + 1) : id;
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
