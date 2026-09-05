package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Routes the persistent Aster March shell between Radia and all authored campaign/content sessions. */
public final class WorldSessionRouter {
    private static final Set<UUID> RELAY_APPROACH = new LinkedHashSet<>();

    private WorldSessionRouter() {}

    public static boolean active(ServerPlayer p) {
        return RELAY_APPROACH.contains(p.getUUID())
                || RadiaHubSessionManager.active(p)
                || FieldSessionManager.active(p)
                || GloamwoodSessionManager.active(p)
                || BrokenAqueductSessionManager.active(p)
                || EmberQuarrySessionManager.active(p)
                || OldRelayStationSessionManager.active(p);
    }

    public static void enterInitial(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        AsterMarchWorldShell.build(level);
        AsterMarchContentOrchestrator.build(level);
        // Legacy per-player sync remains as a compatibility input; SavedData is the final physical-world authority.
        AsterMarchWorldShell.syncProgressionGates(level, p.getUUID());
        AsterMarchSharedWorldProgress.sync(level, p.getUUID());
        RadiaHubSessionManager.enter(p);
    }

    public static void tick(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        AsterMarchWorldShell.build(level);
        AsterMarchContentOrchestrator.build(level);
        AsterMarchWorldShell.syncProgressionGates(level, p.getUUID());
        AsterMarchSharedWorldProgress.sync(level, p.getUUID());
        AsterMarchContentOrchestrator.tick(level, p);

        if (RELAY_APPROACH.contains(p.getUUID())) {
            tickRelayApproach(level, p);
            return;
        }

        if (!BattleSessionManager.exists(p) && transitionAtWorldSeam(level, p)) return;

        if (RadiaHubSessionManager.active(p)) RadiaHubSessionManager.tick(p);
        else if (GloamwoodSessionManager.active(p)) GloamwoodSessionManager.tick(p);
        else if (BrokenAqueductSessionManager.active(p)) BrokenAqueductSessionManager.tick(p);
        else if (EmberQuarrySessionManager.active(p)) EmberQuarrySessionManager.tick(p);
        else if (OldRelayStationSessionManager.active(p)) OldRelayStationSessionManager.tick(p);
        else FieldSessionManager.tick(p);
    }

    private static boolean transitionAtWorldSeam(ServerLevel level, ServerPlayer p) {
        UUID id = p.getUUID();
        Vec3 pos = p.position();

        if (RadiaHubSessionManager.active(p)) {
            if (CampaignContentUnlocks.prologueComplete(id) && pos.z >= 118.0 && Math.abs(pos.x) <= 12.0) {
                RadiaHubSessionManager.remove(p);
                FieldSessionManager.enter(p);
                return true;
            }
            if (AsterMarchSharedWorldProgress.regionOpen(level, TurnboundWorldSavedData.REGION_GLOAMWOOD)
                    && pos.z <= -108.0 && Math.abs(pos.x) <= 12.0) {
                RadiaHubSessionManager.remove(p);
                GloamwoodSessionManager.enter(p);
                return true;
            }
            if (AsterMarchSharedWorldProgress.regionOpen(level, TurnboundWorldSavedData.REGION_BROKEN_AQUEDUCT)
                    && pos.x <= -124.0 && Math.abs(pos.z - 20.0) <= 12.0) {
                RadiaHubSessionManager.remove(p);
                BrokenAqueductSessionManager.enter(p);
                return true;
            }
            if (AsterMarchSharedWorldProgress.regionOpen(level, TurnboundWorldSavedData.REGION_OLD_RELAY_APPROACH)
                    && pos.x >= 124.0 && Math.abs(pos.z + 80.0) <= 13.0) {
                beginRelayApproach(p);
                return true;
            }
            return false;
        }

        if (FieldSessionManager.active(p)) {
            if (pos.z <= 119.0 && Math.abs(pos.x) <= 12.0) {
                FieldSessionManager.remove(p);
                RadiaHubSessionManager.enter(p);
                p.setPos(0.5, 68.0, 106.5);
                p.setYRot(180.0F);
                return true;
            }
            if (AsterMarchSharedWorldProgress.regionOpen(level, TurnboundWorldSavedData.REGION_EMBER_QUARRY)
                    && AsterMarchWorldShell.nearGate(pos, AsterMarchWorldShell.Gate.QUARRY_PASS, 11.0)) {
                FieldSessionManager.remove(p);
                EmberQuarrySessionManager.enter(p);
                return true;
            }
            return false;
        }

        if (GloamwoodSessionManager.active(p) && pos.z >= -120.0 && Math.abs(pos.x) <= 16.0) {
            GloamwoodSessionManager.remove(p);
            RadiaHubSessionManager.enter(p);
            p.setPos(0.5, 66.0, -104.5);
            p.setYRot(0.0F);
            return true;
        }

        if (BrokenAqueductSessionManager.active(p) && pos.x >= -130.0 && Math.abs(pos.z - 20.0) <= 14.0) {
            BrokenAqueductSessionManager.remove(p);
            RadiaHubSessionManager.enter(p);
            p.setPos(-121.5, 66.0, 20.5);
            p.setYRot(-90.0F);
            return true;
        }

        if (EmberQuarrySessionManager.active(p)
                && AsterMarchWorldShell.nearGate(pos, AsterMarchWorldShell.Gate.QUARRY_PASS, 12.0)) {
            EmberQuarrySessionManager.remove(p);
            FieldSessionManager.enter(p);
            p.setPos(-52.5, 68.0, 296.5);
            p.setYRot(180.0F);
            return true;
        }
        return false;
    }

    private static void beginRelayApproach(ServerPlayer p) {
        RadiaHubSessionManager.remove(p);
        RELAY_APPROACH.add(p.getUUID());
        p.setPos(140.5, 66.0, -86.5);
        p.setYRot(-45.0F);
        p.setXRot(3.0F);
        p.setDeltaMovement(Vec3.ZERO);
        p.sendSystemMessage(Component.literal("TURNBOUND · 복원된 동쪽 Relay 접근로"));
    }

    private static void tickRelayApproach(ServerLevel level, ServerPlayer p) {
        if (BattleSessionManager.exists(p)) return;
        Vec3 pos = p.position();
        if (p.tickCount % 20 == 0) {
            AABB area = new AABB(116, 48, -208, 286, 104, -54);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) mob.discard();
        }

        if (pos.x < 130.0 && Math.abs(pos.z + 80.0) <= 20.0) {
            RELAY_APPROACH.remove(p.getUUID());
            RadiaHubSessionManager.enter(p);
            p.setPos(119.5, 66.0, -80.5);
            p.setYRot(90.0F);
            return;
        }
        if (pos.distanceToSqr(new Vec3(270.0, 68.0, -185.0)) <= 13.0 * 13.0) {
            RELAY_APPROACH.remove(p.getUUID());
            OldRelayStationSessionManager.enter(p);
            return;
        }
        if (!AsterMarchWorldShell.relayTransitContains(pos)) {
            p.setPos(140.5, 66.0, -86.5);
            p.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static boolean interactEntity(ServerPlayer p, Entity e) {
        if (AsterMarchContentOrchestrator.interact(p, e)) return true;
        if (RELAY_APPROACH.contains(p.getUUID())) return false;
        if (RadiaHubSessionManager.active(p)) return RadiaHubSessionManager.interactEntity(p, e);
        if (GloamwoodSessionManager.active(p)) return GloamwoodSessionManager.interactEntity(p, e);
        if (BrokenAqueductSessionManager.active(p)) return BrokenAqueductSessionManager.interactEntity(p, e);
        if (EmberQuarrySessionManager.active(p)) return EmberQuarrySessionManager.interactEntity(p, e);
        if (OldRelayStationSessionManager.active(p)) return OldRelayStationSessionManager.interactEntity(p, e);
        return FieldSessionManager.interactEntity(p, e);
    }

    public static void command(ServerPlayer p, String c) {
        if (handleCanonicalFastTravel(p, c)) return;
        if (RELAY_APPROACH.contains(p.getUUID())) return;
        if (RadiaHubSessionManager.active(p)) { RadiaHubSessionManager.command(p, c); return; }
        if (GloamwoodSessionManager.active(p)) { GloamwoodSessionManager.command(p, c); return; }
        if (BrokenAqueductSessionManager.active(p)) { BrokenAqueductSessionManager.command(p, c); return; }
        if (EmberQuarrySessionManager.active(p)) { EmberQuarrySessionManager.command(p, c); return; }
        if (OldRelayStationSessionManager.active(p)) { OldRelayStationSessionManager.command(p, c); return; }
        FieldSessionManager.command(p, c);
    }

    private static boolean handleCanonicalFastTravel(ServerPlayer p, String command) {
        if (command == null) return false;
        String[] parts = command.split("\\|", -1);
        if (parts.length != 2 || !"TRAVEL".equals(parts[0])
                || !AsterMarchFastTravelService.canonicalDestination(parts[1])) return false;

        String destinationId = parts[1];
        if (BattleSessionManager.exists(p)) return true;
        if (!AsterMarchFastTravelService.unlocked(p, destinationId)) {
            p.sendSystemMessage(Component.literal(AsterMarchFastTravelService.lockedReason(destinationId))
                    .withStyle(ChatFormatting.GRAY));
            return true;
        }

        leaveTravelSession(p);
        boolean entered = switch (destinationId) {
            case AsterMarchRegionCatalog.FT_RADIA -> RadiaHubSessionManager.enter(p);
            case AsterMarchRegionCatalog.FT_MEADOW -> FieldSessionManager.enter(p);
            case AsterMarchRegionCatalog.FT_GLOAM -> GloamwoodSessionManager.enter(p);
            case AsterMarchRegionCatalog.FT_AQUEDUCT -> BrokenAqueductSessionManager.enter(p);
            case AsterMarchRegionCatalog.FT_QUARRY -> EmberQuarrySessionManager.enter(p);
            case AsterMarchRegionCatalog.FT_RELAY -> OldRelayStationSessionManager.enter(p);
            default -> false;
        };
        if (!entered) {
            p.sendSystemMessage(Component.literal("계전소 이동을 시작할 수 없습니다.").withStyle(ChatFormatting.RED));
            return true;
        }

        FieldTravelCatalog.Destination destination = FieldTravelCatalog.destination(destinationId);
        p.setPos(destination.x(), destination.y(), destination.z());
        p.setYRot(destination.yaw());
        p.setXRot(3.0F);
        p.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    private static void leaveTravelSession(ServerPlayer p) {
        RELAY_APPROACH.remove(p.getUUID());
        if (RadiaHubSessionManager.active(p)) RadiaHubSessionManager.remove(p);
        else if (FieldSessionManager.active(p)) FieldSessionManager.remove(p);
        else if (GloamwoodSessionManager.active(p)) GloamwoodSessionManager.remove(p);
        else if (BrokenAqueductSessionManager.active(p)) BrokenAqueductSessionManager.remove(p);
        else if (EmberQuarrySessionManager.active(p)) EmberQuarrySessionManager.remove(p);
        else if (OldRelayStationSessionManager.active(p)) OldRelayStationSessionManager.remove(p);
    }

    public static void onBattleEnded(ServerPlayer p, String id, BattleOutcome o) {
        if (RadiaHubSessionManager.active(p)) RadiaHubSessionManager.onBattleEnded(p, id, o);
        else if (GloamwoodSessionManager.active(p)) GloamwoodSessionManager.onBattleEnded(p, id, o);
        else if (BrokenAqueductSessionManager.active(p)) BrokenAqueductSessionManager.onBattleEnded(p, id, o);
        else if (EmberQuarrySessionManager.active(p)) EmberQuarrySessionManager.onBattleEnded(p, id, o);
        else if (OldRelayStationSessionManager.active(p)) OldRelayStationSessionManager.onBattleEnded(p, id, o);
        else FieldSessionManager.onBattleEnded(p, id, o);
    }

    public static void remove(ServerPlayer p) {
        RELAY_APPROACH.remove(p.getUUID());
        AsterMarchContentOrchestrator.remove(p);
        AsterMarchWorldShell.forget(p.getUUID());
        RadiaHubSessionManager.remove(p);
        GloamwoodSessionManager.remove(p);
        BrokenAqueductSessionManager.remove(p);
        EmberQuarrySessionManager.remove(p);
        OldRelayStationSessionManager.remove(p);
        FieldSessionManager.remove(p);
    }

    public static void clearAll(Iterable<ServerPlayer> ps) {
        for (ServerPlayer p : ps) remove(p);
        RELAY_APPROACH.clear();
    }
}
