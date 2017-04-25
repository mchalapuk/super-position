package morejuice;

import javax.annotation.Nonnull;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static morejuice.Preconditions.checkNotNull;
import static morejuice.Preconditions.checkState;

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
public class SuperPosition<Space> {
    public interface Reader<Space> {
        boolean read(@Nonnull Consumer<Space> readTick);
    }

    public interface Mover<Space> {
        void read(@Nonnull Consumer<Space> readTick);
        void move(@Nonnull Consumer<Space> moveTick);
    }

    private final AtomicReference<Position<Space>> free = new AtomicReference<>(); // accessed by reader and mover

    private Mover<Space> mover;
    private Reader<Space> reader;

    public void initialize(final @Nonnull Supplier<Space> constructor) {
        checkNotNull(constructor, "constructor");
        checkState(free.get() == null, "wheel already initialized");

        final List<Consumer<Space>> freeUpdates = new LinkedList<>();
        final List<Consumer<Space>> writingUpdates = new LinkedList<>();
        final List<Consumer<Space>> readingUpdates = new LinkedList<>();

        free.set(new Position<>(constructor.get(), freeUpdates, writingUpdates, readingUpdates));
        mover = new MoverImpl(new Position<>(constructor.get(), writingUpdates, readingUpdates, freeUpdates));
        reader = new ReaderImpl(new Position<>(constructor.get(), readingUpdates, writingUpdates, freeUpdates));
    }

    public @Nonnull Reader<Space> getReader() {
        checkState(reader != null, "wheel must be initialized before position");
        return reader;
    }

    public @Nonnull Mover<Space> getMover() {
        checkState(mover != null, "wheel must be initialized before position");
        return mover;
    }

    private class ReaderImpl implements Reader<Space> {
        private Position<Space> position;

        public ReaderImpl(final @Nonnull Position<Space> position) {
            this.position = position;
        }

        @Override
        public boolean read(final @Nonnull Consumer<Space> readTick) {
            checkNotNull(readTick, "readTick");

            position = free.getAndSet(position);
            return position.read(readTick);
        }
    }

    private class MoverImpl implements Mover<Space> {
        private final Mover<Space> readingPhase = new Mover<Space>() {

            @Override
            public void read(final @Nonnull Consumer<Space> readTick) {
                position.updateAndRead(readTick);
                currentPhase = writingPhase;
            }

            @Override
            public void move(final @Nonnull Consumer<Space> moveTick) {
                throw new IllegalStateException("position must be read before position");
            }
        };

        private final Mover<Space> writingPhase = new Mover<Space>() {
            @Override
            public void read(final @Nonnull Consumer<Space> readTick) {
                throw new IllegalStateException("position already read");
            }

            @Override
            public void move(final @Nonnull Consumer<Space> moveTick) {
                position.write(moveTick);
                position = free.getAndSet(position);
                currentPhase = readingPhase;
            }
        };

        private Mover<Space> currentPhase = readingPhase;
        private Position<Space> position;

        public MoverImpl(final @Nonnull Position<Space> position) {
            this.position = position;
        }

        @Override
        public void move(final @Nonnull Consumer<Space> moveTick) {
            currentPhase.move(checkNotNull(moveTick, "writeTick"));
        }

        @Override
        public void read(final @Nonnull Consumer<Space> readTick) {
            currentPhase.read(checkNotNull(readTick, "readTick"));
        }
    }

    private static class Position<Space> {
        private final Space data;

        // accessed only by mover
        private List<Consumer<Space>> updates;
        private List<Consumer<Space>> other0Updates;
        private List<Consumer<Space>> other1Updates;

        // accessed by reader and mover
        private volatile ReadingStrategy<Space> readingStrategy = this::notReadyRead;

        public Position(final @Nonnull Space initial,
                        final @Nonnull List<Consumer<Space>> updates,
                        final @Nonnull List<Consumer<Space>> other0Updates,
                        final @Nonnull List<Consumer<Space>> other1Updates) {
            this.data = initial;
            this.updates = updates;
            this.other0Updates = other0Updates;
            this.other1Updates = other1Updates;
        }

        public boolean read(final @Nonnull Consumer<Space> readTick) {
            return readingStrategy.read(readTick);
        }

        public void updateAndRead(final @Nonnull Consumer<Space> readTick) {
            updates.forEach(tick -> tick.accept(data));
            updates.clear();
            readTick.accept(data);
        }

        public void write(final @Nonnull Consumer<Space> writeTick) {
            writeTick.accept(data);

            other0Updates.add(writeTick);
            other1Updates.add(writeTick);

            readingStrategy = this::readyRead;
        }

        @SuppressWarnings("unused")
        private boolean notReadyRead(final @Nonnull Consumer<Space> readTick) {
            return false;
        }

        private boolean readyRead(final @Nonnull Consumer<Space> readTick) {
            readingStrategy = this::notReadyRead;
            readTick.accept(data);
            return true;
        }
    }

    private interface ReadingStrategy<Space> {
        boolean read(@Nonnull Consumer<Space> readTick);
    }
}
