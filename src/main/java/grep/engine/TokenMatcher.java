package grep.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Token-level matching utilities: charMatches, peekEscapeMatch, handleEscape, handleCharacterClass.
 */
final class TokenMatcher {
  private TokenMatcher() {}

  static boolean charMatches(char textChar, char patternChar) {
    return patternChar == '.' || textChar == patternChar;
  }

  static boolean peekEscapeMatch(
      String input, int pos, String pattern, int j, int tokenTargetEndExclusive) {
    if (pos >= input.length()) return false;
    if (j + 1 >= tokenTargetEndExclusive) return false;
    char first = pattern.charAt(j + 1);
    char c = input.charAt(pos);
    if (Character.isDigit(first)) {
      return false;
    }
    return switch (first) {
      case 'd' -> Character.isDigit(c);
      case 'w' -> (Character.isLetterOrDigit(c) || c == '_');
      default -> c == first;
    };
  }

  static boolean handleEscape(
      String input,
      int i,
      String pattern,
      int j,
      int patternEnd,
      boolean anchoredEnd,
      Map<Integer, String> groups) {
    if (j + 1 >= patternEnd) throw new RuntimeException("Dangling escape in pattern");
    if (i > input.length()) return false;

    int k = j + 1;
    if (Character.isDigit(pattern.charAt(k))) {
      int groupNum = 0;
      while (k < patternEnd && Character.isDigit(pattern.charAt(k))) {
        groupNum = groupNum * 10 + (pattern.charAt(k) - '0');
        k++;
      }
      String captured = groups.get(groupNum);
      if (captured == null) return false;
      if (i + captured.length() <= input.length() && input.startsWith(captured, i)) {
        return MatcherState.matchPattern(
            input, i + captured.length(), pattern, k, patternEnd, anchoredEnd, groups);
      }
      return false;
    }

    char escapeType = pattern.charAt(j + 1);
    if (i >= input.length()) return false;
    char c = input.charAt(i);

    return switch (escapeType) {
      case 'd' ->
          Character.isDigit(c)
              && MatcherState.matchPattern(
                  input, i + 1, pattern, j + 2, patternEnd, anchoredEnd, groups);
      case 'w' ->
          (Character.isLetterOrDigit(c) || c == '_')
              && MatcherState.matchPattern(
                  input, i + 1, pattern, j + 2, patternEnd, anchoredEnd, groups);
      default ->
          c == escapeType
              && MatcherState.matchPattern(
                  input, i + 1, pattern, j + 2, patternEnd, anchoredEnd, groups);
    };
  }

  static boolean handleCharacterClass(
      String input,
      int i,
      String pattern,
      int j,
      int patternEnd,
      boolean anchoredEnd,
      Map<Integer, String> groups) {

    int closingBracket = PatternUtils.findClosingBracket(pattern, j);
    if (closingBracket == -1 || closingBracket >= patternEnd) {
      throw new RuntimeException("Unclosed character class: " + pattern);
    }

    String charGroup = pattern.substring(j + 1, closingBracket);
    boolean negate = false;
    if (charGroup.startsWith("^")) {
      negate = true;
      charGroup = charGroup.substring(1);
    }

    char quant = '\0';
    if (closingBracket + 1 < patternEnd) {
      char maybe = pattern.charAt(closingBracket + 1);
      if (maybe == '+' || maybe == '?' || maybe == '*') quant = maybe;
    }

    BiPredicate<Character, String> matches =
        (ch, grp) -> {
          for (int idx = 0; idx < grp.length(); idx++) {
            if (idx + 2 < grp.length() && grp.charAt(idx + 1) == '-') {
              char start = grp.charAt(idx);
              char end = grp.charAt(idx + 2);
              if (ch >= start && ch <= end) return true;
              idx += 2;
            } else {
              if (ch == grp.charAt(idx)) return true;
            }
          }
          return false;
        };

    if (quant == '+') {
      int k = i;
      while (k < input.length()) {
        boolean m = matches.test(input.charAt(k), charGroup);
        if (negate) m = !m;
        if (!m) break;
        k++;
      }
      if (k == i) return false;
      for (int split = k; split >= i + 1; split--) {
        if (MatcherState.matchPattern(
            input,
            split,
            pattern,
            closingBracket + 2,
            patternEnd,
            anchoredEnd,
            new HashMap<>(groups))) {
          return true;
        }
      }
      return false;
    } else if (quant == '?') {
      if (MatcherState.matchPattern(
          input, i, pattern, closingBracket + 2, patternEnd, anchoredEnd, new HashMap<>(groups))) {
        return true;
      }

      if (i >= input.length()) return false;

      boolean m = matches.test(input.charAt(i), charGroup);

      if (negate) m = !m;

      if (!m) return false;

      return MatcherState.matchPattern(
          input, i + 1, pattern, closingBracket + 2, patternEnd, anchoredEnd, groups);
    } else if (quant == '*') {
      int k = i;
      while (k < input.length()) {
        boolean m = matches.test(input.charAt(k), charGroup);
        if (negate) m = !m;
        if (!m) break;
        k++;
      }
      for (int split = k; split >= i; split--) {
        if (MatcherState.matchPattern(
            input,
            split,
            pattern,
            closingBracket + 2,
            patternEnd,
            anchoredEnd,
            new HashMap<>(groups))) {
          return true;
        }
      }
      return false;
    } else {
      if (i >= input.length()) return false;
      boolean m = matches.test(input.charAt(i), charGroup);
      if (negate) m = !m;
      if (!m) return false;
      return MatcherState.matchPattern(
          input, i + 1, pattern, closingBracket + 1, patternEnd, anchoredEnd, groups);
    }
  }
}
