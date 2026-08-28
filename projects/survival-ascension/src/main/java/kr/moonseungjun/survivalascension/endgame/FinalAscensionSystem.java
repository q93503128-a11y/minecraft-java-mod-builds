package kr.moonseungjun.survivalascension.endgame;

import kr.moonseungjun.survivalascension.apex.ApexHuntSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionIncidentSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionOperationSystem;
import kr.moonseungjun.survivalascension.production.OutpostSiegeSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime first three acts of the final ascension. The permanent admission authority remains
 * {@link FinalAscensionProgression}; the unique boss and permanent world closure are owned by
 * {@link FinalAscensionBossSystem} and {@link FinalAscensionData}.
 *
 * <p>This system never writes expedition/Apex/infrastructure completion. Every temporary block is
 * placed only into already-loaded air and is removed only if it is still the exact marker that
 * Survival Ascension placed. Player-built repair blocks remain as real world construction.</p>
 */
public final class FinalAscensionSystem {
    private static final String OWNER_KEY = "survivalascension_final_ascension_owner";
    private static final String PHASE_KEY = "survivalascension_final_ascension_phase";

    private static final int TICK_INTERVAL = 5;
    private static final int OWNER_GRACE_TICKS = 120;
    private static final int PHASE_TIMEOUT_TICKS = 1800;
    private static final int ECHO_TIMEOUT_TICKS = 2400;
    private static final int SEAL_CHANNEL_TICKS = 80;
    private static final double PLAYER_RADIUS = 72.0D;
    private static final double MOB_RECALL_RADIUS = 48.0D;
    private static final double EXCLUSION_RADIUS = 128.0D;

    private static final int MINING_TARGETS = 6;
    private static final int BUILD_TARGETS = 4;
    private static final int MOVEMENT_TARGETS = 3;

    private static final ThreatSpec[] ACT_ONE_GUARDS = {
            new ThreatSpec("minecraft:vindicator", "전장 파쇄자"),
            new ThreatSpec("minecraft:wither_skeleton", "균열 추격자"),
            new ThreatSpec("minecraft:witch", "붕괴 주술사")
    };
    private static final ThreatSpec[] ECHO_SET_ONE = {
            new ThreatSpec("minecraft:spider", "삼림의 잔향"),
            new ThreatSpec("minecraft:skeleton", "건조지의 잔향"),
            new ThreatSpec("minecraft:drowned", "습지의 잔향")
    };
    private static final ThreatSpec[] ECHO_SET_TWO = {
            new ThreatSpec("minecraft:ravager", "고산의 잔향"),
            new ThreatSpec("minecraft:guardian", "대양의 잔향"),
            new ThreatSpec("minecraft:witch", "심층의 잔향")
    };
    private static final ThreatSpec[] ECHO_SET_THREE = {
            new ThreatSpec("minecraft:stray", "빙설의 잔향"),
            new ThreatSpec("minecraft:wither_skeleton", "네더의 잔향"),
            new ThreatSpec("minecraft:enderman", "엔드의 잔향")
    };
    private static final ThreatSpec[] COLLAPSE_GUARDS = {
            new ThreatSpec("minecraft:ravager", "붕괴 추격체"),
            new ThreatSpec("minecraft:witch", "붕괴 이변체"),
            new ThreatSpec("minecraft:vindicator", "붕괴 매복체")
    };

    private static final Map<UUID, Run> ACTIVE = new HashMap<>();
    private static int ticker;

    private FinalAscensionSystem() {}

    public static void tryStart(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§5[최후의 승천] §f크리에이티브/관전자 상태에서는 시작할 수 없습니다."));
            return;
        }

        removeStaleServerRuns(level.getServer());
        if (!FinalAscensionProgression.isReady(player)) {
            player.sendSystemMessage(Component.literal("§5[최후의 승천] §f아직 최후의 승천을 열 수 없습니다."));
            FinalAscensionProgression.sendStatus(player);
            return;
        }
        if (FinalAscensionData.get(level.getServer()).isComplete()) {
            player.sendSystemMessage(Component.literal("§d[최후의 승천] §f이 월드는 이미 최종 관문을 돌파했습니다."));
            FinalAscensionData.sendStatus(player);
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§5[최후의 승천] §f이미 진행 중입니다."));
            return;
        }
        if (hasConflictingActivity(player)) {
            player.sendSystemMessage(Component.literal("§5[최후의 승천] §f진행 중인 현장 사건·원정 작전·정점 사냥·방어전·승천 시련·최종 관문을 먼저 끝내세요."));
            return;
        }
        for (Run other : ACTIVE.values()) {
            if (other.level == level && distanceToCenterSqr(player, other.center) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) {
                player.sendSystemMessage(Component.literal("§5[최후의 승천] §f근처에서 다른 최후의 승천이 진행 중입니다. §7(128블록 간격 필요)"));
                return;
            }
        }

        BlockPos center = player.blockPosition();
        if (countOpenSpawnSlots(level, center) < 10) {
            player.sendSystemMessage(Component.literal("§5[최후의 승천] §f주변 공간이 좁습니다. 반경 8~14블록이 열린 지형에서 시작하세요."));
            return;
        }

        Run run = new Run(player.getUUID(), level, center);
        ACTIVE.put(player.getUUID(), run);
        run.bossBar.addPlayer(player);
        run.bossBar.setVisible(true);
        if (!enterPhase(run, player, Phase.ACT1_MINING)) {
            ACTIVE.remove(player.getUUID());
            fail(run, player, "첫 전장 목표를 배치할 열린 공간이 부족합니다.");
            return;
        }

        player.sendSystemMessage(Component.literal("§d[최후의 승천 개방] §f세계의 시험 → 아홉 지역의 잔향 → 붕괴 봉쇄 → 최종 관문을 한 전장에서 연속 돌파합니다."));
        player.sendSystemMessage(Component.literal("§7실제 채굴·건축·전투·기동 능력이 그대로 적용됩니다. 웅크리기는 기존처럼 정밀/안전 작업입니다."));
    }

    public static boolean isActive(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        removeStaleServerRuns(event.getServer());
        if (++ticker < TICK_INTERVAL) return;
        ticker = 0;
        if (ACTIVE.isEmpty()) return;

        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, Run> entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (tickRun(event.getServer(), entry.getValue())) finished.add(entry.getKey());
        }
        for (UUID owner : finished) ACTIVE.remove(owner);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        String ownerText = mob.getPersistentData().getStringOr(OWNER_KEY, "");
        if (ownerText.isEmpty()) return;

        UUID owner;
        try {
            owner = UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            event.setCanceled(true);
            return;
        }

        Run run = ACTIVE.get(owner);
        if (run == null || run.level != level || !run.mobIds.contains(mob.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static boolean tickRun(MinecraftServer server, Run run) {
        ServerPlayer owner = server.getPlayerList().getPlayer(run.owner);
        boolean ownerValid = owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == run.level
                && distanceToCenterSqr(owner, run.center) <= PLAYER_RADIUS * PLAYER_RADIUS;
        if (ownerValid) run.ownerAbsentTicks = 0;
        else run.ownerAbsentTicks += TICK_INTERVAL;
        syncBossBarPlayers(server, run);

        if (run.ownerAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(run, owner, "전장에서 이탈하거나 사망했습니다.");
            return true;
        }
        if (owner == null) return false;
        if (hasConflictingActivity(owner)) {
            fail(run, owner, "다른 대형 전투가 동시에 시작되어 전장을 유지할 수 없습니다.");
            return true;
        }

        long now = run.level.getGameTime();
        if (now >= run.phaseDeadline) {
            fail(run, owner, "현재 막의 제한시간을 초과했습니다.");
            return true;
        }

        pruneAndPressure(run, owner);

        return switch (run.phase) {
            case ACT1_MINING -> tickMining(run, owner);
            case ACT1_BUILD -> tickBuilding(run, owner);
            case ACT1_COMBAT -> tickCombat(run, owner, Phase.ACT1_MOVE);
            case ACT1_MOVE -> tickMovement(run, owner);
            case ACT2_SET_ONE -> tickCombat(run, owner, Phase.ACT2_SET_TWO);
            case ACT2_SET_TWO -> tickCombat(run, owner, Phase.ACT2_SET_THREE);
            case ACT2_SET_THREE -> tickCombat(run, owner, Phase.ACT3_SEAL_ONE);
            case ACT3_SEAL_ONE -> tickSeal(run, owner, Phase.ACT3_SEAL_TWO);
            case ACT3_SEAL_TWO -> tickSeal(run, owner, Phase.ACT3_SEAL_THREE);
            case ACT3_SEAL_THREE -> tickFinalSeal(run, owner);
        };
    }

    private static boolean tickMining(Run run, ServerPlayer owner) {
        run.mineTargets.removeIf(pos -> !run.level.hasChunkAt(pos)
                || !run.level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN));
        int done = MINING_TARGETS - run.mineTargets.size();
        setObjectiveBar(run, "1막 · 채굴로 균열 노출", done, MINING_TARGETS);
        if (!run.mineTargets.isEmpty()) return false;

        owner.sendSystemMessage(Component.literal("§5[세계의 시험] §f불안정 광핵 제거 완료. 실제 블록으로 전장 지지대를 복구하세요."));
        if (!enterPhase(run, owner, Phase.ACT1_BUILD)) {
            fail(run, owner, "건설 목표를 배치할 공간이 부족합니다.");
            return true;
        }
        return false;
    }

    private static boolean tickBuilding(Run run, ServerPlayer owner) {
        run.buildTargets.removeIf(pos -> {
            if (!run.level.hasChunkAt(pos)) return false;
            BlockState state = run.level.getBlockState(pos);
            return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity();
        });
        int done = BUILD_TARGETS - run.buildTargets.size();
        setObjectiveBar(run, "1막 · 전장 지지대 복구", done, BUILD_TARGETS);
        if (!run.buildTargets.isEmpty()) return false;

        owner.sendSystemMessage(Component.literal("§5[세계의 시험] §f전장 지지대 복구 완료. 접근하는 수호자를 격파하세요."));
        if (!enterPhase(run, owner, Phase.ACT1_COMBAT)) {
            fail(run, owner, "수호자를 배치할 공간이 부족합니다.");
            return true;
        }
        return false;
    }

    private static boolean tickMovement(Run run, ServerPlayer owner) {
        List<BlockPos> reached = new ArrayList<>();
        for (BlockPos pos : run.movementTargets) {
            if (distanceToCenterSqr(owner, pos) <= 2.75D * 2.75D) reached.add(pos);
        }
        for (BlockPos pos : reached) {
            run.movementTargets.remove(pos);
            clearMarker(run, pos);
        }

        int done = MOVEMENT_TARGETS - run.movementTargets.size();
        setObjectiveBar(run, "1막 · 전장 기동선 복구", done, MOVEMENT_TARGETS);
        if (!run.movementTargets.isEmpty()) return false;

        owner.sendSystemMessage(Component.literal("§d[1막 돌파] §f세계의 시험 완료. 이제 아홉 지역의 압력이 세 묶음으로 겹쳐집니다."));
        if (!enterPhase(run, owner, Phase.ACT2_SET_ONE)) {
            fail(run, owner, "첫 지역 잔향을 배치할 공간이 부족합니다.");
            return true;
        }
        return false;
    }

    private static boolean tickCombat(Run run, ServerPlayer owner, Phase next) {
        int done = run.phaseTargetCount - run.mobIds.size();
        setObjectiveBar(run, run.phase.title, done, Math.max(1, run.phaseTargetCount));
        if (!run.mobIds.isEmpty()) return false;

        if (run.phase == Phase.ACT1_COMBAT) {
            if (!enterPhase(run, owner, next)) {
                fail(run, owner, "기동 표식을 배치할 공간이 부족합니다.");
                return true;
            }
            return false;
        }

        owner.sendSystemMessage(Component.literal("§5[9지역의 잔향] §f" + run.phase.completionMessage));
        if (!enterPhase(run, owner, next)) {
            fail(run, owner, "다음 지역 잔향을 배치할 공간이 부족합니다.");
            return true;
        }
        return false;
    }

    private static boolean tickSeal(Run run, ServerPlayer owner, Phase next) {
        SealTick result = updateSeal(run, owner);
        if (result == SealTick.FAILED) {
            fail(run, owner, "붕괴 고정점이 훼손되었습니다.");
            return true;
        }
        if (result != SealTick.COMPLETE) return false;

        owner.sendSystemMessage(Component.literal("§4[붕괴 봉쇄] §f고정점 안정화 완료. 다음 붕괴점이 열립니다."));
        if (!enterPhase(run, owner, next)) {
            fail(run, owner, "다음 붕괴 고정점을 배치할 공간이 부족합니다.");
            return true;
        }
        return false;
    }

    private static boolean tickFinalSeal(Run run, ServerPlayer owner) {
        SealTick result = updateSeal(run, owner);
        if (result == SealTick.FAILED) {
            fail(run, owner, "붕괴 고정점이 훼손되었습니다.");
            return true;
        }
        if (result != SealTick.COMPLETE) return false;

        cleanup(run);
        closeBossBar(run);
        owner.sendSystemMessage(Component.literal("§d[최후의 승천] §f세계의 시험, 아홉 지역의 잔향, 붕괴 봉쇄를 모두 돌파했습니다."));
        if (!FinalAscensionBossSystem.tryStartFromClosure(owner, run.center)) {
            owner.sendSystemMessage(Component.literal("§c[최후의 승천] §f최종 관문을 형성하지 못했습니다. 열린 전장에서 다시 도전하세요."));
            return true;
        }
        owner.sendSystemMessage(Component.literal("§7승천 중추가 최심부의 문을 열었습니다. 세계의 경계가 모습을 드러냅니다."));
        return true;
    }

    private static SealTick updateSeal(Run run, ServerPlayer owner) {
        if (run.anchor == null || !run.level.hasChunkAt(run.anchor)
                || !run.level.getBlockState(run.anchor).is(Blocks.CRYING_OBSIDIAN)) {
            return SealTick.FAILED;
        }

        if (!run.mobIds.isEmpty()) {
            run.sealTicks = 0;
            run.bossBar.setName(Component.literal("§4" + run.phase.title + " §7· 수호체 " + run.mobIds.size() + "체"));
            run.bossBar.setProgress(0.0F);
            return SealTick.ACTIVE;
        }

        boolean precise = owner.isShiftKeyDown() && distanceToCenterSqr(owner, run.anchor) <= 3.25D * 3.25D;
        if (precise) run.sealTicks = Math.min(SEAL_CHANNEL_TICKS, run.sealTicks + TICK_INTERVAL);
        else run.sealTicks = Math.max(0, run.sealTicks - TICK_INTERVAL);

        float progress = run.sealTicks / (float) SEAL_CHANNEL_TICKS;
        int percent = Mth.clamp(Math.round(progress * 100.0F), 0, 100);
        run.bossBar.setName(Component.literal("§4" + run.phase.title + " §7· 웅크려 정밀 봉쇄 " + percent + "%"));
        run.bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));

        if (run.sealTicks < SEAL_CHANNEL_TICKS) return SealTick.ACTIVE;
        clearMarker(run, run.anchor);
        run.anchor = null;
        return SealTick.COMPLETE;
    }

    private static boolean enterPhase(Run run, ServerPlayer owner, Phase phase) {
        clearTransient(run);
        run.phase = phase;
        run.phaseTargetCount = 0;
        run.sealTicks = 0;
        run.anchor = null;
        long timeout = phase.isEcho() ? ECHO_TIMEOUT_TICKS : PHASE_TIMEOUT_TICKS;
        run.phaseDeadline = run.level.getGameTime() + timeout;

        return switch (phase) {
            case ACT1_MINING -> prepareMining(run, owner);
            case ACT1_BUILD -> prepareBuilding(run, owner);
            case ACT1_COMBAT -> prepareThreats(run, owner, ACT_ONE_GUARDS, 1.70D, 1.30D,
                    "§5[세계의 시험] §f실제 전투 숙련과 장비로 수호자 3체를 격파하세요.");
            case ACT1_MOVE -> prepareMovement(run, owner);
            case ACT2_SET_ONE -> prepareThreats(run, owner, ECHO_SET_ONE, 1.85D, 1.35D,
                    "§5[9지역의 잔향] §f삼림 · 건조 · 습지의 압력이 한 세트로 겹칩니다.");
            case ACT2_SET_TWO -> prepareThreats(run, owner, ECHO_SET_TWO, 2.00D, 1.40D,
                    "§5[9지역의 잔향] §f고산 · 대양 · 심층의 압력이 한 세트로 겹칩니다.");
            case ACT2_SET_THREE -> prepareThreats(run, owner, ECHO_SET_THREE, 2.15D, 1.45D,
                    "§5[9지역의 잔향] §f빙설 · 네더 · 엔드의 압력이 마지막 세트로 겹칩니다.");
            case ACT3_SEAL_ONE -> prepareSeal(run, owner, 0, "추격 붕괴점");
            case ACT3_SEAL_TWO -> prepareSeal(run, owner, 1, "이변 붕괴점");
            case ACT3_SEAL_THREE -> prepareSeal(run, owner, 2, "매복 붕괴점");
        };
    }

    private static boolean prepareMining(Run run, ServerPlayer owner) {
        for (BlockPos raw : cardinalAnchors(run.center, 10)) {
            BlockPos base = findOpenSpawn(run.level, raw);
            if (base == null) continue;
            boolean spanX = Math.abs(base.getZ() - run.center.getZ()) >= Math.abs(base.getX() - run.center.getX());
            List<BlockPos> wall = new ArrayList<>();
            for (int horizontal = -1; horizontal <= 1; horizontal++) {
                for (int y = 0; y <= 1; y++) {
                    wall.add(spanX ? base.offset(horizontal, y, 0) : base.offset(0, y, horizontal));
                }
            }
            if (!allOpen(run.level, wall)) continue;
            for (BlockPos pos : wall) {
                if (!placeMarker(run, pos, Blocks.CRYING_OBSIDIAN)) return false;
                run.mineTargets.add(pos.immutable());
            }
            owner.sendSystemMessage(Component.literal("§5[세계의 시험] §f보라빛 불안정 광핵 6개를 실제로 채굴해 균열을 노출하세요."));
            owner.sendSystemMessage(Component.literal("§7광역/광맥 채굴 같은 기존 숙련 효과가 그대로 작동하며, 웅크리면 정밀 채굴입니다."));
            setObjectiveBar(run, run.phase.title, 0, MINING_TARGETS);
            return true;
        }
        return false;
    }

    private static boolean prepareBuilding(Run run, ServerPlayer owner) {
        for (BlockPos raw : cardinalAnchors(run.center, 9)) {
            BlockPos base = findOpenSpawn(run.level, raw);
            if (base == null) continue;
            boolean lineX = Math.abs(base.getZ() - run.center.getZ()) >= Math.abs(base.getX() - run.center.getX());
            List<BlockPos> bases = new ArrayList<>();
            for (int i = -1; i <= 2; i++) {
                BlockPos marker = lineX ? base.offset(i, 0, 0) : base.offset(0, 0, i);
                bases.add(marker);
            }
            List<BlockPos> occupied = new ArrayList<>(bases);
            for (BlockPos marker : bases) occupied.add(marker.above());
            if (!allOpen(run.level, occupied)) continue;

            for (BlockPos marker : bases) {
                if (!placeMarker(run, marker, Blocks.AMETHYST_BLOCK)) return false;
                run.buildTargets.add(marker.above().immutable());
            }
            owner.sendSystemMessage(Component.literal("§5[세계의 시험] §f자수정 표식 4개 바로 위 빈칸을 실제 블록으로 연결해 전장 지지대를 복구하세요."));
            owner.sendSystemMessage(Component.literal("§7선/면 건축 등 기존 건축 숙련이 그대로 적용됩니다. 플레이어가 놓은 복구 블록은 전장에 남습니다."));
            setObjectiveBar(run, run.phase.title, 0, BUILD_TARGETS);
            return true;
        }
        return false;
    }

    private static boolean prepareMovement(Run run, ServerPlayer owner) {
        int[][] offsets = {{11, 0}, {-6, 10}, {-6, -10}};
        for (int[] offset : offsets) {
            BlockPos pos = findOpenSpawn(run.level, run.center.offset(offset[0], 0, offset[1]));
            if (pos == null || !placeMarker(run, pos, Blocks.GLOWSTONE)) {
                clearTransient(run);
                return false;
            }
            run.movementTargets.add(pos.immutable());
        }
        owner.sendSystemMessage(Component.literal("§5[세계의 시험] §f세 개의 빛 표식 가까이를 모두 통과해 전장 기동선을 다시 연결하세요."));
        owner.sendSystemMessage(Component.literal("§7질주·공중 기동 등 실제 이동 능력을 그대로 사용할 수 있습니다."));
        setObjectiveBar(run, run.phase.title, 0, MOVEMENT_TARGETS);
        return true;
    }

    private static boolean prepareThreats(Run run, ServerPlayer owner, ThreatSpec[] threats,
                                          double healthScale, double attackScale, String message) {
        int index = 0;
        for (ThreatSpec spec : threats) {
            Mob mob = spawnOne(run.level, run.center, spec, index++, threats.length);
            if (mob == null) {
                clearTransient(run);
                return false;
            }
            strengthen(mob, healthScale, attackScale);
            markMob(mob, run);
            mob.setCustomName(Component.literal("§5" + spec.koreanName));
            mob.setCustomNameVisible(true);
            mob.setGlowingTag(true);
            mob.setTarget(owner);
            run.mobIds.add(mob.getUUID());
        }
        run.phaseTargetCount = run.mobIds.size();
        owner.sendSystemMessage(Component.literal(message));
        setObjectiveBar(run, run.phase.title, 0, Math.max(1, run.phaseTargetCount));
        return run.phaseTargetCount == threats.length;
    }

    private static boolean prepareSeal(Run run, ServerPlayer owner, int index, String label) {
        int[][] offsets = {{10, 0}, {-5, 9}, {-5, -9}};
        int[] offset = offsets[index];
        BlockPos anchor = findOpenSpawn(run.level, run.center.offset(offset[0], 0, offset[1]));
        if (anchor == null || !placeMarker(run, anchor, Blocks.CRYING_OBSIDIAN)) return false;
        run.anchor = anchor.immutable();

        ThreatSpec guard = COLLAPSE_GUARDS[index];
        Mob mob = spawnOne(run.level, run.center, guard, index + 3, 6);
        if (mob == null) {
            clearTransient(run);
            return false;
        }
        strengthen(mob, 2.35D + index * 0.15D, 1.50D + index * 0.05D);
        markMob(mob, run);
        mob.setCustomName(Component.literal("§4" + guard.koreanName));
        mob.setCustomNameVisible(true);
        mob.setGlowingTag(true);
        mob.setTarget(owner);
        run.mobIds.add(mob.getUUID());
        run.phaseTargetCount = 1;

        owner.sendSystemMessage(Component.literal("§4[붕괴] §f" + label + "의 수호체를 먼저 격파한 뒤, 보라빛 고정점 3블록 안에서 웅크려 정밀 봉쇄하세요."));
        run.bossBar.setName(Component.literal("§4" + run.phase.title + " §7· 수호체 1체"));
        run.bossBar.setProgress(0.0F);
        return true;
    }

    private static void pruneAndPressure(Run run, ServerPlayer owner) {
        if (run.mobIds.isEmpty()) return;
        Set<UUID> alive = new HashSet<>();
        for (UUID id : run.mobIds) {
            Entity entity = run.level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            alive.add(id);
            if (mob.getTarget() == null) mob.setTarget(owner);
            if (distanceToCenterSqr(mob, run.center) > MOB_RECALL_RADIUS * MOB_RECALL_RADIUS) {
                mob.getNavigation().moveTo(run.center.getX() + 0.5D, run.center.getY(),
                        run.center.getZ() + 0.5D, 1.30D);
            } else if (mob.distanceToSqr(owner) > 20.0D * 20.0D) {
                mob.getNavigation().moveTo(owner, 1.22D);
            }
        }
        run.mobIds.clear();
        run.mobIds.addAll(alive);
    }

    private static Mob spawnOne(ServerLevel level, BlockPos center, ThreatSpec spec, int index, int count) {
        Identifier identifier = Identifier.parse(spec.entityId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) return null;

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.41D) / Math.max(1, count);
            int radius = 8 + level.getRandom().nextInt(6);
            BlockPos raw = center.offset((int) Math.round(Math.cos(angle) * radius), 0,
                    (int) Math.round(Math.sin(angle) * radius));
            BlockPos pos = findOpenSpawn(level, raw);
            if (pos == null) continue;
            Entity entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }

    private static void strengthen(Mob mob, double healthScale, double attackScale) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(health.getBaseValue(), health.getBaseValue() * healthScale));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(Math.max(attack.getBaseValue(), attack.getBaseValue() * attackScale));
        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(Math.max(armor.getBaseValue(), armor.getBaseValue() + 4.0D));
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(Math.min(0.48D, speed.getBaseValue() * 1.12D));
    }

    private static void markMob(Mob mob, Run run) {
        mob.setPersistenceRequired();
        mob.getPersistentData().putString(OWNER_KEY, run.owner.toString());
        mob.getPersistentData().putString(PHASE_KEY, run.phase.name());
    }

    private static boolean placeMarker(Run run, BlockPos pos, Block block) {
        BlockPos immutable = pos.immutable();
        if (!run.level.hasChunkAt(immutable)) return false;
        if (!run.level.getBlockState(immutable).isAir() || !run.level.getFluidState(immutable).isEmpty()) return false;
        if (!run.level.setBlockAndUpdate(immutable, block.defaultBlockState())) return false;
        run.markers.put(immutable, block);
        return true;
    }

    private static boolean allOpen(ServerLevel level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) return false;
            if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) return false;
        }
        return true;
    }

    private static List<BlockPos> cardinalAnchors(BlockPos center, int radius) {
        return List.of(
                center.offset(radius, 0, 0),
                center.offset(-radius, 0, 0),
                center.offset(0, 0, radius),
                center.offset(0, 0, -radius)
        );
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 4; dy >= -5; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static int countOpenSpawnSlots(ServerLevel level, BlockPos center) {
        int open = 0;
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2.0D * i / 20.0D;
            BlockPos raw = center.offset((int) Math.round(Math.cos(angle) * 11.0D), 0,
                    (int) Math.round(Math.sin(angle) * 11.0D));
            if (findOpenSpawn(level, raw) != null) open++;
        }
        return open;
    }

    private static void setObjectiveBar(Run run, String title, int done, int total) {
        int safeTotal = Math.max(1, total);
        int clampedDone = Mth.clamp(done, 0, safeTotal);
        run.bossBar.setName(Component.literal("§5" + title + " §7· " + clampedDone + "/" + safeTotal));
        run.bossBar.setProgress(Mth.clamp(clampedDone / (float) safeTotal, 0.0F, 1.0F));
    }

    private static boolean hasConflictingActivity(ServerPlayer player) {
        return AscensionTrialSystem.isActive(player)
                || ApexHuntSystem.isActive(player)
                || ExpeditionIncidentSystem.isActive(player)
                || ExpeditionOperationSystem.isActive(player)
                || OutpostSiegeSystem.isActive(player)
                || FinalAscensionBossSystem.isActive(player);
    }

    private static void clearTransient(Run run) {
        for (UUID id : run.mobIds) {
            Entity entity = run.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        run.mobIds.clear();
        for (Map.Entry<BlockPos, Block> entry : new ArrayList<>(run.markers.entrySet())) {
            BlockPos pos = entry.getKey();
            if (run.level.hasChunkAt(pos) && run.level.getBlockState(pos).is(entry.getValue())) {
                run.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
        run.markers.clear();
        run.mineTargets.clear();
        run.buildTargets.clear();
        run.movementTargets.clear();
        run.anchor = null;
    }

    private static void clearMarker(Run run, BlockPos pos) {
        Block expected = run.markers.remove(pos);
        if (expected == null || !run.level.hasChunkAt(pos)) return;
        if (run.level.getBlockState(pos).is(expected)) {
            run.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void cleanup(Run run) {
        clearTransient(run);
    }

    private static void fail(Run run, ServerPlayer owner, String reason) {
        cleanup(run);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("§c[최후의 승천 실패] §f" + reason + " §7시스템이 만든 전장 표식과 잔존 적만 정리했습니다."));
        }
        closeBossBar(run);
    }

    private static void removeStaleServerRuns(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Run> entry : ACTIVE.entrySet()) {
            Run run = entry.getValue();
            if (run.level.getServer() != server) {
                cleanup(run);
                closeBossBar(run);
                stale.add(entry.getKey());
            }
        }
        for (UUID owner : stale) ACTIVE.remove(owner);
    }

    private static void syncBossBarPlayers(MinecraftServer server, Run run) {
        Set<ServerPlayer> shouldSee = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == run.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, run.center) <= PLAYER_RADIUS * PLAYER_RADIUS) {
                shouldSee.add(player);
                if (!run.bossBar.getPlayers().contains(player)) run.bossBar.addPlayer(player);
            }
        }
        for (ServerPlayer viewer : List.copyOf(run.bossBar.getPlayers())) {
            if (!shouldSee.contains(viewer)) run.bossBar.removePlayer(viewer);
        }
    }

    private static void closeBossBar(Run run) {
        run.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(run.bossBar.getPlayers())) run.bossBar.removePlayer(viewer);
    }

    private static double distanceToCenterSqr(Entity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dy = entity.getY() - (center.getY() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private enum SealTick {
        ACTIVE,
        COMPLETE,
        FAILED
    }

    private enum Phase {
        ACT1_MINING("1막 · 채굴로 균열 노출", ""),
        ACT1_BUILD("1막 · 전장 지지대 복구", ""),
        ACT1_COMBAT("1막 · 수호자 격파", ""),
        ACT1_MOVE("1막 · 전장 기동선 복구", ""),
        ACT2_SET_ONE("2막 · 삼림/건조/습지", "삼림 · 건조 · 습지의 잔향을 돌파했습니다."),
        ACT2_SET_TWO("2막 · 고산/대양/심층", "고산 · 대양 · 심층의 잔향을 돌파했습니다."),
        ACT2_SET_THREE("2막 · 빙설/네더/엔드", "빙설 · 네더 · 엔드의 잔향을 돌파했습니다. 붕괴가 시작됩니다."),
        ACT3_SEAL_ONE("3막 · 추격 붕괴점", ""),
        ACT3_SEAL_TWO("3막 · 이변 붕괴점", ""),
        ACT3_SEAL_THREE("3막 · 매복 붕괴점", "");

        final String title;
        final String completionMessage;

        Phase(String title, String completionMessage) {
            this.title = title;
            this.completionMessage = completionMessage;
        }

        boolean isEcho() {
            return this == ACT2_SET_ONE || this == ACT2_SET_TWO || this == ACT2_SET_THREE;
        }
    }

    private record ThreatSpec(String entityId, String koreanName) {}

    private static final class Run {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new LinkedHashSet<>();
        final Map<BlockPos, Block> markers = new HashMap<>();
        final Set<BlockPos> mineTargets = new LinkedHashSet<>();
        final Set<BlockPos> buildTargets = new LinkedHashSet<>();
        final Set<BlockPos> movementTargets = new LinkedHashSet<>();
        Phase phase;
        BlockPos anchor;
        long phaseDeadline;
        int ownerAbsentTicks;
        int phaseTargetCount;
        int sealTicks;

        Run(UUID owner, ServerLevel level, BlockPos center) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.phase = Phase.ACT1_MINING;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("§5최후의 승천"),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(0.0F);
        }
    }
}
