package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
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

import java.util.Locale;

/** Persistent classed mercenaries. Entity tags preserve class, level and kills across reloads. */
public final class VillageMercenarySystem {
    private static final String TAG = "vg_mercenary";
    private static final String CLASS_PREFIX = "vg_merc_class_";
    private static final String LEVEL_PREFIX = "vg_merc_level_";
    private static final String KILLS_PREFIX = "vg_merc_kills_";
    private static int tickCounter;

    private VillageMercenarySystem() {}

    public static void reset() { tickCounter = 0; }

    public static boolean recognize(Mob mob) {
        if (!(mob instanceof IronGolem) || !mob.getTags().contains(TAG)) return false;
        mob.setPersistenceRequired();
        VillageWorldSystem.markAllowedGameMob(mob);
        refreshName(mob);
        return true;
    }

    public static int hireCost(MercenaryClass kind) {
        if (kind == null) return 0;
        return 150 + kind.ordinal() * 35 + VillageProgressionSystem.barracksLevel() * 25;
    }

    public static String hire(ServerPlayer player, MercenaryClass kind) {
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
        mercenary.addTag(TAG);
        mercenary.addTag(CLASS_PREFIX + kind.id());
        mercenary.addTag(LEVEL_PREFIX + "1");
        mercenary.addTag(KILLS_PREFIX + "0");
        applyClassPassives(mercenary, kind, 1);
        refreshName(mercenary);
        VillageWorldSystem.markAllowedGameMob(mercenary);
        if (!level.addFreshEntity(mercenary)) {
            VillageWorldSystem.unmarkAllowedGameMob(mercenary.getUUID());
            VillageProgressionSystem.addCoins(player, cost, "용병 배치 실패 환불");
            return "용병 배치에 실패해 주화를 돌려드렸습니다.";
        }
        return kind.displayName() + " 고용 완료 · Lv.1 · 현재 " + (current + 1) + " / " + cap
                + " · 생존하는 동안 저장과 재접속 후에도 유지됩니다.";
    }

    public static void tick(MinecraftServer server) {
        if (++tickCounter < 20) return;
        tickCounter = 0;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96, VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (IronGolem mercenary : level.getEntitiesOfClass(IronGolem.class, area,
                entity -> entity.getTags().contains(TAG))) {
            recognize(mercenary);
            MercenaryClass kind = mercenaryClass(mercenary);
            int rank = rank(mercenary);
            applyClassPassives(mercenary, kind, rank);
            if (!VillageRaidSystem.isActive()) continue;
            if (kind == MercenaryClass.RANGER) rangedAttack(level, mercenary, rank);
            else if (kind == MercenaryClass.MEDIC) healAllies(level, server, mercenary, rank);
        }
    }

    public static void awardKillExperience(MinecraftServer server, Vec3 deathPosition) {
        if (server == null || deathPosition == null) return;
        ServerLevel level = server.overworld();
        AABB area = new AABB(deathPosition, deathPosition).inflate(48.0);
        for (IronGolem mercenary : level.getEntitiesOfClass(IronGolem.class, area,
                entity -> entity.getTags().contains(TAG) && entity.isAlive())) {
            int kills = kills(mercenary) + 1;
            int currentRank = rank(mercenary);
            int nextRank = Math.min(5, 1 + kills / 8);
            replaceNumericTag(mercenary, KILLS_PREFIX, kills);
            if (nextRank > currentRank) {
                replaceNumericTag(mercenary, LEVEL_PREFIX, nextRank);
                applyClassPassives(mercenary, mercenaryClass(mercenary), nextRank);
                refreshName(mercenary);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        mercenary.getX(), mercenary.getY() + 1.3, mercenary.getZ(),
                        16, 0.45, 0.7, 0.45, 0.05);
            }
        }
    }

    public static String status(MinecraftServer server) {
        if (server == null) return "용병 상태를 확인할 수 없습니다.";
        ServerLevel level = server.overworld();
        return "용병 " + count(level) + " / " + capacity()
                + " · 용병 교리 Lv." + VillageDefenseResearchSystem.level(VillageDefenseResearchSystem.Branch.MERCENARY)
                + " · 적 처치 경험으로 최대 Lv.5까지 성장";
    }

    public static int capacity() {
        return 1 + VillageProgressionSystem.barracksLevel() / 2
                + VillageDefenseResearchSystem.mercenaryCapacityBonus();
    }

    private static int count(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return 0;
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96, VillageWorldSystem.BATTLEFIELD_RADIUS);
        return level.getEntitiesOfClass(IronGolem.class, area, entity -> entity.getTags().contains(TAG)).size();
    }

    private static void rangedAttack(ServerLevel level, IronGolem mercenary, int rank) {
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, mercenary.blockPosition(), 42.0 + rank * 3.0);
        if (target == null) return;
        float damage = (3.0f + rank * 1.3f) * VillageDefenseResearchSystem.mercenaryDamageMultiplier();
        Vec3 start = mercenary.position().add(0, 1.8, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        for (int i = 0; i <= 10; i++) {
            Vec3 point = start.lerp(end, i / 10.0);
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);
    }

    private static void healAllies(ServerLevel level, MinecraftServer server, IronGolem medic, int rank) {
        float amount = 1.5f + rank * 0.8f;
        AABB area = medic.getBoundingBox().inflate(8.0 + rank);
        for (IronGolem ally : level.getEntitiesOfClass(IronGolem.class, area,
                entity -> entity.getTags().contains(TAG) && entity.isAlive())) {
            ally.heal(amount);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(medic) <= (8.0 + rank) * (8.0 + rank)
                    && !VillageRespawnSystem.isDowned(player)) player.heal(amount * 0.65f);
        }
        level.sendParticles(ParticleTypes.HEART, medic.getX(), medic.getY() + 1.4, medic.getZ(),
                4 + rank, 0.7, 0.5, 0.7, 0.02);
    }

    private static void applyClassPassives(IronGolem mercenary, MercenaryClass kind, int rank) {
        int duration = 20 * 60 * 60;
        if (kind == MercenaryClass.BASTION) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, Math.min(2, rank / 2), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.min(3, rank - 1), false, false));
        } else if (kind == MercenaryClass.STRIKER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, Math.min(2, rank / 2), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, Math.min(1, rank / 3), false, false));
        } else if (kind == MercenaryClass.RANGER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, false));
        } else if (kind == MercenaryClass.MEDIC) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, Math.min(1, rank / 3), false, false));
        }
    }

    private static MercenaryClass mercenaryClass(Mob mob) {
        for (String tag : mob.getTags()) {
            if (tag.startsWith(CLASS_PREFIX)) return MercenaryClass.fromId(tag.substring(CLASS_PREFIX.length()));
        }
        return MercenaryClass.BASTION;
    }

    private static int rank(Mob mob) { return numericTag(mob, LEVEL_PREFIX, 1); }
    private static int kills(Mob mob) { return numericTag(mob, KILLS_PREFIX, 0); }

    private static int numericTag(Mob mob, String prefix, int fallback) {
        for (String tag : mob.getTags()) {
            if (!tag.startsWith(prefix)) continue;
            try { return Integer.parseInt(tag.substring(prefix.length())); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    private static void replaceNumericTag(Mob mob, String prefix, int value) {
        String old = null;
        for (String tag : mob.getTags()) if (tag.startsWith(prefix)) { old = tag; break; }
        if (old != null) mob.removeTag(old);
        mob.addTag(prefix + value);
    }

    private static void refreshName(Mob mob) {
        MercenaryClass kind = mercenaryClass(mob);
        mob.setCustomName(Component.literal(kind.displayName() + " Lv." + rank(mob)));
        mob.setCustomNameVisible(true);
    }

    private static BlockPos safeSpawn(ServerLevel level, BlockPos origin) {
        for (int radius = 2; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, 0, dz);
                    if (level.getBlockState(pos.below()).isSolidRender()
                            && level.getBlockState(pos).isAir()
                            && level.getBlockState(pos.above()).isAir()) return pos;
                }
            }
        }
        return origin.above();
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
