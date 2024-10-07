import org.junit.jupiter.api.Test

class Counter {
    @Volatile
    var count = 0
    fun increment() {
        count++
    }
}

class CounterTest {

    @Test
    fun `counter should count the number increment was called`() {
        val counter = Counter()

        val blaster = blast(threads = 1) { counter.increment() }

        val timesIncrementWasCalled = blaster.numThreads * blaster.numIterations
        val actualCount = counter.count

        println(
            """Expected count: $timesIncrementWasCalled
              |Actual count: $actualCount
              |Missed increments: ${timesIncrementWasCalled - actualCount},
              |that's ${100 * (timesIncrementWasCalled - actualCount) / timesIncrementWasCalled}%""".trimMargin()
        )
        assert(actualCount == timesIncrementWasCalled)
    }
}