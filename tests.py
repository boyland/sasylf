import subprocess
import os
import sys

RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
RESET = "\033[0m"

def printProgressBar(iteration, total):
    """
    Prints the progress bar. `iteration` is the number of tests that have been run, `total` is the total number of tests.
    """
    length = 50
    fill='█'
    printEnd="\r"

    #percent = ("{0:." + str(decimals) + "f}").format(100 * (iteration / float(total)))
    filledLength = int(length * iteration // total)
    bar = GREEN + fill * filledLength + RESET + '-' * (length - filledLength)
    print(f'\rProgress: |{bar}| {iteration}/{total} Completed:', end=printEnd)
    if iteration == total:
        print()

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

verbose = False
name = False

if len(sys.argv) > 1:
    if "-v" in sys.argv:
        verbose = True
    if "-n" in sys.argv:
        name = True

small_tests = get_small_tests()
module_tests = []

for root, dirs, files in os.walk("./regression"):
    for dir_name in dirs:
        test_file = os.path.join(root, dir_name, "test.slf")
        if os.path.exists(test_file):
            module_tests.append(f"./regression/{dir_name}/test.slf")

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

def run_unit_tests():
    print(f"{YELLOW}Running unit tests...{RESET}\n")
    result = subprocess.run(["make", "unit-test"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output = result.stdout.decode("utf-8")
    errors = result.stderr.decode("utf-8")
    print(output)
    print(errors)

run_unit_tests()

passed = 0
messages = []

def print_message(message: str):
    '''
    Logs a message to output_lines
    '''
    messages.append(message)

print(f"{YELLOW}Running regression tests...{RESET}")

# Initial call to print 0% progress
printProgressBar(0, len(all_tests))

for i, test in enumerate(all_tests):
    if name:
        print(f"\n{test}")
    
    error_messages = []
    error_lines = get_error_lines(test)
    error_lines_const = error_lines.copy()

    result = subprocess.run(["java", "-jar", "SASyLF.jar", test],
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE
                            )
    
    output_lines = result.stderr.decode("utf-8").split("\n")
    should_not_have_thrown = []

    cropped_test = test[2:] if test.startswith("./") else test

    for line in output_lines:
        if (test in line or cropped_test in line) and line.count(":") >= 2:
            error_line = int(line.split(":")[1])
            if error_line not in error_lines_const:
                should_not_have_thrown.append(error_line)
                error_messages.append(line)
            else:
                remove_all(error_lines, error_line)

    for unthrown_error in error_lines:
        print_message(f"{RED}Error expected in {test} at line {unthrown_error}.{RESET}")

    if verbose:
        for unexpected_error in error_messages:
            print_message(f"{YELLOW}{unexpected_error}{RESET}")
    else:
        for unexpected_error in should_not_have_thrown:
            print_message(f"{RED}Unexpected error in {test} at line {unexpected_error}.{RESET}")

    if len(error_lines) == 0 and len(should_not_have_thrown) == 0:
        passed += 1

    # Update Progress Bar
    printProgressBar(i + 1, len(all_tests))

print()

for message in messages:
    print(message)

print()

print(f"{GREEN}{passed} tests passed.{RESET}")
print(f"{RED}{len(all_tests) - passed} tests failed.{RESET}")
