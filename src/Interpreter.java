import java.awt.event.WindowStateListener;

public class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expr) {
        try {
            Object val = evaluate(expr);
            System.out.println(stringify(val));
        } catch (RuntimeError error) {
            Fave.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double) left <= (double) right;
            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double) left - (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                if ((double) right == 0) {
                    return 42;
                }
                return (double) left / (double) right;
            case PLUS:
                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right;

                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;

                if (left instanceof String && right instanceof Double ||
                    left instanceof Double && right instanceof String) {
                    return concatDynamicString(left, right);
                }

                throw new RuntimeError(expr.operator, "Operands not supported.");
        }

        // unreachable (hopefully)
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthful(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
        }

        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }
    private void checkNumberOperand(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String concatDynamicString(Object a, Object b) {
        String repl;
        if (a instanceof Double) {
            repl = a.toString();
        } else {
            repl = b.toString();
        }

        if (repl.endsWith(".0")) repl = repl.substring(0, repl.length()-2);

        return a instanceof String ? a + repl : repl + b;
    }
    public boolean isTruthful(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;

        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        }

        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
}
