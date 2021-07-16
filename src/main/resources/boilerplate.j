.class public $CLASSNAME$
.super java/lang/Object
.field private static br Ljava/io/BufferedReader;


.method static <clinit>()V
    .limit stack 5

    new java/io/BufferedReader
    dup
    new java/io/InputStreamReader
    dup
    getstatic java/lang/System.in Ljava/io/InputStream;
    invokespecial java/io/InputStreamReader.<init>(Ljava/io/InputStream;)V
    invokespecial java/io/BufferedReader.<init>(Ljava/io/Reader;)V
    putstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    return

.end method


.method public static main([Ljava/lang/String;)V
    .limit locals 1
    .limit stack 2

    invokestatic $CLASSNAME$/main()V
    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.close()V
    return

.end method


.method static printInt(I)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    iload_0
    invokevirtual java/io/PrintStream.println(I)V
    return

.end method


.method static printDouble(D)V
    .limit locals 2
    .limit stack 3

    getstatic java/lang/System.out Ljava/io/PrintStream;
    dload_0
    invokevirtual java/io/PrintStream.println(D)V
    return

.end method


.method static printString(Ljava/lang/String;)V
    .limit locals 1
    .limit stack 2

    getstatic java/lang/System.out Ljava/io/PrintStream;
    aload_0
    invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V
    return

.end method


.method static readInt()I
    .limit stack 1
    .catch java/lang/Exception from TRY to CATCH using CATCH

TRY:
    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.readLine()Ljava/lang/String;
    invokestatic java/lang/Integer.parseInt(Ljava/lang/String;)I
    ireturn
CATCH:
    pop
    goto TRY

.end method


.method static readDouble()D
    .limit stack 2
    .catch java/lang/Exception from TRY to CATCH using CATCH

TRY:
    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.readLine()Ljava/lang/String;
    invokestatic java/lang/Double.parseDouble(Ljava/lang/String;)D
    dreturn
CATCH:
    pop
    goto TRY

.end method


.method static readString()Ljava/lang/String;
    .limit stack 2

    getstatic $CLASSNAME$/br Ljava/io/BufferedReader;
    invokevirtual java/io/BufferedReader.readLine()Ljava/lang/String;
    dup
    ifnonnull END
    pop
    ldc ""
END:
    areturn

.end method


; Generated methods follow
