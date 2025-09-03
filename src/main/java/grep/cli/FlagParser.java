package grep.cli;

import grep.cli.builtin.ERegexFlagHandler;
import grep.cli.builtin.RecursiveFlagHandler;
import java.util.ArrayList;
import java.util.List;

public final class FlagParser {

  private final List<FlagHandler> handlers = new ArrayList<>();

  public FlagParser() {
    // Register all available handlers
    handlers.add(new RecursiveFlagHandler());
    handlers.add(new ERegexFlagHandler());
  }

  public CommandOptions parseArgs(String[] args) throws UsageException {
    CommandOptions opts = new CommandOptions();

    int i = 0;
    while (i < args.length) {
      boolean handled = false;
      for (FlagHandler h : handlers) {
        if (h.supports(args, i) || h.supports(args[i])) {
          i = h.handle(args, i, opts);
          handled = true;
          break;
        }
      }
      if (!handled) {
        // treat as path argument
        opts.paths.add(args[i]);
        i++;
      }
    }

    if (opts.pattern == null) {
      throw new UsageException("Usage: ./your_program.sh [-r] -E <pattern> [file1 file2 ...]");
    }

    return opts;
  }
}
