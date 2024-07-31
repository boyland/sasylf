"""
This script runs all of the tests in ./regression and checks if
  1. Errors are thrown when they should be
  2. Errors are not thrown when they shouldn't be

Run this test with: python tests.py or python3 tests.py, depending on your system

To get verbose output (the entire error message), run with the -v flag: python tests.py -v
To print the name of the test after each execution, run with the -n flag: python tests.py -n
"""

import subprocess
import os
import sys


def remove_all(l, item):
  '''
  Removes all instances of `item` from list `l`
  '''
  while item in l:
    l.remove(item)

def get_small_tests():
    '''
    Returns a list of all the small tests in the regression directory
    '''
    files = []
    for item in os.listdir("./regression"):
        if os.path.isfile(os.path.join("./regression", item)):
            files.append(f"./regression/{item}")
    return files


# whether it prints the entire error message or just the line number
verbose = False
name = False

# check if the user wants verbose output
if len(sys.argv) > 1:
  if "-v" in sys.argv:
    verbose = True
  if "-n" in sys.argv:
    name = True

# load the small tests
small_tests = get_small_tests()
# load the module tests
module_tests = []

for root, dirs, files in os.walk("./regression"):
  for dir_name in dirs:
    test_file = os.path.join(root, dir_name, "test.slf")
    if os.path.exists(test_file):
      module_tests.append(f"./regression/{dir_name}/test.slf")

# remove miscilanious files from small_tests
if "./regression/README.txt" in small_tests:
  small_tests.remove("./regression/README.txt")
if "./regression/.DS_Store" in small_tests:
  small_tests.remove("./regression/.DS_Store")

all_tests = small_tests + module_tests
all_tests.sort()


def read_file(file_path):
    '''
    Reads a file and returns a list of all the lines in the file
    '''
    lines = []
    
    with open(file_path, 'r') as file:
        for line in file:
            lines.append(line)
    
    return lines


def get_error_lines(test: str):
  '''
  Reads the `test` file and returns a list of all the lines should throw an error
  '''
  error_lines = []

  lines = read_file(test)
  
  for i, line in enumerate(lines):
    if "//!" in line or "/*!" in line:
      error_lines.append(i + 1)

  return error_lines

# ANSI escape codes for colors
RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RESET = "\033[0m"



# run the unit tests

def run_unit_tests():
  print(f"{YELLOW}Running unit tests...{RESET}")
  result = subprocess.run(["make", "unit-test"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  output = result.stdout.decode("utf-8")
  errors = result.stderr.decode("utf-8")
  print(output)
  print(errors)



run_unit_tests()




passed = 0

print(f"{YELLOW}Running regression tests...{RESET}")
for test in all_tests:
  if name:
    print(f"\n{test}")
  else:
    print(".", end="")

  sys.stdout.flush()
  error_messages = []
  # parse the test.slf file 
  error_lines = get_error_lines(test)
  error_lines_const = error_lines.copy()

  # run the test in a new process
  result = subprocess.run(["java", "-jar", "SASyLF.jar", test],
                          stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE
                          )
  
  output_lines = result.stderr.decode("utf-8").split("\n")

  # make sure that the errors were thrown in the right places

  should_not_have_thrown = []

  # for each of the output lines, check if that's an error that should be thrown
  for i, line in enumerate(output_lines):
    # tests/errortests/one/demo.slf:72:

    cropped_test = test[2:] if test.startswith("./") else test

    if (test in line or cropped_test in line) and line.count(":") >= 2:
      # get the line number that the error was thrown on
      # the error line number is inbetween the first and second :
      error_line = int(line.split(":")[1])
      if error_line not in error_lines_const:
        should_not_have_thrown.append(error_line)
        error_messages.append(line)
      else:
        #error_lines.remove(error_line)
        remove_all(error_lines, error_line)

  # error_lines contains the lines that should have thrown an error but didn't
  
  # print the expected error messages
  for unthrown_error in error_lines:
    # print that an error was expected
    # include the directory and the line number in the message
    print(f"{RED}     Error expected in {test} at line {unthrown_error}.{RESET}")
    
  if verbose:
    # print the unexpected error messages
    for unexpected_error in error_messages:
      print(f"{YELLOW}     {unexpected_error}{RESET}")

  else:
    # print the unexpected error messages
    for unexpected_error in should_not_have_thrown:
      # print that an error was thrown when it shouldn't have been
      # include the directory and the line number in the message
      print(f"{RED}     Unexpected error in {test} at line {unexpected_error}.{RESET}")

  if len(error_lines) == 0 and len(should_not_have_thrown) == 0:
    passed += 1

print(f"\n{GREEN}{passed} tests passed.{RESET}")
print(f"{RED}{len(all_tests) - passed} tests failed.{RESET}")