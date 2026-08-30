package io.github.q93503128.turnbound.world;

public final class MetaUiCodec {
    private MetaUiCodec() {}

    public static String encode(MetaUiSnapshot snapshot) {
        StringBuilder out = new StringBuilder();
        out.append("H|").append(snapshot.gold()).append('|').append(snapshot.crystal()).append('|')
                .append(snapshot.starEssence()).append('|').append(snapshot.awakeningCore()).append('|')
                .append(snapshot.partyCp()).append('|').append(snapshot.riftUnlocked() ? 1 : 0).append('\n');
        out.append("P|").append(String.join(",", snapshot.activeParty())).append('\n');
        for (var row : snapshot.characters()) out.append("C|").append(row.id()).append('|').append(safe(row.name())).append('|')
                .append(row.level()).append('|').append(row.star()).append('|').append(row.awakened() ? 1 : 0).append('|')
                .append(row.cp()).append('|').append(row.active() ? 1 : 0).append('\n');
        for (var row : snapshot.endgame()) out.append("E|").append(row.id()).append('|').append(row.kind()).append('|')
                .append(safe(row.label())).append('|').append(row.unlocked() ? 1 : 0).append('|').append(row.cleared() ? 1 : 0)
                .append('|').append(row.level()).append('|').append(row.hardPattern() ? 1 : 0).append('\n');
        for (var row : snapshot.challenges()) out.append("X|").append(row.id()).append('|').append(row.ordinal()).append('|')
                .append(safe(row.label())).append('|').append(row.completed() ? 1 : 0).append('|')
                .append(row.autoEvaluable() ? 1 : 0).append('|').append(safe(row.unresolvedReason())).append('\n');
        for (var row : snapshot.regionQuests()) out.append("Q|").append(row.id()).append('|').append(row.region()).append('|')
                .append(row.objectiveSpecified() ? 1 : 0).append('|').append(row.completed() ? 1 : 0).append('|')
                .append(safe(row.chestRule())).append('\n');
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ' ).replace('\r', ' ');
    }
}
