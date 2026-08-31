package io.github.q93503128.turnbound.world;

import java.util.Map;

/** Stable text bridge from Character Quest UI rows to their authored physical investigation sites. */
public final class CharacterQuestRouteCatalog {
    private static final Map<String,String> ROUTES=Map.of(
            "P01","그늘숲 동쪽 · 무너진 북문 초소",
            "P02","라디아 · Clock Tower 내부",
            "P03","라디아 · Barracks 대피기록 보관실",
            "P04","라디아 · Memorial Steps 구호 명부",
            "P05","남문 초원 동쪽 · 옛 사냥길 표식",
            "P06","라디아 · Memorial Steps 무명 기록대",
            "P07","그늘숲 서쪽 · 오래된 계약 제단",
            "P08","잿불 채석장 동쪽 · 붕괴 작업대");
    private CharacterQuestRouteCatalog(){}
    public static String route(String characterId){return ROUTES.getOrDefault(characterId,"라디아 Character Quest 기록");}
}
