package simplf;
 
import java.util.List;

class SimplfFunction implements SimplfCallable {
    private final Stmt.Function declaration;
    private Environment closure;

    SimplfFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    public void setClosure(Environment environment) {
        this.closure = environment;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment callEnv = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            Token param = declaration.params.get(i);
            Object argVal = i < args.size() ? args.get(i) : null;
            callEnv.define(param, param.lexeme, argVal);
        }
        return interpreter.executeBlock(declaration.body, callEnv);
    }

    @Override
    public String toString() {
        return "<fn >";
    }

}