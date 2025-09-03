# NAME
   grep-clone internals — structure of the source code

# SYNOPSIS
The implementation is divided into three primary domains:
   - Application / CLI
   - Search
   - Regex Engine

# DESCRIPTION
The code base is organized to mirror the responsibility boundaries of a minimal grep-like tool:

    ┌────────────────────────────────────┐
    │ grep/app/Main.java                 │
    │   Entry point, command-line driver │
    └────────────────────────────────────┘

    ┌──────────────────────────────────────┐
    │ grep/cli/                            │
    │   - CommandOptions.java              │
    │   - FlagHandler.java                 │
    │   - builtin/ERegexFlagHandler.java   │
    │   - builtin/RecursiveFlagHandler.java│
    │                                      │
    │   CLI parsing and option handling.   │
    └──────────────────────────────────────┘

    ┌────────────────────────────────────┐
    │ grep/search/SearchExecutor.java    │
    │   Dispatches file walking, stdin   │
    │   reading, and line-by-line calls  │
    │   into the regex engine.           │
    └────────────────────────────────────┘

    ┌────────────────────────────────────┐
    │ grep/engine/                       │
    │   RegexEngine.java                 │
    │   MatcherState.java                │
    │   TokenMatcher.java                │
    │   GroupHandler.java                │
    │   QuantifierHandler.java           │
    │   PatternUtils.java                │
    │                                    │
    │   Full custom regex engine, no     │
    │   dependency on java.util.regex.   │
    └────────────────────────────────────┘

# DESIGN PRINCIPLES
- Zero dependency on Java's Pattern/Matcher.
- Recursive-descent style matching with backtracking.
- Clear separation of regex responsibilities:
     * Token-level matching (TokenMatcher)
     * Group and alternation handling (GroupHandler)
     * Quantifier repetition (QuantifierHandler)
     * Utility parsing for brackets/parens (PatternUtils)
- Public API kept simple:

      RegexEngine.matchPatternAnywhere(String input, String pattern, Map<Integer,String> groups)

# EXTENDING WITH PLUGINS

  The codebase was designed to remain hackable and open to extension.
  Although the core functionality is frozen for the challenge, a plugin
  system using [java ISP](https://www.baeldung.com/java-spi)/[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) can be introduced without touching the main code paths.

  ## Concept
   - Define a lightweight interface (`Plugin`) in the `grep.engine` package.
   - Each plugin resides in its own directory (e.g., `grep/plugins/INFO/`).
   - Plugins are discovered via Java's ServiceLoader and registered through `META-INF/services`.

   ## Steps to add a new plugin
   1. Create an interface contract:

            package grep.engine;

            public interface Plugin {
                String name();
                void execute(String input);
            }

  2. Place your plugin in a dedicated subdirectory, for example:

               grep/plugins/INFO/InfoPrinter.java

  3. Implement the contract:

               package grep.plugins.INFO;

               import grep.engine.Plugin;

               public class InfoPrinter implements Plugin {
                   @Override
                   public String name() {
                       return "info-printer";
                   }

                   @Override
                   public void execute(String input) {
                       System.out.println("[INFO] " + input);
                   }
               }

  4. Register the plugin with the Service Provider mechanism:

       - Create file:
                 
             src/main/resources/META-INF/services/grep.engine.Plugin

       - Add the fully-qualified class name of your plugin:

                   grep.plugins.INFO.InfoPrinter

  5. At runtime, load plugins with:

         ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class);
         for (Plugin plugin : loader) {
             System.out.println("Loaded: " + plugin.name());
         }

  ## Notes
  - This follows the "ISP" (Interface Segregation Principle): the Plugin interface is minimal, and each plugin is self-contained.
  - Plugins do not require any modification of existing classes.
  - Contributors can experiment by dropping in new plugin directories without altering core grep or regex engine logic.


## SEE ALSO
- [README.md](../../README.md) for project overview.
- [README-TESTS.md](../test/README-TESTS.md) for testing notes.

# AUTHORS
Code by contributors. Original challenge from Codecrafters.
