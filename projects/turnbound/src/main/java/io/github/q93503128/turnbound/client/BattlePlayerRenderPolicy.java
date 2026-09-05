package io.github.q93503128.turnbound.client;

import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Keeps ordinary player shells out of the local private battle composition without changing server visibility.
 *
 * <p>During a TURNBOUND battle the client only needs the authored party/enemy presentation actors. Suppressing all
 * vanilla player renders on that one client prevents the local third-person shell and nearby multiplayer avatars from
 * crossing the battle frame, while those players remain fully visible to every client that is not in this battle.</p>
 */
public final class BattlePlayerRenderPolicy {
    private BattlePlayerRenderPolicy() {}

    public static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        if (ClientBattleState.snapshot().active()) event.setCanceled(true);
    }
}
