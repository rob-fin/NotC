.class public $_CLASSNAME_$
.super java/lang/Object

.method public <init>()V
    .limit locals 1

    aload_0
    invokespecial java/lang/Object/<init>()V
    return

.end method

; Entry point calls the generated main()I and exits
.method public static main([Ljava/lang/String;)V
    .limit locals 1
    .limit stack  1

    invokestatic $_CLASSNAME_$/main()V
    pop
    return

.end method


; Built-in functions are simply defined in the same class

.method public static printInt(I)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    iload_0
    invokevirtual java/io/PrintStream.println(I)V
    return

.end method


.method public static printDouble(D)V
    .limit locals 2
    .limit stack 4

    getstatic java/lang/System.out Ljava/io/PrintStream;
    dload_0
    invokevirtual java/io/PrintStream.println(D)V
    return

.end method


.method public static printString(Ljava/lang/String;)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload_0
    invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V
    return

.end method


.method public static readInt()I
    .limit locals 1
    .limit stack 1

    invokestatic java/lang/System.console()Ljava/io/Console;
    invokevirtual java/io/Console.readLine()Ljava/lang/String;
    invokestatic java/lang/Integer.parseInt(Ljava/lang/String;)I
    ireturn

.end method


.method public static readDouble()D
    .limit locals 1
    .limit stack 2

    invokestatic java/lang/System.console()Ljava/io/Console;
    invokevirtual java/io/Console.readLine()Ljava/lang/String;
    invokestatic java/lang/Double.parseDouble(Ljava/lang/String;)D
    dreturn

.end method


.method public static readString()Ljava/lang/String;
    .limit locals 1
    .limit stack 1

    invokestatic java/lang/System.console()Ljava/io/Console;
    invokevirtual java/io/Console.readLine()Ljava/lang/String;
    areturn

.end method


; Generated functions follow
