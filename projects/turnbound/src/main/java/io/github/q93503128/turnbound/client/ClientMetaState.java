package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ClientMetaState {
    public record CharacterRow(String id,String name,boolean owned,int nativeStar,int level,int star,boolean awakened,int cp,boolean active,
                               String role,String primaryRole,String difficulty,boolean profileUnlocked,int hp,int attack,int defense,int speed) {}
    public record EquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int enhancement,String equippedCharacterId,
                               String mainType,double mainValue,String subType,double subValue,double mainAt20,double subAt20,
                               int salePrice,boolean sellable) {}
    public record PendingEquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int salePrice,boolean claimable,boolean immediateSellable) {}
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
                           List<ArchiveRow> archiveHistory,List<ShopRow> shopItems,List<CodexRow> codex,List<PendingEquipmentRow> pendingEquipment) {
        public Snapshot {
            activeParty=List.copyOf(activeParty); partyPresets=partyPresets.stream().map(List::copyOf).toList();
            characters=List.copyOf(characters); equipment=List.copyOf(equipment); endgame=List.copyOf(endgame);
            challenges=List.copyOf(challenges); regionQuests=List.copyOf(regionQuests);
            archiveHistory=List.copyOf(archiveHistory); shopItems=List.copyOf(shopItems); codex=List.copyOf(codex);
            pendingEquipment=List.copyOf(pendingEquipment);
        }
        public static Snapshot empty(){return new Snapshot(0,0,0,0,0,false,0,false,List.of(),List.of(List.of(),List.of(),List.of()),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
    }

    private static volatile Snapshot snapshot=Snapshot.empty();
    private static volatile long revision;
    private ClientMetaState(){}
    public static Snapshot snapshot(){return snapshot;}
    public static long revision(){return revision;}

    public static void update(String raw){
        long gold=0,crystal=0,essence=0,core=0; int cp=0,pity=0; boolean rift=false,starter=false;
        List<String> party=new ArrayList<>(); List<List<String>> presets=new ArrayList<>(List.of(List.of(),List.of(),List.of()));
        List<CharacterRow> chars=new ArrayList<>(); List<EquipmentRow> equipment=new ArrayList<>(); List<EndgameRow> endgame=new ArrayList<>();
        List<ChallengeRow> challenges=new ArrayList<>(); List<RegionQuestRow> regions=new ArrayList<>();
        List<ArchiveRow> archive=new ArrayList<>(); List<ShopRow> shop=new ArrayList<>(); List<CodexRow> codex=new ArrayList<>();
        List<PendingEquipmentRow> pendingEquipment=new ArrayList<>();
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
                    case "IR"->pendingEquipment.add(new PendingEquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),"1".equals(p[7]),"1".equals(p[8])));
                    case "E"->endgame.add(new EndgameRow(p[1],p[2],playerEndgameLabel(p[1],p[2],p[3]),"1".equals(p[4]),"1".equals(p[5]),Integer.parseInt(p[6]),"1".equals(p[7])));
                    case "X"->challenges.add(new ChallengeRow(p[1],Integer.parseInt(p[2]),playerChallengeLabel(p[1],Integer.parseInt(p[2]),p[3]),"1".equals(p[4]),"1".equals(p[5]),p.length>6?p[6]:""));
                    case "Q"->regions.add(new RegionQuestRow(p[1],playerRegionLabel(p[2]),"1".equals(p[3]),"1".equals(p[4]),p.length>5?p[5]:""));
                    case "A"->archive.add(new ArchiveRow(p[1],p[2],Integer.parseInt(p[3]),"1".equals(p[4]),Integer.parseInt(p[5]),Integer.parseInt(p[6])));
                    case "S"->shop.add(new ShopRow(p[1],p[2],p[3],p[4],Integer.parseInt(p[5]),"1".equals(p[6])));
                    case "D"->codex.add(new CodexRow(p[1],p[2],p[3],"1".equals(p[4]),"1".equals(p[5]),p.length>6?p[6]:""));
                    default->{}
                }
            }catch(RuntimeException ignored){}
        }
        snapshot=new Snapshot(gold,crystal,essence,core,cp,rift,pity,starter,party,presets,chars,equipment,endgame,challenges,regions,archive,shop,codex,pendingEquipment);
        revision++;
    }

    private static String playerRegionLabel(String region) {
        return switch (region) {
            case "MEADOW" -> "남문 초원";
            case "GLOAMWOOD" -> "그늘숲";
            case "AQUEDUCT" -> "붕괴 수로";
            case "QUARRY" -> "잿불 채석장";
            case "OLD_RELAY", "OLD_RELAY_STATION" -> "구 중계소";
            default -> region;
        };
    }

    private static String playerEndgameLabel(String id, String kind, String fallback) {
        if ("RIFT".equals(kind)) {
            int floor = trailingNumber(id);
            return floor > 0 ? "균열 관문 " + floor + "층" : "균열 관문";
        }
        if ("HARD".equals(kind)) {
            String bossId = id != null && id.startsWith("HARD_") ? id.substring("HARD_".length()) : "";
            if (CanonicalData.contains(bossId)) return CanonicalData.definition(bossId).name() + " · 하드";
            return fallback == null ? "하드 보스" : fallback.replace(" Hard", " · 하드");
        }
        return fallback == null ? "" : fallback;
    }

    private static String playerChallengeLabel(String id, int ordinal, String fallback) {
        return switch (ordinal) {
            case 1 -> "전투불능 없이 승리 1";
            case 2 -> "전투불능 없이 승리 2";
            case 3 -> "아군 행동 12회 미만으로 승리";
            case 4 -> "아군 행동 20회 미만으로 승리";
            case 5 -> "1회 부활 후 승리";
            case 6 -> "반격 5회";
            case 7 -> "추격 6회";
            case 8 -> "행동 게이지 지연 합계 800";
            case 9 -> "보호막으로 피해 1500 흡수";
            case 10 -> "한 전투에서 2000 회복";
            case 11 -> "아군 HP 10% 미만 상태로 승리";
            case 12 -> canonicalName("E003", "특수 적") + " 폭발 전에 처치";
            case 13 -> canonicalName("E003", "특수 적") + " 폭발에서 생존";
            case 14 -> "엘리트 적을 부활 없이 격파";
            case 15 -> canonicalName("B01", "보스 1") + " 하드 격파";
            case 16 -> canonicalName("B02", "보스 2") + " 하드 격파";
            case 17 -> canonicalName("B03", "보스 3") + " 하드 격파";
            case 18 -> canonicalName("B04", "보스 4") + " 하드 격파";
            case 19 -> canonicalName("B05", "보스 5") + " 하드 격파";
            case 20 -> "균열 관문 30층 클리어";
            default -> fallback == null || fallback.isBlank() ? "도전 " + ordinal : fallback;
        };
    }

    private static String canonicalName(String id, String fallback) {
        return CanonicalData.contains(id) ? CanonicalData.definition(id).name() : fallback;
    }

    private static int trailingNumber(String id) {
        if (id == null || id.isBlank()) return -1;
        int start = id.length();
        while (start > 0 && Character.isDigit(id.charAt(start - 1))) start--;
        if (start == id.length()) return -1;
        try { return Integer.parseInt(id.substring(start)); }
        catch (NumberFormatException ignored) { return -1; }
    }
}
