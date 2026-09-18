package dev.moonseungjun.openworldrpg.integration.spellengine;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Reflection-isolated binding to the pinned Spell Engine event API.
 *
 * <p>This first authority slice is intentionally a gate, not a fake resource system. Project-owned
 * spells are blocked on the server unless a real project authority policy is registered. Spell
 * Engine remains free to execute non-project spells because those are outside this project's
 * namespace and are not silently adopted as Openworld RPG progression content.</p>
 */
public final class SpellEngineAuthorityAdapter {
    private static final String MOD_ID = "spell_engine";
    private static final String SPELL_EVENTS = "net.spell_engine.api.spell.event.SpellEvents";
    private static final String CASTING_ATTEMPT_LISTENER =
            "net.spell_engine.api.spell.event.SpellEvents$CastingAttemptEvent";
    private static final String COST_CONSUME_LISTENER =
            "net.spell_engine.api.spell.event.SpellEvents$SpellCostConsumeEvent";
    private static final String SPELL_CAST_LISTENER =
            "net.spell_engine.api.spell.event.SpellEvents$SpellCastEvent";
    private static final String SPELL_CAST_ATTEMPT =
            "net.spell_engine.internals.casting.SpellCast$Attempt";

    private static final SpellCastAuthority AUTHORITY = new SpellCastAuthority(OpenworldRpgMod.MOD_ID);
    private static volatile boolean initialized;

    private SpellEngineAuthorityAdapter() {
    }

    public static SpellCastAuthority authority() {
        return AUTHORITY;
    }

    public static synchronized void initialize(RuntimeProfile profile, Logger logger) {
        if (initialized) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            logger.info(
                    "Openworld RPG Spell Engine authority adapter inactive for profile {} because {} is not loaded.",
                    profile.id(),
                    MOD_ID
            );
            initialized = true;
            return;
        }

        try {
            ClassLoader loader = SpellEngineAuthorityAdapter.class.getClassLoader();
            Class<?> spellEvents = Class.forName(SPELL_EVENTS, false, loader);
            Class<?> castingAttemptListener = Class.forName(CASTING_ATTEMPT_LISTENER, false, loader);
            Class<?> costConsumeListener = Class.forName(COST_CONSUME_LISTENER, false, loader);
            Class<?> spellCastListener = Class.forName(SPELL_CAST_LISTENER, false, loader);
            Class<?> spellCastAttempt = Class.forName(SPELL_CAST_ATTEMPT, false, loader);
            Method attemptNone = spellCastAttempt.getMethod("none");

            Object stagedAttempt = spellEvents.getField("CASTING_ATTEMPT").get(null);
            Object preAttemptEvent = stagedAttempt.getClass().getField("PRE").get(stagedAttempt);
            Object costConsumeEvent = spellEvents.getField("COST_CONSUME").get(null);
            Object spellCastEvent = spellEvents.getField("SPELL_CAST").get(null);

            registerListener(
                    preAttemptEvent,
                    castingAttemptListener,
                    (proxy, method, args) -> handleCastingAttempt(proxy, method, args, attemptNone)
            );
            registerListener(
                    costConsumeEvent,
                    costConsumeListener,
                    SpellEngineAuthorityAdapter::handleCostConsume
            );
            registerListener(
                    spellCastEvent,
                    spellCastListener,
                    SpellEngineAuthorityAdapter::handleSpellCast
            );

            initialized = true;
            logger.info(
                    "Openworld RPG Spell Engine authority gate armed for profile {} using CASTING_ATTEMPT.PRE, "
                            + "COST_CONSUME and SPELL_CAST.",
                    profile.id()
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Spell Engine is loaded, but the pinned Openworld RPG event authority contract could not be resolved.",
                    exception
            );
        }
    }

    private static Object handleCastingAttempt(
            Object proxy,
            Method method,
            Object[] args,
            Method attemptNone
    ) throws ReflectiveOperationException {
        if (isObjectMethod(method)) {
            return objectMethod(proxy, method, args);
        }
        if (!"onCastingAttempt".equals(method.getName())) {
            throw new IllegalStateException("Unexpected Spell Engine casting listener method: " + method);
        }

        Object eventArgs = onlyArgument(method, args);
        Player player = requirePlayer(invokeAccessor(eventArgs, "caster"));
        if (player.level().isClientSide()) {
            return null;
        }

        String spellId = spellId(invokeAccessor(eventArgs, "spell"));
        SpellCastAuthority.AttemptDecision decision = AUTHORITY.authorizeAttempt(player.getUUID(), spellId);
        return switch (decision) {
            case PASS_THROUGH, ALLOW -> null;
            case BLOCK -> invokeStatic(attemptNone);
        };
    }

    private static Object handleCostConsume(Object proxy, Method method, Object[] args)
            throws ReflectiveOperationException {
        if (isObjectMethod(method)) {
            return objectMethod(proxy, method, args);
        }
        if (!"onSpellCostConsume".equals(method.getName())) {
            throw new IllegalStateException("Unexpected Spell Engine cost listener method: " + method);
        }

        Object eventArgs = onlyArgument(method, args);
        Player player = requirePlayer(invokeAccessor(eventArgs, "caster"));
        if (!player.level().isClientSide()) {
            String spellId = spellId(invokeAccessor(eventArgs, "spell"));
            if (AUTHORITY.owns(spellId)) {
                AUTHORITY.onEngineCostConsumed(player.getUUID(), spellId);
            }
        }
        return null;
    }

    private static Object handleSpellCast(Object proxy, Method method, Object[] args)
            throws ReflectiveOperationException {
        if (isObjectMethod(method)) {
            return objectMethod(proxy, method, args);
        }
        if (!"onSpellCast".equals(method.getName())) {
            throw new IllegalStateException("Unexpected Spell Engine cast listener method: " + method);
        }

        Object eventArgs = onlyArgument(method, args);
        Player player = requirePlayer(invokeAccessor(eventArgs, "caster"));
        if (!player.level().isClientSide()) {
            String spellId = spellId(invokeAccessor(eventArgs, "spell"));
            if (AUTHORITY.owns(spellId)) {
                Object action = invokeAccessor(eventArgs, "action");
                Object progressValue = invokeAccessor(eventArgs, "progress");
                if (!(progressValue instanceof Number progress)) {
                    throw new IllegalStateException("Spell Engine cast progress is not numeric: " + progressValue);
                }
                AUTHORITY.onEngineCastCompleted(
                        player.getUUID(),
                        spellId,
                        String.valueOf(action),
                        progress.floatValue()
                );
            }
        }
        return null;
    }

    private static void registerListener(
            Object event,
            Class<?> listenerType,
            InvocationHandler invocationHandler
    ) throws ReflectiveOperationException {
        Object listener = Proxy.newProxyInstance(
                SpellEngineAuthorityAdapter.class.getClassLoader(),
                new Class<?>[]{listenerType},
                invocationHandler
        );
        Method register = event.getClass().getMethod("register", Object.class);
        register.invoke(event, listener);
    }

    private static Object onlyArgument(Method method, Object[] args) {
        if (args == null || args.length != 1 || args[0] == null) {
            throw new IllegalStateException("Unexpected Spell Engine listener arguments for " + method);
        }
        return args[0];
    }

    private static Player requirePlayer(Object value) {
        if (!(value instanceof Player player)) {
            throw new IllegalStateException("Spell Engine event caster is not a Minecraft Player: " + value);
        }
        return player;
    }

    private static String spellId(Object spellEntry) throws ReflectiveOperationException {
        Object optionalKey = invokeFirstAvailable(spellEntry, "unwrapKey", "getKey");
        if (!(optionalKey instanceof Optional<?> optional) || optional.isEmpty()) {
            throw new IllegalStateException("Spell Engine spell registry entry has no key: " + spellEntry);
        }

        Object key = optional.get();
        Object location = invokeFirstAvailable(key, "location", "getValue");
        String id = String.valueOf(location);
        if (id.indexOf(':') <= 0) {
            throw new IllegalStateException("Spell Engine registry key did not resolve to a resource id: " + id);
        }
        return id;
    }

    private static Object invokeAccessor(Object target, String name) throws ReflectiveOperationException {
        return target.getClass().getMethod(name).invoke(target);
    }

    private static Object invokeFirstAvailable(Object target, String... names) throws ReflectiveOperationException {
        NoSuchMethodException last = null;
        for (String name : names) {
            try {
                return target.getClass().getMethod(name).invoke(target);
            } catch (NoSuchMethodException exception) {
                last = exception;
            } catch (InvocationTargetException exception) {
                throw unwrapInvocation(exception);
            }
        }
        throw last != null ? last : new NoSuchMethodException();
    }

    private static Object invokeStatic(Method method) throws ReflectiveOperationException {
        try {
            return method.invoke(null);
        } catch (InvocationTargetException exception) {
            throw unwrapInvocation(exception);
        }
    }

    private static ReflectiveOperationException unwrapInvocation(InvocationTargetException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof ReflectiveOperationException reflective) {
            return reflective;
        }
        return new ReflectiveOperationException(cause);
    }

    private static boolean isObjectMethod(Method method) {
        return method.getDeclaringClass() == Object.class;
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "OpenworldRpgSpellEngineListener";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == (args == null || args.length == 0 ? null : args[0]);
            default -> throw new IllegalStateException("Unexpected Object method: " + method);
        };
    }
}
