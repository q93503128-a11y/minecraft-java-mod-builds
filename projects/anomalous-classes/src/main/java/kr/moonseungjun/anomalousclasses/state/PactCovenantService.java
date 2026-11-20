package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.block.PactAltarBlock;
import kr.moonseungjun.anomalousclasses.block.PactBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
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

    /**
     * Called ONLY by PactTierTwoCombatService after a successful landed hit
     * against a hostile validated BEFORE damage. A killing blow must count:
     * calling enemy() again here would reject a legitimately defeated mob.
     */
    static void onNereaWaterCombo(ServerPlayer player,UUID validatedHostile) {
        if(player.getServer()==null || !PactService.isPact(player)
                || !player.isInWater() || validatedHostile==null) return;
        PactSavedData data=PactSavedData.get(player.getServer());
        int progress=data.reconcileNereaSanctum(player.getUUID(),validatedHostile);
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
