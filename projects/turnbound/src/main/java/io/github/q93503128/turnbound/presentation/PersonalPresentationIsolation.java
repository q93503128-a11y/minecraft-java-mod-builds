package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * Presentation ownership and visibility boundary.
 *
 * <p>Actual battle characters are shared-world spectacle and are visible to nearby players. Owner-only helper
 * markers stay private so focus/target UI cannot leak across clients. Explicit private actors remain supported.</p>
 */
@EventBusSubscriber(modid = Turnbound.MOD_ID)
public final class PersonalPresentationIsolation {
    private static final ThreadLocal<UUID> PRESENTATION_OWNER = new ThreadLocal<>();

    private PersonalPresentationIsolation() {}

    public static void withPrivateActorOwner(UUID owner, Runnable action) {
        if (owner == null || action == null) return;
        UUID previous = PRESENTATION_OWNER.get();
        PRESENTATION_OWNER.set(owner);
        try {
            action.run();
        } finally {
            if (previous == null) PRESENTATION_OWNER.remove();
            else PRESENTATION_OWNER.set(previous);
        }
    }

    public static BattleActorEntity spawnPrivateActor(
            ServerLevel level, String combatantId, Vec3 pos, float yaw, UUID owner) {
        if (level == null || owner == null) return null;
        BattleActorEntity actor = TurnboundBattleActors.spawn(level, combatantId, pos, yaw);
        if (actor == null) return null;
        markPrivate(actor, owner);
        hideFromNonOwners(level, actor, owner);
        return actor;
    }

    public static <T extends Entity> T markPrivate(T entity, UUID owner) {
        if (entity == null || owner == null) return entity;
        entity.removeTag(PersonalPresentationActorCatalog.SHARED_BATTLE_TAG);
        entity.addTag(PersonalPresentationActorCatalog.PRIVATE_TAG);
        entity.addTag(PersonalPresentationActorCatalog.ownerTag(owner));
        return entity;
    }

    public static <T extends Entity> T markSharedBattle(T entity, UUID owner) {
        if (entity == null || owner == null) return entity;
        entity.removeTag(PersonalPresentationActorCatalog.PRIVATE_TAG);
        entity.addTag(PersonalPresentationActorCatalog.SHARED_BATTLE_TAG);
        entity.addTag(PersonalPresentationActorCatalog.ownerTag(owner));
        return entity;
    }

    public static UUID owner(Entity entity) {
        if (entity == null) return null;
        boolean owned = entity.entityTags().contains(PersonalPresentationActorCatalog.PRIVATE_TAG)
                || entity.entityTags().contains(PersonalPresentationActorCatalog.SHARED_BATTLE_TAG);
        if (!owned) return null;
        for (String tag : entity.entityTags()) {
            UUID owner = PersonalPresentationActorCatalog.ownerFromTag(tag);
            if (owner != null) return owner;
        }
        return null;
    }

    public static boolean privateToOwner(Entity entity) {
        return entity != null && entity.entityTags().contains(PersonalPresentationActorCatalog.PRIVATE_TAG);
    }

    public static boolean visibleTo(Entity entity, UUID viewer) {
        UUID owner = owner(entity);
        return owner == null || !privateToOwner(entity) || owner.equals(viewer);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (event.loadedFromDisk() && owner(entity) != null) {
            event.setCanceled(true);
            return;
        }

        UUID owner = PRESENTATION_OWNER.get();
        if (owner == null) return;

        if (entity instanceof BattleActorEntity) {
            markSharedBattle(entity, owner);
        } else if (entity instanceof ArmorStand) {
            markPrivate(entity, owner);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) return;
        Entity target = event.getTarget();
        if (visibleTo(target, viewer.getUUID())) return;
        viewer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
    }

    private static void hideFromNonOwners(ServerLevel level, Entity actor, UUID owner) {
        ClientboundRemoveEntitiesPacket remove = new ClientboundRemoveEntitiesPacket(actor.getId());
        for (ServerPlayer viewer : level.players()) {
            if (!owner.equals(viewer.getUUID())) viewer.connection.send(remove);
        }
    }

    public static <T extends ParticleOptions> boolean particles(
            ServerLevel level, ServerPlayer player, T particle,
            double x, double y, double z, int count,
            double xDist, double yDist, double zDist, double speed) {
        if (level == null || player == null || particle == null) return false;
        return level.sendParticles(player, particle, false, false,
                x, y, z, count, xDist, yDist, zDist, speed);
    }

    /**
     * Scoped battle VFX remain owner-only until multiplayer client playtesting establishes safe spectator radii.
     * Animated battle actors themselves are already shared.
     */
    public static <T extends ParticleOptions> boolean particles(
            ServerLevel level, T particle,
            double x, double y, double z, int count,
            double xDist, double yDist, double zDist, double speed) {
        if (level == null || particle == null) return false;
        UUID owner = PRESENTATION_OWNER.get();
        if (owner != null && level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
            if (player == null || player.level() != level) return false;
            return level.sendParticles(player, particle, false, false,
                    x, y, z, count, xDist, yDist, zDist, speed);
        }
        level.sendParticles(particle, x, y, z, count, xDist, yDist, zDist, speed);
        return true;
    }
}
