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
 * Keeps player-specific presentation out of other clients while leaving shared world actors untouched.
 *
 * <p>A presentation scope owns authored battle actors plus the temporary ArmorStand markers/fallbacks created inside
 * it. NeoForge then retracts those entities from every non-owner client whenever tracking starts. The same scope routes
 * battle particles only to that owner, so two battles may overlap in the same authored arena without leaking actors,
 * markers or VFX into each other.</p>
 */
@EventBusSubscriber(modid = Turnbound.MOD_ID)
public final class PersonalPresentationIsolation {
    private static final ThreadLocal<UUID> PRESENTATION_OWNER = new ThreadLocal<>();

    private PersonalPresentationIsolation() {}

    /** Runs one synchronous player-private presentation block. Nested scopes restore the previous owner. */
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

    /** Convenience for callers that must spawn a private actor outside a scoped presentation block. */
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

    private static boolean privatePresentationType(Entity entity) {
        return entity instanceof BattleActorEntity || entity instanceof ArmorStand;
    }

    /** EntityJoinLevelEvent occurs during addFreshEntity, before normal client tracking/pairing begins. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        // Every tagged private presentation entity is runtime-only. A crash must never resurrect actors or markers.
        if (event.loadedFromDisk() && owner(entity) != null) {
            event.setCanceled(true);
            return;
        }
        UUID owner = PRESENTATION_OWNER.get();
        if (owner != null && privatePresentationType(entity)) markPrivate(entity, owner);
    }

    /** StartTracking is non-cancellable, so retract the just-paired entity from every non-owner client. */
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

    /** Sends a presentation particle packet only to the explicitly intended player. */
    public static <T extends ParticleOptions> boolean particles(
            ServerLevel level, ServerPlayer player, T particle,
            double x, double y, double z, int count,
            double xDist, double yDist, double zDist, double speed) {
        if (level == null || player == null || particle == null) return false;
        return level.sendParticles(player, particle, false, false,
                x, y, z, count, xDist, yDist, zDist, speed);
    }

    /**
     * Battle-VFX dispatcher. Inside a private presentation scope the packet goes only to its owner; outside a scope
     * it preserves the ordinary shared-world broadcast behavior used by genuinely shared ambient presentation.
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
