package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.bootstrap.DependencyContract;
import dev.moonseungjun.openworldrpg.integration.bootstrap.DependencyManifest;
import dev.moonseungjun.openworldrpg.integration.bootstrap.DependencyManifestLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

class DependencyManifestTest {
    @Test
    void canonicalManifestIsStructurallyValidAndPinsEveryRuntimeDependency() {
        DependencyManifest manifest = DependencyManifestLoader.loadDefault();

        assertFalse(manifest.validateStructure().hasErrors());
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> "bettercombat".equals(dep.modId())));
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> "spell_engine".equals(dep.modId())));
        assertTrue(manifest.dependencies().stream().allMatch(dep -> dep.registryIdResolved()));
        assertTrue(manifest.dependencies().stream().allMatch(DependencyContract::enforceVersion));
        assertTrue(manifest.dependencies().stream()
                .allMatch(dep -> dep.expectedVersion() != null && !dep.expectedVersion().isBlank()));
        assertTrue(manifest.dependencies().stream().anyMatch(dep ->
                "bettercombat".equals(dep.modId()) && "3.2.2".equals(dep.expectedVersion())));
        assertTrue(manifest.dependencies().stream().anyMatch(dep ->
                "player_animation_library".equals(dep.modId())
                        && "1.2.6+mc.26.2".equals(dep.expectedVersion())));
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> "alexsmobs".equals(dep.modId())));
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> "threateningly_mobs".equals(dep.modId())));
    }

    @Test
    void enforcedDependencyWithoutExpectedVersionFailsStructureValidation() {
        DependencyManifest manifest = new DependencyManifest(
                1,
                List.of(new DependencyContract(
                        "missing-version",
                        "example_mod",
                        "",
                        true,
                        List.of("gameplay"),
                        "example",
                        "TEST",
                        "RESOLVED"
                ))
        );

        var report = manifest.validateStructure();

        assertTrue(report.hasErrors());
        assertTrue(report.issues().stream()
                .anyMatch(issue -> "manifest.expected_version".equals(issue.code())));
    }
}
