package kr.moonseungjun.earthtostars.ship.runtime.minecraft;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.combat.TurretControlMode;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Locale;

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
                                    int entityId = ShipRuntimeManager.spawnAndControl(player, context.getSource().getLevel(), context.getSource().getLevel().getGameTime());
                                    context.getSource().sendSuccess(() -> Component.literal("개척선 조종 연결 완료. W/S 가속, A/D 선회, Space/Shift 기수 조절. 선체 ID: " + entityId), false);
                                    return 1;
                                }))
                                .then(Commands.literal("restore").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean restored = ShipRuntimeManager.restoreAndControl(player, context.getSource().getLevel(), context.getSource().getLevel().getGameTime());
                                    if (restored) {
                                        context.getSource().sendSuccess(() -> Component.literal("저장된 개척선을 현재 위치에 다시 연결했습니다."), false);
                                        return 1;
                                    }
                                    context.getSource().sendFailure(Component.literal("이 플레이어가 소유한 저장 함선을 찾지 못했습니다."));
                                    return 0;
                                }))
                                .then(Commands.literal("control").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean controlled = ShipRuntimeManager.controlNearest(player, context.getSource().getLevel().getGameTime());
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
                                }))
                                .then(Commands.literal("interior")
                                        .then(Commands.literal("enter").executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            if (ShipInteriorManager.enterNearest(player)) {
                                                context.getSource().sendSuccess(() -> Component.literal("함선 내부로 이동했습니다."), false);
                                                return 1;
                                            }
                                            context.getSource().sendFailure(Component.literal("들어갈 수 있는 함선이 가까이에 없거나 내부 구역을 열 수 없습니다."));
                                            return 0;
                                        }))
                                        .then(Commands.literal("exit").executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            if (ShipInteriorManager.exit(player)) {
                                                context.getSource().sendSuccess(() -> Component.literal("함선 외부로 이동했습니다."), false);
                                                return 1;
                                            }
                                            context.getSource().sendFailure(Component.literal("현재 함선 내부에 있지 않거나 외부로 이동할 수 없습니다."));
                                            return 0;
                                        })))
                                .then(Commands.literal("systems")
                                        .then(Commands.literal("status").executes(context -> {
                                            ShipSystemsManager.SystemStatus status = ShipSystemsManager.status(context.getSource().getPlayerOrException());
                                            if (!status.available()) {
                                                context.getSource().sendFailure(Component.literal("확인할 수 있는 함선 시스템이 없습니다."));
                                                return 0;
                                            }
                                            String text = String.format(
                                                    Locale.ROOT,
                                                    "함선 전력 %.1f / %.1f (틱당 +%.1f) | 기관포 탄약 %d / %d | 센서 접촉 %d",
                                                    status.powerStored(),
                                                    status.powerCapacity(),
                                                    status.generationPerTick(),
                                                    status.ammo(),
                                                    status.ammoCapacity(),
                                                    status.contacts()
                                            );
                                            context.getSource().sendSuccess(() -> Component.literal(text), false);
                                            return 1;
                                        })))
                                .then(Commands.literal("turret")
                                        .then(Commands.literal("off").executes(context -> setTurretMode(context.getSource().getPlayerOrException(), TurretControlMode.OFF, context)))
                                        .then(Commands.literal("manual").executes(context -> setTurretMode(context.getSource().getPlayerOrException(), TurretControlMode.MANUAL, context)))
                                        .then(Commands.literal("auto").executes(context -> setTurretMode(context.getSource().getPlayerOrException(), TurretControlMode.AUTO_DEFENSE, context)))
                                        .then(Commands.literal("control").executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            if (ShipTurretManager.requestManualControl(player, context.getSource().getLevel().getGameTime())) {
                                                context.getSource().sendSuccess(() -> Component.literal("함포 수동 조종을 연결했습니다."), false);
                                                return 1;
                                            }
                                            context.getSource().sendFailure(Component.literal("함포 수동 조종권을 얻을 수 없습니다."));
                                            return 0;
                                        }))
                                        .then(Commands.literal("release").executes(context -> {
                                            if (ShipTurretManager.releaseManualControl(context.getSource().getPlayerOrException().getUUID())) {
                                                context.getSource().sendSuccess(() -> Component.literal("함포 수동 조종을 해제했습니다."), false);
                                                return 1;
                                            }
                                            context.getSource().sendFailure(Component.literal("현재 연결된 함포 조종이 없습니다."));
                                            return 0;
                                        }))
                                        .then(Commands.literal("fire").executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            if (ShipTurretManager.fireManual(player, context.getSource().getLevel().getGameTime())) {
                                                context.getSource().sendSuccess(() -> Component.literal("함포 발사."), false);
                                                return 1;
                                            }
                                            context.getSource().sendFailure(Component.literal("발사할 수 없습니다. 전력, 탄약, 조종권, 재장전 시간 또는 사격각을 확인하세요."));
                                            return 0;
                                        }))
                                        .then(Commands.literal("status").executes(context -> {
                                            ShipTurretManager.TurretStatus status = ShipTurretManager.status(context.getSource().getPlayerOrException());
                                            if (!status.available()) {
                                                context.getSource().sendFailure(Component.literal("사용 가능한 함포가 없습니다."));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    "함포: " + status.mode() + " | 공용 탄약 " + status.ammo() + " | 추적 " + status.contacts() + " | 수동조종 " + (status.controlled() ? "사용 중" : "비어 있음")
                                            ), false);
                                            return 1;
                                        }))))
        );
    }

    private static int setTurretMode(ServerPlayer player, TurretControlMode mode, com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        if (ShipTurretManager.setMode(player, mode)) {
            context.getSource().sendSuccess(() -> Component.literal("함포 모드: " + mode), false);
            return 1;
        }
        context.getSource().sendFailure(Component.literal("무장 제어 권한이 있는 함선을 찾지 못했습니다."));
        return 0;
    }

    @SubscribeEvent
    private static void onServerTick(ServerTickEvent.Post event) {
        ShipSystemsManager.tick(event.getServer());
        ShipRuntimeManager.tick(event.getServer());
        ShipTurretManager.tick(event.getServer());
    }

    @SubscribeEvent
    private static void onServerStarting(ServerStartingEvent event) {
        ShipRuntimeManager.initialize(event.getServer());
        ShipSystemsManager.clear();
        ShipTurretManager.clear();
    }

    @SubscribeEvent
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShipInteriorManager.recoverUnlinkedInteriorPlayer(player);
        }
    }

    @SubscribeEvent
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ShipRuntimeManager.releaseController(event.getEntity().getUUID());
        ShipTurretManager.releaseManualControl(event.getEntity().getUUID());
    }

    @SubscribeEvent
    private static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        ShipRuntimeManager.releaseController(event.getEntity().getUUID());
        ShipTurretManager.releaseManualControl(event.getEntity().getUUID());
    }
}
