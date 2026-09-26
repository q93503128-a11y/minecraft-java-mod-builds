package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent shared Earthloong participation state stored on the server Overworld. */
public final class R01EarthloongEncounterAttachments {
    public static final AttachmentType<R01EarthloongEncounterState> EARTHLOONG_ENCOUNTERS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_earthloong_encounters"
                    ),
                    builder -> builder
                            .initializer(R01EarthloongEncounterState::initial)
                            .persistent(R01EarthloongEncounterState.CODEC)
            );

    private R01EarthloongEncounterAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
