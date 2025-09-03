package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.engine.RegexEngine;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class QuantifierTests {

    @Test
    void testZeroOrOneQuantifier() {
        assertTrue(RegexEngine.matchPatternAnywhere("cat", "ca?t", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("act", "ca?t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("dog", "ca?t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("cag", "ca?t", new HashMap<>()));
    }

    @Test
    void testOneOrMoreQuantifier() {
        assertTrue(RegexEngine.matchPatternAnywhere("cat", "ca+t", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("caaats", "ca+at", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("act", "ca+t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("ca", "ca+t", new HashMap<>()));
    }
}
