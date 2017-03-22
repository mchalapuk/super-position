package almostsynced;

import javax.annotation.Nonnull;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

    public interface Writer<Data> {
        void read(@Nonnull Consumer<Data> readTick);
        void write(@Nonnull Consumer<Data> writeTick);
    }

    private final AtomicReference<StateCopy<Data>> free = new AtomicReference<>(); // accessed by reader and writer

    private Writer<Data> writer;
    private Reader<Data> reader;

    public void initialize(final @Nonnull Supplier<Data> constructor) {
        checkNotNull(constructor, "constructor");
        checkState(free.get() == null, "wheel already initialized");

        final List<Consumer<Data>> freeUpdates = new LinkedList<>();
        final List<Consumer<Data>> writingUpdates = new LinkedList<>();
        final List<Consumer<Data>> readingUpdates = new LinkedList<>();

        free.set(new StateCopy<>(constructor.get(), freeUpdates, writingUpdates, readingUpdates));
        writer = new WriterImpl(new StateCopy<>(constructor.get(), writingUpdates, readingUpdates, freeUpdates));
        reader = new ReaderImpl(new StateCopy<>(constructor.get(), readingUpdates, writingUpdates, freeUpdates));
    }

    public @Nonnull Reader<Data> getReader() {
        checkState(reader != null, "wheel must be initialized before stateCopy");
        return reader;
    }

    public @Nonnull Writer<Data> getWriter() {
        checkState(writer != null, "wheel must be initialized before stateCopy");
        return writer;
    }

    private class ReaderImpl implements Reader<Data> {
        private StateCopy<Data> stateCopy;

        public ReaderImpl(final @Nonnull StateCopy<Data> stateCopy) {
            this.stateCopy = stateCopy;
        }

        @Override
        public boolean read(final @Nonnull Consumer<Data> readTick) {
            checkNotNull(readTick, "readTick");

            stateCopy = free.getAndSet(stateCopy);
            return stateCopy.read(readTick);
        }
    }

    private class WriterImpl implements Writer<Data> {
        private final Writer<Data> readingPhase = new Writer<Data>() {

            @Override
            public void read(final @Nonnull Consumer<Data> readTick) {
                stateCopy.updateAndRead(readTick);
                currentPhase = writingPhase;
            }

            @Override
            public void write(final @Nonnull Consumer<Data> writeTick) {
                throw new IllegalStateException("stateCopy must be read before stateCopy");
            }
        };

        private final Writer<Data> writingPhase = new Writer<Data>() {
            @Override
            public void read(final @Nonnull Consumer<Data> readTick) {
                throw new IllegalStateException("stateCopy already read");
            }

            @Override
            public void write(final @Nonnull Consumer<Data> writeTick) {
                stateCopy.write(writeTick);
                stateCopy = free.getAndSet(stateCopy);
                currentPhase = readingPhase;
            }
        };

        private Writer<Data> currentPhase = readingPhase;
        private StateCopy<Data> stateCopy;

        public WriterImpl(final @Nonnull StateCopy<Data> stateCopy) {
            this.stateCopy = stateCopy;
        }

        @Override
        public void write(final @Nonnull Consumer<Data> writeTick) {
            currentPhase.write(checkNotNull(writeTick, "writeTick"));
        }

        @Override
        public void read(final @Nonnull Consumer<Data> readTick) {
            currentPhase.read(checkNotNull(readTick, "readTick"));
        }
    }

    private static class StateCopy<Data> {
        private final Data data;

        // accessed only by writer
        private List<Consumer<Data>> updates;
        private List<Consumer<Data>> other0Updates;
        private List<Consumer<Data>> other1Updates;

        // accessed by reader and writer
        private volatile ReadingStrategy<Data> readingStrategy = this::notReadyRead;

        public StateCopy(final @Nonnull Data data,
                         final @Nonnull List<Consumer<Data>> updates,
                         final @Nonnull List<Consumer<Data>> other0Updates,
                         final @Nonnull List<Consumer<Data>> other1Updates) {
            this.data = data;
            this.updates = updates;
            this.other0Updates = other0Updates;
            this.other1Updates = other1Updates;
        }

        public boolean read(final @Nonnull Consumer<Data> readTick) {
            return readingStrategy.read(readTick);
        }

        public void updateAndRead(final @Nonnull Consumer<Data> readTick) {
            updates.forEach(tick -> tick.accept(data));
            updates.clear();
            readTick.accept(data);
        }

        public void write(final @Nonnull Consumer<Data> writeTick) {
            writeTick.accept(data);

            other0Updates.add(writeTick);
            other1Updates.add(writeTick);

            readingStrategy = this::readyRead;
        }

        @SuppressWarnings("unused")
        private boolean notReadyRead(final @Nonnull Consumer<Data> readTick) {
            return false;
        }

        private boolean readyRead(final @Nonnull Consumer<Data> readTick) {
            readingStrategy = this::notReadyRead;
            readTick.accept(data);
            return true;
        }
    }

    private interface ReadingStrategy<Data> {
        boolean read(@Nonnull Consumer<Data> readTick);
    }
}
