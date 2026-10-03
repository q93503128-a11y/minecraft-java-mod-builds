package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01OrdinaryEnemyRewardAttachments {
    public static final AttachmentType<R01OrdinaryEnemyRewardState> ORDINARY_ENEMY_REWARDS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "r01_ordinary_enemy_rewards"),
                    builder -> builder.initializer(R01OrdinaryEnemyRewardState::initial).persistent(R01OrdinaryEnemyRewardState.CODEC)
            );

    private R01OrdinaryEnemyRewardAttachments() {}

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}