package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persistent classed mercenaries backed by world SavedData rather than removed entity tag APIs. */
public final class VillageMercenarySystem {
    public static final int MAX_LEVEL = 100;
    private static final String LEGACY_MERCENARY_NAME = "마을 용병";
    private static final Map<UUID, MercenaryClass> CLASSES = new LinkedHashMap<>();
    private static final Map<UUID, Integer> LEVELS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> KILLS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> STRIKER_TRACKED_TARGETS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> STRIKER_OPENING_TARGETS = new LinkedHashMap<>();
    private static final Map<UUID, Long> NEXT_WARD_PULSE = new LinkedHashMap<>();
    private static final Map<UUID, Long> NEXT_ARTILLERY_CAST = new LinkedHashMap<>();
    private static VillageMercenaryData savedData;
    private static VillageMercenarySnapshotData snapshotData;
    private static final List<MercenarySnapshot> NIGHT_SNAPSHOT = new ArrayList<>();
    private static int tickCounter;

    private VillageMercenarySystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenaryData.TYPE);
        snapshotData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenarySnapshotData.TYPE);
        VillageMercenaryPresentationSystem.reset();
        CLASSES.clear();
        LEVELS.clear();
        KILLS.clear();
        STRIKER_TRACKED_TARGETS.clear();
        STRIKER_OPENING_TARGETS.clear();
        NEXT_WARD_PULSE.clear();
        NEXT_ARTILLERY_CAST.clear();
        savedData.classes().forEach((key, value) -> parseUuid(key, uuid -> {
            MercenaryClass kind = MercenaryClass.fromId(value);
            if (kind != null) CLASSES.put(uuid, kind);
        }));
        savedData.levels().forEach((key, value) -> parseUuid(key,
                uuid -> LEVELS.put(uuid, Math.max(1, Math.min(MAX_LEVEL, value)))));
        savedData.kills().forEach((key, value) -> parseUuid(key,
                uuid -> KILLS.put(uuid, Math.max(0, value))));
        sanitize();
        persist();
        loadNightSnapshot();
        tickCounter = 0;
    }

    public static void reset() {
        tickCounter = 0;
        STRIKER_TRACKED_TARGETS.clear();
        STRIKER_OPENING_TARGETS.clear();
        NEXT_WARD_PULSE.clear();
        NEXT_ARTILLERY_CAST.clear();
    }

    public static synchronized boolean recognize(Mob mob) {
        if (!(mob instanceof IronGolem golem) || !CLASSES.containsKey(mob.getUUID())) return false;
        mob.setPersistenceRequired();
        mob.setInvisible(true);
        VillageWorldSystem.markAllowedGameMob(mob);
        applyClassPassives(golem, mercenaryClass(golem), rank(golem));
        refreshName(mob);
        return true;
    }

    /** One-time migration for generic pre-class mercenaries created by the retired barracks path. */
    public static synchronized boolean adoptLegacy(Mob mob) {
        if (!(mob instanceof IronGolem golem)) return false;
        if (CLASSES.containsKey(mob.getUUID())) return recognize(mob);
        Component name = mob.getCustomName();
        if (name == null || !LEGACY_MERCENARY_NAME.equals(name.getString())) return false;
        UUID uuid = mob.getUUID();
        MercenaryClass kind = MercenaryClass.BASTION;
        CLASSES.put(uuid, kind);
        LEVELS.put(uuid, 1);
        KILLS.put(uuid, 0);
        mob.setPersistenceRequired();
        mob.setInvisible(true);
        VillageWorldSystem.markAllowedGameMob(mob);
        applyClassPassives(golem, kind, 1);
        mob.setHealth(mob.getMaxHealth());
        refreshName(mob);
        persist();
        return true;
    }

    public static int hireCost(MercenaryClass kind) {
        if (kind == null) return 0;
        int barracks = Math.max(0, VillageProgressionSystem.barracksLevel());
        int base = 150 + kind.ordinal() * 35;
        int discount = Math.max(0, barracks - 1) * 10;
        int floor = 110 + kind.ordinal() * 30;
        return Math.max(floor, base - discount);
    }

    public static synchronized String hire(ServerPlayer player, MercenaryClass kind) {
        if (kind == null) return "알 수 없는 용병 병과입니다.";
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
            return "용병 고용은 병영 단말기 근처에서만 가능합니다.";
        }
        String blocked = VillageMaintenanceRules.blockReason("용병 고용");
        if (blocked != null) return blocked;
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
            return "병영이 파괴되어 용병을 고용할 수 없습니다.";
        }
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 고용할 수 없습니다.";
        int cap = capacity();
        int current = rosterCount();
        if (current >= cap) return "용병 정원이 가득 찼습니다. 현재 " + current + " / " + cap;
        int cost = hireCost(kind);
        if (!VillageProgressionSystem.spendSupplies(cost)) {
            return "공동 보급품이 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.supplies();
        }
        MinecraftServer server = level.getServer();
        IronGolem mercenary = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
        if (mercenary == null) {
            if (server != null) VillageProgressionSystem.addSupplies(server, cost, "용병 고용 실패 환불");
            return "용병을 배치하지 못해 공동 보급품을 돌려드렸습니다.";
        }
        BlockPos spawn = barracksYardSpawn(level, mercenary.getUUID());
        mercenary.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        mercenary.setPlayerCreated(true);
        mercenary.setPersistenceRequired();
        mercenary.setInvisible(true);
        CLASSES.put(mercenary.getUUID(), kind);
        LEVELS.put(mercenary.getUUID(), 1);
        KILLS.put(mercenary.getUUID(), 0);
        persist();
        applyClassPassives(mercenary, kind, 1);
        mercenary.setHealth(mercenary.getMaxHealth());
        refreshName(mercenary);
        VillageWorldSystem.markAllowedGameMob(mercenary);
        if (!level.addFreshEntity(mercenary)) {
            unregister(mercenary.getUUID());
            VillageWorldSystem.unmarkAllowedGameMob(mercenary.getUUID());
            if (server != null) VillageProgressionSystem.addSupplies(server, cost, "용병 배치 실패 환불");
            return "용병 배치에 실패해 공동 보급품을 돌려드렸습니다.";
        }
        VillageMercenaryPresentationSystem.ensure(level, mercenary, kind, 1);
        return kind.displayName() + " 고용 완료 · Lv.1 · 현재 " + (current + 1) + " / " + cap
                + " · 사망하지 않는 한 저장과 재접속 후에도 유지됩니다.";
    }

    public static synchronized void captureNightSnapshot(MinecraftServer server) {
        NIGHT_SNAPSHOT.clear();
        CLASSES.forEach((uuid, kind) -> NIGHT_SNAPSHOT.add(new MercenarySnapshot(
                kind, LEVELS.getOrDefault(uuid, 1), KILLS.getOrDefault(uuid, 0))));
        persistNightSnapshot();
    }
    public static synchronized void restoreNightSnapshot(MinecraftServer server) {
        discardCurrent(server); CLASSES.clear(); LEVELS.clear(); KILLS.clear();
        ServerLevel level = server.overworld();
        for (MercenarySnapshot snapshot : NIGHT_SNAPSHOT) {
            IronGolem mob = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
            if (mob == null) continue;
            // Retry restoration must follow the same outside-yard contract as a new hire. Rebuilding
            // around the barracks centre could put a fresh UUID back behind its closed doors.
            BlockPos spawn = barracksYardSpawn(level, mob.getUUID());
            mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            mob.setPlayerCreated(true); mob.setPersistenceRequired(); mob.setInvisible(true);
            CLASSES.put(mob.getUUID(), snapshot.kind()); LEVELS.put(mob.getUUID(), snapshot.level());
            KILLS.put(mob.getUUID(), snapshot.kills()); applyClassPassives(mob, snapshot.kind(), snapshot.level());
            mob.setHealth(mob.getMaxHealth());
            refreshName(mob); VillageWorldSystem.markAllowedGameMob(mob);
            if (!level.addFreshEntity(mob)) {
                unregister(mob.getUUID()); VillageWorldSystem.unmarkAllowedGameMob(mob.getUUID());
            } else {
                VillageMercenaryPresentationSystem.ensure(level, mob, snapshot.kind(), snapshot.level());
            }
        }
        persist();
    }
    public static synchronized void resetForNewGame(MinecraftServer server) {
        discardCurrent(server); CLASSES.clear(); LEVELS.clear(); KILLS.clear(); NIGHT_SNAPSHOT.clear();
        reset();
        persist(); persistNightSnapshot();
    }
    private static void discardCurrent(MinecraftServer server) {
        for (UUID uuid : new java.util.HashSet<>(CLASSES.keySet())) {
            VillageMercenaryPresentationSystem.remove(server.overworld(), uuid);
            var entity = server.overworld().getEntity(uuid); if (entity != null) entity.discard();
            VillageWorldSystem.unmarkAllowedGameMob(uuid);
        }
        VillageMercenaryPresentationSystem.reset();
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter < 20) return;
        tickCounter = 0;
        ServerLevel level = server.overworld();
        for (IronGolem mercenary : loadedMercenaries(level)) {
            recognize(mercenary);
            if (mercenary.getTarget() != null
                    && (!VillageRaidSystem.isRaidEnemy(mercenary.getTarget())
                    || mercenary.getTarget() instanceof Mob target
                    && VillageRaidSystem.isAerialEnemy(target))) {
                mercenary.setTarget(null);
            }
            MercenaryClass kind = mercenaryClass(mercenary);
            int rank = rank(mercenary);
            applyClassPassives(mercenary, kind, rank);
            VillageMercenaryPresentationSystem.ensure(level, mercenary, kind, rank);
            if (!VillageRaidSystem.isActive()) continue;
            if (kind == MercenaryClass.BASTION) bastionControl(level, mercenary, rank);
            else if (kind == MercenaryClass.STRIKER) strikerPressure(level, mercenary, rank);
            else if (kind == MercenaryClass.RANGER) rangedAttack(level, mercenary, rank);
            else if (kind == MercenaryClass.MEDIC) healAllies(level, server, mercenary, rank);
            else if (kind == MercenaryClass.WARDER) wardAllies(level, server, mercenary, rank);
            else if (kind == MercenaryClass.ARTILLERIST) artilleryAttack(level, mercenary, rank);
        }
    }

    public static synchronized void awardKillExperience(Mob killer) {
        if (!(killer instanceof IronGolem mercenary) || !isMercenary(mercenary.getUUID())
                || !(mercenary.level() instanceof ServerLevel level)) return;
        UUID uuid = mercenary.getUUID();
        int kills = KILLS.getOrDefault(uuid, 0)
                + VillageDefenseResearchSystem.mercenaryTrainingProgressPerKill();
        int currentRank = LEVELS.getOrDefault(uuid, 1);
        int nextRank = currentRank;
        while (nextRank < MAX_LEVEL && kills >= killsRequiredForLevel(nextRank + 1)) nextRank++;
        KILLS.put(uuid, kills);
        if (nextRank > currentRank) {
            LEVELS.put(uuid, nextRank);
            applyClassPassives(mercenary, mercenaryClass(mercenary), nextRank);
            refreshName(mercenary);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    mercenary.getX(), mercenary.getY() + 1.3, mercenary.getZ(),
                    16, 0.45, 0.7, 0.45, 0.05);
        }
        persist();
    }

    public static synchronized void handleDeath(Mob mob) {
        if (mob == null || !isMercenary(mob.getUUID())) return;
        STRIKER_TRACKED_TARGETS.remove(mob.getUUID());
        STRIKER_OPENING_TARGETS.remove(mob.getUUID());
        if (mob.level() instanceof ServerLevel level) VillageMercenaryPresentationSystem.remove(level, mob.getUUID());
        unregister(mob.getUUID());
    }

    public static synchronized void healAtDawn(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        for (IronGolem mercenary : loadedMercenaries(level)) {
            recognize(mercenary);
            if (mercenary.getHealth() < mercenary.getMaxHealth()) {
                mercenary.setHealth(mercenary.getMaxHealth());
            }
        }
    }

    public static String status(MinecraftServer server) {
        if (server == null) return "용병 상태를 확인할 수 없습니다.";
        ServerLevel level = server.overworld();
        return "용병 명부 " + rosterCount() + " / " + capacity()
                + " · 현재 로드 " + loadedCount(level)
                + " · 용병 교리 Lv."
                + VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.MERCENARY)
                + " · 적 처치 경험으로 최대 Lv." + MAX_LEVEL + "까지 장기 성장";
    }

    public static int capacity() {
        return 1 + VillageProgressionSystem.barracksLevel() / 2
                + VillageDefenseResearchSystem.mercenaryCapacityBonus();
    }

    /** Authoritative saved roster size. Hiring capacity must never depend on current chunk/AABB loading. */
    public static synchronized int rosterCount() {
        return CLASSES.size();
    }

    public static synchronized int loadedCount(ServerLevel level) {
        return loadedMercenaries(level).size();
    }

    /** Resolves the saved roster directly, avoiding repeated battlefield-sized entity scans. */
    public static synchronized List<IronGolem> loadedMercenaries(ServerLevel level) {
        if (level == null) return List.of();
        List<IronGolem> result = new ArrayList<>();
        for (UUID uuid : CLASSES.keySet()) {
            var entity = level.getEntity(uuid);
            if (entity instanceof IronGolem golem && golem.isAlive()) result.add(golem);
        }
        return List.copyOf(result);
    }

    public static synchronized boolean isCombatMercenary(Mob mob) {
        return mob instanceof IronGolem golem && golem.isAlive() && CLASSES.containsKey(golem.getUUID());
    }

    public static boolean blockFriendlyFire(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob target) || !isCombatMercenary(target)) return false;
        var sourceEntity = event.getSource().getEntity();
        var direct = event.getSource().getDirectEntity();
        boolean friendly = sourceEntity instanceof ServerPlayer
                || sourceEntity instanceof Mob sourceMob && isCombatMercenary(sourceMob)
                || direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer;
        if (!friendly) return false;
        event.setAmount(0.0f);
        if (target.getTarget() != null && !VillageRaidSystem.isRaidEnemy(target.getTarget())) {
            target.setTarget(null);
        }
        return true;
    }

    public static synchronized IronGolem nearestCombatMercenary(ServerLevel level, Mob enemy, double range) {
        if (level == null || enemy == null || range <= 0.0) return null;
        IronGolem chosen = null;
        double chosenDistance = range * range;
        for (IronGolem golem : loadedMercenaries(level)) {
            double distance = enemy.distanceToSqr(golem);
            if (distance <= chosenDistance && enemy.hasLineOfSight(golem)) {
                chosenDistance = distance;
                chosen = golem;
            }
        }
        return chosen;
    }

    public static synchronized List<RosterEntry> rosterEntries(MinecraftServer server) {
        if (server == null) return List.of();
        ServerLevel level = server.overworld();
        List<RosterEntry> result = new ArrayList<>();
        CLASSES.forEach((uuid, kind) -> {
            var entity = level.getEntity(uuid);
            boolean loaded = entity instanceof IronGolem golem && golem.isAlive();
            result.add(new RosterEntry(uuid, kind, LEVELS.getOrDefault(uuid, 1),
                    KILLS.getOrDefault(uuid, 0), loaded));
        });
        return List.copyOf(result);
    }

    public static synchronized String retire(ServerPlayer player, UUID uuid) {
        if (player == null || uuid == null) return "퇴역할 용병을 찾을 수 없습니다.";
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
            return "용병 퇴역은 병영 단말기 근처에서만 가능합니다.";
        }
        String blocked = VillageMaintenanceRules.blockReason("용병 퇴역");
        if (blocked != null) return blocked;
        MercenaryClass kind = CLASSES.get(uuid);
        if (kind == null) return "이미 명부에서 제외된 용병입니다.";
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 용병을 퇴역시킬 수 없습니다.";
        var entity = level.getEntity(uuid);
        if (!(entity instanceof IronGolem mercenary) || !mercenary.isAlive()) {
            return "해당 용병이 현재 로드되지 않았습니다. 용병이 있는 구역을 불러온 뒤 다시 시도하세요.";
        }
        int rank = LEVELS.getOrDefault(uuid, 1);
        VillageMercenaryPresentationSystem.remove(level, uuid);
        mercenary.discard();
        VillageWorldSystem.unmarkAllowedGameMob(uuid);
        unregister(uuid);
        return kind.displayName() + " Lv." + rank + " 퇴역 완료 · 고용비는 환불되지 않습니다.";
    }

    private static void bastionControl(ServerLevel level, IronGolem mercenary, int rank) {
        double mastery = masteryProgress(rank);
        double utility = endlessUtilityAdaptation(rank);
        double radius = (12.0 + 12.0 * mastery) * utility;
        int limit = 12 + (int) Math.round(24.0 * mastery);
        VillageRaidSystem.tauntEnemies(level, mercenary, mercenary.position(), radius,
                50 + (int) Math.round(70.0 * mastery), limit);
        Vec3 eye = mercenary.position().add(0, 1.8, 0);
        boolean engaged = false;
        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), radius, limit, null)) {
            if (VillageRaidSystem.isAerialEnemy(enemy)
                    || !VillageDefenseLineOfSight.hasLine(level, eye, enemy)) continue;
            enemy.setTarget(mercenary);
            enemy.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 28 + (int) Math.round(90.0 * mastery), 0));
            engaged = true;
        }
        if (engaged) {
            int barrierAmplifier = Math.min(6, 1 + rank / 18);
            int barrierDuration = 70 + (int) Math.round(50.0 * mastery);
            mercenary.addEffect(new MobEffectInstance(
                    MobEffects.ABSORPTION, barrierDuration, barrierAmplifier, false, false, true));
            VillageDefenseEffectSystem.mercenaryGuardPulse(level, mercenary.position(), radius);
        }
    }

    private static void strikerPressure(ServerLevel level, IronGolem mercenary, int rank) {
        double mastery = masteryProgress(rank);
        double range = (22.0 + 30.0 * mastery) * endlessUtilityAdaptation(rank);
        Mob target = nearestGroundEnemy(level, mercenary.position(), range);
        UUID strikerId = mercenary.getUUID();
        if (target == null || !VillageDefenseLineOfSight.hasLine(level, mercenary.getEyePosition(), target)) {
            STRIKER_TRACKED_TARGETS.remove(strikerId);
            STRIKER_OPENING_TARGETS.remove(strikerId);
            return;
        }
        UUID targetId = target.getUUID();
        UUID previousTarget = STRIKER_TRACKED_TARGETS.put(strikerId, targetId);
        if (!targetId.equals(previousTarget)) {
            STRIKER_OPENING_TARGETS.put(strikerId, targetId);
        }
        mercenary.setTarget(target);
        mercenary.getNavigation().moveTo(target, 1.18 + 0.35 * mastery);
        VillageDefenseEffectSystem.mercenaryStrikerPressure(level, mercenary.position().add(0, 1.2, 0),
                target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    public static void applyOutgoingDamage(LivingIncomingDamageEvent event) {
        if (event == null
                || !(event.getSource().getEntity() instanceof IronGolem mercenary)
                || mercenaryClass(mercenary) != MercenaryClass.STRIKER
                || !(event.getEntity() instanceof Mob target)
                || !VillageRaidSystem.isRaidEnemy(target)) return;
        UUID strikerId = mercenary.getUUID();
        UUID openingTarget = STRIKER_OPENING_TARGETS.get(strikerId);
        if (openingTarget == null || !openingTarget.equals(target.getUUID())) return;
        STRIKER_OPENING_TARGETS.remove(strikerId);
        float openingMultiplier = 2.20f + (float) (0.50 * masteryProgress(rank(mercenary)));
        event.setAmount(event.getAmount() * openingMultiplier);
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
                    10, 0.28, 0.34, 0.28, 0.05);
        }
    }

    private static void rangedAttack(ServerLevel level, IronGolem mercenary, int rank) {
        Vec3 start = mercenary.getEyePosition();
        double mastery = masteryProgress(rank);
        double range = (50.0 + 52.0 * mastery) * endlessUtilityAdaptation(rank);
        Mob target = VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), range,
                        18 + (int) Math.round(18.0 * mastery), null)
                .stream().filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))
                .min(java.util.Comparator
                        .comparingInt((Mob enemy) -> VillageRaidSystem.isAerialEnemy(enemy) ? 0 : 1)
                        .thenComparingInt(enemy -> -VillageRaidSystem.aerialThreatPriority(enemy))
                        .thenComparingDouble(mercenary::distanceToSqr)).orElse(null);
        mercenary.setTarget(null);
        if (target == null) return;
        mercenary.getLookControl().setLookAt(target, 35.0f, 35.0f);
        float damage = 5.2f * mercenaryPower(rank)
                * VillageDefenseResearchSystem.mercenaryDamageMultiplier()
                * endlessDamageAdaptation(rank);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.62, 0);
        VillageDefenseEffectSystem.mercenaryRangerShot(level, start, end);
        level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.14, 0.18, 0.14, 0.02);
        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);
    }

    private static void healAllies(ServerLevel level, MinecraftServer server, IronGolem medic, int rank) {
        double utility = endlessUtilityAdaptation(rank);
        float amount = 2.8f * mercenaryPower(rank)
                * VillageDefenseResearchSystem.mercenaryHealingMultiplier()
                * (float) utility;
        double radius = (8.0 + 13.0 * masteryProgress(rank)) * utility;
        double radiusSquared = radius * radius;
        for (IronGolem ally : loadedMercenaries(level)) {
            if (ally.distanceToSqr(medic) <= radiusSquared) ally.heal(amount);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(medic) <= radiusSquared
                    && !VillageRespawnSystem.isDowned(player)) player.heal(amount * 0.65f);
        }
        VillageDefenseEffectSystem.mercenaryHealPulse(level, medic.position(), radius);
        level.sendParticles(ParticleTypes.HEART, medic.getX(), medic.getY() + 1.4, medic.getZ(),
                3 + Math.min(10, rank / 5), 0.55, 0.4, 0.55, 0.02);
    }


    private static void wardAllies(
            ServerLevel level, MinecraftServer server, IronGolem warder, int rank) {
        long now = level.getGameTime();
        boolean cleansePulse = NEXT_WARD_PULSE.getOrDefault(warder.getUUID(), 0L) <= now;
        if (cleansePulse) NEXT_WARD_PULSE.put(warder.getUUID(), now + 60L);

        double utility = endlessUtilityAdaptation(rank);
        double radius = (7.5 + 7.5 * masteryProgress(rank)) * utility;
        double radiusSq = radius * radius;
        // This method is called once per second. A 60-tick refresh keeps the aura continuous
        // while the ally remains in range, without creating a one-shot permanent status.
        int duration = 60;
        int absorption = rank >= 80 ? 2 : rank >= 40 ? 1 : 0;
        int resistance = rank >= 70 ? 1 : 0;
        for (IronGolem ally : loadedMercenaries(level)) {
            if (!ally.isAlive() || ally.distanceToSqr(warder) > radiusSq) continue;
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, absorption, false, false, true));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, resistance, false, false, true));
            if (cleansePulse) clearHarmfulEffects(ally);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != level || !player.isAlive() || VillageRespawnSystem.isDowned(player)
                    || player.distanceToSqr(warder) > radiusSq) continue;
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, absorption, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, resistance, false, false, true));
            if (cleansePulse) clearHarmfulEffects(player);
        }
        if (cleansePulse) {
            VillageDefenseEffectSystem.mercenaryWardPulse(level, warder.position(), radius);
        }
    }

    private static void clearHarmfulEffects(LivingEntity entity) {
        for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                entity.removeEffect(effect.getEffect());
            }
        }
    }

    private static void artilleryAttack(ServerLevel level, IronGolem artillerist, int rank) {
        long now = level.getGameTime();
        if (NEXT_ARTILLERY_CAST.getOrDefault(artillerist.getUUID(), 0L) > now) return;
        NEXT_ARTILLERY_CAST.put(artillerist.getUUID(), now + Math.max(46L, 72L - rank / 4L));
        double mastery = masteryProgress(rank);
        double range = (38.0 + 28.0 * mastery) * endlessUtilityAdaptation(rank);
        List<Mob> candidates = VillageRaidSystem.activeEnemiesNear(
                level, artillerist.position(), range, 40, null).stream()
                .filter(enemy -> VillageDefenseLineOfSight.hasLine(level, artillerist.getEyePosition(), enemy))
                .toList();
        Mob target = candidates.stream()
                .max(java.util.Comparator.comparingDouble(Mob::getMaxHealth)
                        .thenComparingDouble(enemy -> -artillerist.distanceToSqr(enemy)))
                .orElse(null);
        artillerist.setTarget(null);
        if (target == null) return;
        artillerist.getLookControl().setLookAt(target, 35.0f, 35.0f);
        double radius = 3.2 + 2.3 * mastery;
        float damage = (4.4f + rank * 0.055f) * mercenaryPower(rank)
                * VillageDefenseResearchSystem.mercenaryDamageMultiplier()
                * endlessDamageAdaptation(rank);
        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, target.position(), radius, 18, null)) {
            if (!enemy.isAlive()) continue;
            enemy.hurtServer(level, level.damageSources().mobAttack(artillerist), damage);
            enemy.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 30 + rank / 2, rank >= 75 ? 1 : 0, false, false, true));
        }
        VillageDefenseEffectSystem.mercenaryArtilleryBurst(level, target.position(), radius);
    }

    private static Mob nearestGroundEnemy(ServerLevel level, Vec3 origin, double range) {
        return VillageRaidSystem.activeEnemiesNear(level, origin, range, 64, null).stream()
                .filter(enemy -> !VillageRaidSystem.isAerialEnemy(enemy))
                .min(java.util.Comparator.comparingDouble(enemy -> enemy.position().distanceToSqr(origin)))
                .orElse(null);
    }

    private static boolean fullMercenaryMastery(int rank) {
        return rank >= MAX_LEVEL
                && VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.MERCENARY)
                >= VillageDefenseResearchSystem.MAX_LEVEL;
    }

    private static float endlessDamageAdaptation(int rank) {
        return fullMercenaryMastery(rank)
                ? VillageCampaignProgression.endlessDefenseDamageMultiplier(VillageCouncilState.currentDay())
                : 1.0f;
    }

    private static double endlessUtilityAdaptation(int rank) {
        return fullMercenaryMastery(rank)
                ? VillageCampaignProgression.endlessDefenseUtilityMultiplier(VillageCouncilState.currentDay())
                : 1.0;
    }

    private static double masteryProgress(int rank) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, rank));
        return Math.sqrt((safe - 1) / (double) (MAX_LEVEL - 1));
    }

    private static float mercenaryPower(int rank) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, rank));
        int veteran = Math.min(19, safe - 1);
        int elite = Math.min(40, Math.max(0, safe - 20));
        int master = Math.max(0, safe - 60);
        return 1.0f + veteran * 0.05f + elite * 0.025f + master * 0.018f;
    }

    private static int killsRequiredForLevel(int level) {
        int n = Math.max(0, Math.min(MAX_LEVEL - 1, level - 1));
        return n * 6 + (n * n) / 2;
    }

    private static void applyClassPassives(IronGolem mercenary, MercenaryClass kind, int rank) {
        applyClassAttributes(mercenary, kind, rank);
        mercenary.setInvisible(true);
        int duration = 20 * 60 * 60;
        mercenary.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY, duration, 0, false, false));
        int healthTier = Math.max(0, (rank - 1) / 14);
        if (healthTier > 0) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration, healthTier - 1, false, false));
        }
        if (kind == MercenaryClass.BASTION) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration,
                    Math.min(3, rank / 25), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                    Math.min(5, Math.max(0, rank / 18)), false, false));
        } else if (kind == MercenaryClass.STRIKER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration,
                    Math.min(3, rank / 25), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    Math.min(2, rank / 35), false, false));
        } else if (kind == MercenaryClass.RANGER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    rank >= 75 ? 2 : rank >= 35 ? 1 : 0, false, false));
        } else if (kind == MercenaryClass.MEDIC) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration,
                    rank >= 75 ? 2 : rank >= 30 ? 1 : 0, false, false));
        } else if (kind == MercenaryClass.WARDER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration,
                    rank >= 80 ? 2 : rank >= 35 ? 1 : 0, false, false));
        } else if (kind == MercenaryClass.ARTILLERIST) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    rank >= 70 ? 1 : 0, false, false));
        }
    }

    private static void applyClassAttributes(IronGolem mercenary, MercenaryClass kind, int rank) {
        int safeRank = Math.max(1, Math.min(MAX_LEVEL, rank));
        double durability = VillageDefenseResearchSystem.mercenaryDurabilityMultiplier();
        double maxHealth = (switch (kind) {
            case BASTION -> 340.0 + (safeRank - 1) * 4.0;
            case STRIKER -> 250.0 + (safeRank - 1) * 3.0;
            case RANGER -> 215.0 + (safeRank - 1) * 2.4;
            case MEDIC -> 270.0 + (safeRank - 1) * 2.8;
            case WARDER -> 300.0 + (safeRank - 1) * 3.2;
            case ARTILLERIST -> 225.0 + (safeRank - 1) * 2.5;
        }) * durability;
        double mastery = masteryProgress(safeRank);
        double armor = switch (kind) {
            case BASTION -> 18.0 + 6.0 * mastery;
            case STRIKER -> 11.0 + 7.0 * mastery;
            case RANGER -> 9.0 + 6.0 * mastery;
            case MEDIC -> 11.0 + 6.0 * mastery;
            case WARDER -> 14.0 + 7.0 * mastery;
            case ARTILLERIST -> 8.0 + 5.0 * mastery;
        };
        double attack = (switch (kind) {
            case BASTION -> 11.5 + safeRank * 0.12;
            case STRIKER -> 16.0 + safeRank * 0.20;
            case RANGER -> 5.8 + safeRank * 0.06;
            case MEDIC -> 7.0 + safeRank * 0.06;
            case WARDER -> 8.0 + safeRank * 0.08;
            case ARTILLERIST -> 6.0 + safeRank * 0.07;
        }) * VillageDefenseResearchSystem.mercenaryDamageMultiplier()
                * endlessDamageAdaptation(safeRank);
        double speed = switch (kind) {
            case BASTION -> 0.235;
            case STRIKER -> 0.31;
            case RANGER -> 0.28;
            case MEDIC -> 0.27;
            case WARDER -> 0.265;
            case ARTILLERIST -> 0.275;
        };
        double knockback = switch (kind) {
            case BASTION -> 0.72;
            case STRIKER -> 0.38;
            case RANGER -> 0.22;
            case MEDIC -> 0.30;
            case WARDER -> 0.48;
            case ARTILLERIST -> 0.24;
        };
        setBaseAttribute(mercenary, Attributes.MAX_HEALTH, maxHealth);
        setBaseAttribute(mercenary, Attributes.ARMOR, armor);
        setBaseAttribute(mercenary, Attributes.ATTACK_DAMAGE, attack);
        setBaseAttribute(mercenary, Attributes.MOVEMENT_SPEED, speed);
        setBaseAttribute(mercenary, Attributes.KNOCKBACK_RESISTANCE, knockback);
        if (mercenary.getHealth() > mercenary.getMaxHealth()) mercenary.setHealth(mercenary.getMaxHealth());
    }

    private static void setBaseAttribute(
            IronGolem mercenary,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double value) {
        var instance = mercenary.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    public static int aggroCapacity(IronGolem mercenary) {
        if (mercenary == null) return 0;
        int rank = rank(mercenary);
        double mastery = masteryProgress(rank);
        return switch (mercenaryClass(mercenary)) {
            case BASTION -> 9 + (int) Math.round(7.0 * mastery);
            case STRIKER -> 4 + (int) Math.round(4.0 * mastery);
            case RANGER -> 2 + (int) Math.round(3.0 * mastery);
            case MEDIC -> 1 + (int) Math.round(3.0 * mastery);
            case WARDER -> 3 + (int) Math.round(4.0 * mastery);
            case ARTILLERIST -> 2 + (int) Math.round(3.0 * mastery);
        };
    }

    public static synchronized MercenaryClass classOf(Mob mob) {
        return mob == null ? null : CLASSES.get(mob.getUUID());
    }

    private static synchronized MercenaryClass mercenaryClass(Mob mob) {
        MercenaryClass kind = classOf(mob);
        return kind == null ? MercenaryClass.BASTION : kind;
    }

    private static synchronized int rank(Mob mob) {
        return LEVELS.getOrDefault(mob.getUUID(), 1);
    }

    private static synchronized boolean isMercenary(UUID uuid) {
        return uuid != null && CLASSES.containsKey(uuid);
    }

    private static void refreshName(Mob mob) {
        MercenaryClass kind = mercenaryClass(mob);
        mob.setCustomName(Component.literal(kind.displayName() + " Lv." + rank(mob)));
        mob.setCustomNameVisible(true);
    }

    private static void loadNightSnapshot() {
        NIGHT_SNAPSHOT.clear();
        if (snapshotData == null) return;
        snapshotData.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String[] parts = entry.getValue().split("\\|", 3);
                    if (parts.length != 3) return;
                    MercenaryClass kind = MercenaryClass.fromId(parts[0]);
                    if (kind == null) return;
                    try {
                        int level = Math.max(1, Math.min(MAX_LEVEL, Integer.parseInt(parts[1])));
                        int kills = Math.max(0, Integer.parseInt(parts[2]));
                        NIGHT_SNAPSHOT.add(new MercenarySnapshot(kind, level, kills));
                    } catch (NumberFormatException ignored) {
                    }
                });
    }

    private static void persistNightSnapshot() {
        if (snapshotData == null) return;
        Map<String, String> encoded = new LinkedHashMap<>();
        for (int index = 0; index < NIGHT_SNAPSHOT.size(); index++) {
            MercenarySnapshot snapshot = NIGHT_SNAPSHOT.get(index);
            encoded.put(String.format(Locale.ROOT, "%04d", index),
                    snapshot.kind().id() + "|" + snapshot.level() + "|" + snapshot.kills());
        }
        snapshotData.replace(encoded);
    }

    private record MercenarySnapshot(MercenaryClass kind, int level, int kills) {}

    static BlockPos barracksYardSpawn(ServerLevel level, UUID mercenaryId) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return safeSpawn(level,
                VillageWorldSystem.buildingCenter(VillageProgressionSystem.Building.BARRACKS));
        VillageBuildingCatalog.Spec spec = VillageBuildingCatalog.spec(VillageProgressionSystem.Building.BARRACKS);
        BlockPos origin = center.offset(spec.dx(), 0, spec.dz());
        BlockPos entrance = VillageBuildingCatalog.entrance(level, origin, spec);
        Direction outward = spec.entranceFacing();
        Direction sideways = outward.getClockWise();
        int slot = Math.floorMod(mercenaryId == null ? 0 : mercenaryId.hashCode(), 5) - 2;
        BlockPos yard = entrance.relative(outward, 4).relative(sideways, slot * 2);
        return safeSpawn(level, yard);
    }

    private static BlockPos safeSpawn(ServerLevel level, BlockPos origin) {
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, 0, dz);
                    BlockPos floor = pos.below();
                    if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                            && level.getBlockState(pos).isAir()
                            && level.getBlockState(pos.above()).isAir()) return pos;
                }
            }
        }
        return origin.above();
    }

    private static synchronized void unregister(UUID uuid) {
        CLASSES.remove(uuid);
        LEVELS.remove(uuid);
        KILLS.remove(uuid);
        STRIKER_TRACKED_TARGETS.remove(uuid);
        STRIKER_OPENING_TARGETS.remove(uuid);
        NEXT_WARD_PULSE.remove(uuid);
        NEXT_ARTILLERY_CAST.remove(uuid);
        persist();
    }

    private static synchronized void sanitize() {
        LEVELS.keySet().removeIf(uuid -> !CLASSES.containsKey(uuid));
        KILLS.keySet().removeIf(uuid -> !CLASSES.containsKey(uuid));
        for (UUID uuid : CLASSES.keySet()) {
            LEVELS.put(uuid, Math.max(1, Math.min(MAX_LEVEL, LEVELS.getOrDefault(uuid, 1))));
            KILLS.put(uuid, Math.max(0, KILLS.getOrDefault(uuid, 0)));
        }
    }

    private static synchronized void persist() {
        if (savedData == null) return;
        Map<String, String> classes = new LinkedHashMap<>();
        CLASSES.forEach((uuid, kind) -> classes.put(uuid.toString(), kind.id()));
        Map<String, Integer> levels = new LinkedHashMap<>();
        LEVELS.forEach((uuid, value) -> levels.put(uuid.toString(), value));
        Map<String, Integer> kills = new LinkedHashMap<>();
        KILLS.forEach((uuid, value) -> kills.put(uuid.toString(), value));
        savedData.replace(classes, levels, kills);
    }

    private static void parseUuid(String value, java.util.function.Consumer<UUID> consumer) {
        try { consumer.accept(UUID.fromString(value)); }
        catch (IllegalArgumentException ignored) { }
    }

    public record RosterEntry(UUID uuid, MercenaryClass kind, int level, int kills, boolean loaded) {}

    public enum MercenaryClass {
        BASTION("bastion", "방벽 수호병", "중장갑 전열병. 많은 적을 받아내며 성문과 시설 앞을 버팁니다."),
        STRIKER("striker", "돌격 집행관", "고기동 근접 전투원. 새 표적을 추적해 첫 타에 큰 개시 피해를 줍니다."),
        RANGER("ranger", "성루 명사수", "후방 원거리 전투원. 공중 위협을 우선 요격하고 집중 사격합니다."),
        MEDIC("medic", "전장 치유사", "후방 지원 전투원. 주변 플레이어와 용병을 주기적으로 회복합니다."),
        WARDER("warder", "결계 수도사", "보호·정화 지원병. 주변 아군의 흡수·저항을 유지하고 주기적으로 모든 해로운 효과를 제거합니다."),
        ARTILLERIST("artillerist", "비전 포격병", "후방 광역 화력병. 체력이 높은 위협을 골라 범위 포격으로 적 밀집을 압박합니다.");

        private final String id;
        private final String displayName;
        private final String description;

        MercenaryClass(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }

        public static MercenaryClass fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (MercenaryClass kind : values()) if (kind.id.equals(normalized)) return kind;
            return null;
        }
    }
}
