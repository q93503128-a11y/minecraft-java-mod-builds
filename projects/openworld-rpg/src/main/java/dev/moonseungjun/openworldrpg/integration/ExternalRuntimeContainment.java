package dev.moonseungjun.openworldrpg.integration;

import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

/**
 * Owns the runtime configuration firewall around large external gameplay dependencies.
 *
 * <p>The project intentionally consumes their rendering/casting/animation primitives without
 * inheriting donor progression, loot, spell acquisition or random ecology. These files are
 * project-managed configuration, not user preference.</p>
 */
public final class ExternalRuntimeContainment {
    private static final String BETTER_COMBAT_MOD = "bettercombat";
    private static final String SPELL_ENGINE_MOD = "spell_engine";
    private static final String MOB_FILTER_MOD = "mobfilter";

    private static final String BETTER_COMBAT_SERVER = """
            {
              "allow_vanilla_sweeping": false,
              "fallback_compatibility_enabled": false
            }
            """;

    private static final String SPELL_ENGINE_SERVER = """
            {
              "haste_affects_cooldown": false,
              "spell_cost_exhaust_multiplier": 0.0,
              "spell_cost_item_allowed": false,
              "spell_cost_durability_allowed": false,
              "spell_item_cooldown_lock": false,
              "spell_book_additional_cooldown": 0.0,
              "spell_book_creation_enabled": false,
              "spell_binding_allow_unbinding": false
            }
            """;

    private static final String SPELL_ENGINE_WEAPON_FALLBACK = """
            {
              "enabled": false
            }
            """;

    private static final String RPG_SERIES_LOOT_DISABLED = """
            {
              "injectors": {},
              "regex_injectors": {},
              "fallback": {
                "rolls_multiplier": 0.0,
                "tables": "~:chests/",
                "blacklist": [],
                "entries": []
              }
            }
            """;

    private static final String MOB_FILTER_RULES = """
            {
              "rules": [
                {
                  "name": "Openworld RPG authored ecology boundary",
                  "what": "DISALLOW_SPAWN",
                  "when": {
                    "category": [
                      "MONSTER",
                      "CREATURE",
                      "AMBIENT",
                      "AXOLOTLS",
                      "UNDERGROUND_WATER_CREATURE",
                      "WATER_CREATURE",
                      "WATER_AMBIENT",
                      "MISC"
                    ],
                    "spawnReason": [
                      "NATURAL",
                      "CHUNK_GENERATION",
                      "SPAWNER",
                      "STRUCTURE",
                      "BREEDING",
                      "MOB_SUMMONED",
                      "JOCKEY",
                      "EVENT",
                      "CONVERSION",
                      "REINFORCEMENT",
                      "TRIGGERED",
                      "PATROL",
                      "TRIAL_SPAWNER"
                    ]
                  }
                }
              ],
              "logLevel": "INFO"
            }
            """;

    private static final String MOB_FILTER_SIMPLE = """
            # Openworld RPG uses the advanced mobfilter.json5 policy.
            # Keep this simple file empty so it cannot compete with the authored ecology boundary.
            """;

    private ExternalRuntimeContainment() {
    }

    public static void initialize(RuntimeProfile profile, Logger logger) {
        if (profile == RuntimeProfile.CORE) {
            logger.info("Openworld RPG donor containment inactive for isolated core profile.");
            return;
        }

        FabricLoader loader = FabricLoader.getInstance();
        Path configDir = loader.getConfigDir();

        if (loader.isModLoaded(BETTER_COMBAT_MOD)) {
            writeManaged(configDir.resolve("bettercombat/server.json5"), BETTER_COMBAT_SERVER);
            forceBetterCombatRuntimeIfInitialized();
        }

        if (loader.isModLoaded(SPELL_ENGINE_MOD)) {
            writeManaged(configDir.resolve("spell_engine/server.json5"), SPELL_ENGINE_SERVER);
            writeManaged(configDir.resolve("spell_engine/weapon_fallback.json"), SPELL_ENGINE_WEAPON_FALLBACK);
            writeManaged(configDir.resolve("rpg_series/loot_equipment_v2.json"), RPG_SERIES_LOOT_DISABLED);
            writeManaged(configDir.resolve("rpg_series/loot_scrolls_v2.json"), RPG_SERIES_LOOT_DISABLED);
            refreshSpellEngineDataManagers();
            forceSpellEngineRuntimeIfInitialized();
        }

        if (loader.isModLoaded(MOB_FILTER_MOD)) {
            writeManaged(configDir.resolve("mobfilter.simple"), MOB_FILTER_SIMPLE);
            writeManaged(configDir.resolve("mobfilter.json5"), MOB_FILTER_RULES);
        }

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            verifyAndEnforceRuntime(loader, logger);
        });

        logger.info(
                "Openworld RPG external runtime containment armed for profile {}: donor weapon fallback, "
                        + "RPG-Series loot/progression and random ecology are project-controlled.",
                profile.id()
        );
    }

    private static void verifyAndEnforceRuntime(FabricLoader loader, Logger logger) {
        if (loader.isModLoaded(BETTER_COMBAT_MOD)) {
            forceBetterCombatRuntime();
        }
        if (loader.isModLoaded(SPELL_ENGINE_MOD)) {
            refreshSpellEngineDataManagers();
            forceSpellEngineRuntime();
            verifySpellEngineDataManagers();
        }

        logger.info("Openworld RPG external runtime containment verified before gameplay server start.");
    }

    private static void forceBetterCombatRuntimeIfInitialized() {
        try {
            Object config = Class.forName("net.bettercombat.BetterCombatMod")
                    .getField("config")
                    .get(null);
            if (config != null) {
                enforceBetterCombatConfig(config);
            }
        } catch (ReflectiveOperationException exception) {
            throw containmentFailure("Better Combat runtime config", exception);
        }
    }

    private static void forceBetterCombatRuntime() {
        try {
            Object config = Class.forName("net.bettercombat.BetterCombatMod")
                    .getField("config")
                    .get(null);
            if (config == null) {
                throw new IllegalStateException("Better Combat server config is not initialized.");
            }
            enforceBetterCombatConfig(config);
            if (readBoolean(config, "allow_vanilla_sweeping")
                    || readBoolean(config, "fallback_compatibility_enabled")) {
                throw new IllegalStateException("Better Combat donor fallback containment did not stick.");
            }
        } catch (ReflectiveOperationException exception) {
            throw containmentFailure("Better Combat runtime config", exception);
        }
    }

    private static void enforceBetterCombatConfig(Object config) throws ReflectiveOperationException {
        setBoolean(config, "allow_vanilla_sweeping", false);
        setBoolean(config, "fallback_compatibility_enabled", false);
    }

    private static void refreshSpellEngineDataManagers() {
        try {
            Class<?> spellEngine = Class.forName("net.spell_engine.SpellEngineMod");
            refreshManager(spellEngine.getField("fallbackConfig").get(null));

            Class<?> rpgSeries = Class.forName("net.spell_engine.rpg_series.RPGSeriesCore");
            refreshManager(rpgSeries.getField("lootEquipmentConfig").get(null));
            refreshManager(rpgSeries.getField("lootScrollsConfig").get(null));
        } catch (ReflectiveOperationException exception) {
            throw containmentFailure("Spell Engine data config", exception);
        }
    }

    private static void forceSpellEngineRuntimeIfInitialized() {
        try {
            Object config = Class.forName("net.spell_engine.SpellEngineMod")
                    .getField("config")
                    .get(null);
            if (config != null) {
                enforceSpellEngineConfig(config);
            }
        } catch (ReflectiveOperationException exception) {
            throw containmentFailure("Spell Engine runtime config", exception);
        }
    }

    private static void forceSpellEngineRuntime() {
        try {
            Object config = Class.forName("net.spell_engine.SpellEngineMod")
                    .getField("config")
                    .get(null);
            if (config == null) {
                throw new IllegalStateException("Spell Engine server config is not initialized.");
            }
            enforceSpellEngineConfig(config);
            if (readBoolean(config, "spell_book_creation_enabled")
                    || readBoolean(config, "spell_binding_allow_unbinding")
                    || readBoolean(config, "spell_cost_item_allowed")
                    || readBoolean(config, "spell_cost_durability_allowed")) {
                throw new IllegalStateException("Spell Engine donor progression/cost containment did not stick.");
            }
        } catch (ReflectiveOperationException exception) {
            throw containmentFailure("Spell Engine runtime config", exception);
        }
    }

    private static void enforceSpellEngineConfig(Object config) throws ReflectiveOperationException {
        setBoolean(config, "haste_affects_cooldown", false);
        setFloat(config, "spell_cost_exhaust_multiplier", 0.0F);
        setBoolean(config, "spell_cost_item_allowed", false);
        setBoolean(config, "spell_cost_durability_allowed", false);
        setBoolean(config, "spell_item_cooldown_lock", false);
        setFloat(config, "spell_book_additional_cooldown", 0.0F);
        setBoolean(config, "spell_book_creation_enabled", false);
        setBoolean(config, "spell_binding_allow_unbinding", false);
    }

    private static void verifySpellEngineDataManagers() {
        try {
            Class<?> spellEngine = Class.forName("net.spell_engine.SpellEngineMod");
            Object fallback = managerValue(spellEngine.getField("fallbackConfig").get(null));
            if (readBoolean(fallback, "enabled")) {
                throw new IllegalStateException("Spell Engine weapon fallback is still enabled.");
            }

            Class<?> rpgSeries = Class.forName("net.spell_engine.rpg_series.RPGSeriesCore");
            verifyLootManagerDisabled(rpgSeries.getField("lootEquipmentConfig").get(null));
            verifyLootManagerDisabled(rpgSeries.getField("lootScrollsConfig").get(null));
        } catch (ReflectiveOperationException exception) {
            throw containmentFailure("Spell Engine data config verification", exception);
        }
    }

    private static void verifyLootManagerDisabled(Object manager) throws ReflectiveOperationException {
        Object value = managerValue(manager);
        Object injectors = value.getClass().getField("injectors").get(value);
        Object regexInjectors = value.getClass().getField("regex_injectors").get(value);
        if (!(injectors instanceof Map<?, ?> exact) || !exact.isEmpty()
                || !(regexInjectors instanceof Map<?, ?> regex) || !regex.isEmpty()) {
            throw new IllegalStateException("RPG Series loot injectors are not empty.");
        }

        Object fallback = value.getClass().getField("fallback").get(value);
        if (fallback == null) {
            throw new IllegalStateException("RPG Series loot fallback is missing.");
        }
        float multiplier = ((Number) fallback.getClass().getField("rolls_multiplier").get(fallback)).floatValue();
        if (Math.abs(multiplier) > 1.0e-6F) {
            throw new IllegalStateException("RPG Series loot fallback is still active: " + multiplier);
        }
    }

    private static void refreshManager(Object manager) throws ReflectiveOperationException {
        Method refresh = manager.getClass().getMethod("refresh");
        try {
            refresh.invoke(manager);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new ReflectiveOperationException(cause);
        }
    }

    private static Object managerValue(Object manager) throws ReflectiveOperationException {
        Field value = manager.getClass().getField("value");
        Object result = value.get(manager);
        if (result == null) {
            throw new IllegalStateException("External config manager produced a null value.");
        }
        return result;
    }

    private static void setBoolean(Object target, String field, boolean value) throws ReflectiveOperationException {
        target.getClass().getField(field).setBoolean(target, value);
    }

    private static void setFloat(Object target, String field, float value) throws ReflectiveOperationException {
        target.getClass().getField(field).setFloat(target, value);
    }

    private static boolean readBoolean(Object target, String field) throws ReflectiveOperationException {
        return target.getClass().getField(field).getBoolean(target);
    }

    private static void writeManaged(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            String normalized = content.endsWith("\n") ? content : content + "\n";
            if (!Files.exists(path) || !Files.readString(path, StandardCharsets.UTF_8).equals(normalized)) {
                Files.writeString(path, normalized, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write required external-mod containment config: " + path, exception);
        }
    }

    private static IllegalStateException containmentFailure(String contract, Exception cause) {
        return new IllegalStateException(
                "Openworld RPG could not enforce the pinned " + contract + " contract.",
                cause
        );
    }
}
