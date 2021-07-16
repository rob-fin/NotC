`boilerplate.j` contains Jasmin assembly code common to all compiled programs. The code generator loads it, performs text replacement on the class name placeholder `$CLASSNAME$`, and injects it into its output. Performance tests showed that this is faster than collecting the code with method invocations.

The file defines the built-in functions of the language. They become methods of the generated class. If `readInt` and `readDouble` cannot parse their input, they silently try again. If `readString` encounters null, it returns the empty string.

Also defined is the JVM entry point `main`, which calls the generated `main`.

Resources used by the reading methods for reading from standard input are managed in a static initialization method and in `main`.
