package almostsynced

import spock.lang.Specification

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
class AsyncWheelSpec extends Specification {

    class TestState {
        String data = ""

        TestState() {
        }
    }

    def "throws when constructing with null constructor" () {
        when:
        new AsyncWheel<TestState>(null)

        then:
        def e = thrown(NullPointerException)
        e.printStackTrace System.out
        e.message == "constructor"
    }

    def "throws when reading not initialized" () {
        given:
        def testedWheel = new AsyncWheel<TestState>({ -> new TestState() })

        when:
        testedWheel.getReader()

        then:
        def e = thrown(IllegalStateException)
        e.printStackTrace System.out
        e.message == "wheel must be initialized before reading"
    }

    def "throws when writing not initialized" () {
        given:
        def testedWheel = new AsyncWheel<TestState>({ -> new TestState() })

        when:
        testedWheel.getWriter()

        then:
        def e = thrown(IllegalStateException)
        e.printStackTrace System.out
        e.message == "wheel must be initialized before writing"
    }

    def "throws when initializing twice" () {
        given:
        def testedWheel = new AsyncWheel<TestState>({ -> new TestState() })

        when:
        testedWheel.initialize {}
        testedWheel.initialize {}

        then:
        def e = thrown(IllegalStateException)
        e.printStackTrace System.out
        e.message == "wheel already initialized"
    }

    def "throws when calling Writer.write before Writer.read" () {
        given:
        def testedWheel = new AsyncWheel<TestState>({ -> new TestState() })

        when:
        testedWheel.initialize {}
        testedWheel.getWriter().write {}

        then:
        def e = thrown(IllegalStateException)
        e.printStackTrace System.out
        e.message == "state must be read before writing"
    }

    def "initializes first copy of state" () {
        given:
        def testedWheel = new AsyncWheel<TestState>({ -> new TestState() })

        when:
        testedWheel.initialize { state -> state.data = "initial" }
        def writer = testedWheel.getWriter()

        TestState state0 = null
        writer.read({ state -> state0 = state })

        then:
        state0.data == "initial"
    }
}
