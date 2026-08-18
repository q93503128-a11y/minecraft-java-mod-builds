package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Persistent classed mercenaries backed by world SavedData rather than removed entity tag APIs. */
public final class VillageMercenarySystem {
    private static final Map<UUID, MercenaryClass> CLASSES = new LinkedHashMap<>();
    private static final Map<UUID, Integer> LEVELS = new LinkedHashMap<>();
    private static final Map<UUID, Integer> KILLS = new LinkedHashMap<>();
    private static VillageMercenaryData savedData;
    private static VillageMercenarySnapshotData snapshotData;
    private static final List<MercenarySnapshot> NIGHT_SNAPSHOT = new ArrayList<>();
    private static int tickCounter;

    private VillageMercenarySystem() {}

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenaryData.TYPE);
        snapshotData = server.overworld().getDataStorage().computeIfAbsent(VillageMercenarySnapshotData.TYPE);
        CLASSES.clear();
        LEVELS.clear();
        KILLS.clear();
        savedData.classes().forEach((key, value) -> parseUuid(key, uuid -> {
            MercenaryClass kind = MercenaryClass.fromId(value);
            if (kind != null) CLASSES.put(uuid, kind);
        }));
        savedData.levels().forEach((key, value) -> parseUuid(key,
                uuid -> LEVELS.put(uuid, Math.max(1, Math.min(5, value)))));
        savedData.kills().forEach((key, value) -> parseUuid(key,
                uuid -> KILLS.put(uuid, Math.max(0, value))));
        sanitize();
        persist();
        loadNightSnapshot();
        tickCounter = 0;
    }

    public static void reset() { tickCounter = 0; }

    public static synchronized boolean recognize(Mob mob) {
        if (!(mob instanceof IronGolem) || !CLASSES.containsKey(mob.getUUID())) return false;
        mob.setPersistenceRequired();
        VillageWorldSystem.markAllowedGameMob(mob);
        refreshName(mob);
        return true;
    }

    public static int hireCost(MercenaryClass kind) {
        if (kind == null) return 0;
        return 150 + kind.ordinal() * 35 + VillageProgressionSystem.barracksLevel() * 25;
    }

    public static synchronized String hire(ServerPlayer player, MercenaryClass kind) {
        if (kind == null) return "알 수 없는 용병 병과입니다.";
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
            return "병영이 파괴되어 용병을 고용할 수 없습니다.";
        }
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 고용할 수 없습니다.";
        int cap = capacity();
        int current = count(level);
        if (current >= cap) return "용병 정원이 가득 찼습니다. 현재 " + current + " / " + cap;
        int cost = hireCost(kind);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        IronGolem mercenary = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
        if (mercenary == null) {
            VillageProgressionSystem.addCoins(player, cost, "용병 고용 실패 환불");
            return "용병을 배치하지 못해 주화를 돌려드렸습니다.";
        }
        BlockPos origin = VillageWorldSystem.buildingCenter(VillageProgressionSystem.Building.BARRACKS);
        BlockPos spawn = safeSpawn(level, origin);
        mercenary.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        mercenary.setPlayerCreated(true);
        mercenary.setPersistenceRequired();
        CLASSES.put(mercenary.getUUID(), kind);
        LEVELS.put(mercenary.getUUID(), 1);
        KILLS.put(mercenary.getUUID(), 0);
        persist();
        applyClassPassives(mercenary, kind, 1);
        refreshName(mercenary);
        VillageWorldSystem.markAllowedGameMob(mercenary);
        if (!level.addFreshEntity(mercenary)) {
            unregister(mercenary.getUUID());
            VillageWorldSystem.unmarkAllowedGameMob(mercenary.getUUID());
            VillageProgressionSystem.addCoins(player, cost, "용병 배치 실패 환불");
            return "용병 배치에 실패해 주화를 돌려드렸습니다.";
        }
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
        BlockPos origin = VillageWorldSystem.buildingCenter(VillageProgressionSystem.Building.BARRACKS);
        int index = 0;
        for (MercenarySnapshot snapshot : NIGHT_SNAPSHOT) {
            IronGolem mob = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
            if (mob == null) continue;
            BlockPos spawn = safeSpawn(level, origin.offset((index % 3) * 2, 0, (index / 3) * 2));
            mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            mob.setPlayerCreated(true); mob.setPersistenceRequired();
            CLASSES.put(mob.getUUID(), snapshot.kind()); LEVELS.put(mob.getUUID(), snapshot.level());
            KILLS.put(mob.getUUID(), snapshot.kills()); applyClassPassives(mob, snapshot.kind(), snapshot.level());
            refreshName(mob); VillageWorldSystem.markAllowedGameMob(mob);
            if (!level.addFreshEntity(mob)) { unregister(mob.getUUID()); VillageWorldSystem.unmarkAllowedGameMob(mob.getUUID()); }
            index++;
        }
        persist();
    }
    public static synchronized void resetForNewGame(MinecraftServer server) {
        discardCurrent(server); CLASSES.clear(); LEVELS.clear(); KILLS.clear(); NIGHT_SNAPSHOT.clear();
        tickCounter = 0; persist(); persistNightSnapshot();
    }
    private static void discardCurrent(MinecraftServer server) {
        for (UUID uuid : new java.util.HashSet<>(CLASSES.keySet())) {
            var entity = server.overworld().getEntity(uuid); if (entity != null) entity.discard();
            VillageWorldSystem.unmarkAllowedGameMob(uuid);
        }
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter < 20) return;
        tickCounter = 0;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (IronGolem mercenary : level.getEntitiesOfClass(IronGolem.class, area,
                entity -> isMercenary(entity.getUUID()))) {
            recognize(mercenary);
            MercenaryClass kind = mercenaryClass(mercenary);
            int rank = rank(mercenary);
            applyClassPassives(mercenary, kind, rank);
            if (!VillageRaidSystem.isActive()) continue;
            if (kind == MercenaryClass.BASTION) bastionControl(level, mercenary, rank);
            else if (kind == MercenaryClass.STRIKER) strikerPressure(level, mercenary, rank);
            else if (kind == MercenaryClass.RANGER) rangedAttack(level, mercenary, rank);
            else if (kind == MercenaryClass.MEDIC) healAllies(level, server, mercenary, rank);
        }
    }

    public static synchronized void awardKillExperience(Mob killer) {
        if (!(killer instanceof IronGolem mercenary) || !isMercenary(mercenary.getUUID())
                || !(mercenary.level() instanceof ServerLevel level)) return;
        UUID uuid = mercenary.getUUID();
        int kills = KILLS.getOrDefault(uuid, 0) + 1;
        int currentRank = LEVELS.getOrDefault(uuid, 1);
        int nextRank = Math.min(5, 1 + kills / 8);
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
        if (mob != null && isMercenary(mob.getUUID())) unregister(mob.getUUID());
    }

    public static String status(MinecraftServer server) {
        if (server == null) return "용병 상태를 확인할 수 없습니다.";
        ServerLevel level = server.overworld();
        return "용병 " + count(level) + " / " + capacity()
                + " · 용병 교리 Lv."
                + VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.MERCENARY)
                + " · 적 처치 경험으로 최대 Lv.5까지 성장";
    }

    public static int capacity() {
        return 1 + VillageProgressionSystem.barracksLevel() / 2
                + VillageDefenseResearchSystem.mercenaryCapacityBonus();
    }

    private static synchronized int count(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return 0;
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        return level.getEntitiesOfClass(IronGolem.class, area,
                entity -> isMercenary(entity.getUUID())).size();
    }

    private static void bastionControl(ServerLevel level, IronGolem mercenary, int rank) {
        double radius = 4.5 + rank * 0.55;
        Vec3 eye = mercenary.position().add(0, 1.8, 0);
        boolean engaged = false;
        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), radius, 5 + rank, null)) {
            if (!VillageDefenseLineOfSight.hasLine(level, eye, enemy)) continue;
            enemy.setTarget(mercenary);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 28 + rank * 5, 0));
            engaged = true;
        }
        if (engaged) VillageDefenseEffectSystem.mercenaryGuardPulse(level, mercenary.position(), radius);
    }

    private static void strikerPressure(ServerLevel level, IronGolem mercenary, int rank) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, mercenary.blockPosition(), 22.0 + rank * 2.0);
        if (target == null || !VillageDefenseLineOfSight.hasLine(level, mercenary.position().add(0, 1.8, 0), target)) return;
        mercenary.setTarget(target);
        mercenary.getNavigation().moveTo(target, 1.18 + rank * 0.025);
        VillageDefenseEffectSystem.mercenaryStrikerPressure(level, mercenary.position().add(0, 1.2, 0),
                target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    private static void rangedAttack(ServerLevel level, IronGolem mercenary, int rank) {
        Vec3 start = mercenary.position().add(0, 1.8, 0);
        Mob target = VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), 42.0 + rank * 3.0, 18, null)
                .stream().filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))
                .min(java.util.Comparator.comparingDouble(mercenary::distanceToSqr)).orElse(null);
        mercenary.setTarget(null);
        if (target == null) return;
        float damage = (3.0f + rank * 1.3f) * VillageDefenseResearchSystem.mercenaryDamageMultiplier();
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        VillageDefenseEffectSystem.mercenaryRangerShot(level, start, end);
        level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.14, 0.18, 0.14, 0.02);
        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);
    }

    private static void healAllies(ServerLevel level, MinecraftServer server, IronGolem medic, int rank) {
        float amount = 1.5f + rank * 0.8f;
        AABB area = medic.getBoundingBox().inflate(8.0 + rank);
        for (IronGolem ally : level.getEntitiesOfClass(IronGolem.class, area,
                entity -> isMercenary(entity.getUUID()) && entity.isAlive())) {
            ally.heal(amount);
        }
        double radiusSquared = (8.0 + rank) * (8.0 + rank);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(medic) <= radiusSquared
                    && !VillageRespawnSystem.isDowned(player)) player.heal(amount * 0.65f);
        }
        VillageDefenseEffectSystem.mercenaryHealPulse(level, medic.position(), 8.0 + rank);
        level.sendParticles(ParticleTypes.HEART, medic.getX(), medic.getY() + 1.4, medic.getZ(),
                3 + rank, 0.55, 0.4, 0.55, 0.02);
    }

    private static void applyClassPassives(IronGolem mercenary, MercenaryClass kind, int rank) {
        int duration = 20 * 60 * 60;
        if (kind == MercenaryClass.BASTION) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration,
                    Math.min(2, rank / 2), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                    Math.min(3, rank - 1), false, false));
        } else if (kind == MercenaryClass.STRIKER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration,
                    Math.min(2, rank / 2), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    Math.min(1, rank / 3), false, false));
        } else if (kind == MercenaryClass.RANGER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, false));
        } else if (kind == MercenaryClass.MEDIC) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration,
                    Math.min(1, rank / 3), false, false));
        }
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
                        int level = Math.max(1, Math.min(5, Integer.parseInt(parts[1])));
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

    private static BlockPos safeSpawn(ServerLevel level, BlockPos origin) {
        for (int radius = 2; radius <= 8; radius++) {
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
        persist();
    }

    private static synchronized void sanitize() {
        LEVELS.keySet().removeIf(uuid -> !CLASSES.containsKey(uuid));
        KILLS.keySet().removeIf(uuid -> !CLASSES.containsKey(uuid));
        for (UUID uuid : CLASSES.keySet()) {
            LEVELS.put(uuid, Math.max(1, Math.min(5, LEVELS.getOrDefault(uuid, 1))));
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

    public enum MercenaryClass {
        BASTION("bastion", "방벽 수호병", "높은 생존력과 저지력으로 성문과 시설 앞을 버팁니다."),
        STRIKER("striker", "돌격 집행관", "공격력과 기동성이 높아 전열을 빠르게 정리합니다."),
        RANGER("ranger", "성루 명사수", "원거리에서 적을 자동 사격하며 후방을 지원합니다."),
        MEDIC("medic", "전장 치유사", "주변 플레이어와 용병을 주기적으로 회복합니다.");

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
