package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import io.github.q93503128.turnbound.progression.StarEssenceExchangeRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

/** Physical summon shrine. Summon/economy actions are intentionally absent from the global management menu. */
final class FacilitySummonScreen extends Screen {
    private int left,top,panelWidth,panelHeight;

    FacilitySummonScreen(){super(Component.literal("정령의 흔적"));}

    void refreshSnapshot(){clearWidgets();init();}

    @Override protected void init(){
        super.init();
        panelWidth=Math.min(520,Math.max(350,width-48));
        panelHeight=Math.min(340,Math.max(250,height-48));
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        var s=ClientMetaState.snapshot();

        int x=left+16,y=top+58,gap=4,w=(panelWidth-32-gap*2)/3;
        var one=new BattleHudButton(x,y,w,21,Component.literal("1회 · 300"),0xFF6DC6FF,ignored->send("SUMMON1"));
        one.active=s.crystal()>=GachaCatalog.SINGLE_COST;addRenderableWidget(one);
        var ten=new BattleHudButton(x+w+gap,y,w,21,Component.literal("10회 · 3000"),0xFFFFC857,ignored->send("SUMMON10"));
        ten.active=s.crystal()>=GachaCatalog.TEN_COST;addRenderableWidget(ten);
        var starter=new BattleHudButton(x+(w+gap)*2,y,w,21,Component.literal("초기 10회"),0xFF62D39A,ignored->send("STARTER"));
        starter.active=s.starterArchiveAvailable()&&s.crystal()>=GachaCatalog.TEN_COST;addRenderableWidget(starter);

        int exchangeY=y+29;
        var crystal=new BattleHudButton(x,exchangeY,panelWidth-32,20,
                Component.literal("별의 정수 150 → 크리스탈 300"),0xFFC794FF,ignored->send("ESSENCE_CRYSTAL"));
        crystal.active=s.essence()>=StarEssenceExchangeRules.CRYSTAL_COST;addRenderableWidget(crystal);

        var choices=s.characters().stream().filter(ClientMetaState.CharacterRow::owned)
                .filter(r->r.nativeStar()==4||r.nativeStar()==5)
                .sorted(Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed()
                        .thenComparing(ClientMetaState.CharacterRow::name)).toList();
        int gridY=exchangeY+30,cols=3,cardGap=4,cardW=(panelWidth-32-cardGap*(cols-1))/cols;
        for(int i=0;i<Math.min(choices.size(),9);i++){
            var row=choices.get(i); int cost=StarEssenceExchangeRules.choiceCost(row.nativeStar());
            int xx=x+(i%cols)*(cardW+cardGap),yy=gridY+(i/cols)*23;
            String bonus=row.bonusLevel()>=GrowthRulesV1.duplicateBonusMax()?"MAX":"+"+row.bonusLevel();
            var b=new BattleHudButton(xx,yy,cardW,19,
                    Component.literal("★"+row.nativeStar()+" "+row.name()+" · "+bonus+" · "+cost),
                    row.nativeStar()==5?0xFFFFC857:0xFFC794FF,
                    ignored->send("ESSENCE_PICK"+row.nativeStar()+"|"+row.id()));
            b.active=s.essence()>=cost;addRenderableWidget(b);
        }
    }

    private void send(String raw){ClientPacketDistributor.sendToServer(new MetaCommandPayload(raw));}

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor g,int x,int y,float p){}
    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){
        var s=ClientMetaState.snapshot();
        TurnboundFrameStyle.frame(g,left,top,panelWidth,panelHeight,0xFFC794FF);
        g.text(font,Component.literal("정령의 흔적"),left+16,top+13,0xFFF4F0E6,true);
        g.text(font,Component.literal("크리스탈 "+s.crystal()+" · 별의 정수 "+s.essence()),left+16,top+31,0xFFFFC857,false);
        g.text(font,Component.literal("★5 천장 "+s.fiveStarPity()+" / "+GachaCatalog.HARD_PITY),left+panelWidth-170,top+31,0xFFAEB7C6,false);
        super.extractRenderState(g,mx,my,pt);
    }
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==GLFW.GLFW_KEY_E||e.key()==GLFW.GLFW_KEY_ESCAPE){onClose();return true;}return super.keyPressed(e);}
    @Override public void onClose(){FacilityUiAccess.clear();super.onClose();}
    @Override public boolean isPauseScreen(){return false;}
}
