import java.util.concurrent.CountDownLatch
import kotlin.time.Duration
import kotlin.time.measureTime

class Blaster(
    val numThreads: Int = 100,
    val numIterations: Int = 100_000,
    val workload: () -> Unit
) {

    private var _duration: Duration? = null
    val duration get() = _duration!!

    fun run() {
        val threads = mutableListOf<Thread>()
        val allThreadsReady = CountDownLatch(numThreads)
        val startWorking = CountDownLatch(1)

        repeat(numThreads) {
            threads.add(Thread {
                allThreadsReady.countDown()
                startWorking.await()
                repeat(numIterations) { workload() }
            }.also { it.start() })
        }
        _duration = measureTime {
            startWorking.countDown()
            threads.forEach(Thread::join)
        }
    }
}

fun blast(workload: () -> Unit) = Blaster { workload() }.also { it.run() }