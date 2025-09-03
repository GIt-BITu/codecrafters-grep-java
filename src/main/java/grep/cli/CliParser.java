package grep.cli;

import java.util.List;

/**
 * CLI parsing: delegates token handling to FlagHandler implementations loaded from HandlerRegistry.
 */
public final class CliParser {
  private CliParser() {}

  public static CommandOptions parse(String[] args) throws UsageException {
    CommandOptions opts = new CommandOptions();
    List<FlagHandler> handlers = HandlerRegistry.loadAll();

    int i = 0;
    while (i < args.length) {
      boolean handled = false;
      for (FlagHandler h : handlers) {
        // try position-aware variant first
        try {
          if (h.supports(args, i)) {
            int next = h.handle(args, i, opts);
            if (next <= i) {
              // ensure progress
              next = i + 1;
            }
            i = next;
            handled = true;
            break;
          }
        } catch (ArrayIndexOutOfBoundsException ex) {
          // fallthrough and try the other supports overload
        }

        // fallback: token-only supports
        try {
          if (h.supports(args[i])) {
            int next = h.handle(args, i, opts);
            if (next <= i) next = i + 1;
            i = next;
            handled = true;
            break;
          }
        } catch (ArrayIndexOutOfBoundsException ex) {
          // ignore
        }
      }

      if (!handled) {
        // Unknown flag starting with '-' -> usage
        if (args[i].startsWith("-")) {
          throw new UsageException("Usage: ./your_program.sh [-r] -E <pattern> [file1 file2 ...]");
        }
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
