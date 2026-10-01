package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01MaterialMarketAttachments {
    public static final AttachmentType<R01MaterialMarketState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_material_market"
                    ),
                    builder -> builder
                            .initializer(R01MaterialMarketState::initial)
                            .persistent(R01MaterialMarketState.CODEC)
                            .copyOnDeath()
            );

    private R01MaterialMarketAttachments() {
    }

    public static void initialize() {
    }
}
