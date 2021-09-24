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
    .limit locals 1
    .limit stack 3
    .catch java/lang/Exception from TRY to CATCH using CATCH

    new java/util/Scanner
    dup
    getstatic java/lang/System.in Ljava/io/InputStream;
    invokespecial java/util/Scanner.<init>(Ljava/io/InputStream;)V
    astore_0

TRY:
    aload_0
    invokevirtual java/util/Scanner.nextLine()Ljava/lang/String;
    invokestatic java/lang/Integer.parseInt(Ljava/lang/String;)I
    ireturn
CATCH:
    pop
    goto TRY

.end method


.method static readDouble()D
    .limit locals 1
    .limit stack 3
    .catch java/lang/Exception from TRY to CATCH using CATCH

    new java/util/Scanner
    dup
    getstatic java/lang/System.in Ljava/io/InputStream;
    invokespecial java/util/Scanner.<init>(Ljava/io/InputStream;)V
    astore_0

TRY:
    aload_0
    invokevirtual java/util/Scanner.nextLine()Ljava/lang/String;
    invokestatic java/lang/Double.parseDouble(Ljava/lang/String;)D
    dreturn
CATCH:
    pop
    goto TRY

.end method


.method static readString()Ljava/lang/String;
    .limit stack 3

    new java/util/Scanner
    dup
    getstatic java/lang/System.in Ljava/io/InputStream;
    invokespecial java/util/Scanner.<init>(Ljava/io/InputStream;)V
    invokevirtual java/util/Scanner.nextLine()Ljava/lang/String;
    dup
    ifnonnull END
    pop
    ldc ""
END:
    areturn

.end method


; Generated methods follow
