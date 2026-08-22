package kr.moonseungjun.survivalascension.client;

/*
 * Radial interaction geometry and presentation are adapted from MineMenu.
 * MineMenu: Copyright (c) 2013 Dylan Miller, MIT License.
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
    private static final int ITEM_COUNT = 6;
    private static final double ANGLE_PER_ITEM = 360.0D / ITEM_COUNT;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;

    // MineMenu's default visual values: translucent black wheel, translucent red selection.
    private static final int MENU_R = 0;
    private static final int MENU_G = 0;
    private static final int MENU_B = 0;
    private static final int MENU_A = 153;
    private static final int SELECT_R = 255;
    private static final int SELECT_G = 0;
    private static final int SELECT_B = 0;
    private static final int SELECT_A = 153;

    private static final Entry[] ENTRIES = {
            new Entry("숙련", "레벨 · XP · 현재 효과", new ItemStack(Items.EXPERIENCE_BOTTLE), Action.SKILLS),
            new Entry("가이드", "모드 핵심 규칙과 사용법", new ItemStack(Items.WRITTEN_BOOK), Action.GUIDE),
            new Entry("해금표", "Lv.10/30/60/90 변화", new ItemStack(Items.NETHER_STAR), Action.UNLOCKS),
            new Entry("통계", "현재 숙련 전체 요약", new ItemStack(Items.SPYGLASS), Action.STATS),
            new Entry("조작", "키와 정밀 모드 설명", new ItemStack(Items.COMPASS), Action.CONTROLS),
            new Entry("닫기", "게임으로 돌아가기", new ItemStack(Items.BARRIER), Action.CLOSE)
    };

    public AscensionRadialMenuScreen() {
        super(Component.literal("Survival Ascension"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // MineMenu-style overlay: keep the live world visible behind the wheel.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int selected = selectedIndex();
        graphics.guiRenderState.addGuiElement(new WheelElement(
                RenderPipelines.GUI,
                TextureSetup.noTexture(),
                graphics.pose(),
                centerX,
                centerY,
                selected,
                null
        ));

        for (int i = 0; i < ITEM_COUNT; i++) {
            double angle = Math.toRadians(ANGLE_PER_ITEM * i + 90.0D);
            double radius = Math.sqrt(2.0D * 61.5D * 61.5D);
            int iconX = (int) Math.round(centerX - radius * Math.cos(angle)) - 8;
            int iconY = (int) Math.round(centerY + radius * Math.sin(angle)) - 8;
            graphics.item(ENTRIES[i].icon(), iconX, iconY);
        }

        Entry entry = ENTRIES[selected];
        int titleX = centerX - this.font.width(entry.title()) / 2;
        int detailX = centerX - this.font.width(entry.detail()) / 2;
        graphics.text(this.font, entry.title(), titleX, centerY - 5, 0xFFFFFFFF, true);
        graphics.text(this.font, entry.detail(), detailX, centerY + 8, 0xFFB8B8B8, false);
        graphics.text(this.font, "M · 통합 메뉴", centerX - this.font.width("M · 통합 메뉴") / 2,
                centerY - 102, 0xFFE0E0E0, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        activate(ENTRIES[selectedIndex()].action());
        return true;
    }

    private void activate(Action action) {
        switch (action) {
            case SKILLS -> this.minecraft.gui.setScreen(new SkillsScreen(this));
            case GUIDE -> this.minecraft.gui.setScreen(new GuideScreen(this, GuideScreen.Page.OVERVIEW));
            case UNLOCKS -> this.minecraft.gui.setScreen(new GuideScreen(this, GuideScreen.Page.UNLOCKS));
            case STATS -> this.minecraft.gui.setScreen(new GuideScreen(this, GuideScreen.Page.STATS));
            case CONTROLS -> this.minecraft.gui.setScreen(new GuideScreen(this, GuideScreen.Page.CONTROLS));
            case CLOSE -> this.minecraft.gui.setScreen(null);
        }
    }

    private static int selectedIndex() {
        double mouseAngle = getMouseAngle();
        mouseAngle -= ANGLE_PER_ITEM / 2.0D;
        mouseAngle = 360.0D - mouseAngle;
        mouseAngle = correctAngle(mouseAngle);
        int index = (int) Math.floor(mouseAngle / ANGLE_PER_ITEM);
        return Mth.clamp(index, 0, ITEM_COUNT - 1);
    }

    private static double getMouseAngle() {
        Minecraft minecraft = Minecraft.getInstance();
        double originX = minecraft.getWindow().getScreenWidth() * 0.5D;
        double originY = minecraft.getWindow().getScreenHeight() * 0.5D;
        double x = minecraft.mouseHandler.xpos();
        double y = minecraft.mouseHandler.ypos();
        return correctAngle(-Math.toDegrees(Math.atan2(x - originX, y - originY)));
    }

    private static double correctAngle(double angle) {
        while (angle < 0.0D) angle += 360.0D;
        while (angle >= 360.0D) angle -= 360.0D;
        return angle;
    }

    private record Entry(String title, String detail, ItemStack icon, Action action) {}

    private enum Action {
        SKILLS, GUIDE, UNLOCKS, STATS, CONTROLS, CLOSE
    }

    private record WheelElement(
            RenderPipeline pipeline,
            TextureSetup textureSetup,
            Matrix3x2f pose,
            int x,
            int y,
            int selected,
            ScreenRectangle scissorArea,
            ScreenRectangle bounds
    ) implements GuiElementRenderState {
        private WheelElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                             int x, int y, int selected, ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x, y, selected, scissorArea,
                    boundsFor(x, y, pose, scissorArea));
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                double current = Math.toRadians(correctAngle(ANGLE_PER_ITEM * i + 90.0D + ANGLE_PER_ITEM / 2.0D));
                double next = Math.toRadians(correctAngle(ANGLE_PER_ITEM * (i + 1) + 90.0D + ANGLE_PER_ITEM / 2.0D));
                boolean hovered = i == selected;

                float inner = ((INNER_RADIUS - (hovered ? 2.0F : 0.0F)) / 100.0F) * 130.0F;
                float outer = ((OUTER_RADIUS + (hovered ? 2.0F : 0.0F)) / 100.0F) * 130.0F;
                float p1ix = x + inner * Mth.cos((float) current);
                float p1iy = y + inner * Mth.sin((float) current);
                float p1ox = x + outer * Mth.cos((float) current);
                float p1oy = y + outer * Mth.sin((float) current);
                float p2ox = x + outer * Mth.cos((float) next);
                float p2oy = y + outer * Mth.sin((float) next);
                float p2ix = x + inner * Mth.cos((float) next);
                float p2iy = y + inner * Mth.sin((float) next);

                int r = hovered ? SELECT_R : MENU_R;
                int g = hovered ? SELECT_G : MENU_G;
                int b = hovered ? SELECT_B : MENU_B;
                int a = hovered ? SELECT_A : MENU_A;
                vertexConsumer.addVertexWith2DPose(pose, p1ox, p1oy).setColor(r, g, b, a);
                vertexConsumer.addVertexWith2DPose(pose, p1ix, p1iy).setColor(r, g, b, a);
                vertexConsumer.addVertexWith2DPose(pose, p2ix, p2iy).setColor(r, g, b, a);
                vertexConsumer.addVertexWith2DPose(pose, p2ox, p2oy).setColor(r, g, b, a);
            }
        }

        private static ScreenRectangle boundsFor(int x, int y, Matrix3x2f pose, ScreenRectangle scissor) {
            ScreenRectangle wheel = new ScreenRectangle(x - 110, y - 110, 220, 220).transformMaxBounds(pose);
            return scissor != null ? scissor.intersection(wheel) : wheel;
        }
    }
}
