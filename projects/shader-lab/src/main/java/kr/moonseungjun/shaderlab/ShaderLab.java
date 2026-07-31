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

    private static final String SHADERPACK_FILE = "ShaderLab-Dreamscape-0.4.zip";
    private static final String SHADERPACK_RESOURCE = "/shaderpacks/" + SHADERPACK_FILE;

    public ShaderLab(IEventBus modEventBus) {
        try {
            installDreamscapeShaderpack();
        } catch (IOException exception) {
            LOGGER.error("Shader Lab could not install the Dreamscape shaderpack", exception);
        }

        boolean irisLoaded = ModList.get().isLoaded("iris");
        boolean sodiumLoaded = ModList.get().isLoaded("sodium");
        LOGGER.info(
                "Shader Lab Dreamscape bootstrap loaded (Iris={}, Sodium={}, pack={})",
                irisLoaded,
                sodiumLoaded,
                SHADERPACK_FILE
        );

        if (!irisLoaded || !sodiumLoaded) {
            LOGGER.warn(
                    "Dreamscape requires Iris 1.11.2+ and Sodium 0.9.1+ for Minecraft 26.2. " +
                    "The shaderpack was installed, but shaders will stay disabled until both mods are present."
            );
        }
    }

    private static void installDreamscapeShaderpack() throws IOException {
        Path gameDirectory = FMLPaths.GAMEDIR.get();
        Path shaderpacksDirectory = gameDirectory.resolve("shaderpacks");
        Path destination = shaderpacksDirectory.resolve(SHADERPACK_FILE);
        Files.createDirectories(shaderpacksDirectory);

        try (InputStream input = ShaderLab.class.getResourceAsStream(SHADERPACK_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing embedded shaderpack resource: " + SHADERPACK_RESOURCE);
            }

            Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            moveReplacing(temporary, destination);
        }

        configureIris(gameDirectory.resolve("config").resolve("iris.properties"));
        LOGGER.info("Installed Shader Lab Dreamscape shaderpack at {}", destination);
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
            properties.store(output, "Shader Lab Dreamscape private test preset");
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
