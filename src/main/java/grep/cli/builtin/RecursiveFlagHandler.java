package grep.cli.builtin;

import grep.cli.CommandOptions;
import grep.cli.FlagHandler;
import grep.cli.UsageException;

/** Handles -r (no additional args) */
public final class RecursiveFlagHandler implements FlagHandler {
  @Override
  public boolean supports(String arg) {
    return "-r".equals(arg);
  }

  @Override
  public boolean supports(String[] args, int i) {
    return i < args.length && "-r".equals(args[i]);
  }

  @Override
  public int handle(String[] args, int i, CommandOptions opts) throws UsageException {
    opts.recursive = true;
    return i + 1; // consumed the -r token, resume at the next index
  }
}
