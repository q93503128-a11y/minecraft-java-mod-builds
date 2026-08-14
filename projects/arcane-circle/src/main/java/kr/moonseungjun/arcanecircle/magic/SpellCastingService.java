package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.world.ArcaneQuestData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SpellCastingService {
    private static final long QUEUE_TIMEOUT_TICKS = 2400L;
    private static final long CHARGE_TIMEOUT_TICKS = 1600L;
    private static final long READY_HOLD_TIMEOUT_TICKS = 12000L;

    private static final class FusionQueueState {
        private final List<String> ingredients = new ArrayList<>();
        private long updatedAt;
        private long chargeStartedAt = -1L;
        private int requiredTicks;
        private String resultId = "";
    }

    private static final class ChargeState {
        private final int slot;
        private final String spellId;
        private final long startedAt;
        private final int requiredTicks;

        private ChargeState(int slot, String spellId, long startedAt, int requiredTicks) {
            this.slot = slot;
            this.spellId = spellId;
            this.startedAt = startedAt;
            this.requiredTicks = requiredTicks;
        }
    }

    private static final Map<UUID, FusionQueueState> FUSION_QUEUES = new HashMap<>();
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();

    private SpellCastingService() {}

    public static void beginSlotCharge(ServerPlayer player, int slot) {
        if (ArcaneFieldService.blocksCasting(player)) {
            fail(player, "반마법장 또는 시간 정지로 마법 회로를 전개할 수 없습니다.");
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, slot);
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }
        MagicPlayerData.CooldownStatus cooldown = data.cooldownStatus(player, cast.spell().id());
        if (cooldown.active()) {
            fail(player, String.format("%s 재사용까지 %.1f초", cast.spell().name(), cooldown.remainingTicks() / 20.0));
            return;
        }

        long now = serverClock(player);
        ChargeState existing = CHARGES.get(player.getUUID());
        if (existing != null) {
            CHARGES.remove(player.getUUID());
            WorldMagicService.stop(player);
        }

        clearFusion(player, false);
        int required = requiredCastTicks(player, cast.spell());
        ChargeState charge = new ChargeState(slot, cast.spell().id(), now, required);
        CHARGES.put(player.getUUID(), charge);
        WorldMagicService.charge(player, cast.spell(), false, List.of(), cast.range(),
                required <= 0 ? 1.0 : 0.0);
        String timing = required <= 0
                ? "완성 · 키를 놓으면 발동"
                : String.format("%.1f초 전개 · 완성 후 키를 놓으면 발동", required / 20.0);
        ArcaneNoticeService.push(player, Component.literal("§5[회로 전개] §f"
                + cast.spell().name() + " §7· " + timing));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.45F, 1.18F);
    }

    public static void releaseSlotCharge(ServerPlayer player, int slot) {
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge == null || charge.slot != slot) return;
        long elapsed = serverClock(player) - charge.startedAt;
        CHARGES.remove(player.getUUID());
        WorldMagicService.stop(player);
        if (elapsed > chargeTimeoutTicks(charge)) {
            ArcaneNoticeService.push(player, Component.literal("§7[시전 취소] 유지 한계를 넘어 마법진이 해제되었습니다."));
            return;
        }
        if (elapsed < charge.requiredTicks) {
            int percent = (int) Math.round(100.0 * elapsed / Math.max(1, charge.requiredTicks));
            ArcaneNoticeService.push(player, Component.literal("§7[시전 취소] 회로 전개 " + percent + "% · 완성 전에 키를 놓았습니다."));
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, slot);
        if (!cast.accepted() || !charge.spellId.equals(cast.spell().id())) {
            fail(player, cast.accepted() ? "충전 중 주문 슬롯이 변경되었습니다." : cast.message());
            return;
        }
        castPrepared(player, data, cast);
    }

    public static void castSlot(ServerPlayer player, int slot) {
        beginSlotCharge(player, slot);
        releaseSlotCharge(player, slot);
    }

    public static void tickCharge(ServerPlayer player) {
        tickFusion(player);
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge == null) return;
        long now = serverClock(player);
        long elapsed = now - charge.startedAt;
        if (!player.isAlive() || player.isSpectator() || elapsed > chargeTimeoutTicks(charge)) {
            CHARGES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        SpellDefinition spell = SpellCatalog.spell(charge.spellId).orElse(null);
        if (spell == null || !data(player).state(player).known().contains(spell.id())) {
            CHARGES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, charge.slot);
        if (!cast.accepted() || !charge.spellId.equals(cast.spell().id())) {
            CHARGES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }

        double progress = charge.requiredTicks <= 0 ? 1.0
                : Math.min(1.0, elapsed / (double) Math.max(1, charge.requiredTicks));
        WorldMagicService.charge(player, spell, false, List.of(), cast.range(), progress);
    }

    private static long chargeTimeoutTicks(ChargeState charge) {
        return Math.max(CHARGE_TIMEOUT_TICKS,
                (long) Math.max(0, charge.requiredTicks) + READY_HOLD_TIMEOUT_TICKS);
    }

    public static void cancelCharge(ServerPlayer player, boolean notify) {
        ChargeState removed = CHARGES.remove(player.getUUID());
        if (removed != null) WorldMagicService.stop(player);
        if (notify && removed != null) {
            ArcaneNoticeService.push(player, Component.literal("§7[시전 취소] 전개한 마법진을 해제했습니다."));
        }
    }

    public static String chargingSpell(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        return state == null ? "" : state.spellId;
    }

    public static int chargingSlot(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        return state == null ? -1 : state.slot;
    }

    public static int chargingTicks(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, serverClock(player) - state.startedAt));
    }

    public static int chargingRequiredTicks(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        return state == null ? 0 : state.requiredTicks;
    }

    public static int requiredCastTicks(ServerPlayer player, SpellDefinition spell) {
        MagicPlayerData.MageState state = data(player).state(player);
        int circle = Math.max(1, Math.min(9, spell.circle()));
        int[] sameCircleTicks = {0, 6, 10, 16, 26, 42, 68, 105, 155, 220};
        int circleGap = Math.max(0, state.circle() - circle);
        int masteryTier = SpellCatalog.masteryTier(state.mastery(spell.id()));
        double gapScale = Math.pow(0.78, circleGap);
        double masteryScale = Math.max(0.72, 1.0 - masteryTier * 0.028);
        kr.moonseungjun.arcanecircle.world.MagicTradition chosen =
                kr.moonseungjun.arcanecircle.world.ArcaneWorldData.get(((ServerLevel) player.level()).getServer())
                        .tradition(player);
        double staffScale = Math.max(0.25,
                kr.moonseungjun.arcanecircle.registry.ModItems.equipped(player).castTimeMultiplier());
        double raw = sameCircleTicks[circle] * gapScale * masteryScale * chosen.castTimeMultiplier() * staffScale;
        return raw < 1.0 ? 0 : Math.max(1, (int) Math.round(raw));
    }

    public static void queueFusionSlot(ServerPlayer player, int slot) {
        if (ArcaneFieldService.blocksCasting(player)) {
            fail(player, "반마법장 또는 시간 정지로 융합 회로를 전개할 수 없습니다.");
            return;
        }
        cancelCharge(player, false);
        MagicPlayerData.MageState state = data(player).state(player);
        String spellId = state.slot(slot);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell == null || spell.circle() > state.circle() || !state.known().contains(spellId)) {
            fail(player, "현재 사용할 수 없는 주문 슬롯입니다.");
            return;
        }
        String cooldownBlock = fusionCooldownBlock(player, List.of(spellId));
        if (!cooldownBlock.isBlank()) {
            fail(player, cooldownBlock);
            return;
        }

        long now = serverClock(player);
        FusionQueueState queue = FUSION_QUEUES.computeIfAbsent(player.getUUID(), ignored -> new FusionQueueState());
        if (now - queue.updatedAt > QUEUE_TIMEOUT_TICKS) queue.ingredients.clear();
        if (queue.ingredients.size() >= 3) {
            fail(player, "삼중 융합 회로가 이미 가득 찼습니다. 처음 누른 주문 키를 놓아 시전하세요.");
            return;
        }

        List<String> proposed = new ArrayList<>(queue.ingredients);
        proposed.add(spellId);
        List<SpellCatalog.FusionFormula> candidates = SpellCatalog.candidatesFor(proposed);
        if (proposed.size() >= 2 && candidates.isEmpty()) {
            fail(player, spell.name() + "을 더하면 완성 가능한 융합식이 없습니다.");
            return;
        }

        queue.ingredients.add(spellId);
        queue.updatedAt = now;
        Optional<SpellCatalog.FusionFormula> exact = SpellCatalog.fusionFor(queue.ingredients);
        String names = displayChain(queue.ingredients, " §7+ §d");
        if (exact.isPresent()) {
            SpellDefinition result = SpellCatalog.spell(exact.get().result()).orElseThrow();
            if (!result.id().equals(queue.resultId)) {
                queue.resultId = result.id();
                queue.chargeStartedAt = now;
                queue.requiredTicks = requiredFusionCastTicks(player, result, queue.ingredients.size());
            }
            MagicPlayerData.CastPreparation fusion = data(player).prepareFusion(player, queue.ingredients);
            String extension = SpellCatalog.canExtend(queue.ingredients) ? " · 세 번째 주문 추가 가능" : "";
            if (!fusion.accepted()) {
                queue.resultId = "";
                queue.chargeStartedAt = -1L;
                queue.requiredTicks = 0;
                WorldMagicService.stop(player);
                fail(player, result.name() + " 융합 불가 · " + fusion.message() + extension);
                return;
            }
            WorldMagicService.charge(player, result, true, queue.ingredients, fusion.range(), 0.0);
            ArcaneNoticeService.push(player, Component.literal("§5[융합 전개] §d" + names + " §f→ §e"
                    + result.name() + " §7· " + String.format("%.1f", queue.requiredTicks / 20.0)
                    + "초 유지 후 처음 누른 키를 놓아 시전" + extension));
        } else {
            queue.resultId = "";
            queue.chargeStartedAt = -1L;
            queue.requiredTicks = 0;
            WorldMagicService.stop(player);
            ArcaneNoticeService.push(player, Component.literal("§5[융합 대기] §d" + names + " §7· 후보 "
                    + candidates.size() + "개 · 주문을 하나 더 선택"));
        }
    }

    public static void commitFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.remove(player.getUUID());
        WorldMagicService.stop(player);
        if (ArcaneFieldService.blocksCasting(player)) {
            fail(player, "반마법장 또는 시간 정지로 융합 회로가 소거되었습니다.");
            return;
        }
        if (queue == null || queue.ingredients.isEmpty()) return;
        long now = serverClock(player);
        if (now - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            ArcaneNoticeService.push(player, Component.literal("§7[융합 취소] 회로 유지 시간이 지나 해제되었습니다."));
            return;
        }
        List<String> ingredients = List.copyOf(queue.ingredients);
        String cooldownBlock = fusionCooldownBlock(player, ingredients);
        if (!cooldownBlock.isBlank()) {
            fail(player, cooldownBlock);
            return;
        }
        if (ingredients.size() < 2 || queue.resultId.isBlank() || queue.chargeStartedAt < 0L) {
            ArcaneNoticeService.push(player, Component.literal("§7[융합 취소] 완성된 융합식과 전개 시간이 필요합니다."));
            return;
        }
        long elapsed = now - queue.chargeStartedAt;
        if (elapsed < queue.requiredTicks) {
            int percent = (int) Math.round(100.0 * elapsed / Math.max(1, queue.requiredTicks));
            ArcaneNoticeService.push(player, Component.literal("§7[융합 취소] 복합 회로 전개 " + percent
                    + "% · 완성 전에 처음 누른 키를 놓았습니다."));
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareFusion(player, ingredients);
        if (!cast.accepted() || !queue.resultId.equals(cast.spell().id())) {
            fail(player, cast.accepted() ? "융합 중 결과 회로가 변경되었습니다." : cast.message());
            return;
        }
        castPrepared(player, data, cast);
    }

    public static void clearFusion(ServerPlayer player) {
        clearFusion(player, false);
    }

    public static void clearFusion(ServerPlayer player, boolean notify) {
        FusionQueueState removed = FUSION_QUEUES.remove(player.getUUID());
        if (removed != null) WorldMagicService.stop(player);
        if (notify && removed != null && !removed.ingredients.isEmpty()) {
            ArcaneNoticeService.push(player, Component.literal("§7[융합 취소] 대기 중인 회로를 해제했습니다."));
        }
    }

    public static void clearSession(UUID playerId) {
        FUSION_QUEUES.remove(playerId);
        CHARGES.remove(playerId);
    }

    public static void clearAllSessions() {
        FUSION_QUEUES.clear();
        CHARGES.clear();
    }

    public static List<String> pendingFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.get(player.getUUID());
        if (queue == null) return List.of();
        if (serverClock(player) - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            FUSION_QUEUES.remove(player.getUUID());
            return List.of();
        }
        return List.copyOf(queue.ingredients);
    }

    public static String fusionChargingSpell(ServerPlayer player) {
        FusionQueueState state = FUSION_QUEUES.get(player.getUUID());
        return state == null ? "" : state.resultId;
    }

    public static int fusionChargingTicks(ServerPlayer player) {
        FusionQueueState state = FUSION_QUEUES.get(player.getUUID());
        if (state == null || state.chargeStartedAt < 0L) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, serverClock(player) - state.chargeStartedAt));
    }

    public static int fusionChargingRequiredTicks(ServerPlayer player) {
        FusionQueueState state = FUSION_QUEUES.get(player.getUUID());
        return state == null ? 0 : state.requiredTicks;
    }

    public static int requiredFusionCastTicks(ServerPlayer player, SpellDefinition result, int ingredientCount) {
        MagicPlayerData.MageState state = data(player).state(player);
        int circle = Math.max(1, Math.min(9, result.circle()));
        int direct = requiredCastTicks(player, result);
        int masteryTier = SpellCatalog.masteryTier(state.mastery(result.id()));
        boolean registered = state.known().contains(result.id());
        double complexity = 1.35 + Math.max(0, ingredientCount - 2) * 0.18 + circle * 0.055;
        int unfamiliarPenalty = registered ? 8 : 18 + ingredientCount * 7 + circle * 3;
        int calculated = (int) Math.ceil(direct * complexity) + unfamiliarPenalty - masteryTier * 3;
        int baseMinimum = switch (circle) {
            case 1 -> 10;
            case 2 -> 16;
            case 3 -> 24;
            case 4 -> 36;
            case 5 -> 54;
            case 6 -> 78;
            case 7 -> 108;
            case 8 -> 145;
            default -> 190;
        };
        double staffScale = Math.max(0.25,
                kr.moonseungjun.arcanecircle.registry.ModItems.equipped(player).castTimeMultiplier());
        int minimum = Math.max(1, (int) Math.round(baseMinimum * staffScale));
        int resultTicks = Math.max(minimum, calculated);
        if (registered && result.circle() <= 3 && masteryTier >= 8) return 0;
        return resultTicks;
    }

    private static String fusionCooldownBlock(ServerPlayer player, List<String> ingredients) {
        MagicPlayerData magic = data(player);
        for (String ingredient : ingredients) {
            MagicPlayerData.CooldownStatus status = magic.cooldownStatus(player, ingredient);
            if (!status.active()) continue;
            String name = SpellCatalog.spell(ingredient).map(SpellDefinition::name).orElse(ingredient);
            return name + " 재사용 대기시간이 " + String.format("%.1f", status.remainingTicks() / 20.0)
                    + "초 남아 융합할 수 없습니다.";
        }
        return "";
    }

    private static void startFusionIngredientCooldowns(ServerPlayer player, MagicPlayerData magic,
                                                       List<String> ingredients) {
        for (String ingredient : ingredients) {
            MagicPlayerData.CastPreparation preview = magic.preview(player, ingredient);
            int total = preview.accepted() ? preview.cooldownTicks()
                    : SpellCatalog.spell(ingredient).map(SpellDefinition::cooldownTicks).orElse(20);
            magic.startCooldown(player, ingredient, total);
        }
    }

    private static void tickFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.get(player.getUUID());
        if (queue == null || queue.resultId.isBlank() || queue.chargeStartedAt < 0L) return;
        long now = serverClock(player);
        if (now - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            FUSION_QUEUES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        SpellDefinition result = SpellCatalog.spell(queue.resultId).orElse(null);
        if (result == null) {
            FUSION_QUEUES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        MagicPlayerData.CastPreparation cast = data(player).prepareFusion(player, queue.ingredients);
        if (!cast.accepted() || !queue.resultId.equals(cast.spell().id())) {
            FUSION_QUEUES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        long elapsed = now - queue.chargeStartedAt;
        WorldMagicService.charge(player, result, true, queue.ingredients, cast.range(),
                Math.min(1.0, elapsed / (double) Math.max(1, queue.requiredTicks)));
    }

    static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {
        return WorldMagicService.kineticDistance(player, spell, range);
    }

    private static MagicPlayerData data(ServerPlayer player) {
        return MagicPlayerData.get(((ServerLevel) player.level()).getServer());
    }

    private static long serverClock(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().overworld().getGameTime();
    }

    private static void castPrepared(ServerPlayer player, MagicPlayerData data, MagicPlayerData.CastPreparation cast) {
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }
        if (ArcaneFieldService.blocksCasting(player)) {
            fail(player, "반마법장 또는 시간 정지로 마법 회로가 소거되었습니다.");
            return;
        }
        SpellDefinition spell = cast.spell();
        MagicPlayerData.CooldownStatus cooldown = data.cooldownStatus(player, spell.id());
        if (cooldown.active()) {
            fail(player, String.format("%s 재사용까지 %.1f초", spell.name(), cooldown.remainingTicks() / 20.0));
            return;
        }
        if (!canExecute(player, spell.id(), cast.range())) {
            fail(player, "시전할 대상이나 안전한 공간을 찾지 못했습니다.");
            return;
        }

        CombatGrowthService.Snapshot snapshot = CombatGrowthService.capture(player, cast.range());
        releasePrelude(player, cast);
        data.beginCast(player, cast);
        data.startCooldown(player, spell.id(), cast.cooldownTicks());
        if (cast.fusion()) startFusionIngredientCooldowns(player, data, cast.ingredients());
        SpellKineticsService.launch(player, cast, snapshot);
    }

    static void finishKineticCast(ServerPlayer player, MagicPlayerData.CastPreparation cast,
                                  CombatGrowthService.Snapshot snapshot, boolean executed) {
        if (!executed || !player.isAlive()) {
            ArcaneNoticeService.push(player, Component.literal("§7[시전 종료] 효과가 유효한 대상에 닿지 않았습니다."));
            return;
        }
        MagicPlayerData data = data(player);
        SpellDefinition spell = cast.spell();
        CombatGrowthService.Impact impact = CombatGrowthService.measure(snapshot, spell.circle());
        long marksEarned = kr.moonseungjun.arcanecircle.world.ArcaneEconomyService
                .awardCombat(player, impact, spell.circle());
        MagicPlayerData.CastProgress progress = data.completeCastProgress(player, cast, impact);
        ArcaneQuestData.get(((ServerLevel) player.level()).getServer())
                .recordCast(player, impact, spell.circle(), cast.fusion());
        MagicPlayerData.MageState state = data.state(player);
        MagicPlayerData.EffectiveStats stats = data.effectiveStats(player);

        if (cast.fusion() && progress.mastery().changed()) {
            String chain = displayChain(cast.ingredients(), " §7× §b");
            ArcaneNoticeService.push(player, Component.literal("§d" + chain + " §f→ §e" + spell.name()
                    + " §7· 숙련 " + progress.mastery().casts() + "/" + progress.mastery().required()));
        } else {
            ArcaneNoticeService.push(player, Component.literal("§b" + spell.name() + " §f완료 · 마력 "
                    + (int) state.mana() + "/" + stats.maxMana() + " · 쿨 "
                    + (cast.cooldownTicks() <= 0 ? "없음" : String.format("%.1f", cast.cooldownTicks() / 20.0) + "초")));
        }

        if (impact.meaningful()) {
            String threat = impact.strongKills() > 0 ? " §6강적 처치 " + impact.strongKills()
                    : impact.strongHits() > 0 ? " §e강적 적중 " + impact.strongHits() : "";
            player.sendSystemMessage(Component.literal("§5[주문 숙련] §f적중 " + impact.hits()
                    + " · 처치 " + impact.kills() + threat + " §7· 최고 위협 " + impact.peakThreat()
                    + " · 숙련 +" + impact.masteryGain() + " · 통찰 +" + impact.insightGain()
                    + " · 아르카나 +" + marksEarned));
        }

        ServerLevel level = (ServerLevel) player.level();
        if (progress.mastery().registered()) {
            player.sendSystemMessage(Component.literal("§6[융합 각인] §f" + spell.name()
                    + "의 완성 회로가 마력핵에 새겨졌습니다. 이제 1~5 슬롯에 장착할 수 있습니다."));
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (progress.circle().advanced()) {
            player.sendSystemMessage(Component.literal("§d[써클 승급] §f마력핵이 §5" + progress.circle().current()
                    + "써클§f로 확장되었습니다. 해당 써클 주문서를 해독할 수 있습니다."));
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static boolean canExecute(ServerPlayer player, String id, double range) {
        return switch (id) {
            case "mend" -> player.getHealth() < player.getMaxHealth();
            case "blink", "rift_step", "spatial_gate" -> findBlinkDestination(player, range).isPresent();
            case "plane_shift", "demiplane", "gate" -> true;
            default -> true;
        };
    }

    private static String displayChain(List<String> spellIds, String separator) {
        return spellIds.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .collect(Collectors.joining(separator));
    }

    private static void releasePrelude(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        ServerLevel level = (ServerLevel) player.level();
        SpellDefinition spell = cast.spell();
        double radius = 0.72 + spell.circle() * 0.20 + (cast.fusion() ? 0.35 : 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, cast.fusion() ? 1.0F : 0.72F,
                1.25F - spell.circle() * 0.08F);
        if (cast.fusion()) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.85F, cast.ingredients().size() == 3 ? 1.55F : 1.35F);
        }
    }


    static boolean executeResolved(ServerPlayer player, String id, double range, double power) {
        if (FusionSpellEffects.supports(id)) return FusionSpellEffects.execute(player, id, range, power);
        return switch (id) {
            case "arcane_dart" -> arcaneDart(player, range, power);
            case "ember" -> emberShot(player, range, power);
            case "frost_needle" -> frostNeedle(player, range, power);
            case "gale_step" -> dash(player, range, power);
            case "lesser_ward" -> ward(player, 1, power);
            case "mend" -> mend(player, power);
            case "blink" -> blink(player, range, power, 0);
            case "stone_skin" -> stoneSkin(player, power);
            case "lightning_arc" -> lightningArc(player, range, power);
            case "mana_lance" -> piercingLance(player, range, power, ParticleTypes.ENCHANT);
            case "flame_lance" -> bolt(player, range, power, ParticleTypes.FLAME, 180, 0);
            case "ice_shackles" -> shackles(player, range, power);
            case "wind_blade" -> windBlade(player, range, power);
            case "greater_ward" -> ward(player, 2, power);
            case "flame_wave" -> flameWave(player, range, power);
            case "ice_lance" -> iceLance(player, range, power);
            case "arcane_sight" -> arcaneSight(player, range);
            case "levitation" -> levitation(player, power);
            case "fireball" -> areaAtAim(player, range, power, ParticleTypes.FLAME, true, false);
            case "frost_nova" -> areaAt(player, player.position(), range, power, ParticleTypes.SNOWFLAKE, false, true);
            case "chain_bolt" -> chainBolt(player, range, power);
            case "rift_step" -> blink(player, range, power, 1);
            case "triune_barrage" -> triuneBarrage(player, range, power);
            case "tempest_aegis" -> tempestAegis(player, range, power);
            case "phoenix_field" -> phoenixField(player, range, power);
            case "meteor_shard" -> meteorShard(player, range, power);
            case "blizzard_field" -> blizzardField(player, range, power);
            case "thunder_prison" -> thunderPrison(player, range, power);
            case "mass_mend" -> massMend(player, range, power);
            case "spatial_gate" -> blink(player, range, power, 2);
            case "inferno_domain" -> infernoDomain(player, range, power);
            case "absolute_zero" -> absoluteZero(player, range, power);
            case "tempest_domain" -> tempestDomain(player, range, power);
            case "aegis_citadel" -> aegisCitadel(player, range, power);
            case "arcane_annihilation" -> arcaneAnnihilation(player, range, power);
            default -> HighCircleSpellEffects.execute(player, id, range, power);
        };
    }

    private static boolean arcaneDart(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = frontOrigin(player, 1.25);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        Vec3 side = new Vec3(-player.getLookAngle().z, 0.0, player.getLookAngle().x).normalize().scale(0.16);
        target.ifPresent(mob -> {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0));
        });
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.45F, 1.65F);
        return true;
    }

    private static boolean emberShot(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        boolean result = bolt(player, range, power, ParticleTypes.FLAME, 120, 0);
        target.ifPresent(primary -> {
            for (Mob mob : nearbyTargets(player, primary.position(), 1.8, 1.5)) {
                if (mob == primary) continue;
                ArcaneDamage.hurt(level, player, mob, (float) (power * 0.35));
                mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 60));
            }
        });
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.55F, 1.25F);
        return result;
    }

    private static boolean frostNeedle(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        boolean result = bolt(player, range, power * 0.92, ParticleTypes.SNOWFLAKE, 0, 140);
        target.ifPresent(mob -> {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 75, 2));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 150));
        });
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.38F, 1.75F);
        return result;
    }

    private static boolean bolt(ServerPlayer player, double range, double power, ParticleOptions particle,
                                int fireTicks, int freezeBonus) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.35);
        Optional<Mob> target = lookTarget(player, range);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        if (target.isPresent()) {
            Mob mob = target.get();
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
            if (freezeBonus > 0) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                    mob.getTicksRequiredToFreeze() + freezeBonus + (int) Math.round(power * 8.0)));
        }
        return true;
    }

    private static boolean shackles(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return bolt(player, range, power * 0.55, ParticleTypes.SNOWFLAKE, 0, 70);
        Mob mob = target.get();
        ArcaneDamage.hurt(level, player, mob, (float) power);
        mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze()
                + 180 + (int) Math.round(power * 10.0)));
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3));
        level.playSound(null, mob.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.35F);
        return true;
    }

    private static boolean dash(ServerPlayer player, double range, double power) {
        Vec3 look = player.getLookAngle().normalize();
        double strength = Math.max(1.2, range / 3.4) * Math.max(0.9, Math.sqrt(Math.max(0.1, power)));
        player.push(look.x * strength, Math.max(0.15, look.y * 0.35 + 0.15), look.z * strength);
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    private static boolean ward(ServerPlayer player, int tier, double power) {
        int duration = 100 + tier * 90 + (int) Math.round(power * 7.0);
        int amplifier = Math.max(tier - 1, (int) Math.floor(power / 10.0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        ServerLevel level = (ServerLevel) player.level();
        double radius = 1.25 + tier * 0.55;
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, tier > 1 ? 0.72F : 0.95F);
        return true;
    }

    private static boolean mend(ServerPlayer player, double power) {
        if (player.getHealth() >= player.getMaxHealth()) return false;
        player.heal((float) power);
        return true;
    }

    private static boolean stoneSkin(ServerPlayer player, double power) {
        int duration = 180 + (int) Math.round(power * 16.0);
        int amplifier = power >= 12.0 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amplifier));
        return true;
    }

    private static boolean lightningArc(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        List<Mob> targets = chainedTargets(player, range, 3);
        Vec3 from = frontOrigin(player, 1.35);
        if (targets.isEmpty()) {
            return true;
        }
        double scale = 1.0;
        for (Mob mob : targets) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * scale));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 1));
            from = mob.getEyePosition();
            scale *= 0.78;
        }
        return true;
    }

    private static boolean piercingLance(ServerPlayer player, double range, double power, ParticleOptions particle) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.35);
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        List<Mob> targets = lineTargets(player, range, 1.25);
        for (int index = 0; index < targets.size(); index++) {
            targets.get(index).hurtServer(level, level.damageSources().playerAttack(player),
                    (float) (power * Math.max(0.55, 1.0 - index * 0.12)));
        }
        return true;
    }

    private static boolean flameWave(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.position();
        Vec3 look = horizontalLook(player);
        List<Mob> targets = nearbyTargets(player, origin, range, 4.0).stream()
                .filter(mob -> horizontalDirection(origin, mob.position()).dot(look) > 0.45)
                .toList();
        for (Mob mob : targets) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
        }
        return true;
    }

    private static boolean iceLance(ServerPlayer player, double range, double power) {
        boolean cast = bolt(player, range, power * 1.25, ParticleTypes.SNOWFLAKE, 0, 260);
        lookTarget(player, range).ifPresent(mob -> mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 4)));
        return cast;
    }

    private static boolean arcaneSight(ServerPlayer player, double range) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, player.position(), range, 5.0)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        }
        return true;
    }

    private static boolean levitation(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 34 + (int) Math.round(power * 3.0), 1));
        MageGearService.grantStableDescent(player, 220);
        ServerLevel level = (ServerLevel) player.level();
        return true;
    }

    private static boolean blink(ServerPlayer player, double range, double power, int tier) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> destinationResult = findBlinkDestination(player, range);
        if (destinationResult.isEmpty()) return false;
        BlockPos origin = player.blockPosition();
        BlockPos destination = destinationResult.get();
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        if (tier > 0) {
            int duration = 35 + tier * 35 + (int) Math.round(power * 18.0);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, tier >= 2 ? 2 : 0));
        }
        level.playSound(null, origin, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, 1.1F - tier * 0.18F);
        return true;
    }

    private static Optional<BlockPos> findBlinkDestination(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        for (int step = (int) Math.floor(range); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(player.position().add(look.scale(step)));
            for (int down = 0; down <= 4; down++) {
                BlockPos lowered = candidate.below(down);
                if (safe(level, lowered)) return Optional.of(lowered);
            }
        }
        return Optional.empty();
    }

    private static boolean windBlade(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.35);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        List<Mob> targets = lineTargets(player, range, 1.45);
        for (Mob mob : targets) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            Vec3 away = mob.position().subtract(player.position()).normalize();
            mob.push(away.x * 0.9, 0.18, away.z * 0.9);
        }
        return true;
    }

    private static boolean areaAtAim(ServerPlayer player, double range, double power, ParticleOptions particle,
                                     boolean fire, boolean freeze) {
        Vec3 center = lookTarget(player, range).map(Mob::position).orElse(aimGround(player, range));
        double radius=SpellMetrics.effectRadius("fireball", range, 3);
        boolean result=areaAt(player,center,radius,power,particle,fire,freeze);
        DestructiveMagicService.impact(player,"fireball",center,radius,power);
        return result;
    }

    private static boolean areaAt(ServerPlayer player, Vec3 center, double radius, double power,
                                  ParticleOptions particle, boolean fire, boolean freeze) {
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, radius, 4.0)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
            if (freeze) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                    mob.getTicksRequiredToFreeze() + 180 + (int) Math.round(power * 8.0)));
        }
        return true;
    }

    private static boolean chainBolt(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        List<Mob> targets = chainedTargets(player, range, 5);
        if (targets.isEmpty()) return lightningArc(player, range, power * 0.6);
        Vec3 from = frontOrigin(player, 1.35);
        double scale = 1.0;
        for (Mob target : targets) {
            ArcaneDamage.hurt(level, player, target, (float) (power * scale));
            from = target.getEyePosition();
            scale *= 0.82;
        }
        return true;
    }

    private static boolean triuneBarrage(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = frontOrigin(player, 1.4);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        if (target.isPresent()) {
            Mob mob = target.get();
            ArcaneDamage.hurt(level, player, mob, (float) (power * 1.45));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 140));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 120));
        }
        return true;
    }

    private static boolean tempestAegis(ServerPlayer player, double range, double power) {
        ward(player, 2, power * 0.9);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, player.position(), range, 4.0)) {
            Vec3 away = mob.position().subtract(player.position()).normalize();
            mob.push(away.x * 1.6, 0.35, away.z * 1.6);
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.65));
        }
        return true;
    }

    private static boolean phoenixField(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position();
        AABB box = new AABB(center, center).inflate(range, 4.0, range);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity != player)) {
            if (isAlly(player, entity)) {
                entity.heal((float) (power * 0.65));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            } else if (entity instanceof Mob mob && validTarget(player, mob)) {
                ArcaneDamage.hurt(level, player, mob, (float) power);
                mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 220));
            }
        }
        player.heal((float) (power * 0.8));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1));
        return true;
    }

    private static boolean meteorShard(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = aimGround(player, range);
        areaAt(player, center, 5.0, power * 1.15, ParticleTypes.FLAME, true, false);
        DestructiveMagicService.impact(player,"meteor_shard",center,5.0,power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.25F, 0.75F);
        return true;
    }

    private static boolean blizzardField(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = aimGround(player, range);
        double radius = 6.0;
        for (Mob mob : nearbyTargets(player, center, radius, 5.0)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 320));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 260, 4));
        }
        return true;
    }

    private static boolean thunderPrison(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = aimGround(player, range);
        double radius = 4.5;
        for (Mob mob : nearbyTargets(player, center, radius, 4.0)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 1.1));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 5));
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 180, 0));
        }
        return true;
    }

    private static boolean massMend(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        AABB box = new AABB(player.position(), player.position()).inflate(range, 4.0, range);
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity == player || isAlly(player, entity)));
        for (LivingEntity ally : allies) {
            ally.heal((float) power);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        }
        return true;
    }

    private static boolean infernoDomain(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, 8.0, 6.0)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 1.25));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 420));
        }
        return true;
    }

    private static boolean absoluteZero(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, 8.0, 6.0)) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 1.2));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 600));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 360, 6));
        }
        return true;
    }

    private static boolean tempestDomain(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, 9.0, 7.0)) {
            ArcaneDamage.hurt(level, player, mob, (float) power);
            Vec3 away = mob.position().subtract(center).normalize();
            mob.push(away.x * 2.1, 0.9, away.z * 2.1);
            mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 1));
        }
        return true;
    }

    private static boolean aegisCitadel(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        AABB box = new AABB(player.position(), player.position()).inflate(range, 5.0, range);
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity == player || isAlly(player, entity)));
        int absorption = Math.max(3, (int) Math.floor(power / 7.0));
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 520, absorption));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 420, 2));
        }
        return true;
    }

    private static boolean arcaneAnnihilation(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start=player.getEyePosition(); Vec3 end=start.add(player.getLookAngle().normalize().scale(range));
        DestructiveMagicService.ray(player,"arcane_annihilation",start,end,power);
        List<Mob> targets = lineTargets(player, range, 2.2);
        for (int index = 0; index < targets.size(); index++) {
            targets.get(index).hurtServer(level, level.damageSources().playerAttack(player),
                    (float) (power * Math.max(0.65, 1.15 - index * 0.08)));
        }
        return true;
    }

    private static Optional<Mob> lookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB box = new AABB(start, end).inflate(2.2);
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                        mob -> validTarget(player, mob) && player.hasLineOfSight(mob))
                .stream()
                .filter(mob -> projection(start, look, mob.getEyePosition()) >= 0.0)
                .filter(mob -> projection(start, look, mob.getEyePosition()) <= range + 1.0)
                .min(Comparator.<Mob>comparingDouble(mob -> rayDistanceSquared(start, look, mob.getEyePosition()))
                        .thenComparingDouble(mob -> player.distanceToSqr(mob)));
    }

    private static List<Mob> lineTargets(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB box = new AABB(start, end).inflate(width + 0.8);
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                        mob -> validTarget(player, mob) && player.hasLineOfSight(mob))
                .stream()
                .filter(mob -> projection(start, look, mob.getEyePosition()) >= 0.0)
                .filter(mob -> projection(start, look, mob.getEyePosition()) <= range + 1.0)
                .filter(mob -> rayDistanceSquared(start, look, mob.getEyePosition()) <= width * width)
                .sorted(Comparator.comparingDouble(mob -> projection(start, look, mob.getEyePosition())))
                .toList();
    }

    private static List<Mob> nearbyTargets(ServerPlayer player, Vec3 center, double radius, double vertical) {
        AABB box = new AABB(center, center).inflate(radius, vertical, radius);
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                mob -> validTarget(player, mob));
    }

    private static List<Mob> chainedTargets(ServerPlayer player, double range, int limit) {
        List<Mob> result = new ArrayList<>();
        Optional<Mob> first = lookTarget(player, range);
        if (first.isEmpty()) return result;
        result.add(first.get());
        while (result.size() < limit) {
            Mob last = result.get(result.size() - 1);
            AABB box = last.getBoundingBox().inflate(5.0);
            Optional<Mob> next = ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                            mob -> validTarget(player, mob) && !result.contains(mob) && last.hasLineOfSight(mob))
                    .stream().min(Comparator.comparingDouble(last::distanceToSqr));
            if (next.isEmpty()) break;
            result.add(next.get());
        }
        return result;
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);
    }

    private static boolean isAlly(ServerPlayer player, LivingEntity entity) {
        if (entity == player) return true;
        if (entity instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return true;
        if (entity instanceof ServerPlayer other) return player.isAlliedTo(other);
        return player.isAlliedTo(entity);
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockState below = level.getBlockState(feet.below());
        if (!below.isFaceSturdy(level, feet.below(), Direction.UP)) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition();
        for (int step = (int) Math.max(2, Math.floor(range)); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(origin.add(look.scale(step)));
            for (int down = 0; down <= 8; down++) {
                BlockPos floor = candidate.below(down);
                BlockState state = level.getBlockState(floor);
                if (state.isFaceSturdy(level, floor, Direction.UP)) {
                    return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                }
            }
        }
        return player.position().add(horizontalLook(player).scale(Math.min(5.0, range))).add(0.0, 0.08, 0.0);
    }

    private static Vec3 frontOrigin(ServerPlayer player, double distance) {
        return player.getEyePosition().add(player.getLookAngle().normalize().scale(distance));
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 0.0001 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return flat.lengthSqr() < 0.0001 ? Vec3.ZERO : flat.normalize();
    }

    private static double projection(Vec3 start, Vec3 direction, Vec3 point) {
        return point.subtract(start).dot(direction);
    }

    private static double rayDistanceSquared(Vec3 start, Vec3 direction, Vec3 point) {
        double projection = Math.max(0.0, point.subtract(start).dot(direction));
        Vec3 closest = start.add(direction.scale(projection));
        return closest.distanceToSqr(point);
    }

    private static void fail(ServerPlayer player, String message) {
        ArcaneNoticeService.push(player, Component.literal("§c[마법 실패] §f" + message));
        ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS, 0.35F, 0.7F);
    }
}
