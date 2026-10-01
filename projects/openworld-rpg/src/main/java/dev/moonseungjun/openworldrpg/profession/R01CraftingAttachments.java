package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01CraftingAttachments {
    public static final AttachmentType<R01CraftingState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_crafting"
                    ),
                    builder -> builder
                            .initializer(R01CraftingState::initial)
                            .persistent(R01CraftingState.CODEC)
                            .copyOnDeath()
            );

    private R01CraftingAttachments() {
    }

    public static void initialize() {
    }
}
