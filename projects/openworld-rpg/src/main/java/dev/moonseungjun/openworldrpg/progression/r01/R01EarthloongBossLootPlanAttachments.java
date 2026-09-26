package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent personal Earthloong boss-loot plan attachment. */
public final class R01EarthloongBossLootPlanAttachments {
    public static final AttachmentType<R01EarthloongBossLootPlanState> EARTHLOONG_BOSS_LOOT =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_earthloong_boss_loot"
                    ),
                    builder -> builder
                            .initializer(R01EarthloongBossLootPlanState::initial)
                            .persistent(R01EarthloongBossLootPlanState.CODEC)
                            .copyOnDeath()
            );

    private R01EarthloongBossLootPlanAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
