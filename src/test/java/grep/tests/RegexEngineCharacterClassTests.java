package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.engine.RegexEngine;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class RegexEngineCharacterClassTests {

    @Test
    void testPositiveCharacterClassBasic() {
        assertTrue(RegexEngine.matchPatternAnywhere("cat", "c[aeiou]t", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("cot", "c[ae]t", new HashMap<>()));
    }

    @Test
    void testNegativeCharacterClassBasic() {
        assertTrue(RegexEngine.matchPatternAnywhere("apple", "[^xyz]", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("xyz", "[^xyz]+$", new HashMap<>()));
    }

    @Test
    void testDigitAndWordClasses_Combinations_SH9() {
        // "\d apple" should match "sally has 3 apples"
        assertTrue(RegexEngine.matchPatternAnywhere("sally has 3 apples", "\\d apple", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("sally has 1 orange", "\\d apple", new HashMap<>()));

        // "\d\d\d apples" matches 3 digits
        assertTrue(RegexEngine.matchPatternAnywhere("sally has 124 apples", "\\d\\d\\d apples", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("sally has 12 apples", "\\d\\d\\d apples", new HashMap<>()));

        // "\d \w\w\ws" -> "3 dogs" vs. "1 dog"
        assertTrue(RegexEngine.matchPatternAnywhere("sally has 3 dogs", "\\d \\w\\w\\ws", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("sally has 1 dog", "\\d \\w\\w\\ws", new HashMap<>()));
    }

    @Test
    void testWordClass_OQ2() {
        assertTrue(RegexEngine.matchPatternAnywhere("blueberry", "\\w", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("MANGO", "\\w", new HashMap<>()));
        assertTrue(RegexEngine.matchPatternAnywhere("522", "\\w", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("+#%=", "\\w", new HashMap<>()));
    }

    @Test
    void testDigitClass_OQ2() {
        assertTrue(RegexEngine.matchPatternAnywhere("123", "\\d", new HashMap<>()));
        assertFalse(RegexEngine.matchPatternAnywhere("apple", "\\d", new HashMap<>()));
    }
}