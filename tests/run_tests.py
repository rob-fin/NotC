from os import walk
from os.path import basename, splitext, abspath
from subprocess import run, DEVNULL
from sys import exit

exit_codes = {
    "good_programs": 0,
    "lexical_errors": 1,
    "parse_errors": 2,
    "type_errors": 3,
}

n_run = 0
n_passed = 0

def get_exit_error(actual, expected):
    if actual > 3:
        return "Unexpected system error"
    return ["Good program ", "Lexical error ", "Parse error ", "Type error "] \
           [expected] \
           + \
           ["not caught", "rejected by lexer", "rejected by parser", "rejected by type checker"] \
           [actual]

# Iterate over each .nc file in the test directories,
# run the compiler with it, and check exit status
for test_directory, _, files in walk("."):
    test_category = basename(test_directory)
    expected_exit = exit_codes.get(test_category)
    source_files = [f for f in files if splitext(f)[1] == ".nc"]
    for file_name in source_files:
        absolute_path = abspath(test_directory) + "/" + file_name
        actual_exit = run(["java", "NotC.Compiler", absolute_path], \
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
