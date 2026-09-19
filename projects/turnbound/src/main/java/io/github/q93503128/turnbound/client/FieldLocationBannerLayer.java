package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

/**
 * Brief exploration-first location title.
 *
 * <p>Uses the existing Kenney-backed TURNBOUND skin and only reacts to server-promoted, survey-verified locations.
 * It never invents client coordinates and never opens a screen or steals input.</p>
 */
public final class FieldLocationBannerLayer implements GuiLayer {
    private static final long DISPLAY_MILLIS = 2600L;
    private static String lastLocationId = "";
    private static String visibleTitle = "";
    private static long visibleUntilMillis;

    @Override
    public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) return;
        if (ClientBattleState.snapshot().active()) return;

        FieldUiSnapshot snapshot = ClientFieldState.snapshot();
        update(snapshot, System.currentTimeMillis());
        if (visibleTitle.isBlank() || System.currentTimeMillis() >= visibleUntilMillis) return;

        int maxWidth = Math.max(120, Math.min(260, graphics.guiWidth() - 24));
        String title = UiTextLayout.fit(visibleTitle, maxWidth - 28);
        int width = Math.min(maxWidth, Math.max(128, minecraft.font.width(title) + 28));
        int height = 26;
        int x = (graphics.guiWidth() - width) / 2;
        int y = 31;

        TurnboundUiSkin.inset(graphics, x, y, width, height);
        int tx = x + (width - minecraft.font.width(title)) / 2;
        graphics.text(minecraft.font, Component.literal(title), tx, y + 9, TurnboundUiTokens.TEXT_PRIMARY, true);
    }

    static void update(FieldUiSnapshot snapshot, long nowMillis) {
        if (snapshot == null || !snapshot.active()) {
            lastLocationId = "";
            visibleTitle = "";
            visibleUntilMillis = 0L;
            return;
        }

        String locationId = snapshot.locationId();
        if (locationId == null || locationId.isBlank()) {
            lastLocationId = "";
            return;
        }
        if (!locationId.equals(lastLocationId)) {
            lastLocationId = locationId;
            visibleTitle = snapshot.locationTitle();
            visibleUntilMillis = visibleTitle.isBlank() ? 0L : nowMillis + DISPLAY_MILLIS;
        }
    }
}
