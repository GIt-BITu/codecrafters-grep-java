package grep.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility helpers: findClosingParen, findClosingBracket, splitTopLevelIndices, paren-to-group map
 * management.
 */
final class PatternUtils {
  private static final Map<Integer, Integer> parenToGroupNum = new HashMap<>();
  private static int totalGroupCount = 0;

  private PatternUtils() {}

  static void buildParenIndexMap(String pattern) {
    parenToGroupNum.clear();
    totalGroupCount = 0;
    boolean inCharClass = false;
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (c == '\\') {
        i++;
        continue;
      }
      if (inCharClass) {
        if (c == ']') inCharClass = false;
        continue;
      }
      if (c == '[') {
        inCharClass = true;
        continue;
      }
      if (c == '(') {
        totalGroupCount++;
        parenToGroupNum.put(i, totalGroupCount);
      }
    }
  }

  static Integer getGroupIndexForParen(int pos) {
    return parenToGroupNum.get(pos);
  }

  static int findClosingBracket(String pattern, int start) {
    for (int k = start + 1; k < pattern.length(); k++) {
      char c = pattern.charAt(k);
      if (c == '\\') {
        k++;
        continue;
      }
      if (c == ']') return k;
    }
    return -1;
  }

  static int findClosingParen(String pattern, int start) {
    int depth = 0;
    boolean inCharClass = false;
    for (int k = start; k < pattern.length(); k++) {
      char c = pattern.charAt(k);
      if (c == '\\') {
        k++;
        continue;
      }
      if (inCharClass) {
        if (c == ']') inCharClass = false;
        continue;
      }
      if (c == '[') {
        inCharClass = true;
        continue;
      }
      if (c == '(') depth++;
      else if (c == ')') {
        depth--;
        if (depth == 0) return k;
      }
    }
    throw new RuntimeException("Unmatched ( in pattern: " + pattern);
  }

  static boolean hasTopLevelDelimiter(String pattern, int start, int end, char delimiter) {
    int depth = 0;
    boolean inCharClass = false;
    for (int k = start; k < end; k++) {
      char c = pattern.charAt(k);
      if (c == '\\') {
        k++;
        continue;
      }
      if (inCharClass) {
        if (c == ']') inCharClass = false;
        continue;
      }
      if (c == '[') {
        inCharClass = true;
        continue;
      }
      if (c == '(') depth++;
      else if (c == ')') depth--;
      else if (c == delimiter && depth == 0) return true;
    }
    return false;
  }

  static List<int[]> splitTopLevelIndices(String pattern, int start, int end, char delimiter) {
    List<int[]> parts = new ArrayList<>();
    int depth = 0;
    boolean inCharClass = false;
    int currentStart = start;

    for (int k = start; k < end; k++) {
      char c = pattern.charAt(k);
      if (c == '\\') {
        k++;
        continue;
      }
      if (inCharClass) {
        if (c == ']') inCharClass = false;
        continue;
      }
      if (c == '[') {
        inCharClass = true;
        continue;
      }
      if (c == '(') {
        depth++;
        continue;
      }
      if (c == ')') {
        depth--;
        continue;
      }
      if (c == delimiter && depth == 0) {
        parts.add(new int[] {currentStart, k});
        currentStart = k + 1;
      }
    }
    parts.add(new int[] {currentStart, end});
    return parts;
  }
}
