package almostsynced;

/**
 * Clone of Guava's Preconditions class (this library is dependency free).
 *
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
public class Preconditions {

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
