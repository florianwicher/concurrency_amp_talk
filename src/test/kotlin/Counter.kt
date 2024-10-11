import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.time.times

class Counter {
    @Volatile
    var count = 0

    @Synchronized
    fun increment() {
        count++
    }
}

class OptimisticCounter {
    private var _count = AtomicInteger(0)

    @get:Synchronized
    val count get() = _count.get()

    fun increment() {
        while (true) {
            val currentCount = _count.get()
            val newCount = currentCount + 1
            if (_count.compareAndSet(currentCount, newCount)) return
        }
    }
}

class CounterTest {

    @Test
    fun `counter should count the number increment was called`() {
        val counter = Counter()

        val blaster = blast(threadCount = 2, iterationsPerThread = 100_000) { counter.increment() }

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

    @Test
    fun `pessimistic vs optimistic counter`() {
        val pessimisticCounter = Counter()
        val optimisticCounter = OptimisticCounter()

        val threadCount = 1
        val iterations = 10_000_000

        // prevent biased locking optimization
        pessimisticCounter.count

        val pessimisticBlaster = blast(threadCount, iterations) {
            pessimisticCounter.increment()
        }

        val optimisticBlaster = blast(threadCount, iterations) {
            optimisticCounter.increment()
        }

        val fasterImplBlaster =
            if (pessimisticBlaster.duration < optimisticBlaster.duration) pessimisticBlaster else optimisticBlaster
        val slowerImpl =
            if (pessimisticBlaster.duration < optimisticBlaster.duration) optimisticBlaster else pessimisticBlaster
        val percentFaster =
            (100 * (slowerImpl.duration - fasterImplBlaster.duration) / slowerImpl.duration).roundToInt()
        val fasterImpl = if (fasterImplBlaster == pessimisticBlaster) "Pessimistic counter" else "Optimistic counter"

        println(
            """Pessimistic counter: ${pessimisticBlaster.duration}
              |Optimistic counter: ${optimisticBlaster.duration}
              |$fasterImpl is $percentFaster% faster""".trimMargin()
        )
    }
}