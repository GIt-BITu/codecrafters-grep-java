package grep.app;

import grep.cli.CommandOptions;
import grep.cli.FlagParser;
import grep.cli.UsageException;
import grep.search.SearchExecutor;

public class Main {
  public static void main(String[] args) {
    try {
      // Parse command-line arguments
      FlagParser parser = new FlagParser();
      CommandOptions opts = parser.parseArgs(args);

      // Run the search
      SearchExecutor executor = new SearchExecutor();
      executor.execute(opts.pattern, opts.recursive, opts.paths);

    } catch (IllegalArgumentException e) {
      System.err.println("Error: " + e.getMessage());
      System.err.println("Usage: ./your_program.sh -E <pattern> [path...] [-r]");
      System.exit(1);
    } catch (UsageException e) {
      throw new RuntimeException(e);
    }
  }
}
