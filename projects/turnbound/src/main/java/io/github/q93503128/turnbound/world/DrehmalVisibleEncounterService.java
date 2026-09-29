package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import io.github.q93503128.turnbound.session.MultiplayerPartyService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared visible-enemy runtime for the surveyed Drehmal route.
 *
 * <p>This service is deliberately dormant while route entries remain unverified. Once survey data is promoted,
 * one physical actor group is shared by all players; the server owns encounter claims and starts either the normal
 * solo session or one authoritative nearby-party shared battle through an authored camera-safe arena candidate.</p>
 */
final class DrehmalVisibleEncounterService {
    private static final String COMMON_TAG = "turnbound_drehmal_field_enemy";
    private static final String ENCOUNTER_TAG_PREFIX = "turnbound_drehmal_encounter:";
    private static final String SLOT_TAG_PREFIX = "turnbound_drehmal_slot:";
    private static final double MATERIALIZE_RADIUS = 72.0D;
    /** Party members farther than the encounter leash remain in field state; nobody is teleported in from afar. */
    private static final double SHARED_PARTICIPANT_RADIUS = FieldEncounterRules.HOME_LEASH_RADIUS;
    private static final Map<String, SharedEncounter> ENCOUNTERS = new LinkedHashMap<>();

    private static ServerLevel boundLevel;
    private static long lastTick = Long.MIN_VALUE;

    private DrehmalVisibleEncounterService() {}

    static void tick(ServerPlayer caller) {
        if (caller == null || !(caller.level() instanceof ServerLevel level)) return;
        bind(level);
        long gameTime = level.getGameTime();
        if (lastTick == gameTime) return;
        lastTick = gameTime;

        syncCatalog(level, caller);
        for (SharedEncounter encounter : List.copyOf(ENCOUNTERS.values())) encounter.tick(level);
    }

    static boolean onBattleEnded(ServerPlayer player, String combatEncounterId, BattleOutcome outcome) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return false;
        return onBattleEnded(level, player.getUUID(), combatEncounterId, outcome);
    }

    static boolean onBattleEnded(ServerLevel level, UUID claimantId, String combatEncounterId, BattleOutcome outcome) {
        if (level == null || claimantId == null || combatEncounterId == null || combatEncounterId.isBlank()) return false;
        DrehmalFirstRouteCatalog.EncounterSlot slot = DrehmalFirstRouteCatalog.encounterByCombatId(combatEncounterId);
        if (slot == null) return false;

        SharedEncounter runtime = ENCOUNTERS.get(slot.locator());
        if (runtime != null) runtime.resolve(claimantId, level.getGameTime(), outcome);
        return true;
    }

    static void onPlayerRemoved(ServerPlayer player) {
        if (player == null) return;
        for (SharedEncounter encounter : ENCOUNTERS.values()) {
            if (player.getUUID().equals(encounter.claimedBy) && !BattleSessionManager.exists(player.getUUID())) {
                encounter.releaseAbandoned();
            }
        }
    }

    static void clear() {
        if (boundLevel != null) {
            for (SharedEncounter encounter : ENCOUNTERS.values()) encounter.discardActors(boundLevel);
        }
        ENCOUNTERS.clear();
        boundLevel = null;
        lastTick = Long.MIN_VALUE;
    }

    static boolean fastTravelBlocked(ServerPlayer player) {
        if (player == null || boundLevel == null || player.level() != boundLevel) return false;
        double radiusSq = FieldEncounterRules.DISENGAGE_RADIUS * FieldEncounterRules.DISENGAGE_RADIUS;
        for (SharedEncounter encounter : ENCOUNTERS.values()) {
            if (encounter.claimedBy != null || encounter.phase != FieldEncounterRules.Phase.ALERT) continue;
            if (encounter.pivot.distanceToSqr(player.position()) <= radiusSq) return true;
        }
        return false;
    }

    private static void bind(ServerLevel level) {
        if (boundLevel == level) return;
        clear();
        boundLevel = level;
    }

    private static void syncCatalog(ServerLevel level, ServerPlayer resolverPlayer) {
        Set<String> active = new HashSet<>();
        TurnboundWorldSavedData worldState = TurnboundWorldSavedData.get(level.getServer());
        for (DrehmalFirstRouteCatalog.EncounterSlot slot : DrehmalAdaptiveRoutePlacement.productionEncounters(resolverPlayer)) {
            if ("WORLD_BOSS".equals(slot.tier()) && worldState.encounterCleared(slot.combatEncounterId())) continue;
            DrehmalFirstRouteCatalog.Site site = DrehmalAdaptiveRoutePlacement.site(resolverPlayer, slot.siteLocator());
            DrehmalFirstRouteCatalog.Footprint footprint = DrehmalAdaptiveRoutePlacement.footprint(resolverPlayer, slot.footprintLocator());
            DrehmalFirstRouteCatalog.Patrol patrol = slot.patrolLocator().isBlank()
                    ? null
                    : DrehmalAdaptiveRoutePlacement.patrol(resolverPlayer, slot.patrolLocator());
            if (!DrehmalEncounterActivationRules.ready(slot, site, footprint, patrol)) continue;
            active.add(slot.locator());
            ENCOUNTERS.computeIfAbsent(slot.locator(), ignored -> new SharedEncounter(level, slot, site, footprint, patrol));
        }

        for (String locator : List.copyOf(ENCOUNTERS.keySet())) {
            if (active.contains(locator)) continue;
            SharedEncounter removed = ENCOUNTERS.remove(locator);
            if (removed != null) removed.discardActors(level);
        }
    }

    private static final class SharedEncounter {
        private final ServerLevel level;
        private final DrehmalFirstRouteCatalog.EncounterSlot slot;
        private final DrehmalFirstRouteCatalog.Site site;
        private final DrehmalFirstRouteCatalog.Footprint footprint;
        private final DrehmalFirstRouteCatalog.Patrol patrol;
        private final V04Catalogs.Encounter spec;
        private final DrehmalFieldEncounterPolicy.Policy fieldPolicy;
        private final List<UUID> actors = new ArrayList<>();
        private final List<Vec3> patrolPoints;
        private final List<FieldRoamPlanner.Point> roamPoints;

        private Vec3 pivot;
        private Vec3 facing = new Vec3(0.0D, 0.0D, -1.0D);
        private Vec3 returnTarget;
        private Vec3 lastSafePivot;
        private int patrolIndex;
        private int patrolDwellTicks;
        private long roamSequence;
        private long lastMoveCommandTick = FieldNavigationRules.NEVER;
        private Vec3 lastMoveTarget;
        private long navigationStalledSinceTick = FieldNavigationRules.NEVER;
        private int graceTicks = 40;
        private int alertPreludeTicks;
        private long availableAt;
        private UUID claimedBy;
        private FieldEncounterRules.Phase phase = FieldEncounterRules.Phase.PATROL;
        private boolean blockedArenaWarned;
        private boolean missingVisualWarned;

        private SharedEncounter(
                ServerLevel level,
                DrehmalFirstRouteCatalog.EncounterSlot slot,
                DrehmalFirstRouteCatalog.Site site,
                DrehmalFirstRouteCatalog.Footprint footprint,
                DrehmalFirstRouteCatalog.Patrol patrol
        ) {
            this.level = level;
            this.slot = slot;
            this.site = site;
            this.footprint = footprint;
            this.patrol = patrol;
            this.spec = CampaignEncounterCatalog.spec(slot.combatEncounterId());
            this.fieldPolicy = DrehmalFieldEncounterPolicy.forEncounter(slot, site);
            this.pivot = vec(site.runtimePosition());
            this.returnTarget = pivot;
            this.lastSafePivot = DrehmalFirstRouteRuntime.insideSafetyZone(level, pivot.x, pivot.z) ? null : pivot;
            this.patrolPoints = patrol == null ? List.of() : patrol.points().stream().map(DrehmalVisibleEncounterService::vec).toList();
            this.roamPoints = patrolPoints.stream().map(point -> new FieldRoamPlanner.Point(point.x, point.z)).toList();
            this.roamSequence = slot.locator().hashCode();
        }

        private void tick(ServerLevel level) {
            if (claimedBy != null) {
                if (!BattleSessionManager.exists(claimedBy)) releaseAbandoned();
                return;
            }

            if (level.getGameTime() < availableAt) {
                discardActors(level);
                return;
            }

            List<ServerPlayer> observers = observers(level);
            if (observers.isEmpty()) {
                discardActors(level);
                resetToRoute();
                return;
            }

            if (!ensureActors(level)) return;
            Entity lead = lead(level);
            if (lead == null) return;

            pivot = lead.position();
            if (DrehmalFirstRouteRuntime.insideSafetyZone(level, pivot.x, pivot.z)) {
                if (lastSafePivot != null) {
                    boolean crossedWhilePatrolling = phase == FieldEncounterRules.Phase.PATROL;
                    stopLeadNavigation(level);
                    if (crossedWhilePatrolling) advancePatrolPoint();
                    returnTarget = lastSafePivot;
                    phase = FieldEncounterRules.Phase.RETURN;
                }
            } else {
                lastSafePivot = pivot;
            }

            ServerPlayer nearest = nearestThreat(observers);
            if (graceTicks > 0) graceTicks--;
            Vec3 flatPlayer = nearest == null ? pivot : new Vec3(nearest.getX(), pivot.y, nearest.getZ());
            double playerDistance = nearest == null
                    ? FieldEncounterRules.DISENGAGE_RADIUS + 1.0D
                    : flatPlayer.distanceTo(pivot);
            // A patrol may be much longer than the combat leash. The return anchor is the exact place where this
            // group was patrolling when aggro began, not the first point of the whole route.
            if (phase == FieldEncounterRules.Phase.PATROL) returnTarget = pivot;
            boolean sight = nearest != null && (playerDistance <= 3.0D || nearest.hasLineOfSight(lead));
            double sensedDistance = nearest == null
                    ? FieldEncounterRules.DISENGAGE_RADIUS + 1.0D
                    : sight ? playerDistance : FieldEncounterRules.ALERT_RADIUS + 1.0D;
            double returnDistance = pivot.distanceTo(returnTarget);

            FieldEncounterRules.Phase previous = phase;
            phase = FieldEncounterRules.nextPhase(phase, sensedDistance, returnDistance, graceTicks);
            if (previous == FieldEncounterRules.Phase.PATROL && phase == FieldEncounterRules.Phase.ALERT) {
                returnTarget = pivot;
                alertPreludeTicks = fieldPolicy.alertPreludeTicks();
            } else if (previous == FieldEncounterRules.Phase.RETURN && phase == FieldEncounterRules.Phase.PATROL) {
                pivot = returnTarget;
                graceTicks = Math.max(graceTicks, FieldEncounterRules.RETURN_REAGGRO_GRACE_TICKS);
                alertPreludeTicks = 0;
            } else if (previous == FieldEncounterRules.Phase.ALERT && phase != FieldEncounterRules.Phase.ALERT) {
                alertPreludeTicks = 0;
            }

            if (nearest != null && sight && FieldEncounterRules.shouldEngage(
                    phase, playerDistance, Math.max(graceTicks, alertPreludeTicks))) {
                if (startBattle(level, nearest)) return;
                graceTicks = 40;
                alertPreludeTicks = 0;
                phase = FieldEncounterRules.Phase.RETURN;
            }

            boolean alertPrelude = phase == FieldEncounterRules.Phase.ALERT && alertPreludeTicks > 0;
            boolean patrolPaused = phase == FieldEncounterRules.Phase.PATROL && patrolDwellTicks > 0;
            if (patrolPaused) patrolDwellTicks--;

            Vec3 target = switch (phase) {
                case ALERT -> alertPrelude ? pivot : flatPlayer;
                case RETURN -> returnTarget;
                case PATROL -> patrolPaused ? pivot : patrolTarget();
            };
            double speed = switch (phase) {
                case ALERT -> alertPrelude ? 0.0D : 0.095D;
                case RETURN -> 0.075D;
                case PATROL -> !patrolPaused && patrolPoints.size() >= 2 ? 0.035D : 0.0D;
            };

            Vec3 delta = target.subtract(pivot);
            boolean walking = false;
            if (phase == FieldEncounterRules.Phase.PATROL && !patrolPaused
                    && patrolPoints.size() >= 2 && delta.lengthSqr() < 0.64D) {
                stopLeadNavigation(level);
                advancePatrolPoint();
                patrolDwellTicks = patrol == null ? 0 : FieldRoamPlanner.dwellTicks(
                        patrol.dwellMinTicks(), patrol.dwellMaxTicks(), ++roamSequence);
            } else if (delta.lengthSqr() > 0.01D && speed > 0.0D) {
                walking = driveLeadNavigation(level, target, speed);
            } else {
                stopLeadNavigation(level);
            }
            updateActors(level, walking);
            if (phase == FieldEncounterRules.Phase.ALERT && alertPreludeTicks > 0) alertPreludeTicks--;
        }

        private List<ServerPlayer> observers(ServerLevel level) {
            List<ServerPlayer> out = new ArrayList<>();
            double radiusSq = MATERIALIZE_RADIUS * MATERIALIZE_RADIUS;
            Vec3 center = vec(site.runtimePosition());
            for (ServerPlayer player : level.players()) {
                if (!ExternalWorldBootstrap.active(player) || BattleSessionManager.exists(player) || player.isSpectator()) continue;
                if (player.position().distanceToSqr(center) > radiusSq) continue;
                out.add(player);
            }
            return out;
        }

        private ServerPlayer nearestThreat(List<ServerPlayer> players) {
            ServerPlayer best = null;
            double distance = Double.MAX_VALUE;
            for (ServerPlayer player : players) {
                if (DrehmalFirstRouteRuntime.insideSafetyZone(player)) continue;
                double candidate = player.position().distanceToSqr(pivot);
                if (candidate < distance) {
                    distance = candidate;
                    best = player;
                }
            }
            return best;
        }

        private boolean ensureActors(ServerLevel level) {
            var sitePosition=site.runtimePosition();
            if(sitePosition==null||!DrehmalAdaptiveRoutePlacement.sourceContentClear(
                    level,sitePosition.x(),sitePosition.y(),sitePosition.z(),3.5D)){
                discardActors(level);
                return false;
            }
            if (actorsAlive(level)) return true;
            discardActors(level);
            if (adoptTagged(level)) {
                updateActors(level, false);
                return true;
            }

            List<String> fieldIds = fieldEnemyIds();
            for (String defId : fieldIds) {
                if (TurnboundBattleActors.contains(defId)) continue;
                if (!missingVisualWarned) {
                    Turnbound.LOGGER.error("TURNBOUND refused to materialize {} because {} has no production actor",
                            slot.locator(), defId);
                    missingVisualWarned = true;
                }
                return false;
            }

            for (int i = 0; i < fieldIds.size(); i++) {
                String defId = fieldIds.get(i);
                Vec3 pos = formation(i, facing);
                BattleActorEntity actor = TurnboundBattleActors.spawn(level, defId, pos, yawFor(facing));
                if (actor == null) {
                    discardActors(level);
                    return false;
                }
                actor.setPersistenceRequired();
                actor.setInvulnerable(true);
                actor.setCustomName(Component.literal(CanonicalData.definition(defId, spec.level(), 0, false).name()));
                actor.setCustomNameVisible(false);
                actor.setFieldWalking(false);
                actor.addTag(COMMON_TAG);
                actor.addTag(ENCOUNTER_TAG_PREFIX + slot.locator());
                actor.addTag(SLOT_TAG_PREFIX + i);
                actors.add(actor.getUUID());
            }
            updateActors(level, false);
            return true;
        }

        private boolean adoptTagged(ServerLevel level) {
            Vec3 center = vec(site.runtimePosition());
            AABB area = new AABB(center.x - 80.0D, center.y - 32.0D, center.z - 80.0D,
                    center.x + 80.0D, center.y + 32.0D, center.z + 80.0D);
            Map<Integer, BattleActorEntity> bySlot = new HashMap<>();
            String encounterTag = ENCOUNTER_TAG_PREFIX + slot.locator();
            for (BattleActorEntity actor : level.getEntitiesOfClass(BattleActorEntity.class, area)) {
                if (!actor.entityTags().contains(COMMON_TAG) || !actor.entityTags().contains(encounterTag)) continue;
                int index = slot(actor);
                if (index < 0 || bySlot.putIfAbsent(index, actor) != null) actor.discard();
            }
            List<String> fieldIds = fieldEnemyIds();
            if (bySlot.size() != fieldIds.size()) {
                for (BattleActorEntity actor : bySlot.values()) actor.discard();
                return false;
            }

            actors.clear();
            for (int i = 0; i < fieldIds.size(); i++) {
                BattleActorEntity actor = bySlot.get(i);
                if (actor == null || !TurnboundBattleActors.contains(fieldIds.get(i))) {
                    for (BattleActorEntity adopted : bySlot.values()) adopted.discard();
                    actors.clear();
                    return false;
                }
                actors.add(actor.getUUID());
            }
            Entity lead = level.getEntity(actors.getFirst());
            if (lead != null) pivot = lead.position();
            return true;
        }

        private boolean startBattle(ServerLevel level, ServerPlayer player) {
            List<ServerPlayer> participants = sharedParticipants(level, player);
            for (DrehmalFirstRouteCatalog.ArenaCandidate candidate : footprint.candidates()) {
                var arenaPosition=candidate.center();
                if(!DrehmalAdaptiveRoutePlacement.sourceContentClear(
                        level,arenaPosition.x(),arenaPosition.y(),arenaPosition.z(),5.5D))continue;
                Vec3 center = vec(arenaPosition);
                boolean started = participants.size() > 1
                        ? BattleSessionManager.startSharedEncounterAt(
                                participants, player.getUUID(), slot.combatEncounterId(), false, false,
                                center, candidate.yaw())
                        : BattleSessionManager.startEncounterAt(
                                player, slot.combatEncounterId(), false, false, center, candidate.yaw());
                if (!started) continue;
                claimedBy = player.getUUID();
                discardActors(level);
                blockedArenaWarned = false;
                return true;
            }

            if (!blockedArenaWarned) {
                Turnbound.LOGGER.warn("TURNBOUND encounter {} has no currently open surveyed battle footprint",
                        slot.locator());
                blockedArenaWarned = true;
            }
            return false;
        }

        private List<ServerPlayer> sharedParticipants(ServerLevel level, ServerPlayer trigger) {
            List<ServerPlayer> out = new ArrayList<>();
            double radiusSq = SHARED_PARTICIPANT_RADIUS * SHARED_PARTICIPANT_RADIUS;
            MultiplayerPartyService.Snapshot party = MultiplayerPartyService.snapshot(trigger.getUUID());
            for (UUID memberId : party.members()) {
                ServerPlayer member = level.getServer().getPlayerList().getPlayer(memberId);
                if (member == null || member.level() != level || member.isSpectator()) continue;
                if (!ExternalWorldBootstrap.active(member) || BattleSessionManager.exists(member)) continue;
                if (member.position().distanceToSqr(trigger.position()) > radiusSq) continue;
                out.add(member);
            }
            if (!out.contains(trigger)) out.add(0, trigger);
            return List.copyOf(out);
        }

        private void resolve(UUID claimantId, long gameTime, BattleOutcome outcome) {
            if (claimantId == null) return;
            if (claimedBy != null && !claimedBy.equals(claimantId)) return;
            claimedBy = null;
            availableAt = gameTime
                    + DrehmalEncounterActivationRules.respawnTicks(spec.respawnSeconds(), outcome);
            blockedArenaWarned = false;
            resetToRoute();
        }

        private void releaseAbandoned() {
            claimedBy = null;
            if (boundLevel != null) availableAt = Math.max(availableAt, boundLevel.getGameTime() + 40L);
            resetToRoute();
        }

        private Vec3 patrolTarget() {
            if (patrolPoints.size() < 2) return pivot;
            patrolIndex = Math.floorMod(patrolIndex, patrolPoints.size());
            return patrolPoints.get(patrolIndex);
        }

        private void advancePatrolPoint() {
            if (patrolPoints.size() < 2) return;
            if (patrol != null && "ROAM".equals(patrol.mode())) {
                patrolIndex = FieldRoamPlanner.nextIndex(
                        roamPoints, patrolIndex, ++roamSequence, FieldRoamPlanner.DEFAULT_MIN_NEXT_DISTANCE_SQ);
            } else {
                patrolIndex = (patrolIndex + 1) % patrolPoints.size();
            }
        }

        private List<String> fieldEnemyIds() {
            int count = Math.max(1, Math.min(slot.fieldVisibleCount(), spec.enemies().size()));
            return spec.enemies().subList(0, count);
        }

        private void updateActors(ServerLevel level, boolean walking) {
            float yaw = yawFor(facing);
            for (int i = 0; i < actors.size(); i++) {
                Entity raw = level.getEntity(actors.get(i));
                if (!(raw instanceof BattleActorEntity actor)) continue;
                if (i > 0) {
                    Vec3 pos = formation(i, facing);
                    actor.setPos(pos.x, pos.y, pos.z);
                    actor.setYRot(yaw);
                    actor.setYHeadRot(yaw);
                    actor.setYBodyRot(yaw);
                }
                actor.setFieldWalking(walking);
                if (i == 0 && phase == FieldEncounterRules.Phase.ALERT && alertPreludeTicks > 0) {
                    actor.setCustomName(Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    actor.setCustomNameVisible(true);
                } else {
                    actor.setCustomName(Component.literal(CanonicalData.definition(
                            fieldEnemyIds().get(i), spec.level(), 0, false).name()));
                    if (TurnboundBattleActors.fieldThreatTier(actor.getType()) < 2) actor.setCustomNameVisible(false);
                }
            }
        }

        private Vec3 formation(int index, Vec3 forward) {
            if (index == 0) return pivot;
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            int row = (index + 1) / 2;
            double side = index % 2 == 0 ? 1.0D : -1.0D;
            return pivot.subtract(forward.scale(1.15D * row)).add(right.scale(1.15D * side));
        }

        private boolean driveLeadNavigation(ServerLevel level, Vec3 target, double speedModifier) {
            Entity raw = lead(level);
            if (!(raw instanceof BattleActorEntity actor)) return false;

            Vec3 before = pivot;
            pivot = actor.position();
            Vec3 actualMovement = pivot.subtract(before);
            boolean madeProgress = actualMovement.horizontalDistanceSqr() > 0.0004D;
            if (madeProgress) {
                facing = horizontalDirection(actualMovement, facing);
            }

            double targetShiftSq = lastMoveTarget == null
                    ? Double.POSITIVE_INFINITY
                    : target.distanceToSqr(lastMoveTarget);
            long now = level.getGameTime();
            boolean navigationDone = actor.fieldNavigationDone();
            double targetDistanceSq = pivot.distanceToSqr(target);

            if (madeProgress || targetShiftSq > 0.25D || targetDistanceSq <= FieldNavigationRules.ARRIVAL_DISTANCE_SQ) {
                navigationStalledSinceTick = FieldNavigationRules.NEVER;
            } else if (navigationDone && navigationStalledSinceTick == FieldNavigationRules.NEVER) {
                navigationStalledSinceTick = now;
            }

            if (FieldNavigationRules.shouldSkipBlockedPatrolTarget(
                    phase, now, navigationStalledSinceTick, navigationDone, targetDistanceSq)) {
                stopLeadNavigation(level);
                advancePatrolPoint();
                patrolDwellTicks = patrol == null ? 0 : FieldRoamPlanner.dwellTicks(
                        patrol.dwellMinTicks(), patrol.dwellMaxTicks(), ++roamSequence);
                return false;
            }

            if (FieldNavigationRules.shouldIssue(
                    phase, now, lastMoveCommandTick, targetShiftSq, navigationDone)) {
                lastMoveCommandTick = now;
                lastMoveTarget = target;
                boolean accepted = actor.moveFieldTo(target.x, target.y, target.z, speedModifier);
                if (!accepted && navigationStalledSinceTick == FieldNavigationRules.NEVER) {
                    navigationStalledSinceTick = now;
                }
            }
            return !actor.fieldNavigationDone();
        }

        private void stopLeadNavigation(ServerLevel level) {
            Entity raw = lead(level);
            if (raw instanceof BattleActorEntity actor) {
                pivot = actor.position();
                actor.stopFieldNavigation();
            }
            lastMoveTarget = null;
            lastMoveCommandTick = FieldNavigationRules.NEVER;
            navigationStalledSinceTick = FieldNavigationRules.NEVER;
        }

        private boolean actorsAlive(ServerLevel level) {
            if (actors.size() != fieldEnemyIds().size()) return false;
            for (UUID id : actors) if (!(level.getEntity(id) instanceof BattleActorEntity)) return false;
            return true;
        }

        private Entity lead(ServerLevel level) {
            return actors.isEmpty() ? null : level.getEntity(actors.getFirst());
        }

        private int slot(Entity entity) {
            for (String tag : entity.entityTags()) {
                if (!tag.startsWith(SLOT_TAG_PREFIX)) continue;
                try {
                    return Integer.parseInt(tag.substring(SLOT_TAG_PREFIX.length()));
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
            return -1;
        }

        private void discardActors(ServerLevel level) {
            for (UUID id : actors) {
                Entity entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
            actors.clear();
        }

        private void resetToRoute() {
            pivot = vec(site.runtimePosition());
            returnTarget = pivot;
            lastSafePivot = DrehmalFirstRouteRuntime.insideSafetyZone(level, pivot.x, pivot.z) ? null : pivot;
            patrolIndex = 0;
            patrolDwellTicks = patrol == null ? 0 : FieldRoamPlanner.dwellTicks(
                    patrol.dwellMinTicks(), patrol.dwellMaxTicks(), ++roamSequence);
            lastMoveTarget = null;
            lastMoveCommandTick = FieldNavigationRules.NEVER;
            navigationStalledSinceTick = FieldNavigationRules.NEVER;
            facing = patrolPoints.size() >= 2
                    ? horizontalDirection(patrolPoints.get(1).subtract(patrolPoints.get(0)), new Vec3(0, 0, -1))
                    : new Vec3(0.0D, 0.0D, -1.0D);
            graceTicks = 40;
            alertPreludeTicks = 0;
            phase = FieldEncounterRules.Phase.PATROL;
        }
    }

    private static DrehmalFirstRouteCatalog.Site requiredSite(String locator) {
        DrehmalFirstRouteCatalog.Site site = DrehmalFirstRouteCatalog.site(locator);
        if (site == null) throw new IllegalStateException("Missing production Drehmal site " + locator);
        return site;
    }

    private static DrehmalFirstRouteCatalog.Footprint requiredFootprint(String locator) {
        DrehmalFirstRouteCatalog.Footprint footprint = DrehmalFirstRouteCatalog.footprint(locator);
        if (footprint == null) throw new IllegalStateException("Missing production Drehmal footprint " + locator);
        return footprint;
    }

    private static DrehmalFirstRouteCatalog.Patrol requiredPatrol(String locator) {
        DrehmalFirstRouteCatalog.Patrol patrol = DrehmalFirstRouteCatalog.patrol(locator);
        if (patrol == null) throw new IllegalStateException("Missing production Drehmal patrol " + locator);
        return patrol;
    }

    private static Vec3 vec(DrehmalFirstRouteCatalog.Position position) {
        return new Vec3(position.x() + 0.5D, position.y(), position.z() + 0.5D);
    }

    private static Vec3 horizontalDirection(Vec3 candidate, Vec3 fallback) {
        Vec3 flat = new Vec3(candidate.x, 0.0D, candidate.z);
        return flat.lengthSqr() > 0.000001D ? flat.normalize() : fallback;
    }

    private static float yawFor(Vec3 forward) {
        return (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }
}
