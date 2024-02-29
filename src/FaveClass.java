import java.util.List;
import java.util.Map;

public class FaveClass implements FaveCallable {
    final String name;
    private final Map<String, FaveFunction> methods;

    public FaveClass(String name, Map<String, FaveFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    FaveFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        FaveFunction initializer = findMethod("init");
        if (initializer == null) return 0;

        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        FaveInstance instance = new FaveInstance(this);

        FaveFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, args);
        }

        return instance;
    }
}
