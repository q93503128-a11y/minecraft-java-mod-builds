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
            new Entry("시설 투자", "산업 가공소 투자 · 마지막 투입은 실제 통 준공 현장 필요", new ItemStack(Items.SMITHING_TABLE), "", Action.FUND),
            new Entry("제련 배치", "철원석96 · 구리원석96 · 석탄64", new ItemStack(Items.BLAST_FURNACE), "metalworks", Action.PRODUCE),
            new Entry("구조재 배치", "통나무192 · 조약돌384 · 철32", new ItemStack(Items.IRON_AXE), "timberworks", Action.PRODUCE),
            new Entry("식량 배치", "밀128 · 당근64 · 감자64 · 비트32", new ItemStack(Items.HAY_BLOCK), "provisions", Action.PRODUCE),
            new Entry("정밀 부품 배치", "레드128 · 자수정64 · 금32 · 석영64", new ItemStack(Items.COMPARATOR), "precision", Action.PRODUCE),
            new Entry("물류 거점 연결", "4블록 내 기본 통 앵커 등록/해제 · 보급권1 · 한도3→토목6→중추9", new ItemStack(Items.BARREL), "", Action.DEPOT),
            new Entry("창고 통 연결", "4블록 내 실제 통 ↔ 반경6 자신의 거점 · 거점당 최대8 · 보급권 없음", new ItemStack(Items.CHEST), "", Action.WAREHOUSE),
            new Entry("현장 일괄 적재", "주 인벤토리 대량자원 → 가까운 사용 가능 실제 통 · 핫바/장비 유지", new ItemStack(Items.HOPPER), "", Action.OFFLOAD),
            new Entry("물리 화물 수레", "일반=대량 · Shift=전선묶음(원정/방어/요새 각1) · 레일6+·동력레일·호퍼·제어", new ItemStack(Items.CHEST_MINECART), "", Action.FREIGHT),
            new Entry("전초기지 승격", "등록 통+침대+모닥불+작업대+화로 · 보급권2/철32/금8/석탄32", new ItemStack(Items.CAMPFIRE), "", Action.OUTPOST),
            new Entry("전초 방어전", "보급권1 + 전초재고(식량48/철16/통나무32) · 3공세", new ItemStack(Items.SHIELD), "", Action.SIEGE),
            new Entry("요새 방어전", "보급권2 + 전초재고(식량96/철32/석재벽돌128) · 벽+4공세", new ItemStack(Items.STONE_BRICK_WALL), "", Action.BASTION),
            new Entry("원정 작전", "보급권1 + 전초재고(식량32/철8/연료8) · 전진→작업→귀환", new ItemStack(Items.SPYGLASS), "", Action.OPERATION),
            new Entry("현장 복귀 계약", "활성 전초기지에서 보급권1 · 일반 사망 96블록 내 1회 복귀", new ItemStack(Items.COMPASS), "", Action.RECOVERY),
            new Entry("현장 보급 출고", "보급권1 → 금32 · 자수정16 · 메아리2", new ItemStack(Items.MINECART), "", Action.DISPATCH),
            new Entry("생산 현황", "생산·전초 현지재고·하역장·화물·방어·복귀·원정 상태", new ItemStack(Items.WRITABLE_BOOK), "", Action.STATUS),
            new Entry("뒤로", "인프라 메뉴로 돌아가기", new ItemStack(Items.ARROW), "", Action.BACK)
    };
    private static final int ITEM_COUNT=ENTRIES.length;
    private static final float OUTER_RADIUS=80.0F,INNER_RADIUS=60.0F;
    private static final double ICON_RADIUS=Math.sqrt(2.0D*61.5D*61.5D);

    public ProductionRadialMenuScreen(){super(Component.literal("Survival Ascension · 산업 생산망"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){}

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);if(Minecraft.getInstance().level==null)return;
        int cx=this.width/2,cy=this.height/2,selected=RadialMenuGeometry.selectedIndex(ITEM_COUNT);Matrix3x2f pose=new Matrix3x2f(graphics.pose());ScreenRectangle scissor=graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI,TextureSetup.noTexture(),pose,cx,cy,selected,scissor));
        for(int i=0;i<ITEM_COUNT;i++){double a=RadialMenuGeometry.iconRadians(i,ITEM_COUNT);graphics.item(ENTRIES[i].icon(),(int)Math.round(cx+ICON_RADIUS*Math.cos(a))-8,(int)Math.round(cy+ICON_RADIUS*Math.sin(a))-8);}
        Entry entry=ENTRIES[selected];graphics.text(this.font,entry.title(),cx-this.font.width(entry.title())/2,cy-5,0xFFFFFFFF,true);graphics.text(this.font,entry.detail(),cx-this.font.width(entry.detail())/2,cy+8,0xFFB8B8B8,false);
        String caption="채집 → 통/창고 → 도로·레일 → 일반/전선 화물 → 전초 현지재고 → 방어/원정";graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFE0E0E0,true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){if(event.button()!=0)return false;Entry entry=ENTRIES[RadialMenuGeometry.selectedIndex(ITEM_COUNT)];if(entry.action()==Action.BACK){this.minecraft.gui.setScreen(new InfrastructureRadialMenuScreen());return true;}String action=switch(entry.action()){case FUND->InfrastructureService.ACTION_FUND;case STATUS->ProductionService.ACTION_STATUS;case DISPATCH->ProductionService.ACTION_DISPATCH;case DEPOT->ProductionService.ACTION_DEPOT_TOGGLE;case WAREHOUSE->ProductionService.ACTION_WAREHOUSE_TOGGLE;case OFFLOAD->ProductionService.ACTION_BULK_OFFLOAD;case FREIGHT->ProductionService.ACTION_FREIGHT;case OUTPOST->ProductionService.ACTION_OUTPOST_UPGRADE;case SIEGE->ProductionService.ACTION_OUTPOST_SIEGE;case BASTION->ProductionService.ACTION_BASTION_SIEGE;case OPERATION->ProductionService.ACTION_FIELD_OPERATION;case RECOVERY->ProductionService.ACTION_FIELD_RECOVERY;case PRODUCE->ProductionService.ACTION_PREFIX+entry.programId();case BACK->"";};ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(InfrastructureProject.INDUSTRIAL_WORKS.id(),action));this.minecraft.gui.setScreen(null);return true;}

    private enum Action{FUND,PRODUCE,DEPOT,WAREHOUSE,OFFLOAD,FREIGHT,OUTPOST,SIEGE,BASTION,OPERATION,RECOVERY,DISPATCH,STATUS,BACK}
    private record Entry(String title,String detail,ItemStack icon,String programId,Action action){}
    private record WheelElement(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,int x,int y,int selected,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState{
        private WheelElement(RenderPipeline p,TextureSetup t,Matrix3x2f pose,int x,int y,int s,ScreenRectangle a){this(p,t,pose,x,y,s,a,boundsFor(x,y,pose,a));}
        @Override public void buildVertices(VertexConsumer v){for(int i=0;i<ITEM_COUNT;i++){double c=RadialMenuGeometry.sectorStartRadians(i,ITEM_COUNT),n=RadialMenuGeometry.sectorEndRadians(i,ITEM_COUNT);boolean h=i==selected;float in=((INNER_RADIUS-(h?2:0))/100)*130,out=((OUTER_RADIUS+(h?2:0))/100)*130;float p1ix=x+in*Mth.cos((float)c),p1iy=y+in*Mth.sin((float)c),p1ox=x+out*Mth.cos((float)c),p1oy=y+out*Mth.sin((float)c),p2ox=x+out*Mth.cos((float)n),p2oy=y+out*Mth.sin((float)n),p2ix=x+in*Mth.cos((float)n),p2iy=y+in*Mth.sin((float)n);int r=h?255:0;v.addVertexWith2DPose(pose,p1ox,p1oy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p1ix,p1iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ix,p2iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ox,p2oy).setColor(r,0,0,153);}}
        private static ScreenRectangle boundsFor(int x,int y,Matrix3x2f pose,ScreenRectangle s){ScreenRectangle w=new ScreenRectangle(x-110,y-110,220,220).transformMaxBounds(pose);return s!=null?s.intersection(w):w;}
    }
}