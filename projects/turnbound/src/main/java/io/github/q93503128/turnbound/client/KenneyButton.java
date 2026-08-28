package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;

final class KenneyButton extends Button {
    private static final WidgetSprites LONG=new WidgetSprites(Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"turnbound/button_long_blue"),Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"turnbound/button_long_blue_pressed"));
    KenneyButton(int x,int y,int w,int h,Component msg,OnPress press){super(x,y,w,h,msg,press,DEFAULT_NARRATION);}
    @Override protected void extractContents(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){g.blitSprite(RenderPipelines.GUI_TEXTURED,LONG.get(this.active,this.isHoveredOrFocused()),getX(),getY(),getWidth(),getHeight(),ARGB.white(this.alpha));extractDefaultLabel(g.textRendererForWidget(this,GuiGraphicsExtractor.HoveredTextEffects.NONE));}
}
