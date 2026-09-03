package io.github.q93503128.turnbound.world;

import com.mojang.serialization.Codec;
import io.github.q93503128.turnbound.Turnbound;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Canonical v0.4 player persistence carrier.
 *
 * <p>The gameplay runtime remains represented by {@link CampaignProgressStore}. The serialized snapshot is attached
 * to the ServerPlayer so Minecraft/NeoForge owns entity persistence and death-copy semantics. The old standalone
 * JSON file is retained only as an import path for saves made by pre-migration alpha builds.</p>
 */
public final class TurnboundAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Turnbound.MOD_ID);

    public static final Supplier<AttachmentType<String>> CAMPAIGN_PROFILE = ATTACHMENT_TYPES.register(
            "campaign_profile",
            () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING.fieldOf("campaign"))
                    .copyOnDeath()
                    .build());

    private TurnboundAttachments() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
