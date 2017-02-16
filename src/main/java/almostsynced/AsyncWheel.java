package almostsynced;

import java.lang.reflect.Array;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static almostsynced.Preconditions.checkNotNull;
import static almostsynced.Preconditions.checkState;

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
public class AsyncWheel<Data> {

    public interface Reader<Data> {

        boolean read(Consumer<Data> readTick);
    }

    public interface Writer<Data> extends Reader<Data> {

        void write(Consumer<Data> writeTick);
    }

    private final Class<Data> dataClass;
    private volatile Data[] data;

    public AsyncWheel(Class<Data> dataClass) {
        this.dataClass = checkNotNull(dataClass, "dataClass");
    }

    public void initialize(Supplier<Data> constructor) {
        checkNotNull(constructor, "constructor");
        checkState(data == null, "wheel already initialized");

        //noinspection unchecked
        data = (Data[]) Array.newInstance(dataClass, 3);
        for (int i = 0; i < data.length; ++i) {
            data[0] = constructor.get();
        }
    }

    public Reader<Data> getReader() {
        checkState(data != null, "wheel must be initialized before reading");
        return readTick -> false;
    }

    public Writer<Data> getWriter() {
        checkState(data != null, "wheel must be initialized before writing");

        return new Writer<Data>() {
            private Writer<Data> readingState = new Writer<Data>() {

                @Override
                public boolean read(Consumer<Data> readTick) {
                    readTick.accept(data[0]);
                    currentState = writingState;
                    return true;
                }

                @Override
                public void write(Consumer<Data> writeTick) {
                    throw new IllegalStateException("state must be read before writing");
                }
            };

            private Writer<Data> writingState = new Writer<Data>() {
                @Override
                public boolean read(Consumer<Data> readTick) {
                    throw new IllegalStateException("state already read");
                }

                @Override
                public void write(Consumer<Data> writeTick) {
                    currentState = readingState;
                }
            };

            private Writer<Data> currentState = readingState;

            @Override
            public void write(Consumer<Data> writeTick) {
                currentState.write(writeTick);
            }

            @Override
            public boolean read(Consumer<Data> readTick) {
                return currentState.read(readTick);
            }
        };
    }
}
