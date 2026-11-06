package dev.moonseungjun.augmentationsurvival.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Resolves actual mob portraits when a client entity renderer is available. */
public final class EntityIconClient {
    private static ClientLevel cachedLevel;
    private static final Map<String, Entity> PREVIEWS = new HashMap<>();
    private static final Map<String, Item> SPAWN_EGGS = new HashMap<>();

    private EntityIconClient() { }

    public static ItemStack stack(String entityId) {
        return new ItemStack(SPAWN_EGGS.computeIfAbsent(entityId, EntityIconClient::resolveSpawnEgg));
    }

    private static Item resolveSpawnEgg(String entityId) {
        Identifier typeId = Identifier.tryParse(entityId);
        if (typeId == null) return Items.EGG;
        Identifier eggId = Identifier.fromNamespaceAndPath(typeId.getNamespace(), typeId.getPath() + "_spawn_egg");
        return BuiltInRegistries.ITEM.getOptional(eggId).orElse(Items.EGG);
    }

    /** Returns false for non-living or unavailable optional entities so callers can draw an item fallback. */
    public static boolean render(GuiGraphicsExtractor graphics, String entityId, int x, int y, int size) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || size < 12) return false;
        if (cachedLevel != level) {
            PREVIEWS.clear();
            cachedLevel = level;
        }
        Entity preview = PREVIEWS.computeIfAbsent(entityId, id -> createPreview(level, id));
        if (!(preview instanceof LivingEntity living)) return false;
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x, y, x + size, y + size,
                Math.max(16, size - 4), 0.0F, x + size / 2.0F, y + size / 2.0F, living);
        return true;
    }

    private static Entity createPreview(ClientLevel level, String entityId) {
        Identifier key = Identifier.tryParse(entityId);
        if (key == null) return null;
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
        if (type == null) return null;
        try {
            Entity entity = type.create(level, EntitySpawnReason.COMMAND);
            if (entity instanceof LivingEntity living) {
                living.setYRot(180.0F);
                living.setYHeadRot(180.0F);
                living.setYBodyRot(180.0F);
            }
            return entity;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
