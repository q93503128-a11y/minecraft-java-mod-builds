package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.equipment.EquipmentReforgeService;
import kr.moonseungjun.survivalascension.network.EquipmentActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2f;

public final class EquipmentRadialMenuScreen extends Screen {
    private static final Entry[] ENTRIES = {
            new Entry("재련", new ItemStack(Items.ANVIL), Action.REFORGE),
            new Entry("분해", new ItemStack(Items.GRINDSTONE), Action.SALVAGE),
            new Entry("장비 정보", new ItemStack(Items.SPYGLASS), Action.INFO),
            new Entry("뒤로", new ItemStack(Items.ARROW), Action.BACK)
    };
    private static final int ITEM_COUNT = ENTRIES.length;
    private static final double ANGLE_PER_ITEM = 360.0D / ITEM_COUNT;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;

    public EquipmentRadialMenuScreen() { super(Component.literal("Survival Ascension · 장비")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        int cx = this.width / 2, cy = this.height / 2, selected = selectedIndex();
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI, TextureSetup.noTexture(), pose, cx, cy, selected, scissor));

        for (int i = 0; i < ITEM_COUNT; i++) {
            double angle = Math.toRadians(ANGLE_PER_ITEM * i + 90.0D);
            double radius = Math.sqrt(2.0D * 61.5D * 61.5D);
            graphics.item(ENTRIES[i].icon(), (int) Math.round(cx - radius * Math.cos(angle)) - 8,
                    (int) Math.round(cy + radius * Math.sin(angle)) - 8);
        }

        ItemStack held = minecraft.player.getMainHandItem();
        int rarity = AscensionAffixes.rarity(held);
        Entry entry = ENTRIES[selected];
        String title = entry.title();
        String detail = detail(entry.action(), held, rarity);
        graphics.text(this.font, title, cx - this.font.width(title) / 2, cy - 5, 0xFFFFFFFF, true);
        graphics.text(this.font, detail, cx - this.font.width(detail) / 2, cy + 8, rarity > 0 ? 0xFFE0E0E0 : 0xFFFF7777, false);
        String caption = rarity > 0
                ? AscensionAffixes.rarityName(held) + " · " + AscensionAffixes.affixSummary(held)
                : "주 손에 희귀 장비를 드세요";
        graphics.text(this.font, caption, cx - this.font.width(caption) / 2, cy - 102, 0xFFFFD37A, true);
    }

    private static String detail(Action action, ItemStack held, int rarity) {
        if (action == Action.BACK) return "통합 메뉴로 돌아가기";
        if (rarity <= 0) return "정예 / 승천 / 신화 장비 필요";
        return switch (action) {
            case REFORGE -> "비용 · " + EquipmentReforgeService.costText(rarity);
            case SALVAGE -> "환급 · " + EquipmentReforgeService.salvageText(rarity);
            case INFO -> AscensionAffixes.affixSummary(held);
            case BACK -> "통합 메뉴로 돌아가기";
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        Action action = ENTRIES[selectedIndex()].action();
        if (action == Action.BACK) {
            this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());
            return true;
        }
        if (action == Action.INFO) return true;
        if (this.minecraft.player == null || !AscensionAffixes.isAffixGear(this.minecraft.player.getMainHandItem())) return true;
        int id = action == Action.REFORGE ? EquipmentReforgeService.ACTION_REFORGE : EquipmentReforgeService.ACTION_SALVAGE;
        ClientPacketDistributor.sendToServer(new EquipmentActionPayload(id));
        this.minecraft.gui.setScreen(null);
        return true;
    }

    private static int selectedIndex() {
        double angle = correctAngle(360.0D - (getMouseAngle() - ANGLE_PER_ITEM / 2.0D));
        return Mth.clamp((int) Math.floor(angle / ANGLE_PER_ITEM), 0, ITEM_COUNT - 1);
    }

    private static double getMouseAngle() {
        Minecraft minecraft = Minecraft.getInstance();
        double ox = minecraft.getWindow().getScreenWidth() * 0.5D;
        double oy = minecraft.getWindow().getScreenHeight() * 0.5D;
        return correctAngle(-Math.toDegrees(Math.atan2(minecraft.mouseHandler.xpos() - ox, minecraft.mouseHandler.ypos() - oy)));
    }

    private static double correctAngle(double angle) {
        while (angle < 0.0D) angle += 360.0D;
        while (angle >= 360.0D) angle -= 360.0D;
        return angle;
    }

    private record Entry(String title, ItemStack icon, Action action) {}
    private enum Action { REFORGE, SALVAGE, INFO, BACK }

    private record WheelElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                int x, int y, int selected, ScreenRectangle scissorArea,
                                ScreenRectangle bounds) implements GuiElementRenderState {
        private WheelElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                             int x, int y, int selected, ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x, y, selected, scissorArea, boundsFor(x, y, pose, scissorArea));
        }
        @Override public void buildVertices(VertexConsumer vertexConsumer) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                double current = Math.toRadians(correctAngle(ANGLE_PER_ITEM * i + 90.0D + ANGLE_PER_ITEM / 2.0D));
                double next = Math.toRadians(correctAngle(ANGLE_PER_ITEM * (i + 1) + 90.0D + ANGLE_PER_ITEM / 2.0D));
                boolean hovered = i == selected;
                float inner = ((INNER_RADIUS - (hovered ? 2.0F : 0.0F)) / 100.0F) * 130.0F;
                float outer = ((OUTER_RADIUS + (hovered ? 2.0F : 0.0F)) / 100.0F) * 130.0F;
                float p1ix = x + inner * Mth.cos((float) current), p1iy = y + inner * Mth.sin((float) current);
                float p1ox = x + outer * Mth.cos((float) current), p1oy = y + outer * Mth.sin((float) current);
                float p2ox = x + outer * Mth.cos((float) next), p2oy = y + outer * Mth.sin((float) next);
                float p2ix = x + inner * Mth.cos((float) next), p2iy = y + inner * Mth.sin((float) next);
                int r = hovered ? 255 : 0, a = 153;
                vertexConsumer.addVertexWith2DPose(pose, p1ox, p1oy).setColor(r, 0, 0, a);
                vertexConsumer.addVertexWith2DPose(pose, p1ix, p1iy).setColor(r, 0, 0, a);
                vertexConsumer.addVertexWith2DPose(pose, p2ix, p2iy).setColor(r, 0, 0, a);
                vertexConsumer.addVertexWith2DPose(pose, p2ox, p2oy).setColor(r, 0, 0, a);
            }
        }
        private static ScreenRectangle boundsFor(int x, int y, Matrix3x2f pose, ScreenRectangle scissor) {
            ScreenRectangle wheel = new ScreenRectangle(x - 110, y - 110, 220, 220).transformMaxBounds(pose);
            return scissor != null ? scissor.intersection(wheel) : wheel;
        }
    }
}
