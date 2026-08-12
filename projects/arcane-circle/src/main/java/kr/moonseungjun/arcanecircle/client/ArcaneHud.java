package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;
import java.util.Locale;

/** Minimal combat ribbon: spell seals first, numbers only when they matter. */
public final class ArcaneHud {
    private static final Identifier LAYER_ID=Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID,"spell_ribbon");
    private ArcaneHud(){}

    public static void registerLayers(RegisterGuiLayersEvent event){event.registerAboveAll(LAYER_ID,ArcaneHud::renderWorldHud);}

    private static void renderWorldHud(GuiGraphicsExtractor g,DeltaTracker deltaTracker){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null||!ArcaneClientState.ready())return;
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();Font font=mc.font;
        int radius=w>=520?12:w>=360?11:10,gap=w>=520?13:10,total=(radius*2)*5+gap*4,start=w/2-total/2,y=h-70;
        drawVitals(g,font,w,h,start,total);
        for(int i=0;i<5;i++)drawSeal(g,font,start+i*(radius*2+gap)+radius,y,radius,i);
        drawFusion(g,font,w,y-radius-18);drawNotice(g,font,w,y-radius-41);
    }

    public static void onVanillaLayer(RenderGuiLayerEvent.Pre event){Minecraft mc=Minecraft.getInstance();if(mc.player!=null&&ArcaneClientState.ready()&&VanillaGuiLayers.PLAYER_HEALTH.equals(event.getName()))event.setCanceled(true);}

    private static void drawSeal(GuiGraphicsExtractor g,Font font,int cx,int cy,int r,int slot){
        SpellDefinition s=SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);boolean charging=ArcaneClientState.isChargingSlot(slot);int cd=ArcaneClientState.cooldownRemainingTicks(slot);int color=s==null?0xFF55565B:ArcaneRenderUtil.schoolColor(s.school());
        if(charging){ArcaneRenderUtil.ring(g,cx,cy,r+3,ArcaneClientState.chargingReady()?0xFFFFD77A:color);ArcaneRenderUtil.diamond(g,cx,cy,r+1,0x32FFD77A);}else ArcaneRenderUtil.ring(g,cx,cy,r+1,cd>0?0xFF4E4E53:color);
        ArcaneRenderUtil.ring(g,cx,cy,r-3,cd>0?0xFF55565B:0xFFBDAE96);
        if(s!=null)ArcaneRenderUtil.spellRune(g,cx,cy,s,Math.max(4,r-6),cd>0?0xFF747478:0xFFF5E8D5);else ArcaneRenderUtil.diamond(g,cx,cy,3,0xFF6E6B65);
        tiny(g,font,Integer.toString(slot+1),cx-r-3,cy-r-5,0xFF8C857B,.50F,false);
        if(cd>0){String time=cd>=200?Integer.toString((int)Math.ceil(cd/20.0)):String.format(Locale.ROOT,"%.1f",cd/20.0);tiny(g,font,time,cx,cy-3,0xFFF2E7D8,.58F,true);}
        if(charging){double f=ArcaneClientState.chargingRequiredTicks()<=0?1:ArcaneClientState.chargingFraction();int line=(int)Math.round((r*2+4)*f);g.fill(cx-r-2,cy+r+5,cx-r-2+line,cy+r+7,ArcaneClientState.chargingReady()?0xFFFFD77A:color);}
        if(s!=null&&charging)tiny(g,font,fit(font,s.name(),100),cx,cy-r-15,0xFFEADFCF,.58F,true);
    }

    private static void drawVitals(GuiGraphicsExtractor g,Font font,int width,int height,int start,int total){
        int health=Math.max(0,ArcaneClientState.integer("health",0)),hpMax=Math.max(1,ArcaneClientState.integer("health_max",100)),abs=Math.max(0,ArcaneClientState.integer("absorption",0));
        int mana=Math.max(0,ArcaneClientState.integer("mana",0)),manaMax=Math.max(1,ArcaneClientState.integer("max",100));
        int rail=Math.min(78,Math.max(46,start-18)),left=Math.max(5,start-rail-14),right=Math.min(width-5,start+total+14);
        double hp=Math.min(1,health/(double)hpMax),mp=Math.min(1,mana/(double)manaMax);int y=height-72;
        g.fill(left,y,left+rail,y+2,0xFF3A2929);g.fill(left,y,left+(int)(rail*hp),y+2,hp<.25?0xFFFF5361:0xFFD95A61);if(abs>0)g.fill(left,y-2,left+(int)(rail*Math.min(1,abs/(double)hpMax)),y-1,0xFFFFD060);
        tiny(g,font,health+" / "+hpMax,left,y-10,0xFFCDB9B5,.54F,false);
        int mx=Math.max(start+total+14,right);int manaX=Math.min(width-rail-5,mx);g.fill(manaX,y,manaX+rail,y+2,0xFF242D3A);g.fill(manaX,y,manaX+(int)(rail*mp),y+2,0xFF6CA2E7);tiny(g,font,ArcaneClientState.integer("circle",1)+"C · "+mana+" / "+manaMax,manaX+rail,y-10,0xFFB9C9DD,.54F,true);
    }

    private static void drawFusion(GuiGraphicsExtractor g,Font font,int width,int y){List<String> q=ArcaneClientState.queue();if(q.isEmpty())return;int cx=width/2,x=cx-(q.size()*22)/2;for(int i=0;i<q.size();i++){SpellDefinition s=SpellCatalog.spell(q.get(i)).orElse(null);int color=s==null?0xFF77747A:ArcaneRenderUtil.schoolColor(s.school());ArcaneRenderUtil.ring(g,x+i*22,y,7,color);if(s!=null)ArcaneRenderUtil.spellRune(g,x+i*22,y,s,4,0xFFF3E6D4);if(i<q.size()-1)g.fill(x+i*22+8,y,x+(i+1)*22-8,y+1,0xFF8D806E);}String result=ArcaneClientState.queueResult();if(!result.isBlank()){SpellDefinition s=SpellCatalog.spell(result).orElse(null);tiny(g,font,"→ "+(s==null?result:s.name()),x+q.size()*22+3,y-3,ArcaneClientState.fusionChargingReady()?0xFFFFD57B:0xFFC7B18D,.58F,false);}double p=ArcaneClientState.fusionChargingFraction();if(p>0)g.fill(cx-55,y+12,cx-55+(int)(110*p),y+14,ArcaneClientState.fusionChargingReady()?0xFFFFD57B:0xFF8B71B2);}

    private static void drawNotice(GuiGraphicsExtractor g,Font font,int width,int y){if(!ArcaneClientState.noticeVisible())return;String s=fit(font,ArcaneClientState.noticeText(),width-40);int text=font.width(s),x=width/2-text/2;g.fill(x-8,y-2,x-6,y+11,0xFFD0AD6A);g.text(font,Component.literal(s),x,y,0xFFE8DDCE);}

    public static void onScreenRender(ScreenEvent.Render.Post event){
        if(!(event.getScreen() instanceof InventoryScreen)||!ArcaneClientState.ready())return;Minecraft mc=Minecraft.getInstance();GuiGraphicsExtractor g=event.getGuiGraphics();Font f=mc.font;int width=mc.getWindow().getGuiScaledWidth(),height=mc.getWindow().getGuiScaledHeight(),invLeft=width/2-88,invRight=width/2+88,side=Math.max(invLeft,width-invRight);if(side<128)return;int panelW=Math.min(146,side-10),x=invLeft-panelW-6;if(x<4)x=invRight+6;int y=Math.max(8,(height-98)/2);g.fill(x,y,x+2,y+91,0xFFD0AD6A);tiny(g,f,"마력핵",x+9,y+1,0xFFE4D7C4,.66F,false);tiny(g,f,ArcaneClientState.integer("circle",1)+"C · MP "+ArcaneClientState.integer("mana",0)+" / "+ArcaneClientState.integer("max",100),x+9,y+18,0xFFBFD0E5,.58F,false);tiny(g,f,"회복 "+String.format(Locale.ROOT,"%.1f",ArcaneClientState.regenPerSecond())+" /초",x+9,y+31,0xFF9AC7B0,.56F,false);tiny(g,f,fit(f,ArcaneClientState.text("staff","맨손"),panelW*2),x+9,y+47,0xFFD6B879,.54F,false);tiny(g,f,"C 마도서 · 1~5 시전",x+9,y+69,0xFF777067,.52F,false);tiny(g,f,"숫자키 동시 입력 · 융합",x+9,y+80,0xFF777067,.52F,false);
    }

    private static void tiny(GuiGraphicsExtractor g,Font font,String text,int x,int y,int color,float scale,boolean centered){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);if(centered)g.centeredText(font,Component.literal(text),0,0,color);else g.text(font,Component.literal(text),0,0,color);g.pose().popMatrix();}
    private static String fit(Font font,String v,int pixels){if(v==null||pixels<=0)return"";if(font.width(v)<=pixels)return v;String e="…";int allowed=Math.max(0,pixels-font.width(e)),end=v.length();while(end>0&&font.width(v.substring(0,end))>allowed)end--;return end<=0?e:v.substring(0,end)+e;}
}
