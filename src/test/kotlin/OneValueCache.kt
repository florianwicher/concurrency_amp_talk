import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt

interface OneValueCache<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V)
}

class LockBasedOneValueCache<K, V> : OneValueCache<K, V> {
    private var key: K? = null
    private var value: V? = null

    @Synchronized
    override fun get(key: K) = if (this.key == key) value else null

    @Synchronized
    override fun put(key: K, value: V) {
        this.key = key
        this.value = value
    }
}

class RcuOneValueCache<K, V> : OneValueCache<K, V> {

    data class Pair<K, V>(val key: K, val value: V)

    @Volatile
    private var pair: Pair<K, V>? = null

    override fun get(key: K): V? {
        val currentPair = pair
        return if (currentPair != null && currentPair.key == key) currentPair.value else null
    }

    override fun put(key: K, value: V) {
        pair = Pair(key, value)
    }
}

class OneValueCacheTest {

    @Test
    fun `compare locks and volatile objects`() {
        setOf<OneValueCache<Int, Int>>(LockBasedOneValueCache(), RcuOneValueCache()).map {
            val blaster = blast {
                if (nextBoolean()) {
                    it.put(nextInt(), nextInt())
                } else {
                    if (it.get(nextInt()) != null) {
                        println("This is just dummy-use of the value to avoid compiler optimization")
                    }
                }
            }
            println("${it::class.simpleName}: ${blaster.duration.inWholeMilliseconds} ms")
        }
    }
}