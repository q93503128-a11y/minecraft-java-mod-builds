package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.block.PactBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Only explicit, verifiable covenant violations. Unapproved behavioral
 * taboos (retreat, idle time, ordinary mining or observation) are not inferred.
 */
public final class PactCovenantService {
    private PactCovenantService() {}

    /** Player chose to mine Nerea's actual altar core, not nearby natural stone. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if(event.isCanceled()
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !PactService.isPact(player)
                || player.isCreative()
                || !event.getState().is(PactBlocks.ALTAR_CORE.get())
                || !PactAltarBlock.patronId(event.getState()).equals("nerea")) return;
        PactSavedData data=PactSavedData.get(player.getServer());
        if(data.defileNereaSanctum(player.getUUID())) {
            PactBlessingService.apply(player);
            player.sendSystemMessage(Component.translatable(
                    "message.anomalousclasses.pact.nerea_breach"));
        }
    }

    /** Invoked only after a real hostile was hit by Tide Grasp -> Pressure. */
    public static void onNereaWaterCombo(ServerPlayer player,Mob hostile) {
        if(player.getServer()==null || !PactService.isPact(player)
                || !player.isInWater() || hostile==null
                || hostile.level()!=player.level()
                || !PactCombatService.enemy(player,hostile)) return;
        PactSavedData data=PactSavedData.get(player.getServer());
        int progress=data.reconcileNereaSanctum(player.getUUID(),hostile.getUUID());
        if(progress>0) {
            PactBlessingService.apply(player);
            player.sendSystemMessage(Component.translatable(
                    progress>=2
                            ? "message.anomalousclasses.pact.nerea_restored"
                            : "message.anomalousclasses.pact.nerea_restitution",
                    progress,2));
        }
    }
}
