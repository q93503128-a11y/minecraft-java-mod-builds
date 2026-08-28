package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.network.BattleCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList; import java.util.List;

public final class BattleScreen extends Screen {
    private static final Identifier PANEL=Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"turnbound/panel_blue");
    private static final Identifier INSET=Identifier.fromNamespaceAndPath(Turnbound.MOD_ID,"turnbound/panel_inset_blue");
    private final List<KenneyButton> skillButtons=new ArrayList<>(),targetButtons=new ArrayList<>(); private KenneyButton autoButton,speedButton,fleeButton; private String selectedSkill=""; private long seen=-1;
    public BattleScreen(){super(Component.literal("TURNBOUND"));}
    @Override protected void init(){super.init(); skillButtons.clear();targetButtons.clear();int sw=Math.min(150,Math.max(105,width/6));int sy=height-62;for(int i=0;i<5;i++){final int n=i;KenneyButton b=new KenneyButton(width/2-((sw+4)*5)/2+i*(sw+4),sy,sw,42,Component.empty(),x->skill(n));skillButtons.add(addRenderableWidget(b));}
        int tw=Math.min(138,Math.max(96,width/7)); for(int i=0;i<9;i++){final int n=i; int x=i<4?18:width-tw-18; int row=i<4?i:i-4; int y=58+row*50; KenneyButton b=new KenneyButton(x,y,tw,42,Component.empty(),z->target(n));targetButtons.add(addRenderableWidget(b));}
        autoButton=addRenderableWidget(new KenneyButton(width-228,12,66,32,Component.literal("AUTO"),b->send("AUTO"))); speedButton=addRenderableWidget(new KenneyButton(width-156,12,66,32,Component.literal("×1"),b->send("SPEED"))); fleeButton=addRenderableWidget(new KenneyButton(width-84,12,66,32,Component.literal("퇴각"),b->send("FLEE"))); refresh();}
    @Override public void tick(){super.tick();if(seen!=ClientBattleState.revision())refresh();}
    private void refresh(){seen=ClientBattleState.revision();var s=ClientBattleState.snapshot();for(int i=0;i<skillButtons.size();i++){var b=skillButtons.get(i);if(i<s.skills().size()){var sk=s.skills().get(i);b.setMessage(Component.literal((selectedSkill.equals(sk.id())?"▶ ":"")+sk.name()+(sk.remaining()>0?"  CD "+sk.remaining():"")));b.active=!s.finished()&&!s.auto()&&s.actorId().startsWith("ally_")&&sk.remaining()==0;b.visible=true;}else{b.visible=false;}}
        for(int i=0;i<targetButtons.size();i++){var b=targetButtons.get(i);if(i<s.units().size()){var u=s.units().get(i);b.setMessage(Component.literal(u.name()+"  "+u.hp()+"/"+u.maxHp()));b.active=!s.finished();b.visible=true;}else b.visible=false;} autoButton.setMessage(Component.literal(s.auto()?"AUTO ON":"AUTO")); speedButton.setMessage(Component.literal("×"+s.speed()));}
    private void skill(int i){var s=ClientBattleState.snapshot();if(i>=s.skills().size())return;var sk=s.skills().get(i);selectedSkill=sk.id();if(sk.targetRule().equals("SELF")||sk.targetRule().endsWith("_ALL"))send("ACT|"+s.actorId()+"|"+sk.id()+"|");else refresh();}
    private void target(int i){var s=ClientBattleState.snapshot();if(selectedSkill.isBlank()||i>=s.units().size())return;send("ACT|"+s.actorId()+"|"+selectedSkill+"|"+s.units().get(i).id());selectedSkill="";}
    private static void send(String s){ClientPacketDistributor.sendToServer(new BattleCommandPayload(s));}
    @Override public void extractBackground(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){g.fill(0,0,width,height,0x9910131A);g.blitSprite(RenderPipelines.GUI_TEXTURED,PANEL,8,48,Math.min(160,width/4),220);g.blitSprite(RenderPipelines.GUI_TEXTURED,PANEL,width-Math.min(160,width/4)-8,48,Math.min(160,width/4),270);g.blitSprite(RenderPipelines.GUI_TEXTURED,INSET,width/2-230,8,460,42);g.blitSprite(RenderPipelines.GUI_TEXTURED,INSET,width/2-390,height-72,780,62);}
    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor g,int mx,int my,float pt){super.extractRenderState(g,mx,my,pt);var s=ClientBattleState.snapshot();g.text(font,Component.literal("TURNBOUND · "+(s.finished()?result(s.outcome()):"전투 진행")),width/2-65,17,0xFFF4F0E6,true);int x=width/2-190;for(String id:s.timeline()){var u=s.units().stream().filter(v->v.id().equals(id)).findFirst().orElse(null);if(u!=null){g.text(font,Component.literal(u.name().substring(0,Math.min(2,u.name().length()))),x,35,u.side().equals("ALLY")?0xFF6DC6FF:0xFFFF7A59,true);x+=48;}} if(!s.message().isBlank())g.text(font,Component.literal(s.message()),width/2-150,height-84,0xFFAEB7C6,true);}
    private static String result(String o){return o.equals("ALLY_VICTORY")?"승리":o.equals("ENEMY_VICTORY")?"패배":"종료";}
    @Override public boolean isPauseScreen(){return false;} @Override public void onClose(){if(ClientBattleState.snapshot().finished())send("FLEE");}
}
