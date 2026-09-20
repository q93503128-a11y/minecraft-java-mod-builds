package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** World-space battle resource/buff/debuff badges projected immediately above actors. */
public final class BattleStatusLayer implements GuiLayer {
    @Override public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker){
        ClientBattleState.Snapshot snapshot=ClientBattleState.snapshot();
        if(!snapshot.active()||snapshot.finished())return;
        Minecraft minecraft=Minecraft.getInstance();if(minecraft.player==null)return;
        for(ClientBattleState.Unit unit:snapshot.units()){
            if(unit.downed())continue;
            List<BattleStatusPresentation.Badge> badges=BattleStatusPresentation.badges(unit);if(badges.isEmpty())continue;
            BattleLiveProjection.ScreenPoint p=BattleLiveProjection.project(unit.x(),unit.y()+2.25,unit.z(),graphics.guiWidth(),graphics.guiHeight());
            if(p==null)continue;
            String text=badges.stream().limit(3).map(BattleStatusPresentation.Badge::text).reduce((a,b)->a+" · "+b).orElse("");
            int w=Math.min(156,minecraft.font.width(text)+10),x=(int)Math.round(p.x()-w/2.0),y=(int)Math.round(p.y()-5);
            x=Math.max(3,Math.min(graphics.guiWidth()-w-3,x));y=Math.max(30,Math.min(graphics.guiHeight()-14,y));
            int color=badges.getFirst().color();
            graphics.fill(x,y,x+w,y+12,TurnboundUiTokens.INSET);
            graphics.fill(x,y,x+2,y+12,color);
            graphics.text(minecraft.font,Component.literal(UiTextLayout.fit(text,w-8)),x+5,y+2,color,true);
        }
    }
}
