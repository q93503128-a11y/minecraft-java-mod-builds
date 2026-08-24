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
            new Entry("승천 각인", new ItemStack(Items.AMETHYST_SHARD), Action.IMPRINT),
            new Entry("재련", new ItemStack(Items.ANVIL), Action.REFORGE),
            new Entry("신화 각성", new ItemStack(Items.NETHER_STAR), Action.AWAKEN),
            new Entry("분해", new ItemStack(Items.GRINDSTONE), Action.SALVAGE),
            new Entry("장비 정보", new ItemStack(Items.SPYGLASS), Action.INFO),
            new Entry("뒤로", new ItemStack(Items.ARROW), Action.BACK)
    };
    private static final int ITEM_COUNT = ENTRIES.length;
    private static final float OUTER_RADIUS = 80.0F;
    private static final float INNER_RADIUS = 60.0F;
    private static final double ICON_RADIUS = Math.sqrt(2.0D * 61.5D * 61.5D);

    public EquipmentRadialMenuScreen() { super(Component.literal("Survival Ascension · 장비")); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.level==null||minecraft.player==null)return;
        int cx=this.width/2,cy=this.height/2,selected=RadialMenuGeometry.selectedIndex(ITEM_COUNT);
        Matrix3x2f pose=new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor=graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI,TextureSetup.noTexture(),pose,cx,cy,selected,scissor));
        for(int i=0;i<ITEM_COUNT;i++){double a=RadialMenuGeometry.iconRadians(i,ITEM_COUNT);graphics.item(ENTRIES[i].icon(),(int)Math.round(cx+ICON_RADIUS*Math.cos(a))-8,(int)Math.round(cy+ICON_RADIUS*Math.sin(a))-8);}
        ItemStack held=minecraft.player.getMainHandItem();
        int rarity=AscensionAffixes.rarity(held);
        boolean imprintable=AscensionAffixes.canImprint(held);
        Entry entry=ENTRIES[selected];
        String title=entry.title(),detail=detail(entry.action(),held,rarity,imprintable);
        graphics.text(this.font,title,cx-this.font.width(title)/2,cy-5,0xFFFFFFFF,true);
        int detailColor=(rarity>0||entry.action()==Action.IMPRINT&&imprintable||entry.action()==Action.BACK)?0xFFE0E0E0:0xFFFF7777;
        graphics.text(this.font,detail,cx-this.font.width(detail)/2,cy+8,detailColor,false);
        if(entry.action()==Action.AWAKEN&&rarity==3&&!AscensionAffixes.isAwakened(held)){
            String top="자수정256 · 다이아24 · 파편8",bottom="메아리64 · 드래곤숨결16";
            graphics.text(this.font,top,cx-this.font.width(top)/2,cy+20,0xFFD7B4FF,false);
            graphics.text(this.font,bottom,cx-this.font.width(bottom)/2,cy+31,0xFFD7B4FF,false);
        }
        String caption=rarity>0?AscensionAffixes.rarityName(held)+" · "+AscensionAffixes.affixSummary(held)
                :(imprintable?"각인 가능 · "+AscensionAffixes.imprintCategoryName(held):"주 손에 장비를 드세요");
        graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFFFD37A,true);
    }

    private static String detail(Action action,ItemStack held,int rarity,boolean imprintable){
        if(action==Action.BACK)return "통합 메뉴로 돌아가기";
        if(action==Action.IMPRINT){
            if(rarity>0)return "이미 Survival Ascension affix 장비입니다";
            if(!imprintable)return "검/메이스/활/쇠뇌/곡괭이/도끼/삽/괭이/방어구/방패 표준 태그 장비 필요";
            return EquipmentReforgeService.imprintRangeText();
        }
        if(rarity<=0)return "정예 / 승천 / 신화 장비 필요";
        return switch(action){case REFORGE->"비용 · "+EquipmentReforgeService.costText(held);case AWAKEN->rarity<3?"신화 III 장비 필요":(AscensionAffixes.isAwakened(held)?"이미 각성 완료":"4번째 affix 개방");case SALVAGE->"환급 · "+EquipmentReforgeService.salvageText(rarity);case INFO->AscensionAffixes.affixSummary(held);case IMPRINT->"";case BACK->"통합 메뉴로 돌아가기";};
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()!=0)return false;
        Action action=ENTRIES[RadialMenuGeometry.selectedIndex(ITEM_COUNT)].action();
        if(action==Action.BACK){this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());return true;}
        if(action==Action.INFO)return true;
        if(this.minecraft.player==null)return true;
        ItemStack held=this.minecraft.player.getMainHandItem();
        if(action==Action.IMPRINT){
            if(!AscensionAffixes.canImprint(held))return true;
            ClientPacketDistributor.sendToServer(new EquipmentActionPayload(EquipmentReforgeService.ACTION_IMPRINT));
            this.minecraft.gui.setScreen(null);return true;
        }
        if(!AscensionAffixes.isAffixGear(held))return true;
        int id=switch(action){case REFORGE->EquipmentReforgeService.ACTION_REFORGE;case AWAKEN->EquipmentReforgeService.ACTION_AWAKEN;case SALVAGE->EquipmentReforgeService.ACTION_SALVAGE;default->-1;};
        if(id>=0)ClientPacketDistributor.sendToServer(new EquipmentActionPayload(id));
        this.minecraft.gui.setScreen(null);return true;
    }

    private record Entry(String title,ItemStack icon,Action action){}
    private enum Action{IMPRINT,REFORGE,AWAKEN,SALVAGE,INFO,BACK}
    private record WheelElement(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,int x,int y,int selected,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState{
        private WheelElement(RenderPipeline p,TextureSetup t,Matrix3x2f pose,int x,int y,int s,ScreenRectangle a){this(p,t,pose,x,y,s,a,boundsFor(x,y,pose,a));}
        @Override public void buildVertices(VertexConsumer v){for(int i=0;i<ITEM_COUNT;i++){double c=RadialMenuGeometry.sectorStartRadians(i,ITEM_COUNT),n=RadialMenuGeometry.sectorEndRadians(i,ITEM_COUNT);boolean h=i==selected;float in=((INNER_RADIUS-(h?2:0))/100)*130,out=((OUTER_RADIUS+(h?2:0))/100)*130;float p1ix=x+in*Mth.cos((float)c),p1iy=y+in*Mth.sin((float)c),p1ox=x+out*Mth.cos((float)c),p1oy=y+out*Mth.sin((float)c),p2ox=x+out*Mth.cos((float)n),p2oy=y+out*Mth.sin((float)n),p2ix=x+in*Mth.cos((float)n),p2iy=y+in*Mth.sin((float)n);int r=h?255:0;v.addVertexWith2DPose(pose,p1ox,p1oy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p1ix,p1iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ix,p2iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ox,p2oy).setColor(r,0,0,153);}}
        private static ScreenRectangle boundsFor(int x,int y,Matrix3x2f pose,ScreenRectangle s){ScreenRectangle w=new ScreenRectangle(x-110,y-110,220,220).transformMaxBounds(pose);return s!=null?s.intersection(w):w;}
    }
}
