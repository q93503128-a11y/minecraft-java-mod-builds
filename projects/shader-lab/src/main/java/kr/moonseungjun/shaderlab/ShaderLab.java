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
import java.util.Properties;

@Mod(value = ShaderLab.MOD_ID, dist = Dist.CLIENT)
public final class ShaderLab {
    public static final String MOD_ID = "shaderlab";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String SHADERPACK_FILE = "ShaderLab-Reverie-0.6.zip";
    private static final String SHADERPACK_RESOURCE = "/shaderpacks/" + SHADERPACK_FILE;
    private static final String[] OBSOLETE_SHADERPACKS = {
            "ShaderLab-Dreamscape-0.4.zip",
            "ShaderLab-Dreamscape-0.5.zip"
    };

    public ShaderLab(IEventBus modEventBus) {
        try {
            installReverieShaderpack();
        } catch (IOException exception) {
            LOGGER.error("Shader Lab could not install the Reverie shaderpack", exception);
        }

        boolean irisLoaded = ModList.get().isLoaded("iris");
        boolean sodiumLoaded = ModList.get().isLoaded("sodium");
        LOGGER.info(
                "Shader Lab Reverie bootstrap loaded (Iris={}, Sodium={}, pack={})",
                irisLoaded,
                sodiumLoaded,
                SHADERPACK_FILE
        );

        if (!irisLoaded || !sodiumLoaded) {
            LOGGER.error(
                    "Bundled Iris or Sodium was not discovered. The single-JAR package is incomplete " +
                    "and the shader will remain disabled."
            );
        }
    }

    private static void installReverieShaderpack() throws IOException {
        Path gameDirectory = FMLPaths.GAMEDIR.get();
        Path shaderpacksDirectory = gameDirectory.resolve("shaderpacks");
        Path destination = shaderpacksDirectory.resolve(SHADERPACK_FILE);
        Files.createDirectories(shaderpacksDirectory);

        for (String obsoleteName : OBSOLETE_SHADERPACKS) {
            Files.deleteIfExists(shaderpacksDirectory.resolve(obsoleteName));
        }

        try (InputStream input = ShaderLab.class.getResourceAsStream(SHADERPACK_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing embedded shaderpack resource: " + SHADERPACK_RESOURCE);
            }

            Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            moveReplacing(temporary, destination);
        }

        configureIris(gameDirectory.resolve("config").resolve("iris.properties"));
        LOGGER.info("Installed Shader Lab Reverie shaderpack at {}", destination);
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

        properties.setProperty("shaderPack", SHADERPACK_FILE);
        properties.setProperty("enableShaders", "true");

        Path temporary = irisConfig.resolveSibling("iris.properties.tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "Shader Lab Reverie private test preset");
        }
        moveReplacing(temporary, irisConfig);
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
