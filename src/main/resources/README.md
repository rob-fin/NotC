`builtin_headers.notc` declares the built-in functions of the language. They are added to the symbol table.

`builtin_definitions.j` contains their definitions. The code generator injects them into its output.

Implementation notes: If `readInt` and `readDouble` cannot parse their input, they silently try again. If `readString` encounters null, it returns the empty string.