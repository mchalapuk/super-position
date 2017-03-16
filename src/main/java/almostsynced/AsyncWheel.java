package almostsynced;

import javax.annotation.Nonnull;
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

        boolean read(@Nonnull Consumer<Data> readTick);
    }

    public interface Writer<Data> extends Reader<Data> {

        void write(@Nonnull Consumer<Data> writeTick);
    }

    private final Class<Data> dataClass;
    private volatile StateCopy[] copies;

    private int writerIndex = 0;

    public AsyncWheel(final @Nonnull Class<Data> dataClass) {
        this.dataClass = checkNotNull(dataClass, "dataClass");
    }

    public void initialize(final @Nonnull Supplier<Data> constructor) {
        checkNotNull(constructor, "constructor");
        checkState(copies == null, "wheel already initialized");

        //noinspection unchecked
        copies = (StateCopy[]) Array.newInstance(StateCopy.class, 3);
        for (int i = 0; i < copies.length; ++i) {
            copies[i] = new StateCopy(constructor.get());
        }
    }

    public @Nonnull Reader<Data> getReader() {
        checkState(copies != null, "wheel must be initialized before reading");
        return readTick -> false;
    }

    public @Nonnull Writer<Data> getWriter() {
        checkState(copies != null, "wheel must be initialized before writing");

        return new WriterImpl();
    }

    private class WriterImpl implements Writer<Data> {
        private Writer<Data> readingPhase = new Writer<Data>() {

            @Override
            public boolean read(final @Nonnull Consumer<Data> readTick) {
                checkNotNull(readTick, "readTick");

                final StateCopy current = copies[writerIndex];
                updateWithPreviousTicks(current);

                readTick.accept(current.data);

                currentPhase = writingPhase;
                return true;
            }

            @Override
            public void write(final @Nonnull Consumer<Data> writeTick) {
                throw new IllegalStateException("state must be read before writing");
            }

            private void updateWithPreviousTicks(StateCopy current) {
                final StateCopy currentMinusOne = copies[(writerIndex + 2) % 3];
                final StateCopy currentMinusTwo = copies[(writerIndex + 1) % 3];
                currentMinusOne.lastWriteTick.accept(current.data);
                currentMinusTwo.lastWriteTick.accept(current.data);
            }

        };

        private Writer<Data> writingPhase = new Writer<Data>() {
            @Override
            public boolean read(final @Nonnull Consumer<Data> readTick) {
                throw new IllegalStateException("state already read");
            }

            @Override
            public void write(final @Nonnull Consumer<Data> writeTick) {
                checkNotNull(writeTick, "writeTick");

                final StateCopy current = copies[writerIndex];
                current.lastWriteTick = writeTick;

                writeTick.accept(current.data);

                writerIndex = (writerIndex + 1) % 3;
                currentPhase = readingPhase;
            }
        };

        private Writer<Data> currentPhase = readingPhase;

        @Override
        public void write(final @Nonnull Consumer<Data> writeTick) {
            currentPhase.write(writeTick);
        }

        @Override
        public boolean read(final @Nonnull Consumer<Data> readTick) {
            return currentPhase.read(readTick);
        }
    }

    private class StateCopy {
        private Data data;
        private Consumer<Data> lastWriteTick;

        public StateCopy(final Data data) {
            this.data = data;
            this.lastWriteTick = unused -> {};
        }
    }
}
