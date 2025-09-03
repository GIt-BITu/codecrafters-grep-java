package grep.search;

import grep.cli.CommandOptions;
import grep.engine.RegexEngine;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * SearchExecutor: provides both execute(CommandOptions) (used by tests)
 * and execute (pattern, recursive, paths).
 */
public final class SearchExecutor {

    // overload used by tests
    public static boolean execute(CommandOptions opts) {
        return execute(opts.pattern, opts.recursive, opts.paths);
    }

    public static boolean execute(String pattern, boolean recursive, List<String> paths) {
        boolean anyMatch = false;

        try {
            if (recursive) {
                if (paths.isEmpty()) paths.add(".");
                for (String startArg : paths) {
                    Path start = Paths.get(startArg);
                    if (!Files.exists(start)) {
                        System.err.println("Error: path does not exist: " + startArg);
                        System.exit(2);
                    }
                    if (Files.isDirectory(start)) {
                        try (Stream<Path> stream = Files.walk(start)) {
                            Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();
                            while (it.hasNext()) {
                                Path filePath = it.next();
                                boolean fileMatched = processFileRecursive(filePath, pattern);
                                if (fileMatched) anyMatch = true;
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (Files.isRegularFile(start)) {
                        boolean fileMatched = processFileRecursive(start, pattern);
                        if (fileMatched) anyMatch = true;
                    } else {
                        System.err.println("Error: not a file or directory: " + startArg);
                        System.exit(2);
                    }
                }
            } else {
                if (paths.isEmpty()) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (RegexEngine.matchPatternAnywhere(line, pattern, new HashMap<>())) {
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
                                if (RegexEngine.matchPatternAnywhere(line, pattern, new HashMap<>())) {
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
            System.err.println("Matcher error: " + re.getMessage());
            System.exit(2);
        }
        return anyMatch;
    }

    private static boolean processFileRecursive(Path filePath, String pattern) {
        boolean matched = false;
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (RegexEngine.matchPatternAnywhere(line, pattern, new HashMap<>())) {
                    System.out.println(filePath + ":" + line);
                    matched = true;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
            System.exit(2);
        }
        return matched;
    }
}
