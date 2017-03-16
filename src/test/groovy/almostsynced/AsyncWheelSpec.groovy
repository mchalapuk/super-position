package almostsynced

import spock.lang.Specification

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
class AsyncWheelSpec extends Specification {

    class TestState {
        String data = "initial"
    }

    def "throws when initializing with null constructor" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize null

        then:
        def e = thrown(NullPointerException)
        e.message == "constructor"
    }

    def "throws when reading not initialized" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.getReader()

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel must be initialized before reading"
    }

    def "throws when writing not initialized" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.getWriter()

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel must be initialized before writing"
    }

    def "throws when initializing twice" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize {}
        testedWheel.initialize {}

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel already initialized"
    }

    def "throws when calling Writer.write before Writer.read" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize {}
        testedWheel.getWriter().write {}

        then:
        def e = thrown(IllegalStateException)
        e.message == "state must be read before writing"
    }

    def "initializes first copy of state" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def writer = testedWheel.getWriter()

        TestState state0 = null
        writer.read({ state -> state0 = state })

        then:
        state0.data == "initial"
    }

    def "initializes second copy of state" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def writer = testedWheel.getWriter()

        writer.read({ state -> })
        writer.write({ state -> })

        TestState state1 = null
        writer.read({ state -> state1 = state })

        then:
        state1.data == "initial"
    }

    def "initializes third copy of state" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def writer = testedWheel.getWriter()

        writer.read({ state -> })
        writer.write({ state -> })
        writer.read({ state -> })
        writer.write({ state -> })

        TestState state2 = null
        writer.read({ state -> state2 = state })

        then:
        state2.data == "initial"
    }

    def "writer reads state modified in previous tick" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def writer = testedWheel.getWriter()

        writer.read({ state -> })
        writer.write({ state -> state.data = "modified" })

        TestState state2 = null
        writer.read({ state -> state2 = state })

        then:
        state2.data == "modified"
    }

    def "writer reads state modified two ticks before" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def writer = testedWheel.getWriter()

        writer.read({ state -> })
        writer.write({ state -> state.data = "modified" })
        writer.read({ state -> })
        writer.write({ state -> })

        TestState state2 = null
        writer.read({ state -> state2 = state })

        then:
        state2.data == "modified"
    }

    def "reader not ready after initialization" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def reader = testedWheel.getReader()

        def ready = reader.read({ state -> })

        then:
        !ready
    }

    def "reader ready after first copy modified" () {
        given:
        def testedWheel = new AsyncWheel<TestState>()

        when:
        testedWheel.initialize { new TestState() }
        def writer = testedWheel.getWriter()
        def reader = testedWheel.getReader()

        writer.read({ state -> })
        writer.write({ state -> })
        def ready = reader.read({ state -> })

        then:
        ready
    }
}
