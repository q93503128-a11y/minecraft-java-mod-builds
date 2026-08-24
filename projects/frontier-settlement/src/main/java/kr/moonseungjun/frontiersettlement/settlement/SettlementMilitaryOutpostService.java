package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.content.FrontierSoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loaded-only dangerous-region military overlay for otherwise-general outposts. */
public final class SettlementMilitaryOutpostService {
    public static final String MILITARY_SENTRY_TAG = "frontier_settlement_military_outpost_sentry";
    public static final String MILITARY_OUTPOST_TAG_PREFIX = "frontier_settlement_military_outpost_";
    public static final long RECRUIT_FOOD_COST = 6L;
    public static final long RECRUIT_METAL_COST = 2L;
    public static final int TARGET_FOOD_RESERVE = 12;
    public static final int TARGET_METAL_RESERVE = 4;

    private static final int DANGER_RADIUS = 28;
    private static final int CLOSE_THREAT_RADIUS = 16;
    private static final int PATROL_RADIUS = 24;
    private static final int LOADED_MARGIN = 32;
    private static final int PATROL_INTERVAL_TICKS = 40;
    private static final int RECRUIT_INTERVAL_TICKS = 600;
    private static final double SENTRY_SEARCH_RADIUS = 32.0D;
    private static final double HOME_RADIUS_SQR = 8.0D * 8.0D;
    private static final double LEASH_RADIUS_SQR = PATROL_RADIUS * PATROL_RADIUS;

    private static final int[][] DANGER_SAMPLES = {
            {0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8},
            {8, 8}, {8, -8}, {-8, 8}, {-8, -8}
    };

    private SettlementMilitaryOutpostService() {}

    public record DangerEvidence(boolean loaded, int hostiles, int closeHostiles,
                                 int threatKinds, int enclosedDarkSamples) {
        public boolean dangerous() {
            if (!loaded) return false;
            boolean concentratedSwarm = hostiles >= 5 && closeHostiles >= 3;
            boolean mixedPressure = hostiles >= 4 && closeHostiles >= 2 && threatKinds >= 2;
            boolean entrenchedDarkness = hostiles >= 3 && closeHostiles >= 1 && enclosedDarkSamples >= 3;
            return concentratedSwarm || mixedPressure || entrenchedDarkness;
        }
    }

    public record SupplySnapshot(long food, long metal) {}

    public static void tick(MinecraftServer server, SettlementData data) {
        int tick = server.getTickCount();
        if (tick % 20 != 0) return;
        ServerLevel level = server.overworld();
        for (OutpostRecord outpost : data.outposts()) {
            if (!"general".equals(outpost.specialization())) continue;
            DangerEvidence evidence = dangerEvidence(level, outpost);
            if (!evidence.loaded()) continue;

            FrontierSoldierEntity sentry = findSentry(level, outpost);
            if (!evidence.dangerous()) {
                if (sentry != null && tick % PATROL_INTERVAL_TICKS == 0) standDown(outpost, sentry);
                continue;
            }

            if (sentry == null && tick % RECRUIT_INTERVAL_TICKS == 0) sentry = tryRecruit(level, outpost);
            if (sentry != null && tick % PATROL_INTERVAL_TICKS == 0) patrol(level, outpost, sentry);
        }
    }

    public static boolean isActiveMilitaryOutpost(ServerLevel level, OutpostRecord outpost) {
        return "general".equals(outpost.specialization()) && dangerEvidence(level, outpost).dangerous();
    }

    public static int activeMilitaryOutpostCount(ServerLevel level, SettlementData data) {
        int count = 0;
        for (OutpostRecord outpost : data.outposts()) if (isActiveMilitaryOutpost(level, outpost)) count++;
        return count;
    }

    public static int loadedSentryCount(ServerLevel level, SettlementData data) {
        int count = 0;
        for (OutpostRecord outpost : data.outposts()) {
            if (!militaryAreaLoaded(level, outpost)) continue;
            if (findSentry(level, outpost) != null) count++;
        }
        return count;
    }

    public static SupplySnapshot activeSupplySnapshot(ServerLevel level, SettlementData data) {
        long food = 0L;
        long metal = 0L;
        for (OutpostRecord outpost : data.outposts()) {
            if (!isActiveMilitaryOutpost(level, outpost)) continue;
            if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) continue;
            food += SettlementInventory.countFood(container);
            metal += countMatching(container, SettlementStorageService::isMetalStack);
        }
        return new SupplySnapshot(food, metal);
    }

    public static int foodSupplyShortage(ServerLevel level, OutpostRecord outpost) {
        if (!isActiveMilitaryOutpost(level, outpost)) return 0;
        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return 0;
        return Math.max(0, TARGET_FOOD_RESERVE - (int) Math.min(Integer.MAX_VALUE, SettlementInventory.countFood(container)));
    }

    public static int metalSupplyShortage(ServerLevel level, OutpostRecord outpost) {
        if (!isActiveMilitaryOutpost(level, outpost)) return 0;
        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return 0;
        long present = countMatching(container, SettlementStorageService::isMetalStack);
        return Math.max(0, TARGET_METAL_RESERVE - (int) Math.min(Integer.MAX_VALUE, present));
    }

    public static DangerEvidence dangerEvidence(ServerLevel level, OutpostRecord outpost) {
        if (!militaryAreaLoaded(level, outpost)) return new DangerEvidence(false, 0, 0, 0, 0);
        BlockPos center = outpost.center();
        AABB area = new AABB(center).inflate(DANGER_RADIUS, 12.0D, DANGER_RADIUS);
        List<Monster> threats = level.getEntitiesOfClass(Monster.class, area, Monster::isAlive);
        int close = 0;
        Set<Class<?>> threatClasses = new HashSet<>();
        double closeRadiusSqr = (double) CLOSE_THREAT_RADIUS * CLOSE_THREAT_RADIUS;
        for (Monster threat : threats) {
            threatClasses.add(threat.getClass());
            if (threat.distanceToSqr(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D) <= closeRadiusSqr) close++;
        }

        int enclosedDark = 0;
        for (int[] sampleOffset : DANGER_SAMPLES) {
            BlockPos sample = center.offset(sampleOffset[0], 1, sampleOffset[1]);
            if (!level.canSeeSky(sample) && level.getBrightness(LightLayer.BLOCK, sample) <= 7) enclosedDark++;
        }
        return new DangerEvidence(true, threats.size(), close, threatClasses.size(), enclosedDark);
    }

    /** Military sentries are combat/service units and never item/iron farms. */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().entityTags().contains(MILITARY_SENTRY_TAG)) event.getDrops().clear();
    }

    private static FrontierSoldierEntity tryRecruit(ServerLevel level, OutpostRecord outpost) {
        if (!isActiveMilitaryOutpost(level, outpost) || findSentry(level, outpost) != null) return null;
        if (!(level.getBlockEntity(outpost.stockpile()) instanceof Container container)) return null;
        if (SettlementInventory.countFood(container) < RECRUIT_FOOD_COST
                || countMatching(container, SettlementStorageService::isMetalStack) < RECRUIT_METAL_COST) return null;

        BlockPos home = outpost.center().above();
        if (!level.hasChunkAt(home)) return null;
        FrontierSoldierEntity sentry = new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level);
        sentry.setPos(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        sentry.setCustomName(Component.literal("전초 수비대 #" + outpost.id()));
        sentry.setCustomNameVisible(true);
        sentry.setPersistenceRequired();
        sentry.setPlayerCreated(true);
        sentry.addTag(MILITARY_SENTRY_TAG);
        sentry.addTag(assignmentTag(outpost));
        if (!level.addFreshEntity(sentry)) return null;

        if (!consumeLocalSupply(container, RECRUIT_FOOD_COST, RECRUIT_METAL_COST)) {
            sentry.discard();
            return null;
        }
        return sentry;
    }

    private static void patrol(ServerLevel level, OutpostRecord outpost, FrontierSoldierEntity sentry) {
        BlockPos home = outpost.center().above();
        double homeDistance = sentry.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (homeDistance > LEASH_RADIUS_SQR) {
            if (sentry.getTarget() != null) sentry.setTarget(null);
            sentry.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.96D);
            return;
        }
        Monster threat = nearestCombatThreat(level, outpost.center());
        if (threat != null) { sentry.setTarget(threat); return; }
        standDown(outpost, sentry);
    }

    private static Monster nearestCombatThreat(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(DANGER_RADIUS, 12.0D, DANGER_RADIUS);
        return level.getEntitiesOfClass(Monster.class, area,
                        monster -> monster.isAlive() && !(monster instanceof Creeper)).stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(
                        center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D))).orElse(null);
    }

    private static void standDown(OutpostRecord outpost, FrontierSoldierEntity sentry) {
        if (sentry.getTarget() != null) sentry.setTarget(null);
        BlockPos home = outpost.center().above();
        if (sentry.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D) > HOME_RADIUS_SQR) {
            sentry.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.88D);
        } else sentry.getNavigation().stop();
    }

    private static FrontierSoldierEntity findSentry(ServerLevel level, OutpostRecord outpost) {
        String assignment = assignmentTag(outpost);
        AABB search = new AABB(outpost.center()).inflate(SENTRY_SEARCH_RADIUS, 16.0D, SENTRY_SEARCH_RADIUS);
        List<FrontierSoldierEntity> sentries = level.getEntitiesOfClass(FrontierSoldierEntity.class, search,
                sentry -> sentry.entityTags().contains(MILITARY_SENTRY_TAG) && sentry.entityTags().contains(assignment));
        if (!sentries.isEmpty()) return sentries.getFirst();

        // Alpha.41 save migration: presentation/body replacement is 1:1 and never charges recruitment again.
        List<IronGolem> legacy = level.getEntitiesOfClass(IronGolem.class, search,
                sentry -> !(sentry instanceof FrontierSoldierEntity)
                        && sentry.entityTags().contains(MILITARY_SENTRY_TAG)
                        && sentry.entityTags().contains(assignment));
        return legacy.isEmpty() ? null : migrateLegacySentry(level, legacy.getFirst());
    }

    private static FrontierSoldierEntity migrateLegacySentry(ServerLevel level, IronGolem legacy) {
        FrontierSoldierEntity replacement = new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level);
        replacement.setPos(legacy.getX(), legacy.getY(), legacy.getZ());
        replacement.setYRot(legacy.getYRot());
        replacement.setXRot(legacy.getXRot());
        replacement.setCustomName(legacy.getCustomName());
        replacement.setCustomNameVisible(legacy.isCustomNameVisible());
        replacement.setPersistenceRequired();
        replacement.setPlayerCreated(true);
        for (String tag : legacy.entityTags()) replacement.addTag(tag);
        replacement.setHealth(Math.min(replacement.getMaxHealth(), legacy.getHealth()));
        if (!level.addFreshEntity(replacement)) return null;
        legacy.discard();
        return replacement;
    }

    private static String assignmentTag(OutpostRecord outpost) {
        return MILITARY_OUTPOST_TAG_PREFIX + outpost.id();
    }

    private static boolean militaryAreaLoaded(ServerLevel level, OutpostRecord outpost) {
        if (!level.hasChunkAt(outpost.center()) || !level.hasChunkAt(outpost.stockpile())) return false;
        BlockPos center = outpost.center();
        for (int dx = -LOADED_MARGIN; dx <= LOADED_MARGIN; dx += 16) {
            for (int dz = -LOADED_MARGIN; dz <= LOADED_MARGIN; dz += 16) {
                if (!level.hasChunkAt(center.offset(dx, 0, dz))) return false;
            }
        }
        return true;
    }

    private static boolean consumeLocalSupply(Container container, long food, long metal) {
        if (SettlementInventory.countFood(container) < food
                || countMatching(container, SettlementStorageService::isMetalStack) < metal) return false;
        consumeMatching(container, food, SettlementInventory::isFood);
        consumeMatching(container, metal, SettlementStorageService::isMetalStack);
        container.setChanged();
        return true;
    }

    private static long countMatching(Container container, java.util.function.Predicate<ItemStack> predicate) {
        long total = 0L;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) total += stack.getCount();
        }
        return total;
    }

    private static void consumeMatching(Container container, long amount,
                                        java.util.function.Predicate<ItemStack> predicate) {
        long left = amount;
        for (int slot = 0; slot < container.getContainerSize() && left > 0L; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) continue;
            int take = (int) Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
    }
}
