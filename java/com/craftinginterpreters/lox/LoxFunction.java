package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private Token name;
    private final List<Token> params;
    private final List<Stmt> body;
    private final Environment closure;
    private final Boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Environment closure,
            boolean isInitializer) {
        this(declaration.name, declaration.params, declaration.body, closure, isInitializer);
    }

    LoxFunction(Expr.Function declaration, Environment closure,
            boolean isInitializer) {
        this(declaration.name, declaration.params, declaration.body, closure, isInitializer);
    }

    LoxFunction(Token name, List<Token> params, List<Stmt> body, Environment closure,
            boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.closure = closure;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(name, params, body, environment,
                                 isInitializer);
    }

    public void define(Token name) {
        if(this.name == null) this.name = name;
    }

    @Override
    public String toString() {
        String name = this.name != null ? this.name.lexeme : "object";
        return "<fn " + name + ">";
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for(int i = 0; i < params.size(); ++i) {
            environment.define(params.get(i).lexeme,
                arguments.get(i));
        }

        try {
            interpreter.executeBlock(body, environment);
        } catch(Return returnValue) {
            if (isInitializer) return closure.getAt(0, "this");

            return returnValue.value;
        }

        if(isInitializer) return closure.getAt(0, "this");

        return null;
    }
}
