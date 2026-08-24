package kr.moonseungjun.frontiersettlement.compat.jade;

import kr.moonseungjun.frontiersettlement.client.ClientSettlementState;
import kr.moonseungjun.frontiersettlement.network.SettlementContextTarget;
import net.minecraft.network.chat.Component;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Adds at most two compact lines for authoritative Frontier infrastructure under the crosshair. */
public final class FrontierJadeBlockProvider implements IBlockComponentProvider {
    public static final FrontierJadeBlockProvider INSTANCE = new FrontierJadeBlockProvider();
    private FrontierJadeBlockProvider() {}

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        SettlementContextTarget target = ClientSettlementState.context().targetAt(accessor.getPosition());
        if (target == null) return;
        tooltip.add(Component.literal(target.title()));
        String detail = target.detail();
        if (target.progress() >= 0) detail += " · " + target.progress() + "%";
        if (!detail.isBlank()) tooltip.add(Component.literal(detail));
    }

    @Override
    public net.minecraft.resources.Identifier getUid() {
        return FrontierJadePlugin.SETTLEMENT_STATUS;
    }

    @Override
    public int getDefaultPriority() {
        return 2500;
    }
}
