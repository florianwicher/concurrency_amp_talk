import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.util.Collections.synchronizedSet

class Singleton private constructor() {

    init {
        sleep(100) //works hard to create an instance
    }

    companion object {
        private var instance: Singleton? = null

        fun getInstance(): Singleton {
            if (instance == null) instance = Singleton()
            return instance!!
        }
    }
}

class SingletonTest {

    @Test
    fun `singleton should only exist once`() {
        val singletons = synchronizedSet(HashSet<Singleton>())
        blast(threadCount = 1) {
            singletons += Singleton.getInstance()
        }

        assertEquals(1, singletons.size)
    }
}