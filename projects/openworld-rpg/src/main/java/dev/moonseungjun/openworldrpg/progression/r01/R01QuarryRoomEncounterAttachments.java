package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent shared non-spatial state for R01 Quarry room encounters. */
public final class R01QuarryRoomEncounterAttachments {
    public static final AttachmentType<R01QuarryRoomEncounterState> QUARRY_ROOMS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_quarry_room_encounters"
                    ),
                    builder -> builder
                            .initializer(R01QuarryRoomEncounterState::initial)
                            .persistent(R01QuarryRoomEncounterState.CODEC)
            );

    private R01QuarryRoomEncounterAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
