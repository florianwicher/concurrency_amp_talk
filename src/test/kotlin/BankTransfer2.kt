import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.concurrent.Volatile

class DeadlockFreeBankAccount(
    val accountNumber: Int,
    @Volatile private var _balance: Int
) {
    val balance get() = _balance

    @Synchronized
    fun deposit(amount: Int) {
        _balance += amount
    }

    @Synchronized
    fun withdraw(amount: Int): Boolean {
        if (_balance >= amount) {
            _balance -= amount
            return true
        }
        return false
    }

    // Transfers are atomic. The total amount of money in the system seen by other threads is preserved.
    fun transfer(amount: Int, to: DeadlockFreeBankAccount) {
        val (smaller,larger) = listOf(this, to).sortedBy { it.accountNumber }
        synchronized(smaller) {
            synchronized(larger) {
                if (withdraw(amount)) to.deposit(amount)
            }
        }

        if (withdraw(amount)) to.deposit(amount)
    }
}

class DeadlockFreeBankTransferTest {

    @Test
    fun `money should be preserved`() {
        fun randomAmount() = (1..1000).random()

        val accounts = List(2) { DeadlockFreeBankAccount(it, randomAmount()) }
        val moneyInCirculationBefore = accounts.sumOf { it.balance }

        blast {
            val from = accounts.random()
            val to = accounts.random()
            val amount = randomAmount()
            from.transfer(amount, to)
        }

        val moneyInCirculationAfter = accounts.sumOf { it.balance }
        assertEquals(moneyInCirculationBefore, moneyInCirculationAfter)
    }
}