package io.github.q93503128.turnbound.world;

public final class MetaUiCodec {
    private MetaUiCodec() {}

    public static String encode(MetaUiSnapshot snapshot) {
        StringBuilder out = new StringBuilder();
        out.append("H|").append(snapshot.gold()).append('|').append(snapshot.crystal()).append('|')
                .append(snapshot.starEssence()).append('|').append(snapshot.awakeningCore()).append('|')
                .append(snapshot.partyCp()).append('|').append(snapshot.riftUnlocked() ? 1 : 0).append('|')
                .append(snapshot.fiveStarPity()).append('|').append(snapshot.starterArchiveAvailable() ? 1 : 0).append('\n');
        out.append("P|").append(String.join(",", snapshot.activeParty())).append('\n');
        for (int i = 0; i < snapshot.partyPresets().size(); i++) {
            out.append("PP|").append(i + 1).append('|').append(String.join(",", snapshot.partyPresets().get(i))).append('\n');
        }
        for (var row : snapshot.characters()) out.append("C|").append(row.id()).append('|').append(safe(row.name())).append('|')
                .append(row.owned()?1:0).append('|').append(row.nativeStar()).append('|').append(row.level()).append('|').append(row.star()).append('|')
                .append(row.awakened()?1:0).append('|').append(row.cp()).append('|').append(row.active()?1:0).append('|')
                .append(safe(row.role())).append('|').append(safe(row.primaryRole())).append('|').append(safe(row.difficulty())).append('|')
                .append(row.profileUnlocked()?1:0).append('|').append(row.hp()).append('|').append(row.attack()).append('|')
                .append(row.defense()).append('|').append(row.speed()).append('\n');
        for (var row : snapshot.equipment()) out.append("I|").append(row.instanceId()).append('|').append(row.itemId()).append('|')
                .append(safe(row.name())).append('|').append(row.tier()).append('|').append(row.slot()).append('|').append(row.enhancement()).append('|')
                .append(row.equippedCharacterId()).append('|').append(row.mainType()).append('|').append(row.mainValue()).append('|')
                .append(row.subType()).append('|').append(row.subValue()).append('|').append(row.mainAt20()).append('|').append(row.subAt20()).append('|')
                .append(row.salePrice()).append('|').append(row.sellable()?1:0).append('\n');
        for (var row : snapshot.pendingEquipment()) out.append("IR|").append(row.instanceId()).append('|').append(row.itemId()).append('|')
                .append(safe(row.name())).append('|').append(row.tier()).append('|').append(row.slot()).append('|').append(row.salePrice()).append('|')
                .append(row.claimable()?1:0).append('|').append(row.immediateSellable()?1:0).append('\n');
        for (var row : snapshot.endgame()) out.append("E|").append(row.id()).append('|').append(row.kind()).append('|')
                .append(safe(row.label())).append('|').append(row.unlocked()?1:0).append('|').append(row.cleared()?1:0)
                .append('|').append(row.level()).append('|').append(row.hardPattern()?1:0).append('\n');
        for (var row : snapshot.challenges()) out.append("X|").append(row.id()).append('|').append(row.ordinal()).append('|')
                .append(safe(row.label())).append('|').append(row.completed()?1:0).append('|').append(row.autoEvaluable()?1:0)
                .append('|').append(safe(row.unresolvedReason())).append('\n');
        // Region quest IDs are server progression keys. The client only needs a readable title for this passive list.
        for (var row : snapshot.regionQuests()) out.append("Q|").append(safe(regionQuestTitle(row.id(), row.region()))).append('|')
                .append(safe(row.region())).append('|').append(row.objectiveSpecified()?1:0).append('|')
                .append(row.completed()?1:0).append('|').append(safe(row.chestRule())).append('\n');
        for (var row : snapshot.archiveHistory()) out.append("A|").append(row.characterId()).append('|').append(safe(row.name())).append('|')
                .append(row.nativeStars()).append('|').append(row.newlyOwned()?1:0).append('|').append(row.essenceGranted()).append('|').append(row.pityAfter()).append('\n');
        for (var row : snapshot.shopItems()) out.append("S|").append(row.itemId()).append('|').append(safe(row.name())).append('|')
                .append(row.tier()).append('|').append(row.slot()).append('|').append(row.price()).append('|').append(row.unlocked()?1:0).append('\n');
        for (var row : snapshot.codex()) out.append("D|").append(row.category()).append('|').append(row.id()).append('|').append(safe(row.name())).append('|')
                .append(row.discovered()?1:0).append('|').append(row.detailUnlocked()?1:0).append('|').append(safe(row.summary())).append('\n');
        return out.toString();
    }

    static String regionQuestTitle(String id, String region) {
        String key = id == null ? "" : id;
        String known = switch (key) {
            case "RQ_MEADOW_01" -> "초원의 잔향";
            case "RQ_GLOAM_01" -> "그늘 아래의 흔적";
            case "RQ_AQUEDUCT_01" -> "멈춘 수로의 기록";
            case "RQ_QUARRY_01" -> "재 속의 잔해";
            case "RQ_RELAY_01", "RQ_OLD_RELAY_01" -> "중계소의 잔류 신호";
            default -> null;
        };
        if (known != null) return known;
        return switch (region == null ? "" : region) {
            case "MEADOW" -> "남문 초원 지역 임무";
            case "GLOAMWOOD" -> "그늘숲 지역 임무";
            case "AQUEDUCT" -> "붕괴 수로 지역 임무";
            case "QUARRY" -> "잿불 채석장 지역 임무";
            case "OLD_RELAY", "OLD_RELAY_STATION" -> "구 중계소 지역 임무";
            default -> "지역 임무";
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
