package kr.moonseungjun.titanbreak.world;

import kr.moonseungjun.titanbreak.entity.ApexStalkerEntity;
import kr.moonseungjun.titanbreak.entity.BulwarkEntity;
import kr.moonseungjun.titanbreak.entity.BurrowerEntity;
import kr.moonseungjun.titanbreak.entity.BurstlingEntity;
import kr.moonseungjun.titanbreak.entity.ChronoHoundEntity;
import kr.moonseungjun.titanbreak.entity.CinderEntity;
import kr.moonseungjun.titanbreak.entity.CrusherEntity;
import kr.moonseungjun.titanbreak.entity.GliderEntity;
import kr.moonseungjun.titanbreak.entity.HowlerEntity;
import kr.moonseungjun.titanbreak.entity.IronMawEntity;
import kr.moonseungjun.titanbreak.entity.JammerEntity;
import kr.moonseungjun.titanbreak.entity.NeedlerEntity;
import kr.moonseungjun.titanbreak.entity.NullEyeEntity;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import kr.moonseungjun.titanbreak.entity.RegrowerEntity;
import kr.moonseungjun.titanbreak.entity.RevenantEntity;
import kr.moonseungjun.titanbreak.entity.RipperEntity;
import kr.moonseungjun.titanbreak.entity.ShockChoirEntity;
import kr.moonseungjun.titanbreak.entity.SiphonEntity;
import kr.moonseungjun.titanbreak.entity.SkitterEntity;
import kr.moonseungjun.titanbreak.entity.SpitterEntity;
import kr.moonseungjun.titanbreak.entity.StalkerEntity;
import kr.moonseungjun.titanbreak.entity.VoltaicEntity;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EncounterDirector {
    private static final int NORMAL_CAP = 5;
    private static final int ELITE_CAP = 2;
    private static final long INTRO_DELAY = 100L;
    private static final long NORMAL_MIN_DELAY = 360L;
    private static final long NORMAL_DELAY_JITTER = 280L;
    private static final long ELITE_MIN_DELAY = 1_600L;
    private static final long ELITE_DELAY_JITTER = 1_000L;
    private static final long BOSS_WARNING_DELAY = 220L;

    private static final String[] NORMAL_SPECIES = {
            "ripper", "skitter", "bulwark", "spitter", "needler", "glider", "howler", "jammer",
            "voltaic", "cinder", "regrower", "burrower", "crusher", "stalker", "burstling", "siphon"
    };
    private static final String[] ELITE_SPECIES = {
            "chrono_hound", "null_eye", "iron_maw", "revenant", "apex_stalker", "shock_choir"
    };

    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();
    private EncounterDirector() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State progression) {
        if (!(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()
                || level.getDifficulty() == Difficulty.PEACEFUL) return;
        long now = level.getGameTime();
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState(
                now + INTRO_DELAY, now + 240L + player.getRandom().nextInt(180),
                now + ELITE_MIN_DELAY + player.getRandom().nextInt(500)));
        if (!runtime.introSent && now >= runtime.introTick) {
            runtime.introSent = true;
            player.sendSystemMessage(Component.translatable("message.titanbreak.hunt_intro"));
        }
        if (bossReady(progression) && !progression.hasBossFirstKill("the_pursuer")) tickBoss(player, level, runtime, now);
        if (player.getHealth() <= player.getMaxHealth() * 0.35F || now < runtime.nextNormalTick) return;

        int nearbyNormal = countNearby(level, player, false, 72.0D);
        int nearbyElite = countNearby(level, player, true, 88.0D);
        boolean elitesUnlocked = progression.normalFirstKillCount() >= NORMAL_SPECIES.length && !progression.installedView().isEmpty();
        if (elitesUnlocked && progression.eliteFirstKillCount() < ELITE_SPECIES.length
                && now >= runtime.nextEliteTick && nearbyElite < ELITE_CAP) {
            String species = chooseUndiscoveredElite(player, progression);
            if (spawnSpecies(level, player, species, 28, 40)) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.elite_signature",
                        Component.translatable("entity.titanbreak." + species)), true);
                runtime.nextEliteTick = now + ELITE_MIN_DELAY + player.getRandom().nextInt((int) ELITE_DELAY_JITTER + 1);
                runtime.nextNormalTick = now + 420L;
                return;
            }
        }

        if (nearbyNormal >= NORMAL_CAP) {
            runtime.nextNormalTick = now + 180L;
            return;
        }
        int room = NORMAL_CAP - nearbyNormal;
        int desired = progression.normalFirstKillCount() < NORMAL_SPECIES.length ? 1
                : Math.min(room, 1 + player.getRandom().nextInt(progression.adaptationLevel() >= 4 ? 3 : 2));
        int spawned = 0;
        String firstSpecies = null;
        for (int i = 0; i < desired; i++) {
            String species = chooseNormal(player, progression, i == 0);
            if (spawnSpecies(level, player, species, 22, 36)) {
                if (firstSpecies == null) firstSpecies = species;
                spawned++;
            }
        }
        if (spawned > 0 && firstSpecies != null && !progression.hasNormalFirstKill(firstSpecies)) {
            player.sendSystemMessage(Component.translatable("message.titanbreak.new_signature",
                    Component.translatable("entity.titanbreak." + firstSpecies)), true);
        }
        runtime.nextNormalTick = now + NORMAL_MIN_DELAY + player.getRandom().nextInt((int) NORMAL_DELAY_JITTER + 1);
    }

    private static void tickBoss(ServerPlayer player, ServerLevel level, RuntimeState runtime, long now) {
        if (hasNearbyPursuer(level, player)) { runtime.bossSpawnTick = -1L; return; }
        if (runtime.bossSpawnTick == 0L) {
            runtime.bossSpawnTick = now + BOSS_WARNING_DELAY;
            player.sendSystemMessage(Component.translatable("message.titanbreak.pursuer_warning"));
            return;
        }
        if (runtime.bossSpawnTick > 0L && now >= runtime.bossSpawnTick) {
            if (spawnSpecies(level, player, "the_pursuer", 44, 58)) {
                player.sendSystemMessage(Component.translatable("message.titanbreak.pursuer_arrival"));
                runtime.bossSpawnTick = -1L;
                runtime.nextNormalTick = now + 1_200L;
            } else runtime.bossSpawnTick = now + 100L;
        }
    }

    private static boolean bossReady(TitanPlayerData.State progression) {
        return progression.normalFirstKillCount() >= NORMAL_SPECIES.length
                && progression.eliteFirstKillCount() >= ELITE_SPECIES.length
                && progression.installedView().size() >= 2 && progression.adaptationLevel() >= 4;
    }

    private static String chooseNormal(ServerPlayer player, TitanPlayerData.State progression, boolean preferUndiscovered) {
        if (preferUndiscovered) {
            List<String> unseen = new ArrayList<>();
            for (String species : NORMAL_SPECIES) if (!progression.hasNormalFirstKill(species)) unseen.add(species);
            if (!unseen.isEmpty()) return unseen.get(player.getRandom().nextInt(unseen.size()));
        }
        return NORMAL_SPECIES[player.getRandom().nextInt(NORMAL_SPECIES.length)];
    }

    private static String chooseUndiscoveredElite(ServerPlayer player, TitanPlayerData.State progression) {
        List<String> unseen = new ArrayList<>();
        for (String species : ELITE_SPECIES) if (!progression.hasEliteFirstKill(species)) unseen.add(species);
        if (!unseen.isEmpty()) return unseen.get(player.getRandom().nextInt(unseen.size()));
        return ELITE_SPECIES[player.getRandom().nextInt(ELITE_SPECIES.length)];
    }

    private static boolean spawnSpecies(ServerLevel level, ServerPlayer player, String species, int minRange, int maxRange) {
        EntityType<?> type = switch (species) {
            case "ripper" -> ModEntities.RIPPER.get(); case "skitter" -> ModEntities.SKITTER.get();
            case "bulwark" -> ModEntities.BULWARK.get(); case "spitter" -> ModEntities.SPITTER.get();
            case "needler" -> ModEntities.NEEDLER.get(); case "glider" -> ModEntities.GLIDER.get();
            case "howler" -> ModEntities.HOWLER.get(); case "jammer" -> ModEntities.JAMMER.get();
            case "voltaic" -> ModEntities.VOLTAIC.get(); case "cinder" -> ModEntities.CINDER.get();
            case "regrower" -> ModEntities.REGROWER.get(); case "burrower" -> ModEntities.BURROWER.get();
            case "crusher" -> ModEntities.CRUSHER.get(); case "stalker" -> ModEntities.STALKER.get();
            case "burstling" -> ModEntities.BURSTLING.get(); case "siphon" -> ModEntities.SIPHON.get();
            case "chrono_hound" -> ModEntities.CHRONO_HOUND.get(); case "null_eye" -> ModEntities.NULL_EYE.get();
            case "iron_maw" -> ModEntities.IRON_MAW.get(); case "revenant" -> ModEntities.REVENANT.get();
            case "apex_stalker" -> ModEntities.APEX_STALKER.get(); case "shock_choir" -> ModEntities.SHOCK_CHOIR.get();
            case "the_pursuer" -> ModEntities.THE_PURSUER.get(); default -> null;
        };
        if (type == null) return false;
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            int range = minRange + player.getRandom().nextInt(Math.max(1, maxRange - minRange + 1));
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * range);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * range);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if ("glider".equals(species)) y += 5 + player.getRandom().nextInt(4);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getWorldBorder().isWithinBounds(pos)) continue;
            Entity entity = type.create(level, EntitySpawnReason.EVENT);
            if (!(entity instanceof Mob mob)) return false;
            mob.setPos(x + 0.5D, y, z + 0.5D);
            mob.setYRot(player.getRandom().nextFloat() * 360.0F);
            if (!level.noCollision(mob)) continue;
            mob.setTarget(player);
            return level.addFreshEntity(mob);
        }
        return false;
    }

    private static int countNearby(ServerLevel level, ServerPlayer player, boolean elites, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && (elites ? isElite(living) : isNormal(living))).size();
    }

    private static boolean hasNearbyPursuer(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(PursuerEntity.class, player.getBoundingBox().inflate(180.0D), Entity::isAlive).isEmpty();
    }

    private static boolean isNormal(LivingEntity entity) {
        return entity instanceof RipperEntity || entity instanceof SkitterEntity || entity instanceof BulwarkEntity
                || entity instanceof SpitterEntity || entity instanceof NeedlerEntity || entity instanceof GliderEntity
                || entity instanceof HowlerEntity || entity instanceof JammerEntity || entity instanceof VoltaicEntity
                || entity instanceof CinderEntity || entity instanceof RegrowerEntity || entity instanceof BurrowerEntity
                || entity instanceof CrusherEntity || entity instanceof StalkerEntity || entity instanceof BurstlingEntity
                || entity instanceof SiphonEntity;
    }

    private static boolean isElite(LivingEntity entity) {
        return entity instanceof ChronoHoundEntity || entity instanceof NullEyeEntity || entity instanceof IronMawEntity
                || entity instanceof RevenantEntity || entity instanceof ApexStalkerEntity || entity instanceof ShockChoirEntity;
    }

    public static void clear(UUID playerId) { RUNTIME.remove(playerId); }
    public static void clearAll() { RUNTIME.clear(); }

    private static final class RuntimeState {
        private final long introTick; private boolean introSent; private long nextNormalTick; private long nextEliteTick; private long bossSpawnTick;
        private RuntimeState(long introTick, long nextNormalTick, long nextEliteTick) {
            this.introTick = introTick; this.nextNormalTick = nextNormalTick; this.nextEliteTick = nextEliteTick;
        }
    }
}
