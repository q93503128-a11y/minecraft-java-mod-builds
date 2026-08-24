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
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;
    private static final double ICON_RADIUS = Math.sqrt(2.0D * 61.5D * 61.5D);

    public AscensionRadialMenuScreen() { super(Component.literal("Survival Ascension")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (Minecraft.getInstance().level == null) return;
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int selected = RadialMenuGeometry.selectedIndex(ITEM_COUNT);
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI, TextureSetup.noTexture(), pose, centerX, centerY, selected, scissor));

        for (int i = 0; i < ITEM_COUNT; i++) {
            double angle = RadialMenuGeometry.iconRadians(i, ITEM_COUNT);
            graphics.item(ENTRIES[i].icon(),
                    (int) Math.round(centerX + ICON_RADIUS * Math.cos(angle)) - 8,
                    (int) Math.round(centerY + ICON_RADIUS * Math.sin(angle)) - 8);
        }

        Entry entry = ENTRIES[selected];
        graphics.text(this.font, entry.title(), centerX - this.font.width(entry.title()) / 2, centerY - 5, 0xFFFFFFFF, true);
        graphics.text(this.font, entry.detail(), centerX - this.font.width(entry.detail()) / 2, centerY + 8, 0xFFB8B8B8, false);
        String caption = "M · 통합 메뉴";
        graphics.text(this.font, caption, centerX - this.font.width(caption) / 2, centerY - 102, 0xFFE0E0E0, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        activate(ENTRIES[RadialMenuGeometry.selectedIndex(ITEM_COUNT)].action());
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

    private record Entry(String title, String detail, ItemStack icon, Action action) {}
    private enum Action { SKILLS, MINING, CONSTRUCTION, EQUIPMENT, INFRASTRUCTURE, GUIDE, CLOSE }

    private record WheelElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                                int x, int y, int selected, ScreenRectangle scissorArea,
                                ScreenRectangle bounds) implements GuiElementRenderState {
        private WheelElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                             int x, int y, int selected, ScreenRectangle scissorArea) {
            this(pipeline, textureSetup, pose, x, y, selected, scissorArea, boundsFor(x, y, pose, scissorArea));
        }

        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                double start = RadialMenuGeometry.sectorStartRadians(i, ITEM_COUNT);
                double end = RadialMenuGeometry.sectorEndRadians(i, ITEM_COUNT);
                boolean hovered = i == selected;
                float inner = ((INNER_RADIUS - (hovered ? 2.0F : 0.0F)) / 100.0F) * 130.0F;
                float outer = ((OUTER_RADIUS + (hovered ? 2.0F : 0.0F)) / 100.0F) * 130.0F;
                float p1ix = x + inner * Mth.cos((float) start), p1iy = y + inner * Mth.sin((float) start);
                float p1ox = x + outer * Mth.cos((float) start), p1oy = y + outer * Mth.sin((float) start);
                float p2ox = x + outer * Mth.cos((float) end), p2oy = y + outer * Mth.sin((float) end);
                float p2ix = x + inner * Mth.cos((float) end), p2iy = y + inner * Mth.sin((float) end);
                int red = hovered ? 255 : 0;
                vertexConsumer.addVertexWith2DPose(pose, p1ox, p1oy).setColor(red, 0, 0, 153);
                vertexConsumer.addVertexWith2DPose(pose, p1ix, p1iy).setColor(red, 0, 0, 153);
                vertexConsumer.addVertexWith2DPose(pose, p2ix, p2iy).setColor(red, 0, 0, 153);
                vertexConsumer.addVertexWith2DPose(pose, p2ox, p2oy).setColor(red, 0, 0, 153);
            }
        }

        private static ScreenRectangle boundsFor(int x, int y, Matrix3x2f pose, ScreenRectangle scissor) {
            ScreenRectangle wheel = new ScreenRectangle(x - 110, y - 110, 220, 220).transformMaxBounds(pose);
            return scissor != null ? scissor.intersection(wheel) : wheel;
        }
    }
}
