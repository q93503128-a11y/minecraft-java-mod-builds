package kr.moonseungjun.senbonzakura;

import kr.moonseungjun.senbonzakura.client.BankaiWorldRenderer;
import kr.moonseungjun.senbonzakura.client.ClientBankaiNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SenbonzakuraShowcase.MOD_ID, dist = Dist.CLIENT)
public final class SenbonzakuraShowcaseClient {
    public SenbonzakuraShowcaseClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientBankaiNetwork::register);
        NeoForge.EVENT_BUS.addListener(BankaiWorldRenderer::onExtract);
        NeoForge.EVENT_BUS.addListener(BankaiWorldRenderer::onSubmit);
    }
}
