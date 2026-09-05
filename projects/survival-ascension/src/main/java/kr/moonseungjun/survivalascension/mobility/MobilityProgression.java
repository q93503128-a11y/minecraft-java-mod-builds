package kr.moonseungjun.survivalascension.mobility;

/*
 * Movement design is independently implemented after studying ParCool's public
 * action set. ParCool is LGPL-3.0 and no ParCool source/assets are copied here.
 */

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.endgame.FinalAscensionData;
import kr.moonseungjun.survivalascension.expedition.ExpeditionAction;
import kr.moonseungjun.survivalascension.expedition.ExpeditionProgression;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.network.MobilityCooldownPayload;
import kr.moonseungjun.survivalascension.network.SkillNetwork;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MobilityProgression {
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mobility_speed");
    private static final Identifier STEP_ID = Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mobility_step_height");
    private static final Identifier SAFE_FALL_ID = Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mobility_safe_fall");
    private static final Map<UUID, TraversalState> TRAVERSAL = new HashMap<>();
    private static final Map<UUID, Long> DASH_READY_TICK = new HashMap<>();
    private static final Map<UUID, Integer> AIR_DASH_COUNT = new HashMap<>();
    private static final Map<UUID, Integer> APPLIED_ATTRIBUTE_LEVEL = new HashMap<>();

    private MobilityProgression() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        // Attribute state changes only when the mobility level changes. The old path re-read player
        // progression and rewrote three transient modifiers every server tick for every player.
        if (player.tickCount % 10 == 0 || !APPLIED_ATTRIBUTE_LEVEL.containsKey(uuid)) {
            refreshAttributesIfNeeded(player);
        }
        if (player.onGround() && AIR_DASH_COUNT.getOrDefault(uuid, 0) != 0) AIR_DASH_COUNT.put(uuid, 0);
        trackTraversal(player);
        syncDashCooldown(player);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        APPLIED_ATTRIBUTE_LEVEL.remove(player.getUUID());
        refreshAttributesIfNeeded(player);
        // A dedicated-server relog must not reset a live dash cooldown. Client state can survive
        // world switches, so always send the authoritative server value on join.
        if (DASH_READY_TICK.containsKey(player.getUUID())) syncDashCooldown(player);
        else SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(0));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        // Traversal distance is session-local, but cooldown and airborne quota are gameplay state.
        // Keep those two until landing or actual server shutdown so relogging cannot refresh a dash.
        TRAVERSAL.remove(uuid);
        APPLIED_ATTRIBUTE_LEVEL.remove(uuid);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        TRAVERSAL.clear();
        DASH_READY_TICK.clear();
        AIR_DASH_COUNT.clear();
        APPLIED_ATTRIBUTE_LEVEL.clear();
    }

    public static void performAction(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative() || player.isPassenger() || player.isInWater() || player.isFallFlying()) return;
        int level = SkillProgressData.get(player).level(player, SkillType.MOBILITY);
        if (level < 30) return;
        long now = player.level().getGameTime();
        UUID uuid = player.getUUID();
        if (now < DASH_READY_TICK.getOrDefault(uuid, 0L)) return;

        boolean airborne = !player.onGround();
        int usedAirDashes = 0;
        if (airborne) {
            if (level < 60) return;
            usedAirDashes = AIR_DASH_COUNT.getOrDefault(uuid, 0);
            int allowed = maxAirDashes(player, level);
            if (usedAirDashes >= allowed) return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-5D) return;
        horizontal = horizontal.normalize();
        if (airborne) AIR_DASH_COUNT.put(uuid, usedAirDashes + 1);
        boolean finalMastery = level >= 100 && finalAscensionComplete(player);
        double power = SkillTuning.mobilityDashPower(level) + (finalMastery ? 0.15D : 0.0D);
        double lift = airborne ? Math.max(0.04D, player.getDeltaMovement().y) : Math.max(0.10D, player.getDeltaMovement().y);
        Vec3 impulse = horizontal.scale(power);
        player.setDeltaMovement(impulse.x, lift, impulse.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        int cooldown = SkillTuning.mobilityDashCooldownTicks(level);
        if (finalMastery) cooldown = Math.max(12, cooldown - 4);
        DASH_READY_TICK.put(uuid, now + cooldown);
        SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(cooldown));
        announceMilestones(player, SkillProgressionService.award(player, SkillType.MOBILITY, airborne ? 5L : 3L));
        ExpeditionProgression.recordAction(player, ExpeditionAction.DASHES_USED, 1);
    }

    private static void syncDashCooldown(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Long ready = DASH_READY_TICK.get(uuid);
        if (ready == null) return;
        long rawRemaining = Math.max(0L, ready - player.level().getGameTime());
        int remaining = (int)Math.min(Integer.MAX_VALUE, rawRemaining);
        // The HUD follows authoritative server game ticks instead of wall-clock time. This keeps it
        // correct through low TPS, pause, and short server stalls. Packets exist only while cooling down.
        SkillNetwork.sendMobilityCooldown(player, new MobilityCooldownPayload(remaining));
        if (remaining <= 0) DASH_READY_TICK.remove(uuid);
    }

    private static int maxAirDashes(ServerPlayer player, int level) {
        if (level < 60) return 0;
        if (level >= 100 && finalAscensionComplete(player)) {
            return ExpeditionProgression.hasFieldMastery(player) ? 5 : 4;
        }
        if (level >= 90
                && player.level() instanceof ServerLevel serverLevel
                && WorldAscensionData.get(serverLevel.getServer()).stage() >= 2
                && InfrastructureData.get(player).isComplete(InfrastructureProject.ASCENSION_NEXUS)) {
            if (level >= 100 && ExpeditionProgression.hasFieldMastery(player)) return 4;
            return level >= 100 ? 3 : 2;
        }
        return 1;
    }

    private static boolean finalAscensionComplete(ServerPlayer player) {
        return player.level() instanceof ServerLevel level
                && FinalAscensionData.get(level.getServer()).isComplete();
    }

    private static void trackTraversal(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        TraversalState state = TRAVERSAL.get(uuid);
        if (state == null || !state.dimension.equals(dimension)) {
            TRAVERSAL.put(uuid, new TraversalState(dimension, pos.x, pos.z, 0.0D));
            return;
        }
        double dx = pos.x - state.x, dz = pos.z - state.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double bank = state.bank;
        boolean legitimate = player.isSprinting()
                && !player.isPassenger()
                && !player.getAbilities().flying
                && !player.isFallFlying()
                && !player.isSwimming()
                && distance > 0.01D
                && distance <= 1.75D;
        if (legitimate) bank += distance;
        if (bank >= 6.0D && !player.isCreative() && !player.isSpectator()) {
            int units = (int) Math.floor(bank / 6.0D);
            bank -= units * 6.0D;
            announceMilestones(player, SkillProgressionService.award(player, SkillType.MOBILITY, units * 2L));
            ExpeditionProgression.recordSkillAction(player, SkillType.MOBILITY, units * 6);
        }
        TRAVERSAL.put(uuid, new TraversalState(dimension, pos.x, pos.z, bank));
    }

    private static void refreshAttributesIfNeeded(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int level = SkillProgressData.get(player).level(player, SkillType.MOBILITY);
        Integer applied = APPLIED_ATTRIBUTE_LEVEL.get(uuid);
        if (applied != null && applied == level) return;
        applyAttributes(player, level);
        APPLIED_ATTRIBUTE_LEVEL.put(uuid, level);
    }

    private static void applyAttributes(ServerPlayer player, int level) {
        var speed = player.getAttributes().getInstance(Attributes.MOVEMENT_SPEED);
        var step = player.getAttributes().getInstance(Attributes.STEP_HEIGHT);
        var safeFall = player.getAttributes().getInstance(Attributes.SAFE_FALL_DISTANCE);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID,
                SkillTuning.mobilitySpeedMultiplier(level) - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (step != null) step.addOrUpdateTransientModifier(new AttributeModifier(STEP_ID,
                SkillTuning.mobilityStepHeight(level) - 0.6D, AttributeModifier.Operation.ADD_VALUE));
        if (safeFall != null) safeFall.addOrUpdateTransientModifier(new AttributeModifier(SAFE_FALL_ID,
                SkillTuning.mobilitySafeFallDistance(level) - 3.0D, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§d[기동 해금] §f1블록 단차 자동 넘기기 + 낙하 안전 강화"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§d[기동 해금] §fV · 지상 돌진"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§d[기동 해금] §f공중에서 V를 한 번 더 사용할 수 있습니다."));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§d[기동 해금] §f극한 돌진 · 종말 단계 승천 중추 완공 시 공중 돌진 2회"));
        if (oldLevel < 100 && newLevel >= 100) {
            String dashes = ExpeditionProgression.hasFieldMastery(player) ? "4회" : "3회";
            player.sendSystemMessage(Component.literal("§d[기동 숙련 VI] §f2블록 단차 · 16블록 안전 낙하 · 중추 완공 시 공중 돌진 " + dashes));
        }
    }

    private record TraversalState(ResourceKey<Level> dimension, double x, double z, double bank) {}
}
