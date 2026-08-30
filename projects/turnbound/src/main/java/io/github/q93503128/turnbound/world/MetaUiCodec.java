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
                .append(row.subType()).append('|').append(row.subValue()).append('|').append(row.mainAt20()).append('|').append(row.subAt20()).append('\n');
        for (var row : snapshot.endgame()) out.append("E|").append(row.id()).append('|').append(row.kind()).append('|')
                .append(safe(row.label())).append('|').append(row.unlocked()?1:0).append('|').append(row.cleared()?1:0)
                .append('|').append(row.level()).append('|').append(row.hardPattern()?1:0).append('\n');
        for (var row : snapshot.challenges()) out.append("X|").append(row.id()).append('|').append(row.ordinal()).append('|')
                .append(safe(row.label())).append('|').append(row.completed()?1:0).append('|').append(row.autoEvaluable()?1:0)
                .append('|').append(safe(row.unresolvedReason())).append('\n');
        for (var row : snapshot.regionQuests()) out.append("Q|").append(row.id()).append('|').append(row.region()).append('|')
                .append(row.objectiveSpecified()?1:0).append('|').append(row.completed()?1:0).append('|').append(safe(row.chestRule())).append('\n');
        for (var row : snapshot.archiveHistory()) out.append("A|").append(row.characterId()).append('|').append(safe(row.name())).append('|')
                .append(row.nativeStars()).append('|').append(row.newlyOwned()?1:0).append('|').append(row.essenceGranted()).append('|').append(row.pityAfter()).append('\n');
        for (var row : snapshot.shopItems()) out.append("S|").append(row.itemId()).append('|').append(safe(row.name())).append('|')
                .append(row.tier()).append('|').append(row.slot()).append('|').append(row.price()).append('|').append(row.unlocked()?1:0).append('\n');
        for (var row : snapshot.codex()) out.append("D|").append(row.category()).append('|').append(row.id()).append('|').append(safe(row.name())).append('|')
                .append(row.discovered()?1:0).append('|').append(row.detailUnlocked()?1:0).append('|').append(safe(row.summary())).append('\n');
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
}
