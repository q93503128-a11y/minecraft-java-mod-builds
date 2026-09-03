package kr.moonseungjun.survivalascension.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureService;
import kr.moonseungjun.survivalascension.network.InfrastructureActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public final class InfrastructureRadialMenuScreen extends Screen {
    private static final Entry[] ENTRIES = {
            new Entry("채석장 네트워크", "터널 5×5×8→7×7×10", new ItemStack(Items.RAIL), InfrastructureProject.QUARRY_NETWORK, Action.FUND),
            new Entry("관개 시설", "Lv.30 자동 재파종", new ItemStack(Items.WATER_BUCKET), InfrastructureProject.IRRIGATION_WORKS, Action.FUND),
            new Entry("건축 공방", "입체 5³→7³", new ItemStack(Items.SMITHING_TABLE), InfrastructureProject.BUILDER_FOUNDRY, Action.FUND),
            new Entry("전투 훈련장", "질주 전방 균열선", new ItemStack(Items.IRON_SWORD), InfrastructureProject.COMBAT_ACADEMY, Action.FUND),
            new Entry("토목 공사소", "3폭 도로/교량", new ItemStack(Items.SCAFFOLDING), InfrastructureProject.CIVIL_WORKS, Action.FUND),
            new Entry("산업 가공소", "생산·창고·전초·화물", new ItemStack(Items.BLAST_FURNACE), InfrastructureProject.INDUSTRIAL_WORKS, Action.OPEN_PRODUCTION),
            new Entry("정점 추적소", "원정권 정점 사냥", new ItemStack(Items.SPYGLASS), InfrastructureProject.APEX_TRACKING_POST, Action.FUND),
            new Entry("승천 중추", "공중 돌진·승천 시련", new ItemStack(Items.END_CRYSTAL), InfrastructureProject.ASCENSION_NEXUS, Action.FUND),
            new Entry("최후의 승천", "준비 4조건 · 세계의 시험 → 9지역 잔향 → 붕괴 봉쇄", new ItemStack(Items.NETHER_STAR), InfrastructureProject.ASCENSION_NEXUS, Action.FINAL_ASCENSION),
            new Entry("진행도", "월드 · 인프라 · 최후의 승천 준비 현황", new ItemStack(Items.MAP), null, Action.STATUS),
            new Entry("뒤로", "통합 메뉴로 돌아가기", new ItemStack(Items.ARROW), null, Action.BACK)
    };
    private static final int ITEM_COUNT=ENTRIES.length;
    private static final float OUTER_RADIUS=80.0F,INNER_RADIUS=60.0F;
    private static final double ICON_RADIUS=Math.sqrt(2.0D*61.5D*61.5D);

    public InfrastructureRadialMenuScreen(){super(Component.literal("Survival Ascension · 인프라"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){}

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);if(Minecraft.getInstance().level==null)return;
        int cx=this.width/2,cy=this.height/2,selected=RadialMenuGeometry.selectedIndex(ITEM_COUNT);Matrix3x2f pose=new Matrix3x2f(graphics.pose());ScreenRectangle scissor=graphics.peekScissorStack();
        graphics.submitGuiElementRenderState(new WheelElement(RenderPipelines.GUI,TextureSetup.noTexture(),pose,cx,cy,selected,scissor));
        for(int i=0;i<ITEM_COUNT;i++){double a=RadialMenuGeometry.iconRadians(i,ITEM_COUNT);graphics.item(ENTRIES[i].icon(),(int)Math.round(cx+ICON_RADIUS*Math.cos(a))-8,(int)Math.round(cy+ICON_RADIUS*Math.sin(a))-8);}
        Entry entry=ENTRIES[selected];String detail=detailFor(entry);graphics.text(this.font,entry.title(),cx-this.font.width(entry.title())/2,cy-10,0xFFFFFFFF,true);renderDetailLines(graphics,detail,cx,cy+3);
        String caption=ellipsize("기능을 먼저 보고 선택하세요 · 비용은 싱글플레이 체급으로 조정됨",Math.min(420,Math.max(120,this.width-24)));graphics.text(this.font,caption,cx-this.font.width(caption)/2,cy-102,0xFFE0E0E0,true);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(event.button()!=0)return false;
        Entry entry=ENTRIES[RadialMenuGeometry.selectedIndex(ITEM_COUNT)];
        if(entry.action()==Action.BACK){this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());return true;}
        if(entry.action()==Action.STATUS){ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(InfrastructureService.ALL_PROJECTS,InfrastructureService.ACTION_STATUS));this.minecraft.gui.setScreen(null);return true;}
        if(entry.action()==Action.OPEN_PRODUCTION){this.minecraft.gui.setScreen(new ProductionRadialMenuScreen());return true;}
        if(entry.action()==Action.FINAL_ASCENSION){ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(entry.project().id(),InfrastructureService.ACTION_FINAL_ASCENSION));this.minecraft.gui.setScreen(null);return true;}
        ClientPacketDistributor.sendToServer(new InfrastructureActionPayload(entry.project().id(),InfrastructureService.ACTION_FUND));this.minecraft.gui.setScreen(null);return true;
    }

    private static String detailFor(Entry entry){
        if(entry.project()==null || entry.action()==Action.FINAL_ASCENSION)return entry.detail();
        StringBuilder out=new StringBuilder(entry.detail());
        for(InfrastructureProject.Requirement requirement:entry.project().requirements()){
            if(!out.isEmpty())out.append(" · ");
            out.append(requirement.label()).append(' ').append(requirement.amount());
        }
        return out.toString();
    }

    private void renderDetailLines(GuiGraphicsExtractor graphics, String detail, int cx, int startY) {
        int maxWidth = Math.min(420, Math.max(120, this.width - 24));
        List<String> lines = wrapDetail(detail, maxWidth, 3);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            graphics.text(this.font, line, cx - this.font.width(line) / 2, startY + i * 10, 0xFFB8B8B8, false);
        }
    }

    private List<String> wrapDetail(String text, int maxWidth, int maxLines) {
        if (text == null || text.isBlank()) return List.of("");
        String remaining = text.trim();
        List<String> lines = new ArrayList<>();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            int fit = fitPrefix(remaining, maxWidth);
            if (fit >= remaining.length()) {
                lines.add(remaining);
                remaining = "";
                break;
            }
            int preferred = remaining.lastIndexOf(" · ", fit);
            int split = preferred > 0 ? preferred : fit;
            if (split <= 0) split = Math.min(remaining.length(), Math.max(1, fit));
            String line = remaining.substring(0, split).trim();
            if (line.isEmpty()) {
                line = remaining.substring(0, Math.min(remaining.length(), Math.max(1, fit))).trim();
                split = Math.max(1, fit);
            }
            lines.add(line);
            remaining = remaining.substring(Math.min(split, remaining.length())).trim();
            if (remaining.startsWith("·")) remaining = remaining.substring(1).trim();
        }
        if (!remaining.isEmpty() && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, ellipsize(lines.get(last) + " · " + remaining, maxWidth));
        }
        return lines.isEmpty() ? List.of(ellipsize(text, maxWidth)) : List.copyOf(lines);
    }

    private int fitPrefix(String text, int maxWidth) {
        if (text.isEmpty()) return 0;
        int low = 1;
        int high = text.length();
        int best = 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (this.font.width(text.substring(0, mid)) <= maxWidth) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    private String ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "…";
        int fit = fitPrefix(text, Math.max(1, maxWidth - this.font.width(suffix)));
        return text.substring(0, Math.max(1, fit)).trim() + suffix;
    }

    private enum Action{FUND,OPEN_PRODUCTION,FINAL_ASCENSION,STATUS,BACK}
    private record Entry(String title,String detail,ItemStack icon,InfrastructureProject project,Action action){}
    private record WheelElement(RenderPipeline pipeline,TextureSetup textureSetup,Matrix3x2f pose,int x,int y,int selected,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState{
        private WheelElement(RenderPipeline p,TextureSetup t,Matrix3x2f pose,int x,int y,int s,ScreenRectangle a){this(p,t,pose,x,y,s,a,boundsFor(x,y,pose,a));}
        @Override public void buildVertices(VertexConsumer v){for(int i=0;i<ITEM_COUNT;i++){double c=RadialMenuGeometry.sectorStartRadians(i,ITEM_COUNT),n=RadialMenuGeometry.sectorEndRadians(i,ITEM_COUNT);boolean h=i==selected;float in=((INNER_RADIUS-(h?2:0))/100)*130,out=((OUTER_RADIUS+(h?2:0))/100)*130;float p1ix=x+in*Mth.cos((float)c),p1iy=y+in*Mth.sin((float)c),p1ox=x+out*Mth.cos((float)c),p1oy=y+out*Mth.sin((float)c),p2ox=x+out*Mth.cos((float)n),p2oy=y+out*Mth.sin((float)n),p2ix=x+in*Mth.cos((float)n),p2iy=y+in*Mth.sin((float)n);int r=h?255:0;v.addVertexWith2DPose(pose,p1ox,p1oy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p1ix,p1iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ix,p2iy).setColor(r,0,0,153);v.addVertexWith2DPose(pose,p2ox,p2oy).setColor(r,0,0,153);}}
        private static ScreenRectangle boundsFor(int x,int y,Matrix3x2f pose,ScreenRectangle s){ScreenRectangle w=new ScreenRectangle(x-110,y-110,220,220).transformMaxBounds(pose);return s!=null?s.intersection(w):w;}
    }
}
