#!/usr/bin/env python3

import os
import subprocess
import sys

def main():
    n_run    = 0
    n_passed = 0

    # Test category -> Expected exit code
    expected_exits = {
        "good_programs":   0,
        "syntax_errors":   1,
        "semantic_errors": 2,
    }

    # Actual exit code -> Test failure message
    encountered_failures = {
        0: "Not caught",
        1: "Rejected when parsing",
        2: "Rejected during semantic analysis"
    }

    # Iterate over each .notc file in the test directories,
    # run the compiler with it, and check exit status.
    # TODO once code generator is in place:
    #      Also check if produced output matches expected output.
    for test_directory, _, files in os.walk("."):
        test_category = os.path.basename(test_directory)
        expected_exit = expected_exits.get(test_category)
        source_files = [f for f in files if os.path.splitext(f)[1] == ".notc"]
        for file_name in source_files:
            absolute_path = os.path.abspath(test_directory) + "/" + file_name
            run_result = subprocess.run(["java", "notc.Compiler", absolute_path],
                                        stderr=subprocess.DEVNULL)
            actual_exit = run_result.returncode
            if actual_exit == expected_exit:
                n_passed += 1
            else:
                msg = [f"{file_name} in {test_category}:"]
                with open(absolute_path, 'r') as f:
                    msg.append(f.read())
                error_msg = encountered_failures.get(actual_exit,
                                                    "Unexpected system error")
                msg.append(error_msg)
                print("\n".join(msg))
            n_run += 1

    print(f"Passed {n_passed}/{n_run} tests")
    sys.exit(0 if n_passed == n_run else 1)

if __name__ == '__main__':
    main()
