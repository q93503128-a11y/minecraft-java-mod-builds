package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent server-world timing state for the R01 Regalhart territory controller. */
public final class R01RegalhartTerritoryAttachments {
    public static final AttachmentType<R01RegalhartTerritoryState> TERRITORY =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_regalhart_territory"
                    ),
                    builder -> builder
                            .initializer(
                                    R01RegalhartTerritoryState::initial
                            )
                            .persistent(
                                    R01RegalhartTerritoryState.CODEC
                            )
            );

    private R01RegalhartTerritoryAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
