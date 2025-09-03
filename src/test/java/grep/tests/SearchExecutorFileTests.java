package grep.tests;

import static org.junit.jupiter.api.Assertions.*;

import grep.cli.CommandOptions;
import grep.search.SearchExecutor;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SearchExecutorFileTests {

    private String runAndCapture(CommandOptions opts) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
        try {
            SearchExecutor.execute(opts);
        } finally {
            System.setOut(originalOut);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    @Test
    void testRecursiveSearch_YX6(@TempDir Path tmp) throws Exception {
        Path dir = tmp.resolve("dir");
        Path sub = dir.resolve("subdir");
        Files.createDirectories(sub);
        Path f1 = dir.resolve("fruits-8347.txt");
        Path f2 = sub.resolve("vegetables-6563.txt");
        Path f3 = dir.resolve("vegetables-5654.txt");

        Files.createDirectories(dir);
        Files.writeString(f1, "watermelon\npear\n");
        Files.writeString(f2, "tomato\ncelery\ncauliflower\n");
        Files.writeString(f3, "lettuce\ncabbage\ncucumber\n");

        CommandOptions opts = new CommandOptions();
        opts.pattern = ".+er";
        opts.recursive = true;
        opts.paths.add(dir.toString());

        String out = runAndCapture(opts);
        // Should include any line with "...er"
        assertTrue(out.contains("celery"));
        assertTrue(out.contains("cauliflower"));
        assertTrue(out.contains("watermelon"));
        assertTrue(out.contains("cucumber"));
        assertFalse(out.contains("pear"));
    }

    @Test
    void testRecursiveNoMatchesExit1_YX6(@TempDir Path tmp) throws Exception {
        Path dir = tmp.resolve("dir");
        Files.createDirectories(dir);
        Path f = dir.resolve("fruits.txt");
        Files.writeString(f, "apple\nbanana\n");

        CommandOptions opts = new CommandOptions();
        opts.pattern = "missing_fruit";
        opts.recursive = true;
        opts.paths.add(dir.toString());

        PrintStream originalOut = System.out;
        try {
            boolean found = SearchExecutor.execute(opts);
            assertFalse(found);
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testMultipleFiles_IS6(@TempDir Path tmp) throws Exception {
        Path f1 = tmp.resolve("fruits-5811.txt");
        Path f2 = tmp.resolve("vegetables-1194.txt");
        Files.writeString(f1, "orange\napple\n");
        Files.writeString(f2, "onion\npotato\n");

        CommandOptions opts1 = new CommandOptions();
        opts1.pattern = "or.+$";
        opts1.paths.add(f1.toString());
        opts1.paths.add(f2.toString());
        String out1 = runAndCapture(opts1);
        assertTrue(out1.contains("fruits-5811.txt:orange"));
        assertFalse(out1.contains("vegetables-1194.txt:orange"));

        CommandOptions opts2 = new CommandOptions();
        opts2.pattern = "onion";
        opts2.paths.add(f1.toString());
        opts2.paths.add(f2.toString());
        String out2 = runAndCapture(opts2);
        assertTrue(out2.contains("vegetables-1194.txt:onion"));
        assertFalse(out2.contains("fruits-5811.txt:onion"));
    }

    @Test
    void testMultiLineFile_OL9(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("fruits-8613.txt");
        Files.writeString(f, "lemon\nraspberry\nblueberry\n");

        CommandOptions opts = new CommandOptions();
        opts.pattern = "rry$";
        opts.paths.add(f.toString());

        String out = runAndCapture(opts);
        // both "raspberry" and "blueberry" end with "rry"
        assertTrue(out.contains("raspberry"));
        assertTrue(out.contains("blueberry"));
        // but not "lemon"
        assertFalse(out.contains("lemon"));
    }
}