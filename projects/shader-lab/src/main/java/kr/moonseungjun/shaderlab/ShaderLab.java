package kr.moonseungjun.shaderlab;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Mod(value = ShaderLab.MOD_ID, dist = Dist.CLIENT)
public final class ShaderLab {
    public static final String MOD_ID = "shaderlab";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String SHADERPACK_ARCHIVE = "ShaderLab-Reverie-0.8.zip";
    private static final String SHADERPACK_DIRECTORY = "ShaderLab-Reverie-0.8";
    private static final String SHADERPACK_RESOURCE = "/shaderpacks/" + SHADERPACK_ARCHIVE;
    private static final String[] OBSOLETE_SHADERPACKS = {
            "ShaderLab-Dreamscape-0.4.zip",
            "ShaderLab-Dreamscape-0.5.zip",
            "ShaderLab-Reverie-0.6.zip",
            "ShaderLab-Reverie-0.6",
            "ShaderLab-Reverie-0.7.zip",
            "ShaderLab-Reverie-0.7",
            "ShaderLab-Reverie-0.8.zip"
    };

    public ShaderLab(IEventBus modEventBus) {
        try {
            installReverieShaderpackDirectory();
        } catch (IOException exception) {
            LOGGER.error("Shader Lab could not install the Reverie shaderpack directory", exception);
        }

        boolean irisLoaded = ModList.get().isLoaded("iris");
        boolean sodiumLoaded = ModList.get().isLoaded("sodium");
        LOGGER.info(
                "Shader Lab Reverie bootstrap loaded (Iris={}, Sodium={}, packDirectory={})",
                irisLoaded,
                sodiumLoaded,
                SHADERPACK_DIRECTORY
        );

        if (!irisLoaded || !sodiumLoaded) {
            LOGGER.error(
                    "Bundled Iris or Sodium was not discovered. The single-JAR package is incomplete " +
                    "and the shader will remain disabled."
            );
        }
    }

    private static void installReverieShaderpackDirectory() throws IOException {
        Path gameDirectory = FMLPaths.GAMEDIR.get();
        Path shaderpacksDirectory = gameDirectory.resolve("shaderpacks");
        Path destination = shaderpacksDirectory.resolve(SHADERPACK_DIRECTORY);
        Path temporary = shaderpacksDirectory.resolve(SHADERPACK_DIRECTORY + ".tmp");
        Files.createDirectories(shaderpacksDirectory);

        for (String obsoleteName : OBSOLETE_SHADERPACKS) {
            deleteRecursively(shaderpacksDirectory.resolve(obsoleteName));
        }
        deleteRecursively(temporary);
        Files.createDirectories(temporary);

        try (InputStream resource = ShaderLab.class.getResourceAsStream(SHADERPACK_RESOURCE)) {
            if (resource == null) {
                throw new IOException("Missing embedded shaderpack resource: " + SHADERPACK_RESOURCE);
            }
            try (ZipInputStream zip = new ZipInputStream(resource)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    Path output = temporary.resolve(entry.getName()).normalize();
                    if (!output.startsWith(temporary)) {
                        throw new IOException("Rejected unsafe shaderpack entry: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(output);
                    } else {
                        Files.createDirectories(output.getParent());
                        Files.copy(zip, output, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zip.closeEntry();
                }
            }
        } catch (IOException exception) {
            deleteRecursively(temporary);
            throw exception;
        }

        if (!Files.isDirectory(temporary.resolve("shaders"))) {
            deleteRecursively(temporary);
            throw new IOException("Embedded Reverie archive did not contain a shaders directory");
        }

        deleteRecursively(destination);
        moveReplacing(temporary, destination);
        configureIris(gameDirectory.resolve("config").resolve("iris.properties"));
        LOGGER.info("Installed Shader Lab Reverie shaderpack directory at {}", destination);
    }

    private static void configureIris(Path irisConfig) throws IOException {
        Files.createDirectories(irisConfig.getParent());

        Properties properties = new Properties();
        if (Files.isRegularFile(irisConfig)) {
            try (InputStream input = Files.newInputStream(irisConfig)) {
                properties.load(input);
            }

            Path backup = irisConfig.resolveSibling("iris.properties.shaderlab-backup");
            if (!Files.exists(backup)) {
                Files.copy(irisConfig, backup);
            }
        }

        properties.setProperty("shaderPack", SHADERPACK_DIRECTORY);
        properties.setProperty("enableShaders", "true");

        Path temporary = irisConfig.resolveSibling("iris.properties.tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "Shader Lab Reverie 0.8 lightweight dream-sky preset");
        }
        moveReplacing(temporary, irisConfig);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        if (Files.isDirectory(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        } else {
            Files.deleteIfExists(root);
        }
    }

    private static void moveReplacing(Path source, Path destination) throws IOException {
        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
