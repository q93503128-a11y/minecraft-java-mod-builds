package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.network.SettlementSnapshotPayload;
import kr.moonseungjun.frontiersettlement.settlement.BuildingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BuildingPaletteScreen extends Screen {
    private int panelX, panelY, panelWidth, panelHeight;
    public BuildingPaletteScreen(){super(Minecraft.getInstance(),Minecraft.getInstance().font,Component.literal("마을 건설"));}
    @Override protected void init(){
        panelWidth=Math.min(430,Math.max(320,this.width-24)); panelHeight=Math.min(252,Math.max(238,this.height-24)); panelX=(this.width-panelWidth)/2; panelY=Math.max(12,(this.height-panelHeight)/2);
        int gap=10,innerX=panelX+12,innerY=panelY+32,columnWidth=(panelWidth-34)/2,rightX=innerX+columnWidth+gap;
        addBuilding(BuildingType.HOUSE,innerX,innerY+12,columnWidth); addBuilding(BuildingType.WAREHOUSE,innerX,innerY+32,columnWidth);
        addBuilding(BuildingType.MARKET,innerX,innerY+68,columnWidth); addBuilding(BuildingType.CART_STATION,innerX,innerY+88,columnWidth);
        addBuilding(BuildingType.GUARD_POST,innerX,innerY+124,columnWidth); addBuilding(BuildingType.WATCHTOWER,innerX,innerY+144,columnWidth); addBuilding(BuildingType.BARRACKS,innerX,innerY+164,columnWidth);
        addBuilding(BuildingType.LUMBER_CAMP,rightX,innerY+12,columnWidth); addBuilding(BuildingType.FARM,rightX,innerY+32,columnWidth); addBuilding(BuildingType.QUARRY,rightX,innerY+52,columnWidth); addBuilding(BuildingType.MINE,rightX,innerY+72,columnWidth); addBuilding(BuildingType.BLACKSMITH,rightX,innerY+92,columnWidth); addBuilding(BuildingType.WORKSHOP,rightX,innerY+112,columnWidth); addBuilding(BuildingType.CONSTRUCTION_OFFICE,rightX,innerY+132,columnWidth); addBuilding(BuildingType.ADVANCED_WORKSHOP,rightX,innerY+152,columnWidth);
        int infraY=innerY+186,infraGap=6,infraWidth=(panelWidth-36)/3;
        addRenderableWidget(Button.builder(Component.literal("도로 계획"),b->{RoadPlacementClient.beginPlacement();this.minecraft.gui.setScreen(null);}).bounds(innerX,infraY,infraWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("전초기지"),b->{OutpostPlacementClient.beginPlacement();this.minecraft.gui.setScreen(null);}).bounds(innerX+infraWidth+infraGap,infraY,infraWidth,20).build());
        SettlementSnapshotPayload data=ClientSettlementState.snapshot();
        Button civil=Button.builder(Component.literal(data.tier().equals("영지")?"토목 평탄화":"토목 · 영지 잠김"),b->{CivilWorkPlacementClient.beginPlacement();this.minecraft.gui.setScreen(null);}).bounds(innerX+(infraWidth+infraGap)*2,infraY,infraWidth,20).build();
        civil.active=data.tier().equals("영지"); addRenderableWidget(civil);
        addRenderableWidget(Button.builder(Component.literal("닫기"),b->this.onClose()).bounds(panelX+panelWidth-58,panelY+7,46,20).build());
    }
    private void addBuilding(BuildingType type,int x,int y,int width){SettlementSnapshotPayload data=ClientSettlementState.snapshot();boolean unlocked=(data.buildingUnlockMask()&(1<<type.ordinal()))!=0,affordable=data.wood()>=type.woodCost()&&data.stone()>=type.stoneCost();String state=unlocked?(affordable?"":" · 자원부족"):" · 잠김";Button button=Button.builder(Component.literal(type.displayName()+"  목"+type.woodCost()+" 석"+type.stoneCost()+state),c->{BuildingPlacementClient.beginPlacement(type);this.minecraft.gui.setScreen(null);}).bounds(x,y,width,18).build();button.active=unlocked;addRenderableWidget(button);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float p){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float p){g.fill(panelX,panelY,panelX+panelWidth,panelY+panelHeight,0xD0101114);g.fill(panelX,panelY,panelX+3,panelY+panelHeight,0xFFD1A85A);g.text(this.font,Component.literal("마을 건설"),panelX+12,panelY+10,0xFFFFFFFF,true);int innerX=panelX+12,innerY=panelY+32,gap=10,columnWidth=(panelWidth-34)/2,rightX=innerX+columnWidth+gap;g.text(this.font,Component.literal("기반"),innerX,innerY,0xFFFFD58A,true);g.text(this.font,Component.literal("물류·교역"),innerX,innerY+56,0xFFFFD58A,true);g.text(this.font,Component.literal("방어"),innerX,innerY+112,0xFFFFD58A,true);g.text(this.font,Component.literal("생산·건설"),rightX,innerY,0xFFFFD58A,true);g.text(this.font,Component.literal("인프라"),innerX,innerY+174,0xFFFFD58A,true);super.extractRenderState(g,mx,my,p);}
    @Override public boolean isPauseScreen(){return false;} @Override public boolean isInGameUi(){return true;}
}
