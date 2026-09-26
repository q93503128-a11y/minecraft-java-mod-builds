package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent bounded class-attribution evidence for the current/recent R01 Quarry run. */
public final class R01QuarryRunAttributionAttachments {
    public static final AttachmentType<R01QuarryRunAttributionState> QUARRY_RUN_ATTRIBUTION =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_quarry_run_attribution"
                    ),
                    builder -> builder
                            .initializer(R01QuarryRunAttributionState::initial)
                            .persistent(R01QuarryRunAttributionState.CODEC)
                            .copyOnDeath()
            );

    private R01QuarryRunAttributionAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
