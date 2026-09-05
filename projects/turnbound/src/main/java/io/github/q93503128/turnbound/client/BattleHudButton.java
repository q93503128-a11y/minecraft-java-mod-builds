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
        String raw=getMessage().getString();
        if(isStableSkillLabel(raw)){
            TurnboundUiSkin.battleSkillButton(graphics,getX(),getY(),getWidth(),getHeight(),active,isHoveredOrFocused(),selected);
            drawStableSkillLabel(graphics,raw);
            return;
        }
        TurnboundUiSkin.button(graphics,getX(),getY(),getWidth(),getHeight(),active,isHoveredOrFocused(),selected,accent);
        var font=Minecraft.getInstance().font;
        String label=UiTextLayout.fit(raw,Math.max(8,getWidth()-12));
        int tx=getX()+Math.max(5,(getWidth()-font.width(label))/2),ty=getY()+Math.max(2,(getHeight()-font.lineHeight)/2);
        graphics.text(font,Component.literal(label),tx,ty,active?TurnboundUiTokens.TEXT_PRIMARY:0xFFA5A5A5,true);
    }

    private static boolean isStableSkillLabel(String raw){
        return raw!=null&&raw.length()>=4&&raw.charAt(0)>='1'&&raw.charAt(0)<='5'&&raw.charAt(1)==' '&&raw.charAt(2)==' ';
    }

    private void drawStableSkillLabel(GuiGraphicsExtractor graphics,String raw){
        var font=Minecraft.getInstance().font;
        String body=raw.substring(3).trim(),cooldown="";
        int split=body.lastIndexOf("  ");
        if(split>0){
            String tail=body.substring(split+2).trim();
            boolean digits=!tail.isEmpty();
            for(int i=0;i<tail.length();i++)digits&=Character.isDigit(tail.charAt(i));
            if(digits){cooldown=tail;body=body.substring(0,split).trim();}
        }

        int ty=getY()+Math.max(2,(getHeight()-font.lineHeight)/2);
        int numberX=getX()+8;
        int separatorX=getX()+22;
        int numberColor=!active?0xFF8A9098:selected?0xFFA2FFB0:0xFFD9DEE5;
        graphics.text(font,Component.literal(String.valueOf(raw.charAt(0))),numberX,ty,numberColor,true);
        graphics.fill(separatorX,getY()+5,separatorX+1,getY()+getHeight()-5,
                selected&&active?0xAA76E58A:0x665B6570);

        int chipW=cooldown.isEmpty()?0:Math.min(38,Math.max(26,font.width("CD "+cooldown)+8));
        int nameX=separatorX+7;
        String name=UiTextLayout.fit(body,Math.max(10,getWidth()-(nameX-getX())-chipW-7));
        int nameColor=!active?0xFF969CA5:0xFFF6F7F8;
        graphics.text(font,Component.literal(name),nameX,ty,nameColor,true);

        if(!cooldown.isEmpty()){
            int chipX=getX()+getWidth()-chipW-5;
            graphics.fill(chipX,getY()+5,chipX+chipW,getY()+getHeight()-5,0xD021242A);
            graphics.fill(chipX,getY()+5,chipX+2,getY()+getHeight()-5,0xFFB78A36);
            String cd="CD "+cooldown;
            graphics.text(font,Component.literal(cd),chipX+Math.max(3,(chipW-font.width(cd))/2),ty,0xFFFFD27A,true);
        }
    }
}
