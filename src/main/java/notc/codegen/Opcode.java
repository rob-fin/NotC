package notc.codegen;

// Opcodes and their impact on the stack depth when executed
enum Opcode {                          // Stack:
    IADD         ("iadd",        -1),  // [ i i ] -> [ i ]
    ISUB         ("isub",        -1),
    IMUL         ("imul",        -1),
    IDIV         ("idiv",        -1),
    IREM         ("irem",        -1),
    IAND         ("iand",        -1),
    IOR          ("ior",         -1),
    IUSHR        ("iushr",       -1),
    DADD         ("dadd",        -2),  // [ d d ] -> [ d ]
    DSUB         ("dsub",        -2),
    DMUL         ("dmul",        -2),
    DDIV         ("ddiv",        -2),
    DREM         ("drem",        -2),
    ISTORE       ("istore",      -1),  // [ val ] -> [ ]
    DSTORE       ("dstore",      -2),
    ASTORE       ("astore",      -1),
    ILOAD        ("iload",        1),  // [ ] -> [ val ]
    DLOAD        ("dload",        2),
    ALOAD        ("aload",        1),
    LDC          ("ldc",          1),
    LDC2_W       ("ldc2_w",       2),
    ICONST_0     ("iconst_0",     1),
    ICONST_1     ("iconst_1",     1),
    ICONST_M1    ("iconst_m1",    1),
    DCONST_1     ("dconst_1",     2),
    IF_ICMPLT    ("if_icmplt",   -2),  // [ i i ] -> [ ]
    IF_ICMPGT    ("if_icmpgt",   -2),
    IF_ICMPGE    ("if_icmpge",   -2),
    IF_ICMPLE    ("if_icmple",   -2),
    IF_ICMPEQ    ("if_icmpeq",   -2),
    IF_ICMPNE    ("if_icmpne",   -2),
    DUP          ("dup",          1),  // [ val ] -> [ val val ]
    DUP2         ("dup2",         2),
    POP          ("pop",         -1),  // [ val ] -> [ ]
    POP2         ("pop2",        -2),
    DCMPG        ("dcmpg",       -3),  // [ d d ] -> [ i ]
    IFEQ         ("ifeq",        -1),  // [ i ] -> [ ]
    IFNE         ("ifne",        -1),
    I2D          ("i2d",          1),  // [ i ] -> [ d ]
    D2I          ("d2i",         -1),  // [ d ] -> [ i ]
    INEG         ("ineg",         0),  // [ val ] -> [ val ]
    DNEG         ("dneg",         0),
    IRETURN      ("ireturn",     -1),  // [ returnval ] -> [ ]
    DRETURN      ("dreturn",     -2),
    ARETURN      ("areturn",     -1),
    RETURN       ("return",       0),  // [ ] -> [ ]
    GOTO         ("goto",         0),
    INVOKESTATIC ("invokestatic", 0);

    final String mnemonic;
    final int defaultStackChange;

    Opcode(String mnemonic, int defaultStackChange) {
        this.mnemonic = mnemonic;
        this.defaultStackChange = defaultStackChange;
    }

}