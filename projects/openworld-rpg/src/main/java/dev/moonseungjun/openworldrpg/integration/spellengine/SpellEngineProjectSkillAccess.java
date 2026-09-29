package dev.moonseungjun.openworldrpg.integration.spellengine;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectClassSkillLoadout;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Reflection-isolated bridge that publishes project-owned class skill slots into Spell Engine.
 *
 * <p>Spell Engine remains the casting/targeting/delivery backend. The project owns which skills
 * exist in the five class slots, so donor spell books, weapon containers and hotbar progression do
 * not become gameplay authority.</p>
 */
public final class SpellEngineProjectSkillAccess {
    private static final String MOD_ID = "spell_engine";
    private static final String SOURCE_PREFIX = "openworld_rpg:class_skill_slot/";
    private static volatile Binding binding;

    private SpellEngineProjectSkillAccess() {
    }

    /**
     * Publishes the current root class's implemented skill slots from the authoritative server.
     */
    public static void refreshPublishedSkills(Player player) {
        Objects.requireNonNull(player, "player");
        if (player.level().isClientSide()
                || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }

        try {
            Binding access = binding();
            if (!access.ownerClass().isInstance(player)) {
                return;
            }

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> containers =
                    (LinkedHashMap<String, Object>) access.serverSideContainers()
                            .invoke(player);

            containers.keySet().removeIf(
                    key -> key.startsWith(SOURCE_PREFIX)
            );

            PlayerProgressionService.state(player).activeClass().ifPresent(
                    rootClass -> {
                        for (var slot
                                : ProjectClassSkillLoadout.implementedSlots(rootClass)) {
                            containers.put(
                                    sourceName(slot.slotIndex()),
                                    access.newContainedSpellContainer(
                                            slot.spellId()
                                    )
                            );
                        }
                    }
            );

            access.syncServerSideContainers().invoke(null, player);
            rewriteActiveSpells(player);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new IllegalStateException(
                    "Spell Engine project skill publication failed.",
                    cause == null ? exception : cause
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine project skill publication contract is unavailable.",
                    exception
            );
        }
    }

    /**
     * Returns the server-published project spell for a canonical slot.
     *
     * <p>This works on both logical sides because Spell Engine synchronizes its server-side
     * containers to the owning client.</p>
     */
    public static Optional<String> publishedSpellId(
            Player player,
            int slotIndex
    ) {
        Objects.requireNonNull(player, "player");
        if (slotIndex < 0
                || slotIndex >= ProjectClassSkillLoadout.TOTAL_SLOT_COUNT
                || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return Optional.empty();
        }

        try {
            Binding access = binding();
            if (!access.ownerClass().isInstance(player)) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> containers =
                    (Map<String, Object>) access.serverSideContainers()
                            .invoke(player);
            Object container = containers.get(sourceName(slotIndex));
            if (container == null) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            List<String> spellIds =
                    (List<String>) access.containerSpellIds()
                            .invoke(container);
            if (spellIds.size() != 1) {
                return Optional.empty();
            }
            return Optional.of(spellIds.getFirst());
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new IllegalStateException(
                    "Spell Engine project skill slot read failed.",
                    cause == null ? exception : cause
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine project skill slot contract is unavailable.",
                    exception
            );
        }
    }

    /**
     * Replaces donor active-spell resolution with the project-published class skill list.
     *
     * <p>This is invoked from one isolated compatibility mixin at the end of Spell Engine's
     * container refresh. Passive/modifier handling is left untouched; only actively castable
     * skill authority is replaced.</p>
     */
    public static void rewriteActiveSpells(Player player) {
        Objects.requireNonNull(player, "player");
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }

        try {
            Binding access = binding();
            if (!access.ownerClass().isInstance(player)) {
                return;
            }

            List<String> desiredIds = publishedProjectSpellIds(
                    player,
                    access
            );

            Object currentResult = access.getSpellContainers()
                    .invoke(player);
            @SuppressWarnings("unchecked")
            List<Object> currentActives =
                    (List<Object>) access.resultActives()
                            .invoke(currentResult);

            if (activeSpellIds(currentActives).equals(desiredIds)) {
                return;
            }

            List<Object> desiredHolders =
                    resolveSpellHolders(player, desiredIds, access);

            Object nextResult = access.resultConstructor().newInstance(
                    access.resultActiveContainer().invoke(currentResult),
                    desiredHolders,
                    access.resultPassives().invoke(currentResult),
                    access.resultModifiers().invoke(currentResult),
                    access.resultSources().invoke(currentResult)
            );
            access.setSpellContainers().invoke(player, nextResult);

            if (access.spellCasterPlayerClass().isInstance(player)) {
                Object interactor =
                        access.getInteractor().invoke(player);
                interactor.getClass()
                        .getMethod("invalidateOptions")
                        .invoke(interactor);
            }
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new IllegalStateException(
                    "Spell Engine active project skill rewrite failed.",
                    cause == null ? exception : cause
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine active skill rewrite contract is unavailable.",
                    exception
            );
        }
    }

    private static List<String> publishedProjectSpellIds(
            Player player,
            Binding access
    ) throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        Map<String, Object> containers =
                (Map<String, Object>) access.serverSideContainers()
                        .invoke(player);

        List<Map.Entry<String, Object>> projectEntries =
                containers.entrySet().stream()
                        .filter(entry ->
                                entry.getKey().startsWith(SOURCE_PREFIX))
                        .sorted(Comparator.comparingInt(
                                entry -> slotIndex(entry.getKey())
                        ))
                        .toList();

        List<String> result = new ArrayList<>();
        for (var entry : projectEntries) {
            @SuppressWarnings("unchecked")
            List<String> spellIds =
                    (List<String>) access.containerSpellIds()
                            .invoke(entry.getValue());
            result.addAll(spellIds);
        }
        return List.copyOf(result);
    }

    private static List<String> activeSpellIds(List<Object> holders) {
        List<String> result = new ArrayList<>();
        for (Object value : holders) {
            if (!(value instanceof Holder<?> holder)) {
                continue;
            }
            holder.unwrapKey().ifPresent(
                    key -> result.add(key.identifier().toString())
            );
        }
        return List.copyOf(result);
    }

    private static List<Object> resolveSpellHolders(
            Player player,
            List<String> spellIds,
            Binding access
    ) throws ReflectiveOperationException {
        Object registry = access.spellRegistryFrom()
                .invoke(null, player.level());
        Method get = registry.getClass()
                .getMethod("get", Identifier.class);

        List<Object> holders = new ArrayList<>();
        for (String spellId : spellIds) {
            Object result = get.invoke(
                    registry,
                    Identifier.parse(spellId)
            );
            if (!(result instanceof Optional<?> optional)
                    || optional.isEmpty()) {
                throw new IllegalStateException(
                        "Published project class skill is missing from Spell Engine registry: "
                                + spellId
                );
            }
            holders.add(optional.orElseThrow());
        }
        return List.copyOf(holders);
    }

    private static String sourceName(int slotIndex) {
        return SOURCE_PREFIX + slotIndex;
    }

    private static int slotIndex(String sourceName) {
        try {
            return Integer.parseInt(
                    sourceName.substring(SOURCE_PREFIX.length())
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Malformed project class skill source: " + sourceName,
                    exception
            );
        }
    }

    private static Binding binding()
            throws ReflectiveOperationException {
        Binding current = binding;
        if (current != null) {
            return current;
        }
        synchronized (SpellEngineProjectSkillAccess.class) {
            current = binding;
            if (current != null) {
                return current;
            }

            ClassLoader loader =
                    SpellEngineProjectSkillAccess.class.getClassLoader();
            Class<?> owner = Class.forName(
                    "net.spell_engine.internals.container.SpellContainerSource$Owner",
                    false,
                    loader
            );
            Class<?> source = Class.forName(
                    "net.spell_engine.internals.container.SpellContainerSource",
                    false,
                    loader
            );
            Class<?> result = Class.forName(
                    "net.spell_engine.internals.container.SpellContainerSource$Result",
                    false,
                    loader
            );
            Class<?> container = Class.forName(
                    "net.spell_engine.api.spell.container.SpellContainer",
                    false,
                    loader
            );
            Class<?> contentType = Class.forName(
                    "net.spell_engine.api.spell.container.SpellContainer$ContentType",
                    false,
                    loader
            );
            Class<?> spellRegistry = Class.forName(
                    "net.spell_engine.api.spell.registry.SpellRegistry",
                    false,
                    loader
            );
            Class<?> spellCasterPlayer = Class.forName(
                    "net.spell_engine.internals.casting.SpellCaster$Player",
                    false,
                    loader
            );

            Constructor<?> resultConstructor =
                    result.getDeclaredConstructor(
                            container,
                            List.class,
                            List.class,
                            List.class,
                            List.class
                    );
            Constructor<?> containerConstructor =
                    container.getDeclaredConstructor(
                            contentType,
                            String.class,
                            String.class,
                            String.class,
                            int.class,
                            List.class,
                            int.class
                    );

            @SuppressWarnings({"rawtypes", "unchecked"})
            Object none = Enum.valueOf(
                    (Class<? extends Enum>) contentType.asSubclass(
                            Enum.class
                    ),
                    "NONE"
            );

            current = new Binding(
                    owner,
                    spellCasterPlayer,
                    owner.getMethod("serverSideSpellContainers"),
                    owner.getMethod("setSpellContainers", result),
                    owner.getMethod("getSpellContainers"),
                    source.getMethod(
                            "syncServerSideContainers",
                            Player.class
                    ),
                    result.getMethod("activeContainer"),
                    result.getMethod("actives"),
                    result.getMethod("passives"),
                    result.getMethod("modifiers"),
                    result.getMethod("sources"),
                    resultConstructor,
                    containerConstructor,
                    none,
                    container.getMethod("spell_ids"),
                    spellRegistry.getMethod(
                            "from",
                            net.minecraft.world.level.Level.class
                    ),
                    spellCasterPlayer.getMethod("getInteractor")
            );
            binding = current;
            return current;
        }
    }

    private record Binding(
            Class<?> ownerClass,
            Class<?> spellCasterPlayerClass,
            Method serverSideContainers,
            Method setSpellContainers,
            Method getSpellContainers,
            Method syncServerSideContainers,
            Method resultActiveContainer,
            Method resultActives,
            Method resultPassives,
            Method resultModifiers,
            Method resultSources,
            Constructor<?> resultConstructor,
            Constructor<?> containerConstructor,
            Object noneContentType,
            Method containerSpellIds,
            Method spellRegistryFrom,
            Method getInteractor
    ) {
        Object newContainedSpellContainer(String spellId) {
            try {
                return containerConstructor.newInstance(
                        noneContentType,
                        "",
                        "",
                        "",
                        0,
                        List.of(spellId),
                        0
                );
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                throw new IllegalStateException(
                        "Spell Engine project skill container creation failed.",
                        cause == null ? exception : cause
                );
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Pinned Spell Engine container constructor is unavailable.",
                        exception
                );
            }
        }
    }
}
