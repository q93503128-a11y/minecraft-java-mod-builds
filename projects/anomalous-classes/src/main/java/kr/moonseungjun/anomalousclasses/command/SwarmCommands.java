package kr.moonseungjun.anomalousclasses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.stream.Collectors;
import kr.moonseungjun.anomalousclasses.state.SwarmCasterService;
import kr.moonseungjun.anomalousclasses.state.SwarmRules;
import kr.moonseungjun.anomalousclasses.state.SwarmSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Usable command interface for user-authored tactics before Codex GUI integration. */
public final class SwarmCommands {
    private SwarmCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("swarm")
            .executes(ctx -> status(ctx.getSource()))
            .then(Commands.literal("tactic")
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("save")
                    .then(Commands.argument("attackPercent", IntegerArgumentType.integer(0, 100))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> save(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "attackPercent"),
                                    StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("select")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> select(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(ctx -> delete(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))));
    }

    private static ServerPlayer eligible(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!SwarmCasterService.isSwarm(player)) {
            source.sendFailure(Component.translatable("message.anomalousclasses.swarm.class_only"));
            return null;
        }
        return player;
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = eligible(source);
        if (player == null) return 0;
        SwarmSavedData.State state = SwarmCasterService.current(player);
        int level = kr.moonseungjun.anomalousclasses.state.ClassProfileSavedData.get(player.getServer())
                .profile(player.getUUID()).progress(kr.moonseungjun.anomalousclasses.state.ClassId.SWARM_CASTER).level();
        source.sendSuccess(() -> Component.translatable("message.anomalousclasses.swarm.status",
                state.alive(), SwarmRules.maximum(level), state.attack(), state.guard(),
                state.regroup(), state.activeName()), false);
        return 1;
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = eligible(source);
        if (player == null) return 0;
        SwarmSavedData.State state = SwarmCasterService.current(player);
        String text = state.tactics().isEmpty() ? "-" : state.tactics().entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue() + "%)")
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.translatable("message.anomalousclasses.swarm.list", text), false);
        return 1;
    }

    private static int save(CommandSourceStack source, int percent, String raw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = eligible(source);
        if (player == null) return 0;
        String name = raw.trim();
        if (!SwarmCasterService.current(player).saveTactic(name, percent)) {
            source.sendFailure(Component.translatable("message.anomalousclasses.swarm.invalid_tactic"));
            return 0;
        }
        SwarmSavedData.get(player.getServer()).changed();
        source.sendSuccess(() -> Component.translatable("message.anomalousclasses.swarm.saved", name, percent), false);
        return 1;
    }

    private static int select(CommandSourceStack source, String raw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = eligible(source);
        if (player == null) return 0;
        String name = raw.trim();
        if (!SwarmCasterService.current(player).activate(name)) {
            source.sendFailure(Component.translatable("message.anomalousclasses.swarm.not_found", name));
            return 0;
        }
        SwarmSavedData.get(player.getServer()).changed();
        source.sendSuccess(() -> Component.translatable("message.anomalousclasses.swarm.selected", name), false);
        return 1;
    }

    private static int delete(CommandSourceStack source, String raw)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = eligible(source);
        if (player == null) return 0;
        String name = raw.trim();
        if (!SwarmCasterService.current(player).removeTactic(name)) {
            source.sendFailure(Component.translatable("message.anomalousclasses.swarm.not_found", name));
            return 0;
        }
        SwarmSavedData.get(player.getServer()).changed();
        source.sendSuccess(() -> Component.translatable("message.anomalousclasses.swarm.deleted", name), false);
        return 1;
    }
}
