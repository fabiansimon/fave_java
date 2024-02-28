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

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    @Override
    public String toString() {
        return fClass.name + " instance";
    }
}
