package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dependency-free compatibility seam for content supplied by the Survival Ascension pack.
 *
 * External implementation classes are never referenced here. Registry IDs, vanilla/NeoForge tags,
 * and Survival-owned data tags are the only contracts. This keeps the standalone JAR loadable while
 * allowing the locked pack to be audited and used when those mods are actually present.
 */
public final class ContentPackCompatibility {
    private static final TagKey<EntityType<?>> EXPEDITION_MAJOR_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );
    private static final Identifier TBS_ARCHIVISTS_JOURNAL = Identifier.fromNamespaceAndPath("tbos", "archivists_journal");
    private static final int TBS_JOURNAL_CHECK_DELAY_TICKS = 60;
    private static final List<String> PACK_NAMESPACES = List.of("tbos", "amethyst_resonance");
    private static final Map<UUID, Long> TBS_JOURNAL_CHECK_READY = new HashMap<>();
    private static volatile Map<String, NamespaceCensus> CENSUS = Map.of();
    private static volatile Map<String, List<Identifier>> AFFIX_GEAR_IDS = Map.of();

    private ContentPackCompatibility() {}

    /** Hostile mobs remain the normal combat target; common-tagged bosses and Survival-tagged
     * major targets are included even when their implementation does not use Minecraft's Enemy marker. */
    public static boolean isCombatTarget(LivingEntity entity) {
        return entity instanceof Enemy
                || entity.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)
                || isMajorExpeditionTarget(entity);
    }

    /** Data-driven major-target contract. Optional content IDs live in datapack JSON, never here. */
    public static boolean isMajorExpeditionTarget(LivingEntity entity) {
        return entity.getType().builtInRegistryHolder().is(EXPEDITION_MAJOR_TARGETS);
    }

    /**
     * Scan the actually loaded registries after datapacks/tags are ready. The resulting numbers are
     * authoritative for the exact locked runtime used by the server, rather than estimates from mod pages.
     */
    public static synchronized void refreshRegistryCensus() {
        Map<String, NamespaceCensus> census = new LinkedHashMap<>();
        Map<String, List<Identifier>> gearByNamespace = new LinkedHashMap<>();

        for (String namespace : PACK_NAMESPACES) {
            List<String> entityIds = new ArrayList<>();
            List<String> monsterIds = new ArrayList<>();
            List<String> incidentCandidateIds = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                if (!namespace.equals(id.getNamespace())) continue;
                entityIds.add(id.toString());
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                if (type == null || type.getCategory() != MobCategory.MONSTER) continue;
                monsterIds.add(id.toString());
                if (isConservativeIncidentCandidate(id, type)) incidentCandidateIds.add(id.toString());
            }

            List<String> itemIds = new ArrayList<>();
            List<String> gearIds = new ArrayList<>();
            List<Identifier> gearRegistryIds = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                if (!namespace.equals(id.getNamespace())) continue;
                itemIds.add(id.toString());
                var item = BuiltInRegistries.ITEM.getValue(id);
                if (item == null) continue;
                ItemStack stack = new ItemStack(item);
                if (!isStandardAffixGear(stack)) continue;
                gearIds.add(id.toString());
                gearRegistryIds.add(id);
            }

            entityIds.sort(String::compareTo);
            monsterIds.sort(String::compareTo);
            incidentCandidateIds.sort(String::compareTo);
            itemIds.sort(String::compareTo);
            gearIds.sort(String::compareTo);
            gearRegistryIds.sort((a, b) -> a.toString().compareTo(b.toString()));
            gearByNamespace.put(namespace, List.copyOf(gearRegistryIds));
            census.put(namespace, new NamespaceCensus(
                    namespace,
                    ModList.get().isLoaded(namespace),
                    entityIds.size(),
                    monsterIds.size(),
                    incidentCandidateIds.size(),
                    itemIds.size(),
                    gearIds.size(),
                    List.copyOf(incidentCandidateIds),
                    List.copyOf(gearIds)
            ));
        }

        CENSUS = Map.copyOf(census);
        AFFIX_GEAR_IDS = Map.copyOf(gearByNamespace);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        refreshRegistryCensus();
        for (String line : censusLines()) SurvivalAscension.LOGGER.info("[content-census] {}", line);
    }

    /** Human-readable runtime census for /ascension content and CI server-log auditing. */
    public static List<String> censusLines() {
        if (CENSUS.isEmpty()) refreshRegistryCensus();
        List<String> lines = new ArrayList<>();
        for (String namespace : PACK_NAMESPACES) {
            NamespaceCensus entry = CENSUS.get(namespace);
            if (entry == null) continue;
            lines.add("namespace=" + entry.namespace()
                    + " loaded=" + entry.loaded()
                    + " entities=" + entry.entityTypes()
                    + " monsters=" + entry.monsterTypes()
                    + " conservative_incident_candidates=" + entry.incidentCandidates()
                    + " items=" + entry.items()
                    + " affix_gear=" + entry.affixGear());
            lines.add("namespace=" + entry.namespace() + " incident_candidate_ids=" + String.join(",", entry.incidentCandidateIds()));
            lines.add("namespace=" + entry.namespace() + " affix_gear_ids=" + String.join(",", entry.affixGearIds()));
        }
        return List.copyOf(lines);
    }

    /**
     * Returns a real content-pack equipment base for high-rank elite bonus drops. Rank 2 prefers the
     * controlled Amethyst Resonance tier; rank 3 may draw from every compatible locked namespace.
     */
    public static ItemStack randomAffixGear(RandomSource random, int eliteRank) {
        if (AFFIX_GEAR_IDS.isEmpty()) refreshRegistryCensus();
        List<Identifier> pool = new ArrayList<>();
        if (eliteRank <= 2) pool.addAll(AFFIX_GEAR_IDS.getOrDefault("amethyst_resonance", List.of()));
        if (eliteRank >= 3 || pool.isEmpty()) {
            for (String namespace : PACK_NAMESPACES) pool.addAll(AFFIX_GEAR_IDS.getOrDefault(namespace, List.of()));
        }
        if (pool.isEmpty()) return ItemStack.EMPTY;
        int start = random.nextInt(pool.size());
        for (int offset = 0; offset < pool.size(); offset++) {
            Identifier id = pool.get((start + offset) % pool.size());
            var item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null) continue;
            ItemStack stack = new ItemStack(item);
            if (isStandardAffixGear(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isConservativeIncidentCandidate(Identifier id, EntityType<?> type) {
        if (type.getCategory() != MobCategory.MONSTER) return false;
        if (type.builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)
                || type.builtInRegistryHolder().is(EXPEDITION_MAJOR_TARGETS)) return false;
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        for (String blocked : List.of("boss", "curator", "cantor", "guardian", "warden", "anchor", "core", "projectile", "dummy")) {
            if (path.contains(blocked)) return false;
        }
        return true;
    }

    private static boolean isStandardAffixGear(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1 && (
                stack.is(ItemTags.SPEARS)
                        || stack.is(ItemTags.SWORDS)
                        || stack.is(Tags.Items.TOOLS_BOW)
                        || stack.is(Tags.Items.TOOLS_CROSSBOW)
                        || stack.is(ItemTags.PICKAXES)
                        || stack.is(ItemTags.AXES)
                        || stack.is(ItemTags.SHOVELS)
                        || stack.is(ItemTags.HOES)
                        || stack.is(Tags.Items.TOOLS_MACE)
                        || stack.is(Tags.Items.TOOLS_SHIELD)
                        || stack.is(ItemTags.HEAD_ARMOR)
                        || stack.is(ItemTags.CHEST_ARMOR)
                        || stack.is(ItemTags.LEG_ARMOR)
                        || stack.is(ItemTags.FOOT_ARMOR)
        );
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if (!ModList.get().isLoaded("tbos") || ExpeditionData.get(player).tbsJournalGuardChecked(player)) return;
        TBS_JOURNAL_CHECK_READY.put(player.getUUID(), player.level().getGameTime() + TBS_JOURNAL_CHECK_DELAY_TICKS);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TBS_JOURNAL_CHECK_READY.remove(event.getEntity().getUUID());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) || player.tickCount % 5 != 0) return;
        Long ready = TBS_JOURNAL_CHECK_READY.get(player.getUUID());
        if (ready == null || player.level().getGameTime() < ready) return;
        TBS_JOURNAL_CHECK_READY.remove(player.getUUID());
        if (!ModList.get().isLoaded("tbos")) return;
        ExpeditionData data = ExpeditionData.get(player);
        if (data.tbsJournalGuardChecked(player)) return;
        removeOneInitialTbsJournal(player);
        data.markTbsJournalGuardChecked(player);
    }

    private static boolean removeOneInitialTbsJournal(net.minecraft.server.level.ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !TBS_ARCHIVISTS_JOURNAL.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) continue;
            stack.shrink(1);
            inventory.setChanged();
            return true;
        }
        return false;
    }

    public record NamespaceCensus(
            String namespace,
            boolean loaded,
            int entityTypes,
            int monsterTypes,
            int incidentCandidates,
            int items,
            int affixGear,
            List<String> incidentCandidateIds,
            List<String> affixGearIds
    ) {}
}
