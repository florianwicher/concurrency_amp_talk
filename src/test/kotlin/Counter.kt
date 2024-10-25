import org.junit.jupiter.api.Test

class Counter {

    var count = 0
        get() {
            return field
        }

    fun increment() {
            count++
    }
}

class CounterTest {

    @Test
    fun `counter should count the number of times increment was called`() {
        val counter = Counter()

        val blaster = blast(threadCount = 1, iterationsPerThread = 100_000) { counter.increment() }

        val timesIncrementWasCalled = blaster.threadCount * blaster.iterationsPerThread
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