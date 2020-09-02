package com.craftinginterpreters.lox;

import java.util.List;

class Interpreter
    implements Expr.Visitor<Object>,
        Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    private final Object unassigned = new Object();

    // before inclusion of statements
//    void interpret(Expr expression) {
    void interpret(List<Stmt> statements) {
        try {
            for(Stmt statement : statements) {
                execute(statement);
            }
        } catch(RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for(Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    // Stmt.Visitor<Void>

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExitStmt(Stmt.Exit stmt) {
        throw new RuntimeError(stmt.operator, "Loop exit issued out of loop construct.");
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
//        Object exprVal =
        evaluate(stmt.expression);

        // should not print when running from file;
//            String text = stringify(exprVal);
//            if(exprVal instanceof String) text = "'" + text + "'";
//            System.out.println(text);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if(stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = unassigned;
        if(stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch(RuntimeError error) {
                if(error.token.type == TokenType.BREAK) break;
                if(error.token.type == TokenType.CONTINUE) continue;
                throw error;
            }
        }
        return null;
    }

    // Expr.Visitor<Object>

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        environment.assign(expr.name, value);
        return value;
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
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if(expr.operator.type == TokenType.OR) {
            if(isTruthy(left)) return left;
        } else {
            if(!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object first = evaluate(expr.first);

        if(isTruthy(first)) return evaluate(expr.second);
        return evaluate(expr.third);
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
    public Object visitVariableExpr(Expr.Variable expr) {
        Object val = environment.get(expr.name);
        if(!val.equals(unassigned)) return val;

        throw new RuntimeError(expr.name,
        "Use of variable before assignment.");
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

    private void checkNumberOperands(Token operator, Object left, Object right) {
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
}
