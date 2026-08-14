from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms"
HART = BASE / "entity/SilverHartEntity.java"
HOUND = BASE / "entity/AshHoundEntity.java"
WISP = BASE / "entity/RiverWispEntity.java"
ECO = BASE / "world/ErdenFantasyEcologyManager.java"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


hart = '''package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Northern Erden herd animal: wary of travellers and happiest close to its herd. */
public final class SilverHartEntity extends Goat {
    public SilverHartEntity(EntityType<? extends Goat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new AvoidEntityGoal<>(
                this, Player.class, 18.0F, 1.18D, 1.42D));
    }
}
'''
HART.write_text(hart, encoding="utf-8")

hound = '''package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Western Erden pack predator. It is wildlife, not a reskinned tameable wolf. */
public final class AshHoundEntity extends Wolf {
    public AshHoundEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.20D, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }
}
'''
HOUND.write_text(hound, encoding="utf-8")

wisp = '''package kr.moonseungjun.livingkingdoms.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Ambient Silver River spirit. Players cannot turn it into an Allay-style item courier. */
public final class RiverWispEntity extends Allay {
    public RiverWispEntity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }
}
'''
WISP.write_text(wisp, encoding="utf-8")

text = ECO.read_text(encoding="utf-8")
if "BEHAVIOR_INTERVAL" not in text:
    text = text.replace(
        "    private static final int SPAWN_INTERVAL = 100;\n",
        "    private static final int SPAWN_INTERVAL = 100;\n"
        "    private static final int BEHAVIOR_INTERVAL = 20;\n",
        1)

old_tick = '''        if (level.getGameTime() % SPAWN_INTERVAL != 0L) return;
        spawnAroundTravellers(level);'''
new_tick = '''        if (level.getGameTime() % BEHAVIOR_INTERVAL == 0L) {
            tickLoadedSpeciesBehavior(level);
        }
        if (level.getGameTime() % SPAWN_INTERVAL == 0L) {
            spawnAroundTravellers(level);
        }'''
if "tickLoadedSpeciesBehavior(level);" not in text:
    require(old_tick in text, "ecology spawn tick anchor missing")
    text = text.replace(old_tick, new_tick, 1)

if "private static void tickLoadedSpeciesBehavior(" not in text:
    anchor = '''    private static void spawnAroundTravellers(ServerLevel level) {
'''
    helper = '''    private static void tickLoadedSpeciesBehavior(ServerLevel level) {
        Set<UUID> handledHarts = new HashSet<>();
        Set<UUID> handledHounds = new HashSet<>();
        Set<UUID> handledWisps = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            AABB area = new AABB(
                    player.getX() - LOCAL_RADIUS, level.getMinY(), player.getZ() - LOCAL_RADIUS,
                    player.getX() + LOCAL_RADIUS, level.getMaxY(), player.getZ() + LOCAL_RADIUS);
            herdSilverHarts(level.getEntitiesOfClass(SilverHartEntity.class, area), player, handledHarts);
            coordinateAshHounds(level, level.getEntitiesOfClass(AshHoundEntity.class, area), player, handledHounds);
            bindRiverWisps(level, level.getEntitiesOfClass(RiverWispEntity.class, area), handledWisps);
        }
    }

    private static void herdSilverHarts(
            List<SilverHartEntity> harts,
            ServerPlayer player,
            Set<UUID> handled) {
        List<SilverHartEntity> herd = harts.stream()
                .filter(hart -> handled.add(hart.getUUID()))
                .sorted(java.util.Comparator.comparing(Entity::getUUID))
                .toList();
        if (herd.size() < 2) return;
        SilverHartEntity leader = herd.getFirst();
        for (int index = 1; index < herd.size(); index++) {
            SilverHartEntity follower = herd.get(index);
            if (follower.distanceToSqr(player) <= 18.0D * 18.0D) continue;
            if (follower.distanceToSqr(leader) > 12.0D * 12.0D) {
                follower.getNavigation().moveTo(leader, 0.90D);
            }
        }
    }

    private static void coordinateAshHounds(
            ServerLevel level,
            List<AshHoundEntity> hounds,
            ServerPlayer player,
            Set<UUID> handled) {
        if (hounds.isEmpty()) return;
        boolean huntingTime = level.isDarkOutside();
        AshHoundEntity leader = null;
        for (AshHoundEntity hound : hounds.stream()
                .sorted(java.util.Comparator.comparing(Entity::getUUID)).toList()) {
            if (!handled.add(hound.getUUID())) continue;
            if (!huntingTime) {
                if (hound.getTarget() instanceof Player) hound.setTarget(null);
                continue;
            }
            if (leader == null) leader = hound;
            if (player.isAlive() && hound.distanceToSqr(player) <= 34.0D * 34.0D) {
                hound.setTarget(player);
            } else if (leader != hound && leader.getTarget() instanceof Player target) {
                hound.setTarget(target);
            }
        }
    }

    private static void bindRiverWisps(
            ServerLevel level,
            List<RiverWispEntity> wisps,
            Set<UUID> handled) {
        for (RiverWispEntity wisp : wisps) {
            if (!handled.add(wisp.getUUID())) continue;
            int z = wisp.blockPosition().getZ();
            int centerX = (int) Math.round(AuthoredContinentDensity.silverRiverCenterX(z));
            double offset = Math.abs(wisp.getX() - (centerX + 0.5D));
            if (offset <= 26.0D || !level.hasChunk(centerX >> 4, z >> 4)) continue;
            double targetY = Math.max(65.0D, wisp.getY());
            wisp.getNavigation().moveTo(centerX + 0.5D, targetY, z + 0.5D, 1.05D);
        }
    }

'''
    require(anchor in text, "ecology spawnAroundTravellers anchor missing")
    text = text.replace(anchor, helper + anchor, 1)

old_pass_fragment = "actual_custom_entity_types=true actual_entity_instances=true northern_forest_spawn=true"
new_pass_fragment = "actual_custom_entity_types=true actual_entity_instances=true hart_player_avoidance=true hart_herding=true ash_hound_untameable=true ash_hound_night_pack=true river_wisp_no_item_courier=true river_bound_navigation=true northern_forest_spawn=true"
if "hart_player_avoidance=true" not in text:
    require(old_pass_fragment in text, "ecology PASS behavior anchor missing")
    text = text.replace(old_pass_fragment, new_pass_fragment, 1)
ECO.write_text(text, encoding="utf-8")

for path, tokens in {
    HART: ["AvoidEntityGoal", "Player.class, 18.0F"],
    HOUND: ["InteractionResult.PASS", "return false;", "HurtByTargetGoal"],
    WISP: ["InteractionResult.PASS"],
    ECO: ["tickLoadedSpeciesBehavior", "herdSilverHarts", "coordinateAshHounds", "bindRiverWisps", "hart_herding=true", "ash_hound_untameable=true", "ash_hound_night_pack=true", "river_bound_navigation=true"],
}.items():
    current = path.read_text(encoding="utf-8")
    for token in tokens:
        require(token in current, f"missing fantasy species behavior token {token} in {path.name}")

print("Prepared species-specific herd, predator and river-spirit behavior for Java25 compile gating.")
