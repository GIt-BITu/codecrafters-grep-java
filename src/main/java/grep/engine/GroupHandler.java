package grep.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Group-level helpers: alternation handling etc.
 */
final class GroupHandler {
    private GroupHandler() {}

    static boolean handleAlternation(
            String input, int i, String pattern, int openParen, int closeParen,
            int patternEnd, boolean anchoredEnd, Map<Integer, String> groups, int groupIndex) {

        int insideStart = openParen + 1;
        List<int[]> options = PatternUtils.splitTopLevelIndices(pattern, insideStart, closeParen, '|');

        int suffixStart = closeParen + 1;

        for (int[] opt : options) {
            int optStart = opt[0];
            int optEnd = opt[1];
            Map<Integer, String> newGroups = new HashMap<>(groups);
            for (int len = 0; len <= input.length() - i; len++) {
                String candidate = input.substring(i, i + len);
                if (MatcherState.matchPattern(candidate, 0, pattern, optStart, optEnd, true, newGroups)) {
                    newGroups.put(groupIndex, candidate);
                    if (MatcherState.matchPattern(input, i + len, pattern, suffixStart, patternEnd, anchoredEnd, newGroups)) {
                        groups.clear();
                        groups.putAll(newGroups);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
