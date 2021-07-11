; Jasmin assembly code common to all compiled programs.
; The code generator loads it, performs text replacement on
; the class name placeholder, and injects it into its output.

.class public $CLASSNAME$
.super java/lang/Object
.field private static sc Ljava/util/Scanner;

; The JVM entry point main sets up a Scanner
; for the reader functions and then calls the generated main.
.method public static main([Ljava/lang/String;)V
    .limit locals 1
    .limit stack 3
    new java/util/Scanner
    dup
    getstatic java/lang/System.in Ljava/io/InputStream;
    invokespecial java/util/Scanner.<init>(Ljava/io/InputStream;)V
    putstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokestatic $CLASSNAME$/main()V
    getstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokevirtual java/util/Scanner.close()V
    return
.end method

; Implementations of the built-in functions of the language.
; They simply become methods of the generated class.

; Prints an integer.
.method private static printInt(I)V
    .limit locals 1
    .limit stack 2
    getstatic java/lang/System.out Ljava/io/PrintStream;
    iload_0
    invokevirtual java/io/PrintStream.println(I)V
    return
.end method

; Prints a floating-point value.
.method private static printDouble(D)V
    .limit locals 2
    .limit stack 3
    getstatic java/lang/System.out Ljava/io/PrintStream;
    dload_0
    invokevirtual java/io/PrintStream.println(D)V
    return
.end method

; Prints a text string.
.method private static printString(Ljava/lang/String;)V
    .limit stack 2
    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload_0
    invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V
    return
.end method

; Reads an integer from standard input.
.method private static readInt()I
    .limit stack 2
    getstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokevirtual java/util/Scanner.nextInt()I
    ; Skip newline character
    getstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokevirtual java/util/Scanner.nextLine()Ljava/lang/String;
    pop
    ireturn
.end method

; Reads a floating-point value from standard input.
.method private static readDouble()D
    .limit stack 4
    getstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokevirtual java/util/Scanner.nextDouble()D
    getstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokevirtual java/util/Scanner.nextLine()Ljava/lang/String;
    pop
    dreturn
.end method

; Reads a text string from standard input.
.method private static readString()Ljava/lang/String;
    .limit stack 2
    getstatic $CLASSNAME$/sc Ljava/util/Scanner;
    invokevirtual java/util/Scanner.nextLine()Ljava/lang/String;
    areturn
.end method

; Generated methods follow
