package grep.cli;

public interface FlagHandler {
  boolean supports(String arg);

  /**
   * @return true if this handler will process the token at args[i].
   */
  boolean supports(String[] args, int i);

  /**
   * Handle the token at args[i], possibly consuming additional arguments. Must return the next
   * index to continue parsing from. If usage should be displayed, throw UsageException.
   */
  int handle(String[] args, int i, CommandOptions opts) throws UsageException;
}
