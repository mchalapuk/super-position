package morejuice

import spock.lang.Specification

/**
 * @author Maciej Chałapuk &lt;maciej@chalapuk.pl&gt;
 */
class SuperPositionSpec extends Specification {

    class TestState {
        String data = "initial"
    }

    def "throws when initializing with null constructor" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize null

        then:
        def e = thrown(NullPointerException)
        e.message == "constructor"
    }

    def "throws when reading not initialized" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.getReader()

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel must be initialized before position"
    }

    def "throws when moving not initialized" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.getMover()

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel must be initialized before position"
    }

    def "throws when initializing twice" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        testedPosition.initialize { new TestState() }

        then:
        def e = thrown(IllegalStateException)
        e.message == "wheel already initialized"
    }

    def "throws when calling Mover.move before Mover.read" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        testedPosition.getMover().move {}

        then:
        def e = thrown(IllegalStateException)
        e.message == "position must be read before position"
    }

    def "initializes first copy of state" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()

        TestState state0 = null
        mover.read({ state -> state0 = state })

        then:
        state0.data == "initial"
    }

    def "initializes second copy of state" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()

        mover.read({ state -> })
        mover.move({ state -> })

        TestState state1 = null
        mover.read({ state -> state1 = state })

        then:
        state1.data == "initial"
    }

    def "initializes third copy of state" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()

        mover.read({ state -> })
        mover.move({ state -> })
        mover.read({ state -> })
        mover.move({ state -> })

        TestState state2 = null
        mover.read({ state -> state2 = state })

        then:
        state2.data == "initial"
    }

    def "mover reads state modified in previous tick" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()

        mover.read({ state -> })
        mover.move({ state -> state.data = "modified" })

        TestState state2 = null
        mover.read({ state -> state2 = state })

        then:
        state2.data == "modified"
    }

    def "mover reads state modified two ticks before" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()

        mover.read({ state -> })
        mover.move({ state -> state.data = "modified" })
        mover.read({ state -> })
        mover.move({ state -> })

        TestState state2 = null
        mover.read({ state -> state2 = state })

        then:
        state2.data == "modified"
    }

    def "reader not ready after initialization" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def reader = testedPosition.getReader()

        def ready = reader.read({ state -> })

        then:
        !ready
    }

    def "reader ready after first copy modified" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()
        def reader = testedPosition.getReader()

        mover.read({ state -> })
        mover.move({ state -> })
        def ready = reader.read({ state -> })

        then:
        ready
    }

    def "reader reads state modified by mover" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()
        def reader = testedPosition.getReader()

        mover.read { state -> }
        mover.move { state -> state.data = "modified" }

        TestState state2 = null
        reader.read { state -> state2 = state }

        then:
        state2.data == "modified"
    }

    def "reader not ready after reading first state modified by mover" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()
        def reader = testedPosition.getReader()

        mover.read { state -> }
        mover.move { state ->  }

        reader.read { state -> }
        def ready = reader.read { state -> }

        then:
        !ready
    }

    def "reader not ready after reading last state modified by mover" () {
        given:
        def testedPosition = new SuperPosition<TestState>()

        when:
        testedPosition.initialize { new TestState() }
        def mover = testedPosition.getMover()
        def reader = testedPosition.getReader()

        mover.read { state -> }
        mover.move { state ->  }
        mover.read { state -> }
        mover.move { state ->  }
        mover.read { state -> }
        mover.move { state ->  }
        mover.read { state -> }
        mover.move { state ->  }

        reader.read { state -> }
        def ready = reader.read { state -> }

        then:
        !ready
    }
}
