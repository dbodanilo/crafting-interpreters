package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch(RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private Boolean isTruthy(Object object) {
        if(object == null) return false;
        if(object instanceof Boolean) return (Boolean)object;
        return true;
    }

    private Boolean isEqual(Object a, Object b) {
        // nil is only equal to nil
        if(a == null && b == null) return true;
        if(a == null) return false;

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {
        if(left instanceof Double && right instanceof Double)
        {
            if(operator.type == TokenType.SLASH &&
               (double)right == 0) {
                throw new RuntimeError(operator, "Denominator must be non-zero.");
            }
            return;
        }

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
        if(object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if(object instanceof Double) {
            String text = object.toString();
            if(text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object first = evaluate(expr.first);

        if(isTruthy(first)) return evaluate(expr.second);
        return evaluate(expr.third);

//        throw new RuntimeError(expr.left,
//        "Ternary operator not implemented yet.");
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case COMMA: return right;

            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if(left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                // "string" + 0 = "string0"
                if(left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                throw new RuntimeError(expr.operator,
                "Operands must be two numbers or two strings.");

            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable
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
}
