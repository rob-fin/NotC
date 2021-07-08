; Jasmin assembly implementation of the built-in functions of the language.
; Intended to be loaded by the code generator and injected into its output,
; so that the built-in functions simply are methods of the generated class.


; Prints an integer.
.method public static printInt(I)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    iload_0
    invokevirtual java/io/PrintStream.println(I)V
    return

.end method


; Prints a floating-point value.
.method public static printDouble(D)V
    .limit locals 2
    .limit stack 3

    getstatic java/lang/System.out Ljava/io/PrintStream;
    dload_0
    invokevirtual java/io/PrintStream.println(D)V
    return

.end method


; Prints a text string.
.method public static printString(Ljava/lang/String;)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload_0
    invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V
    return

.end method


; Reads from the console until an integer can be parsed.
.method public static readInt()I
    .limit locals 0
    .limit stack 1
    .catch java/lang/Exception from TRY to CATCH using CATCH

TRY:
    invokestatic java/lang/System.console()Ljava/io/Console;
    invokevirtual java/io/Console.readLine()Ljava/lang/String;
    invokestatic java/lang/Integer.parseInt(Ljava/lang/String;)I
    ireturn
CATCH:
    pop
    goto TRY

.end method


; Reads from the console until a double can be parsed.
.method public static readDouble()D
    .limit locals 0
    .limit stack 2
    .catch java/lang/Exception from TRY to CATCH using CATCH

TRY:
    invokestatic java/lang/System.console()Ljava/io/Console;
    invokevirtual java/io/Console.readLine()Ljava/lang/String;
    invokestatic java/lang/Double.parseDouble(Ljava/lang/String;)D
    dreturn
CATCH:
    pop
    goto TRY

.end method


; Reads a text string from the console.
; In case null is read (e.g. from EOF), the empty string is put on the stack instead.
.method public static readString()Ljava/lang/String;
    .limit locals 0
    .limit stack 2

    invokestatic java/lang/System.console()Ljava/io/Console;
    invokevirtual java/io/Console.readLine()Ljava/lang/String;
    dup
    ifnonnull END
    pop
    ldc ""
END:
    areturn

.end method
