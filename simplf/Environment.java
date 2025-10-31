package simplf; 

class Environment {
    private AssocList values;
    private final Environment enclosing;

    Environment() {
        //throw new UnsupportedOperationException("TODO: implement environments.");
        this.values = null;
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.values = null;
        this.enclosing = enclosing;
    }

    void define(Token varToken, String name, Object value) {
        values = new AssocList(name, value, values);
    }

    void assign(Token name, Object value) {
        for (AssocList cursor = values; cursor != null; cursor = cursor.next) {
            if (cursor.name.equals(name.lexeme)) {
                cursor.value = value;
                return;
            }
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '");
    }

    Object get(Token name) {
        for (AssocList cursor = values; cursor != null; cursor = cursor.next) {
            if (cursor.name.equals(name.lexeme)) {
                return cursor.value;
            }
        }
        if (enclosing != null) {
            return enclosing.get(name);
        }
        throw new RuntimeError(name, "Undefined variable '");
    }
}

