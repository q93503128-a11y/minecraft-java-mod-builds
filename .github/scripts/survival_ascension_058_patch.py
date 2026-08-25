#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "projects/survival-ascension"


def read(rel: str) -> str:
    return (PROJECT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = PROJECT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str) -> None:
    text = read(rel)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{rel}: expected one anchor, got {count}: {old[:100]!r}")
    write(rel, text.replace(old, new, 1))


def replace_count(rel: str, old: str, new: str, expected: int) -> None:
    text = read(rel)
    count = text.count(old)
    if count != expected:
        raise RuntimeError(f"{rel}: expected {expected} anchors, got {count}: {old[:100]!r}")
    write(rel, text.replace(old, new))


def prepend_after_title(rel: str, section: str) -> None:
    text = read(rel)
    anchor = "# Survival Ascension\n\n"
    if anchor not in text:
        raise RuntimeError(f"{rel}: title anchor missing")
    if section.strip() in text:
        return
    write(rel, text.replace(anchor, anchor + section.rstrip() + "\n\n", 1))


# Version / protocol / content-pack lock.
replace_once("gradle.properties", "mod_version=0.57.0-alpha.1", "mod_version=0.58.0-alpha.1")
replace_once("modpack/content-lock.json", '"version": "0.57.0-alpha.1-content-preview.1"', '"version": "0.58.0-alpha.1-content-preview.1"')

# Persist one construction-length preference in the existing skill SavedData. Legacy 0 resolves to max unlocked.
rel = "src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java"
replace_once(rel,
'''    private record PlayerEntry(String uuid, Map<String, Long> skills, long legacyMiningXp, boolean introduced) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                SKILL_XP_CODEC.optionalFieldOf("skills", Map.of()).forGetter(PlayerEntry::skills),
                Codec.LONG.optionalFieldOf("mining_xp", 0L).forGetter(PlayerEntry::legacyMiningXp),
                Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerEntry::introduced)
        ).apply(instance, PlayerEntry::new));
    }
''',
'''    private record PlayerEntry(String uuid, Map<String, Long> skills, long legacyMiningXp, boolean introduced,
                               int constructionLength) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                SKILL_XP_CODEC.optionalFieldOf("skills", Map.of()).forGetter(PlayerEntry::skills),
                Codec.LONG.optionalFieldOf("mining_xp", 0L).forGetter(PlayerEntry::legacyMiningXp),
                Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerEntry::introduced),
                Codec.INT.optionalFieldOf("construction_length", 0).forGetter(PlayerEntry::constructionLength)
        ).apply(instance, PlayerEntry::new));
    }
''')
replace_once(rel,
'''    private static final class PlayerState {
        private final Map<String, Long> xp = new HashMap<>();
        private boolean introduced;

        private PlayerState(Map<String, Long> xp, long legacyMiningXp, boolean introduced) {
            xp.forEach((id, value) -> this.xp.put(id, Math.max(0L, value)));
            if (!this.xp.containsKey(SkillType.MINING.id()) && legacyMiningXp > 0L) {
                this.xp.put(SkillType.MINING.id(), legacyMiningXp);
            }
            this.introduced = introduced;
        }
    }
''',
'''    private static final class PlayerState {
        private final Map<String, Long> xp = new HashMap<>();
        private boolean introduced;
        private int constructionLength;

        private PlayerState(Map<String, Long> xp, long legacyMiningXp, boolean introduced, int constructionLength) {
            xp.forEach((id, value) -> this.xp.put(id, Math.max(0L, value)));
            if (!this.xp.containsKey(SkillType.MINING.id()) && legacyMiningXp > 0L) {
                this.xp.put(SkillType.MINING.id(), legacyMiningXp);
            }
            this.introduced = introduced;
            this.constructionLength = Math.max(0, constructionLength);
        }
    }
''')
replace_once(rel,
'''            players.put(entry.uuid(), new PlayerState(entry.skills(), entry.legacyMiningXp(), entry.introduced()));
''',
'''            players.put(entry.uuid(), new PlayerState(entry.skills(), entry.legacyMiningXp(), entry.introduced(), entry.constructionLength()));
''')
replace_once(rel,
'''        players.forEach((uuid, state) -> result.add(new PlayerEntry(uuid, Map.copyOf(state.xp), 0L, state.introduced)));
''',
'''        players.forEach((uuid, state) -> result.add(new PlayerEntry(
                uuid, Map.copyOf(state.xp), 0L, state.introduced, state.constructionLength)));
''')
replace_once(rel,
'''        players.put(key, new PlayerState(Map.of(), 0L, false));
''',
'''        players.put(key, new PlayerState(Map.of(), 0L, false, 0));
''')
replace_once(rel,
'''    public long xp(ServerPlayer player, SkillType skill) { return state(player).xp.getOrDefault(skill.id(), 0L); }
    public int level(ServerPlayer player, SkillType skill) { return SkillTuning.levelFromXp(xp(player, skill)); }
    public Map<String, Long> snapshot(ServerPlayer player) { return Map.copyOf(state(player).xp); }
''',
'''    public long xp(ServerPlayer player, SkillType skill) { return state(player).xp.getOrDefault(skill.id(), 0L); }
    public int level(ServerPlayer player, SkillType skill) { return SkillTuning.levelFromXp(xp(player, skill)); }
    public Map<String, Long> snapshot(ServerPlayer player) { return Map.copyOf(state(player).xp); }
    public int constructionLengthSelection(ServerPlayer player) { return state(player).constructionLength; }

    public void setConstructionLengthSelection(ServerPlayer player, int length) {
        PlayerState state = state(player);
        int clamped = Math.max(0, length);
        if (state.constructionLength == clamped) return;
        state.constructionLength = clamped;
        setDirty();
    }
''')

# Server-authoritative selectable line/causeway length.
rel = "src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java"
replace_once(rel,
'''    private static final int CAUSEWAY_WIDTH = 3;
    private static final Map<UUID, ConstructionMode> MODES = new HashMap<>();
''',
'''    private static final int CAUSEWAY_WIDTH = 3;
    private static final int[] CONSTRUCTION_LENGTHS = {5, 9, 17, 33, 49, 65};
    private static final Map<UUID, ConstructionMode> MODES = new HashMap<>();
''')
replace_once(rel,
'''            if (resolved == ConstructionMode.CAUSEWAY) {
                int length = ExpeditionProgression.hasFieldMastery(player) && level >= 100 ? 65 : SkillTuning.constructionLineLength(level);
                player.sendSystemMessage(Component.literal("§7바라보는 수평 방향으로 §e3폭 × " + length + "칸§7의 실제 도로/교량 바닥을 시공합니다. Shift는 단일 배치."));
            }
        }
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
''',
'''            if (resolved == ConstructionMode.LINE || resolved == ConstructionMode.CAUSEWAY) {
                int length = selectedLength(player, level);
                String shape = resolved == ConstructionMode.CAUSEWAY ? "3폭 × " + length + "칸" : length + "칸";
                player.sendSystemMessage(Component.literal("§7현재 길이 §e" + shape + "§7 · 건축 메뉴에서 Shift+클릭으로 변경. 실제 배치 중 Shift는 단일 배치."));
            }
        }
    }

    public static void cycleLength(ServerPlayer player) {
        int level = SkillProgressData.get(player).level(player, SkillType.CONSTRUCTION);
        int max = maxUnlockedLength(player, level);
        if (max < 5) {
            player.sendSystemMessage(Component.literal("§6[건축] §f길이 선택은 건축 Lv.10부터 사용할 수 있습니다."));
            return;
        }
        int current = selectedLength(player, level);
        int next = CONSTRUCTION_LENGTHS[0];
        for (int i = 0; i < CONSTRUCTION_LENGTHS.length; i++) {
            int candidate = CONSTRUCTION_LENGTHS[i];
            if (candidate > max) break;
            if (candidate == current) {
                int following = i + 1 < CONSTRUCTION_LENGTHS.length ? CONSTRUCTION_LENGTHS[i + 1] : CONSTRUCTION_LENGTHS[0];
                next = following <= max ? following : CONSTRUCTION_LENGTHS[0];
                SkillProgressData.get(player).setConstructionLengthSelection(player, next);
                player.sendSystemMessage(Component.literal("§6[건축 길이] §f선/도로 배치 길이: §e" + next + "칸 §7(서버 해금 상한 " + max + ")"));
                return;
            }
            next = candidate;
        }
        SkillProgressData.get(player).setConstructionLengthSelection(player, next);
        player.sendSystemMessage(Component.literal("§6[건축 길이] §f선/도로 배치 길이: §e" + next + "칸 §7(서버 해금 상한 " + max + ")"));
    }

    private static int selectedLength(ServerPlayer player, int level) {
        int max = maxUnlockedLength(player, level);
        if (max < 5) return 1;
        int stored = SkillProgressData.get(player).constructionLengthSelection(player);
        if (stored <= 0) return max;
        int resolved = CONSTRUCTION_LENGTHS[0];
        for (int length : CONSTRUCTION_LENGTHS) {
            if (length > max || length > stored) break;
            resolved = length;
        }
        return Math.min(resolved, max);
    }

    private static int maxUnlockedLength(ServerPlayer player, int level) {
        return level >= 100 && ExpeditionProgression.hasFieldMastery(player)
                ? 65
                : SkillTuning.constructionLineLength(level);
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
''')
replace_once(rel,
'''        if (mode == ConstructionMode.LINE) {
            int size = fieldMastery ? 65 : SkillTuning.constructionLineLength(level);
''',
'''        if (mode == ConstructionMode.LINE) {
            int size = selectedLength(player, level);
''')
replace_once(rel,
'''        if (mode == ConstructionMode.CAUSEWAY) {
            int length = fieldMastery ? 65 : SkillTuning.constructionLineLength(level);
''',
'''        if (mode == ConstructionMode.CAUSEWAY) {
            int length = selectedLength(player, level);
''')

# New no-data packet: client asks the server to cycle within the unlocked list; it never sends a desired length.
write("src/main/java/kr/moonseungjun/survivalascension/network/ConstructionLengthPayload.java", '''package kr.moonseungjun.survivalascension.network;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ConstructionLengthPayload() implements CustomPacketPayload {
    public static final Type<ConstructionLengthPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "construction_length"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConstructionLengthPayload> CODEC = StreamCodec.unit(new ConstructionLengthPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
''')

rel = "src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java"
replace_once(rel, 'private static final String PROTOCOL = "8";', 'private static final String PROTOCOL = "9";')
replace_once(rel,
'''        registrar.playToServer(ConstructionModePayload.TYPE, ConstructionModePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ConstructionProgression.setMode(player, ConstructionMode.fromId(payload.modeId()));
                }));
''',
'''        registrar.playToServer(ConstructionModePayload.TYPE, ConstructionModePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ConstructionProgression.setMode(player, ConstructionMode.fromId(payload.modeId()));
                }));
        registrar.playToServer(ConstructionLengthPayload.TYPE, ConstructionLengthPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ConstructionProgression.cycleLength(player);
                }));
''')

rel = "src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java"
replace_once(rel,
'''import kr.moonseungjun.survivalascension.network.ConstructionModePayload;
''',
'''import kr.moonseungjun.survivalascension.network.ConstructionLengthPayload;
import kr.moonseungjun.survivalascension.network.ConstructionModePayload;
''')
replace_once(rel,
'''        String caption="건축 Lv."+level+" · Shift = 강제 단일 · 도로/교량은 진행 방향 시공";
''',
'''        String caption="건축 Lv."+level+" · Shift+클릭(선/도로)=길이 변경 · 실제 배치 중 Shift=강제 단일";
''')
replace_once(rel,
'''        if(entry.back()){this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());return true;}
        if(ClientSkillState.level(SkillType.CONSTRUCTION)<entry.mode().requiredLevel())return true;
        ClientPacketDistributor.sendToServer(new ConstructionModePayload(entry.mode().id()));
        this.minecraft.gui.setScreen(null);
''',
'''        if(entry.back()){this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());return true;}
        if(ClientSkillState.level(SkillType.CONSTRUCTION)<entry.mode().requiredLevel())return true;
        if(hasShiftDown()&&(entry.mode()==ConstructionMode.LINE||entry.mode()==ConstructionMode.CAUSEWAY)){
            ClientPacketDistributor.sendToServer(new ConstructionLengthPayload());
            return true;
        }
        ClientPacketDistributor.sendToServer(new ConstructionModePayload(entry.mode().id()));
        this.minecraft.gui.setScreen(null);
''')

# Reuse ExpeditionData's already-persisted generic integer map for a one-time content-pack compatibility guard.
rel = "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java"
replace_once(rel,
'''    private static final int ALL_REGIONS_MASK = (1 << ExpeditionRegion.values().length) - 1;
    private static final Codec<Map<String, Integer>> INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
''',
'''    private static final int ALL_REGIONS_MASK = (1 << ExpeditionRegion.values().length) - 1;
    private static final String TBS_JOURNAL_GUARD_KEY = "_compat.tbos_archivists_journal_checked";
    private static final Codec<Map<String, Integer>> INT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);
''')
replace_once(rel,
'''    public boolean incidentResolved(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).incidentRewardMask & region.bit()) != 0;
    }
''',
'''    public boolean incidentResolved(ServerPlayer player, ExpeditionRegion region) {
        return (state(player).incidentRewardMask & region.bit()) != 0;
    }

    public boolean tbsJournalGuardChecked(ServerPlayer player) {
        return state(player).progress.getOrDefault(TBS_JOURNAL_GUARD_KEY, 0) > 0;
    }

    public boolean markTbsJournalGuardChecked(ServerPlayer player) {
        State state = state(player);
        if (state.progress.getOrDefault(TBS_JOURNAL_GUARD_KEY, 0) > 0) return false;
        state.progress.put(TBS_JOURNAL_GUARD_KEY, 1);
        setDirty();
        return true;
    }
''')

# Dependency-free TBS compatibility: one delayed login cleanup of exactly one stale auto-granted journal.
rel = "src/main/java/kr/moonseungjun/survivalascension/compat/ContentPackCompatibility.java"
replace_once(rel,
'''import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.registries.Registries;
''',
'''import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
''')
replace_once(rel,
'''import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;
''',
'''import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
''')
replace_once(rel,
'''    private static final TagKey<EntityType<?>> EXPEDITION_MAJOR_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );

    private ContentPackCompatibility() {}
''',
'''    private static final TagKey<EntityType<?>> EXPEDITION_MAJOR_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );
    private static final Identifier TBS_ARCHIVISTS_JOURNAL = Identifier.fromNamespaceAndPath("tbos", "archivists_journal");
    private static final int TBS_JOURNAL_CHECK_DELAY_TICKS = 60;
    private static final Map<UUID, Long> TBS_JOURNAL_CHECK_READY = new HashMap<>();

    private ContentPackCompatibility() {}
''')
replace_once(rel,
'''    /** Data-driven major-target contract. Optional content IDs live in datapack JSON, never here. */
    public static boolean isMajorExpeditionTarget(LivingEntity entity) {
        return entity.getType().builtInRegistryHolder().is(EXPEDITION_MAJOR_TARGETS);
    }
}
''',
'''    /** Data-driven major-target contract. Optional content IDs live in datapack JSON, never here. */
    public static boolean isMajorExpeditionTarget(LivingEntity entity) {
        return entity.getType().builtInRegistryHolder().is(EXPEDITION_MAJOR_TARGETS);
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
}
''')

# Rare/high-reward incident tier, physical perimeter particles, and multiplayer overlap admission.
rel = "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java"
replace_once(rel,
'''import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
''',
'''import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
''')
replace_once(rel,
'''    private static final int OUTSIDE_GRACE_TICKS = 200;
    private static final double EVENT_RADIUS = 48.0D;
    private static final Map<UUID, ActiveIncident> ACTIVE = new HashMap<>();
''',
'''    private static final int OUTSIDE_GRACE_TICKS = 200;
    private static final double EVENT_RADIUS = 48.0D;
    private static final double RARE_CHANCE = 0.15D;
    private static final int RARE_EXTRA_TIME_TICKS = 300;
    private static final int OVERLAP_RETRY_TICKS = 600;
    private static final double INCIDENT_CENTER_CLEARANCE = EVENT_RADIUS * 2.0D + 16.0D;
    private static final int BOUNDARY_POINTS = 16;
    private static final Map<UUID, ActiveIncident> ACTIVE = new HashMap<>();
''')
replace_once(rel,
'''        start(player, level, region, ExpeditionIncident.random(region, level.getRandom()));
''',
'''        start(player, level, region, ExpeditionIncident.random(region, level.getRandom()),
                level.getRandom().nextDouble() < RARE_CHANCE);
''')
replace_once(rel,
'''        active.actionProgress = Math.min(active.incident.actionTarget(), active.actionProgress + amount);
        updateBossBar(active);
        if (active.actionProgress >= active.incident.actionTarget()) complete(player, active);
    }

    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region, ExpeditionIncident incident) {
        long now = level.getGameTime();
        player.getPersistentData().putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        ActiveIncident active = new ActiveIncident(player.getUUID(), level, player.blockPosition(), incident, now + incident.durationTicks());

        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> spawned = spawnAmbush(player, active);
            int minimum = Math.max(3, incident.spawnCount() * 2 / 3);
''',
'''        active.actionProgress = Math.min(active.actionTarget(), active.actionProgress + amount);
        updateBossBar(active);
        if (active.actionProgress >= active.actionTarget()) complete(player, active);
    }

    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region, ExpeditionIncident incident, boolean rare) {
        long now = level.getGameTime();
        BlockPos center = player.blockPosition();
        if (overlapsActiveIncident(level, center)) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }
        player.getPersistentData().putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        ActiveIncident active = new ActiveIncident(player.getUUID(), level, center, incident,
                now + incident.durationTicks() + (rare ? RARE_EXTRA_TIME_TICKS : 0), rare);

        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> spawned = spawnAmbush(player, active);
            int minimum = Math.max(3, active.spawnTarget() * 2 / 3);
''')
replace_once(rel,
'''        updateBossBar(active);
        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            player.sendSystemMessage(Component.literal("§c[현장 사건] §f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 반경 48블록 안에서 습격대 " + active.initialMobCount + "체를 정리하세요."));
        } else {
            player.sendSystemMessage(Component.literal("§6[현장 사건] §f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 제한시간 안에 " + incident.action().koreanName() + " §e" + incident.actionTarget() + "§7을 수행하세요."));
        }
''',
'''        updateBossBar(active);
        String prefix = rare ? "§d[희귀 현장 사건] " : (incident.kind() == ExpeditionIncident.Kind.AMBUSH ? "§c[현장 사건] " : "§6[현장 사건] ");
        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            player.sendSystemMessage(Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 표시된 반경 48블록 안에서 습격대 " + active.initialMobCount + "체를 정리하세요."
                    + (rare ? " §d· 강화 보상" : "")));
        } else {
            player.sendSystemMessage(Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()
                    + " §7· 표시된 반경 48블록 안에서 제한시간 내 " + incident.action().koreanName() + " §e" + active.actionTarget() + "§7을 수행하세요."
                    + (rare ? " §d· 강화 보상" : "")));
        }
''')
replace_once(rel,
'''        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
''',
'''        if (now % 20L == 0L) renderBoundary(active);

        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
''')
replace_once(rel,
'''        for (int i = 0; i < active.incident.spawnCount(); i++) {
            String typeId = types.get(i % types.size());
            Mob mob = spawnOne(active.level, active.center, active.incident.region() == ExpeditionRegion.OCEAN,
                    typeId, i, active.incident.spawnCount());
''',
'''        int spawnTarget = active.spawnTarget();
        for (int i = 0; i < spawnTarget; i++) {
            String typeId = types.get(i % types.size());
            Mob mob = spawnOne(active.level, active.center, active.incident.region() == ExpeditionRegion.OCEAN,
                    typeId, i, spawnTarget);
''')
replace_once(rel,
'''        int stage = active.incident.region().requiredWorldStage();
        int skillXp = 100 + stage * 50;
        SkillProgressionService.award(player, active.incident.region().rewardSkill(), skillXp);
        if (stage == 0) {
            giveOrDrop(player, new ItemStack(Items.EMERALD, 4));
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, 8));
        } else if (stage == 1) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 2));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 4));
        } else {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 4));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 8));
        }

        player.sendSystemMessage(Component.literal("§a[현장 사건 해결] §f" + active.incident.region().koreanName() + " · §e"
                + active.incident.koreanName() + " §7· " + active.incident.region().rewardSkill().koreanName()
                + " 숙련 XP +" + skillXp));

        ExpeditionDirective.Task bonusTask = data.firstIncompleteTask(player, active.incident.region());
        if (bonusTask != null) {
            int bonus = Math.max(1, bonusTask.target() / 5);
''',
'''        int stage = active.incident.region().requiredWorldStage();
        int skillXp = (100 + stage * 50) * (active.rare ? 2 : 1);
        SkillProgressionService.award(player, active.incident.region().rewardSkill(), skillXp);
        if (stage == 0) {
            giveOrDrop(player, new ItemStack(Items.EMERALD, active.rare ? 10 : 4));
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, active.rare ? 20 : 8));
        } else if (stage == 1) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, active.rare ? 5 : 2));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, active.rare ? 10 : 4));
        } else {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, active.rare ? 8 : 4));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, active.rare ? 16 : 8));
        }

        player.sendSystemMessage(Component.literal((active.rare ? "§d[희귀 현장 사건 해결] " : "§a[현장 사건 해결] ")
                + "§f" + active.incident.region().koreanName() + " · §e" + active.incident.koreanName() + " §7· "
                + active.incident.region().rewardSkill().koreanName() + " 숙련 XP +" + skillXp));

        ExpeditionDirective.Task bonusTask = data.firstIncompleteTask(player, active.incident.region());
        if (bonusTask != null) {
            int bonus = Math.max(1, bonusTask.target() / (active.rare ? 3 : 5));
''')
replace_once(rel,
'''            active.bossBar.setName(Component.literal("§6현장 사건 §7[" + active.incident.koreanName() + "] §f"
                    + active.actionProgress + "/" + active.incident.actionTarget() + " · " + seconds + "초"));
            float progress = active.incident.actionTarget() <= 0 ? 0.0F
                    : (float) active.actionProgress / active.incident.actionTarget();
''',
'''            active.bossBar.setName(Component.literal((active.rare ? "§d희귀 현장 사건 " : "§6현장 사건 ")
                    + "§7[" + active.incident.koreanName() + "] §f" + active.actionProgress + "/" + active.actionTarget()
                    + " · " + seconds + "초"));
            float progress = active.actionTarget() <= 0 ? 0.0F
                    : (float) active.actionProgress / active.actionTarget();
''')
replace_once(rel,
'''            active.bossBar.setName(Component.literal("§c현장 사건 §7[" + active.incident.koreanName() + "] §f적 "
                    + active.mobIds.size() + " · " + seconds + "초"));
''',
'''            active.bossBar.setName(Component.literal((active.rare ? "§d희귀 현장 사건 " : "§c현장 사건 ")
                    + "§7[" + active.incident.koreanName() + "] §f적 " + active.mobIds.size() + " · " + seconds + "초"));
''')
replace_once(rel,
'''    private static void removeStaleServerIncidents(MinecraftServer server) {
''',
'''    private static boolean overlapsActiveIncident(ServerLevel level, BlockPos center) {
        double clearanceSqr = INCIDENT_CENTER_CLEARANCE * INCIDENT_CENTER_CLEARANCE;
        for (ActiveIncident active : ACTIVE.values()) {
            if (active.level != level) continue;
            double dx = center.getX() - active.center.getX();
            double dy = center.getY() - active.center.getY();
            double dz = center.getZ() - active.center.getZ();
            if (dx * dx + dy * dy + dz * dz < clearanceSqr) return true;
        }
        return false;
    }

    private static void renderBoundary(ActiveIncident active) {
        ParticleOptions particle = active.rare ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD;
        double y = active.center.getY() + 1.1D;
        for (int i = 0; i < BOUNDARY_POINTS; i++) {
            double angle = Math.PI * 2.0D * i / BOUNDARY_POINTS;
            double x = active.center.getX() + 0.5D + Math.cos(angle) * EVENT_RADIUS;
            double z = active.center.getZ() + 0.5D + Math.sin(angle) * EVENT_RADIUS;
            active.level.sendParticles(particle, x, y, z, 1, 0.0D, 0.12D, 0.0D, 0.0D);
        }
        active.level.sendParticles(particle, active.center.getX() + 0.5D, y, active.center.getZ() + 0.5D,
                active.rare ? 4 : 2, 0.35D, 0.15D, 0.35D, 0.0D);
    }

    private static void removeStaleServerIncidents(MinecraftServer server) {
''')
replace_once(rel,
'''        final long deadline;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
''',
'''        final long deadline;
        final boolean rare;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
''')
replace_once(rel,
'''        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.incident = incident;
            this.deadline = deadline;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("현장 사건"),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }
''',
'''        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline, boolean rare) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.incident = incident;
            this.deadline = deadline;
            this.rare = rare;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal(rare ? "희귀 현장 사건" : "현장 사건"),
                    rare ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }

        int actionTarget() {
            int base = incident.actionTarget();
            return rare && base > 0 ? Math.max(base + 1, (base * 3 + 1) / 2) : base;
        }

        int spawnTarget() {
            int base = incident.spawnCount();
            return rare && base > 0 ? Math.max(base + 2, (base * 3 + 1) / 2) : base;
        }
''')

# Wire the optional-content one-shot guard and bump runtime version.
rel = "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
replace_once(rel,
'''import kr.moonseungjun.survivalascension.command.AscensionCommands;
''',
'''import kr.moonseungjun.survivalascension.command.AscensionCommands;
import kr.moonseungjun.survivalascension.compat.ContentPackCompatibility;
''')
replace_once(rel,
'''    public static final String VERSION = "0.57.0-alpha.1";
    // 0.57: pre-test stabilization keeps enlarged mining/tree work loaded-only and closes expedition origin-action gaps.
''',
'''    public static final String VERSION = "0.58.0-alpha.1";
    // 0.58: field incidents gain rare physical scale/perimeters and multiplayer admission; construction length becomes selectable.
''')
replace_once(rel,
'''        NeoForge.EVENT_BUS.addListener(ExpeditionProgression::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ExpeditionIncidentSystem::onPlayerTick);
''',
'''        NeoForge.EVENT_BUS.addListener(ExpeditionProgression::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ContentPackCompatibility::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ContentPackCompatibility::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ContentPackCompatibility::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ExpeditionIncidentSystem::onPlayerTick);
''')
replace_once(rel,
'''LOGGER.info("Survival Ascension {} loaded: pre-test chunk/accounting hardening + scaled mastery + ranged shooter attribution + spear momentum drive lines + mace outer impact rings + shield guard waves + ranged projectile snapshots/impact bursts + armor affix progression + regional 3/6/9 logistics + frontline freight/local supply + tagged major targets + shovel earthworks + optional expedition biome tags + content-pack gear imprint + physical logistics/freight + civil works + destructible bastion defense", VERSION);''',
'''LOGGER.info("Survival Ascension {} loaded: rare bounded field incidents + visible incident perimeters + multiplayer incident admission + selectable server-authoritative construction length + one-shot TBS journal guard + pre-test chunk/accounting hardening + scaled mastery + ranged shooter attribution + spear momentum drive lines + mace outer impact rings + shield guard waves + ranged projectile snapshots/impact bursts + armor affix progression + regional 3/6/9 logistics + frontline freight/local supply + tagged major targets + shovel earthworks + optional expedition biome tags + content-pack gear imprint + physical logistics/freight + civil works + destructible bastion defense", VERSION);''')

# Keep the baseline regression audit aware of the intentional protocol bump.
replace_once("tools/test_current_source.py", 'need(network, [\'PROTOCOL = "8"\'], "protocol")', 'need(network, [\'PROTOCOL = "9"\'], "protocol")')

# Release source audit: version bump + explicit 0.58 contracts.
rel = "tools/test_release_source.py"
replace_once(rel, 'REQUIRED_VERSION = "0.57.0-alpha.1"', 'REQUIRED_VERSION = "0.58.0-alpha.1"')
replace_count(rel, 'VERSION = \\\"0.57.0-alpha.1\\\"', 'VERSION = \\\"0.58.0-alpha.1\\\"', 3)
replace_once(rel, '"Mod version: `0.57.0-alpha.1`"', '"Mod version: `0.58.0-alpha.1`"')
text = read(rel)
audit_anchor = '''if errors:\n    print("RELEASE SOURCE AUDIT FAIL")\n'''
if text.count(audit_anchor) != 1:
    raise RuntimeError("release audit insertion anchor drift")
block = '''# 0.58 bounded field incidents, construction-length selection and optional-content onboarding guard.\nskill_data58 = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillProgressData.java")\nconstruction58 = read("src/main/java/kr/moonseungjun/survivalascension/construction/ConstructionProgression.java")\nconstruction_ui58 = read("src/main/java/kr/moonseungjun/survivalascension/client/ConstructionRadialMenuScreen.java")\nnetwork58 = read("src/main/java/kr/moonseungjun/survivalascension/network/SkillNetwork.java")\nlength_payload58 = read("src/main/java/kr/moonseungjun/survivalascension/network/ConstructionLengthPayload.java")\nincident58 = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionIncidentSystem.java")\nexpedition_data58 = read("src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionData.java")\ncompat58 = read("src/main/java/kr/moonseungjun/survivalascension/compat/ContentPackCompatibility.java")\nneed(skill_data58, ["construction_length", "constructionLengthSelection", "setConstructionLengthSelection"], "0.58 persisted construction length")\nneed(construction58, ["CONSTRUCTION_LENGTHS = {5, 9, 17, 33, 49, 65}", "cycleLength(ServerPlayer player)", "selectedLength(ServerPlayer player, int level)", "maxUnlockedLength", "setConstructionLengthSelection"], "0.58 server-authoritative construction length")\nneed(construction_ui58, ["ConstructionLengthPayload", "hasShiftDown()", "Shift+클릭(선/도로)=길이 변경"], "0.58 construction length UI")\nneed(network58, [\'PROTOCOL = "9"\', "ConstructionLengthPayload.TYPE", "ConstructionProgression.cycleLength(player)"], "0.58 network protocol")\nneed(length_payload58, [\'"construction_length"\', "StreamCodec.unit(new ConstructionLengthPayload())"], "0.58 no-data construction cycle payload")\nneed(incident58, ["RARE_CHANCE = 0.15D", "INCIDENT_CENTER_CLEARANCE", "overlapsActiveIncident", "renderBoundary", "ParticleTypes.END_ROD", "ParticleTypes.TOTEM_OF_UNDYING", "active.actionTarget()", "active.spawnTarget()", "BossBarColor.PURPLE"], "0.58 rare/multiplayer incident runtime")\nneed(expedition_data58, ["_compat.tbos_archivists_journal_checked", "tbsJournalGuardChecked", "markTbsJournalGuardChecked"], "0.58 TBS guard persistence")\nneed(compat58, [\'Identifier.fromNamespaceAndPath("tbos", "archivists_journal")\', 'ModList.get().isLoaded("tbos")', "removeOneInitialTbsJournal", "stack.shrink(1)"], "0.58 TBS journal one-shot guard")\nforbid(compat58, ["com.nightbeam", "setChunkForced", "addRegionTicket", "getChunk("], "0.58 optional-content/force-load policy")\nforbid(incident58 + construction58, ["setChunkForced", "addRegionTicket", "getChunk("], "0.58 field/construction force-load policy")\nneed(project_doc, ["## 0.58 Field Incident & Construction Control", "Network protocol: `9`"], "0.58 PROJECT docs")\nneed(readme, ["## 0.58.0-alpha.1", "희귀 현장 사건", "Shift+클릭", "protocol `9`"], "0.58 README docs")\nneed(changelog, ["## 0.58.0-alpha.1", "0.58.0-alpha.1-content-preview.1"], "0.58 CHANGELOG docs")\n\n'''
write(rel, text.replace(audit_anchor, block + audit_anchor, 1))

# Release content-pack audit version. External mod versions are intentionally unchanged.
rel = "tools/test_release_content_pack.py"
replace_once(rel, 'REQUIRED_LOCK_VERSION = "0.57.0-alpha.1-content-preview.1"', 'REQUIRED_LOCK_VERSION = "0.58.0-alpha.1-content-preview.1"')
replace_once(rel, "baseline = baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.57.0-alpha.1`')", "baseline = baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.58.0-alpha.1`')")

# Documentation: concise release section, preserving older 0.57 regression notes.
rel = "PROJECT.md"
replace_once(rel, "- Mod version: `0.57.0-alpha.1`", "- Mod version: `0.58.0-alpha.1`")
replace_once(rel, "- Network protocol: `8`", "- Network protocol: `9`")
replace_once(rel,
'''- Existing-world compatibility: no new SavedData ID or migration.''',
'''- Existing-world compatibility: no new SavedData ID. 0.58 extends the existing skill profile codec with optional `construction_length=0` (legacy 0 = max unlocked), and reuses the existing expedition progress integer map for one one-time TBS onboarding guard. All older saves decode with defaults.''')
project = read(rel)
anchor = "## Core direction\n"
section = '''## 0.58 Field Incident & Construction Control / 현장 사건·건축 제어\n- 15% of eligible field incidents become rare incidents with about 1.5x physical objective/enemy scale, +15s time, doubled mastery XP and upgraded vanilla-material rewards.\n- Active incidents render a bounded 48-block particle perimeter once per second. No block scan, force-load, custom entity or background simulation is added.\n- New incident centers are refused inside a 112-block clearance from another active incident in the same ServerLevel, preventing players' incident arenas from overlapping. Progress and spawned-mob UUID sets remain owner-scoped.\n- LINE/CAUSEWAY length is selectable from unlocked 5/9/17/33/49/65 steps. Legacy selection 0 resolves to the current maximum; 65 requires Lv100 + Field Mastery. The client sends only a cycle request and the server recomputes the legal maximum.\n- Construction menu Shift+click on LINE/CAUSEWAY cycles length; Shift during actual placement remains the single-block precision override.\n- A delayed, one-time compatibility guard removes at most one `tbos:archivists_journal` from inventory when TBS is present, then persists completion in existing expedition data. No TBS implementation class is linked.\n- Network protocol: `9`. Content-pack dependency versions remain unchanged.\n\n'''
if section.strip() not in project:
    if anchor not in project:
        raise RuntimeError("PROJECT core anchor missing")
    write(rel, project.replace(anchor, section + anchor, 1))

rel = "README.md"
replace_once(rel, "Network protocol `8`.", "Network protocol `9`.")
readme = read(rel)
anchor = "Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.\n\n"
section = '''## 0.58.0-alpha.1 — Field Incident & Construction Control / 현장 사건·건축 제어\n희귀 현장 사건이 추가됐다. 일반 사건 발생 시 15% 확률로 희귀 등급이 되며 실제 적/행동 목표가 약 1.5배 커지고 제한시간이 15초 늘어난 대신 숙련 XP가 2배, 바닐라 물자 보상이 강화된다. 사건 중심에는 48블록 경계가 초당 한 번 입자로 표시되며, 다른 플레이어의 활성 사건 중심과 112블록 이내에서는 새 사건을 열지 않아 멀티플레이 사건 영역이 겹치지 않는다.\n\n건축 선/도로 길이는 해금된 5/9/17/33/49/65 단계 중 선택할 수 있다. 건축 메뉴에서 선 또는 도로/교량에 Shift+클릭하면 길이가 순환하며, 실제 블록 배치 중 Shift는 기존대로 강제 단일 배치다. 클라이언트는 원하는 숫자를 보내지 않고 순환 요청만 보내며 서버가 레벨·현장 숙련을 다시 검사한다. 기존 세이브의 선택값 0은 자동으로 현재 최대 길이로 해석된다.\n\nTBS가 설치된 경우 초기 접속 직후 `tbos:archivists_journal`이 남아 있는 오래된/중복 온보딩 상태를 한 번만 검사해 최대 1개만 정리한다. 검사는 기존 원정 SavedData 안에 완료 플래그를 남기며 TBS 구현 클래스를 import하지 않는다. Network protocol `9`; 외부 콘텐츠팩 버전은 그대로다.\n\n'''
if section.strip() not in readme:
    if anchor not in readme:
        raise RuntimeError("README intro anchor missing")
    write(rel, readme.replace(anchor, anchor + section, 1))

rel = "CHANGELOG.md"
changelog = read(rel)
changelog_anchor = "# Changelog\n\n"
section = '''## 0.58.0-alpha.1\n- Added a 15% rare tier to bounded expedition field incidents: ~1.5x physical target scale, +15s, 2x mastery XP and stronger vanilla-material rewards.\n- Added a once-per-second 48-block incident perimeter and same-level 112-block active-incident center clearance for multiplayer isolation.\n- Added persistent, server-authoritative 5/9/17/33/49/65 LINE/CAUSEWAY length selection; Shift+click cycles in the construction radial while placement Shift stays precision-single.\n- Added a one-time delayed `tbos:archivists_journal` compatibility cleanup using registry identity only; no TBS implementation dependency.\n- Network protocol 9. External content dependencies remain locked to the same files.\n- Content pack release: `0.58.0-alpha.1-content-preview.1`.\n\n'''
if section.strip() not in changelog:
    if changelog_anchor not in changelog:
        raise RuntimeError("CHANGELOG title anchor missing")
    write(rel, changelog.replace(changelog_anchor, changelog_anchor + section, 1))

rel = "TESTING.md"
testing = read(rel)
if "## 0.58 focused checks" not in testing:
    write(rel, testing.rstrip() + '''\n\n## 0.58 focused checks\n- Two players: trigger eligible incidents near each other; the second incident must not start within 112 blocks of the first center, and neither player's kills/actions may complete the other's incident.\n- Incident perimeter: verify the 48-block ring appears about once per second without chunk generation or hitching; rare incidents use the distinct rare presentation.\n- Rare incident: verify target scale is larger, reward is stronger, and completion still consumes only that region's one incident reward.\n- Construction LINE/CAUSEWAY: Shift+click cycles only through currently unlocked 5/9/17/33/49/65 lengths; relog preserves the choice; a spoofed client cannot request a locked numeric length because the packet has no length field.\n- Actual placement Shift still creates only the origin block even after selecting a bulk length.\n- Existing 0.57 save with no construction selection starts at its maximum currently unlocked length.\n- With TBS installed, first guarded login removes at most one initial `tbos:archivists_journal`; later legitimately obtained journals must survive relog because the guard is already marked complete.\n''')

print("Survival Ascension 0.58 patch applied")
