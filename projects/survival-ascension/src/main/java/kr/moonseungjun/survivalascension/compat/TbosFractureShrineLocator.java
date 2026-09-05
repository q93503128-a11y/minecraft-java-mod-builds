package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.network.FractureShrineTargetPayload;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Soft optional TBOS bridge: no implementation class is linked at compile time and no chunk is force-loaded. */
public final class TbosFractureShrineLocator {
    private static final String TBOS_MOD_ID = "tbos";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private TbosFractureShrineLocator() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!ModList.get().isLoaded(TBOS_MOD_ID) || player.level() != player.level().getServer().overworld()) {
            SkillNetwork.sendFractureShrineTarget(player, FractureShrineTargetPayload.clear()); return;
        }
        Target target = nearest(player);
        SkillNetwork.sendFractureShrineTarget(player, target == null ? FractureShrineTargetPayload.clear()
                : FractureShrineTargetPayload.target(target.exact(), target.pos().getX(), target.pos().getZ()));
    }

    private static Target nearest(ServerPlayer player) {
        try {
            ServerLevel level = player.level();
            Map<String, Target> targets = new LinkedHashMap<>();
            Class<?> world = Class.forName("com.nightbeam.tbos.world.AdventureWorldManager");
            Object planned = world.getMethod("plannedShrines", ServerLevel.class).invoke(null, level);
            if (planned instanceof List<?> list) for (Object plan : list) put(targets, plan, "target", false);
            Class<?> sites = Class.forName("com.nightbeam.tbos.site.TemporalSiteManager");
            Object data = sites.getMethod("data", ServerLevel.class).invoke(null, level);
            Object built = data.getClass().getMethod("fractureShrines").invoke(data);
            if (built instanceof List<?> list) for (Object placement : list) put(targets, placement, "origin", true);
            BlockPos here = player.blockPosition(); Target best = null; long bestSq = Long.MAX_VALUE;
            for (Target t : targets.values()) {
                long dx = (long)t.pos().getX() - here.getX(), dz = (long)t.pos().getZ() - here.getZ(), sq = dx * dx + dz * dz;
                if (sq < bestSq) { bestSq = sq; best = t; }
            }
            return best;
        } catch (ReflectiveOperationException | LinkageError e) {
            if (WARNED.compareAndSet(false, true)) SurvivalAscension.LOGGER.warn("TBOS Fracture Shrine locator disabled safely", e);
            return null;
        }
    }

    private static void put(Map<String, Target> targets, Object value, String posMethod, boolean exact) throws ReflectiveOperationException {
        Method variantMethod = value.getClass().getMethod("variant");
        Object variant = variantMethod.invoke(value), pos = value.getClass().getMethod(posMethod).invoke(value);
        if (variant != null && pos instanceof BlockPos blockPos) targets.put(variant.toString(), new Target(blockPos.immutable(), exact));
    }
    private record Target(BlockPos pos, boolean exact) {}
}
