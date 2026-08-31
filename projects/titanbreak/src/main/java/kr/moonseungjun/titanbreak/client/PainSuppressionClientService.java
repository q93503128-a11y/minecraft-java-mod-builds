package kr.moonseungjun.titanbreak.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-only hurt-camera recovery for reflex and pain-suppression neural augments. */
public final class PainSuppressionClientService {
    private PainSuppressionClientService() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int cap = Integer.MAX_VALUE;
        TitanClientState.AugmentMeta reflex = TitanClientState.augmentMeta("reflex_accelerator");
        if (reflex != null && reflex.installed() && reflex.enhancement() >= 7) cap = Math.min(cap, 4);

        TitanClientState.AugmentMeta pain = TitanClientState.augmentMeta("pain_suppressor");
        if (pain != null && pain.installed()) {
            double ratio = mc.player.getHealth() / Math.max(1.0D, mc.player.getMaxHealth());
            double threshold = pain.enhancement() >= 5 ? 0.70D : 0.50D;
            if (ratio <= threshold) {
                cap = Math.min(cap, pain.enhancement() >= 5 ? 5 : 7);
                if (pain.enhancement() >= 10 && ratio <= 0.25D) cap = Math.min(cap, 1);
            }
        }

        if (cap != Integer.MAX_VALUE && mc.player.hurtTime > cap) mc.player.hurtTime = cap;
    }
}
