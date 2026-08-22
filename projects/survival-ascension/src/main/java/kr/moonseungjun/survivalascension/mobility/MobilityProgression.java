package kr.moonseungjun.survivalascension.mobility;

/*
 * Movement design is independently implemented after studying ParCool's public
 * action set. ParCool is LGPL-3.0 and no ParCool source/assets are copied here.
 */

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
    private static final Map<UUID, Boolean> AIR_DASH_USED = new HashMap<>();

    private MobilityProgression() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int level = SkillProgressData.get(player).level(player, SkillType.MOBILITY);
        applyAttributes(player, level);
        UUID uuid = player.getUUID();
        if (player.onGround()) AIR_DASH_USED.put(uuid, false);
        trackTraversal(player);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        TRAVERSAL.remove(uuid);
        DASH_READY_TICK.remove(uuid);
        AIR_DASH_USED.remove(uuid);
    }

    public static void performAction(ServerPlayer player) {
        if (player.isSpectator() || player.isCreative() || player.isPassenger() || player.isInWater() || player.isFallFlying()) return;
        int level = SkillProgressData.get(player).level(player, SkillType.MOBILITY);
        if (level < 30) return;
        long now = player.level().getGameTime();
        if (now < DASH_READY_TICK.getOrDefault(player.getUUID(), 0L)) return;

        boolean airborne = !player.onGround();
        if (airborne) {
            if (level < 60 || AIR_DASH_USED.getOrDefault(player.getUUID(), false)) return;
            AIR_DASH_USED.put(player.getUUID(), true);
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-5D) return;
        horizontal = horizontal.normalize();
        double power = SkillTuning.mobilityDashPower(level);
        double lift = airborne ? Math.max(0.04D, player.getDeltaMovement().y) : Math.max(0.10D, player.getDeltaMovement().y);
        Vec3 impulse = horizontal.scale(power);
        player.setDeltaMovement(impulse.x, lift, impulse.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        DASH_READY_TICK.put(player.getUUID(), now + SkillTuning.mobilityDashCooldownTicks(level));
        announceMilestones(player, SkillProgressionService.award(player, SkillType.MOBILITY, airborne ? 5L : 3L));
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
        }
        TRAVERSAL.put(uuid, new TraversalState(dimension, pos.x, pos.z, bank));
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
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§d[기동 해금] §fR · 지상 돌진"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§d[기동 해금] §f공중에서 R을 한 번 더 사용할 수 있습니다."));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§d[기동 해금] §f극한 돌진 · 더 긴 거리와 짧은 재사용 대기시간"));
    }

    private record TraversalState(ResourceKey<Level> dimension, double x, double z, double bank) {}
}
