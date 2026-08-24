package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;
import java.util.Set;

/**
 * Bounded exploration-to-settlement progression glue.
 * It only observes structures at an online player's already-loaded position and direct player kills.
 * It never locates/generates structures, force-loads chunks, creates loot/resources or mutates companion content.
 */
public final class SettlementExplorationService {
    static final int STRUCTURE_SCAN_INTERVAL_TICKS = 100;
    static final float EXTERNAL_BOSS_MIN_HEALTH = 80.0F;
    private static final Set<String> EXCLUDED_STRUCTURE_NAMESPACES = Set.of("minecraft", FrontierSettlement.MOD_ID, "neoforge");
    private static final Set<String> VANILLA_CONQUEST_TARGETS = Set.of("minecraft:ender_dragon", "minecraft:wither");

    private SettlementExplorationService() {}

    public static boolean tick(MinecraftServer server, SettlementData data) {
        if (!data.founded() || server.getTickCount() % STRUCTURE_SCAN_INTERVAL_TICKS != 0) return false;
        Registry<Structure> registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        boolean changed = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) continue;
            if (!level.hasChunkAt(player.blockPosition())) continue;
            Map<Structure, ?> structures = level.structureManager().getAllStructuresAt(player.blockPosition());
            for (Structure structure : structures.keySet()) {
                Identifier id = registry.getKey(structure);
                if (id == null || !isExternalStructure(id)) continue;
                if (!level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure).isValid()) continue;
                if (!data.recordExternalStructure(id.toString())) continue;
                changed = true;
                player.sendSystemMessage(Component.literal("개척 발견 · 외부 구조물 " + id + " | 탐험 진척 " + data.explorationScore()));
            }
        }
        return changed;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = level.getServer();
        SettlementData data = SettlementData.get(server);
        if (!data.founded()) return;

        LivingEntity victim = event.getEntity();
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id == null || !isConquestTarget(victim, id)) return;
        if (!data.recordExternalBoss(id.toString())) return;

        player.sendSystemMessage(Component.literal("개척 정복 · 강적 " + id + " | 탐험 진척 " + data.explorationScore()));
        SettlementService.broadcast(server, data);
    }

    private static boolean isExternalStructure(Identifier id) {
        return !EXCLUDED_STRUCTURE_NAMESPACES.contains(id.getNamespace());
    }

    private static boolean isConquestTarget(LivingEntity victim, Identifier id) {
        if (VANILLA_CONQUEST_TARGETS.contains(id.toString())) return true;
        if (!(victim instanceof Mob) || victim.getMaxHealth() < EXTERNAL_BOSS_MIN_HEALTH) return false;
        String namespace = id.getNamespace();
        return !"minecraft".equals(namespace) && !FrontierSettlement.MOD_ID.equals(namespace) && !"neoforge".equals(namespace);
    }
}
