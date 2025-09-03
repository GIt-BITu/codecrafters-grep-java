package grep.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core recursion entrypoint delegated from RegexEngine. This file holds the primary recursive
 * matching loop but delegates token/group/quantifier concerns to TokenMatcher, GroupHandler, and
 * QuantifierHandler.
 */
final class MatcherState {

  private MatcherState() {}

  static boolean matchPattern(
      String input,
      int i,
      String pattern,
      int j,
      int patternEnd,
      boolean anchoredEnd,
      Map<Integer, String> groups) {

    // top-level alternation optimization: split by '|' at the top level
    List<int[]> topParts = PatternUtils.splitTopLevelIndices(pattern, j, patternEnd, '|');
    if (topParts.size() > 1) {
      for (int[] part : topParts) {
        int partStart = part[0], partEnd = part[1];
        Map<Integer, String> cloned = new HashMap<>(groups);
        if (matchPattern(input, i, pattern, partStart, partEnd, anchoredEnd, cloned)) {
          groups.clear();
          groups.putAll(cloned);
          return true;
        }
      }
      return false;
    }

    if (j == patternEnd) {
      return !anchoredEnd || i == input.length();
    }
    if (i > input.length()) return false;

    char pc = pattern.charAt(j);

    boolean hasPlus, hasQuestion, hasStar;
    if (pc == '\\') {
      hasPlus = (j + 2 < patternEnd && pattern.charAt(j + 2) == '+');
      hasQuestion = (j + 2 < patternEnd && pattern.charAt(j + 2) == '?');
      hasStar = (j + 2 < patternEnd && pattern.charAt(j + 2) == '*');
    } else {
      hasPlus = (j + 1 < patternEnd && pattern.charAt(j + 1) == '+');
      hasQuestion = (j + 1 < patternEnd && pattern.charAt(j + 1) == '?');
      hasStar = (j + 1 < patternEnd && pattern.charAt(j + 1) == '*');
    }

    // Groups / alternation inside groups
    if (pc == '(') {
      int closing = PatternUtils.findClosingParen(pattern, j);
      int insideStart = j + 1;
      boolean topLevelAlt = PatternUtils.hasTopLevelDelimiter(pattern, insideStart, closing, '|');
      char next = (closing + 1 < patternEnd) ? pattern.charAt(closing + 1) : '\0';
      Integer groupIndexObj = PatternUtils.getGroupIndexForParen(j);
      int groupIndex = groupIndexObj != null ? groupIndexObj : (groups.size() + 1);

      if (topLevelAlt) {
        if (next == '+') {
          return QuantifierHandler.handleGroupRepeat(
              input,
              i,
              pattern,
              j,
              closing,
              patternEnd,
              anchoredEnd,
              groups,
              groupIndex,
              1,
              Integer.MAX_VALUE);
        } else if (next == '?') {
          return QuantifierHandler.handleGroupRepeat(
              input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 0, 1);
        } else if (next == '*') {
          return QuantifierHandler.handleGroupRepeat(
              input,
              i,
              pattern,
              j,
              closing,
              patternEnd,
              anchoredEnd,
              groups,
              groupIndex,
              0,
              Integer.MAX_VALUE);
        } else {
          return GroupHandler.handleAlternation(
              input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex);
        }
      } else {
        if (next == '+') {
          return QuantifierHandler.handleGroupRepeat(
              input,
              i,
              pattern,
              j,
              closing,
              patternEnd,
              anchoredEnd,
              groups,
              groupIndex,
              1,
              Integer.MAX_VALUE);
        } else if (next == '?') {
          return QuantifierHandler.handleGroupRepeat(
              input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 0, 1);
        } else if (next == '*') {
          return QuantifierHandler.handleGroupRepeat(
              input,
              i,
              pattern,
              j,
              closing,
              patternEnd,
              anchoredEnd,
              groups,
              groupIndex,
              0,
              Integer.MAX_VALUE);
        } else {
          // inline the group: try all candidate lengths
          for (int len = 0; len <= input.length() - i; len++) {
            String candidate = input.substring(i, i + len);
            Map<Integer, String> newGroups = new HashMap<>(groups);
            if (matchPattern(candidate, 0, pattern, insideStart, closing, true, newGroups)) {
              newGroups.put(groupIndex, candidate);
              if (matchPattern(
                  input, i + len, pattern, closing + 1, patternEnd, anchoredEnd, newGroups)) {
                groups.clear();
                groups.putAll(newGroups);
                return true;
              }
            }
          }
          return false;
        }
      }
    }

    // escapes
    if (pc == '\\') {
      if (hasPlus) {
        return QuantifierHandler.handleSingleTokenRepeat(
            input, i, pattern, j, patternEnd, anchoredEnd, groups, 1, Integer.MAX_VALUE, true);
      } else if (hasQuestion) {
        return QuantifierHandler.handleSingleTokenRepeat(
            input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, 1, true);
      } else if (hasStar) {
        return QuantifierHandler.handleSingleTokenRepeat(
            input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, Integer.MAX_VALUE, true);
      } else {
        return TokenMatcher.handleEscape(input, i, pattern, j, patternEnd, anchoredEnd, groups);
      }
    }

    // character class
    if (pc == '[') {
      return TokenMatcher.handleCharacterClass(
          input, i, pattern, j, patternEnd, anchoredEnd, groups);
    }

    // quantifiers for a single token
    if (hasPlus) {
      return QuantifierHandler.handleSingleTokenRepeat(
          input, i, pattern, j, patternEnd, anchoredEnd, groups, 1, Integer.MAX_VALUE, false);
    } else if (hasQuestion) {
      return QuantifierHandler.handleSingleTokenRepeat(
          input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, 1, false);
    } else if (hasStar) {
      return QuantifierHandler.handleSingleTokenRepeat(
          input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, Integer.MAX_VALUE, false);
    }

    // literal or dot
    if (i < input.length() && TokenMatcher.charMatches(input.charAt(i), pc)) {
      return matchPattern(input, i + 1, pattern, j + 1, patternEnd, anchoredEnd, groups);
    }
    return false;
  }
}
