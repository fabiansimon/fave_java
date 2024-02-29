import java.util.HashMap;
import java.util.Map;

public class FaveInstance {
    private FaveClass fClass;
    private final Map<String, Object> fields = new HashMap<>();

    FaveInstance(FaveClass fClass) {
        this.fClass = fClass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        FaveFunction method = fClass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return fClass.name + " instance";
    }
}
