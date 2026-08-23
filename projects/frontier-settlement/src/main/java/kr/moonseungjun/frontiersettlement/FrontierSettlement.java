package kr.moonseungjun.frontiersettlement;

import kr.moonseungjun.frontiersettlement.command.SettlementCommands;
import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.network.SettlementNetwork;
import kr.moonseungjun.frontiersettlement.settlement.SettlementService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FrontierSettlement.MOD_ID)
public final class FrontierSettlement {
    public static final String MOD_ID = "frontier_settlement";

    public FrontierSettlement(IEventBus modBus, ModContainer modContainer) {
        FrontierContent.register(modBus);
        modBus.addListener(SettlementNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(SettlementCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SettlementService::onServerTick);
        NeoForge.EVENT_BUS.addListener(SettlementService::onPlayerLoggedIn);
    }
}
