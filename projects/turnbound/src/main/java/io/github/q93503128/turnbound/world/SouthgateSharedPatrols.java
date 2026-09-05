package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-shared moving encounter silhouettes for Chapter 1 Southgate.
 *
 * Campaign progression remains per player. This service advances each physical patrol once per server tick, picks
 * the nearest eligible player for alert/engage behavior, and keeps a single actor group regardless of how many
 * players have an active Southgate session.
 */
final class SouthgateSharedPatrols {
    private static final String COMMON_TAG = "turnbound_southgate_shared_enemy";
    private static final String ENCOUNTER_TAG_PREFIX = "turnbound_southgate_encounter:";
    private static final String SLOT_TAG_PREFIX = "turnbound_southgate_slot:";
    private static final Map<String, SharedPatrol> PATROLS = new LinkedHashMap<>();
    private static ServerLevel boundLevel;
    private static long lastTick = Long.MIN_VALUE;
    private static boolean legacyCleaned;

    private SouthgateSharedPatrols() {}

    static void ensure(ServerLevel level, StarterSliceWorld.BuiltSlice slice, SouthgateChapterWorld.BuiltChapter chapter) {
        bind(level);
        putIfAbsent("ENC_M01", slice.m01Home(), slice.m01End(), null, 0.0F);
        putIfAbsent("ENC_M02", slice.m02Home(), slice.m02End(), null, 0.0F);
        putIfAbsent("ENC_M03", chapter.m03Home(), chapter.m03End(), null, 0.0F);
        putIfAbsent("ENC_M04", chapter.m04Home(), chapter.m04End(), chapter.m04BattleAnchor(), 180.0F);
        putIfAbsent("ENC_M05", chapter.m05Home(), chapter.m05End(), chapter.m05BattleAnchor(), 180.0F);
        putIfAbsent("BATTLE_B01", chapter.bossApproach(), chapter.bossApproach(), chapter.bossAnchor(), chapter.bossYaw());
        cleanupLegacyCopies(level);
        refresh(level);
    }

    static void tick(ServerLevel level) {
        bind(level);
        long gameTime = level.getGameTime();
        if (lastTick == gameTime) return;
        lastTick = gameTime;
        for (SharedPatrol patrol : PATROLS.values()) patrol.tick(level);
    }

    static void refresh(ServerLevel level) {
        bind(level);
        lastTick = Long.MIN_VALUE;
        tick(level);
    }

    static void onPlayerSessionRemoved(ServerLevel level) {
        refresh(level);
    }

    private static void putIfAbsent(String encounterId, Vec3 home, Vec3 end, Vec3 battleAnchor, float battleYaw) {
        PATROLS.putIfAbsent(encounterId, new SharedPatrol(encounterId, home, end, battleAnchor, battleYaw));
    }

    private static void bind(ServerLevel level) {
        if (boundLevel == level) return;
        PATROLS.clear();
        boundLevel = level;
        lastTick = Long.MIN_VALUE;
        legacyCleaned = false;
    }

    private static List<ServerPlayer> demanders(ServerLevel level, String encounterId) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!FieldSessionManager.active(player) || BattleSessionManager.exists(player)) continue;
            Set<String> clears = StarterFieldProgress.project(CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters());
            if (clears.contains(encounterId) || !SouthgateEncounterVisibilityRules.unlocked(encounterId, clears)) continue;
            result.add(player);
        }
        return result;
    }

    private static void cleanupLegacyCopies(ServerLevel level) {
        if (legacyCleaned) return;
        for (ServerPlayer player : level.players()) if (BattleSessionManager.exists(player)) return;

        Set<String> names = new java.util.HashSet<>();
        for (SharedPatrol patrol : PATROLS.values()) {
            for (String defId : patrol.spec.enemies()) {
                names.add(CanonicalData.definition(defId, patrol.spec.level(), 0, false).name());
            }
        }
        AABB area = new AABB(AsterMarchRegionCatalog.SOUTHGATE.minX() - 6, 50,
                AsterMarchRegionCatalog.SOUTHGATE.minZ() - 6,
                AsterMarchRegionCatalog.SOUTHGATE.maxX() + 6, 100,
                AsterMarchRegionCatalog.SOUTHGATE.maxZ() + 6);
        for (BattleActorEntity entity : level.getEntitiesOfClass(BattleActorEntity.class, area)) {
            if (!entity.entityTags().contains(COMMON_TAG) && entity.getCustomName() != null
                    && names.contains(entity.getCustomName().getString())) entity.discard();
        }
        for (ArmorStand entity : level.getEntitiesOfClass(ArmorStand.class, area)) {
            if (!entity.entityTags().contains(COMMON_TAG) && entity.getCustomName() != null
                    && names.contains(entity.getCustomName().getString())) entity.discard();
        }
        legacyCleaned = true;
    }

    private static final class SharedPatrol {
        private final String encounterId;
        private final V04Catalogs.Encounter spec;
        private final Vec3 home;
        private final Vec3 patrolEnd;
        private final Vec3 fixedBattleAnchor;
        private final float fixedBattleYaw;
        private final List<UUID> actors = new ArrayList<>();
        private Vec3 pivot;
        private Vec3 facing = new Vec3(0, 0, -1);
        private boolean towardEnd = true;
        private int graceTicks = 40;
        private FieldEncounterRules.Phase phase = FieldEncounterRules.Phase.PATROL;

        private SharedPatrol(String encounterId, Vec3 home, Vec3 patrolEnd, Vec3 fixedBattleAnchor, float fixedBattleYaw) {
            this.encounterId = encounterId;
            this.spec = CampaignEncounterCatalog.spec(encounterId);
            this.home = home;
            this.patrolEnd = patrolEnd;
            this.fixedBattleAnchor = fixedBattleAnchor;
            this.fixedBattleYaw = fixedBattleYaw;
            this.pivot = home;
        }

        private void tick(ServerLevel level) {
            List<ServerPlayer> players = demanders(level, encounterId);
            if (players.isEmpty()) {
                discardNow(level);
                resetMotion();
                return;
            }

            ensureActors(level);
            ServerPlayer nearest = nearest(players);
            if (nearest == null) return;

            if (graceTicks > 0) graceTicks--;
            Vec3 playerFlat = new Vec3(nearest.getX(), pivot.y, nearest.getZ());
            double playerDistance = playerFlat.distanceTo(pivot);
            double homeDistance = pivot.distanceTo(home);
            FieldEncounterRules.Phase previous = phase;
            phase = FieldEncounterRules.nextPhase(phase, playerDistance, homeDistance, graceTicks);
            if (previous == FieldEncounterRules.Phase.RETURN && phase == FieldEncounterRules.Phase.PATROL) {
                pivot = home;
                towardEnd = true;
                graceTicks = Math.max(graceTicks, FieldEncounterRules.RETURN_REAGGRO_GRACE_TICKS);
            }

            if (FieldEncounterRules.shouldEngage(phase, playerDistance, graceTicks)) {
                boolean started;
                if (fixedBattleAnchor != null) {
                    started = BattleSessionManager.startEncounterAt(nearest, encounterId, false, false, fixedBattleAnchor, fixedBattleYaw);
                } else {
                    BattleSessionManager.startEncounter(nearest, encounterId, false, false);
                    started = BattleSessionManager.exists(nearest);
                }
                if (started) {
                    graceTicks = 40;
                    phase = FieldEncounterRules.Phase.RETURN;
                    if (demanders(level, encounterId).isEmpty()) {
                        discardNow(level);
                        resetMotion();
                    }
                    return;
                }
                graceTicks = 40;
                phase = FieldEncounterRules.Phase.RETURN;
            }

            Vec3 target = switch (phase) {
                case ALERT -> playerFlat;
                case RETURN -> home;
                case PATROL -> towardEnd ? patrolEnd : home;
            };
            double speed = switch (phase) {
                case ALERT -> 0.105;
                case RETURN -> 0.075;
                case PATROL -> spec.boss() ? 0.0 : 0.035;
            };
            Vec3 delta = target.subtract(pivot);
            boolean walking = false;
            if (phase == FieldEncounterRules.Phase.PATROL && delta.lengthSqr() < 0.36) {
                towardEnd = !towardEnd;
            } else if (delta.lengthSqr() > 0.0001 && speed > 0.0) {
                Vec3 movement = delta.normalize().scale(Math.min(speed, delta.length()));
                pivot = pivot.add(movement);
                facing = horizontalDirection(movement, facing);
                walking = true;
            }
            updateActors(level, walking);
        }

        private ServerPlayer nearest(List<ServerPlayer> players) {
            ServerPlayer best = null;
            double bestDistance = Double.MAX_VALUE;
            for (ServerPlayer player : players) {
                double distance = player.position().distanceToSqr(pivot);
                if (distance < bestDistance) {
                    best = player;
                    bestDistance = distance;
                }
            }
            return best;
        }

        private void ensureActors(ServerLevel level) {
            if (actorsAlive(level)) return;
            discardNow(level);
            if (adoptTagged(level)) {
                updateActors(level, false);
                return;
            }
            for (int i = 0; i < spec.enemies().size(); i++) {
                Vec3 pos = formation(i, facing);
                Entity actor = spawnActor(level, pos, spec.enemies().get(i));
                actor.addTag(COMMON_TAG);
                actor.addTag(ENCOUNTER_TAG_PREFIX + encounterId);
                actor.addTag(SLOT_TAG_PREFIX + i);
                actors.add(actor.getUUID());
            }
            updateActors(level, false);
        }

        private boolean adoptTagged(ServerLevel level) {
            AABB area = new AABB(AsterMarchRegionCatalog.SOUTHGATE.minX() - 6, 50,
                    AsterMarchRegionCatalog.SOUTHGATE.minZ() - 6,
                    AsterMarchRegionCatalog.SOUTHGATE.maxX() + 6, 100,
                    AsterMarchRegionCatalog.SOUTHGATE.maxZ() + 6);
            Map<Integer, Entity> bySlot = new HashMap<>();
            collect(level.getEntitiesOfClass(BattleActorEntity.class, area), bySlot);
            collect(level.getEntitiesOfClass(ArmorStand.class, area), bySlot);
            if (bySlot.size() != spec.enemies().size()) {
                for (Entity entity : bySlot.values()) entity.discard();
                return false;
            }
            actors.clear();
            for (int i = 0; i < spec.enemies().size(); i++) {
                Entity entity = bySlot.get(i);
                if (entity == null) {
                    for (UUID id : actors) {
                        Entity previous = level.getEntity(id);
                        if (previous != null) previous.discard();
                    }
                    actors.clear();
                    return false;
                }
                actors.add(entity.getUUID());
            }
            Entity lead = level.getEntity(actors.getFirst());
            if (lead != null) pivot = lead.position();
            return true;
        }

        private <T extends Entity> void collect(List<T> entities, Map<Integer, Entity> bySlot) {
            String encounterTag = ENCOUNTER_TAG_PREFIX + encounterId;
            for (Entity entity : entities) {
                if (!entity.entityTags().contains(COMMON_TAG) || !entity.entityTags().contains(encounterTag)) continue;
                int slot = slot(entity);
                if (slot < 0 || bySlot.putIfAbsent(slot, entity) != null) entity.discard();
            }
        }

        private int slot(Entity entity) {
            for (String tag : entity.entityTags()) {
                if (!tag.startsWith(SLOT_TAG_PREFIX)) continue;
                try { return Integer.parseInt(tag.substring(SLOT_TAG_PREFIX.length())); }
                catch (NumberFormatException ignored) { return -1; }
            }
            return -1;
        }

        private Entity spawnActor(ServerLevel level, Vec3 pos, String defId) {
            float yaw = yawFor(facing);
            BattleActorEntity animated = TurnboundBattleActors.spawn(level, defId, pos, yaw);
            if (animated != null) {
                animated.setCustomName(Component.literal(CanonicalData.definition(defId, spec.level(), 0, false).name()));
                animated.setCustomNameVisible(false);
                animated.setFieldWalking(false);
                return animated;
            }

            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(CanonicalData.definition(defId, spec.level(), 0, false).name()));
            stand.setCustomNameVisible(false);
            stand.setItemSlot(EquipmentSlot.CHEST, spec.boss() ? Items.DIAMOND_CHESTPLATE.getDefaultInstance() : Items.IRON_CHESTPLATE.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS.getDefaultInstance());
            if ("E002".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
            else if ("E005".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.GOLDEN_HOE.getDefaultInstance());
            else if ("B01".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_AXE.getDefaultInstance());
            else if (!"E003".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
            level.addFreshEntity(stand);
            return stand;
        }

        private void updateActors(ServerLevel level, boolean walking) {
            float targetYaw = yawFor(facing);
            for (int i = 0; i < actors.size(); i++) {
                Entity entity = level.getEntity(actors.get(i));
                if (entity == null) continue;
                Vec3 position = formation(i, facing);
                entity.setPos(position.x, position.y, position.z);
                float yaw = smoothYaw(entity.getYRot(), targetYaw, 0.35F);
                entity.setYRot(yaw);
                if (entity instanceof BattleActorEntity animated) {
                    animated.setYHeadRot(yaw);
                    animated.setYBodyRot(yaw);
                    animated.setFieldWalking(walking);
                } else if (entity instanceof ArmorStand stand) {
                    stand.setYHeadRot(yaw);
                }
                entity.setCustomNameVisible(i == 0 && phase == FieldEncounterRules.Phase.ALERT);
                if (i == 0 && phase == FieldEncounterRules.Phase.ALERT) {
                    entity.setCustomName(Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                } else {
                    entity.setCustomName(Component.literal(CanonicalData.definition(spec.enemies().get(i), spec.level(), 0, false).name()));
                }
            }
        }

        private Vec3 formation(int index, Vec3 forward) {
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            return index == 0 ? pivot : pivot.subtract(forward.scale(1.2)).add(right.scale(index % 2 == 0 ? 1.25 : -1.25));
        }

        private boolean actorsAlive(ServerLevel level) {
            if (actors.size() != spec.enemies().size()) return false;
            for (UUID id : actors) if (level.getEntity(id) == null) return false;
            return true;
        }

        private void discardNow(ServerLevel level) {
            for (UUID id : actors) {
                Entity entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
            actors.clear();
        }

        private void resetMotion() {
            pivot = home;
            facing = new Vec3(0, 0, -1);
            towardEnd = true;
            graceTicks = 40;
            phase = FieldEncounterRules.Phase.PATROL;
        }
    }

    private static Vec3 horizontalDirection(Vec3 candidate, Vec3 fallback) {
        Vec3 flat = new Vec3(candidate.x, 0, candidate.z);
        return flat.lengthSqr() > 0.000001 ? flat.normalize() : fallback;
    }

    private static float yawFor(Vec3 forward) {
        return (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    private static float smoothYaw(float current, float target, float factor) {
        return current + wrapDegrees(target - current) * factor;
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) wrapped -= 360.0F;
        if (wrapped < -180.0F) wrapped += 360.0F;
        return wrapped;
    }
}
