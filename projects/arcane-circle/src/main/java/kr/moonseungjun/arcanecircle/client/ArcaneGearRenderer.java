package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;

/**
 * Adds a recognisable wizard silhouette on top of the equipment textures: a pointed hat,
 * layered shoulders, a split flared robe and rune-banded boots. It is geometry, not particles,
 * and the tiers differ in both size and ornament count.
 */
public final class ArcaneGearRenderer {
    private static final ContextKey<Integer> HAT_TIER = key("hat_tier");
    private static final ContextKey<Integer> ROBE_TIER = key("robe_tier");
    private static final ContextKey<Integer> BOOTS_TIER = key("boots_tier");

    private ArcaneGearRenderer() {}

    public static void registerStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                state.setRenderData(HAT_TIER, hatTier(avatar.getItemBySlot(EquipmentSlot.HEAD)));
                state.setRenderData(ROBE_TIER, robeTier(avatar.getItemBySlot(EquipmentSlot.CHEST)));
                state.setRenderData(BOOTS_TIER, bootsTier(avatar.getItemBySlot(EquipmentSlot.FEET)));
            }
        });
    }

    public static void onPlayerRender(RenderPlayerEvent.Post<?> event) {
        int hat = event.getRenderState().getRenderDataOrDefault(HAT_TIER, 0);
        int robe = event.getRenderState().getRenderDataOrDefault(ROBE_TIER, 0);
        int boots = event.getRenderState().getRenderDataOrDefault(BOOTS_TIER, 0);
        if (hat <= 0 && robe <= 0 && boots <= 0) return;

        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        if (robe > 0) submitRobe(stack, event, robe);
        if (boots > 0) submitBoots(stack, event, boots);
        if (hat > 0) submitHat(stack, event, hat);
        stack.popPose();
    }

    private static void submitHat(PoseStack stack, RenderPlayerEvent.Post<?> event, int tier) {
        int body = bodyColor(tier);
        int trim = trimColor(tier);
        double brim = 0.48 + tier * 0.055;
        double inner = 0.24;
        double y = 2.02;
        double height = 0.72 + tier * 0.15;
        int segments = 16 + tier * 4;
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose, out) -> {
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                double b = Math.PI * 2.0 * (i + 1) / segments;
                float ax = (float) (Math.cos(a) * brim);
                float az = (float) (Math.sin(a) * brim);
                float bx = (float) (Math.cos(b) * brim);
                float bz = (float) (Math.sin(b) * brim);
                float iax = (float) (Math.cos(a) * inner);
                float iaz = (float) (Math.sin(a) * inner);
                float ibx = (float) (Math.cos(b) * inner);
                float ibz = (float) (Math.sin(b) * inner);
                quad(out, pose, ax, (float) y, az, bx, (float) y, bz,
                        ibx, (float) (y + 0.025), ibz, iax, (float) (y + 0.025), iaz, body);

                double bend = 0.10 * Math.sin(a * 1.5 + tier);
                float apexX = (float) (0.10 + tier * 0.025);
                float apexZ = (float) (-0.04 + bend);
                quad(out, pose,
                        iax, (float) (y + 0.02), iaz,
                        ibx, (float) (y + 0.02), ibz,
                        apexX, (float) (y + height), apexZ,
                        apexX, (float) (y + height), apexZ,
                        (i & 1) == 0 ? body : darken(body));
            }
        });
        submitRing(stack, event, 0.30, y + 0.13, 18, trim, 1.1F + tier * 0.18F);
        if (tier >= 2) submitRing(stack, event, brim * 0.86, y + 0.01, 28, trim, 0.8F);
        if (tier >= 3) submitStar(stack, event, 0.22, y + height * 0.60, trim, 1.05F);
    }

    private static void submitRobe(PoseStack stack, RenderPlayerEvent.Post<?> event, int tier) {
        int body = bodyColor(tier);
        int dark = darken(body);
        int trim = trimColor(tier);
        float shoulder = 0.40F + tier * 0.035F;
        float waist = 0.32F;
        float hem = 0.48F + tier * 0.075F;
        float top = 1.48F;
        float middle = 0.92F;
        float bottom = 0.08F;
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose, out) -> {
            // Shoulder mantle.
            quad(out, pose, -shoulder, top, -0.24F, shoulder, top, -0.24F,
                    waist, 1.22F, -0.31F, -waist, 1.22F, -0.31F, body);
            quad(out, pose, shoulder, top, 0.24F, -shoulder, top, 0.24F,
                    -waist, 1.22F, 0.31F, waist, 1.22F, 0.31F, body);
            quad(out, pose, -shoulder, top, 0.24F, -shoulder, top, -0.24F,
                    -waist, 1.22F, -0.31F, -waist, 1.22F, 0.31F, dark);
            quad(out, pose, shoulder, top, -0.24F, shoulder, top, 0.24F,
                    waist, 1.22F, 0.31F, waist, 1.22F, -0.31F, dark);

            // Four flared skirt panels with a visible front split.
            quad(out, pose, -waist, middle, -0.26F, -0.035F, middle, -0.27F,
                    -0.07F, bottom, -0.39F, -hem, bottom, -0.37F, body);
            quad(out, pose, 0.035F, middle, -0.27F, waist, middle, -0.26F,
                    hem, bottom, -0.37F, 0.07F, bottom, -0.39F, dark);
            quad(out, pose, waist, middle, 0.26F, -waist, middle, 0.26F,
                    -hem, bottom, 0.38F, hem, bottom, 0.38F, body);
            quad(out, pose, -waist, middle, 0.25F, -waist, middle, -0.25F,
                    -hem, bottom, -0.36F, -hem, bottom, 0.36F, dark);
            quad(out, pose, waist, middle, -0.25F, waist, middle, 0.25F,
                    hem, bottom, 0.36F, hem, bottom, -0.36F, body);
        });
        submitBelt(stack, event, tier, trim);
        if (tier >= 2) {
            submitRing(stack, event, hem * 0.92, bottom + 0.035, 30, trim, 0.78F + tier * 0.14F);
        }
        if (tier >= 3) {
            submitStar(stack, event, 0.17, 1.17, trim, 0.92F);
        }
    }

    private static void submitBoots(PoseStack stack, RenderPlayerEvent.Post<?> event, int tier) {
        int body = darken(bodyColor(tier));
        int trim = trimColor(tier);
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.debugFilledBox(), (pose, out) -> {
            box(out, pose, -0.29F, 0.02F, -0.19F, -0.03F, 0.36F, 0.22F, body);
            box(out, pose, 0.03F, 0.02F, -0.19F, 0.29F, 0.36F, 0.22F, body);
        });
        submitAnkleBand(stack, event, -0.16, 0.27, trim, tier);
        submitAnkleBand(stack, event, 0.16, 0.27, trim, tier);
    }

    private static void submitBelt(PoseStack stack, RenderPlayerEvent.Post<?> event, int tier, int color) {
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.lines(), (pose, out) -> {
            line(out, pose, -0.34F, 0.93F, -0.285F, 0.34F, 0.93F, -0.285F, color, 1.0F + tier * 0.18F);
            line(out, pose, 0.34F, 0.93F, -0.285F, 0.34F, 0.93F, 0.285F, color, 1.0F + tier * 0.18F);
            line(out, pose, 0.34F, 0.93F, 0.285F, -0.34F, 0.93F, 0.285F, color, 1.0F + tier * 0.18F);
            line(out, pose, -0.34F, 0.93F, 0.285F, -0.34F, 0.93F, -0.285F, color, 1.0F + tier * 0.18F);
        });
    }

    private static void submitAnkleBand(PoseStack stack, RenderPlayerEvent.Post<?> event,
                                        double centerX, double y, int color, int tier) {
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.lines(), (pose, out) -> {
            int segments = 12;
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                double b = Math.PI * 2.0 * (i + 1) / segments;
                line(out, pose,
                        (float) (centerX + Math.cos(a) * 0.15), (float) y, (float) (Math.sin(a) * 0.19),
                        (float) (centerX + Math.cos(b) * 0.15), (float) y, (float) (Math.sin(b) * 0.19),
                        color, 0.82F + tier * 0.16F);
            }
        });
    }

    private static void submitRing(PoseStack stack, RenderPlayerEvent.Post<?> event,
                                   double radius, double y, int segments, int color, float width) {
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.lines(), (pose, out) -> {
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2.0 * i / segments;
                double b = Math.PI * 2.0 * (i + 1) / segments;
                line(out, pose, (float) (Math.cos(a) * radius), (float) y, (float) (Math.sin(a) * radius),
                        (float) (Math.cos(b) * radius), (float) y, (float) (Math.sin(b) * radius), color, width);
            }
        });
    }

    private static void submitStar(PoseStack stack, RenderPlayerEvent.Post<?> event,
                                   double radius, double y, int color, float width) {
        event.getSubmitNodeCollector().submitCustomGeometry(stack, RenderTypes.lines(), (pose, out) -> {
            int points = 5;
            for (int i = 0; i < points; i++) {
                int j = (i + 2) % points;
                double a = -Math.PI / 2.0 + Math.PI * 2.0 * i / points;
                double b = -Math.PI / 2.0 + Math.PI * 2.0 * j / points;
                line(out, pose, (float) (Math.cos(a) * radius), (float) y, (float) (Math.sin(a) * radius),
                        (float) (Math.cos(b) * radius), (float) y, (float) (Math.sin(b) * radius), color, width);
            }
        });
    }

    private static void box(VertexConsumer out, PoseStack.Pose pose,
                            float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        quad(out, pose, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, color);
        quad(out, pose, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, color);
        quad(out, pose, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, color);
        quad(out, pose, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, color);
        quad(out, pose, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, color);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, int color) {
        out.addVertex(pose, ax, ay, az).setColor(color);
        out.addVertex(pose, bx, by, bz).setColor(color);
        out.addVertex(pose, cx, cy, cz).setColor(color);
        out.addVertex(pose, dx, dy, dz).setColor(color);
    }

    private static void line(VertexConsumer out, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             int color, float width) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.0001F) return;
        dx /= length;
        dy /= length;
        dz /= length;
        out.addVertex(pose, ax, ay, az).setColor(color).setNormal(pose, dx, dy, dz).setLineWidth(width);
        out.addVertex(pose, bx, by, bz).setColor(color).setNormal(pose, dx, dy, dz).setLineWidth(width);
    }

    private static int hatTier(ItemStack stack) {
        if (stack.getItem() == ModItems.ARCHMAGE_CROWN.get()) return 3;
        if (stack.getItem() == ModItems.SAGE_HAT.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_HAT.get()) return 1;
        return 0;
    }

    private static int robeTier(ItemStack stack) {
        if (stack.getItem() == ModItems.ARCHMAGE_ROBE.get()) return 3;
        if (stack.getItem() == ModItems.SAGE_ROBE.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_ROBE.get()) return 1;
        return 0;
    }

    private static int bootsTier(ItemStack stack) {
        if (stack.getItem() == ModItems.FROSTSTEP_BOOTS.get()) return 3;
        if (stack.getItem() == ModItems.SKYWALKER_BOOTS.get()) return 2;
        if (stack.getItem() == ModItems.MAGE_BOOTS.get()) return 1;
        return 0;
    }

    private static int bodyColor(int tier) {
        return switch (tier) {
            case 1 -> 0xEA34234D;
            case 2 -> 0xEE273D72;
            case 3 -> 0xF02B153F;
            default -> 0xEA302044;
        };
    }

    private static int trimColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFFF0B6FF;
            case 2 -> 0xFF86DFFF;
            case 3 -> 0xFFFFD56A;
            default -> 0xFFE8C0FF;
        };
    }

    private static int darken(int argb) {
        int a = argb & 0xFF000000;
        int r = (int) (((argb >> 16) & 0xFF) * 0.58);
        int g = (int) (((argb >> 8) & 0xFF) * 0.58);
        int b = (int) ((argb & 0xFF) * 0.58);
        return a | (r << 16) | (g << 8) | b;
    }

    private static ContextKey<Integer> key(String path) {
        return new ContextKey<>(Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, path));
    }
}
