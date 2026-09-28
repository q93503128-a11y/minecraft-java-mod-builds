package dev.moonseungjun.openworldrpg.housing;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent player + Overworld housing authority. */
public final class R01HousingAttachments {
    public static final AttachmentType<R01HousingPlayerState> PLAYER_HOUSING =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_player_housing"
                    ),
                    builder -> builder
                            .initializer(R01HousingPlayerState::initial)
                            .persistent(R01HousingPlayerState.CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<R01HousingWorldState> WORLD_HOUSING =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_world_housing"
                    ),
                    builder -> builder
                            .initializer(R01HousingWorldState::initial)
                            .persistent(R01HousingWorldState.CODEC)
            );

    private R01HousingAttachments() {
    }

    public static void initialize() {
        // Class initialization registers attachment types.
    }
}
