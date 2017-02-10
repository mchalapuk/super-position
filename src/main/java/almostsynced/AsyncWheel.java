package almostsynced;

import java.util.function.Consumer;

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
public class AsyncWheel<State> {

    public interface Reader<State> {

        boolean read(Consumer<State> readTick);
    }

    public interface Writer<State> {

        boolean write(Consumer<State> writeTick);
    }

    private final Class<State> stateClass;
    private boolean initialized = false;

    public AsyncWheel(Class<State> stateClass) {
        this.stateClass = checkNotNull(stateClass, "stateClass");
    }

    public void initialize(Consumer<State> initializer) {
        checkState(!initialized, "wheel already initialized");
        initialized = true;
    }

    public Reader<State> getReader() {
        checkState(initialized, "wheel must be initialized before reading");
        return readTick -> false;
    }

    public Writer<State> getWriter() {
        checkState(initialized, "wheel must be initialized before writing");
        return writeTick -> false;
    }

    private static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    private static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
