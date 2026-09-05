from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SURV = ROOT / "projects/survival-ascension"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if text.count(old) != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {text.count(old)}")
    return text.replace(old, new, 1)


def replace_method(text: str, start_sig: str, next_sig: str, replacement: str, label: str) -> str:
    start = text.find(start_sig)
    if start < 0:
        raise SystemExit(f"{label}: start signature not found")
    end = text.find(next_sig, start)
    if end < 0:
        raise SystemExit(f"{label}: next signature not found")
    return text[:start] + replacement.rstrip() + "\n\n" + text[end:]

# ---------------------------------------------------------------------------
# Version / changelog
# ---------------------------------------------------------------------------
p = SURV / "gradle.properties"
s = p.read_text(encoding="utf-8")
s = replace_once(s, "mod_version=0.61.8-alpha.1", "mod_version=0.61.9-alpha.1", "gradle version")
p.write_text(s, encoding="utf-8")

p = SURV / "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
s = p.read_text(encoding="utf-8")
s = replace_once(s, 'public static final String VERSION = "0.61.8-alpha.1";', 'public static final String VERSION = "0.61.9-alpha.1";', "java version")
p.write_text(s, encoding="utf-8")

p = SURV / "CHANGELOG.md"
s = p.read_text(encoding="utf-8")
section = """## 0.61.9-alpha.1
- Consolidated expedition active-operation validation into one authoritative lifecycle path used by tick, login, status and progress recording. Stored dimension, game mode and operation/extraction deadlines are now rejected before a stale operation is presented as resumed.
- Added one client-session reset boundary for skill snapshots/recent XP notices, expedition snapshots, mobility cooldown HUD state and Mythic target state when the client leaves a world, preventing static HUD data from leaking into another connection.
- Hardened Mythic runtime ownership across entity dimension transfer: a Mythic UUID that joins a different ServerLevel now replaces the old runtime/bossbar while preserving contributor credit instead of retaining a stale level pointer.
- Corrected the full-pack Weapons Expanded Korean descriptions for Polluting, Leech and Cleaving; Polluting now states its actual Poison I durations (8s/15s), and the previous machine-translated Cleaving sentence was removed.
- No new parallel SavedData, alternate encounter registry or duplicate safety-state layer was added; the patch reduces lifecycle duplication instead.

"""
s = replace_once(s, "# Changelog\n\n", "# Changelog\n\n" + section, "changelog header")
p.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# Client session state: every static cache gets one explicit reset contract.
# ---------------------------------------------------------------------------
p = SURV / "src/main/java/kr/moonseungjun/survivalascension/client/ClientSkillState.java"
s = p.read_text(encoding="utf-8")
anchor = """    public static long xp(SkillType skill) {
        return XP.getOrDefault(skill.id(), 0L);
    }
"""
replacement = """    public static void reset() {
        XP.clear();
        RECENT.clear();
        lastUpdate = null;
        lastUpdateMillis = 0L;
    }

""" + anchor
s = replace_once(s, anchor, replacement, "ClientSkillState.reset")
p.write_text(s, encoding="utf-8")

p = SURV / "src/main/java/kr/moonseungjun/survivalascension/client/ClientMobilityState.java"
s = p.read_text(encoding="utf-8")
anchor = """    public static int remainingTicks() { return Math.max(0, remainingCooldownTicks); }
"""
replacement = """    public static void reset() {
        totalCooldownTicks = 0;
        remainingCooldownTicks = 0;
    }

""" + anchor
s = replace_once(s, anchor, replacement, "ClientMobilityState.reset")
p.write_text(s, encoding="utf-8")

p = SURV / "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java"
s = p.read_text(encoding="utf-8")
s = replace_once(
    s,
    "        if (minecraft.player == null || minecraft.level == null) ClientMythicState.clear();",
    """        if (minecraft.player == null || minecraft.level == null) {
            // One session boundary owns all static client caches. Do not let a previous server/world
            // leak HUD/progression state into the next connection while waiting for fresh snapshots.
            ClientSkillState.reset();
            ClientExpeditionState.reset();
            ClientMobilityState.reset();
            ClientMythicState.clear();
        }""",
    "client session reset",
)
p.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# Mythic runtime: one UUID must have exactly one runtime bound to its current level.
# ---------------------------------------------------------------------------
p = SURV / "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java"
s = p.read_text(encoding="utf-8")
old = """    private static MythicRuntime ensureMythicRuntime(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) throw new IllegalStateException("mythic mob must be server-side");
        return MYTHICS.computeIfAbsent(mob.getUUID(), id -> new MythicRuntime(level, mob));
    }
"""
new = """    private static MythicRuntime ensureMythicRuntime(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) throw new IllegalStateException("mythic mob must be server-side");
        UUID id = mob.getUUID();
        MythicRuntime current = MYTHICS.get(id);
        if (current != null && current.level == level) return current;

        // Entity UUIDs survive portal/dimension transfer. A computeIfAbsent here would keep the
        // old ServerLevel forever, so replace that runtime atomically and carry only real credit.
        MythicRuntime replacement = new MythicRuntime(level, mob);
        if (current != null) {
            replacement.contributors.addAll(current.contributors);
            closeMythicBar(current);
        }
        MYTHICS.put(id, replacement);
        return replacement;
    }
"""
s = replace_once(s, old, new, "mythic runtime authority")
p.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# Expedition operation: collapse duplicated active-state checks into one path.
# ---------------------------------------------------------------------------
p = SURV / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java"
s = p.read_text(encoding="utf-8")

on_tick = """    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ActiveContext context = validateActive(player);
        if (context == null) return;

        ExpeditionOperationData data = context.data();
        ExpeditionOperationData.ActiveOperation active = context.active();
        ExpeditionOperation operation = context.operation();
        ServerLevel level = context.level();
        double distanceSq = active.anchor().distSqr(player.blockPosition());
        if (!active.rangeReached() && distanceSq >= operation.rangeTarget() * operation.rangeTarget()) {
            if (data.markRangeReached(player)) {
                player.sendSystemMessage(Component.literal("§6[작전 전진선 돌파] §f" + operation.koreanName()
                        + " §7· 이제 전초 " + WORK_RADIUS + "블록 밖의 " + active.region().koreanName()
                        + "에서 현장 목표가 기록됩니다."));
                active = data.active(player);
            }
        }
        if (active != null && active.complication() == ExpeditionComplication.FORWARD_SHIFT
                && active.complicationState() > 0
                && distanceSq >= (double) active.complicationState() * active.complicationState()
                && ExpeditionProgression.currentRegion(player) == active.region()) {
            if (data.completeForwardShift(player)) {
                player.sendSystemMessage(Component.literal("§a[전선 재전개 완료] §f추가 전진선을 확보했습니다. 남은 현장 목표 기록이 재개됩니다."));
                active = data.active(player);
            }
        }
        if (active != null && data.objectivesComplete(player, operation)
                && distanceSq <= RETURN_RADIUS * RETURN_RADIUS
                && OutpostService.isRecoveryOperational(player, level, active.dimension(), active.anchor())) {
            complete(player, operation);
        }
    }
"""
s = replace_method(s, "    public static void onPlayerTick(PlayerTickEvent.Post event) {", "    public static void onLivingDeath", on_tick, "expedition tick")

on_login = """    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (ExpeditionOperationData.get(player).active(player) == null) return;
        ActiveContext context = validateActive(player);
        if (context == null) return;
        player.sendSystemMessage(Component.literal("§6[원정 작전 재개] §f로그아웃 전 진행 중이던 작전이 유지되어 있습니다."));
        sendStatus(player, context);
    }
"""
s = replace_method(s, "    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)", "    public static void recordAction", on_login, "expedition login")

record_action = """    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0) return;
        ActiveContext context = validateActive(player);
        if (context == null || !context.active().rangeReached()) return;

        ExpeditionOperationData data = context.data();
        ExpeditionOperationData.ActiveOperation active = context.active();
        ServerLevel level = context.level();
        double distanceSq = active.anchor().distSqr(player.blockPosition());
        if (distanceSq < WORK_RADIUS * WORK_RADIUS || ExpeditionProgression.currentRegion(player) != active.region()) return;
        ExpeditionOperation operation = context.operation();
        if (active.complication() == ExpeditionComplication.DEEP_FRONT
                && distanceSq < operation.rangeTarget() * operation.rangeTarget()) return;
        if (active.complication() == ExpeditionComplication.FORWARD_SHIFT && active.complicationState() > 0) return;
        for (int i = 0; i < operation.tasks().size(); i++) {
            ExpeditionOperation.Task task = operation.tasks().get(i);
            if (task.action() != action) continue;
            ExpeditionOperationData.ProgressResult result = data.addProgress(player, i, amount, task.target());
            if (result.newProgress() == result.oldProgress()) continue;
            boolean objectivesComplete = data.objectivesComplete(player, operation);
            if (result.taskCompletedNow()) {
                player.sendSystemMessage(Component.literal("§6[작전 목표 완료] §f" + task.action().koreanName()
                        + " §a" + result.newProgress() + "/" + task.target()));
                ExpeditionOperationData.ActiveOperation refreshed = data.active(player);
                if (!objectivesComplete && refreshed != null
                        && refreshed.complication() == ExpeditionComplication.FORWARD_SHIFT
                        && refreshed.complicationState() == 0) {
                    int targetRadius = operation.rangeTarget() + FORWARD_SHIFT_EXTRA;
                    if (data.beginForwardShift(player, targetRadius)) {
                        player.sendSystemMessage(Component.literal("§c[전선 재전개] §f첫 현장 목표가 끝났습니다. 남은 목표는 원점 기준 §e"
                                + targetRadius + "블록§f까지 추가 전진한 뒤 다시 기록됩니다."));
                    }
                }
            } else {
                int oldQuarter = result.oldProgress() * 4 / task.target();
                int newQuarter = result.newProgress() * 4 / task.target();
                if (newQuarter > oldQuarter) {
                    player.sendSystemMessage(Component.literal("§6[작전 진행] §f" + task.action().koreanName()
                            + " §e" + result.newProgress() + "/" + task.target()), true);
                }
            }
            if (objectivesComplete) {
                ExpeditionOperationData.ActiveOperation refreshed = data.active(player);
                if (refreshed != null
                        && refreshed.complication() == ExpeditionComplication.HOT_EXTRACTION
                        && refreshed.complicationState() == 0) {
                    int window = refreshed.complication().extractionWindowTicks(operation);
                    if (data.armExtraction(player, level.getGameTime() + window)) {
                        player.sendSystemMessage(Component.literal("§c[긴급 철수] §f현장 목표 완료. §e"
                                + window / 1200 + "분 " + (window / 20) % 60
                                + "초§f 안에 같은 전초8블록으로 귀환해야 합니다."));
                    }
                }
                player.sendSystemMessage(Component.literal("§a[작전 현장 목표 완료] §f같은 전초기지 반경 "
                        + RETURN_RADIUS + "블록으로 복귀하면 작전이 완료됩니다."));
            }
            return;
        }
    }
"""
s = replace_method(s, "    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {", "    public static void sendStatus", record_action, "expedition action")

send_status = """    public static void sendStatus(ServerPlayer player) {
        ExpeditionOperationData data = ExpeditionOperationData.get(player);
        player.sendSystemMessage(Component.literal("§6[원정 작전] §f지역 최초 완수 §e" + data.uniqueCompleted(player)
                + "/9 §7· 총 귀환 성공 §f" + data.totalCompletions(player)
                + (data.masteryClaimed(player) ? " §6· 9종 완주 보상 수령" : "")));
        if (data.active(player) == null) {
            player.sendSystemMessage(Component.literal("§7활성 전초기지에서 보급권1로 시작 · 완수한 해당 원정권 필요 · 출발/현지작업/귀환 + 작전 변수1개"));
            return;
        }
        ActiveContext context = validateActive(player);
        if (context == null) {
            player.sendSystemMessage(Component.literal("§7현재 유효한 원정 작전 없음"));
            return;
        }
        sendStatus(player, context);
    }

    private static void sendStatus(ServerPlayer player, ActiveContext context) {
        ExpeditionOperationData.ActiveOperation active = context.active();
        ExpeditionOperation operation = context.operation();
        long now = context.now();
        long seconds = Math.max(0L, (active.deadline() - now + 19L) / 20L);
        player.sendSystemMessage(Component.literal("§f" + operation.koreanName() + " §7· 전진선 "
                + (active.rangeReached() ? "§a돌파" : "§e" + operation.rangeTarget() + "블록 필요")
                + " §7· 남은 " + seconds / 60 + "분 " + seconds % 60 + "초"));
        player.sendSystemMessage(Component.literal("§c작전 변수 §f" + active.complication().koreanName()
                + " §7· " + active.complication().description()));
        if (active.region() == ExpeditionRegion.DEEP && ContentPackCompatibility.hasResonanceOperationRewards()) {
            player.sendSystemMessage(Component.literal("  §d공명 회수 계약 §7· 귀환 시 현재 손 장비 종류 우선 · 현재 §d"
                    + TargetedResonanceRecovery.describeFocus(player.getMainHandItem(), player.getOffhandItem())));
        }
        if (active.complication() == ExpeditionComplication.FORWARD_SHIFT && active.complicationState() > 0) {
            player.sendSystemMessage(Component.literal("  §c재전개 대기 §7· 원점에서 §e" + active.complicationState()
                    + "블록§7까지 추가 전진 필요"));
        } else if (active.complication() == ExpeditionComplication.FORWARD_SHIFT && active.complicationState() < 0) {
            player.sendSystemMessage(Component.literal("  §a전선 재전개 완료"));
        } else if (active.complication() == ExpeditionComplication.HOT_EXTRACTION && active.complicationState() == 1) {
            long extractionSeconds = Math.max(0L, (active.extractionDeadline() - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("  §c긴급 철수 §e" + extractionSeconds / 60
                    + "분 " + extractionSeconds % 60 + "초 남음"));
        }
        player.sendSystemMessage(Component.literal("  §7- §f" + operation.tasks().get(0).action().koreanName()
                + " §e" + active.progressA() + "§7/§f" + operation.tasks().get(0).target()));
        player.sendSystemMessage(Component.literal("  §7- §f" + operation.tasks().get(1).action().koreanName()
                + " §e" + active.progressB() + "§7/§f" + operation.tasks().get(1).target()));
    }

    private record ActiveContext(
            ExpeditionOperationData data,
            ExpeditionOperationData.ActiveOperation active,
            ExpeditionOperation operation,
            ServerLevel level,
            long now) {}

    private static ActiveContext validateActive(ServerPlayer player) {
        ExpeditionOperationData data = ExpeditionOperationData.get(player);
        ExpeditionOperationData.ActiveOperation active = data.active(player);
        if (active == null) return null;
        if (player.isCreative() || player.isSpectator()) {
            fail(player, "게임 모드가 변경되어 작전이 종료되었습니다.");
            return null;
        }
        if (!(player.level() instanceof ServerLevel level)
                || !active.dimension().equals(level.dimension().toString())) {
            fail(player, "작전 중 다른 차원으로 이탈했습니다.");
            return null;
        }
        ExpeditionOperation operation = ExpeditionOperation.forRegion(active.region());
        long now = level.getGameTime();
        if (now >= active.deadline()) {
            fail(player, "작전 제한시간을 초과했습니다.");
            return null;
        }
        active = recoverComplicationState(player, data, active, operation, level);
        if (active == null) return null;
        if (active.complication() == ExpeditionComplication.HOT_EXTRACTION
                && active.complicationState() == 1
                && active.extractionDeadline() > 0L
                && now >= active.extractionDeadline()) {
            fail(player, "긴급 철수 제한시간을 초과했습니다.");
            return null;
        }
        return new ActiveContext(data, active, operation, level, now);
    }
"""
s = replace_method(s, "    public static void sendStatus(ServerPlayer player) {", "    private static ExpeditionOperationData.ActiveOperation recoverComplicationState", send_status, "expedition status/validator")

p.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# Full-pack Korean overlay: lock known custom-enchant semantics instead of MT drift.
# ---------------------------------------------------------------------------
p = ROOT / "projects/frontier-settlement/src/main/resources/assets/weaponsexpanded/lang/ko_kr.json"
data = json.loads(p.read_text(encoding="utf-8"))
data["enchantment.weaponsexpanded.polluting.desc"] = "직접 근접 공격 시 독 I을 부여합니다. 오염 I은 8초, 오염 II는 15초 지속됩니다."
data["enchantment.weaponsexpanded.leech.desc"] = "방패로 피해를 막으면 체력을 회복하지만 방패 내구도가 추가로 소모됩니다."
data["enchantment.weaponsexpanded.cleaving.desc"] = "방패를 무력화하는 시간을 인챈트 레벨당 1초 증가시킵니다."
p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Sanity checks before Gradle gets a chance to compile.
assert "0.61.9-alpha.1" in (SURV / "gradle.properties").read_text(encoding="utf-8")
assert "private static ActiveContext validateActive" in (SURV / "src/main/java/kr/moonseungjun/survivalascension/expedition/ExpeditionOperationSystem.java").read_text(encoding="utf-8")
assert "ClientSkillState.reset();" in (SURV / "src/main/java/kr/moonseungjun/survivalascension/client/SurvivalAscensionClient.java").read_text(encoding="utf-8")
print("Survival alpha619 lifecycle patch applied")
