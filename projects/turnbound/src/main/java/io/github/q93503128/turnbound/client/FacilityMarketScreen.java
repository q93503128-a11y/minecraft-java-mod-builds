package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

/** Physical equipment merchant. Buying and selling are not exposed by the global management menu. */
final class FacilityMarketScreen extends Screen {
    private int left,top,panelWidth,panelHeight;

    FacilityMarketScreen(){super(Component.literal("장비 상점"));}
    void refreshSnapshot(){clearWidgets();init();}

    @Override protected void init(){
        super.init();
        panelWidth=Math.min(560,Math.max(380,width-48));
        panelHeight=Math.min(340,Math.max(250,height-48));
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int gap=10,colW=(panelWidth-32-gap)/2,leftX=left+16,rightX=leftX+colW+gap,y=top+62;

        int i=0;
        for(var row:ClientMetaState.snapshot().shopItems()){
            if(i>=9)break;
            String label=row.tier()+" · "+row.name()+" · "+row.price()+"G";
            var b=new BattleHudButton(leftX,y+i*24,colW,20,Component.literal(label),row.unlocked()?0xFFFFC857:0xFF707987,
                    ignored->send("BUY|"+row.itemId()));
            b.active=row.unlocked()&&ClientMetaState.snapshot().gold()>=row.price();addRenderableWidget(b);i++;
        }

        i=0;
        var sell=ClientMetaState.snapshot().equipment().stream().filter(ClientMetaState.EquipmentRow::sellable)
                .sorted(Comparator.comparingInt(ClientMetaState.EquipmentRow::salePrice).reversed()).toList();
        for(var row:sell){
            if(i>=9)break;
            addRenderableWidget(new BattleHudButton(rightX,y+i*24,colW,20,
                    Component.literal(row.name()+" · "+row.salePrice()+"G"),0xFF62D39A,
                    ignored->send("SELL|"+row.instanceId())));
            i++;
        }
    }

    private void send(String raw){ClientPacketDistributor.sendToServer(new MetaCommandPayload(raw));}

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor g,int x,int y,float p){}
    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){
        TurnboundFrameStyle.frame(g,left,top,panelWidth,panelHeight,0xFFFFC857);
        g.text(font,Component.literal("장비 상점"),left+16,top+13,0xFFF4F0E6,true);
        g.text(font,Component.literal("보유 골드 "+ClientMetaState.snapshot().gold()),left+16,top+31,0xFFFFC857,false);
        int gap=10,colW=(panelWidth-32-gap)/2,leftX=left+16,rightX=leftX+colW+gap;
        g.text(font,Component.literal("구매"),leftX,top+48,0xFFAEB7C6,true);
        g.text(font,Component.literal("판매"),rightX,top+48,0xFFAEB7C6,true);
        super.extractRenderState(g,mx,my,pt);
    }
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==GLFW.GLFW_KEY_E||e.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(e);}
    @Override public void onClose(){FacilityUiAccess.clear();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
