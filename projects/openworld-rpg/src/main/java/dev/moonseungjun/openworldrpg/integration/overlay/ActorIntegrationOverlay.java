package dev.moonseungjun.openworldrpg.integration.overlay;

import dev.moonseungjun.openworldrpg.integration.api.IntegrationPolicy;
import java.util.List;
import java.util.Map;

public record ActorIntegrationOverlay(
        String target,
        String role,
        boolean required,
        Map<String, IntegrationPolicy> policy,
        List<String> tags,
        String encounter
) {
}
