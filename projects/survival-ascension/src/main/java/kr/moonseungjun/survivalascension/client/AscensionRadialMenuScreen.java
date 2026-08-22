package kr.moonseungjun.survivalascension.client;

/*
 * Radial interaction geometry and presentation are adapted from MineMenu.
 * MineMenu: Copyright (c) 2013 Dylan Miller, MIT License.
 * 26.2 GUI render-state submission follows the current public radial pattern used by JustDireThings.
 * See THIRD_PARTY_NOTICES.md and META-INF/third-party/MINEMENU_MIT.txt.
 */

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import org.joml.Matrix3x2f;

public final class AscensionRadialMenuScreen extends Screen {
    private static final Entry[] ENTRIES = {
            new Entry("숙련", "레벨 · XP · 현재 효과", new ItemStack(Items.EXPERIENCE_BOTTLE), Action.SKILLS),
            new Entry("채굴", "자동 · 굴착 · 광맥 · 추출 · 터널", new ItemStack(Items.DIAMOND_PICKAXE), Action.MINING),
            new Entry("건축", "선 · 벽 · 바닥 배치 모드", new ItemStack(Items.BRICKS), Action.CONSTRUCTION),
            new Entry("장비", "희귀 장비 정보 · 재련 · 분해", new ItemStack(Items.ANVIL), Action.EQUIPMENT),
            new Entry("인프라", "공동 자원 투입 · 대형 기능 해금", new ItemStack(Items.BEACON), Action.INFRASTRUCTURE),
            new Entry("가이드", "가이드 · 해금표 · 통계 · 조작", new ItemStack(Items.WRITTEN_BOOK), Action.GUIDE),
            new Entry("닫기", "게임으로 돌아가기", new ItemStack(Items.BARRIER), Action.CLOSE)
    };
    private static final int ITEM_COUNT = ENTRIES.length;
    private static final double ANGLE_PER_ITEM = 360.0D / ITEM_COUNT;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;
    private static final int MENU_A = 153, SELECT_A = 153;

    public AscensionRadialMenuScreen() { super(Component.literal("Survival Ascension")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        int centerX = this.width / 2, centerY = this.height / 2, selected = selectedIndex();
        Matrix3x2f capturedPose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI, TextureSetup.noTexture(), capturedPose, centerX, centerY, selected, scissor));
        for (int i = 0; i < ITEM_COUNT; i++) {
            double angle = Math.toRadians(ANGLE_PER_ITEM * i + 90.0D);
            double radius = Math.sqrt(2.0D * 61.5D * 61.5D);
            graphics.item(ENTRIES[i].icon(), (int) Math.round(centerX - radius * Math.cos(angle)) - 8,
                    (int) Math.round(centerY + radius * Math.sin(angle)) - 8);
        }
        Entry entry = ENTRIES[selected];
        graphics.text(this.font, entry.title(), centerX - this.font.width(entry.title()) / 2, centerY - 5, 0xFFFFFFFF, true);
        graphics.text(this.font, entry.detail(), centerX - this.font.width(entry.detail()) / 2, centerY + 8, 0xFFB8B8B8, false);
        String caption = "M · 통합 메뉴";
        graphics.text(this.font, caption, centerX - this.font.width(caption) / 2, centerY - 102, 0xFFE0E0E0, true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        activate(ENTRIES[selectedIndex()].action());
        return true;
    }

    private void activate(Action action) {
        switch (action) {
            case SKILLS -> this.minecraft.gui.setScreen(new SkillsScreen(this));
            case MINING -> this.minecraft.gui.setScreen(new MiningRadialMenuScreen());
            case CONSTRUCTION -> this.minecraft.gui.setScreen(new ConstructionRadialMenuScreen());
            case EQUIPMENT -> this.minecraft.gui.setScreen(new EquipmentRadialMenuScreen());
            case INFRASTRUCTURE -> this.minecraft.gui.setScreen(new InfrastructureRadialMenuScreen());
            case GUIDE -> this.minecraft.gui.setScreen(new GuideScreen(this, GuideScreen.Page.OVERVIEW));
            case CLOSE -> this.minecraft.gui.setScreen(null);
        }
    }

    private static int selectedIndex() {
        double angle = correctAngle(360.0D - (getMouseAngle() - ANGLE_PER_ITEM / 2.0D));
        return Mth.clamp((int) Math.floor(angle / ANGLE_PER_ITEM), 0, ITEM_COUNT - 1);
    }
    private static double getMouseAngle() {
        Minecraft minecraft = Minecraft.getInstance();
        double ox = minecraft.getWindow().getScreenWidth() * 0.5D, oy = minecraft.getWindow().getScreenHeight() * 0.5D;
        return correctAngle(-Math.toDegrees(Math.atan2(minecraft.mouseHandler.xpos() - ox, minecraft.mouseHandler.ypos() - oy)));
    }
    private static double correctAngle(double angle) {
        while (angle < 0.0D) angle += 360.0D;
        while (angle >= 360.0D) angle -= 360.0D;
        return angle;
    }

    private record Entry(String title, String detail, ItemStack icon, Action action) {}
    private enum Action { SKILLS, MINING, CONSTRUCTION, EQUIPMENT, INFRASTRUCTURE, GUIDE, CLOSE }

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
                int r = hovered ? 255 : 0, a = hovered ? SELECT_A : MENU_A;
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
