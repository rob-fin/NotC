![Build](https://github.com/rob-fin/NotC/actions/workflows/build.yml/badge.svg)

# NotC

A JVM compiler for a small strongly typed C-like language. Written in Java with the [ANTLR](https://www.antlr.org/) parser generator. Work in progress.

## Building and running
Requires JDK 11 or higher.
* *nix: ```./gradlew build```
* Windows:  ```gradlew.bat build```

This creates the executable ``` build/libs/NotC.jar```  which can be executed with a source file argument.
(At the moment it only outputs abstract syntax trees in LISP-style text format.)

## Feature progress
- [x] Front-end analysis
	- [x] Lexer and parser
	- [x]  Semantics checker
- [ ] Back-end code generation
	- [ ] Bytecode generator
	- [ ] .class file assembler
