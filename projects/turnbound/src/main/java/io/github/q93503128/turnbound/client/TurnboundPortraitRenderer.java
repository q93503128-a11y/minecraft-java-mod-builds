package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.HeroPortraitPlan;
import io.github.q93503128.turnbound.presentation.SignatureBattleActors;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared live-3D portrait bridge.
 *
 * <p>Preview actors use the exact production GeckoLib entity type/model but are never added to the level.
 * The cache is level-local and is discarded on world change.</p>
 */
public final class TurnboundPortraitRenderer {
    private static final Map<String, BattleActorEntity> PREVIEWS = new HashMap<>();
    private static final AtomicInteger PREVIEW_ENTITY_IDS = new AtomicInteger(-1);
    private static ClientLevel previewLevel;

    private TurnboundPortraitRenderer() {}

    public static boolean extract(GuiGraphicsExtractor graphics, String combatantId,
                                  int x0, int y0, int x1, int y1, boolean unavailable) {
        if (graphics == null || combatantId == null || combatantId.isBlank() || x1 <= x0 || y1 <= y0) return false;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return false;
        if (previewLevel != level) {
            PREVIEWS.clear();
            previewLevel = level;
        }

        String visualId = visualId(combatantId);
        BattleActorEntity actor = PREVIEWS.computeIfAbsent(visualId, id -> createPreview(level, id));
        if (actor == null) {
            PREVIEWS.remove(visualId);
            return false;
        }

        if (minecraft.player != null) {
            actor.setPos(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
        }
        actor.setYRot(0.0F);
        actor.setYHeadRot(0.0F);
        actor.setYBodyRot(0.0F);

        HeroPortraitPlan.Camera camera = HeroPortraitPlan.camera(combatantId);
        int box = Math.max(1, Math.min(x1 - x0, y1 - y0));
        int size = Math.max(8, Math.round(box * 1.42F * camera.scale()));
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics, x0, y0, x1, y1, size, camera.offsetY(), camera.xAngle(), camera.yAngle(), actor);

        if (unavailable) {
            graphics.fill(x0, y0, x1, y1, 0x9A101318);
            int cx = (x0 + x1) / 2;
            int cy = (y0 + y1) / 2;
            graphics.fill(cx - 1, cy - 5, cx + 2, cy + 6, 0xCC707987);
            graphics.fill(cx - 1, cy + 8, cx + 2, cy + 11, 0xCC707987);
        }
        return true;
    }

    private static String visualId(String combatantId) {
        if (!HeroPortraitPlan.coreHero(combatantId)) return combatantId;
        String signature = ClientMetaState.snapshot().equipment().stream()
                .filter(row -> "SIGNATURE".equals(row.slot()) && combatantId.equals(row.equippedCharacterId()))
                .map(ClientMetaState.EquipmentRow::itemId)
                .findFirst().orElse("");
        String planned = HeroPortraitPlan.signatureVisualId(combatantId, signature);
        if (!planned.equals(combatantId) && SignatureBattleActors.contains(planned)) return planned;
        return combatantId;
    }

    private static BattleActorEntity createPreview(ClientLevel level, String visualId) {
        BattleActorEntity preview = null;
        if (SignatureBattleActors.contains(visualId)) preview = SignatureBattleActors.preview(level, visualId);
        else if (TurnboundBattleActors.contains(visualId)) preview = TurnboundBattleActors.preview(level, visualId);
        if (preview == null) return null;

        // Minecraft 26.2 item render-state extraction reads a living entity's network id even for GUI previews.
        // These preview actors never join the level, so assign a client-local negative id explicitly.
        preview.setId(PREVIEW_ENTITY_IDS.getAndDecrement());
        return preview;
    }

    public static void clear() {
        PREVIEWS.clear();
        previewLevel = null;
    }
}
