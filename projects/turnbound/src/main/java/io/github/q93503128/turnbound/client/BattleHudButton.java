package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Shared TURNBOUND button with hard text-fitting and stable cooldown presentation. */
final class BattleHudButton extends Button {
    private final int accent;
    private boolean selected;

    BattleHudButton(int x,int y,int width,int height,Component message,int accent,OnPress onPress){
        super(x,y,width,height,TurnboundUiText.playerFacingLabel(message),onPress,DEFAULT_NARRATION);
        this.accent=accent;
    }

    void setSelected(boolean selected){this.selected=selected;}
    @Override public void setMessage(Component message){super.setMessage(TurnboundUiText.playerFacingLabel(message));}

    @Override protected void extractContents(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        TurnboundUiSkin.button(graphics,getX(),getY(),getWidth(),getHeight(),active,isHoveredOrFocused(),selected,accent);
        String raw=getMessage().getString();if(drawStableSkillLabel(graphics,raw))return;
        var font=Minecraft.getInstance().font;String label=UiTextLayout.fit(raw,Math.max(8,getWidth()-12));int tx=getX()+Math.max(5,(getWidth()-font.width(label))/2),ty=getY()+Math.max(2,(getHeight()-font.lineHeight)/2);
        graphics.text(font,Component.literal(label),tx,ty,active?0xFFF4F0E6:0xFFA5A5A5,true);
    }

    private boolean drawStableSkillLabel(GuiGraphicsExtractor graphics,String raw){
        if(raw==null||raw.length()<4||raw.charAt(0)<'1'||raw.charAt(0)>'5'||raw.charAt(1)!=' '||raw.charAt(2)!=' ')return false;
        var font=Minecraft.getInstance().font;String body=raw.substring(3).trim(),cooldown="";int split=body.lastIndexOf("  ");
        if(split>0){String tail=body.substring(split+2).trim();boolean digits=!tail.isEmpty();for(int i=0;i<tail.length();i++)digits&=Character.isDigit(tail.charAt(i));if(digits){cooldown=tail;body=body.substring(0,split).trim();}}
        int ty=getY()+Math.max(2,(getHeight()-font.lineHeight)/2);graphics.text(font,Component.literal(String.valueOf(raw.charAt(0))),getX()+7,ty,0xFFF4F0E6,true);int nameX=getX()+22;int chipW=cooldown.isEmpty()?0:Math.min(34,Math.max(24,font.width("CD "+cooldown)+8));String name=UiTextLayout.fit(body,Math.max(10,getWidth()-(nameX-getX())-chipW-7));graphics.text(font,Component.literal(name),nameX,ty,active?0xFFF4F0E6:0xFFAEB7C6,false);
        if(!cooldown.isEmpty()){int chipX=getX()+getWidth()-chipW-4;graphics.fill(chipX,getY()+4,chipX+chipW,getY()+getHeight()-4,0xD01A1E25);String cd="CD "+cooldown;graphics.text(font,Component.literal(cd),chipX+(chipW-font.width(cd))/2,ty,0xFFFFC857,true);}return true;
    }
}
