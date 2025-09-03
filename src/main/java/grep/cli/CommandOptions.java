package grep.cli;

import java.util.ArrayList;
import java.util.List;

/** Immutable-ish options container for parsed CLI flags and args. */
public final class CommandOptions {
  public boolean recursive; // -r
  public String pattern; // -E <pattern>
  public final List<String> paths = new ArrayList<>(); // positional paths

}
