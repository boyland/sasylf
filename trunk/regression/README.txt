This directory includes files that exhibited bugs that are now fixed.
They are included to help prevent regression: when a bug re-appears
after later changes.

A simple naming system distinguishes files that should NOT have errors.
Files named goodN.slf have no errors and should be accepted.
Files named badN.slf have errors that should be rejected.

A line comment of the form //! should be placed exactly on those
lines expected to generate a SASyLF error.  This is used in the main
Makefile to test regression.