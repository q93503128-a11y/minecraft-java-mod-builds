package kr.moonseungjun.riftfrontier.diagnostics;

import com.mojang.brigadier.Command;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Server-side read-only runtime diagnostics. No gameplay mutation is exposed here. */
public final class RuntimeDiagnosticsCommand {
    private RuntimeDiagnosticsCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("riftfrontier")
                .then(Commands.literal("runtime")
                    .executes(context -> {
                        var source = context.getSource();
                        var snapshot = ContentRuntime.snapshot();
                        var worldData = RiftfrontierWorldData.get(source.getLevel());
                        source.sendSuccess(
                            () -> Component.literal(
                                "Riftfrontier runtime | generation=" + snapshot.generation()
                                    + " | packs=" + snapshot.packIds()
                                    + " | definitions=" + snapshot.definitionCount()
                                    + " | fingerprint=" + snapshot.fingerprint()
                                    + " | world={" + worldData.diagnosticSummary() + "}"
                            ),
                            false
                        );
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );
    }
}
