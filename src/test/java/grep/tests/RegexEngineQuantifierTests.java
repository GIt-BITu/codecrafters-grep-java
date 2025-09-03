package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.engine.RegexEngine;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class RegexEngineQuantifierTests {

    @Test
    void testZeroOrOneQuantifier_NY8() {
        // "ca?t" should match "cat" and "ct"
        assertTrue(RegexEngine.matchPatternAnywhere("cat", "ca?t", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("ct", "ca?t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("caaat", "ca?t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("dog", "ca?t", new HashMap<>()));
    }

    @Test
    void testOneOrMoreQuantifier_FZ7() {
        // "ca+t" should match "cat" and "caaaat" but not "ct" or "ca"
        assertTrue(RegexEngine.matchPatternAnywhere("cat", "ca+t", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("caaaat", "ca+t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("ct", "ca+t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("ca", "ca+t", new HashMap<>()));
    }
}