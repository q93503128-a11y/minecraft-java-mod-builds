package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.content.ContentRuntimeSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Process-local server bridge for authenticated player weapon intents.
 *
 * <p>Content reloads replace the whole capability by generation, which also drops stale executions.
 * The current M3 slice intentionally has no approved production hit-volume geometry yet, so the
 * runtime advances the authoritative attack clock but exposes no hit candidates until that separate
 * evidence gate is completed. This is not damage authority and cannot be used to claim combat is
 * presentation/field complete.</p>
 */
public final class PlayerWeaponServerRuntime {
    private static State state;

    private PlayerWeaponServerRuntime() {}

    public static IntentResult handleMoveIntent(ServerPlayer player, ContentId moveId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(moveId, "moveId");
        State current = currentState();
        try {
            current.adapter.beginMove(player, moveId, player.level().getGameTime());
            return IntentResult.ACCEPTED;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return IntentResult.REJECTED;
        }
    }

    /** Event-driven per-player tick; no global player/entity scan is performed. */
    public static MinecraftPlayerWeaponCombatAdapter.TickResult tickPlayer(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        State current = currentState();
        if (!current.adapter.hasSession(player.getUUID())) return MinecraftPlayerWeaponCombatAdapter.TickResult.idle();
        return current.adapter.tick((ServerLevel) player.level(), player, player.level().getGameTime());
    }

    /** Exact-instance cleanup for logout, dimension handoff, clone retirement and entity leave. */
    public static boolean clearPlayer(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        State current = state;
        return current != null && current.adapter.clearActor(player);
    }

    /**
     * UUID-wide identity fence for reconnect/server boundaries. This intentionally clears any stale
     * predecessor instance before the newly authenticated player can establish combat authority.
     */
    public static boolean clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        State current = state;
        return current != null && current.adapter.clearActor(playerId);
    }

    /** Entity lifetime edge; exact-instance cleanup makes duplicate/delayed leave events harmless. */
    public static void entityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clearPlayer(player);
    }

    /** No process-local player combat capability may survive the authoritative server lifetime. */
    public static void serverStopped(ServerStoppedEvent event) {
        clearAllForContentBoundary();
    }

    public static synchronized void clearAllForContentBoundary() {
        state = null;
    }

    private static synchronized State currentState() {
        ContentRuntimeSnapshot snapshot = ContentRuntime.requireCurrent();
        if (state != null && state.generation == snapshot.generation()) return state;

        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(snapshot);
        PlayerWeaponItemStackLoadoutResolver loadoutResolver = new PlayerWeaponItemStackLoadoutResolver(catalog);
        MinecraftPlayerWeaponCombatAdapter adapter = new MinecraftPlayerWeaponCombatAdapter(
            catalog,
            loadoutResolver,
            (level, actor, attack) -> List.of()
        );
        state = new State(snapshot.generation(), adapter);
        return state;
    }

    public enum IntentResult {
        ACCEPTED,
        REJECTED
    }

    private record State(long generation, MinecraftPlayerWeaponCombatAdapter adapter) {}
}
