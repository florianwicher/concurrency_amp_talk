import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt

interface TwoValueCache<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V)
}

class LockBasedTwoValueCache<K, V> : TwoValueCache<K, V> {
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

class ImmutableObjectTwoValueCache<K, V> : TwoValueCache<K, V> {

    data class KeyValuePair<K, V>(val key: K, val value: V)

    @Volatile
    private var pair: KeyValuePair<K, V>? = null

    override fun get(key: K): V? {
        val currentPair = pair
        return if (currentPair != null && currentPair.key == key) currentPair.value else null
    }

    override fun put(key: K, value: V) {
        pair = KeyValuePair(key, value)
    }
}

class TwoValueCacheTest {

    @Test
    fun `compare locks and volatile objects`() {
        setOf<TwoValueCache<Int, String>>(LockBasedTwoValueCache(), ImmutableObjectTwoValueCache()).forEach {
            val blaster = blast {
                val j = nextInt()
                if (nextBoolean()) {
                    val value = "Value-$j"
                    it.put(j, value)
                } else {
                    it.get(j)
                    if (it.get(j) == "asdf") {
                        println("This is just dummy-use of the value to avoid compiler optimization")
                    }
                }
            }
            println("${it.javaClass.simpleName}: ${blaster.duration.inWholeMilliseconds} ms")
        }
    }
}