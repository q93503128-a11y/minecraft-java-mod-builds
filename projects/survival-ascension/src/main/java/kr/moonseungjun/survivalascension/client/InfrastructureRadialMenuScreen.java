package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureService;
import kr.moonseungjun.survivalascension.network.InfrastructureActionPayload;
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

public final class InfrastructureRadialMenuScreen extends Screen {
    private static final Entry[] ENTRIES = {
            new Entry("채석장 네트워크", "조약돌1024 · 철256 · 레드스톤128 · 다이아32", new ItemStack(Items.RAIL), InfrastructureProject.QUARRY_NETWORK, Action.FUND),
            new Entry("관개 시설", "구리512 · 철128 · 레드스톤128 · 유리128 · 슬라임32", new ItemStack(Items.WATER_BUCKET), InfrastructureProject.IRRIGATION_WORKS, Action.FUND),
            new Entry("건축 공방", "석재벽돌1024 · 철256 · 구리256 · 레드스톤128 · 흑요석64", new ItemStack(Items.SMITHING_TABLE), InfrastructureProject.BUILDER_FOUNDRY, Action.FUND),
            new Entry("전투 훈련장", "철512 · 금256 · 에메랄드128 · 레드스톤128 · 메아리32", new ItemStack(Items.IRON_SWORD), InfrastructureProject.COMBAT_ACADEMY, Action.FUND),
            new Entry("진행도", "공동 프로젝트 전체 진행도 확인", new ItemStack(Items.MAP), null, Action.STATUS),
            new Entry("뒤로", "통합 메뉴로 돌아가기", new ItemStack(Items.ARROW), null, Action.BACK)
    };
    private static final int ITEM_COUNT = ENTRIES.length;
    private static final double ANGLE_PER_ITEM = 360.0D / ITEM_COUNT;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;

    public InfrastructureRadialMenuScreen() { super(Component.literal("Survival Ascension · 인프라")); }
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
            double angle = Math.toRadians(ANGLE_PER_ITEM * i + 90.0D), radius = Math.sqrt(2.0D * 61.5D * 61.5D);
            graphics.item(ENTRIES[i].icon(), (int) Math.round(cx - radius * Math.cos(angle)) - 8, (int) Math.round(cy + radius * Math.sin(angle)) - 8);
        }
        Entry entry = ENTRIES[selected];
        graphics.text(this.font, entry.title(), cx - this.font.width(entry.title()) / 2, cy - 5, 0xFFFFFFFF, true);
        graphics.text(this.font, entry.detail(), cx - this.font.width(entry.detail()) / 2, cy + 8, 0xFFB8B8B8, false);
        String caption = "공동 진행 · 클릭하면 보유 재료를 필요한 만큼 투입";
        graphics.text(this.font, caption, cx - this.font.width(caption) / 2, cy - 102, 0xFFE0E0E0, true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        Entry entry = ENTRIES[selectedIndex()];
        if (entry.action() == Action.BACK) { this.minecraft.gui.setScreen(new AscensionRadialMenuScreen()); return true; }
        if (entry.action() == Action.STATUS) {
            ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(InfrastructureService.ALL_PROJECTS, InfrastructureService.ACTION_STATUS));
            this.minecraft.gui.setScreen(null); return true;
        }
        ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(entry.project().id(), InfrastructureService.ACTION_FUND));
        this.minecraft.gui.setScreen(null); return true;
    }

    private static int selectedIndex() { double a=correctAngle(360-(getMouseAngle()-ANGLE_PER_ITEM/2)); return Mth.clamp((int)Math.floor(a/ANGLE_PER_ITEM),0,ITEM_COUNT-1); }
    private static double getMouseAngle(){Minecraft m=Minecraft.getInstance();double ox=m.getWindow().getScreenWidth()*.5,oy=m.getWindow().getScreenHeight()*.5;return correctAngle(-Math.toDegrees(Math.atan2(m.mouseHandler.xpos()-ox,m.mouseHandler.ypos()-oy)));}
    private static double correctAngle(double a){while(a<0)a+=360;while(a>=360)a-=360;return a;}
    private enum Action { FUND, STATUS, BACK }
    private record Entry(String title,String detail,ItemStack icon,InfrastructureProject project,Action action){}
    private record WheelElement(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,int x,int y,int selected,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState{
        private WheelElement(RenderPipeline p,TextureSetup t,Matrix3x2f pose,int x,int y,int s,ScreenRectangle a){this(p,t,pose,x,y,s,a,boundsFor(x,y,pose,a));}
        @Override public void buildVertices(VertexConsumer v){for(int i=0;i<ITEM_COUNT;i++){double c=Math.toRadians(correctAngle(ANGLE_PER_ITEM*i+90+ANGLE_PER_ITEM/2)),n=Math.toRadians(correctAngle(ANGLE_PER_ITEM*(i+1)+90+ANGLE_PER_ITEM/2));boolean h=i==selected;float in=((INNER_RADIUS-(h?2:0))/100)*130,out=((OUTER_RADIUS+(h?2:0))/100)*130;float p1ix=x+in*Mth.cos((float)c),p1iy=y+in*Mth.sin((float)c),p1ox=x+out*Mth.cos((float)c),p1oy=y+out*Mth.sin((float)c),p2ox=x+out*Mth.cos((float)n),p2oy=y+out*Mth.sin((float)n),p2ix=x+in*Mth.cos((float)n),p2iy=y+in*Mth.sin((float)n);int r=h?255:0;v.addVertexWith2DPose(pose,p1ox,p1oy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p1ix,p1iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ix,p2iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ox,p2oy).setColor(r,0,0,153);}}
        private static ScreenRectangle boundsFor(int x,int y,Matrix3x2f pose,ScreenRectangle s){ScreenRectangle w=new ScreenRectangle(x-110,y-110,220,220).transformMaxBounds(pose);return s!=null?s.intersection(w):w;}
    }
}
