package grep.tests;

import grep.engine.RegexEngine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class RegexEngineAcceptanceTest {

    @Test
    void testSimpleLiteralMatch() {
        assertTrue(RegexEngine.matchPatternAnywhere("hello", "hello", new HashMap<>()));
    }

    @Test
    void testNoMatch() {
        assertFalse(RegexEngine.matchPatternAnywhere("world", "hello", new HashMap<>()));
    }

    @Test
    void testDotMatchesAnything() {
        assertTrue(RegexEngine.matchPatternAnywhere("abc", "a.c", new HashMap<>()));
    }

    @Test
    void testAnchors() {
        assertTrue(RegexEngine.matchPatternAnywhere("start", "^st", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("end", "nd$", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("bend", "^st", new HashMap<>()));
    }

    @Test
    void testAlternation() {
        assertTrue(RegexEngine.matchPatternAnywhere("dog", "cat|dog", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("fish", "cat|dog", new HashMap<>()));
    }
}
