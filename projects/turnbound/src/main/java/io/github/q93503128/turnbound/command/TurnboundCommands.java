package io.github.q93503128.turnbound.command;

import com.mojang.brigadier.Command;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TurnboundCommands { private TurnboundCommands(){}
    public static void register(RegisterCommandsEvent event){ event.getDispatcher().register(Commands.literal("turnbound")
      .then(Commands.literal("p0").executes(c->{String r=P0Scenario.runAutoDiagnostic(160); c.getSource().sendSuccess(()->Component.literal("TURNBOUND P0: "+r),false); return Command.SINGLE_SUCCESS;}))
      .then(Commands.literal("battle").executes(c->{var p=c.getSource().getPlayerOrException(); BattleSessionManager.start(p); c.getSource().sendSuccess(()->Component.literal("TURNBOUND P0 전투 시작"),false); return Command.SINGLE_SUCCESS;}))
      .then(Commands.literal("leave").executes(c->{var p=c.getSource().getPlayerOrException(); BattleSessionManager.end(p); return Command.SINGLE_SUCCESS;}))); }
}
