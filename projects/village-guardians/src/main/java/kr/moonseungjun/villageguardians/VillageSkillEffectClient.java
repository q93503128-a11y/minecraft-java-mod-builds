package kr.moonseungjun.villageguardians;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageSkillEffectClient {
    private static final Map<Integer, Motion> MOTIONS = new HashMap<>();
    private static boolean renderListenerRegistered;

    private VillageSkillEffectClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                VillageSkillEffectEntities.SKILL_EFFECT.get(),
                VillageSkillEffectRenderer::new);
        if (!renderListenerRegistered) {
            renderListenerRegistered = true;
            NeoForge.EVENT_BUS.addListener(VillageSkillEffectClient::onRenderPlayer);
        }
    }

    public static void acceptMotion(VillageNetwork.SkillMotionPayload payload) {
        if (payload == null || payload.entityId() < 0 || payload.durationTicks() <= 0) return;
        long now = System.nanoTime();
        MOTIONS.compute(payload.entityId(), (id, old) -> {
            long startedAt = old != null && old.name.equals(payload.motion())
                    ? old.startedAt : now;
            return new Motion(payload.motion(), startedAt,
                    Math.max(old == null ? 0L : old.expiresAt,
                            now + payload.durationTicks() * 50_000_000L));
        });
    }

    private static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, Motion>> iterator = MOTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < now) iterator.remove();
        }
        int id = event.getRenderState().id;
        Motion motion = MOTIONS.get(id);
        if (motion == null) return;

        PoseStack stack = event.getPoseStack();
        if ("vanguard_spin".equals(motion.name)) {
            float elapsedSeconds = (now - motion.startedAt) / 1_000_000_000.0f;
            float radians = elapsedSeconds * (float) Math.toRadians(900.0);
            stack.mulPose(new Quaternionf().rotateY(radians));

            // Rotate the whole rendered avatar including the held weapon, not the camera.
            event.getRenderState().bodyRot = 0.0f;
            event.getRenderState().yRot = 0.0f;
            event.getRenderState().xRot = 0.0f;
            event.getRenderState().walkAnimationSpeed = 0.0f;
            return;
        }

        if (!motion.name.startsWith("promotion:")) return;
        String skill = motion.name.substring("promotion:".length());
        long lifetime = Math.max(1L, motion.expiresAt - motion.startedAt);
        float t = Math.max(0.0f, Math.min(1.0f, (now - motion.startedAt) / (float) lifetime));
        float envelope = (float) Math.sin(Math.PI * t);
        float oscillation = (float) Math.sin(Math.PI * 2.0 * t);

        event.getRenderState().walkAnimationSpeed = 0.0f;

        if (skill.startsWith("vanguard_")) {
            stack.mulPose(new Quaternionf()
                    .rotateY(oscillation * 0.11f)
                    .rotateX(-envelope * 0.08f));
        } else if (skill.startsWith("ranger_")) {
            stack.mulPose(new Quaternionf()
                    .rotateX(envelope * 0.07f)
                    .rotateY(-oscillation * 0.035f));
        } else if (skill.startsWith("arcanist_")) {
            stack.translate(0.0f, envelope * 0.055f, 0.0f);
            stack.mulPose(new Quaternionf().rotateY(oscillation * 0.055f));
        } else if (skill.startsWith("luminar_")) {
            stack.translate(0.0f, envelope * 0.045f, 0.0f);
            stack.mulPose(new Quaternionf().rotateZ(oscillation * 0.025f));
        } else if (skill.startsWith("warden_")) {
            stack.mulPose(new Quaternionf().rotateX(envelope * 0.09f));
        }

        // Final second-promotion signatures receive a stronger whole-body read.
        switch (skill) {
            case "vanguard_heaven_sever" ->
                    stack.mulPose(new Quaternionf().rotateX(-envelope * 0.22f));
            case "ranger_meteor_bow" ->
                    stack.mulPose(new Quaternionf().rotateX(envelope * 0.15f));
            case "arcanist_singularity" -> {
                stack.translate(0.0f, envelope * 0.075f, 0.0f);
                stack.mulPose(new Quaternionf().rotateY(oscillation * 0.10f));
            }
            case "luminar_last_miracle" -> {
                stack.translate(0.0f, envelope * 0.085f, 0.0f);
                stack.mulPose(new Quaternionf().rotateZ(oscillation * 0.045f));
            }
            case "warden_fortress_descent" ->
                    stack.mulPose(new Quaternionf().rotateX(envelope * 0.17f));
            default -> {
            }
        }
    }

    public static void clear() {
        MOTIONS.clear();
    }

    private record Motion(String name, long startedAt, long expiresAt) {}
}
