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
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;
    private static final double ICON_RADIUS = Math.sqrt(2.0D * 61.5D * 61.5D);

    public MiningRadialMenuScreen() { super(Component.literal("Survival Ascension · 채굴")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (Minecraft.getInstance().level == null) return;
        int cx = this.width / 2, cy = this.height / 2;
        int selected = RadialMenuGeometry.selectedIndex(ITEM_COUNT);
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI, TextureSetup.noTexture(), pose, cx, cy, selected, scissor));
        for (int i = 0; i < ITEM_COUNT; i++) {
            double angle = RadialMenuGeometry.iconRadians(i, ITEM_COUNT);
            graphics.item(ENTRIES[i].icon(), (int)Math.round(cx + ICON_RADIUS * Math.cos(angle)) - 8, (int)Math.round(cy + ICON_RADIUS * Math.sin(angle)) - 8);
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
        Entry entry = ENTRIES[RadialMenuGeometry.selectedIndex(ITEM_COUNT)];
        if (entry.back()) { this.minecraft.gui.setScreen(new AscensionRadialMenuScreen()); return true; }
        int level = ClientSkillState.level(SkillType.MINING);
        if (level < entry.mode().requiredLevel()) return true;
        ClientPacketDistributor.sendToServer(new MiningModePayload(entry.mode().id()));
        this.minecraft.gui.setScreen(null);
        return true;
    }

    private record Entry(String title, String detail, ItemStack icon, MiningMode mode, boolean back) {}
    private record WheelElement(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int x, int y, int selected, ScreenRectangle scissorArea, ScreenRectangle bounds) implements GuiElementRenderState {
        private WheelElement(RenderPipeline p, TextureSetup t, Matrix3x2f pose, int x, int y, int s, ScreenRectangle a) { this(p,t,pose,x,y,s,a,boundsFor(x,y,pose,a)); }
        @Override public void buildVertices(VertexConsumer v) {
            for (int i = 0; i < ITEM_COUNT; i++) {
                double start = RadialMenuGeometry.sectorStartRadians(i, ITEM_COUNT), end = RadialMenuGeometry.sectorEndRadians(i, ITEM_COUNT);
                boolean h = i == selected;
                float in = ((INNER_RADIUS - (h ? 2 : 0)) / 100) * 130, out = ((OUTER_RADIUS + (h ? 2 : 0)) / 100) * 130;
                float p1ix=x+in*Mth.cos((float)start),p1iy=y+in*Mth.sin((float)start),p1ox=x+out*Mth.cos((float)start),p1oy=y+out*Mth.sin((float)start),p2ox=x+out*Mth.cos((float)end),p2oy=y+out*Mth.sin((float)end),p2ix=x+in*Mth.cos((float)end),p2iy=y+in*Mth.sin((float)end); int r=h?255:0;
                v.addVertexWith2DPose(pose,p1ox,p1oy).setColor(r,0,0,153); v.addVertexWith2DPose(pose,p1ix,p1iy).setColor(r,0,0,153); v.addVertexWith2DPose(pose,p2ix,p2iy).setColor(r,0,0,153); v.addVertexWith2DPose(pose,p2ox,p2oy).setColor(r,0,0,153);
            }
        }
        private static ScreenRectangle boundsFor(int x,int y,Matrix3x2f pose,ScreenRectangle s){ScreenRectangle w=new ScreenRectangle(x-110,y-110,220,220).transformMaxBounds(pose);return s!=null?s.intersection(w):w;}
    }
}
