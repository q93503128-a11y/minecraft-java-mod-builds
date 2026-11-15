package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Opt-in CI/console-only compatibility audit. Never logs by default, never
 * mutates world registries or spawns entities, and never grants host control.
 *
 * This reports REAL registered EntityType IDs after all pinned mods are loaded.
 * A type being a Mob is not sufficient to possess it: scripted AI, ownership,
 * animations, original drops and boss phases must still be checked separately.
 */
public final class ExternalMobAudit {
    private static final String PROPERTY = "anomalousclasses.auditExternalMobs";
    private ExternalMobAudit() {}

    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(PROPERTY)) return;
        ServerLevel level = event.getServer().overworld();
        Map<String, Integer> counts = new TreeMap<>();
        Map<String, Integer> mobs = new HashMap<>();
        int errors = 0;
        int total = 0;
        System.out.println("ANOMALOUS_HOST_AUDIT_BEGIN|version=1");
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || "minecraft".equals(id.getNamespace())
                    || "anomalousclasses".equals(id.getNamespace())) continue;
            total++;
            String ns = id.getNamespace();
            counts.merge(ns, 1, Integer::sum);
            String entityClass = "unknown";
            String isMob = "unknown";
            try {
                // Only construct for reflection; never call addFreshEntity or
                // place/activate any external creature in the world.
                Entity sample = type.create(level);
                if (sample != null) {
                    entityClass = sample.getClass().getName();
                    isMob = Boolean.toString(sample instanceof Mob);
                    if (sample instanceof Mob) mobs.merge(ns, 1, Integer::sum);
                } else {
                    isMob = "null";
                }
            } catch (Exception | LinkageError failure) {
                errors++;
                entityClass = "ERROR_" + failure.getClass().getSimpleName();
            }
            System.out.println("ANOMALOUS_HOST_AUDIT_ENTITY|"
                    + id + "|mob=" + isMob + "|class=" + entityClass
                    + "|category=" + type.getCategory()
                    + "|declaredHost=" + (ParasiteRules.supported(id) != null));
        }
        for (var entry : counts.entrySet()) {
            System.out.println("ANOMALOUS_HOST_AUDIT_NAMESPACE|"
                    + entry.getKey() + "|total=" + entry.getValue()
                    + "|mobs=" + mobs.getOrDefault(entry.getKey(), 0));
        }
        System.out.println("ANOMALOUS_HOST_AUDIT_END|types=" + total
                + "|namespaces=" + counts.size() + "|constructorErrors=" + errors);
    }
}
