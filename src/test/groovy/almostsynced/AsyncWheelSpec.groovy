package almostsynced

import spock.lang.Specification

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
class AsyncWheelSpec extends Specification {

    class TestState {
        String data = ""
    }

    def "throws when constructing with null class literal" () {
        when:
        new AsyncWheel<TestState>(null)

        then:
        def e = thrown(NullPointerException)
        e.message == "stateClass"
    }

    def "throws when reading not initialized" () {
        given:
        def testedWheel = new AsyncWheel<TestState>(TestState.class)

        when:
        testedWheel.getReader()

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel must be initialized before reading"
    }

    def "throws when writing not initialized" () {
        given:
        def testedWheel = new AsyncWheel<TestState>(TestState.class)

        when:
        testedWheel.getWriter()

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel must be initialized before writing"
    }

    def "throws when initializing twice" () {
        given:
        def testedWheel = new AsyncWheel<TestState>(TestState.class)

        when:
        testedWheel.initialize { state -> state.data = "initial" }
        testedWheel.initialize { state -> state.data = "initial" }

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel already initialized"
    }
}
