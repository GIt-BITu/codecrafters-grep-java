package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.cli.CliParser;
import grep.cli.CommandOptions;
import grep.cli.UsageException;
import org.junit.jupiter.api.Test;

public class CliParserTest {

    @Test
    void testParsePatternAndFile() throws Exception {
        String[] args = {"-E", "hello", "file.txt"};
        CommandOptions opts = CliParser.parse(args);
        assertEquals("hello", opts.pattern);
        assertEquals(1, opts.paths.size());
        assertEquals("file.txt", opts.paths.get(0));
    }

    @Test
    void testParseRecursiveFlag() throws Exception {
        String[] args = {"-E", "pattern", "-r", "dir"};
        CommandOptions opts = CliParser.parse(args);
        assertTrue(opts.recursive);
        assertEquals("pattern", opts.pattern);
        assertEquals("dir", opts.paths.get(0));
    }

    @Test
    void testMissingPatternThrows() {
        String[] args = {"-E"};
        assertThrows(UsageException.class, () -> CliParser.parse(args));
    }

    @Test
    void testUnknownFlagThrows() {
        String[] args = {"-X", "hello"};
        assertThrows(UsageException.class, () -> CliParser.parse(args));
    }
}
