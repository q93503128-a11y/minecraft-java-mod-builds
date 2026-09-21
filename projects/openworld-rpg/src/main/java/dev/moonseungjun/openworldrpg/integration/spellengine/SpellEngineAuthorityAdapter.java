package dev.moonseungjun.openworldrpg.integration.spellengine;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellTransactionPolicy;
import dev.moonseungjun.openworldrpg.combat.authority.SpellCastAuthority;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectMinecraftDamageApplicator;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatStateStore;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Reflection-isolated binding to the pinned Spell Engine event/custom-impact API.
 */
public final class SpellEngineAuthorityAdapter {
    private static final String MOD_ID = "spell_engine";
    private static final String SPELL_EVENTS = "net.spell_engine.api.spell.event.SpellEvents";
    private static final String SPELL_HANDLERS = "net.spell_engine.api.spell.event.SpellHandlers";
    private static final String CASTING_ATTEMPT_LISTENER =
            "net.spell_engine.api.spell.event.SpellEvents$CastingAttemptEvent";
    private static final String COST_CONSUME_LISTENER =
            "net.spell_engine.api.spell.event.SpellEvents$SpellCostConsumeEvent";
    private static final String SPELL_CAST_LISTENER =
            "net.spell_engine.api.spell.event.SpellEvents$SpellCastEvent";
    private static final String CUSTOM_IMPACT =
            "net.spell_engine.api.spell.event.SpellHandlers$CustomImpact";
    private static final String IMPACT_RESULT =
            "net.spell_engine.api.spell.event.SpellHandlers$ImpactResult";
    private static final String SPELL_CAST_ATTEMPT =
            "net.spell_engine.internals.casting.SpellCast$Attempt";
    private static final String SPELL_CASTER_ENTITY =
            "net.spell_engine.internals.casting.SpellCaster$Entity";
    private static final String SPELL_CAST_PROCESS =
            "net.spell_engine.internals.casting.SpellCast$Process";
    private static final String PROJECT_IMPACT_HANDLER = OpenworldRpgMod.MOD_ID + ":project_impact";

    private static final SpellCastAuthority AUTHORITY = new SpellCastAuthority(OpenworldRpgMod.MOD_ID);
    private static final PlayerCombatStateStore COMBAT_STATES = CombatStateServices.states();

    private static volatile ProcessBinding processBinding = ProcessBinding.disabled();
    private static volatile boolean initialized;
    private static volatile boolean canonicalPoliciesRegistered;

    private SpellEngineAuthorityAdapter() {
    }

    public static SpellCastAuthority authority() {
        return AUTHORITY;
    }

    public static PlayerCombatStateStore combatStates() {
        return COMBAT_STATES;
    }

    public static synchronized void initialize(RuntimeProfile profile, Logger logger) {
        if (initialized) {
            return;
        }

        registerCanonicalPolicies();

        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            processBinding = ProcessBinding.disabled();
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
            Class<?> spellHandlers = Class.forName(SPELL_HANDLERS, false, loader);
            Class<?> customImpact = Class.forName(CUSTOM_IMPACT, false, loader);
            Class<?> impactResult = Class.forName(IMPACT_RESULT, false, loader);
            Class<?> spellCasterEntity = Class.forName(SPELL_CASTER_ENTITY, false, loader);
            Class<?> spellCastProcess = Class.forName(SPELL_CAST_PROCESS, false, loader);

            Method attemptNone = spellCastAttempt.getMethod("none");
            Constructor<?> impactResultConstructor = impactResult.getDeclaredConstructor(boolean.class, boolean.class);
            Method getSpellCastProcess = spellCasterEntity.getMethod("getSpellCastProcess");
            Method processId = spellCastProcess.getMethod("id");
            processBinding = new ProcessBinding(
                    spellCasterEntity,
                    spellCastProcess,
                    getSpellCastProcess,
                    processId
            );

            Object stagedAttempt = spellEvents.getField("CASTING_ATTEMPT").get(null);
            Object preAttemptEvent = stagedAttempt.getClass().getField("PRE").get(stagedAttempt);
            Object postAttemptEvent = stagedAttempt.getClass().getField("POST").get(stagedAttempt);
            Object costConsumeEvent = spellEvents.getField("COST_CONSUME").get(null);
            Object spellCastEvent = spellEvents.getField("SPELL_CAST").get(null);

            registerListener(
                    preAttemptEvent,
                    castingAttemptListener,
                    (proxy, method, args) -> handleCastingAttempt(proxy, method, args, attemptNone, false)
            );
            registerListener(
                    postAttemptEvent,
                    castingAttemptListener,
                    (proxy, method, args) -> handleCastingAttempt(proxy, method, args, attemptNone, true)
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
            registerCustomImpact(spellHandlers, customImpact, impactResultConstructor);

            initialized = true;
            logger.info(
                    "Openworld RPG Spell Engine authority gate armed for profile {} using CASTING_ATTEMPT.PRE/POST, "
                            + "cast-process continuation identity, COST_CONSUME, SPELL_CAST and custom impact {}.",
                    profile.id(),
                    PROJECT_IMPACT_HANDLER
            );
        } catch (ReflectiveOperationException exception) {
            processBinding = ProcessBinding.disabled();
            throw new IllegalStateException(
                    "Spell Engine is loaded, but the pinned Openworld RPG event/impact authority contract could not be resolved.",
                    exception
            );
        }
    }

    private static void registerCanonicalPolicies() {
        if (canonicalPoliciesRegistered) {
            return;
        }

        ProjectSpellSpec arcBolt = ProjectSpellSpec.arcBolt();
        AUTHORITY.registerPolicy(
                arcBolt.id(),
                new ProjectSpellTransactionPolicy(
                        arcBolt,
                        COMBAT_STATES,
                        ProjectSpellTransactionPolicy.SpellImpactPort.directMagic()
                )
        );
        canonicalPoliciesRegistered = true;
    }

    private static Object handleCastingAttempt(
            Object proxy,
            Method method,
            Object[] args,
            Method attemptNone,
            boolean acceptedStage
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

        Object spellEntry = invokeAccessor(eventArgs, "spell");
        String spellId = spellId(spellEntry);
        if (AUTHORITY.owns(spellId) && !readDonorCostContract(spellEntry).isNeutralForProjectAuthority()) {
            return invokeStatic(attemptNone);
        }

        long gameTick = player.level().getGameTime();
        boolean engineContinuation = isEngineContinuation(player, spellId);
        SpellCastAuthority.AttemptDecision decision = acceptedStage
                ? AUTHORITY.commitAcceptedCast(player.getUUID(), spellId, gameTick, engineContinuation)
                : AUTHORITY.preflightAttempt(player.getUUID(), spellId, gameTick, engineContinuation);

        return switch (decision) {
            case PASS_THROUGH, ALLOW -> null;
            case BLOCK -> invokeStatic(attemptNone);
        };
    }

    private static boolean isEngineContinuation(Player player, String spellId)
            throws ReflectiveOperationException {
        ProcessBinding current = processBinding;
        if (!current.enabled()) {
            return false;
        }
        if (!current.spellCasterEntity().isInstance(player)) {
            throw new IllegalStateException(
                    "Spell Engine is loaded, but the server Player does not expose the pinned caster process interface."
            );
        }

        Object process;
        try {
            process = current.getSpellCastProcess().invoke(player);
        } catch (InvocationTargetException exception) {
            throw unwrapInvocation(exception);
        }
        if (process == null) {
            return false;
        }
        if (!current.spellCastProcess().isInstance(process)) {
            throw new IllegalStateException("Spell Engine returned an unexpected cast process: " + process.getClass());
        }

        Object id;
        try {
            id = current.processId().invoke(process);
        } catch (InvocationTargetException exception) {
            throw unwrapInvocation(exception);
        }
        return spellId.equals(String.valueOf(id));
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
                AUTHORITY.onEngineCostConsumed(player.getUUID(), spellId, player.level().getGameTime());
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
                        player.level().getGameTime(),
                        String.valueOf(action),
                        progress.floatValue()
                );
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static void registerCustomImpact(
            Class<?> spellHandlers,
            Class<?> customImpactType,
            Constructor<?> impactResultConstructor
    ) throws ReflectiveOperationException {
        Object rawMap = spellHandlers.getField("customImpact").get(null);
        if (!(rawMap instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Spell Engine customImpact registry is not a Map.");
        }

        Map<String, Object> handlers = (Map<String, Object>) map;
        if (handlers.containsKey(PROJECT_IMPACT_HANDLER)) {
            throw new IllegalStateException("Duplicate Spell Engine custom impact handler: " + PROJECT_IMPACT_HANDLER);
        }

        Object listener = Proxy.newProxyInstance(
                SpellEngineAuthorityAdapter.class.getClassLoader(),
                new Class<?>[]{customImpactType},
                (proxy, method, args) -> handleCustomImpact(proxy, method, args, impactResultConstructor)
        );
        handlers.put(PROJECT_IMPACT_HANDLER, listener);
    }

    private static Object handleCustomImpact(
            Object proxy,
            Method method,
            Object[] args,
            Constructor<?> impactResultConstructor
    ) throws ReflectiveOperationException {
        if (isObjectMethod(method)) {
            return objectMethod(proxy, method, args);
        }
        if (!"onSpellImpact".equals(method.getName()) || args == null || args.length != 5) {
            throw new IllegalStateException("Unexpected Spell Engine custom impact invocation: " + method);
        }

        Object spellEntry = args[0];
        Object spellPower = args[1];
        LivingEntity caster = requireLiving(args[2]);
        Entity target = args[3] instanceof Entity entity ? entity : null;
        Object impactContext = args[4];

        if (!(caster instanceof Player player) || player.level().isClientSide()) {
            return impactResultConstructor.newInstance(false, false);
        }

        String spellId = spellId(spellEntry);
        if (!AUTHORITY.owns(spellId)) {
            return impactResultConstructor.newInstance(false, false);
        }

        Object powerValue = invokeAccessor(spellPower, "baseValue");
        if (!(powerValue instanceof Number power)) {
            throw new IllegalStateException("Spell Engine spell power baseValue is not numeric: " + powerValue);
        }
        Object totalValue = invokeSingleArgumentMethod(impactContext, "total", spellEntry);
        if (!(totalValue instanceof Number total)) {
            throw new IllegalStateException("Spell Engine impact total is not numeric: " + totalValue);
        }

        if (!(target instanceof LivingEntity livingTarget)
                || livingTarget == player
                || livingTarget.level() != player.level()) {
            return impactResultConstructor.newInstance(false, false);
        }

        var sourceSnapshot = CombatStateServices.combatSnapshots()
                .snapshot(player.getUUID())
                .orElse(null);
        var targetSnapshot = ExternalActorBindingRuntime.combatProfile(livingTarget)
                .map(profile -> profile.projectTargetSnapshot())
                .orElse(null);
        if (sourceSnapshot == null || targetSnapshot == null) {
            return impactResultConstructor.newInstance(false, false);
        }

        SpellCastAuthority.ImpactDecision decision = AUTHORITY.onImpact(
                player.getUUID(),
                spellId,
                player.level().getGameTime(),
                livingTarget.getId(),
                power.doubleValue(),
                total.doubleValue(),
                sourceSnapshot,
                targetSnapshot
        );
        if (!decision.accepted()) {
            return impactResultConstructor.newInstance(false, false);
        }

        boolean applied = ProjectMinecraftDamageApplicator.applyDirectMagic(
                player,
                livingTarget,
                decision.finalDamage()
        );
        return impactResultConstructor.newInstance(applied, applied && decision.critical());
    }

    private static SpellEngineDonorCostContract readDonorCostContract(Object spellEntry)
            throws ReflectiveOperationException {
        Object spell = invokeAccessor(spellEntry, "value");
        Object cost = spell.getClass().getField("cost").get(spell);
        if (cost == null) {
            throw new IllegalStateException("Project Spell Engine spell has no cost object.");
        }

        boolean batching = cost.getClass().getField("batching").getBoolean(cost);
        double exhaust = ((Number) cost.getClass().getField("exhaust").get(cost)).doubleValue();
        int durability = ((Number) cost.getClass().getField("durability").get(cost)).intValue();
        boolean hasEffectCost = cost.getClass().getField("effect_id").get(cost) != null;
        boolean hasItemCost = cost.getClass().getField("item").get(cost) != null;

        Object cooldown = cost.getClass().getField("cooldown").get(cost);
        if (cooldown == null) {
            throw new IllegalStateException("Project Spell Engine spell has no cooldown object.");
        }

        boolean hasCooldownGroup = cooldown.getClass().getField("group").get(cooldown) != null;
        double attemptCooldownSeconds =
                ((Number) cooldown.getClass().getField("attempt_duration").get(cooldown)).doubleValue();
        double cooldownSeconds =
                ((Number) cooldown.getClass().getField("duration").get(cooldown)).doubleValue();

        return new SpellEngineDonorCostContract(
                batching,
                exhaust,
                durability,
                hasEffectCost,
                hasItemCost,
                hasCooldownGroup,
                attemptCooldownSeconds,
                cooldownSeconds
        );
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

    private static LivingEntity requireLiving(Object value) {
        if (!(value instanceof LivingEntity living)) {
            throw new IllegalStateException("Spell Engine impact caster is not a LivingEntity: " + value);
        }
        return living;
    }

    private static String spellId(Object spellEntry) throws ReflectiveOperationException {
        Object optionalKey = invokeFirstAvailable(spellEntry, "unwrapKey", "getKey");
        if (!(optionalKey instanceof Optional<?> optional) || optional.isEmpty()) {
            throw new IllegalStateException("Spell Engine spell registry entry has no key: " + spellEntry);
        }

        Object key = optional.get();
        Object location = invokeFirstAvailable(key, "location", "identifier", "getValue");
        String id = String.valueOf(location);
        if (id.indexOf(':') <= 0) {
            throw new IllegalStateException("Spell Engine registry key did not resolve to a resource id: " + id);
        }
        return id;
    }

    private static Object invokeAccessor(Object target, String name) throws ReflectiveOperationException {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (InvocationTargetException exception) {
            throw unwrapInvocation(exception);
        }
    }

    private static Object invokeSingleArgumentMethod(Object target, String name, Object argument)
            throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                try {
                    return method.invoke(target, argument);
                } catch (IllegalArgumentException ignored) {
                    // Continue until the erased/runtime parameter accepts the Holder instance.
                } catch (InvocationTargetException exception) {
                    throw unwrapInvocation(exception);
                }
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + name + "(...)");
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

    private record ProcessBinding(
            Class<?> spellCasterEntity,
            Class<?> spellCastProcess,
            Method getSpellCastProcess,
            Method processId
    ) {
        private static ProcessBinding disabled() {
            return new ProcessBinding(null, null, null, null);
        }

        private boolean enabled() {
            return spellCasterEntity != null;
        }
    }
}
