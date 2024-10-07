import java.util.concurrent.CountDownLatch
import kotlin.time.Duration
import kotlin.time.measureTime

class Blaster(
    val numThreads: Int,
    val numIterations: Int,
    val workload: () -> Unit
) {

    val hasAlreadyRun get() = _duration != null

    private var _duration: Duration? = null
    val duration get() = _duration!!

    fun run(): Blaster {
        if(hasAlreadyRun) throw IllegalStateException("Blaster can only be run once")
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

        return this
    }
}

fun blast(threads: Int = 100, iterationsPerThread: Int = 100_000,
          workload: () -> Unit) = Blaster(threads, iterationsPerThread) { workload() }.also { it.run() }