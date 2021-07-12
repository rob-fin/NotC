; Jasmin assembly code common to all compiled programs.
; The code generator loads it, performs text replacement on
; the class name placeholder, and injects it into its output.

.class public $CLASSNAME$
.super java/lang/Object
.field private static br Ljava/io/BufferedReader;

; The JVM entry point main sets up a reader resource
; for the reader functions and then calls the generated main.
.method public static main([Ljava/lang/String;)V
    .limit locals 1
    .limit stack 5
                                                                            ; stack:
    new java/io/BufferedReader                                              ; br
    dup                                                                     ; br br
    new java/io/InputStreamReader                                           ; br br isr
    dup                                                                     ; br br isr isr
    getstatic java/lang/System.in Ljava/io/InputStream;                     ; br br isr isr sysin
    invokespecial java/io/InputStreamReader.<init>(Ljava/io/InputStream;)V  ; br br isr
    invokespecial java/io/BufferedReader.<init>(Ljava/io/Reader;)V          ; br
    putstatic $CLASSNAME$/br Ljava/io/BufferedReader;                       ;
    invokestatic $CLASSNAME$/main()V
    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.close()V
    return

.end method


; Implementations of the built-in functions of the language.
; They simply become methods of the generated class.


.method private static printInt(I)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    iload_0
    invokevirtual java/io/PrintStream.println(I)V
    return

.end method


.method private static printDouble(D)V
    .limit locals 2
    .limit stack 3

    getstatic java/lang/System.out Ljava/io/PrintStream;
    dload_0
    invokevirtual java/io/PrintStream.println(D)V
    return

.end method


.method private static printString(Ljava/lang/String;)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload_0
    invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V
    return

.end method


.method private static readInt()I
    .limit stack 1

    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.readLine()Ljava/lang/String;
    invokestatic java/lang/Integer.parseInt(Ljava/lang/String;)I
    ireturn

.end method


.method private static readDouble()D
    .limit stack 2

    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.readLine()Ljava/lang/String;
    invokestatic java/lang/Double.parseDouble(Ljava/lang/String;)D
    dreturn

.end method


.method private static readString()Ljava/lang/String;
    .limit stack 1

    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.readLine()Ljava/lang/String;
    areturn

.end method


; Generated methods follow.
