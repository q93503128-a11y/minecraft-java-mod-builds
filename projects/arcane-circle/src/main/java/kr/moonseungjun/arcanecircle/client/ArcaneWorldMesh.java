package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Hybrid filled-and-edge world mesh. Broad luminous plates carry the spell body; lines are accents. */
final class ArcaneWorldMesh {
    private final List<Segment> segments;
    private final List<Face> faces;
    private final float lineScale;
    private final float lineFloor;
    ArcaneWorldMesh(List<Segment> segments,List<Face> faces,float lineScale,float lineFloor){this.segments=List.copyOf(segments);this.faces=List.copyOf(faces);this.lineScale=lineScale;this.lineFloor=lineFloor;}
    int size(){return segments.size()+faces.size()*2;}

    void submit(PoseStack poseStack,SubmitNodeCollector collector,int argb,float windowScale){
        if(faces.isEmpty()&&segments.isEmpty())return;
        if(!faces.isEmpty())collector.submitCustomGeometry(poseStack,RenderTypes.debugFilledBox(),(pose,out)->{
            for(Face face:faces){int color=tone(argb,face.brightness,face.alpha);vertex(out,pose,face.a,color);vertex(out,pose,face.b,color);vertex(out,pose,face.c,color);vertex(out,pose,face.d,color);}
        });
        if(!segments.isEmpty()){
            // Three edge passes create a saturated halo + readable mid edge + white-hot colored core.
            // This keeps the effect punchy without relying on thousands of vanilla particles.
            submitLines(poseStack,collector,tone(argb,.58,.34),windowScale*4.60F);
            submitLines(poseStack,collector,tone(argb,.82,.76),windowScale*2.20F);
            submitLines(poseStack,collector,tone(argb,.98,1.0),windowScale*.78F);
        }
    }
    private void submitLines(PoseStack stack,SubmitNodeCollector collector,int color,float scale){collector.submitCustomGeometry(stack,RenderTypes.lines(),(pose,out)->{for(Segment s:segments){Vec3 d=s.end.subtract(s.start);if(d.lengthSqr()<1e-8)continue;Vec3 n=d.normalize();float w=Math.max(lineFloor,s.width*scale*lineScale);out.addVertex(pose,(float)s.start.x,(float)s.start.y,(float)s.start.z).setColor(color).setNormal(pose,(float)n.x,(float)n.y,(float)n.z).setLineWidth(w);out.addVertex(pose,(float)s.end.x,(float)s.end.y,(float)s.end.z).setColor(color).setNormal(pose,(float)n.x,(float)n.y,(float)n.z).setLineWidth(w);}});}
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 v,int color){out.addVertex(pose,(float)v.x,(float)v.y,(float)v.z).setColor(color);}
    private static final double VIVID_SATURATION=2.05;
    private static final double FACE_ALPHA_BOOST=1.78;
    private static int tone(int argb,double brightness,double alphaScale){
        int baseA=(argb>>>24)&255,baseR=(argb>>>16)&255,baseG=(argb>>>8)&255,baseB=argb&255;
        // Luma-based saturation keeps the dominant school color strong instead of bleaching it
        // toward pastel white when brightness is raised.
        double luma=baseR*.2126+baseG*.7152+baseB*.0722;
        int a=(int)Math.round(baseA*Math.min(1.0,alphaScale*FACE_ALPHA_BOOST));
        int r=(int)Math.round((luma+(baseR-luma)*VIVID_SATURATION)*brightness);
        int g=(int)Math.round((luma+(baseG-luma)*VIVID_SATURATION)*brightness);
        int b=(int)Math.round((luma+(baseB-luma)*VIVID_SATURATION)*brightness);
        return(clamp(a)<<24)|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);
    }
    private static int clamp(int v){return Math.max(0,Math.min(255,v));}
    static Builder builder(int budget){return new Builder(budget,1.0F,.72F);}
    static Builder fineBuilder(int budget){return new Builder(budget,.46F,.34F);}
    record Segment(Vec3 start,Vec3 end,float width){}
    record Face(Vec3 a,Vec3 b,Vec3 c,Vec3 d,float brightness,float alpha){}

    static final class Builder{
        private final int budget;private final float lineScale,lineFloor;private final List<Segment> segments=new ArrayList<>();private final List<Face> faces=new ArrayList<>();
        Builder(int budget,float lineScale,float lineFloor){this.budget=Math.max(8,budget);this.lineScale=Math.max(.1F,lineScale);this.lineFloor=Math.max(.1F,lineFloor);}
        int size(){return segments.size()+faces.size()*2;}boolean full(){return size()>=budget;}ArcaneWorldMesh build(){return new ArcaneWorldMesh(segments,faces,lineScale,lineFloor);}
        Builder line(Vec3 a,Vec3 b,float width){if(!full()&&a!=null&&b!=null&&a.distanceToSqr(b)>1e-8)segments.add(new Segment(a,b,width));return this;}
        Builder face(Vec3 a,Vec3 b,Vec3 c,Vec3 d,float brightness,float alpha){if(!full())faces.add(new Face(a,b,c,d,brightness,alpha));return this;}
        Builder triangle(Vec3 a,Vec3 b,Vec3 c,float brightness,float alpha){return face(a,b,c,c,brightness,alpha);}
        Builder polyline(List<Vec3> p,float width,boolean closed){if(p==null||p.size()<2)return this;for(int i=1;i<p.size()&&!full();i++)line(p.get(i-1),p.get(i),width);if(closed&&!full())line(p.getLast(),p.getFirst(),width);return this;}
        Builder arc(Basis basis,Vec3 center,double radius,double start,double sweep,int count,float width){Vec3 prev=center.add(basis.point(start,radius));for(int i=1;i<=Math.max(2,count)&&!full();i++){Vec3 cur=center.add(basis.point(start+sweep*i/Math.max(2,count),radius));line(prev,cur,width);prev=cur;}return this;}
        Builder circle(Basis basis,Vec3 center,double radius,int count,float width){return arc(basis,center,radius,-Math.PI/2,Math.PI*2,count,width);}
        Builder band(Basis basis,Vec3 center,double inner,double outer,int count,float brightness,float alpha){int n=Math.max(12,count);for(int i=0;i<n&&!full();i++){double a=-Math.PI/2+Math.PI*2*i/n,b=-Math.PI/2+Math.PI*2*(i+1)/n;face(center.add(basis.point(a,outer)),center.add(basis.point(b,outer)),center.add(basis.point(b,inner)),center.add(basis.point(a,inner)),brightness,alpha);}return this;}
        Builder brokenBand(Basis basis,Vec3 center,double inner,double outer,int count,int period,float brightness,float alpha){int n=Math.max(12,count);for(int i=0;i<n&&!full();i++){if(Math.floorMod(i,Math.max(2,period))==0)continue;double a=-Math.PI/2+Math.PI*2*i/n,b=-Math.PI/2+Math.PI*2*(i+1)/n;face(center.add(basis.point(a,outer)),center.add(basis.point(b,outer)),center.add(basis.point(b,inner)),center.add(basis.point(a,inner)),brightness,alpha);}return this;}
        Builder disc(Basis basis,Vec3 center,double radius,int count,float brightness,float alpha){int n=Math.max(12,count);for(int i=0;i<n&&!full();i++){double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;triangle(center,center.add(basis.point(a,radius)),center.add(basis.point(b,radius)),brightness,alpha);}return this;}
        Builder polygon(Basis basis,Vec3 center,double radius,int sides,double rotation,float width){int n=Math.max(3,sides);List<Vec3> p=new ArrayList<>();for(int i=0;i<n;i++)p.add(center.add(basis.point(rotation+Math.PI*2*i/n,radius)));return polyline(p,width,true);}
        Builder polygonPlate(Basis basis,Vec3 center,double radius,int sides,double rotation,float brightness,float alpha){int n=Math.max(3,sides);for(int i=0;i<n&&!full();i++){Vec3 a=center.add(basis.point(rotation+Math.PI*2*i/n,radius)),b=center.add(basis.point(rotation+Math.PI*2*(i+1)/n,radius));triangle(center,a,b,brightness,alpha);}return this;}
        Builder diamond(Basis basis,Vec3 center,double radius,double rotation,float brightness,float alpha){return polygonPlate(basis,center,radius,4,rotation+Math.PI/4,brightness,alpha);}
        Builder star(Basis basis,Vec3 center,double outer,double inner,int points,double rotation,float width){List<Vec3> p=starPoints(basis,center,outer,inner,points,rotation);return polyline(p,width,true);}
        Builder starPlate(Basis basis,Vec3 center,double outer,double inner,int points,double rotation,float brightness,float alpha){List<Vec3> p=starPoints(basis,center,outer,inner,points,rotation);for(int i=0;i<p.size()&&!full();i++)triangle(center,p.get(i),p.get((i+1)%p.size()),brightness,alpha);return this;}
        private static List<Vec3> starPoints(Basis basis,Vec3 center,double outer,double inner,int points,double rotation){int n=Math.max(3,points);List<Vec3> p=new ArrayList<>();for(int i=0;i<n*2;i++)p.add(center.add(basis.point(rotation+Math.PI*i/n,(i&1)==0?outer:inner)));return p;}
        Builder runeChords(Basis basis,Vec3 center,double radius,int count,int skip,double rotation,float width){int n=Math.max(3,count),s=Math.max(1,Math.min(n-1,skip));List<Vec3> p=new ArrayList<>();for(int i=0;i<n;i++)p.add(center.add(basis.point(rotation+Math.PI*2*i/n,radius)));for(int i=0;i<n&&!full();i++)line(p.get(i),p.get((i+s)%n),width*(i%3==0?1.45F:.72F));return this;}
        Builder runeGlyph(Basis basis,Vec3 center,double size,int seed,double rotation,float width){
            Vec3 x=basis.point(rotation,size),y=basis.point(rotation+Math.PI/2.0,size);
            Vec3 nx=x.scale(-1),ny=y.scale(-1);
            int pattern=Math.floorMod(seed,8);
            line(center.add(nx),center.add(x),width);
            if((pattern&1)!=0)line(center.add(ny),center.add(y),width*.82F);
            if((pattern&2)!=0)line(center.add(nx.scale(.75)).add(ny.scale(.70)),center.add(x.scale(.70)).add(y.scale(.78)),width*.72F);
            if((pattern&4)!=0)line(center.add(nx.scale(.72)).add(y.scale(.68)),center.add(x.scale(.78)).add(ny.scale(.65)),width*.72F);
            double cap=size*.55;
            if(pattern%3==0)diamond(basis,center.add(y.scale(.72)),cap*.48,rotation,1.16F,.42F);
            else if(pattern%3==1)polygon(basis,center.add(x.scale(.42)),cap*.46,3,rotation+.35,width*.68F);
            else circle(basis,center.add(ny.scale(.58)),cap*.34,10,width*.62F);
            return this;
        }
        Builder runeRing(Basis basis,Vec3 center,double radius,int count,double size,int seed,double rotation,float width){
            int n=Math.max(4,count);
            for(int i=0;i<n&&!full();i++){
                double a=rotation+Math.PI*2.0*i/n;
                Vec3 at=center.add(basis.point(a,radius));
                runeGlyph(basis,at,size,seed+i,a+Math.PI/2.0,width*(i%5==0?1.22F:.76F));
            }
            return this;
        }
        Builder sphere(Vec3 center,double radius,int detail,float width){orb(center,radius,Math.max(16,18+detail*3),.92F,.34F);circle(Basis.ground(),center,radius,24+detail*3,width);circle(Basis.facing(new Vec3(1,0,0)),center,radius,24+detail*3,width*.72F);return this;}
        Builder orb(Vec3 center,double radius,int detail,float brightness,float alpha){disc(Basis.ground(),center,radius,detail,brightness,alpha*.55F);disc(Basis.facing(new Vec3(1,0,0)),center,radius,detail,brightness,alpha);disc(Basis.facing(new Vec3(0,0,1)),center,radius,detail,brightness*.85F,alpha*.75F);return this;}
        Builder shard(Vec3 center,Vec3 axis,Basis basis,double length,double radius,float brightness,float alpha){Vec3 n=axis.lengthSqr()<1e-8?new Vec3(0,0,1):axis.normalize();Vec3 front=center.add(n.scale(length*.55)),back=center.subtract(n.scale(length*.45));int sides=6;for(int i=0;i<sides&&!full();i++){Vec3 a=center.add(basis.point(Math.PI*2*i/sides,radius)),b=center.add(basis.point(Math.PI*2*(i+1)/sides,radius));triangle(front,a,b,brightness,alpha);triangle(back,b,a,brightness*.72F,alpha*.85F);}return this;}
        Builder beamPrism(Vec3 origin,Vec3 axis,Basis basis,double length,double radius,float brightness,float alpha){Vec3 n=axis.lengthSqr()<1e-8?new Vec3(0,0,1):axis.normalize(),end=origin.add(n.scale(length));Vec3 r=basis.right.scale(radius),u=basis.up.scale(radius);Vec3[] a={origin.add(r).add(u),origin.subtract(r).add(u),origin.subtract(r).subtract(u),origin.add(r).subtract(u)};Vec3[] b={end.add(r).add(u),end.subtract(r).add(u),end.subtract(r).subtract(u),end.add(r).subtract(u)};for(int i=0;i<4&&!full();i++)face(a[i],a[(i+1)%4],b[(i+1)%4],b[i],brightness*(i%2==0?1F:.72F),alpha);face(b[0],b[1],b[2],b[3],brightness*1.15F,alpha);return this;}
        Builder helix(Vec3 origin,Vec3 axis,Basis basis,double length,double radius,int turns,int count,float width,boolean taper){Vec3 n=axis.lengthSqr()<1e-8?new Vec3(0,0,1):axis.normalize();int c=Math.max(8,count);Vec3 prev=origin;for(int i=1;i<=c&&!full();i++){double t=i/(double)c,rr=taper?radius*Math.sin(Math.PI*t):radius;Vec3 cur=origin.add(n.scale(length*t)).add(basis.point(Math.PI*2*turns*t,rr));line(prev,cur,width*(i%5==0?1.35F:.72F));prev=cur;}return this;}
        Builder ribbon(Vec3 origin,Vec3 axis,Basis basis,double length,double width,int waves,int count,float brightness,float alpha){Vec3 n=axis.lengthSqr()<1e-8?new Vec3(0,0,1):axis.normalize();int c=Math.max(8,count);Vec3 la=null,lb=null;for(int i=0;i<=c&&!full();i++){double t=i/(double)c;double wave=Math.sin(t*Math.PI*2*waves)*width*.42;Vec3 center=origin.add(n.scale(length*t)).add(basis.up.scale(wave));Vec3 a=center.add(basis.right.scale(width*(.18+.12*Math.sin(Math.PI*t)))),b=center.subtract(basis.right.scale(width*(.18+.12*Math.sin(Math.PI*t))));if(la!=null)face(la,lb,b,a,(float)(brightness*(.78+.22*Math.sin(Math.PI*t))),alpha);la=a;lb=b;}return this;}
        Builder cone(Vec3 origin,Vec3 axis,Basis basis,double length,double endRadius,int ribs,int rings,float width){Vec3 n=axis.lengthSqr()<1e-8?new Vec3(0,0,1):axis.normalize();int r=Math.max(3,ribs);for(int i=0;i<r&&!full();i++){Vec3 a=origin.add(n.scale(length)).add(basis.point(Math.PI*2*i/r,endRadius)),b=origin.add(n.scale(length)).add(basis.point(Math.PI*2*(i+1)/r,endRadius));triangle(origin,a,b,.88F,.16F);line(origin,a,width*(i%2==0?1.1F:.55F));}for(int i=1;i<=Math.max(1,rings)&&!full();i++){double t=i/(double)Math.max(1,rings);band(basis,origin.add(n.scale(length*t)),endRadius*t*.88,endRadius*t,Math.max(18,r*4),.95F,.22F);}return this;}
    }

    record Basis(Vec3 right,Vec3 up){
        Basis{right=right.lengthSqr()<1e-8?new Vec3(1,0,0):right.normalize();up=up.lengthSqr()<1e-8?new Vec3(0,1,0):up.normalize();}
        static Basis ground(){return new Basis(new Vec3(1,0,0),new Vec3(0,0,1));}
        static Basis facing(Vec3 normal){Vec3 n=normal.lengthSqr()<1e-8?new Vec3(0,0,1):normal.normalize();Vec3 ref=Math.abs(n.y)>.92?new Vec3(1,0,0):new Vec3(0,1,0);Vec3 r=n.cross(ref).normalize();return new Basis(r,r.cross(n).normalize());}
        static Basis fromNormal(Vec3 normal,Vec3 hint){Vec3 n=normal.lengthSqr()<1e-8?new Vec3(0,1,0):normal.normalize();Vec3 h=hint.lengthSqr()<1e-8?new Vec3(1,0,0):hint.normalize();Vec3 r=n.cross(h);if(r.lengthSqr()<1e-8)r=n.cross(new Vec3(0,0,1));r=r.normalize();return new Basis(r,r.cross(n).normalize());}
        Vec3 point(double angle,double radius){return right.scale(Math.cos(angle)*radius).add(up.scale(Math.sin(angle)*radius));}
        Vec3 normal(){Vec3 n=right.cross(up);return n.lengthSqr()<1e-8?new Vec3(0,1,0):n.normalize();}
    }
}
