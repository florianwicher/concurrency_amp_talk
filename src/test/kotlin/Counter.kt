import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class Counter {
    @Volatile
    var count = 0
    fun increment() {
        count++
    }
}

class OptimisticCounter {
    private var count = AtomicInteger(0)

    fun increment() {
        while (true) {
            val currentCount = count.get()
            val newCount = currentCount + 1
            if (count.compareAndSet(currentCount, newCount)) return
        }
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

    @Test
    fun `pessimistic vs optimistic counter`() {
        val pessimisticCounter = Counter()
        val optimisticCounter = OptimisticCounter()

        val threadCount = 1
        val iterations = 100_000

        val pessimisticBlaster = blast(threadCount, iterations) {
            pessimisticCounter.increment()
        }

        val optimisticBlaster = blast(threadCount, iterations) {
            optimisticCounter.increment()
        }

        println(
            """Regular counter: ${pessimisticBlaster.duration}
              |Optimistic counter: ${optimisticBlaster.duration}""".trimMargin()
        )
    }
}