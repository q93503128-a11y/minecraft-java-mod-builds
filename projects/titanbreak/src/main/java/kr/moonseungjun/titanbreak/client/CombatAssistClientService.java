package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.network.CombatAssistIntentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Client camera correction only; this class never attacks or fires for the player. */
public final class CombatAssistClientService {
    private static boolean lastRequested;
    private static int lockedTargetId = -1;
    private static Vec3 smoothedAim;

    private CombatAssistClientService() {}

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null || mc.gui.screen() != null) {
            resetLocal();
            return;
        }

        TitanClientState.AugmentMeta assist = TitanClientState.augmentMeta("target_assist");
        TitanClientState.AugmentMeta predictive = TitanClientState.augmentMeta("predictive_combat_core");
        TitanClientState.AugmentMeta autopilot = TitanClientState.augmentMeta("combat_autopilot");
        boolean standardInstalled = (assist != null && assist.installed()) || (predictive != null && predictive.installed());
        boolean autopilotActive = TitanClientState.integer("autopilotTicks", 0) > 0
                && autopilot != null && autopilot.installed();
        boolean combatIntent = mc.options.keyAttack.isDown() || mc.options.keyUse.isDown() || TitanKeyMappings.ANALYSIS.isDown();
        boolean requested = (standardInstalled && combatIntent) || autopilotActive;
        if (requested != lastRequested || mc.player.tickCount % 20 == 0) {
            ClientPacketDistributor.sendToServer(new CombatAssistIntentPayload(standardInstalled && combatIntent));
            lastRequested = requested;
        }

        boolean normalAssistActive = TitanClientState.flag("assistActive");
        if (!requested || (!normalAssistActive && !autopilotActive) || TitanClientState.integer("jamTicks", 0) > 0) {
            lockedTargetId = -1;
            smoothedAim = null;
            return;
        }

        int assistEnh = assist != null && assist.installed() ? assist.enhancement() : 0;
        int predictiveEnh = predictive != null && predictive.installed() ? predictive.enhancement() : -1;
        int autopilotEnh = autopilotActive ? autopilot.enhancement() : -1;
        double range = autopilotActive ? 36.0D : predictiveEnh >= 0 ? 48.0D : 24.0D;
        double cone = autopilotEnh >= 10 ? 63.0D : autopilotActive ? 46.0D
                : predictiveEnh >= 0 ? 14.0D : (assistEnh >= 3 ? 14.0D : 10.0D);
        LivingEntity target = chooseTarget(mc, range, cone, predictiveEnh >= 7 || autopilotEnh >= 7);
        if (target == null) return;

        int weakpointEnh = OcularAnalysisClientService.enhancement("weakpoint_analysis_eye");
        boolean autopilotWeakpoint = autopilotEnh >= 7
                && (weakpointEnh >= 0 || OcularAnalysisClientService.enhancement("structural_section_eye") >= 7);
        double aimHeight = (weakpointEnh >= 10 || autopilotWeakpoint)
                ? OcularAnalysisClientService.preferredAimHeight(target)
                : (assistEnh >= 7 ? 0.68D : 0.55D);
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * aimHeight, 0.0D);
        if (predictiveEnh >= 0 && (!TitanClientState.flag("active") || predictiveEnh >= 10)) {
            double seconds = predictiveEnh >= 5 ? 0.40D : 0.20D;
            aim = aim.add(target.getDeltaMovement().scale(seconds * 20.0D));
        }
        if (assistEnh >= 10 || weakpointEnh >= 10 || autopilotActive) {
            double retain = autopilotEnh >= 10 ? 0.48D : autopilotActive ? 0.58D : 0.68D;
            smoothedAim = smoothedAim == null ? aim : smoothedAim.scale(retain).add(aim.scale(1.0D - retain));
            aim = smoothedAim;
        } else {
            smoothedAim = aim;
        }

        Vec3 origin = mc.player.getEyePosition();
        Vec3 delta = aim.subtract(origin);
        if (delta.lengthSqr() <= 1.0E-6D) return;
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float desiredYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        float desiredPitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        float yawError = Mth.wrapDegrees(desiredYaw - mc.player.getYRot());
        float pitchError = Mth.wrapDegrees(desiredPitch - mc.player.getXRot());

        float maxStep = autopilotEnh >= 10 ? 3.20F : autopilotActive ? 2.05F
                : assistEnh >= 5 ? 1.05F : assistEnh > 0 ? 0.72F : 0.42F;
        if (predictiveEnh >= 7) maxStep += 0.12F;
        float response = autopilotEnh >= 10 ? 0.34F : autopilotActive ? 0.28F : 0.20F;
        float yawStep = Mth.clamp(yawError * response, -maxStep, maxStep);
        float pitchStep = Mth.clamp(pitchError * (response * 0.90F), -maxStep * 0.75F, maxStep * 0.75F);
        mc.player.setYRot(mc.player.getYRot() + yawStep);
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + pitchStep, -90.0F, 90.0F));
    }

    private static LivingEntity chooseTarget(Minecraft mc, double range, double coneDegrees, boolean multiTrack) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle().normalize();
        AABB area = mc.player.getBoundingBox().inflate(range);
        List<Candidate> candidates = new ArrayList<>();
        for (LivingEntity living : mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player && entity.isAlive() && entity instanceof Enemy)) {
            if (!mc.player.hasLineOfSight(living)) continue;
            Vec3 center = living.position().add(0.0D, living.getBbHeight() * 0.55D, 0.0D);
            Vec3 direction = center.subtract(eye);
            double distance = direction.length();
            if (distance <= 1.0E-6D || distance > range) continue;
            double dot = Mth.clamp(look.dot(direction.scale(1.0D / distance)), -1.0D, 1.0D);
            double angle = Math.toDegrees(Math.acos(dot));
            if (angle > coneDegrees) continue;
            candidates.add(new Candidate(living, angle + distance * 0.012D));
        }
        if (candidates.isEmpty()) {
            lockedTargetId = -1;
            return null;
        }
        candidates.sort(Comparator.comparingDouble(Candidate::score));

        if (!multiTrack && lockedTargetId >= 0) {
            for (Candidate candidate : candidates) {
                if (candidate.entity().getId() == lockedTargetId) return candidate.entity();
            }
        }
        LivingEntity selected = candidates.getFirst().entity();
        lockedTargetId = selected.getId();
        return selected;
    }

    private record Candidate(LivingEntity entity, double score) {}

    private static void resetLocal() {
        lastRequested = false;
        lockedTargetId = -1;
        smoothedAim = null;
    }
}
