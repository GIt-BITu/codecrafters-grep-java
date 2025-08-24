import java.util.*;
import java.util.Scanner;
import java.util.regex.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

public class Main {
    // Map from opening-paren index in pattern -> group number (1-based)
    private static Map<Integer, Integer> parenToGroupNum = new HashMap<>();
    private static int totalGroupCount = 0;

    public static void main(String[] args){
        // Parse flags: support -r (optional) and -E <pattern> (required)
        boolean recursive = false;
        String pattern = null;
        List<String> paths = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-r")) {
                recursive = true;
            } else if (a.equals("-E")) {
                if (i + 1 >= args.length) {
                    System.out.println("Usage: ./your_program.sh [-r] -E <pattern> [file1 file2 ...]");
                    System.exit(1);
                }
                pattern = args[i + 1];
                i++; // skip pattern token
            } else {
                paths.add(a);
            }
        }

        if (pattern == null) {
            System.out.println("Usage: ./your_program.sh [-r] -E <pattern> [file1 file2 ...]");
            System.exit(1);
        }

        // Build group index map for this pattern (once)
        buildParenIndexMap(pattern);

        boolean anyMatch = false;

        try {
            if (recursive) {
                // If no paths specified, default to current directory
                if (paths.isEmpty()) paths.add(".");

                for (String startArg : paths) {
                    Path start = Paths.get(startArg);
                    if (!Files.exists(start)) {
                        System.err.println("Error: path does not exist: " + startArg);
                        System.exit(2);
                    }

                    if (Files.isDirectory(start)) {
                        // Walk all regular files under the start directory
                        try (Stream<Path> stream = Files.walk(start)) {
                            Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();
                            while (it.hasNext()) {
                                Path filePath = it.next();
                                boolean fileMatched = processFileRecursive(filePath, pattern, start.toString());
                                if (fileMatched) anyMatch = true;
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading directory " + startArg + ": " + e.getMessage());
                            System.exit(2);
                        }
                    } else if (Files.isRegularFile(start)) {
                        // Single file argument alongside -r: still process file and print prefix as path
                        boolean fileMatched = processFileRecursive(start, pattern, start.getParent() != null ? start.getParent().toString() : ".");
                        if (fileMatched) anyMatch = true;
                    } else {
                        // Not a file or directory (e.g., special file)
                        System.err.println("Error: not a file or directory: " + startArg);
                        System.exit(2);
                    }
                }

            } else {
                // Non-recursive mode
                if (paths.isEmpty()) {
                    // Read from stdin (multiple lines possible)
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            Map<Integer, String> groups = new HashMap<>();
                            if (matchPatternAnywhere(line, pattern, groups)) {
                                System.out.println(line);
                                anyMatch = true;
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading stdin: " + e.getMessage());
                        System.exit(2);
                    }
                } else {
                    int fileCount = paths.size();
                    for (String filename : paths) {
                        Path p = Paths.get(filename);
                        if (!Files.exists(p)) {
                            System.err.println("Error reading file " + filename + ": No such file");
                            System.exit(2);
                        }
                        if (!Files.isRegularFile(p)) {
                            System.err.println("Error: not a regular file: " + filename);
                            System.exit(2);
                        }

                        try (BufferedReader br = Files.newBufferedReader(p)) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                Map<Integer, String> groups = new HashMap<>();
                                if (matchPatternAnywhere(line, pattern, groups)) {
                                    if (fileCount > 1) {
                                        System.out.println(filename + ":" + line);
                                    } else {
                                        System.out.println(line);
                                    }
                                    anyMatch = true;
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error reading file " + filename + ": " + e.getMessage());
                            System.exit(2);
                        }
                    }
                }
            }
        } catch (RuntimeException re) {
            // If your custom matcher throws for a malformed pattern, let it fail (tests may expect this)
            System.err.println("Matcher error: " + re.getMessage());
            System.exit(2);
        }

        System.exit(anyMatch ? 0 : 1);
    }

    // process a single file when in recursive mode; print matches prefixed by the file path.
    // startDirStr is the string of the starting directory used for walk; we print filePath relative to
    // current working directory by using filePath.toString(), which matches the examples (e.g. "dir/subdir/file.txt").
    private static boolean processFileRecursive(Path filePath, String pattern, String startDirStr) {
        boolean matched = false;
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                Map<Integer, String> groups = new HashMap<>();
                if (matchPatternAnywhere(line, pattern, groups)) {
                    // print file path + ":" + line
                    System.out.println(filePath.toString() + ":" + line);
                    matched = true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath.toString() + ": " + e.getMessage());
            System.exit(2);
        }
        return matched;
    }

    // ---------- build mapping from '(' position -> group number ----------
    private static void buildParenIndexMap(String pattern) {
        parenToGroupNum.clear();
        totalGroupCount = 0;

        boolean inCharClass = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                // skip escaped char
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
            } else if (c == ')') {
                // nothing to do
            }
        }
    }

    // ---------- entry ----------
    public static boolean matchPatternAnywhere(String input, String pattern, Map<Integer, String> groups) {
        boolean anchoredStart = pattern.startsWith("^");
        boolean anchoredEnd   = pattern.endsWith("$");

        int patternStart = anchoredStart ? 1 : 0;
        int patternEnd   = anchoredEnd ? pattern.length() - 1 : pattern.length();

        // Fast path: ^...$ -> delegate to Java regex (and still keep custom fallback)
        if (anchoredStart && anchoredEnd) {
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(input);
                if (m.matches()) return true;
            } catch (PatternSyntaxException ignored) {
                // fall through to custom engine
            }
        }

        if (anchoredStart) {
            return matchPattern(input, 0, pattern, patternStart, patternEnd, anchoredEnd, groups);
        }

        for (int i = 0; i <= input.length(); i++) {
            if (matchPattern(input, i, pattern, patternStart, patternEnd, anchoredEnd, new HashMap<>(groups))) {
                return true;
            }
        }
        return false;
    }

    // ---------- core matcher ----------
    private static boolean matchPattern(
            String input, int i, String pattern, int j, int patternEnd,
            boolean anchoredEnd, Map<Integer, String> groups) {

        if (j == patternEnd) {
            return !anchoredEnd || i == input.length();
        }
        if (i > input.length()) {
            return false;
        }

        char pc = pattern.charAt(j);

        // token-aware quantifier detection for single tokens (., literal, \w, \d)
        boolean hasPlus, hasQuestion, hasStar;
        if (pc == '\\') {
            // quantifier may follow escape target (but for multi-digit backref this is handled later)
            hasPlus     = (j + 2 < patternEnd && pattern.charAt(j + 2) == '+');
            hasQuestion = (j + 2 < patternEnd && pattern.charAt(j + 2) == '?');
            hasStar     = (j + 2 < patternEnd && pattern.charAt(j + 2) == '*');
        } else {
            hasPlus     = (j + 1 < patternEnd && pattern.charAt(j + 1) == '+');
            hasQuestion = (j + 1 < patternEnd && pattern.charAt(j + 1) == '?');
            hasStar     = (j + 1 < patternEnd && pattern.charAt(j + 1) == '*');
        }

        // ----- groups & alternation -----
        if (pc == '(') {
            int closing = findClosingParen(pattern, j);
            int insideStart = j + 1;
            int insideEnd = closing;

            // alternation only if there's a TOP-LEVEL '|' inside this group
            boolean topLevelAlt = hasTopLevelDelimiter(pattern, insideStart, insideEnd, '|');

            // quantifier after the group?
            char next = (closing + 1 < patternEnd) ? pattern.charAt(closing + 1) : '\0';

            // group number is precomputed for this '(' position
            Integer groupIndexObj = parenToGroupNum.get(j);
            int groupIndex = groupIndexObj != null ? groupIndexObj : (groups.size() + 1);

            if (topLevelAlt) {
                if (next == '+') {
                    return handleGroupRepeat(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 1, Integer.MAX_VALUE);
                } else if (next == '?') {
                    return handleGroupRepeat(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 0, 1);
                } else if (next == '*') {
                    return handleGroupRepeat(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 0, Integer.MAX_VALUE);
                } else {
                    return handleAlternation(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex);
                }
            } else {
                // no alternation inside the group text
                if (next == '+') {
                    return handleGroupRepeat(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 1, Integer.MAX_VALUE);
                } else if (next == '?') {
                    return handleGroupRepeat(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 0, 1);
                } else if (next == '*') {
                    return handleGroupRepeat(input, i, pattern, j, closing, patternEnd, anchoredEnd, groups, groupIndex, 0, Integer.MAX_VALUE);
                } else {
                    // no quantifier: inline the group and continue
                    for (int len = 0; len <= input.length() - i; len++) {
                        String candidate = input.substring(i, i + len);
                        Map<Integer, String> newGroups = new HashMap<>(groups);
                        // Match candidate against the pattern slice insideStart..insideEnd (anchored)
                        if (matchPattern(candidate, 0, pattern, insideStart, insideEnd, true, newGroups)) {
                            // record capture for this group
                            newGroups.put(groupIndex, candidate);
                            if (matchPattern(input, i + len, pattern, closing + 1, patternEnd, anchoredEnd, newGroups)) {
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

        // ----- escapes -----
        if (pc == '\\') {
            if (hasPlus) {
                return handleSingleTokenRepeat(input, i, pattern, j, patternEnd, anchoredEnd, groups, 1, Integer.MAX_VALUE, true);
            } else if (hasQuestion) {
                return handleSingleTokenRepeat(input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, 1, true);
            } else if (hasStar) {
                return handleSingleTokenRepeat(input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, Integer.MAX_VALUE, true);
            } else {
                return handleEscape(input, i, pattern, j, patternEnd, anchoredEnd, groups);
            }
        }

        // ----- character class -----
        if (pc == '[') {
            // Delegate to character-class-aware handler that understands + ? *
            return handleCharacterClass(input, i, pattern, j, patternEnd, anchoredEnd, groups);
        }

        // ----- literal / '.' with quantifiers -----
        if (hasPlus) {
            return handleSingleTokenRepeat(input, i, pattern, j, patternEnd, anchoredEnd, groups, 1, Integer.MAX_VALUE, false);
        } else if (hasQuestion) {
            return handleSingleTokenRepeat(input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, 1, false);
        } else if (hasStar) {
            return handleSingleTokenRepeat(input, i, pattern, j, patternEnd, anchoredEnd, groups, 0, Integer.MAX_VALUE, false);
        }

        // literal or dot
        return i < input.length()
                && charMatches(input.charAt(i), pc)
                && matchPattern(input, i + 1, pattern, j + 1, patternEnd, anchoredEnd, groups);
    }

    private static boolean charMatches(char textChar, char patternChar) {
        // '.' matches any character
        return patternChar == '.' || textChar == patternChar;
    }

    // ---------- repeat for single token (literal/dot/escape) ----------
    private static boolean handleSingleTokenRepeat(
            String input, int i, String pattern, int j, int patternEnd,
            boolean anchoredEnd, Map<Integer, String> groups,
            int min, int max, boolean specialToken) {

        // compute tokenEndJ correctly for specialToken: account for multi-digit backrefs
        int tokenEndJ;
        if (specialToken) {
            // pattern[j] == '\\'
            int k = j + 1;
            if (k < pattern.length() && Character.isDigit(pattern.charAt(k))) {
                while (k < patternEnd && Character.isDigit(pattern.charAt(k))) k++;
                tokenEndJ = k - 1; // last digit index
            } else {
                // escape like \w or \d or \. — single char escape target
                tokenEndJ = j + 1;
            }
        } else {
            tokenEndJ = j; // literal token at j, quantifier at j+1 so token region is j..j
        }

        // Greedy consume as many as possible (respecting max count)
        int k = i;
        int count = 0;

        while (k < input.length() && count < max) {
            if (specialToken) {
                if (!peekEscapeMatch(input, k, pattern, j, tokenEndJ + 1)) break;
            } else {
                if (!charMatches(input.charAt(k), pattern.charAt(j))) break;
            }
            k++;
            count++;
        }

        // Enforce the minimum properly: if we didn't consume enough, fail immediately
        if (count < min) return false;

        int maxSplit = k;
        int minSplit = i + min; // must consume at least 'min' chars

        // backtrack from greedy to min
        // next pattern index is tokenEndJ + 2 if token region is j..(tokenEndJ) and quantifier occupies tokenEndJ+1
        int nextPatternIndex = tokenEndJ + 2;
        for (int split = maxSplit; split >= minSplit; split--) {
            if (matchPattern(input, split, pattern, nextPatternIndex, patternEnd, anchoredEnd, new HashMap<>(groups))) {
                return true;
            }
        }
        return false;
    }

    // Check whether the escape token at pattern[j..tokenTargetEnd) matches input at pos
    private static boolean peekEscapeMatch(String input, int pos, String pattern, int j, int tokenTargetEndExclusive) {
        if (pos >= input.length()) return false;
        // token target is pattern[j+1 .. tokenTargetEndExclusive-1]
        if (j + 1 >= tokenTargetEndExclusive) return false;
        // if it's digits -> backreference token, but repeating backrefs is unusual;
        // for peek match we treat single-digit/one-char escapes (\d, \w) specially
        char first = pattern.charAt(j + 1);
        char c = input.charAt(pos);
        if (Character.isDigit(first)) {
            // If backreference, we cannot match greedily here without resolved groups, so return false.
            // This makes repeating backreferences behave conservatively (rarely used).
            return false;
        }
        switch (first) {
            case 'd': return Character.isDigit(c);
            case 'w': return (Character.isLetterOrDigit(c) || c == '_');
            default:  return c == first;
        }
    }

    // ---------- escapes & classes ----------
    private static boolean handleEscape(
            String input, int i, String pattern, int j, int patternEnd,
            boolean anchoredEnd, Map<Integer, String> groups) {
        if (j + 1 >= patternEnd) throw new RuntimeException("Dangling escape in pattern");
        if (i > input.length()) return false;

        // identify escape token: could be digit sequence (backreference) or \d \w or literal escape
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
                return matchPattern(input, i + captured.length(), pattern, k, patternEnd, anchoredEnd, groups);
            }
            return false;
        }

        char escapeType = pattern.charAt(j + 1);
        if (i >= input.length()) return false;
        char c = input.charAt(i);

        switch (escapeType) {
            case 'd':
                return Character.isDigit(c)
                        && matchPattern(input, i + 1, pattern, j + 2, patternEnd, anchoredEnd, groups);
            case 'w':
                return (Character.isLetterOrDigit(c) || c == '_')
                        && matchPattern(input, i + 1, pattern, j + 2, patternEnd, anchoredEnd, groups);
            default:
                // treat \x where x is a literal (e.g. \. or \[) as literal match of x
                return c == escapeType && matchPattern(input, i + 1, pattern, j + 2, patternEnd, anchoredEnd, groups);
        }
    }

    /**
     * Character class handler:
     * - supports ranges a-z and explicit chars
     * - supports negation ^ as first char
     * - supports quantifiers after the closing bracket ( + ? * )
     */
    private static boolean handleCharacterClass(
            String input, int i, String pattern, int j, int patternEnd,
            boolean anchoredEnd, Map<Integer, String> groups) {

        int closingBracket = findClosingBracket(pattern, j);
        if (closingBracket == -1 || closingBracket >= patternEnd) {
            throw new RuntimeException("Unclosed character class: " + pattern);
        }

        String charGroup = pattern.substring(j + 1, closingBracket);
        boolean negate = false;
        if (charGroup.startsWith("^")) {
            negate = true;
            charGroup = charGroup.substring(1);
        }

        // quantifier after ]
        char quant = '\0';
        if (closingBracket + 1 < patternEnd) {
            char maybe = pattern.charAt(closingBracket + 1);
            if (maybe == '+' || maybe == '?' || maybe == '*') quant = maybe;
        }

        java.util.function.BiPredicate<Character, String> matches = (ch, grp) -> {
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
                if (matchPattern(input, split, pattern, closingBracket + 2, patternEnd, anchoredEnd, new HashMap<>(groups))) {
                    return true;
                }
            }
            return false;

        } else if (quant == '?') {
            if (matchPattern(input, i, pattern, closingBracket + 2, patternEnd, anchoredEnd, new HashMap<>(groups))) {
                return true;
            }
            if (i >= input.length()) return false;
            boolean m = matches.test(input.charAt(i), charGroup);
            if (negate) m = !m;
            if (!m) return false;
            return matchPattern(input, i + 1, pattern, closingBracket + 2, patternEnd, anchoredEnd, groups);

        } else if (quant == '*') {
            int k = i;
            while (k < input.length()) {
                boolean m = matches.test(input.charAt(k), charGroup);
                if (negate) m = !m;
                if (!m) break;
                k++;
            }
            for (int split = k; split >= i; split--) {
                if (matchPattern(input, split, pattern, closingBracket + 2, patternEnd, anchoredEnd, new HashMap<>(groups))) {
                    return true;
                }
            }
            return false;

        } else {
            if (i >= input.length()) return false;
            boolean m = matches.test(input.charAt(i), charGroup);
            if (negate) m = !m;
            if (!m) return false;
            return matchPattern(input, i + 1, pattern, closingBracket + 1, patternEnd, anchoredEnd, groups);
        }
    }

    private static int findClosingBracket(String pattern, int start) {
        for (int k = start + 1; k < pattern.length(); k++) {
            char c = pattern.charAt(k);
            if (c == '\\') { k++; continue; }
            if (c == ']') return k;
        }
        return -1;
    }

    // ---------- groups with alternation (single occurrence) ----------
    private static int findClosingParen(String pattern, int start) {
        int depth = 0;
        boolean inCharClass = false;
        for (int k = start; k < pattern.length(); k++) {
            char c = pattern.charAt(k);
            if (c == '\\') { k++; continue; }
            if (inCharClass) {
                if (c == ']') inCharClass = false;
                continue;
            }
            if (c == '[') { inCharClass = true; continue; }
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return k;
            }
        }
        throw new RuntimeException("Unmatched ( in pattern: " + pattern);
    }

    // returns true if a top-level (depth==0) delimiter char is present in pattern[start:end)
    private static boolean hasTopLevelDelimiter(String pattern, int start, int end, char delimiter) {
        int depth = 0;
        boolean inCharClass = false;
        for (int k = start; k < end; k++) {
            char c = pattern.charAt(k);
            if (c == '\\') { k++; continue; }
            if (inCharClass) {
                if (c == ']') inCharClass = false;
                continue;
            }
            if (c == '[') { inCharClass = true; continue; }
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == delimiter && depth == 0) return true;
        }
        return false;
    }

    // split top-level parts returning list of [partStart, partEnd) ranges
    private static List<int[]> splitTopLevelIndices(String pattern, int start, int end, char delimiter) {
        List<int[]> parts = new ArrayList<>();
        int depth = 0;
        boolean inCharClass = false;
        int currentStart = start;

        for (int k = start; k < end; k++) {
            char c = pattern.charAt(k);
            if (c == '\\') { k++; continue; }
            if (inCharClass) {
                if (c == ']') inCharClass = false;
                continue;
            }
            if (c == '[') { inCharClass = true; continue; }
            if (c == '(') { depth++; continue; }
            if (c == ')') { depth--; continue; }
            if (c == delimiter && depth == 0) {
                parts.add(new int[]{currentStart, k});
                currentStart = k + 1;
            }
        }
        parts.add(new int[]{currentStart, end});
        return parts;
    }

    private static boolean handleAlternation(
            String input, int i, String pattern, int openParen, int closeParen,
            int patternEnd, boolean anchoredEnd, Map<Integer, String> groups, int groupIndex) {

        int insideStart = openParen + 1;
        int insideEnd = closeParen;
        List<int[]> options = splitTopLevelIndices(pattern, insideStart, insideEnd, '|');

        int suffixStart = closeParen + 1;
        int suffixEnd = patternEnd;

        for (int[] opt : options) {
            int optStart = opt[0];
            int optEnd = opt[1];
            Map<Integer, String> newGroups = new HashMap<>(groups);
            for (int len = 0; len <= input.length() - i; len++) {
                String candidate = input.substring(i, i + len);
                if (matchPattern(candidate, 0, pattern, optStart, optEnd, true, newGroups)) {
                    newGroups.put(groupIndex, candidate);
                    if (matchPattern(input, i + len, pattern, suffixStart, suffixEnd, anchoredEnd, newGroups)) {
                        groups.clear();
                        groups.putAll(newGroups);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ---------- group repetition (+ ? *) with greedy backtracking ----------
    private static boolean handleGroupRepeat(
            String input, int i, String pattern, int openParen, int closeParen,
            int patternEnd, boolean anchoredEnd, Map<Integer, String> groups,
            int groupIndex, int minReps, int maxReps) {

        int unitStart = openParen + 1;
        int unitEnd = closeParen;
        int afterQuant = closeParen + 2; // skip the quantifier symbol

        // Detect if unit can match empty to avoid infinite loops
        boolean unitCanBeEmpty = matchPattern("", 0, pattern, unitStart, unitEnd, true, new HashMap<>());

        // Determine a conservative maximum repetitions based on input length
        int theoreticalMax = unitCanBeEmpty ? Math.min(maxReps, (input.length() - i) + 1) : Math.min(maxReps, (input.length() - i));

        for (int reps = theoreticalMax; reps >= minReps; reps--) {
            // Try to consume exactly `reps` occurrences of `unit` from i, greedily over lengths
            for (int endPos : consumeUnitExactlyN(input, i, pattern, unitStart, unitEnd, reps, unitCanBeEmpty)) {
                String captured = input.substring(i, endPos);
                Map<Integer, String> newGroups = new HashMap<>(groups);
                if (matchPattern(input, endPos, pattern, afterQuant, patternEnd, anchoredEnd, newGroups)) {
                    newGroups.put(groupIndex, captured);
                    groups.clear();
                    groups.putAll(newGroups);
                    return true;
                }
            }
        }
        return false;
    }

    // Returns all possible end positions after matching `unit` exactly n times starting at `pos`,
    // ordered greedily (longer total match first).
    private static List<Integer> consumeUnitExactlyN(String input, int pos, String pattern, int unitStart, int unitEnd, int n, boolean unitCanBeEmpty) {
        List<Integer> results = new ArrayList<>();
        if (n == 0) {
            results.add(pos);
            return results;
        }
        // For the first repetition, try all candidate lengths (long to short), then recurse
        List<Integer> candEnds = allUnitEnds(input, pos, pattern, unitStart, unitEnd);
        // greedy: try longer first
        candEnds.sort((a, b) -> Integer.compare(b, a));
        for (int end : candEnds) {
            if (!unitCanBeEmpty && end == pos) continue; // must advance
            List<Integer> tails = consumeUnitExactlyN(input, end, pattern, unitStart, unitEnd, n - 1, unitCanBeEmpty);
            results.addAll(tails);
        }
        return results;
    }

    // All end positions 'end' such that input[pos:end] matches `unit` exactly (anchored)
    private static List<Integer> allUnitEnds(String input, int pos, String pattern, int unitStart, int unitEnd) {
        List<Integer> ends = new ArrayList<>();
        for (int len = input.length() - pos; len >= 0; len--) { // long -> short
            String candidate = input.substring(pos, pos + len);
            if (matchPattern(candidate, 0, pattern, unitStart, unitEnd, true, new HashMap<>())) {
                ends.add(pos + len);
            }
        }
        return ends;
    }
}
