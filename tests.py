import subprocess
import os
import sys


def remove_all(l, item):
  while item in l:
    l.remove(item)

def list_files(directory):
    files = []
    for item in os.listdir(directory):
        if os.path.isfile(os.path.join(directory, item)):
            files.append(f"./regression/{item}")
    return files


# whether it prints the entire error message or just the line number
verbose = False

if len(sys.argv) > 1:
  if "-v" in sys.argv:
    verbose = True

small_tests = list_files("./regression")
module_tests = []

for root, dirs, files in os.walk("./regression"):
  for dir_name in dirs:
    test_file = os.path.join(root, dir_name, "test.slf")
    if os.path.exists(test_file):
      module_tests.append(dir_name)

small_tests.sort()
module_tests.sort()

if "./regression/README.txt" in small_tests:
  small_tests.remove("./regression/README.txt")
if "./regression/.DS_Store" in small_tests:
  small_tests.remove("./regression/.DS_Store")


def read_file_and_count_lines(file_path):
    lines = []
    
    with open(file_path, 'r') as file:
        for line in file:
            lines.append(line)
    
    return lines


def get_error_lines(test: str):

  error_lines = []

  lines = read_file_and_count_lines(test)
  
  for i, line in enumerate(lines):
    if "//!" in line or "/*!" in line:
      error_lines.append(i + 1)

  return error_lines

# ANSI escape codes for colors
RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RESET = "\033[0m"

passed = 0

print("Running module tests")
for test in module_tests:
  print(".", end='')
  sys.stdout.flush()
  error_messages = []
  # parse the test.slf file 
  error_lines = get_error_lines(f"./regression/{test}/test.slf")

  result = subprocess.run(["java", "-jar", "SASyLF.jar", "--root", f"./regression/{test}/test.slf"],
                          stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE
                          )
  
  output_lines = result.stderr.decode("utf-8").split("\n")

  # make sure that the errors were thrown in the right places

  should_not_have_thrown = []

  # for each of the output lines, check if that's an error that should be thrown
  for i, line in enumerate(output_lines):
    # tests/errortests/one/demo.slf:72:
    if f"regression/{test}/test.slf" in line and line.count(":") >= 2:
      # get the line number that the error was thrown on
      # the error line number is inbetween the first and second :
      error_line = int(line.split(":")[1])
      if error_line not in error_lines:
        should_not_have_thrown.append(error_line)
        error_messages.append(line)
      else:
        error_lines.remove(error_line)
        remove_all(error_lines, error_line)

  # error_lines contains the lines that should have thrown an error but didn't
  
  for unthrown_error in error_lines:
    # print that an error was expected
    # include the directory and the line number in the message
    print(f"\n{RED}Error expected in regression/{test}/test.slf at line {unthrown_error}.{RESET}")
    
  if verbose:
    for unexpected_error in error_messages:
      print(f"{YELLOW}     {unexpected_error}{RESET}")

  else:
    for unexpected_error in should_not_have_thrown:
      # print that an error was thrown when it shouldn't have been
      # include the directory and the line number in the message
      print(f"\n{RED}Unexpected error in regression/{test}/test.slf at line {unexpected_error}.{RESET}")

  if len(error_lines) == 0 and len(should_not_have_thrown) == 0:
    passed += 1


# do the same thing but for small_tests

print("\nRunning individual tests")

for test in small_tests:
  print(".", end="")
  sys.stdout.flush()
  error_messages = []
  # parse the test.slf file 
  error_lines = get_error_lines(test)
  error_lines_const = error_lines.copy()

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

  #print(f"error_lines (after): {error_lines}")

  # error_lines contains the lines that should have thrown an error but didn't
  
  for unthrown_error in error_lines:
    # print that an error was expected
    # include the directory and the line number in the message
    print(f"{RED}     Error expected in {test} at line {unthrown_error}.{RESET}")
    
  if verbose:
    for unexpected_error in error_messages:
      print(f"{YELLOW}     {unexpected_error}{RESET}")

  else:
    for unexpected_error in should_not_have_thrown:
      # print that an error was thrown when it shouldn't have been
      # include the directory and the line number in the message
      print(f"{RED}     Unexpected error in {test} at line {unexpected_error}.{RESET}")

  if len(error_lines) == 0 and len(should_not_have_thrown) == 0:
    passed += 1


print(f"\n{GREEN}{passed} tests passed.{RESET}")
print(f"{RED}{len(small_tests) + len(module_tests) - passed} tests failed.{RESET}")