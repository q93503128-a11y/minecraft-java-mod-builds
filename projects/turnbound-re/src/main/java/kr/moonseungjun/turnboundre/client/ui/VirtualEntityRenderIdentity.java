package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns client-only synthetic ids to entities that exist only for GUI/stage rendering.
 * Minecraft 26.2 item render-state extraction requires every rendered LivingEntity to already own an id.
 * Negative ids deliberately stay outside the normal positive world-entity id space.
 */
final class VirtualEntityRenderIdentity {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-1);

    private VirtualEntityRenderIdentity() {}

    static <T extends Entity> T assign(T entity) {
        if (entity == null) throw new IllegalArgumentException("entity must not be null");
        entity.setId(nextSyntheticId());
        return entity;
    }

    static int nextSyntheticId() {
        return NEXT_ID.getAndDecrement();
    }
}
