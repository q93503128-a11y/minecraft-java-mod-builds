package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * Keeps player-specific presentation out of other clients while leaving shared world/combat actors untouched.
 *
 * <p>Private actors are still authoritative server entities so existing animation/timing code can stay unchanged.
 * NeoForge fires StartTracking after vanilla pairing data is sent; non-owners immediately receive a remove packet,
 * and the same filter runs again whenever tracking restarts after a chunk/range transition.</p>
 */
@EventBusSubscriber(modid = Turnbound.MOD_ID)
public final class PersonalPresentationIsolation {
    private PersonalPresentationIsolation() {}

    /**
     * Spawns an ordinary authored battle actor, then immediately marks it as owner-only and retracts the pairing from
     * every already-present non-owner. Future tracking starts are filtered by {@link #onStartTracking}.
     */
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
        entity.addTag(PersonalPresentationActorCatalog.COMMON_TAG);
        entity.addTag(PersonalPresentationActorCatalog.ownerTag(owner));
        return entity;
    }

    public static UUID owner(Entity entity) {
        if (entity == null || !entity.entityTags().contains(PersonalPresentationActorCatalog.COMMON_TAG)) return null;
        for (String tag : entity.entityTags()) {
            UUID owner = PersonalPresentationActorCatalog.ownerFromTag(tag);
            if (owner != null) return owner;
        }
        return null;
    }

    public static boolean visibleTo(Entity entity, UUID viewer) {
        UUID owner = owner(entity);
        return owner == null || owner.equals(viewer);
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

    /** Sends a presentation particle packet only to the intended player. */
    public static <T extends ParticleOptions> boolean particles(
            ServerLevel level, ServerPlayer player, T particle,
            double x, double y, double z, int count,
            double xDist, double yDist, double zDist, double speed) {
        if (level == null || player == null || particle == null) return false;
        return level.sendParticles(player, particle, false, false,
                x, y, z, count, xDist, yDist, zDist, speed);
    }
}
