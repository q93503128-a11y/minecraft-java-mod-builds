package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Deterministic, scoutable front assignment and side/rear siege routing. */
public final class VillageAttackPlanSystem {
    private static final Map<UUID, Front> ACTIVE_FRONTS = new HashMap<>();
    private static final Map<Integer, Integer> JOIN_INDEX = new HashMap<>();
    private static int attackTicks;
    private static int warningTicks;
    private static int warningDay = -1;
    private static VillageTimePhase lastPhase;

    private VillageAttackPlanSystem() {}

    public static void reset() {
        ACTIVE_FRONTS.clear();
        JOIN_INDEX.clear();
        attackTicks = 0;
        warningTicks = 0;
        warningDay = -1;
        lastPhase = null;
    }

    public static void onRaidEnemyJoin(ServerLevel level, Mob mob) {
        if (level == null || mob == null || !VillageRaidSystem.isRaidEnemy(mob)) return;
        int day = VillageCouncilState.currentDay();
        int wave = parseWave(mob);
        int key = day * 100 + wave;
        int index = JOIN_INDEX.getOrDefault(key, 0);
        JOIN_INDEX.put(key, index + 1);
        Front front = frontForIndex(day, wave, index);
        ACTIVE_FRONTS.put(mob.getUUID(), front);
        Condition condition = condition(day, wave);
        applyCondition(mob, condition);
        if (front != Front.NORTH) {
            BlockPos spawn = spawnOrigin(front, index);
            mob.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
    }

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        VillageTimePhase phase = VillageCouncilState.currentPhase();
        int day = VillageCouncilState.currentDay();
        if (phase == VillageTimePhase.NIGHT && lastPhase != VillageTimePhase.NIGHT) {
            warningTicks = 240;
            warningDay = day;
            broadcastNightWarning(server, day);
        }
        lastPhase = phase;
        if (warningTicks > 0) {
            if (warningTicks % 40 == 0) renderWarnings(server.overworld(), day);
            warningTicks--;
        }
        if (!VillageRaidSystem.isActive()) {
            if (phase == VillageTimePhase.DAY) {
                ACTIVE_FRONTS.clear();
                JOIN_INDEX.clear();
            }
            return;
        }

        attackTicks++;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        for (UUID id : new HashSet<>(ACTIVE_FRONTS.keySet())) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                ACTIVE_FRONTS.remove(id);
                continue;
            }
            Front front = ACTIVE_FRONTS.getOrDefault(id, Front.NORTH);
            if (front == Front.NORTH) continue;
            VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.primarySideFor(front);
            if (insideFortress(center, mob.blockPosition())) continue;
            if (VillageSiegeSegmentSystem.breached(segment)) {
                BlockPos inside = VillageSiegeSegmentSystem.insideApproach(segment);
                mob.getNavigation().moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 1.18);
                continue;
            }
            BlockPos target = VillageSiegeSegmentSystem.attackPoint(segment, mob.blockPosition());
            mob.setTarget(null);
            mob.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5);
            mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.10);
            if (attackTicks % 30 == 0 && VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) {
                VillageEnemyArchetypeSystem.Archetype archetype = VillageRaidSystem.archetypeOf(mob);
                if (archetype == null) archetype = VillageEnemyArchetypeSystem.Archetype.GRUNT;
                int raw = Math.max(1, Math.round((7.0f + day * 0.65f)
                        * VillageEnemyArchetypeSystem.structureDamageMultiplier(archetype)
                        * VillageWarfrontSystem.structureDamageMultiplier(day)
                        * VillageDifficultyTuning.earlyStructureMultiplier(day)
                        * condition(day, parseWave(mob)).structureMultiplier()));
                VillageSiegeSegmentSystem.damage(server, segment, raw, mob.blockPosition());
                VillageEnemyArchetypeSystem.onStructureHit(level, mob, archetype);
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }

    public static Front frontOf(UUID uuid) { return ACTIVE_FRONTS.getOrDefault(uuid, Front.NORTH); }

    public static AttackPlan preview(int day, int wave, int count) {
        Map<Front, Integer> counts = new java.util.LinkedHashMap<>();
        for (int index = 0; index < Math.max(0, count); index++) {
            counts.merge(frontForIndex(day, wave, index), 1, Integer::sum);
        }
        Front main = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(Front.NORTH);
        List<String> detachments = new ArrayList<>();
        counts.forEach((front, amount) -> {
            if (front != main && amount > 0) detachments.add(front.displayName() + " 약 " + amount + "명");
        });
        String detachment = detachments.isEmpty() ? "없음" : String.join(" · ", detachments);
        Condition condition = condition(day, wave);
        return new AttackPlan(main, detachment, condition,
                warStage(day), specialThreat(day, wave));
    }

    public static String scoutLine(int day, int wave, int count) {
        AttackPlan plan = preview(day, wave, count);
        return "주공: " + plan.main().displayName()
                + "\n별동대: " + plan.detachment()
                + "\n전쟁 단계: " + plan.stage()
                + "\n전장 상황: " + plan.condition().displayName() + " · " + plan.condition().description()
                + "\n특수 위협: " + plan.specialThreat();
    }

    public static Front frontForIndex(int day, int wave, int index) {
        int i = Math.max(0, index);
        if (day <= 4) return Front.NORTH;
        if (day <= 7) {
            if (i % 5 != 0) return Front.NORTH;
            return ((day + wave) & 1) == 0 ? Front.NORTH_WEST : Front.NORTH_EAST;
        }
        if (day <= 11) {
            if (i % 4 != 0) return Front.NORTH;
            return ((day + wave) & 1) == 0 ? Front.WEST : Front.EAST;
        }
        if (day <= 15) {
            int lane = i % 5;
            if (lane == 0) return Front.WEST;
            if (lane == 1) return Front.EAST;
            return Front.NORTH;
        }
        int lane = i % 10;
        if (lane == 0) return Front.SOUTH_WEST;
        if (lane == 1) return Front.SOUTH_EAST;
        if (lane == 2 || lane == 3) return ((wave + i) & 1) == 0 ? Front.WEST : Front.EAST;
        return Front.NORTH;
    }

    public static Condition condition(int day, int wave) {
        if (day < 6) return Condition.CLEAR;
        int cycle = Math.floorMod(day * 7 + wave * 3, 4);
        return switch (cycle) {
            case 0 -> Condition.SIEGE_DRUMS;
            case 1 -> Condition.BLACK_FOG;
            case 2 -> Condition.SCORCHED_APPROACH;
            default -> Condition.CLEAR;
        };
    }

    private static void applyCondition(Mob mob, Condition condition) {
        int longTicks = 20 * 60 * 30;
        if (condition == Condition.SIEGE_DRUMS) {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, longTicks, 0));
        } else if (condition == Condition.BLACK_FOG) {
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, longTicks, 0));
        } else if (condition == Condition.SCORCHED_APPROACH) {
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, longTicks, 0));
        }
    }

    private static void broadcastNightWarning(MinecraftServer server, int day) {
        int players = VillageProgressionSystem.previewRaidPlayerCount(server);
        int wave = 1;
        VillageWaveTrait trait = VillageWaveTrait.select(day, wave);
        int count = VillageRaidSystem.previewWaveCount(day, wave, players, trait);
        AttackPlan plan = preview(day, wave, count);
        server.getPlayerList().broadcastSystemMessage(Component.literal(
                "§6[성벽 경계 신호] §f오늘 밤 주공은 §e" + plan.main().displayName()
                        + "§f입니다. 별동대: " + plan.detachment()
                        + "\n§7전장 상황: " + plan.condition().displayName()
                        + " · 위험 방향에는 연기 신호가 올라옵니다."), false);
    }

    private static void renderWarnings(ServerLevel level, int day) {
        int players = Math.max(1, level.getServer().getPlayerList().getPlayerCount());
        VillageWaveTrait trait = VillageWaveTrait.select(day, 1);
        int count = VillageRaidSystem.previewWaveCount(day, 1, players, trait);
        Map<Front, Boolean> used = new HashMap<>();
        for (int i = 0; i < count; i++) used.put(frontForIndex(day, 1, i), true);
        for (Front front : used.keySet()) {
            BlockPos pos = spawnOrigin(front, 0);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    front == preview(day, 1, count).main() ? 28 : 14, 2.2, 1.0, 2.2, 0.04);
            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                    8, 1.4, 0.25, 1.4, 0.02);
        }
    }

    private static BlockPos spawnOrigin(Front front, int index) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(new BlockPos(0, 0, 0));
        int d = VillageWorldSystem.ENEMY_SPAWN_DISTANCE;
        int spread = Math.floorMod(index, 7) * 3 - 9;
        return switch (front) {
            case NORTH -> center.offset(spread, 0, -d);
            case NORTH_WEST -> center.offset(-d + 20, 0, -d + 24 + spread);
            case NORTH_EAST -> center.offset(d - 20, 0, -d + 24 + spread);
            case WEST -> center.offset(-d, 0, spread);
            case EAST -> center.offset(d, 0, spread);
            case SOUTH_WEST -> center.offset(-36 + spread, 0, d);
            case SOUTH_EAST -> center.offset(36 + spread, 0, d);
        };
    }

    private static boolean insideFortress(BlockPos center, BlockPos pos) {
        int r = VillageWorldSystem.FORTRESS_RADIUS - 4;
        return Math.abs(pos.getX() - center.getX()) < r && Math.abs(pos.getZ() - center.getZ()) < r;
    }

    private static int parseWave(Mob mob) {
        Component name = mob.getCustomName();
        if (name == null) return Math.max(1, VillageRaidSystem.previewMaxWaves(VillageCouncilState.currentDay()));
        String text = ChatFormatting.stripFormatting(name.getString());
        if (text == null) return 1;
        int marker = text.indexOf("웨이브 ");
        if (marker < 0) return Math.max(1, VillageRaidSystem.previewMaxWaves(VillageCouncilState.currentDay()));
        int start = marker + 4;
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
        if (end <= start) return 1;
        try { return Math.max(1, Integer.parseInt(text.substring(start, end))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private static String warStage(int day) {
        if (day <= 4) return "약탈 공세";
        if (day <= 8) return "조직 전열";
        if (day <= 12) return "본격 공성";
        if (day <= 16) return "복합 침투전";
        return "다전선 총공세";
    }

    private static String specialThreat(int day, int wave) {
        if (day < 5) return "기본 전열·원거리 병력";
        if (day < 9) return "공병·파쇄병의 측면 압박 가능";
        if (day < 13) return "지원병·탑 사냥꾼·엘리트 혼성";
        if (day < 17) return "다전선 공성 + 침투 엘리트";
        return "후방 침투 + 복합 공성 + 보스 지휘 효과";
    }

    public record AttackPlan(Front main, String detachment, Condition condition, String stage, String specialThreat) {}

    public enum Front {
        NORTH("북문 정면"), NORTH_WEST("북서 성벽"), NORTH_EAST("북동 성벽"),
        WEST("서쪽 방벽"), EAST("동쪽 방벽"), SOUTH_WEST("후방 서측"), SOUTH_EAST("후방 동측");
        private final String displayName;
        Front(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public enum Condition {
        CLEAR("맑은 전장", "추가 전장 보정 없음", 1.0f),
        SIEGE_DRUMS("공성 북소리", "적 전열 공격력이 상승하고 구조물 압박이 15% 증가", 1.15f),
        BLACK_FOG("검은 안개", "적 기동 속도가 상승해 측면 대응 시간이 짧아짐", 1.05f),
        SCORCHED_APPROACH("불탄 진입로", "적이 화염 저항을 얻어 화염 방어만으로 막기 어려움", 1.05f);
        private final String displayName;
        private final String description;
        private final float structureMultiplier;
        Condition(String displayName, String description, float structureMultiplier) {
            this.displayName = displayName; this.description = description; this.structureMultiplier = structureMultiplier;
        }
        public String displayName() { return displayName; }
        public String description() { return description; }
        public float structureMultiplier() { return structureMultiplier; }
    }
}
