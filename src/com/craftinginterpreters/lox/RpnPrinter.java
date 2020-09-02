package com.craftinginterpreters.lox;

public class RpnPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return postfixate("=", expr.value);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return postfixate(expr.left.lexeme + expr.right.lexeme,
                expr.first, expr.second, expr.third);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return postfixate(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return print(expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if(expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        if(expr.operator.type == TokenType.MINUS) {
            Expr left = new Expr.Literal(0.0);
            Expr.Binary newExpr = new Expr.Binary(left, expr.operator, expr.right);
            return visitBinaryExpr(newExpr);
        }
        return postfixate(expr.operator.lexeme, expr.right);
    }

    private String postfixate(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        for (Expr expr : exprs) {
            builder.append(print(expr));
            builder.append(" ");
        }
        builder.append(name);

        return builder.toString();
    }

    public static void main(String[] args) {
//        Lox.runPrompt(new RpnPrinter());
    }
}
