'''
Tests structure:
  If a file is found in the tests directory, it is run as its own test.
  If a folder is found in the tests directory, there should be a file called
  "test.slf" in that folder. That file is ran as a test
  
'''

import subprocess
import os

# walk through regression

small_tests = []
module_tests = []

for root, dirs, files in os.walk("./regression"):
  for dir_name in dirs:
    #print(f"dir_name: {dir_name}")
    test_file = os.path.join(root, dir_name, "test.slf")
    if os.path.exists(test_file):
      module_tests.append(dir_name)
  for file_name in files:
    small_tests.append(os.path.join(root, file_name))

def read_file_and_count_lines(file_path):
    lines = []
    
    with open(file_path, 'r') as file:
        for line in file:
            lines.append(line)
    
    return lines


def get_error_lines(test: str):

  error_lines = []

  lines = read_file_and_count_lines(f"./regression/{test}/test.slf")
  
  for i, line in enumerate(lines):
    if "//!" in line:
      error_lines.append(i + 1)

  return error_lines



for test in module_tests:

  # parse the test.slf file 
  error_lines = get_error_lines(test)

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
      else:
        error_lines.remove(error_line)

  # error_lines contains the lines that should have thrown an error but didn't
  
  for unthrown_error in error_lines:
    # print that an error was expected
    # include the directory and the line number in the message
    print(f"Error expected in regression/{test}/test.slf at line {unthrown_error}.")
    

  for unexpected_error in should_not_have_thrown:
    # print that an error was thrown when it shouldn't have been
    # include the directory and the line number in the message
    print(f"Unexpected error in regression/{test}/test.slf at line {unexpected_error}.")