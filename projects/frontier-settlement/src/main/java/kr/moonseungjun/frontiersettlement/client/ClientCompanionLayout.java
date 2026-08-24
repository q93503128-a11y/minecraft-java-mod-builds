package kr.moonseungjun.frontiersettlement.client;

import net.neoforged.fml.ModList;

/** Keeps Frontier's compact HUD clear of known companion HUD defaults without depending on them. */
public final class ClientCompanionLayout {
    private ClientCompanionLayout() {}

    public static int resourceHudY() {
        return ModList.get().isLoaded("xaerominimap") ? 154 : 8;
    }
}
