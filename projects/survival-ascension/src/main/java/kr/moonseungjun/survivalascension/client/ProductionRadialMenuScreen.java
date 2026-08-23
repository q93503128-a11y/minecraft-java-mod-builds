package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureService;
import kr.moonseungjun.survivalascension.network.InfrastructureActionPayload;
import kr.moonseungjun.survivalascension.production.ProductionService;
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

public final class ProductionRadialMenuScreen extends Screen {
    private static final Entry[] ENTRIES = {
            new Entry("시설 투자", "산업 가공소 건설 재료를 현재 인벤토리에서 투입", new ItemStack(Items.SMITHING_TABLE), "", Action.FUND),
            new Entry("제련 배치", "철원석96 · 구리원석96 · 석탄64", new ItemStack(Items.BLAST_FURNACE), "metalworks", Action.PRODUCE),
            new Entry("구조재 배치", "통나무192 · 조약돌384 · 철32", new ItemStack(Items.IRON_AXE), "timberworks", Action.PRODUCE),
            new Entry("식량 배치", "밀128 · 당근64 · 감자64 · 비트32", new ItemStack(Items.HAY_BLOCK), "provisions", Action.PRODUCE),
            new Entry("정밀 부품 배치", "레드128 · 자수정64 · 금32 · 석영64", new ItemStack(Items.COMPARATOR), "precision", Action.PRODUCE),
            new Entry("현장 보급 출고", "보급권1 → 금32 · 자수정16 · 메아리2", new ItemStack(Items.MINECART), "", Action.DISPATCH),
            new Entry("생산 현황", "4계통 버퍼 · 누적 사이클 · 현장 보급권", new ItemStack(Items.WRITABLE_BOOK), "", Action.STATUS),
            new Entry("뒤로", "인프라 메뉴로 돌아가기", new ItemStack(Items.ARROW), "", Action.BACK)
    };
    private static final int ITEM_COUNT = ENTRIES.length;
    private static final double ANGLE_PER_ITEM = 360.0D / ITEM_COUNT;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;

    public ProductionRadialMenuScreen() { super(Component.literal("Survival Ascension · 산업 생산망")); }
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
        String caption = "4계통 모두 1배치 → 현장 보급권 1회 · 버퍼 계통당 최대3";
        graphics.text(this.font, caption, cx - this.font.width(caption) / 2, cy - 102, 0xFFE0E0E0, true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return false;
        Entry entry = ENTRIES[selectedIndex()];
        if (entry.action() == Action.BACK) { this.minecraft.gui.setScreen(new InfrastructureRadialMenuScreen()); return true; }
        String action = switch (entry.action()) {
            case FUND -> InfrastructureService.ACTION_FUND;
            case STATUS -> ProductionService.ACTION_STATUS;
            case DISPATCH -> ProductionService.ACTION_DISPATCH;
            case PRODUCE -> ProductionService.ACTION_PREFIX + entry.programId();
            case BACK -> "";
        };
        ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(InfrastructureProject.INDUSTRIAL_WORKS.id(), action));
        this.minecraft.gui.setScreen(null);
        return true;
    }

    private static int selectedIndex() { double a=correctAngle(360-(getMouseAngle()-ANGLE_PER_ITEM/2)); return Mth.clamp((int)Math.floor(a/ANGLE_PER_ITEM),0,ITEM_COUNT-1); }
    private static double getMouseAngle(){Minecraft m=Minecraft.getInstance();double ox=m.getWindow().getScreenWidth()*.5,oy=m.getWindow().getScreenHeight()*.5;return correctAngle(-Math.toDegrees(Math.atan2(m.mouseHandler.xpos()-ox,m.mouseHandler.ypos()-oy)));}
    private static double correctAngle(double a){while(a<0)a+=360;while(a>=360)a-=360;return a;}
    private enum Action { FUND, PRODUCE, DISPATCH, STATUS, BACK }
    private record Entry(String title,String detail,ItemStack icon,String programId,Action action){}
    private record WheelElement(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,int x,int y,int selected,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState{
        private WheelElement(RenderPipeline p,TextureSetup t,Matrix3x2f pose,int x,int y,int s,ScreenRectangle a){this(p,t,pose,x,y,s,a,boundsFor(x,y,pose,a));}
        @Override public void buildVertices(VertexConsumer v){for(int i=0;i<ITEM_COUNT;i++){double c=Math.toRadians(correctAngle(ANGLE_PER_ITEM*i+90+ANGLE_PER_ITEM/2)),n=Math.toRadians(correctAngle(ANGLE_PER_ITEM*(i+1)+90+ANGLE_PER_ITEM/2));boolean h=i==selected;float in=((INNER_RADIUS-(h?2:0))/100)*130,out=((OUTER_RADIUS+(h?2:0))/100)*130;float p1ix=x+in*Mth.cos((float)c),p1iy=y+in*Mth.sin((float)c),p1ox=x+out*Mth.cos((float)c),p1oy=y+out*Mth.sin((float)c),p2ox=x+out*Mth.cos((float)n),p2oy=y+out*Mth.sin((float)n),p2ix=x+in*Mth.cos((float)n),p2iy=y+in*Mth.sin((float)n);int r=h?255:0;v.addVertexWith2DPose(pose,p1ox,p1oy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p1ix,p1iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ix,p2iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ox,p2oy).setColor(r,0,0,153);}}
        private static ScreenRectangle boundsFor(int x,int y,Matrix3x2f pose,ScreenRectangle s){ScreenRectangle w=new ScreenRectangle(x-110,y-110,220,220).transformMaxBounds(pose);return s!=null?s.intersection(w):w;}
    }
}
