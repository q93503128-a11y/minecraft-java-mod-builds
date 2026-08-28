package io.github.q93503128.turnbound.client;

import java.util.*;

public final class ClientBattleState {
    public record Unit(String id,String defId,String side,String name,int hp,int maxHp,int barrier,long gauge,boolean downed){}
    public record Skill(String id,String name,String targetRule,int baseCooldown,int remaining){}
    public record Snapshot(boolean active,boolean auto,int speed,String outcome,String actorId,boolean finished,List<Unit> units,List<String> timeline,List<Skill> skills,String message){}
    private static volatile Snapshot snapshot=new Snapshot(false,false,1,"RUNNING","",true,List.of(),List.of(),List.of(),""); private static volatile long revision;
    private ClientBattleState(){} public static Snapshot snapshot(){return snapshot;} public static long revision(){return revision;}
    public static void update(String raw){ boolean active=false,auto=false,finished=false;int speed=1;String outcome="RUNNING",actor="",msg="";List<Unit> units=new ArrayList<>();List<String> timeline=new ArrayList<>();List<Skill> skills=new ArrayList<>();
        for(String line:raw.split("\n")){if(line.isBlank())continue;String[] p=line.split("\\|",-1);try{switch(p[0]){
            case "H"->{active="1".equals(p[1]);auto="1".equals(p[2]);speed=Integer.parseInt(p[3]);outcome=p[4];actor=p[5];finished="1".equals(p[6]);}
            case "U"->units.add(new Unit(p[1],p[2],p[3],p[4],Integer.parseInt(p[5]),Integer.parseInt(p[6]),Integer.parseInt(p[7]),Long.parseLong(p[8]),"1".equals(p[9])));
            case "T"->{if(p.length>1&&!p[1].isBlank())timeline.addAll(Arrays.asList(p[1].split(",")));}
            case "S"->skills.add(new Skill(p[1],p[2],p[3],Integer.parseInt(p[4]),Integer.parseInt(p[5])));
            case "M"->msg=p[1]+" · "+p[2]+" → "+p[3]+(p[4].equals("0")?"":" · "+p[4]); default->{} } }catch(RuntimeException ignored){} }
        snapshot=new Snapshot(active,auto,speed,outcome,actor,finished,List.copyOf(units),List.copyOf(timeline),List.copyOf(skills),msg); revision++;
    }
}
