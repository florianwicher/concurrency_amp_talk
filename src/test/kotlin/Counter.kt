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
    fun `counter should count all increments`() {
        val counter = Counter()

        val blaster = blast { counter.increment() }.also { it.run() }

        val expectedCount = blaster.numThreads * blaster.numIterations
        val actualCount = counter.count

        assert(actualCount == expectedCount) {
            """Expected count: $expectedCount
              |Actual count: $actualCount
              |Missed increments: ${expectedCount - actualCount}, that's ${100 * (expectedCount - actualCount) / expectedCount}%""".trimMargin()
        }
    }
}