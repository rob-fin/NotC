package notc.codegen;

import notc.antlrgen.NotCParser;
import notc.antlrgen.NotCParser.Type;
import notc.antlrgen.NotCParser.Signature;

import org.antlr.v4.runtime.Token;

// Utilities for formatting language constructs in an AST
// as Jasm assembly in the code generation environment
final class JvmFormatter {

    private JvmFormatter() {}

    static String methodDescriptor(Signature sig) {
        StringBuilder sb = new StringBuilder("\"(");
        for (Type t : sig.paramTypes())
            sb.append(typeDescriptor(t));
        sb.append(")" + typeDescriptor(sig.returnType()) + "\"");
        return sb.toString();
    }

    private static String typeDescriptor(Type t) {
        switch (t) {
            case BOOL:   return "Z";
            case VOID:   return "V";
            case STRING: return "Ljava/lang/String;";
            case INT:    return "I";
            case DOUBLE: return "D";
            default:     return "";
        }
    }

    static String formatLoad(Type t) {
        return typePrefix(t) + "load ";
    }

    static String formatStore(Type t) {
        return typePrefix(t) + "store ";
    }

    static char typePrefix(Type t) {
        switch (t) {
            case STRING: return 'a';
            case DOUBLE: return 'd';
            case INT:
            case BOOL:   return 'i';
            default:     return ' ';
        }
    }

    static String operationByToken(Token opTok) {
        switch (opTok.getType()) {
            case NotCParser.MUL:  return "mul";
            case NotCParser.DIV:  return "div";
            case NotCParser.REM:  return "rem";
            case NotCParser.INCR:
            case NotCParser.ADD:  return "add";
            case NotCParser.DECR:
            case NotCParser.SUB:  return "sub";
            case NotCParser.LT:   return "lt";
            case NotCParser.GT:   return "gt";
            case NotCParser.GE:   return "ge";
            case NotCParser.LE:   return "le";
            case NotCParser.EQ:   return "eq";
            case NotCParser.NE:   return "ne";
            case NotCParser.AND:  return "and";
            case NotCParser.OR:   return "or";
            default:              return "";
        }
    }

    static String formatDup(Type t) {
        return (t.isDouble()) ? "dup2" : "dup";
    }
}
