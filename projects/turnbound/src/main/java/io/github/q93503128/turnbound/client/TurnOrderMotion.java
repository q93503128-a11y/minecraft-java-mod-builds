package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure planner for short Turn Order rail transitions.
 *
 * Matching starts with an LCS so ordinary turn consumption keeps later duplicate portraits attached
 * to the correct occurrence. Remaining identical IDs are then paired by nearest slot, which preserves
 * visible jumps caused by Gauge manipulation instead of snapping the token to its destination.
 */
final class TurnOrderMotion {
    record Move(String unitId, int fromIndex, int toIndex) {
        int delta() { return fromIndex - toIndex; }
        boolean tempoJump() { return Math.abs(delta()) > 1; }
    }

    private TurnOrderMotion() {}

    static List<Move> plan(List<String> before, List<String> after, int limit) {
        if (limit <= 0 || after == null || after.isEmpty()) return List.of();
        List<String> a = clip(before, limit);
        List<String> b = clip(after, limit);
        if (a.isEmpty()) {
            List<Move> fresh = new ArrayList<>();
            for (int j = 0; j < b.size(); j++) fresh.add(new Move(b.get(j), j, j));
            return List.copyOf(fresh);
        }

        int[][] dp = lcs(a, b);
        int[] fromForAfter = new int[b.size()];
        Arrays.fill(fromForAfter, -1);
        boolean[] usedBefore = new boolean[a.size()];

        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (a.get(i).equals(b.get(j)) && dp[i][j] == 1 + dp[i + 1][j + 1]) {
                fromForAfter[j] = i;
                usedBefore[i] = true;
                i++; j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }

        // Recover genuine reorder jumps omitted by the monotonic LCS match.
        for (j = 0; j < b.size(); j++) {
            if (fromForAfter[j] >= 0) continue;
            int best = -1, bestDistance = Integer.MAX_VALUE;
            for (i = 0; i < a.size(); i++) {
                if (usedBefore[i] || !a.get(i).equals(b.get(j))) continue;
                int distance = Math.abs(i - j);
                if (distance < bestDistance) {
                    best = i;
                    bestDistance = distance;
                }
            }
            if (best >= 0) {
                fromForAfter[j] = best;
                usedBefore[best] = true;
            }
        }

        List<Move> out = new ArrayList<>();
        for (j = 0; j < b.size(); j++) {
            int from = fromForAfter[j] >= 0 ? fromForAfter[j] : j;
            out.add(new Move(b.get(j), from, j));
        }
        return List.copyOf(out);
    }

    static Move moveAt(List<Move> moves, int toIndex) {
        if (moves == null || toIndex < 0 || toIndex >= moves.size()) return null;
        Move move = moves.get(toIndex);
        return move.toIndex() == toIndex ? move : null;
    }

    static double easedProgress(long elapsedMs, long durationMs) {
        if (durationMs <= 0) return 1.0;
        double t = Math.max(0.0, Math.min(1.0, elapsedMs / (double) durationMs));
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
    }

    private static int[][] lcs(List<String> a, List<String> b) {
        int[][] dp = new int[a.size() + 1][b.size() + 1];
        for (int i = a.size() - 1; i >= 0; i--) {
            for (int j = b.size() - 1; j >= 0; j--) {
                dp[i][j] = a.get(i).equals(b.get(j))
                        ? 1 + dp[i + 1][j + 1]
                        : Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        return dp;
    }

    private static List<String> clip(List<String> input, int limit) {
        if (input == null || input.isEmpty()) return List.of();
        return List.copyOf(input.subList(0, Math.min(limit, input.size())));
    }
}
