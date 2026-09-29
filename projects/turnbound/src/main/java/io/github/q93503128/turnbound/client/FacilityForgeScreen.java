package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/** Physical New Drabyel blacksmith. Enhancement lives here, not in the global equipment-management menu. */
final class FacilityForgeScreen extends Screen {
    private int left,top,panelWidth,panelHeight,page;
    private String selected="";

    FacilityForgeScreen(){super(Component.literal("대장간"));}

    void refreshSnapshot(){clearWidgets();init();}

    @Override protected void init(){
        super.init();
        panelWidth=Math.min(520,Math.max(340,width-48));
        panelHeight=Math.min(330,Math.max(240,height-48));
        left=(width-panelWidth)/2; top=(height-panelHeight)/2;

        List<ClientMetaState.EquipmentRow> rows=ClientMetaState.snapshot().equipment().stream()
                .sorted(Comparator.comparingInt(ClientMetaState.EquipmentRow::enhancement).reversed()
                        .thenComparing(ClientMetaState.EquipmentRow::name)).toList();
        if(!selected.isBlank()&&rows.stream().noneMatch(r->r.instanceId().equals(selected)))selected="";
        if(selected.isBlank()&&!rows.isEmpty())selected=rows.getFirst().instanceId();

        int listW=Math.min(250,panelWidth*52/100),rowH=20,listTop=top+56;
        int per=Math.max(4,(panelHeight-92)/(rowH+3));
        int pages=Math.max(1,(rows.size()+per-1)/per);
        page=Math.max(0,Math.min(page,pages-1));
        int start=page*per,end=Math.min(rows.size(),start+per);
        for(int i=start;i<end;i++){
            var row=rows.get(i); int yy=listTop+(i-start)*(rowH+3);
            String label=row.tier()+" · "+row.name()+" +"+row.enhancement();
            addRenderableWidget(new BattleHudButton(left+14,yy,listW-20,rowH,Component.literal(label),
                    row.instanceId().equals(selected)?0xFF6DC6FF:0xFFAEB7C6,
                    ignored->{selected=row.instanceId();clearWidgets();init();}));
        }

        var row=rows.stream().filter(r->r.instanceId().equals(selected)).findFirst().orElse(null);
        if(row!=null){
            int rx=left+listW+8,rw=panelWidth-listW-22;
            boolean maxed=row.enhancement()>=GrowthRulesV1.maxEnhancement();
            int cost=maxed?0:GrowthRulesV1.enhancementCost(row.tier(),row.enhancement());
            var enhance=new BattleHudButton(rx,top+118,rw,22,
                    Component.literal(maxed?"강화 완료":"+ "+(row.enhancement()+1)+" 강화 · "+cost+"G"),
                    maxed?0xFF707987:0xFFFFC857,
                    ignored->ClientPacketDistributor.sendToServer(new MetaCommandPayload("ENHANCE|"+row.instanceId())));
            enhance.active=!maxed&&ClientMetaState.snapshot().gold()>=cost;
            addRenderableWidget(enhance);
        }

        if(page>0)addRenderableWidget(new BattleHudButton(left+14,top+panelHeight-27,54,17,Component.literal("<"),0xFF707987,ignored->{page--;clearWidgets();init();}));
        if(page+1<pages)addRenderableWidget(new BattleHudButton(left+72,top+panelHeight-27,54,17,Component.literal(">"),0xFF707987,ignored->{page++;clearWidgets();init();}));
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor g,int x,int y,float p){}

    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){
        TurnboundFrameStyle.frame(g,left,top,panelWidth,panelHeight,0xFFFFC857);
        g.text(font,Component.literal("대장간"),left+14,top+13,0xFFF4F0E6,true);
        g.text(font,Component.literal("보유 골드 "+ClientMetaState.snapshot().gold()),left+14,top+30,0xFFFFC857,false);
        var row=ClientMetaState.snapshot().equipment().stream().filter(r->r.instanceId().equals(selected)).findFirst().orElse(null);
        if(row==null){
            g.text(font,Component.literal("강화할 장비가 없습니다."),left+14,top+62,0xFF87909E,false);
        }else{
            int listW=Math.min(250,panelWidth*52/100),rx=left+listW+8,rw=panelWidth-listW-22;
            g.text(font,Component.literal(UiTextLayout.fit(row.name(),rw)),rx,top+59,0xFFF4F0E6,true);
            g.text(font,Component.literal(row.tier()+" · +"+row.enhancement()),rx,top+76,0xFFAEB7C6,false);
            g.text(font,Component.literal(UiTextLayout.fit(row.mainType()+" "+row.mainValue(),rw)),rx,top+93,0xFF62D39A,false);
        }
        super.extractRenderState(g,mx,my,pt);
    }

    @Override public boolean keyPressed(KeyEvent e){if(e.key()==GLFW.GLFW_KEY_E||e.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(e);}
    @Override public void onClose(){FacilityUiAccess.clear();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
