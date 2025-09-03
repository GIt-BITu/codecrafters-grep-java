# NAME
   grep-clone tests — description of the test suite

# SYNOPSIS
    mvn -DskipTests=false test

# DESCRIPTION
   The test suite is written using JUnit 5. It ensures correctness of the CLI parsing, recursive directory traversal,
   and the custom regex engine.

# STRUCTURE
    src/test/java/grep/tests/

    ┌───────────────────────────────────────────┐
    │ SearchExecutorFileTests.java              │
    │   - Verifies search over single files.    │
    │   - Tests positive and negative matches.  │
    │   - Exercises recursive directory search. │
    └───────────────────────────────────────────┘

Additional test cases cover stdin handling and multi-file behavior.

# RUNNING TESTS
    $ mvn test
    # All tests run under Maven Surefire with the default lifecycle.

# EXPECTED OUTPUT
On passing:
      mvn exits with status 0.

During test execution, SearchExecutor will emit matching lines to stdout.
    This is expected behavior and not considered test failure.

# EXTENDING
To add more cases:
  
1. Create a new class under grep.tests.*
2. Use @TempDir to create temporary files.
3. Assert against the boolean return of SearchExecutor.execute() or capture stdout if needed.

## NOTE:
* These JUnit tests were written to mirror the official test cases from the CodeCrafters Grep challenge. we only added JUnit tests for coverage.

## SEE ALSO
- [README.md](../../README.md) for project usage.
- [README-CODE.md](../main/README-CODE.md) for internal architecture.

# AUTHORS    
    Written and refactored by contributors as part of the Codecrafters challenge.
