package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces legacy field encounter markers with the same authored actor assets used in battle.
 * The ArmorStand remains as the server-side encounter anchor so chapter progression code does not fork.
 */
@EventBusSubscriber(modid = Turnbound.MOD_ID)
public final class FieldEncounterPresentationBridge {
    private static final double SCAN_RADIUS = 176.0;
    private static final double CLEANUP_RADIUS_SQ = 208.0 * 208.0;
    private static final Map<UUID, VisualLink> VISUALS = new LinkedHashMap<>();

    private FieldEncounterPresentationBridge() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        clearAll(event.getServer().getPlayerList().getPlayers());
    }

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        AABB scan = player.getBoundingBox().inflate(SCAN_RADIUS);
        for (ArmorStand marker : level.getEntitiesOfClass(ArmorStand.class, scan)) {
            String actorId = actorIdFor(marker);
            if (actorId == null || !TurnboundBattleActors.contains(actorId)) continue;
            marker.setInvisible(true);
            marker.setCustomNameVisible(false);
            ensureVisual(level, player, marker, actorId);
        }
        cleanupNearbyOrphans(level, player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            for (VisualLink link : List.copyOf(VISUALS.values())) {
                Entity visual = level.getEntity(link.visualId());
                if (visual != null) visual.discard();
            }
        }
        VISUALS.clear();
    }

    private static void ensureVisual(ServerLevel level, ServerPlayer player, ArmorStand marker, String actorId) {
        VisualLink link = VISUALS.get(marker.getUUID());
        BattleActorEntity actor = null;
        if (link != null && actorId.equals(link.actorId())) {
            Entity existing = level.getEntity(link.visualId());
            if (existing instanceof BattleActorEntity battleActor) actor = battleActor;
        }
        if (actor == null) {
            if (link != null) {
                Entity old = level.getEntity(link.visualId());
                if (old != null) old.discard();
            }
            actor = TurnboundBattleActors.spawn(level, actorId, marker.position(), marker.getYRot());
            if (actor == null) return;
            actor.setFieldWalking(false);
            VISUALS.put(marker.getUUID(), new VisualLink(actor.getUUID(), actorId));
        }

        Vec3 pos = marker.position();
        actor.setPos(pos.x, pos.y, pos.z);
        Vec3 towardPlayer = player.position().subtract(pos);
        if (towardPlayer.lengthSqr() > 0.001 && towardPlayer.lengthSqr() < 24.0 * 24.0) {
            float yaw = (float) Math.toDegrees(Math.atan2(-towardPlayer.x, towardPlayer.z));
            actor.setYRot(yaw);
            actor.setYHeadRot(yaw);
            actor.setYBodyRot(yaw);
        }
        actor.setCustomName(marker.getCustomName());
        actor.setCustomNameVisible(true);
        actor.setFieldWalking(false);
    }

    private static void cleanupNearbyOrphans(ServerLevel level, ServerPlayer player) {
        for (var entry : List.copyOf(VISUALS.entrySet())) {
            Entity marker = level.getEntity(entry.getKey());
            Entity visual = level.getEntity(entry.getValue().visualId());
            if (marker != null) continue;
            if (visual == null) {
                VISUALS.remove(entry.getKey());
                continue;
            }
            if (player.position().distanceToSqr(visual.position()) <= CLEANUP_RADIUS_SQ) {
                visual.discard();
                VISUALS.remove(entry.getKey());
            }
        }
    }

    private static String actorIdFor(ArmorStand marker) {
        if (marker.getCustomName() == null) return null;
        String label = marker.getCustomName().getString();
        for (var encounter : CampaignEncounterCatalog.all()) {
            if (!encounter.label().equals(label) || encounter.enemies().isEmpty()) continue;
            return encounter.enemies().getFirst();
        }
        return null;
    }

    private record VisualLink(UUID visualId, String actorId) {}
}
