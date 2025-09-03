package grep.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Public entry for the hand-rolled regex engine. No use of java.util.regex inside matching logic.
 */
public final class RegexEngine {
  private RegexEngine() {}

  /**
   * Public matcher: returns true if a pattern matches anywhere in input. The group map will be
   * populated with capture group strings if matches occur.
   */
  public static boolean matchPatternAnywhere(
      String input, String pattern, Map<Integer, String> groups) {
    // ensure a paren map for this pattern built (safe if already built)
    PatternUtils.buildParenIndexMap(pattern);

    boolean anchoredStart = pattern.startsWith("^");
    boolean anchoredEnd = pattern.endsWith("$");
    int patternStart = anchoredStart ? 1 : 0;
    int patternEnd = anchoredEnd ? pattern.length() - 1 : pattern.length();

    if (anchoredStart) {
      return MatcherState.matchPattern(
          input, 0, pattern, patternStart, patternEnd, anchoredEnd, groups);
    }

    for (int i = 0; i <= input.length(); i++) {
      if (MatcherState.matchPattern(
          input, i, pattern, patternStart, patternEnd, anchoredEnd, new HashMap<>(groups))) {
        return true;
      }
    }
    return false;
  }
}
