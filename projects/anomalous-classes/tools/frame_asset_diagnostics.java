import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Pixel-level frame atlas and runtime-part review. */
public final class frame_asset_diagnostics {
    private record Part(String name, int x, int y, int width, int height) {}
    private static final Part[] PARTS = {
            new Part("corner_tl", 32, 32, 160, 160), new Part("corner_tr", 576, 32, 160, 160),
            new Part("corner_bl", 32, 576, 160, 160), new Part("corner_br", 576, 576, 160, 160),
            new Part("rail_top_left", 318, 64, 24, 64), new Part("rail_top_right", 426, 64, 24, 64),
            new Part("rail_bottom_left", 319, 640, 24, 64), new Part("rail_bottom_right", 426, 640, 24, 64),
            new Part("rail_side_left", 64, 434, 64, 24), new Part("rail_side_right", 640, 428, 64, 24),
            new Part("jewel_side_left", 84, 344, 24, 64), new Part("jewel_side_right", 660, 344, 24, 64),
            new Part("jewel_top", 368, 48, 32, 88), new Part("jewel_bottom", 368, 628, 32, 88)
    };
    private static final Part[] PREVIOUS_PARTS = {
            new Part("corner_tl", 32, 32, 160, 160), new Part("corner_tr", 576, 32, 160, 160),
            new Part("corner_bl", 32, 576, 160, 160), new Part("corner_br", 576, 576, 160, 160),
            new Part("rail_top_left", 318, 64, 24, 64), new Part("rail_top_right", 426, 64, 24, 64),
            new Part("rail_bottom_left", 319, 640, 24, 64), new Part("rail_bottom_right", 426, 640, 24, 64),
            new Part("rail_side_left", 64, 434, 64, 24), new Part("rail_side_right", 640, 428, 64, 24),
            new Part("jewel_side_left", 72, 344, 48, 64), new Part("jewel_side_right", 648, 344, 48, 64),
            new Part("jewel_top", 352, 48, 64, 88), new Part("jewel_bottom", 352, 628, 64, 88)
    };
    private frame_asset_diagnostics() {}

    public static void main(String[] args) throws Exception {
        Path project = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        Path output = (args.length < 2 ? project.resolve("ui/reviews/2026-11-11/frame-root-cause") : Path.of(args[1]))
                .toAbsolutePath().normalize();
        Path root = project.resolve("src/main/resources/assets/anomalousclasses/textures/gui/menu_v1");
        BufferedImage atlas = read(root.resolve("frame_atlas.png"));
        BufferedImage surface = read(root.resolve("surface_tile.png"));
        BufferedImage title = read(root.resolve("title_plate.png"));
        BufferedImage buttons = read(root.resolve("button_states.png"));
        Path parts = root.resolve("frame_parts");
        Files.createDirectories(output);
        Files.createDirectories(output.resolve("after"));
        Path previousParts = Files.createTempDirectory("anomalous-frame-before-");
        System.out.printf("ATLAS %dx%d type=%d%n", atlas.getWidth(), atlas.getHeight(), atlas.getType());
        for (Part part : PARTS) {
            BufferedImage crop = read(parts.resolve(part.name() + ".png"));
            int clear = 0, lowAlpha = 0, opaque = 0, nonBlack = 0, blackAlpha = 0;
            int minAlpha = 255, maxAlpha = 0, minX = crop.getWidth(), minY = crop.getHeight(), maxX = -1, maxY = -1;
            int mismatches = 0, alphaDiffs = 0, visibleRgbDiffs = 0, transparentRgbDiffs = 0, maxAlphaDiff = 0, maxRgbDiff = 0;
            for (int y = 0; y < crop.getHeight(); y++) for (int x = 0; x < crop.getWidth(); x++) {
                int pixel = crop.getRGB(x, y), alpha = pixel >>> 24;
                int sourcePixel = atlas.getRGB(part.x()+x, part.y()+y), sourceAlpha = sourcePixel >>> 24;
                if (alpha == 0) clear++;
                if (alpha > 0 && alpha < 32) lowAlpha++;
                if (alpha >= 250) opaque++;
                if (alpha > 0 && ((pixel >>> 16 & 255) > 24 || (pixel >>> 8 & 255) > 24 || (pixel & 255) > 24)) nonBlack++;
                if (alpha > 0 && (pixel & 0x00FFFFFF) < 0x00080808) blackAlpha++;
                minAlpha = Math.min(minAlpha, alpha); maxAlpha = Math.max(maxAlpha, alpha);
                if (alpha > 0) { minX=Math.min(minX,x); minY=Math.min(minY,y); maxX=Math.max(maxX,x); maxY=Math.max(maxY,y); }
                if (pixel != sourcePixel) {
                    mismatches++;
                    if (alpha != sourceAlpha) { alphaDiffs++; maxAlphaDiff=Math.max(maxAlphaDiff,Math.abs(alpha-sourceAlpha)); }
                    if (alpha > 0 || sourceAlpha > 0) visibleRgbDiffs++;
                    else transparentRgbDiffs++;
                    for (int shift : new int[]{16,8,0}) maxRgbDiff=Math.max(maxRgbDiff,Math.abs((pixel >>> shift & 255)-(sourcePixel >>> shift & 255)));
                }
            }
            System.out.printf("%-20s %dx%d clear=%d lowA=%d opaque=%d blackA=%d alpha=%d..%d bounds=%d,%d..%d,%d cropDiff=%d(alpha=%d,maxA=%d maxRGB=%d visibleRGB=%d transparentRGB=%d)%n",
                    part.name(), crop.getWidth(), crop.getHeight(), clear, lowAlpha, opaque, blackAlpha,
                    minAlpha, maxAlpha, minX, minY, maxX, maxY, mismatches, alphaDiffs, maxAlphaDiff, maxRgbDiff, visibleRgbDiffs, transparentRgbDiffs);
            if (mismatches != 0) throw new IOException("Extracted RGBA differs from source atlas for " + part.name());
            if(part.name().equals("corner_tl")||part.name().equals("jewel_top")) printSamples(crop,part.name());
            crop.flush();
        }
        extractPreviousParts(project, previousParts);
        auditPreviousCrops(previousParts,atlas);
        BufferedImage previousPanel=renderProfileFrame(previousParts,surface,title,buttons,true);
        BufferedImage currentPanel=renderProfileFrame(parts,surface,title,buttons,false);
        write(previousPanel,output.resolve("profile_frame_before_scale4_static.png"));
        write(currentPanel,output.resolve("profile_frame_after_scale4_static.png"));
        writeFullComparison(previousPanel,currentPanel,output.resolve("profile_frame_before_after.png"));
        cropPanel(currentPanel,output.resolve("after"));
        writeComparisons(previousPanel,currentPanel,output.resolve("comparison"));
        previousPanel.flush(); currentPanel.flush();
        if (args.length >= 3) {
            BufferedImage screenshot = read(Path.of(args[2]));
            System.out.printf("SCREENSHOT %dx%d%n", screenshot.getWidth(), screenshot.getHeight());
            screenshotCrop(screenshot, output.resolve("screenshot_corner_tl.png"), 24, 48, 152, 150, 4);
            screenshotCrop(screenshot, output.resolve("screenshot_corner_tr.png"), screenshot.getWidth()-176, 48, 152, 150, 4);
            screenshotCrop(screenshot, output.resolve("screenshot_corner_bl.png"), 24, screenshot.getHeight()-178, 152, 150, 4);
            screenshotCrop(screenshot, output.resolve("screenshot_corner_br.png"), screenshot.getWidth()-176, screenshot.getHeight()-178, 152, 150, 4);
            screenshotCrop(screenshot, output.resolve("screenshot_jewel_top.png"), screenshot.getWidth()/2-96, 48, 192, 150, 3);
            screenshotCrop(screenshot, output.resolve("screenshot_jewel_bottom.png"), screenshot.getWidth()/2-96, screenshot.getHeight()-178, 192, 150, 3);
            screenshot.flush();
        }
        atlas.flush(); surface.flush(); title.flush(); buttons.flush();
        deleteTemporaryDirectory(previousParts);
    }

    private static void extractPreviousParts(Path project, Path destination) throws Exception {
        Path repository = project.getParent().getParent();
        for (Part part : PARTS) {
            String relative = "projects/anomalous-classes/src/main/resources/assets/anomalousclasses/"
                    + "textures/gui/menu_v1/frame_parts/" + part.name() + ".png";
            Process process = new ProcessBuilder("git", "show", "HEAD:" + relative)
                    .directory(repository.toFile()).start();
            byte[] bytes = process.getInputStream().readAllBytes();
            byte[] errors = process.getErrorStream().readAllBytes();
            int status = process.waitFor();
            if (status != 0) throw new IOException("Cannot read previous committed frame part " + part.name()
                    + ": " + new String(errors));
            Files.write(destination.resolve(part.name() + ".png"), bytes);
        }
    }

    private static void auditPreviousCrops(Path previousParts,BufferedImage atlas)throws IOException {
        System.out.println("PREVIOUS EXTRACTOR PIXEL COMPARISON (against previous atlas crop):");
        for(Part part:PREVIOUS_PARTS){
            BufferedImage image=read(previousParts.resolve(part.name()+".png"));
            int mismatches=0,alphaDiffs=0,visibleRgbDiffs=0,transparentRgbDiffs=0,maxRgb=0,maxAlpha=0;
            for(int y=0;y<part.height();y++)for(int x=0;x<part.width();x++){
                int actual=image.getRGB(x,y),source=atlas.getRGB(part.x()+x,part.y()+y);
                if(actual==source)continue;
                mismatches++;
                int a=actual>>>24,sa=source>>>24;
                if(a!=sa){alphaDiffs++;maxAlpha=Math.max(maxAlpha,Math.abs(a-sa));}
                if(a>0||sa>0)visibleRgbDiffs++;else transparentRgbDiffs++;
                for(int shift:new int[]{16,8,0})maxRgb=Math.max(maxRgb,Math.abs((actual>>>shift&255)-(source>>>shift&255)));
            }
            System.out.printf("  %-20s %dx%d diff=%d alphaDiff=%d maxAlpha=%d maxRGB=%d visibleRGB=%d transparentRGB=%d%n",
                    part.name(),part.width(),part.height(),mismatches,alphaDiffs,maxAlpha,maxRgb,visibleRgbDiffs,transparentRgbDiffs);
            image.flush();
        }
    }

    private static void deleteTemporaryDirectory(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) Files.deleteIfExists(file);
        }
        Files.deleteIfExists(directory);
    }

    private static BufferedImage renderProfileFrame(Path parts, BufferedImage surface, BufferedImage title,
                                                    BufferedImage buttons, boolean previous) throws IOException {
        // The supplied 2048x1104 gameplay capture is a 512x276 logical GUI at scale 4.
        int logicalW=512,logicalH=276,scale=4;
        BufferedImage out=new BufferedImage(logicalW*scale,logicalH*scale,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setComposite(AlphaComposite.Src); fillChecker(g,logicalW*scale,logicalH*scale,scale*8);
        g.setComposite(AlphaComposite.SrcOver); g.scale(scale,scale);
        int margin=logicalW<360?5:10,top=logicalH<250?4:10;
        Box box=new Box(margin,top,Math.max(1,logicalW-margin*2),Math.max(1,logicalH-top*2));
        tileSurface(g,surface,box);
        drawFrame(g,parts,box,previous);
        drawTitlePlate(g,title,box,logicalW,logicalH);
        int inset=Math.min(30,Math.max(12,Math.min(box.w(),box.h())/10));
        int navH=Math.max(19,Math.min(23,logicalH/15));
        int navY=box.bottom()-navH-Math.max(7,inset/2);
        int innerW=box.w()-inset*2,gap=6,buttonW=Math.max(1,Math.min(156,(innerW-gap)/2));
        int buttonX=box.x()+(box.w()-(buttonW*2+gap))/2;
        drawButton(g,buttons,new Box(buttonX,navY,buttonW,navH));
        drawButton(g,buttons,new Box(buttonX+buttonW+gap,navY,buttonW,navH));
        g.dispose(); return out;
    }

    private static void tileSurface(Graphics2D g,BufferedImage texture,Box b) {
        for(int y=b.y();y<b.bottom();y+=64) for(int x=b.x();x<b.right();x+=64) {
            int w=Math.min(64,b.right()-x),h=Math.min(64,b.bottom()-y);
            draw(g,texture,x,y,w,h,0,0,Math.max(1,w*4),Math.max(1,h*4));
        }
    }

    private static void drawFrame(Graphics2D g,Path parts,Box b,boolean previous) throws IOException {
        int corner=Math.max(6,Math.min(40,Math.min(b.w(),b.h())/10)); corner=Math.min(corner,Math.min(b.w()/2,b.h()/2));
        int offset=scale(corner,32,160),thick=Math.max(1,scale(corner,64,160)),cx=b.x()+b.w()/2;
        boolean ornaments=corner>=14&&b.w()>=120&&b.h()>=48;
        int jewelSourceWidth=previous?64:32, sideSourceWidth=previous?48:24;
        int jw=ornaments?scale(corner,jewelSourceWidth,160):0,jh=ornaments?scale(corner,88,160):0;
        int jl=cx-jw/2,jr=cx+(jw+1)/2,start=b.x()+corner-1,end=b.right()-corner+1;
        int ty=b.y()+offset,by=b.bottom()-corner+offset,tile=scale(corner,24,160);
        BufferedImage[] top={read(parts.resolve("rail_top_left.png")),read(parts.resolve("rail_top_right.png"))};
        BufferedImage[] bottom={read(parts.resolve("rail_bottom_left.png")),read(parts.resolve("rail_bottom_right.png"))};
        if(previous) {
            if(ornaments){ hTiles(g,top[0],start,ty,jl,thick,tile,24,64); hTiles(g,top[1],jr,ty,end,thick,tile,24,64);
                hTiles(g,bottom[0],start,by,jl,thick,tile,24,64); hTiles(g,bottom[1],jr,by,end,thick,tile,24,64);
            } else {hTiles(g,top[0],start,ty,end,thick,tile,24,64);hTiles(g,bottom[0],start,by,end,thick,tile,24,64);}
        } else {
            if(ornaments){ hRail(g,top[0],start,ty,jl,thick,12,24,64); hRail(g,top[1],jr,ty,end,thick,12,24,64);
                hRail(g,bottom[0],start,by,jl,thick,12,24,64); hRail(g,bottom[1],jr,by,end,thick,12,24,64);
            } else {hRail(g,top[0],start,ty,end,thick,12,24,64);hRail(g,bottom[0],start,by,end,thick,12,24,64);}
        }
        int cy=b.y()+b.h()/2,sw=ornaments?scale(corner,sideSourceWidth,160):0,sh=ornaments?scale(corner,64,160):0;
        int sy=cy-sh/2,lx=b.x()+offset,rx=b.right()-corner+offset,se=b.bottom()-corner+1,ss=b.y()+corner-1,st=scale(corner,24,160);
        BufferedImage[] side={read(parts.resolve("rail_side_left.png")),read(parts.resolve("rail_side_right.png"))};
        if(previous){
            if(ornaments){vTiles(g,side[0],lx,ss,sy,thick,tile,64,24);vTiles(g,side[0],lx,sy+sh,se,thick,tile,64,24);
                vTiles(g,side[1],rx,ss,sy,thick,tile,64,24);vTiles(g,side[1],rx,sy+sh,se,thick,tile,64,24);
            }else{vTiles(g,side[0],lx,ss,se,thick,tile,64,24);vTiles(g,side[1],rx,ss,se,thick,tile,64,24);}
        } else {
            if(ornaments){vRail(g,side[0],lx,ss,sy,thick,12,64,24);vRail(g,side[0],lx,sy+sh,se,thick,12,64,24);
                vRail(g,side[1],rx,ss,sy,thick,12,64,24);vRail(g,side[1],rx,sy+sh,se,thick,12,64,24);
            }else{vRail(g,side[0],lx,ss,se,thick,12,64,24);vRail(g,side[1],rx,ss,se,thick,12,64,24);}
        }
        draw(g,read(parts.resolve("corner_tl.png")),b.x(),b.y(),corner,corner,0,0,160,160);
        draw(g,read(parts.resolve("corner_tr.png")),b.right()-corner,b.y(),corner,corner,0,0,160,160);
        draw(g,read(parts.resolve("corner_bl.png")),b.x(),b.bottom()-corner,corner,corner,0,0,160,160);
        draw(g,read(parts.resolve("corner_br.png")),b.right()-corner,b.bottom()-corner,corner,corner,0,0,160,160);
        if(ornaments){
            int sjl=lx+(thick-sw)/2,sjr=rx+(thick-sw)/2;
            draw(g,read(parts.resolve("jewel_side_left.png")),sjl,sy,sw,sh,0,0,sideSourceWidth,64);
            draw(g,read(parts.resolve("jewel_side_right.png")),sjr,sy,sw,sh,0,0,sideSourceWidth,64);
            int jy=ty+(thick-jh)/2,jby=by+(thick-jh)/2;
            draw(g,read(parts.resolve("jewel_top.png")),jl,jy,jw,jh,0,0,jewelSourceWidth,88);
            draw(g,read(parts.resolve("jewel_bottom.png")),jl,jby,jw,jh,0,0,jewelSourceWidth,88);
        }
        for(BufferedImage image:top)image.flush();for(BufferedImage image:bottom)image.flush();for(BufferedImage image:side)image.flush();
    }

    private static void drawTitlePlate(Graphics2D g,BufferedImage image,Box outer,int screenW,int screenH) {
        int ph=screenH<280?24:32,pw=Math.min(340,Math.max(150,outer.w()*36/100));pw=Math.min(pw,Math.max(1,outer.w()-12));
        int cap=Math.max(1,(int)Math.round(ph*132.0/342.0)),jewel=Math.max(1,(int)Math.round(ph*112.0/342.0));
        int middle=Math.max(2,pw-cap*2-jewel),lm=middle/2,rm=middle-lm,x=(screenW-pw)/2,y=outer.y()-3;
        draw(g,image,x,y,cap,ph,0,0,132,342);draw(g,image,x+cap,y,lm,ph,132,0,324,342);
        draw(g,image,x+cap+lm,y,jewel,ph,456,0,112,342);draw(g,image,x+cap+lm+jewel,y,rm,ph,568,0,324,342);
        draw(g,image,x+pw-cap,y,cap,ph,892,0,132,342);
    }

    private static void drawButton(Graphics2D g,BufferedImage image,Box b) {
        int cap=Math.min(b.h(),Math.max(1,b.w()/2)),center=Math.max(0,b.w()-cap*2);
        draw(g,image,b.x(),b.y(),cap,b.h(),0,0,256,256);draw(g,image,b.x()+cap,b.y(),center,b.h(),256,0,512,256);
        draw(g,image,b.x()+b.w()-cap,b.y(),cap,b.h(),768,0,256,256);
    }

    private static void hRail(Graphics2D g,BufferedImage image,int x,int y,int end,int h,int sampleX,int tw,int th) {
        draw(g,image,x,y,end-x,h,sampleX,0,1,th);
    }
    private static void vRail(Graphics2D g,BufferedImage image,int x,int y,int end,int w,int sampleY,int tw,int th) {
        draw(g,image,x,y,w,end-y,0,sampleY,tw,1);
    }
    private static void hTiles(Graphics2D g,BufferedImage image,int x,int y,int end,int h,int tile,int sw,int sh) {
        while(x<end){int dw=Math.min(tile,end-x);int cw=Math.min(sw,(int)Math.ceil(dw*sw/(double)tile));draw(g,image,x,y,dw,h,0,0,cw,sh);x+=dw;}
    }
    private static void vTiles(Graphics2D g,BufferedImage image,int x,int y,int end,int w,int tile,int sw,int sh) {
        while(y<end){int dh=Math.min(tile,end-y);int ch=Math.min(sh,(int)Math.ceil(dh*sh/(double)tile));draw(g,image,x,y,w,dh,0,0,sw,ch);y+=dh;}
    }
    private static void draw(Graphics2D g,BufferedImage image,int x,int y,int w,int h,int u,int v,int sw,int sh) {
        if(w>0&&h>0&&sw>0&&sh>0)g.drawImage(image,x,y,x+w,y+h,u,v,Math.min(image.getWidth(),u+sw),Math.min(image.getHeight(),v+sh),null);
    }
    private static int scale(int value,int numerator,int denominator){return Math.max(1,Math.round(value*(float)numerator/denominator));}
    private static void cropPanel(BufferedImage image,Path directory)throws IOException {
        int w=image.getWidth(),h=image.getHeight(),cx=w/2,sz=170;
        screenshotCrop(image,directory.resolve("corner_tl.png"),0,0,190,190,3);
        screenshotCrop(image,directory.resolve("corner_tr.png"),w-190,0,190,190,3);
        screenshotCrop(image,directory.resolve("corner_bl.png"),0,h-190,190,190,3);
        screenshotCrop(image,directory.resolve("corner_br.png"),w-190,h-190,190,190,3);
        screenshotCrop(image,directory.resolve("jewel_top.png"),cx-100,0,200,190,3);
        screenshotCrop(image,directory.resolve("jewel_bottom.png"),cx-100,h-190,200,190,3);
    }

    private static void fillChecker(Graphics2D g,int width,int height,int tile) {
        Color a=new Color(54,39,65),b=new Color(91,70,96);
        for(int y=0;y<height;y+=tile)for(int x=0;x<width;x+=tile){
            g.setColor((((x/tile)+(y/tile))&1)==0?a:b);g.fillRect(x,y,Math.min(tile,width-x),Math.min(tile,height-y));
        }
    }

    private static void writeComparisons(BufferedImage before,BufferedImage after,Path directory)throws IOException {
        Files.createDirectories(directory);
        int w=before.getWidth(),h=before.getHeight();
        for(String name:List.of("corner_tl","corner_tr","corner_bl","corner_br","jewel_top","jewel_bottom")){
            BufferedImage oldCrop=cropForPart(before,name),newCrop=cropForPart(after,name);
            int gap=8;
            BufferedImage compare=new BufferedImage(oldCrop.getWidth()+newCrop.getWidth()+gap,Math.max(oldCrop.getHeight(),newCrop.getHeight()),BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=compare.createGraphics();g.setColor(new Color(13,9,19));g.fillRect(0,0,compare.getWidth(),compare.getHeight());
            g.drawImage(oldCrop,0,0,null);g.drawImage(newCrop,oldCrop.getWidth()+gap,0,null);g.dispose();
            write(compare,directory.resolve(name+"_before_after.png"));
            oldCrop.flush();newCrop.flush();compare.flush();
        }
        // Keep a full-scale render pair for direct inspection; the detailed crops above isolate the affected joins.
        if(before.getWidth()!=after.getWidth()||before.getHeight()!=after.getHeight())throw new IOException("Review image sizes differ");
    }

    private static void writeFullComparison(BufferedImage before,BufferedImage after,Path path)throws IOException {
        int halfWidth=before.getWidth()/2,halfHeight=before.getHeight()/2,header=24;
        BufferedImage result=new BufferedImage(halfWidth*2,halfHeight+header,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();g.setColor(new Color(20,13,27));g.fillRect(0,0,result.getWidth(),result.getHeight());
        g.setColor(new Color(237,224,205));g.setFont(new java.awt.Font("SansSerif",java.awt.Font.BOLD,13));
        g.drawString("BEFORE: committed runtime + old crops/tiling",8,17);
        g.drawString("AFTER: isolated crops + continuous rails",halfWidth+8,17);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(before,0,header,halfWidth,halfHeight,null);
        g.drawImage(after,halfWidth,header,halfWidth*2,halfHeight+header,0,0,after.getWidth(),after.getHeight(),null);
        g.dispose();write(result,path);result.flush();
    }

    private static BufferedImage cropForPart(BufferedImage image,String name) {
        int w=image.getWidth(),h=image.getHeight(),cx=w/2,size=190;
        int x,y,cropW,cropH;
        switch(name){
            case "corner_tl" -> {x=0;y=0;cropW=size;cropH=size;}
            case "corner_tr" -> {x=w-size;y=0;cropW=size;cropH=size;}
            case "corner_bl" -> {x=0;y=h-size;cropW=size;cropH=size;}
            case "corner_br" -> {x=w-size;y=h-size;cropW=size;cropH=size;}
            case "jewel_top" -> {x=cx-100;y=0;cropW=200;cropH=size;}
            default -> {x=cx-100;y=h-size;cropW=200;cropH=size;}
        }
        BufferedImage crop=new BufferedImage(cropW,cropH,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=crop.createGraphics();g.drawImage(image,0,0,cropW,cropH,x,y,x+cropW,y+cropH,null);g.dispose();
        return crop;
    }

    private record Box(int x,int y,int w,int h){int right(){return x+w;}int bottom(){return y+h;}}

    private static void screenshotCrop(BufferedImage source, Path path, int x, int y, int w, int h, int scale) throws IOException {
        int cx=Math.max(0,Math.min(x,source.getWidth()-1)), cy=Math.max(0,Math.min(y,source.getHeight()-1));
        int cw=Math.min(w,source.getWidth()-cx), ch=Math.min(h,source.getHeight()-cy);
        BufferedImage crop=new BufferedImage(cw,ch,BufferedImage.TYPE_INT_ARGB);
        for(int yy=0;yy<ch;yy++) for(int xx=0;xx<cw;xx++) crop.setRGB(xx,yy,source.getRGB(cx+xx,cy+yy));
        BufferedImage enlarged=new BufferedImage(cw*scale,ch*scale,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=enlarged.createGraphics(); g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(crop,0,0,enlarged.getWidth(),enlarged.getHeight(),null); g.dispose(); write(enlarged,path);
        crop.flush(); enlarged.flush();
    }

    private static void printSamples(BufferedImage image,String name){
        int[][] points={{0,0},{0,80},{0,159},{159,0},{159,80},{159,159},{40,40},{80,80},{120,120},{150,150},{16,16},{48,48}};
        System.out.println("  samples "+name+":");
        for(int[] p:points){int x=Math.min(image.getWidth()-1,p[0]),y=Math.min(image.getHeight()-1,p[1]),v=image.getRGB(x,y);System.out.printf("    %3d,%3d %08X%n",x,y,v);}
    }

    private static BufferedImage read(Path path) throws IOException {
        BufferedImage image=ImageIO.read(path.toFile());
        if(image==null) throw new IOException("Not a PNG: "+path);
        return image;
    }
    private static void write(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if(!ImageIO.write(image,"png",path.toFile())) throw new IOException("PNG writer unavailable");
    }
}
