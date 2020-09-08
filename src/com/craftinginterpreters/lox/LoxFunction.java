package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Expr.Function declaration;
    private final Environment closure;
    private Token name;

    LoxFunction(Token name, Expr.Function declaration, Environment closure) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
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
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        for(int i = 0; i < declaration.params.size(); ++i) {
            environment.define(declaration.params.get(i).lexeme,
                arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch(Return returnValue) {
            return returnValue.value;
        }

        return null;
    }
}
