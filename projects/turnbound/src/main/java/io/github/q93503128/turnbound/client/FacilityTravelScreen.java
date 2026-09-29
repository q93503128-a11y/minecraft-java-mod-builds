package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.FieldCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** Physical stable/waystation UI. Fast travel remains a world service rather than a global menu category. */
final class FacilityTravelScreen extends Screen {
    private int left,top,panelWidth,panelHeight;

    FacilityTravelScreen(){super(Component.literal("마구간"));}

    @Override protected void init(){
        super.init();
        panelWidth=Math.min(430,Math.max(300,width-60));
        panelHeight=Math.min(280,Math.max(210,height-60));
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int y=top+56;
        for(var travel:ClientFieldState.snapshot().travels()){
            if(!travel.unlocked())continue;
            var b=new BattleHudButton(left+16,y,panelWidth-32,21,
                    Component.literal(travel.label()+(travel.current()?" · 현재 위치":"")),
                    travel.current()?0xFF62D39A:0xFF6DC6FF,
                    ignored->{ClientPacketDistributor.sendToServer(new FieldCommandPayload("TRAVEL|"+travel.id()));onClose();});
            b.active=!travel.current();
            addRenderableWidget(b);
            y+=25;
            if(y>top+panelHeight-30)break;
        }
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor g,int x,int y,float p){}
    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){
        TurnboundFrameStyle.frame(g,left,top,panelWidth,panelHeight,0xFF6DC6FF);
        g.text(font,Component.literal("마구간"),left+16,top+14,0xFFF4F0E6,true);
        g.text(font,Component.literal("직접 발견한 거점으로 이동"),left+16,top+32,0xFFAEB7C6,false);
        if(ClientFieldState.snapshot().travels().stream().noneMatch(t->t.unlocked())){
            g.text(font,Component.literal("아직 발견한 이동 거점이 없습니다."),left+16,top+62,0xFF87909E,false);
        }
        super.extractRenderState(g,mx,my,pt);
    }
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==GLFW.GLFW_KEY_E||e.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(e);}
    @Override public void onClose(){FacilityUiAccess.clear();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
