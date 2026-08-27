package kr.moonseungjun.titanbreak.combat;

import net.minecraft.server.level.ServerPlayer;

/**
 * Reserved for heavy-frame / reinforced-skeleton augmentation builds.
 * Reflex Drive never grants terrain breaching by itself.
 */
public final class BreachService {
    private BreachService() {}

    public static boolean canBreach(ServerPlayer player) {
        return false;
    }
}
