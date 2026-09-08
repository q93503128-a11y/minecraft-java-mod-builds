package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.EarthToStars;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = EarthToStars.MOD_ID)
public final class ShipServerEvents {
    private ShipServerEvents() {
    }

    @SubscribeEvent
    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("earthtostars")
                        .then(Commands.literal("ship")
                                .then(Commands.literal("spawn").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int entityId = ShipRuntimeManager.spawnAndControl(
                                            player,
                                            context.getSource().getLevel(),
                                            context.getSource().getLevel().getGameTime()
                                    );
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("개척선 조종 연결 완료. W/S 가속, A/D 선회, Space/Shift 기수 조절. 선체 ID: " + entityId),
                                            false
                                    );
                                    return 1;
                                }))
                                .then(Commands.literal("restore").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean restored = ShipRuntimeManager.restoreAndControl(
                                            player,
                                            context.getSource().getLevel(),
                                            context.getSource().getLevel().getGameTime()
                                    );
                                    if (restored) {
                                        context.getSource().sendSuccess(() -> Component.literal("저장된 개척선을 현재 위치에 다시 연결했습니다."), false);
                                        return 1;
                                    }
                                    context.getSource().sendFailure(Component.literal("이 플레이어가 소유한 저장 함선을 찾지 못했습니다."));
                                    return 0;
                                }))
                                .then(Commands.literal("control").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean controlled = ShipRuntimeManager.controlNearest(
                                            player,
                                            context.getSource().getLevel().getGameTime()
                                    );
                                    if (controlled) {
                                        context.getSource().sendSuccess(() -> Component.literal("가까운 개척선 조종을 연결했습니다."), false);
                                        return 1;
                                    }
                                    context.getSource().sendFailure(Component.literal("조종 가능한 개척선이 가까이에 없습니다."));
                                    return 0;
                                }))
                                .then(Commands.literal("release").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean released = ShipRuntimeManager.releaseController(player);
                                    if (released) {
                                        context.getSource().sendSuccess(() -> Component.literal("개척선 조종을 해제했습니다."), false);
                                        return 1;
                                    }
                                    context.getSource().sendFailure(Component.literal("현재 연결된 개척선 조종이 없습니다."));
                                    return 0;
                                })))
        );
    }

    @SubscribeEvent
    private static void onServerTick(ServerTickEvent.Post event) {
        ShipRuntimeManager.tick(event.getServer());
    }

    @SubscribeEvent
    private static void onServerStarting(ServerStartingEvent event) {
        ShipRuntimeManager.initialize(event.getServer());
    }

    @SubscribeEvent
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ShipRuntimeManager.releaseController(event.getEntity().getUUID());
    }

    @SubscribeEvent
    private static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        ShipRuntimeManager.releaseController(event.getEntity().getUUID());
    }
}
