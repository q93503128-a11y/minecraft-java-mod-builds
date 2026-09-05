package io.github.q93503128.turnbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/** Readable authored-world overview with wheel zoom centered around the player. */
final class AsterMarchMapScreen extends Screen {
    private static final int TEXT = TurnboundUiTokens.TEXT_PRIMARY;
    private static final int SECONDARY = TurnboundUiTokens.TEXT_SECONDARY;
    private static final int MUTED = TurnboundUiTokens.TEXT_MUTED;
    private static final int BLUE = TurnboundUiTokens.PRIMARY;
    private static final int GOLD = TurnboundUiTokens.ACCENT;
    private static final int GREEN = TurnboundUiTokens.SUCCESS;
    private static final int RED = TurnboundUiTokens.DANGER;
    private static final int ROAD = TurnboundUiTokens.MAP_ROAD;

    private static final double[][] SOUTH_ROUTE = {{0,110},{0,132},{13,176},{88,205},{190,230},{286,240},{355,245}};
    private static final double[][] GLOAM_ROUTE = {{0,-108},{-3,-145},{-12,-161},{-68,-226},{-40,-300},{-98,-392},{-35,-440}};
    private static final double[][] AQUEDUCT_ROUTE = {{-124,20},{-180,42},{-240,-18},{-274,46},{-320,20},{-380,15},{-430,35}};
    private static final double[][] QUARRY_ROUTE = {{190,230},{118,266},{42,286},{-60,300},{-110,315},{20,405},{65,455}};
    private static final double[][] RELAY_ROUTE = {{124,-80},{166,-104},{232,-156},{270,-185},{365,-305},{430,-350}};

    private int left, top, panelWidth, panelHeight;
    private double zoom = 1.0;

    AsterMarchMapScreen() { super(Component.literal("아스테르 변경 지도")); }

    @Override protected void init() {
        super.init();
        panelWidth = Math.min(980, Math.max(360, width - 22));
        panelHeight = Math.min(680, Math.max(300, height - 22));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_M || event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        zoom = Math.max(1.0, Math.min(4.0, zoom + (scrollY > 0 ? 0.35 : -0.35)));
        return true;
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, BLUE);
        graphics.text(font, Component.literal("아스테르 변경 · 월드 지도"), left + 16, top + 14, TEXT, true);
        String help = "휠 확대/축소 · " + String.format(java.util.Locale.ROOT, "×%.1f", zoom) + "  /  M 또는 ESC 닫기";
        graphics.text(font, Component.literal(help), left + panelWidth - 16 - font.width(help), top + 14, SECONDARY, false);
        graphics.text(font, Component.literal("지역 · 이동로 · 시설 · 사냥터 · 보스"), left + 16, top + 30, SECONDARY, false);

        // Common GUI-scale 3 windows are logically ~540px wide: still use the empty right half as information space.
        boolean wide = panelWidth >= 480;
        int infoReserve = wide ? Math.min(190, Math.max(150, panelWidth / 3)) : 0;
        int mapSize = Math.min(panelHeight - (wide ? 64 : 126), panelWidth - 30 - infoReserve);
        mapSize = Math.max(170, mapSize);
        int mapX = left + 15;
        int mapY = top + 47;

        graphics.fill(mapX - 2, mapY - 2, mapX + mapSize + 2, mapY + mapSize + 2, 0xFF8B7B60);
        graphics.fill(mapX, mapY, mapX + mapSize, mapY + mapSize, 0xFF20262A);

        double px = minecraft.player == null ? 0.0 : minecraft.player.position().x;
        double pz = minecraft.player == null ? 0.0 : minecraft.player.position().z;
        Viewport view = viewport(px, pz);
        for (AsterMarchMapData.Region region : AsterMarchMapData.REGIONS) {
            int x1 = mapX + worldToMap(region.minX(), view.minX, view.span, mapSize);
            int x2 = mapX + worldToMap(region.maxX(), view.minX, view.span, mapSize);
            int y1 = mapY + worldToMap(region.minZ(), view.minZ, view.span, mapSize);
            int y2 = mapY + worldToMap(region.maxZ(), view.minZ, view.span, mapSize);
            int rx = Math.max(mapX, Math.min(x1, x2)), ry = Math.max(mapY, Math.min(y1, y2));
            int rr = Math.min(mapX + mapSize, Math.max(x1, x2)), rb = Math.min(mapY + mapSize, Math.max(y1, y2));
            if (rr <= rx || rb <= ry) continue;
            graphics.fill(rx, ry, rr, rb, regionColor(region.label()));
            outline(graphics, rx, ry, rr - rx, rb - ry, 0xA0D5C8A6);
            if (mapSize >= 330 && rx + 8 < rr) {
                String label = UiTextLayout.fit(region.label(), Math.max(18, rr - rx - 8));
                graphics.text(font, Component.literal(label), rx + 4, ry + 4, 0xEFFFFFFF, true);
            }
        }

        drawRoute(graphics,mapX,mapY,mapSize,view,SOUTH_ROUTE); drawRoute(graphics,mapX,mapY,mapSize,view,GLOAM_ROUTE);
        drawRoute(graphics,mapX,mapY,mapSize,view,AQUEDUCT_ROUTE); drawRoute(graphics,mapX,mapY,mapSize,view,QUARRY_ROUTE);
        drawRoute(graphics,mapX,mapY,mapSize,view,RELAY_ROUTE);

        AsterMarchMapData.Marker hovered = null; double hoveredDistance = Double.MAX_VALUE;
        for (AsterMarchMapData.Marker marker : AsterMarchMapData.MARKERS) {
            if (!inside(marker.x(), marker.z(), view)) continue;
            int sx = mapX + worldToMap(marker.x(), view.minX, view.span, mapSize);
            int sy = mapY + worldToMap(marker.z(), view.minZ, view.span, mapSize);
            AsterMarchMarkerStyle.draw(graphics, sx, sy, marker.kind());
            double dx=mouseX-sx,dy=mouseY-sy,d=dx*dx+dy*dy; if(d<=81.0&&d<hoveredDistance){hoveredDistance=d;hovered=marker;}
        }
        if (inside(px,pz,view)) {
            int psx=mapX+worldToMap(px,view.minX,view.span,mapSize),psy=mapY+worldToMap(pz,view.minZ,view.span,mapSize);
            graphics.fill(psx-4,psy-1,psx+5,psy+2,0xFF101317); graphics.fill(psx-1,psy-4,psx+2,psy+5,0xFF101317);
            graphics.fill(psx-3,psy,psx+4,psy+1,0xFFFFFFFF); graphics.fill(psx,psy-3,psx+1,psy+4,0xFFFFFFFF);
        }

        AsterMarchMapData.Marker focus = hovered != null ? hovered : AsterMarchMapData.nearest(px,pz);
        int infoX=wide?mapX+mapSize+13:mapX, infoY=wide?mapY:mapY+mapSize+10;
        int infoW=wide?left+panelWidth-15-infoX:mapSize, infoH=wide?mapSize:Math.max(60,top+panelHeight-13-infoY);
        TurnboundFrameStyle.inset(graphics,infoX,infoY,infoW,infoH);
        graphics.text(font,Component.literal("현재  X "+(int)Math.round(px)+" · Z "+(int)Math.round(pz)),infoX+9,infoY+9,TEXT,true);
        if(focus!=null){
            graphics.text(font,Component.literal(UiTextLayout.fit(focus.label(),infoW-18)),infoX+9,infoY+29,markerColor(focus.kind()),true);
            graphics.text(font,Component.literal(UiTextLayout.fit(focus.info(),infoW-18)),infoX+9,infoY+45,SECONDARY,false);
            int distance=(int)Math.round(Math.hypot(focus.x()-px,focus.z()-pz)); graphics.text(font,Component.literal("약 "+distance+"m"),infoX+9,infoY+61,MUTED,false);
        }
        int legendY=infoY+91;
        if(infoH>150){
            legend(graphics,infoX+9,legendY,AsterMarchMapData.Kind.FACILITY,"시설");
            legend(graphics,infoX+9,legendY+17,AsterMarchMapData.Kind.HUNT,"사냥터");
            legend(graphics,infoX+9,legendY+34,AsterMarchMapData.Kind.RELAY,"계전소");
            legend(graphics,infoX+9,legendY+51,AsterMarchMapData.Kind.BOSS,"보스");
        }
        if(infoH>190) graphics.text(font,Component.literal("━ 주요 이동로"),infoX+9,legendY+72,ROAD,false);
        if(infoH>220){
            graphics.text(font,Component.literal("N · 미니맵 숨김/표시"),infoX+9,legendY+94,SECONDARY,false);
            graphics.text(font,Component.literal("J · 목표 상세 접기/열기"),infoX+9,legendY+109,SECONDARY,false);
        }
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    private void legend(GuiGraphicsExtractor g,int x,int y,AsterMarchMapData.Kind kind,String text){
        AsterMarchMarkerStyle.drawSmall(g,x+4,y+5,kind);
        g.text(font,Component.literal(text),x+11,y,SECONDARY,false);
    }
    private Viewport viewport(double px,double pz){double full=AsterMarchMapData.MAX-AsterMarchMapData.MIN,span=full/zoom;double minX=zoom<=1.001?AsterMarchMapData.MIN:clamp(px-span/2.0,AsterMarchMapData.MIN,AsterMarchMapData.MAX-span);double minZ=zoom<=1.001?AsterMarchMapData.MIN:clamp(pz-span/2.0,AsterMarchMapData.MIN,AsterMarchMapData.MAX-span);return new Viewport(minX,minZ,span);}
    private void drawRoute(GuiGraphicsExtractor g,int mapX,int mapY,int mapSize,Viewport view,double[][] route){for(int i=0;i<route.length-1;i++){int x0=mapX+worldToMap(route[i][0],view.minX,view.span,mapSize),y0=mapY+worldToMap(route[i][1],view.minZ,view.span,mapSize),x1=mapX+worldToMap(route[i+1][0],view.minX,view.span,mapSize),y1=mapY+worldToMap(route[i+1][1],view.minZ,view.span,mapSize);drawLineClipped(g,x0,y0,x1,y1,mapX,mapY,mapX+mapSize,mapY+mapSize,ROAD);}}
    private void drawLineClipped(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int minX,int minY,int maxX,int maxY,int color){int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy,guard=0;while(guard++<4096){if(x0>=minX&&x0<maxX&&y0>=minY&&y0<maxY)g.fill(x0,y0,x0+2,y0+2,color);if(x0==x1&&y0==y1)break;int e2=2*err;if(e2>=dy){err+=dy;x0+=sx;}if(e2<=dx){err+=dx;y0+=sy;}}}
    private int worldToMap(double value,double minimum,double span,int mapSize){return(int)Math.round((value-minimum)/span*mapSize);} private boolean inside(double x,double z,Viewport v){return x>=v.minX&&x<=v.minX+v.span&&z>=v.minZ&&z<=v.minZ+v.span;}
    private int regionColor(String label){return switch(label){case"라디아"->0xD05D765A;case"남문 초원"->0xC0748B4B;case"그늘숲"->0xC03C5A48;case"붕괴 수로"->0xC06B7076;case"잿불 채석장"->0xC0845B45;case"구 중계소"->0xC0444655;default->0xB05C636B;};}
    private void outline(GuiGraphicsExtractor g,int x,int y,int width,int height,int color){if(width<=1||height<=1)return;g.fill(x,y,x+width,y+1,color);g.fill(x,y+height-1,x+width,y+height,color);g.fill(x,y,x+1,y+height,color);g.fill(x+width-1,y,x+width,y+height,color);}
    private static int markerColor(AsterMarchMapData.Kind kind){return AsterMarchMarkerStyle.color(kind);}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
    private record Viewport(double minX,double minZ,double span){}
}
