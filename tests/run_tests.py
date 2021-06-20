#!/usr/bin/env python3

from os import walk
from os.path import basename, splitext, abspath
from subprocess import run, DEVNULL
from sys import exit

exit_codes = {
    "good_programs": 0,
    "syntax_errors": 1,
    "semantic_errors":   2,
}

n_run = 0
n_passed = 0

def get_exit_error(actual, expected):
    if not 0 <= actual < 2:
        return "Unexpected system error"
    return ["Good program ", "Syntax error ", "Semantic error "] \
           [expected] \
           + \
           ["not caught", "rejected when parsing", "rejected in semantic analysis"] \
           [actual]

# Iterate over each .notc file in the test directories,
# run the compiler with it, and check exit status
for test_directory, _, files in walk("."):
    test_category = basename(test_directory)
    expected_exit = exit_codes.get(test_category)
    source_files = [f for f in files if splitext(f)[1] == ".notc"]
    for file_name in source_files:
        absolute_path = abspath(test_directory) + "/" + file_name
        actual_exit = run(["java", "notc.Compiler", absolute_path],
                          stderr=DEVNULL).returncode
        # TODO: Check if produced output matches expected output
        if actual_exit == expected_exit:
            n_passed += 1
        else:
            msg = [file_name]
            msg.append(get_exit_error(actual_exit, expected_exit))
            with open(absolute_path, 'r') as f:
                msg.append(f.read())
            print("\n".join(msg))
        n_run += 1

print(f"Passed {n_passed}/{n_run} tests")
exit(0 if n_passed == n_run else 1)
