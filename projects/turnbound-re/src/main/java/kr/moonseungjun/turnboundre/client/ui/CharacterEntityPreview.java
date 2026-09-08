package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-only cache for the selected character's real Minecraft entity preview.
 * The authoritative character->entity id comes from the server progression snapshot.
 */
final class CharacterEntityPreview {
    private ClientLevel cachedLevel;
    private String cachedSourceEntity = "";
    private EntityType<?> cachedType;
    private LivingEntity cachedEntity;

    EntityPreviewLayout.PreviewSpec layout(Minecraft minecraft, String sourceEntity, int regionWidth, int regionHeight) {
        LivingEntity entity = resolve(minecraft, sourceEntity);
        if (entity == null || cachedType == null) return EntityPreviewLayout.PreviewSpec.hidden(regionWidth);
        return EntityPreviewLayout.fit(regionWidth, regionHeight, cachedType.getWidth(), cachedType.getHeight());
    }

    void extract(GuiGraphicsExtractor graphics, UiLayoutMetrics.Rect region, EntityPreviewLayout.PreviewSpec spec) {
        if (!spec.visible() || cachedEntity == null) return;
        int x0 = region.x() + spec.xOffset();
        int y0 = region.y() + spec.yOffset();
        InventoryScreen.renderEntityInInventoryFollowsAngle(
                graphics,
                x0,
                y0,
                x0 + spec.width(),
                y0 + spec.height(),
                spec.renderScale(),
                0.0F,
                0.0F,
                0.35F,
                cachedEntity);
    }

    void clear() {
        cachedLevel = null;
        cachedSourceEntity = "";
        cachedType = null;
        cachedEntity = null;
    }

    private LivingEntity resolve(Minecraft minecraft, String sourceEntity) {
        ClientLevel level = minecraft.level;
        if (level == null || sourceEntity == null || sourceEntity.isBlank()) {
            clear();
            return null;
        }
        if (level == cachedLevel && sourceEntity.equals(cachedSourceEntity)) return cachedEntity;

        clear();
        Identifier id = Identifier.tryParse(sourceEntity);
        if (id == null) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return null;

        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof LivingEntity living)) return null;

        cachedLevel = level;
        cachedSourceEntity = sourceEntity;
        cachedType = type;
        cachedEntity = living;
        return living;
    }
}
