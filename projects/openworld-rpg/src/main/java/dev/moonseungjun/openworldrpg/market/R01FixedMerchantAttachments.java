package dev.moonseungjun.openworldrpg.market;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01FixedMerchantAttachments {
    public static final AttachmentType<R01FixedMerchantState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_fixed_merchants"
                    ),
                    builder -> builder
                            .initializer(R01FixedMerchantState::initial)
                            .persistent(R01FixedMerchantState.CODEC)
                            .copyOnDeath()
            );

    private R01FixedMerchantAttachments() {
    }

    public static void initialize() {
    }
}
