package kr.moonseungjun.turnboundre.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * TURNBOUND worlds do not use live vanilla Mob entities as gameplay actors.
 * Vanilla entity ids remain catalog/archetype metadata for CharacterDefinitions and client previews only.
 */
public final class VanillaMobWorldPolicy {
    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus required");
        bus.addListener(this::onEntityJoinLevel);
    }

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob)) return;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        if (id != null && "minecraft".equals(id.getNamespace())) event.setCanceled(true);
    }
}
