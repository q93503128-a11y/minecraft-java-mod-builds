package kr.moonseungjun.riftfrontier.expedition;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Server-authoritative technical combat adapter for Region 01.
 *
 * Vanilla mobs are deliberately used as behaviour proxies during M2. Their silhouettes, names and
 * presentation are not production art. What is production-relevant here is encounter composition,
 * pressure scaling, extraction choice pressure and the replaceable role boundary.
 *
 * Runtime threat ownership is tracked by expedition sequence rather than by a spatial query. A patrol
 * therefore cannot be "cleared" merely by luring a live proxy outside the technical cell, and terminal
 * cleanup can still discard a tracked proxy after it has moved away from its spawn center.
 */
public final class Region01EncounterRuntime {
    private static final String RUN_TAG_PREFIX = "riftfrontier.region01.run.";
    private static final String ROLE_TAG_PREFIX = "riftfrontier.region01.role.";
    private static final String ROLE_HUNTER = "hunter";
    private static final String ROLE_SCOUT = "scout";
    private static final String ROLE_ELITE = "elite_anchor";

    /**
     * M2 has exactly one authoritative non-terminal expedition. Direct handles make same-process
     * ownership independent of movement. They intentionally do not survive a JVM/server restart.
     * Persisted tagged proxies from a previous process are rejected event-by-event when they load;
     * no global or per-tick world scan is required.
     */
    private static final Map<Long, List<Mob>> RUN_THREATS = new HashMap<>();

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
        RUN_THREATS.put(runSequence, new ArrayList<>());
        EncounterPlan plan = planForPressure(pressure);

        for (int i = 0; i < plan.hunters(); i++) {
            Zombie hunter = new Zombie(level);
            spawn(level, hunter, center.offset(-4 + (i * 2), 0, 3), runSequence, ROLE_HUNTER);
        }
        for (int i = 0; i < plan.scouts(); i++) {
            Skeleton scout = new Skeleton(EntityTypes.SKELETON, level);
            scout.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            spawn(level, scout, center.offset(4 - (i * 2), 0, -3), runSequence, ROLE_SCOUT);
        }

        Ravager elite = new Ravager(EntityTypes.RAVAGER, level);
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
        List<Mob> tracked = RUN_THREATS.remove(runSequence);
        if (tracked == null) return;
        for (Mob mob : tracked) {
            if (!mob.isRemoved()) mob.discard();
        }
        tracked.clear();
    }

    /**
     * Event-driven restart cleanup. A tagged Region 01 proxy is valid only when this server process
     * owns a live direct tracker for its run sequence. Persisted proxies loaded after a restart have
     * tags but no process-local tracker, so they are discarded immediately as stale technical state.
     */
    public static boolean discardIfOrphaned(Entity entity) {
        OptionalLong runSequence = taggedRunSequence(entity);
        if (runSequence.isEmpty()) return false;
        if (RUN_THREATS.containsKey(runSequence.getAsLong())) return false;
        entity.discard();
        return true;
    }

    /** Exposed for deterministic regression coverage of the stable run ownership tag. */
    public static OptionalLong taggedRunSequence(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(RUN_TAG_PREFIX)) continue;
            String suffix = tag.substring(RUN_TAG_PREFIX.length());
            try {
                long sequence = Long.parseLong(suffix);
                return sequence > 0L ? OptionalLong.of(sequence) : OptionalLong.empty();
            } catch (NumberFormatException ignored) {
                return OptionalLong.empty();
            }
        }
        return OptionalLong.empty();
    }

    private static List<Mob> liveThreats(ServerLevel level, BlockPos center, long runSequence) {
        List<Mob> tracked = RUN_THREATS.get(runSequence);
        if (tracked == null || tracked.isEmpty()) return List.of();
        return tracked.stream()
            .filter(mob -> mob.level() == level)
            .filter(Mob::isAlive)
            .filter(mob -> !mob.isRemoved())
            .toList();
    }

    private static void spawn(ServerLevel level, Mob mob, BlockPos pos, long runSequence, String role) {
        mob.setPersistenceRequired();
        mob.addTag(runTag(runSequence));
        mob.addTag(ROLE_TAG_PREFIX + role);
        mob.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        if (!level.addFreshEntity(mob)) throw new IllegalStateException("Minecraft rejected Region 01 encounter spawn for role " + role);
        RUN_THREATS.computeIfAbsent(runSequence, ignored -> new ArrayList<>()).add(mob);
    }

    private static String runTag(long runSequence) {
        return RUN_TAG_PREFIX + runSequence;
    }
}
