package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.survivalascension.mining.MiningMode;
import kr.moonseungjun.survivalascension.network.MiningModePayload;
import kr.moonseungjun.survivalascension.progress.SkillType;
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

public final class MiningRadialMenuScreen extends Screen {
    private static final Entry[] ENTRIES = {
            new Entry("자동", "광석=광맥 / 일반=굴착", new ItemStack(Items.IRON_PICKAXE), MiningMode.AUTO, false),
            new Entry("굴착", "Lv.10 · 항상 시선 평면 광역", new ItemStack(Items.STONE), MiningMode.PLANE, false),
            new Entry("광맥", "Lv.30 · 연결된 같은 광석만", new ItemStack(Items.DIAMOND_ORE), MiningMode.VEIN, false),
            new Entry("추출", "Lv.90 · 주변 같은 광석 비연결 탐색", new ItemStack(Items.NETHER_STAR), MiningMode.EXTRACT, false),
            new Entry("터널", "Lv.90 + 채석장 네트워크 · 5×5×8", new ItemStack(Items.NETHERITE_PICKAXE), MiningMode.BORE, false),
            new Entry("뒤로", "통합 메뉴로 돌아가기", new ItemStack(Items.ARROW), MiningMode.AUTO, true)
    };
    private static final int ITEM_COUNT = ENTRIES.length;
    private static final double ANGLE_PER_ITEM = 360.0D / ITEM_COUNT;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;

    public MiningRadialMenuScreen() { super(Component.literal("Survival Ascension · 채굴")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (Minecraft.getInstance().level == null) return;
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

        Entry entry = ENTRIES[selected];
        int level = ClientSkillState.level(SkillType.MINING);
        boolean unlocked = entry.back() || level >= entry.mode().requiredLevel();
        String title = unlocked ? entry.title() : entry.title() + " · 잠김";
        String detail = unlocked ? entry.detail() : "채굴 Lv." + entry.mode().requiredLevel() + " 필요";
        graphics.text(this.font, title, cx - this.font.width(title) / 2, cy - 5, unlocked ? 0xFFFFFFFF : 0xFFFF7777, true);
        graphics.text(this.font, detail, cx - this.font.width(detail) / 2, cy + 8, 0xFFB8B8B8, false);
        String caption = "채굴 Lv." + level + " · Shift = 항상 1×1";
        graphics.text(this.font, caption, cx - this.font.width(caption) / 2, cy - 102, 0xFFE0E0E0, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        Entry entry = ENTRIES[selectedIndex()];
        if (entry.back()) {
            this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());
            return true;
        }
        int level = ClientSkillState.level(SkillType.MINING);
        if (level < entry.mode().requiredLevel()) return true;
        ClientPacketDistributor.sendToServer(new MiningModePayload(entry.mode().id()));
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

    private record Entry(String title, String detail, ItemStack icon, MiningMode mode, boolean back) {}

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
