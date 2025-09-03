package grep.engine;

import java.util.*;

/** Handles + ? * quantifiers for both single tokens and groups (greedy backtracking). */
final class QuantifierHandler {
  private QuantifierHandler() {}

  static boolean handleSingleTokenRepeat(
      String input,
      int i,
      String pattern,
      int j,
      int patternEnd,
      boolean anchoredEnd,
      Map<Integer, String> groups,
      int min,
      int max,
      boolean specialToken) {

    int tokenEndJ;
    if (specialToken) {
      int k = j + 1;
      if (k < pattern.length() && Character.isDigit(pattern.charAt(k))) {
        while (k < patternEnd && Character.isDigit(pattern.charAt(k))) k++;
        tokenEndJ = k - 1;
      } else {
        tokenEndJ = j + 1;
      }
    } else {
      tokenEndJ = j;
    }

    int k = i;
    int count = 0;
    while (k < input.length() && count < max) {
      if (specialToken) {
        if (!TokenMatcher.peekEscapeMatch(input, k, pattern, j, tokenEndJ + 1)) break;
      } else {
        if (!TokenMatcher.charMatches(input.charAt(k), pattern.charAt(j))) break;
      }
      k++;
      count++;
    }

    if (count < min) return false;

    int maxSplit = k;
    int minSplit = i + min;
    int nextPatternIndex = tokenEndJ + 2;
    for (int split = maxSplit; split >= minSplit; split--) {
      if (MatcherState.matchPattern(
          input,
          split,
          pattern,
          nextPatternIndex,
          patternEnd,
          anchoredEnd,
          new HashMap<>(groups))) {
        return true;
      }
    }
    return false;
  }

  static boolean handleGroupRepeat(
      String input,
      int i,
      String pattern,
      int openParen,
      int closeParen,
      int patternEnd,
      boolean anchoredEnd,
      Map<Integer, String> groups,
      int groupIndex,
      int minReps,
      int maxReps) {

    int unitStart = openParen + 1;
    int afterQuant = closeParen + 2;

    boolean unitCanBeEmpty =
        MatcherState.matchPattern("", 0, pattern, unitStart, closeParen, true, new HashMap<>());

    int theoreticalMax =
        unitCanBeEmpty
            ? Math.min(maxReps, (input.length() - i) + 1)
            : Math.min(maxReps, (input.length() - i));

    for (int reps = theoreticalMax; reps >= minReps; reps--) {
      for (int endPos :
          consumeUnitExactlyN(input, i, pattern, unitStart, closeParen, reps, unitCanBeEmpty)) {
        String captured = input.substring(i, endPos);
        Map<Integer, String> newGroups = new HashMap<>(groups);
        if (MatcherState.matchPattern(
            input, endPos, pattern, afterQuant, patternEnd, anchoredEnd, newGroups)) {
          newGroups.put(groupIndex, captured);
          groups.clear();
          groups.putAll(newGroups);
          return true;
        }
      }
    }
    return false;
  }

  private static List<Integer> consumeUnitExactlyN(
      String input,
      int pos,
      String pattern,
      int unitStart,
      int unitEnd,
      int n,
      boolean unitCanBeEmpty) {
    List<Integer> results = new ArrayList<>();
    if (n == 0) {
      results.add(pos);
      return results;
    }
    List<Integer> canedEnds = allUnitEnds(input, pos, pattern, unitStart, unitEnd);
    Collections.sort(canedEnds, Collections.reverseOrder());
    for (int end : canedEnds) {
      if (!unitCanBeEmpty && end == pos) continue;
      List<Integer> tails =
          consumeUnitExactlyN(input, end, pattern, unitStart, unitEnd, n - 1, unitCanBeEmpty);
      results.addAll(tails);
    }
    return results;
  }

  private static List<Integer> allUnitEnds(
      String input, int pos, String pattern, int unitStart, int unitEnd) {
    List<Integer> ends = new ArrayList<>();
    for (int len = input.length() - pos; len >= 0; len--) {
      String candidate = input.substring(pos, pos + len);
      if (MatcherState.matchPattern(
          candidate, 0, pattern, unitStart, unitEnd, true, new HashMap<>())) {
        ends.add(pos + len);
      }
    }
    return ends;
  }
}
