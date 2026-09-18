package dev.moonseungjun.openworldrpg.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.integration.bootstrap.DependencyManifest;
import dev.moonseungjun.openworldrpg.integration.bootstrap.DependencyManifestLoader;
import org.junit.jupiter.api.Test;

class DependencyManifestTest {
    @Test
    void canonicalManifestIsStructurallyValid() {
        DependencyManifest manifest = DependencyManifestLoader.loadDefault();

        assertFalse(manifest.validateStructure().hasErrors());
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> "bettercombat".equals(dep.modId())));
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> "spell_engine".equals(dep.modId())));
        assertTrue(manifest.dependencies().stream().anyMatch(dep -> !dep.registryIdResolved()));
    }
}
