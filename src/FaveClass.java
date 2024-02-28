import java.util.List;

public class FaveClass implements FaveCallable {
    final String name;

    public FaveClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        FaveInstance instance = new FaveInstance(this);
        return instance;
    }
}
