import java.util.List;

public interface FaveCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> args);
}
