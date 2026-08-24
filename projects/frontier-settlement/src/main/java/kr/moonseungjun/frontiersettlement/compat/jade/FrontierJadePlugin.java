package kr.moonseungjun.frontiersettlement.compat.jade;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade plugin. No Frontier core class references Jade, so Jade is never a boot dependency. */
@WailaPlugin
public final class FrontierJadePlugin implements IWailaPlugin {
    public static final Identifier SETTLEMENT_STATUS =
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "settlement_status");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FrontierJadeBlockProvider.INSTANCE, Block.class);
    }
}
