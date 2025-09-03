package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.engine.RegexEngine;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class RegexEngineWildcardAndAlternationTests {

    @Test
    void testWildcardDot() {
        assertTrue(RegexEngine.matchPatternAnywhere("cat", "c.t", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("cut", "c.t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("ct", "c.t", new HashMap<>()));
    }

    @Test
    void testWildcardWithPlus() {
        assertTrue(RegexEngine.matchPatternAnywhere("orange", "or.+$", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("or", "or.+$", new HashMap<>()));
    }

    @Test
    void testAlternation_ZM7() {
        assertTrue(RegexEngine.matchPatternAnywhere("a cat", "a (cat|dog)", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("a cow", "a (cat|dog)", new HashMap<>()));
    }
}