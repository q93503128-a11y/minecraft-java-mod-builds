package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-controlled vanilla flight permissions for a real reserved swarm unit.
 * The vanilla player movement engine retains native collision and input handling.
 */
public final class SwarmFlightService {
    private static final String GRANTED = "anomalousclasses_swarm_flight_granted";
    private static final String DESCENT = "anomalousclasses_swarm_descent";
    private static final String ORIGINAL_SPEED = "anomalousclasses_swarm_original_fly_speed";
    private static final Map<UUID, Flight> MODES = new HashMap<>();
    private static MinecraftServer lastServer;

    private enum Mode { BOARDING, FLYING, DESCENDING }

    private static final class Flight {
        private Mode mode;
        private final int required;
        private int lowLiftTicks;

        Flight(Mode mode, int required) {
            this.mode = mode;
            this.required = required;
        }
    }

    private SwarmFlightService() {}

    public static void toggle(ServerPlayer player) {
        if (!SwarmCasterService.isSwarm(player) || !player.isAlive()
                || player.isSpectator()) return;
        Flight current = MODES.get(player.getUUID());
        if (current != null) {
            if (current.mode == Mode.DESCENDING)
                say(player, "message.anomalousclasses.swarm.flight_landing");
            else {
                end(player, true);
                say(player, "message.anomalousclasses.swarm.flight_stopped");
            }
            return;
        }
        int level = ClassProfileSavedData.get(player.getServer())
                .profile(player.getUUID()).progress(ClassId.SWARM_CASTER).level();
        SwarmSavedData.State state = SwarmCasterService.current(player);
        if (!SwarmFlightRules.qualified(level, state.alive())) {
            say(player, "message.anomalousclasses.swarm.flight_insufficient");
            return;
        }
        if (player.isCreative() || player.getAbilities().mayfly || player.isPassenger()
                || player.isFallFlying() || player.isInWaterOrBubble() || player.isInLava()) {
            say(player, "message.anomalousclasses.swarm.flight_unavailable");
            return;
        }
        MODES.put(player.getUUID(), new Flight(Mode.BOARDING,
                SwarmFlightRules.requiredLift(level)));
        say(player, "message.anomalousclasses.swarm.flight_gathering");
    }

    public static boolean needsLift(ServerPlayer player, SwarmSavedData.State state) {
        Flight flight = MODES.get(player.getUUID());
        return flight != null && flight.mode != Mode.DESCENDING
                && state.lift() < flight.required;
    }

    public static int liftRequired(ServerPlayer player) {
        Flight flight = MODES.get(player.getUUID());
        return flight == null || flight.mode == Mode.DESCENDING ? 0 : flight.required;
    }

    public static boolean isBoarding(ServerPlayer player) {
        Flight flight = MODES.get(player.getUUID());
        return flight != null && flight.mode == Mode.BOARDING;
    }

    public static void afterRoleUpdate(ServerPlayer player, SwarmSavedData.State state,
                                        boolean hasPhysicalLiftNode) {
        Flight flight = MODES.get(player.getUUID());
        if (flight == null || flight.mode != Mode.BOARDING
                || state.lift() != flight.required || !hasPhysicalLiftNode) return;
        if (!eligible(player)) {
            end(player, true);
            return;
        }
        var abilities = player.getAbilities();
        player.getPersistentData().putFloat(ORIGINAL_SPEED, abilities.getFlyingSpeed());
        abilities.setFlyingSpeed(SwarmFlightRules.flyingSpeed(ClassProfileSavedData.get(player.getServer())
                .profile(player.getUUID()).progress(ClassId.SWARM_CASTER).level()));
        abilities.mayfly = true;
        abilities.flying = true;
        player.getPersistentData().putBoolean(GRANTED, true);
        player.getPersistentData().remove(DESCENT);
        player.onUpdateAbilities();
        player.fallDistance = 0;
        flight.mode = Mode.FLYING;
        say(player, "message.anomalousclasses.swarm.flight_started");
    }

    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && SwarmCasterService.isSwarm(player) && !player.isCreative()
                && !player.isSpectator() && !player.isPassenger() && !player.isFallFlying()
                && !player.isInWaterOrBubble() && !player.isInLava();
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (lastServer != server) {
            MODES.clear();
            lastServer = server;
        }
        var online = new HashSet<UUID>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            online.add(id);
            Flight flight = MODES.get(id);
            boolean savedGrant = player.getPersistentData().getBoolean(GRANTED);
            boolean savedDescent = player.getPersistentData().getBoolean(DESCENT);
            if (flight == null && (savedGrant || savedDescent)) {
                // A crash or disconnect must never grant perpetual creative flight.
                if (savedGrant) revokeFlight(player);
                if (!player.onGround() && player.isAlive() && !player.isCreative()
                        && !player.isSpectator()) {
                    flight = new Flight(Mode.DESCENDING, 0);
                    MODES.put(id, flight);
                    player.getPersistentData().putBoolean(DESCENT, true);
                } else {
                    player.getPersistentData().remove(DESCENT);
                }
            }
            if (flight == null) {
                if (SwarmCasterService.isSwarm(player)
                        && SwarmCasterService.current(player).lift() > 0) release(player);
                continue;
            }
            if (flight.mode == Mode.DESCENDING) {
                if (player.onGround() || !player.isAlive()) {
                    finishLanding(player);
                } else if (server.overworld().getGameTime() % 12 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 45, 0,
                            false, false, false));
                    player.fallDistance = 0;
                }
                continue;
            }
            if (!eligible(player)) {
                end(player, true);
                continue;
            }
            SwarmSavedData.State state = SwarmCasterService.current(player);
            if (flight.mode == Mode.BOARDING) {
                if (state.alive() < flight.required) {
                    end(player, true);
                    say(player, "message.anomalousclasses.swarm.flight_insufficient");
                }
                continue;
            }
            if (state.lift() < flight.required) {
                if (++flight.lowLiftTicks >= 20) {
                    end(player, true);
                    say(player, "message.anomalousclasses.swarm.flight_insufficient");
                    continue;
                }
            } else flight.lowLiftTicks = 0;
            if (player.getAbilities().mayfly && !player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            player.fallDistance = 0;
        }
        MODES.keySet().removeIf(id -> !online.contains(id));
    }

    /** Flight never continues through an unsupported state or role loss. */
    public static void end(ServerPlayer player, boolean controlledDescent) {
        Flight previous = MODES.remove(player.getUUID());
        boolean hadGrant = player.getPersistentData().getBoolean(GRANTED);
        if (hadGrant) revokeFlight(player);
        release(player);
        if (controlledDescent && (hadGrant || (previous != null
                && previous.mode == Mode.DESCENDING)) && player.isAlive()
                && !player.onGround() && !player.isCreative() && !player.isSpectator()) {
            MODES.put(player.getUUID(), new Flight(Mode.DESCENDING, 0));
            player.getPersistentData().putBoolean(DESCENT, true);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 45, 0,
                    false, false, false));
            player.fallDistance = 0;
        } else {
            player.getPersistentData().remove(DESCENT);
        }
    }

    private static void release(ServerPlayer player) {
        if (player.getServer() == null) return;
        SwarmSavedData data = SwarmSavedData.get(player.getServer());
        if (data.player(player.getUUID()).releaseLift() > 0) data.changed();
    }

    private static void revokeFlight(ServerPlayer player) {
        var abilities = player.getAbilities();
        if (!player.isCreative() && !player.isSpectator()) {
            abilities.mayfly = false;
            abilities.flying = false;
        }
        float speed = player.getPersistentData().contains(ORIGINAL_SPEED)
                ? player.getPersistentData().getFloat(ORIGINAL_SPEED) : .05F;
        abilities.setFlyingSpeed(speed);
        player.getPersistentData().remove(GRANTED);
        player.getPersistentData().remove(ORIGINAL_SPEED);
        player.onUpdateAbilities();
    }

    private static void finishLanding(ServerPlayer player) {
        MODES.remove(player.getUUID());
        player.getPersistentData().remove(DESCENT);
        player.fallDistance = 0;
    }

    /** Lifecycle removes runtime possession but conserves N and safety. */
    public static void onLifecycle(ServerPlayer player, boolean preserveLanding) {
        end(player, preserveLanding);
        if (!preserveLanding) finishLanding(player);
        MODES.remove(player.getUUID());
    }

    private static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
