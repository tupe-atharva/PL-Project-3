package simplf; 

import java.util.List;

import simplf.Expr.Lambda;
import simplf.Stmt.For;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Object> {
    public Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {

    }

    public void interpret(List<Stmt> stmts) {
        try {
            for (Stmt stmt : stmts) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Simplf.runtimeError(error);
        }
    }

    @Override
    public Object visitExprStmt(Stmt.Expression stmt) {
        return evaluate(stmt.expr);
    }

    @Override
    public Object visitPrintStmt(Stmt.Print stmt) {
        Object val = evaluate(stmt.expr);
        System.out.println(stringify(val));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name, stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitBlockStmt(Stmt.Block stmt) {
        Environment previous = this.environment;
        try {
            this.environment = new Environment(previous);
            return executeBlock(stmt.statements, this.environment);
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Object visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.cond))) {
            return execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            return execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitWhileStmt(Stmt.While stmt) {
        Object last = null;
        while (isTruthy(evaluate(stmt.cond))) {
            last = execute(stmt.body);
        }
        return last;
    }

    @Override
    public Object visitForStmt(For stmt) {
        throw new UnsupportedOperationException("For loops are not interpreted.");
    }

    @Override
    public Object visitFunctionStmt(Stmt.Function stmt) {
        SimplfFunction function = new SimplfFunction(stmt, this.environment);
        environment.define(stmt.name, stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.op.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitBinary(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.op.type) {
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                throw new RuntimeError(expr.op, "Addition operation not supported for operands.");
            case MINUS:
                checkNumbers(expr.op, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumbers(expr.op, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumbers(expr.op, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.op, "Cannot divide by zero.");
                }
                return (double) left / (double) right;
            case GREATER:
                checkNumbers(expr.op, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumbers(expr.op, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumbers(expr.op, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumbers(expr.op, left, right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case COMMA:
                return right;
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitUnary(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.op.type) {
            case MINUS:
                checkNumber(expr.op, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitLiteral(Expr.Literal expr) {
        return expr.val;
    }

    @Override
    public Object visitGrouping(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVarExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof SimplfCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and lambdas.");
        }
        java.util.ArrayList<Object> arguments = new java.util.ArrayList<>();
        for (Expr argument : expr.args) {
            arguments.add(evaluate(argument));
        }
        return ((SimplfCallable) callee).call(this, arguments);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        if (isTruthy(evaluate(expr.cond))) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    private Object execute(Stmt stmt) {
        return stmt.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null)
            return b == null;
        return a.equals(b);
    }

    private void checkNumber(Token op, Object object) {
        if (object instanceof Double)
            return;
        throw new RuntimeError(op, "Operand must be a number");
    }

    private void checkNumbers(Token op, Object a, Object b) {
        if (a instanceof Double && b instanceof Double)
            return;
        throw new RuntimeError(op, "Operand must be numbers");
    }

    private String stringify(Object object) {
        if (object == null)
            return "nil";
        if (object instanceof Double) {
            String num = object.toString();
            if (num.endsWith(".0")) {
                num = num.substring(0, num.length() - 2);
            }
            return num;
        }
        return object.toString();
    }

    @Override
    public Object visitLambda(Lambda expr) {
        Environment closure = this.environment;
        // Return a callable that captures current environment
        return new SimplfCallable() {
            @Override
            public Object call(Interpreter interpreter, java.util.List<Object> args) {
                Environment callEnv = new Environment(closure);
                for (int i = 0; i < expr.params.size(); i++) {
                    Token param = expr.params.get(i);
                    Object argVal = i < args.size() ? args.get(i) : null;
                    callEnv.define(param, param.lexeme, argVal);
                }
                return interpreter.evaluateInEnvironment(expr.body, callEnv);
            }
        };
    }

    // Execute a series of statements within a given environment.
    // Returns the last non-null value produced by an expression statement, if any.
    public Object executeBlock(java.util.List<Stmt> statements, Environment env) {
        Environment previous = this.environment;
        Object last = null;
        try {
            this.environment = env;
            for (Stmt statement : statements) {
                last = execute(statement);
            }
        } finally {
            this.environment = previous;
        }
        return last;
    }

    // Evaluate an expression within a given environment and restore previous afterwards.
    public Object evaluateInEnvironment(Expr expr, Environment env) {
        Environment previous = this.environment;
        try {
            this.environment = env;
            return evaluate(expr);
        } finally {
            this.environment = previous;
        }
    }
} 