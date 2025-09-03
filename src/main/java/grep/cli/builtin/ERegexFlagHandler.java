package grep.cli.builtin;

import grep.cli.CommandOptions;
import grep.cli.FlagHandler;
import grep.cli.UsageException;

/** Handles -E <pattern> */
public final class ERegexFlagHandler implements FlagHandler {

  @Override
  public boolean supports(String arg) {
    return "-E".equals(arg);
  }

  @Override
  public boolean supports(String[] args, int i) {
    return i < args.length && "-E".equals(args[i]);
  }

  @Override
  public int handle(String[] args, int index, CommandOptions opts) throws UsageException {
    if (index + 1 >= args.length) {
      throw new UsageException("Usage: ./your_program.sh [-r] -E <pattern> [file1 file2 ...]");
    }
    opts.pattern = args[index + 1];
    // return the next index to continue at
    return index + 2;
  }
}
