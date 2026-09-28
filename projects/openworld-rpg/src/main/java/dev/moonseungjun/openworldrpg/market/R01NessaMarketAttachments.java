package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01NessaMarketAttachments {
    public static final AttachmentType<R01NessaMarketState> MARKET =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_nessa_market"
                    ),
                    builder -> builder
                            .initializer(R01NessaMarketState::initial)
                            .persistent(R01NessaMarketState.CODEC)
                            .copyOnDeath()
            );

    private R01NessaMarketAttachments() {
    }

    public static void initialize() {
    }
}
