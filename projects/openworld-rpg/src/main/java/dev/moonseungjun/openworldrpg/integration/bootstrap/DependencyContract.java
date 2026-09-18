package dev.moonseungjun.openworldrpg.integration.bootstrap;

import java.util.List;
import java.util.Locale;

public record DependencyContract(
        String logicalId,
        String modId,
        String expectedVersion,
        boolean enforceVersion,
        List<String> requiredProfiles,
        String integrationModule,
        String licenseBoundary,
        String verification
) {
    public boolean requiredFor(RuntimeProfile profile) {
        if (requiredProfiles == null) {
            return false;
        }
        String target = profile.id();
        return requiredProfiles.stream()
                .filter(value -> value != null)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .anyMatch(target::equals);
    }

    public boolean registryIdResolved() {
        return modId != null
                && !modId.isBlank()
                && "RESOLVED".equalsIgnoreCase(verification);
    }
}
