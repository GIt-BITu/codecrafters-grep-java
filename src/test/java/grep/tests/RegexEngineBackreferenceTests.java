package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.engine.RegexEngine;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class RegexEngineBackreferenceTests {

    @Test
    void testSingleBackreference_SB5() {
        assertTrue(RegexEngine.matchPatternAnywhere("cat and cat", "(cat) and \\1", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("cat and dog", "(cat) and \\1", new HashMap<>()));

        assertTrue(RegexEngine.matchPatternAnywhere("grep 101 is doing grep 101 times",
                "(\\w\\w\\w\\w \\d\\d\\d) is doing \\1 times", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("$?! 101 is doing $?! 101 times",
                "(\\w\\w\\w \\d\\d\\d) is doing \\1 times", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("grep yes is doing grep yes times",
                "(\\w\\w\\w\\w \\d\\d\\d) is doing \\1 times", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("abcd is abcd, not efg",
                "([abcd]+) is \\1, not [^xyz]+", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("efgh is efgh, not efg",
                "([abcd]+) is \\1, not [^xyz]+", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("abcd is abcd, not xyz",
                "([abcd]+) is \\1, not [^xyz]+", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("this starts and ends with this",
                "^(\\w+) starts and ends with \\1$", new HashMap<>()));
    }

    @Test
    void testMultipleBackreferences_TG1() {
        assertTrue(RegexEngine.matchPatternAnywhere("3 red squares and 3 red circles",
                "(\\d+) (\\w+) squares and \\1 \\2 circles", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("3 red squares and 4 red circles",
                "(\\d+) (\\w+) squares and \\1 \\2 circles", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("cat and fish, cat with fish",
                "(c.t|d.g) and (f..h|b..d), \\1 with \\2", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("bat and fish, cat with fish",
                "(c.t|d.g) and (f..h|b..d), \\1 with \\2", new HashMap<>()));
    }

    @Test
    void testNestedBackreferences_XE5() {
        assertTrue(RegexEngine.matchPatternAnywhere("'cat and cat' is the same as 'cat and cat'",
                "('(cat) and \\2') is the same as \\1", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("'cat and cat' is the same as 'cat and dog'",
                "('(cat) and \\2') is the same as \\1", new HashMap<>()));

        assertTrue(RegexEngine.matchPatternAnywhere(
                "grep 101 is doing grep 101 times, and again grep 101 times",
                "((\\w\\w\\w\\w) (\\d\\d\\d)) is doing \\2 \\3 times, and again \\1 times",
                new HashMap<>()
        ));
        assertFalse(RegexEngine.matchPatternAnywhere(
                "$?! 101 is doing $?! 101 times, and again $?! 101 times",
                "((\\w\\w\\w) (\\d\\d\\d)) is doing \\2 \\3 times, and again \\1 times",
                new HashMap<>()
        ));

        assertTrue(RegexEngine.matchPatternAnywhere(
                "abc-def is abc-def, not efg, abc, or def",
                "(([abc]+)-([def]+)) is \\1, not ([^xyz]+), \\2, or \\3",
                new HashMap<>()
        ));
        assertFalse(RegexEngine.matchPatternAnywhere(
                "efg-hij is efg-hij, not klm, efg, or hij",
                "(([abc]+)-([def]+)) is \\1, not ([^xyz]+), \\2, or \\3",
                new HashMap<>()
        ));
        assertFalse(RegexEngine.matchPatternAnywhere(
                "abc-def is abc-def, not xyz, abc, or def",
                "(([abc]+)-([def]+)) is \\1, not ([^xyz]+), \\2, or \\3",
                new HashMap<>()
        ));

        assertTrue(RegexEngine.matchPatternAnywhere(
                "apple pie is made of apple and pie. love apple pie",
                "^((\\w+) (\\w+)) is made of \\2 and \\3. love \\1$",
                new HashMap<>()
        ));
        assertFalse(RegexEngine.matchPatternAnywhere(
                "pineapple pie is made of apple and pie. love pineapple pie",
                "^((apple) (\\w+)) is made of \\2 and \\3. love \\1$",
                new HashMap<>()
        ));
        assertFalse(RegexEngine.matchPatternAnywhere(
                "apple pie is made of apple and cake. love apple pie",
                "^((\\w+) (pie)) is made of \\2 and \\3. love \\1$",
                new HashMap<>()
        ));

        assertTrue(RegexEngine.matchPatternAnywhere(
                "'howwdy hey there' is made up of 'howwdy' and 'hey'. howwdy hey there",
                "'((how+dy) (he?y) there)' is made up of '\\2' and '\\3'. \\1",
                new HashMap<>()
        ));
    }
}