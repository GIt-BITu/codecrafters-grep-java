package grep.cli;

import grep.cli.builtin.ERegexFlagHandler;
import grep.cli.builtin.RecursiveFlagHandler;
import java.util.ArrayList;
import java.util.List;

/** Small explicit registry of built-in flag handlers. Tests expect ERegex handler to be present. */
public final class HandlerRegistry {
  private HandlerRegistry() {}

  public static List<FlagHandler> loadAll() {
    List<FlagHandler> handlers = new ArrayList<>();
    // keep order: -E handler first typically, then -r (order isn't critical for tests)
    handlers.add(new ERegexFlagHandler());
    handlers.add(new RecursiveFlagHandler());
    return handlers;
  }
}
