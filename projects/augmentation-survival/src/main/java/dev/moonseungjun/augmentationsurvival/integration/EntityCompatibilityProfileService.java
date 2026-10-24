package dev.moonseungjun.augmentationsurvival.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared identity/control profile boundary for vanilla and external scripted bosses.
 *
 * <p>Reward bands deliberately remain separate. Recognizing an entity as a boss here affects
 * threat scaling, boss-targeted augments, control policy and ordinary-loot exclusions, but does not
 * silently grant a core reward that has not been authored in the boss reward contract.</p>
 */
public final class EntityCompatibilityProfileService {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/entity_compatibility_profiles.json";

    private static volatile Snapshot current;
    private static volatile RuntimeBindings runtime = RuntimeBindings.empty();

    private EntityCompatibilityProfileService() {
    }

    public static void initialize() {
        current = loadBundled();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> bindRuntime());
        AugmentationSurvival.LOGGER.info(
                "Loaded entity compatibility profile contract: {} exact selectors, {} token selectors, {} dynamic adapters",
                current.exactSpecs().size(),
                current.tokenSpecs().size(),
                current.dynamicSpecs().size()
        );
    }

    public static Optional<Profile> resolve(LivingEntity entity) {
        Snapshot snapshot = current();
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId != null) {
            Profile exact = snapshot.exactProfiles().get(entityId.toString());
            if (exact != null) {
                return Optional.of(exact);
            }
            Profile runtimeProfile = runtime.tokenResolvedProfiles().get(entityId.toString());
            if (runtimeProfile != null) {
                return Optional.of(runtimeProfile);
            }
        }
        for (DynamicBinding binding : runtime.dynamicBindings()) {
            if (binding.matches(entity)) {
                return Optional.of(binding.profile());
            }
        }
        return Optional.empty();
    }

    public static boolean isBoss(LivingEntity entity) {
        return resolve(entity).map(Profile::boss).orElse(false);
    }

    public static boolean allowPull(LivingEntity entity) {
        return resolve(entity).map(Profile::allowPull).orElse(true);
    }

    public static double pullMultiplier(LivingEntity entity) {
        return resolve(entity).map(Profile::pullMultiplier).orElse(1.0D);
    }

    public static boolean allowPositionSwap(LivingEntity entity) {
        return resolve(entity).map(Profile::allowPositionSwap).orElse(true);
    }

    public static boolean allowTimeStop(LivingEntity entity) {
        return resolve(entity).map(Profile::allowTimeStop).orElse(true);
    }

    public static boolean allowStun(LivingEntity entity) {
        return resolve(entity).map(Profile::allowStun).orElse(true);
    }

    public static boolean allowPercentHpDamage(LivingEntity entity) {
        return resolve(entity).map(Profile::allowPercentHpDamage).orElse(true);
    }

    public static boolean allowWallImpact(LivingEntity entity) {
        return resolve(entity).map(Profile::allowWallImpact).orElse(true);
    }

    public static boolean allowDominate(LivingEntity entity) {
        return resolve(entity).map(Profile::allowDominate).orElse(true);
    }

    private static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Entity compatibility profiles are not initialized");
        }
        return snapshot;
    }

    private static synchronized void bindRuntime() {
        Snapshot snapshot = current();
        Map<String, Profile> resolvedTokens = new LinkedHashMap<>();
        List<DynamicBinding> dynamicBindings = new ArrayList<>();

        validateExactProfiles(snapshot);
        for (TokenSpec spec : snapshot.tokenSpecs()) {
            if (!requiredModLoaded(spec.requiredMod())) {
                continue;
            }
            Identifier resolved = resolveTokenSelector(spec);
            Profile previous = resolvedTokens.putIfAbsent(resolved.toString(), spec.profile());
            if (previous != null && previous != spec.profile()) {
                throw new IllegalStateException(
                        "Multiple compatibility profiles resolved to entity " + resolved
                );
            }
            AugmentationSurvival.LOGGER.info(
                    "Resolved compatibility token profile {} -> {}",
                    spec.profile().id(),
                    resolved
            );
        }

        for (DynamicSpec spec : snapshot.dynamicSpecs()) {
            if (!requiredModLoaded(spec.requiredMod())) {
                continue;
            }
            dynamicBindings.add(bindDynamic(spec));
        }

        runtime = new RuntimeBindings(
                Collections.unmodifiableMap(resolvedTokens),
                List.copyOf(dynamicBindings)
        );
        AugmentationSurvival.LOGGER.info(
                "Bound entity compatibility runtime: {} token profiles, {} dynamic adapters",
                resolvedTokens.size(),
                dynamicBindings.size()
        );
    }

    private static void validateExactProfiles(Snapshot snapshot) {
        for (ExactSpec spec : snapshot.exactSpecs()) {
            if (!requiredModLoaded(spec.requiredMod())) {
                continue;
            }
            if (!BuiltInRegistries.ENTITY_TYPE.keySet().contains(spec.entityId())) {
                throw new IllegalStateException(
                        "Required compatibility entity is missing for profile "
                                + spec.profile().id() + ": " + spec.entityId()
                );
            }
        }
    }

    private static Identifier resolveTokenSelector(TokenSpec spec) {
        List<Identifier> namespaceEntries = new ArrayList<>();
        for (Identifier candidate : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            if (candidate.getNamespace().equals(spec.namespace())) {
                namespaceEntries.add(candidate);
            }
        }

        List<Identifier> exactMatches = new ArrayList<>();
        for (Identifier candidate : namespaceEntries) {
            String normalizedPath = normalize(candidate.getPath());
            for (String token : spec.tokens()) {
                if (normalizedPath.equals(normalize(token))) {
                    exactMatches.add(candidate);
                    break;
                }
            }
        }
        if (exactMatches.size() == 1) {
            return exactMatches.getFirst();
        }
        if (exactMatches.size() > 1) {
            throw ambiguousToken(spec, exactMatches, namespaceEntries);
        }

        List<Identifier> containedMatches = new ArrayList<>();
        for (Identifier candidate : namespaceEntries) {
            String normalizedPath = normalize(candidate.getPath());
            for (String token : spec.tokens()) {
                String normalizedToken = normalize(token);
                if (!normalizedToken.isEmpty() && normalizedPath.contains(normalizedToken)) {
                    containedMatches.add(candidate);
                    break;
                }
            }
        }
        if (containedMatches.size() == 1) {
            return containedMatches.getFirst();
        }
        if (containedMatches.isEmpty()) {
            throw new IllegalStateException(
                    "Could not resolve compatibility token profile " + spec.profile().id()
                            + " in namespace " + spec.namespace()
                            + " using tokens " + spec.tokens()
                            + "; available entity ids=" + namespaceEntries
            );
        }
        throw ambiguousToken(spec, containedMatches, namespaceEntries);
    }

    private static IllegalStateException ambiguousToken(
            TokenSpec spec,
            List<Identifier> matches,
            List<Identifier> namespaceEntries
    ) {
        return new IllegalStateException(
                "Ambiguous compatibility token profile " + spec.profile().id()
                        + " using tokens " + spec.tokens()
                        + "; matches=" + matches
                        + "; namespace entity ids=" + namespaceEntries
        );
    }

    private static DynamicBinding bindDynamic(DynamicSpec spec) {
        try {
            ClassLoader loader = EntityCompatibilityProfileService.class.getClassLoader();
            Class<?> accessor = Class.forName(spec.accessorClass(), false, loader);
            Method method = accessor.getMethod(spec.booleanMethod());
            if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) {
                throw new IllegalStateException(
                        "Dynamic compatibility adapter method must return boolean: " + spec.booleanMethod()
                );
            }
            AugmentationSurvival.LOGGER.info(
                    "Bound dynamic compatibility adapter {} -> {}#{}",
                    spec.profile().id(),
                    spec.accessorClass(),
                    spec.booleanMethod()
            );
            return new DynamicBinding(spec.profile(), accessor, method);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to bind required compatibility adapter " + spec.profile().id(),
                    exception
            );
        }
    }

    private static boolean requiredModLoaded(String modId) {
        return modId == null || modId.isBlank() || FabricLoader.getInstance().isModLoaded(modId);
    }

    private static String normalize(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = EntityCompatibilityProfileService.class
                .getClassLoader()
                .getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing entity compatibility contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read entity compatibility contract", exception);
        }
    }

    private static Snapshot parse(JsonObject root) {
        int schema = root.get("schema_version").getAsInt();
        if (schema != 1) {
            throw new IllegalStateException("Unsupported entity compatibility schema: " + schema);
        }
        ControlDefaults defaults = parseDefaults(root.getAsJsonObject("default_boss_control"));
        Map<String, Profile> exactProfiles = new LinkedHashMap<>();
        List<ExactSpec> exactSpecs = new ArrayList<>();
        List<TokenSpec> tokenSpecs = new ArrayList<>();

        for (JsonElement element : root.getAsJsonArray("profiles")) {
            JsonObject object = element.getAsJsonObject();
            String id = requiredString(object, "id");
            String requiredMod = optionalString(object, "required_mod");
            JsonObject selector = object.getAsJsonObject("selector");
            String type = requiredString(selector, "type");
            Profile profile = parseProfile(id, object.getAsJsonObject("profile"), defaults, "");
            if (type.equals("entity")) {
                Identifier entityId = Identifier.tryParse(requiredString(selector, "value"));
                if (entityId == null) {
                    throw new IllegalStateException("Invalid compatibility entity id for " + id);
                }
                if (exactProfiles.putIfAbsent(entityId.toString(), profile) != null) {
                    throw new IllegalStateException("Duplicate compatibility entity selector: " + entityId);
                }
                exactSpecs.add(new ExactSpec(requiredMod, entityId, profile));
            } else if (type.equals("entity_path_tokens")) {
                String namespace = requiredString(selector, "namespace");
                List<String> tokens = stringList(selector.getAsJsonArray("tokens"));
                if (tokens.isEmpty()) {
                    throw new IllegalStateException("Empty compatibility token selector: " + id);
                }
                tokenSpecs.add(new TokenSpec(requiredMod, namespace, tokens, profile));
            } else {
                throw new IllegalStateException("Unsupported compatibility selector type: " + type);
            }
        }

        List<DynamicSpec> dynamicSpecs = new ArrayList<>();
        JsonArray dynamicArray = root.getAsJsonArray("dynamic_adapters");
        for (JsonElement element : dynamicArray) {
            JsonObject object = element.getAsJsonObject();
            String id = requiredString(object, "id");
            String requiredMod = requiredString(object, "required_mod");
            String accessorClass = requiredString(object, "accessor_class");
            String booleanMethod = requiredString(object, "boolean_method");
            Profile profile = parseProfile(
                    id,
                    object.getAsJsonObject("profile"),
                    defaults,
                    accessorClass + "#" + booleanMethod
            );
            dynamicSpecs.add(new DynamicSpec(
                    requiredMod,
                    accessorClass,
                    booleanMethod,
                    profile
            ));
        }

        return new Snapshot(
                Collections.unmodifiableMap(exactProfiles),
                List.copyOf(exactSpecs),
                List.copyOf(tokenSpecs),
                List.copyOf(dynamicSpecs)
        );
    }

    private static ControlDefaults parseDefaults(JsonObject object) {
        if (object == null) {
            throw new IllegalStateException("Missing default_boss_control");
        }
        return new ControlDefaults(
                optionalBoolean(object, "allow_pull", true),
                optionalDouble(object, "pull_multiplier", 1.0D),
                optionalBoolean(object, "allow_position_swap", true),
                optionalBoolean(object, "allow_time_stop", true),
                optionalBoolean(object, "allow_stun", true),
                optionalBoolean(object, "allow_percent_hp_damage", true),
                optionalBoolean(object, "allow_wall_impact", true),
                optionalBoolean(object, "allow_dominate", false)
        );
    }

    private static Profile parseProfile(
            String id,
            JsonObject object,
            ControlDefaults defaults,
            String customAdapter
    ) {
        JsonObject profile = object == null ? new JsonObject() : object;
        double pullMultiplier = optionalDouble(
                profile,
                "pull_multiplier",
                defaults.pullMultiplier()
        );
        if (!Double.isFinite(pullMultiplier) || pullMultiplier < 0.0D) {
            throw new IllegalStateException("Invalid pull multiplier for compatibility profile " + id);
        }
        return new Profile(
                id,
                optionalBoolean(profile, "boss", true),
                optionalBoolean(profile, "allow_pull", defaults.allowPull()),
                pullMultiplier,
                optionalBoolean(profile, "allow_position_swap", defaults.allowPositionSwap()),
                optionalBoolean(profile, "allow_time_stop", defaults.allowTimeStop()),
                optionalBoolean(profile, "allow_stun", defaults.allowStun()),
                optionalBoolean(profile, "allow_percent_hp_damage", defaults.allowPercentHpDamage()),
                optionalBoolean(profile, "allow_wall_impact", defaults.allowWallImpact()),
                optionalBoolean(profile, "allow_dominate", defaults.allowDominate()),
                customAdapter
        );
    }

    private static List<String> stringList(JsonArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (JsonElement element : array) {
            String value = element.getAsString();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static String requiredString(JsonObject object, String key) {
        String value = optionalString(object, key);
        if (value.isBlank()) {
            throw new IllegalStateException("Missing compatibility field: " + key);
        }
        return value;
    }

    private static String optionalString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static boolean optionalBoolean(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }

    private static double optionalDouble(JsonObject object, String key, double fallback) {
        return object != null && object.has(key) ? object.get(key).getAsDouble() : fallback;
    }

    public record Profile(
            String id,
            boolean boss,
            boolean allowPull,
            double pullMultiplier,
            boolean allowPositionSwap,
            boolean allowTimeStop,
            boolean allowStun,
            boolean allowPercentHpDamage,
            boolean allowWallImpact,
            boolean allowDominate,
            String customAdapter
    ) {
    }

    private record ControlDefaults(
            boolean allowPull,
            double pullMultiplier,
            boolean allowPositionSwap,
            boolean allowTimeStop,
            boolean allowStun,
            boolean allowPercentHpDamage,
            boolean allowWallImpact,
            boolean allowDominate
    ) {
    }

    private record ExactSpec(String requiredMod, Identifier entityId, Profile profile) {
    }

    private record TokenSpec(
            String requiredMod,
            String namespace,
            List<String> tokens,
            Profile profile
    ) {
    }

    private record DynamicSpec(
            String requiredMod,
            String accessorClass,
            String booleanMethod,
            Profile profile
    ) {
    }

    private record Snapshot(
            Map<String, Profile> exactProfiles,
            List<ExactSpec> exactSpecs,
            List<TokenSpec> tokenSpecs,
            List<DynamicSpec> dynamicSpecs
    ) {
    }

    private record RuntimeBindings(
            Map<String, Profile> tokenResolvedProfiles,
            List<DynamicBinding> dynamicBindings
    ) {
        static RuntimeBindings empty() {
            return new RuntimeBindings(Map.of(), List.of());
        }
    }

    private static final class DynamicBinding {
        private final Profile profile;
        private final Class<?> accessorClass;
        private final Method booleanMethod;
        private final Map<LivingEntity, Boolean> cache = Collections.synchronizedMap(new WeakHashMap<>());
        private boolean loggedInvocationFailure;

        private DynamicBinding(Profile profile, Class<?> accessorClass, Method booleanMethod) {
            this.profile = profile;
            this.accessorClass = accessorClass;
            this.booleanMethod = booleanMethod;
        }

        Profile profile() {
            return profile;
        }

        boolean matches(LivingEntity entity) {
            if (!accessorClass.isInstance(entity)) {
                return false;
            }
            synchronized (cache) {
                Boolean cached = cache.get(entity);
                if (cached != null) {
                    return cached;
                }
                boolean result = invoke(entity);
                cache.put(entity, result);
                return result;
            }
        }

        private boolean invoke(LivingEntity entity) {
            try {
                return Boolean.TRUE.equals(booleanMethod.invoke(entity));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                if (!loggedInvocationFailure) {
                    loggedInvocationFailure = true;
                    AugmentationSurvival.LOGGER.error(
                            "Dynamic compatibility adapter {} failed; treating unmatched entities as non-boss",
                            profile.id(),
                            exception
                    );
                }
                return false;
            }
        }
    }
}
