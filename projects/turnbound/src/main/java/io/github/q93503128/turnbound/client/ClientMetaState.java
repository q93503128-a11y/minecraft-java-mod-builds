package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ClientMetaState {
    public record CharacterRow(String id,String name,boolean owned,int nativeStar,int level,int star,boolean awakened,int cp,boolean active,
                               String role,String primaryRole,String difficulty,boolean profileUnlocked,int hp,int attack,int defense,int speed) {}
    public record EquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int enhancement,String equippedCharacterId,
                               String mainType,double mainValue,String subType,double subValue,double mainAt20,double subAt20,
                               int salePrice,boolean sellable) {}
    public record EndgameRow(String id,String kind,String label,boolean unlocked,boolean cleared,int level,boolean hardPattern) {}
    public record ChallengeRow(String id,int ordinal,String label,boolean completed,boolean autoEvaluable,String unresolvedReason) {}
    public record RegionQuestRow(String id,String region,boolean objectiveSpecified,boolean completed,String chestRule) {}
    public record ArchiveRow(String characterId,String name,int nativeStars,boolean newlyOwned,int essenceGranted,int pityAfter) {}
    public record ShopRow(String itemId,String name,String tier,String slot,int price,boolean unlocked) {}
    public record CodexRow(String category,String id,String name,boolean discovered,boolean detailUnlocked,String summary) {}

    public record Snapshot(long gold,long crystal,long essence,long core,int partyCp,boolean riftUnlocked,
                           int fiveStarPity,boolean starterArchiveAvailable,
                           List<String> activeParty,List<List<String>> partyPresets,
                           List<CharacterRow> characters,List<EquipmentRow> equipment,List<EndgameRow> endgame,
                           List<ChallengeRow> challenges,List<RegionQuestRow> regionQuests,
                           List<ArchiveRow> archiveHistory,List<ShopRow> shopItems,List<CodexRow> codex) {
        public Snapshot {
            activeParty=List.copyOf(activeParty); partyPresets=partyPresets.stream().map(List::copyOf).toList();
            characters=List.copyOf(characters); equipment=List.copyOf(equipment); endgame=List.copyOf(endgame);
            challenges=List.copyOf(challenges); regionQuests=List.copyOf(regionQuests);
            archiveHistory=List.copyOf(archiveHistory); shopItems=List.copyOf(shopItems); codex=List.copyOf(codex);
        }
        public static Snapshot empty(){return new Snapshot(0,0,0,0,0,false,0,false,List.of(),List.of(List.of(),List.of(),List.of()),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
    }

    private static volatile Snapshot snapshot=Snapshot.empty();
    private ClientMetaState(){}
    public static Snapshot snapshot(){return snapshot;}

    public static void update(String raw){
        long gold=0,crystal=0,essence=0,core=0; int cp=0,pity=0; boolean rift=false,starter=false;
        List<String> party=new ArrayList<>(); List<List<String>> presets=new ArrayList<>(List.of(List.of(),List.of(),List.of()));
        List<CharacterRow> chars=new ArrayList<>(); List<EquipmentRow> equipment=new ArrayList<>(); List<EndgameRow> endgame=new ArrayList<>();
        List<ChallengeRow> challenges=new ArrayList<>(); List<RegionQuestRow> regions=new ArrayList<>();
        List<ArchiveRow> archive=new ArrayList<>(); List<ShopRow> shop=new ArrayList<>(); List<CodexRow> codex=new ArrayList<>();
        for(String line:raw.split("\n")){
            if(line.isBlank())continue; String[] p=line.split("\\|",-1);
            try{
                switch(p[0]){
                    case "H"->{gold=Long.parseLong(p[1]);crystal=Long.parseLong(p[2]);essence=Long.parseLong(p[3]);core=Long.parseLong(p[4]);cp=Integer.parseInt(p[5]);rift="1".equals(p[6]);if(p.length>7)pity=Integer.parseInt(p[7]);if(p.length>8)starter="1".equals(p[8]);}
                    case "P"->{if(p.length>1&&!p[1].isBlank())party.addAll(Arrays.asList(p[1].split(",")));}
                    case "PP"->{int slot=Integer.parseInt(p[1])-1;if(slot>=0&&slot<3)presets.set(slot,p.length>2&&!p[2].isBlank()?List.of(p[2].split(",")):List.of());}
                    case "C"->chars.add(new CharacterRow(p[1],p[2],"1".equals(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]),Integer.parseInt(p[6]),
                            "1".equals(p[7]),Integer.parseInt(p[8]),"1".equals(p[9]),p[10],p[11],p[12],"1".equals(p[13]),
                            Integer.parseInt(p[14]),Integer.parseInt(p[15]),Integer.parseInt(p[16]),Integer.parseInt(p[17])));
                    case "I"->equipment.add(new EquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),p[7],p[8],Double.parseDouble(p[9]),p[10],Double.parseDouble(p[11]),Double.parseDouble(p[12]),Double.parseDouble(p[13]),p.length>14?Integer.parseInt(p[14]):0,p.length>15&&"1".equals(p[15])));
                    case "E"->endgame.add(new EndgameRow(p[1],p[2],p[3],"1".equals(p[4]),"1".equals(p[5]),Integer.parseInt(p[6]),"1".equals(p[7])));
                    case "X"->challenges.add(new ChallengeRow(p[1],Integer.parseInt(p[2]),p[3],"1".equals(p[4]),"1".equals(p[5]),p.length>6?p[6]:""));
                    case "Q"->regions.add(new RegionQuestRow(p[1],p[2],"1".equals(p[3]),"1".equals(p[4]),p.length>5?p[5]:""));
                    case "A"->archive.add(new ArchiveRow(p[1],p[2],Integer.parseInt(p[3]),"1".equals(p[4]),Integer.parseInt(p[5]),Integer.parseInt(p[6])));
                    case "S"->shop.add(new ShopRow(p[1],p[2],p[3],p[4],Integer.parseInt(p[5]),"1".equals(p[6])));
                    case "D"->codex.add(new CodexRow(p[1],p[2],p[3],"1".equals(p[4]),"1".equals(p[5]),p.length>6?p[6]:""));
                    default->{}
                }
            }catch(RuntimeException ignored){}
        }
        snapshot=new Snapshot(gold,crystal,essence,core,cp,rift,pity,starter,party,presets,chars,equipment,endgame,challenges,regions,archive,shop,codex);
    }
}
