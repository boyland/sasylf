import os
import sys

tests = [
  "one",
  "two", # This one doesn't work because of variable capture
  "three",
  "four",
  "five",
  "six",
  "seven",
  "eight",
  "nine",
  "ten"
]

if len(sys.argv) > 1:
  tests = [sys.argv[1]]

regression_tests = [str(i) for i in range(58, 74 + 1)]

print("\033[92mRunning complex tests")

for test in tests:
  os.system(f"java -jar SASyLF.jar --root tests/{test} tests/{test}/demo.slf")

print("\033[96mRunning good regression tests")

if len(sys.argv) == 1:
  for rt in regression_tests:
    os.system(f"java -jar SASyLF.jar --root regression regression/good{rt}.slf")