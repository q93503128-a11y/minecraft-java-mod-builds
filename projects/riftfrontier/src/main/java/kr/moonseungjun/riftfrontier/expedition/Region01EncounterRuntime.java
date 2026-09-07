package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Server-authoritative technical combat adapter for Region 01.
 *
 * Vanilla mobs are deliberately used as behaviour proxies during M2. Their silhouettes, names and
 * presentation are not production art. What is production-relevant here is encounter composition,
 * pressure scaling, extraction choice pressure and the replaceable role boundary.
 */
public final class Region01EncounterRuntime {
    private static final String RUN_TAG_PREFIX = "riftfrontier.region01.run.";
    private static final String ROLE_TAG_PREFIX = "riftfrontier.region01.role.";
    private static final String ROLE_HUNTER = "hunter";
    private static final String ROLE_SCOUT = "scout";
    private static final String ROLE_ELITE = "elite_anchor";

    private Region01EncounterRuntime() {}

    public record EncounterPlan(int hunters, int scouts, int elites, int hazardTicks, int hazardAmplifier) {
        public int totalThreats() { return hunters + scouts + elites; }
    }

    public static EncounterPlan planForPressure(int pressure) {
        int normalized = Math.max(0, pressure);
        int hunters = 1 + Math.min(2, normalized / 2);
        int scouts = 1 + Math.min(2, normalized / 3);
        int elites = 1;
        int hazardTicks = 60 + Math.min(180, normalized * 20);
        int hazardAmplifier = Math.min(2, normalized / 3);
        return new EncounterPlan(hunters, scouts, elites, hazardTicks, hazardAmplifier);
    }

    public static EncounterPlan begin(ServerLevel level, BlockPos center, long runSequence, int pressure) {
        clearRun(level, center, runSequence);
        EncounterPlan plan = planForPressure(pressure);

        for (int i = 0; i < plan.hunters(); i++) {
            Zombie hunter = new Zombie(EntityType.ZOMBIE, level);
            spawn(level, hunter, center.offset(-4 + (i * 2), 0, 3), runSequence, ROLE_HUNTER);
        }
        for (int i = 0; i < plan.scouts(); i++) {
            Skeleton scout = new Skeleton(EntityType.SKELETON, level);
            scout.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            spawn(level, scout, center.offset(4 - (i * 2), 0, -3), runSequence, ROLE_SCOUT);
        }

        // Technical proxy for a heavy controller role. Ravager's native shield-stun window gives
        // this elite actual counterplay instead of a health multiplier. Final art/AI is an M3 gate.
        Ravager elite = new Ravager(EntityType.RAVAGER, level);
        spawn(level, elite, center.offset(0, 0, 2), runSequence, ROLE_ELITE);
        return plan;
    }

    public static void applySalvageHazard(ServerPlayer player, int pressure) {
        EncounterPlan plan = planForPressure(pressure);
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, plan.hazardTicks(), plan.hazardAmplifier()));
        player.sendSystemMessage(Component.literal(
            "[Riftfrontier] Rift drag pulse: movement impaired for " + plan.hazardTicks() + " ticks"
                + (plan.hazardAmplifier() > 0 ? " (intensity " + (plan.hazardAmplifier() + 1) + ")" : "")
                + ". Recovering salvage while patrols remain active is deliberately riskier."
        ));
    }

    public static int liveThreatCount(ServerLevel level, BlockPos center, long runSequence) {
        return liveThreats(level, center, runSequence).size();
    }

    public static boolean patrolCleared(ServerLevel level, BlockPos center, long runSequence) {
        return liveThreatCount(level, center, runSequence) == 0;
    }

    public static void clearRun(ServerLevel level, BlockPos center, long runSequence) {
        for (Mob mob : liveThreats(level, center, runSequence)) mob.discard();
    }

    private static List<Mob> liveThreats(ServerLevel level, BlockPos center, long runSequence) {
        String runTag = runTag(runSequence);
        AABB bounds = new AABB(center).inflate(16.0D, 8.0D, 16.0D);
        return level.getEntitiesOfClass(Mob.class, bounds, mob -> mob.isAlive() && mob.getTags().contains(runTag));
    }

    private static void spawn(ServerLevel level, Mob mob, BlockPos pos, long runSequence, String role) {
        mob.setPersistenceRequired();
        mob.addTag(runTag(runSequence));
        mob.addTag(ROLE_TAG_PREFIX + role);
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        if (!level.addFreshEntity(mob)) throw new IllegalStateException("Minecraft rejected Region 01 encounter spawn for role " + role);
    }

    private static String runTag(long runSequence) {
        return RUN_TAG_PREFIX + runSequence;
    }
}
