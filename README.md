# NAME
   grep-clone — educational reimplementation of grep with custom regex engine

# DESCRIPTION
   This project is a from-scratch implementation of a simplified `grep(1)` utility. It supports:
 - extended regular expressions,
 - recursive directory traversal,
 - standard input streaming.
 - Pattern matching with literals, `.` (dot), character classes, groups.
 - Quantifiers: `*`, `+`, `?`.
 - Grouping with alternation `(a|b)`.
 - Backreferences `\1`, `\2`, ….
 - Recursive search with `-r`.
 - Multiple file arguments or standard input.

 
   The implementation avoids the Java built-in regex engine.  
   All matching logic is handled in our own recursive matcher.
    The purpose of this implementation is educational: to demonstrate how pattern
    matching, backtracking, and recursive descent engines can be built in Java
    without relying on java.util.regex. The code quality has been refactored to
    achieve modularity and readability, with strict separation of concerns.

# SYNOPSIS
    ./your_program.sh -E <pattern> [FILE...]
    ./your_program.sh -r -E <pattern> [DIRECTORY...]

# USAGE
- Non-recursive search through files:

      ./your_program.sh -E "pattern" file.txt

- Recursive search through a directory:
      
      ./your_program.sh -r -E "pattern" directory/

Standard input:

        echo "text" | ./your_program.sh -E "pattern"

# EXIT STATUS
    0   At least one match was found.
    1   No matches were found.
    2   An error occurred.

## SEE ALSO
- [README-CODE.md](./src/main/README-CODE.md) for code internals.
- [README-TESTS.md](./src/test/README-TESTS.md) for test documentation.

# AUTHORS
Written and refactored by contributors as part of the Codecrafters challenge.
