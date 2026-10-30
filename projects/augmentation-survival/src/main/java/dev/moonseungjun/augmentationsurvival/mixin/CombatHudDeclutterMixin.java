package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.client.AugmentIconClient;
import dev.moonseungjun.augmentationsurvival.client.ClientActiveLoadoutState;
import dev.moonseungjun.augmentationsurvival.client.CombatHudClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Declutters the ten-slot active strip: charge pips replace overlapping 1/2 text. */
@Mixin(value = CombatHudClient.class, remap = false)
abstract class CombatHudDeclutterMixin {
    private static final int TEXT = 0xFFF4F7F9;
    private static final int MUTED = 0xFF89939A;
    private static final int READY = 0xFFE7F6FF;
    private static final int SPENT = 0xFF3A444A;

    @Inject(method = "drawSkills", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$drawReadableSkills(
            GuiGraphicsExtractor g,
            Minecraft client,
            int x,
            int y,
            CallbackInfo ci
    ) {
        ci.cancel();
        int slot = 16;
        int gap = 2;
        for (int i = 0; i < 10; i++) {
            int sx = x + i * (slot + gap);
            String id = ClientActiveLoadoutState.slot(i);
            int cd = ClientActiveLoadoutState.cooldownTicks(i);
            int maxCharges = ClientActiveLoadoutState.maxCharges(i);
            int readyCharges = ClientActiveLoadoutState.readyCharges(i);
            int nextRecharge = ClientActiveLoadoutState.nextRechargeTicks(i);
            boolean empty = id.isBlank();

            g.fill(sx, y, sx + slot, y + slot, empty ? 0xA8070A0D : 0xCC101519);
            g.outline(sx, y, slot, slot, empty ? 0x88313A40 : 0xBB6D7B83);
            if (!empty) {
                try {
                    var definition = AugmentCatalog.require(id);
                    g.item(AugmentIconClient.stackFor(id, definition.tier()), sx, y);
                } catch (RuntimeException ignored) {
                    // Keep the slot readable even if presentation metadata is temporarily unavailable.
                }

                boolean unavailable;
                int countdown;
                if (maxCharges > 1) {
                    unavailable = readyCharges <= 0;
                    countdown = nextRecharge;
                    drawChargePips(g, sx, y, slot, Math.max(0, readyCharges), maxCharges);
                } else {
                    unavailable = cd > 0;
                    countdown = cd;
                }
                if (unavailable) {
                    g.fill(sx, y, sx + slot, y + slot, 0x9A000000);
                    if (countdown > 0) {
                        String sec = Integer.toString((countdown + 19) / 20);
                        g.text(client.font, sec,
                                sx + slot / 2 - client.font.width(sec) / 2,
                                y + 4,
                                TEXT,
                                true);
                    }
                }
            }

            // One small key chip only; no charge/cooldown text competes for the same corner.
            String key = i == 9 ? "0" : Integer.toString(i + 1);
            g.fill(sx, y, sx + 7, y + 8, empty ? 0x78000000 : 0xA0000000);
            g.text(client.font, key, sx + 1, y, empty ? 0xFF586168 : MUTED, false);
        }
    }

    private static void drawChargePips(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int slot,
            int ready,
            int max
    ) {
        int shown = Math.min(2, Math.max(1, max));
        int pipW = 4;
        int gap = 1;
        int total = shown * pipW + (shown - 1) * gap;
        int startX = x + slot - total - 1;
        int py = y + slot - 3;
        for (int i = 0; i < shown; i++) {
            int px = startX + i * (pipW + gap);
            g.fill(px, py, px + pipW, py + 2, i < ready ? READY : SPENT);
        }
    }
}
