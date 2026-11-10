package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Converts legacy rank arrays to completed graph stages without losing SP or unrelated growth. */
public final class GraphSkillTreeMigration {
    private GraphSkillTreeMigration() {}

    public static boolean migrateAll(PlayerClassProfile profile) {
        EnumMap<ClassId, ClassProgress> migrated = new EnumMap<>(ClassId.class);
        boolean changed = false;
        for (ClassId classId : ClassId.values()) {
            ClassProgress progress = profile.progress(classId);
            if (progress.skillTreeVersion() > GraphSkillTreeCatalog.CURRENT_VERSION) {
                throw new IllegalStateException("Unsupported future skill-tree save version for " + classId.key());
            }
            if (progress.skillTreeVersion() == GraphSkillTreeCatalog.CURRENT_VERSION) {
                validateCurrent(classId, progress);
                continue;
            }
            migrated.put(classId, convertLegacy(classId, progress));
            changed = true;
        }
        if (changed) profile.updateProgressBatch(migrated);
        return changed;
    }

    private static ClassProgress convertLegacy(ClassId classId, ClassProgress old) {
        if (!old.purchasedNodes().isEmpty()) {
            throw new IllegalStateException("Unversioned graph nodes cannot be safely migrated for " + classId.key());
        }
        var legacyNodes = SkillTreeCatalog.nodes(classId);
        int[] ranks = old.skillRanks();
        if (ranks.length != 0 && ranks.length != legacyNodes.size()) {
            throw new IllegalStateException("Cannot safely migrate legacy rank data for " + classId.key());
        }
        if (ranks.length == 0) ranks = new int[legacyNodes.size()];

        GraphSkillTreeCatalog.Tree tree = GraphSkillTreeCatalog.tree(classId);
        Set<String> purchased = new HashSet<>();
        long refunded = 0;
        long completedCost = 0;
        for (int index = 0; index < legacyNodes.size(); index++) {
            SkillTreeCatalog.Node oldNode = legacyNodes.get(index);
            int rank = ranks[index];
            if (rank > oldNode.maxRank()) {
                throw new IllegalStateException("Legacy rank exceeds its maximum for " + classId.key() + "/" + oldNode.id());
            }
            GraphSkillTreeCatalog.Skill skill = tree.skills().get(oldNode.id());
            if (skill == null) throw new IllegalStateException("Missing graph mapping for legacy skill " + oldNode.id());
            long mappedSpend = 0;
            for (GraphSkillTreeCatalog.Segment segment : skill.segments()) {
                if (rank >= segment.oldRankTo()) {
                    purchased.addAll(segment.minorNodeIds());
                    purchased.add(segment.majorNodeId());
                    completedCost += segment.oldSegmentSp();
                    mappedSpend += segment.oldSegmentSp();
                } else if (rank >= segment.oldRankFrom()) {
                    int partial = oldNode.costFromTo(segment.oldRankFrom() - 1, rank);
                    refunded += partial;
                    mappedSpend += partial;
                }
            }
            int oldSkillSpend = oldNode.costFromTo(0, rank);
            if (mappedSpend != oldSkillSpend) {
                throw new IllegalStateException("Legacy skill spend does not map exactly for " + classId.key() + "/" + oldNode.id());
            }
        }
        int convertedSpend = GraphSkillTreeCatalog.spent(classId, purchased);
        if (convertedSpend != completedCost) throw new IllegalStateException("Migration graph spend mismatch for " + classId.key());
        long newPoints = (long) old.skillPoints() + refunded;
        long beforeTotal = (long) old.skillPoints() + SkillTreeCatalog.spent(classId, ranks);
        long afterTotal = newPoints + convertedSpend;
        if (newPoints < 0 || newPoints > Integer.MAX_VALUE || beforeTotal != afterTotal) {
            throw new IllegalStateException("Migration SP conservation failed for " + classId.key());
        }
        validateOwnedGraph(classId, purchased, (int) newPoints);
        return old.withGraphTree((int) newPoints, purchased, GraphSkillTreeCatalog.CURRENT_VERSION);
    }

    private static void validateCurrent(ClassId classId, ClassProgress progress) {
        if (progress.skillRanks().length != 0) {
            throw new IllegalStateException("Graph save contains legacy ranks for " + classId.key());
        }
        validateOwnedGraph(classId, progress.purchasedNodes(), progress.skillPoints());
    }

    private static void validateOwnedGraph(ClassId classId, Set<String> purchased, int points) {
        var tree = GraphSkillTreeCatalog.tree(classId);
        GraphSkillTreeCatalog.spent(classId, purchased);
        for (String id : purchased) {
            GraphSkillTreeCatalog.Node node = tree.node(id);
            if (node == null || !node.purchasable()) throw new IllegalStateException("Invalid owned skill node " + id);
            if (node.requiresPreviousMajor() != null && !purchased.contains(node.requiresPreviousMajor())) {
                throw new IllegalStateException("Owned node is missing its previous stage: " + id);
            }
            if (!purchased.containsAll(node.requiresStageMinors())) {
                throw new IllegalStateException("Owned node is missing its stage path: " + id);
            }
        }
        long total = (long) points + GraphSkillTreeCatalog.spent(classId, purchased);
        if (total > Integer.MAX_VALUE) throw new IllegalStateException("Skill point total overflow for " + classId.key());
    }
}
