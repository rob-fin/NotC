![Build](https://github.com/rob-fin/NotC/actions/workflows/build.yml/badge.svg)


# NotC

A toy compiler for a rough subset of C targeting the JVM. Written in Java with the [ANTLR](https://www.antlr.org/) parser generator.

## Building and running
Requires JDK 11 or higher.

* Linux/macOS: ```./gradlew build```

* Windows: ```gradlew.bat build```

This creates the executable ```build/libs/notcc.jar```.
```
$ java -jar build/libs/notcc.jar -h
usage: java -jar notcc.jar <options> <source file>
where options include:
 -c,--class <name>       Name of generated class.
                         Defaults to base name of source file.
 -d,--directory <path>   Destination directory of generated class file.
                         Defaults to working directory of invoking
                         process.
 -h,--help               Print this message and exit
$ cp src/test/resources/valid_programs/factorial.notc .
$ cat factorial.notc
void main() {
    int n = readInt();
    printInt(factorial(n));
}

int factorial(int n) {
    if (n == 0)
        return 1;
    return n * factorial(n - 1);
}
$ java -jar build/libs/notcc.jar factorial.notc
$ echo 5 | java factorial
120
```

  ## Language
For a precise syntax definition, see [notc.ebnf](notc.ebnf).

The built-in types of the language are  ```int```, ```double```, ```bool```, ```string```, and ```void```. Function definitions are the only top-level constructs. They have statements, some of which have expressions. Statements include:
* Variable declarations and initializations:
    ```c
    string s;
    int i, j, k;
    double d = expression;
    ```
* Repetitions:
    ```c
    while (expression)
        statement;

    for (expression; expression; expression)
        statement;
    ```
* Conditionals:
    ```c
    if (expression)
        statement;

    if (expression)
        statement1;
    else
        statement2;
    ```
* Blocks:
    ```c
    {
        statement1;
        statement2;
    }
    ```
* Returns:
    ```c
    return expression;
    ```
* Expressions statements:
    ```c
    expression;
    ```
Expressions include:
* Literals that can be inferred to a type (```8```, ```4.3```, ```"afdsaf"```, ```true```).
* Function calls (```fun(2, "eh")```).
* Prefix and postfix increments and decrements (```i++```, ```--d```).
* Assignments (```i = 7```).
* Binary arithmetic operations with ```+```, ```-```, ```*```, ```/```, and ```%```.
* Binary comparison operations with ```<```, ```>```, ```>=```, ```<=```, ```==```, and ```!=```.
* Binary boolean operations with ```&&``` and ```||```.
* Variable references by identifier (```i```).

The language constructs listed above follow the semantics of their C counterparts rather closely.
Some considerations:
* The entry point function needs this exact name and signature: ```void main()```.
* Functions need not be declared before they are called.
* ```int``` and ```double``` compile to their namesake primitive JVM types whereas ```bool``` compiles to ```int``` and behaves similarly to C99's ```_Bool```. Implicit conversions between all three are supported. An expression's type is the largest of its subexpressions' types (e.g. ```1 + 2.0 == 3.0```).
* ```string``` is similar to Java's ```String```.

### Built-in functions
The following are functions for reading from standard input and writing to standard output.
* ```void printInt(int)```
* ```void printDouble(double)```
* ```void printString(string)```
* ```int readInt()```
* ```double readDouble()```
* ```string readString()```

They are "built-in" in the sense that the compiler simply includes their definitions in each compiled program.

## Compiler
From an input program, the ANTLR-generated parser constructs a parse tree. The tree is traversed twice:

* First to analyze the semantics of the program.
* Then to generate bytecode instructions from it.

The semantic analysis phase infers types of expressions, type checks them, and annotates their tree nodes with their inferred types. This phase also fills in a symbol table by resolving identifiers to variables and functions.

Using the type annotations and the symbol table, the code generation phase constructs a [Jasmin](http://jasmin.sourceforge.net/) representation of the program. Finally, this is assembled and written to a class file.