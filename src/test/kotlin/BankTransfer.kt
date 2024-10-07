import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.opentest4j.AssertionFailedError
import java.lang.Thread.sleep
import kotlin.concurrent.Volatile

private class BankAccount(
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

    fun transfer(amount: Int, to: BankAccount) {
        synchronized(this) {
            synchronized(to) {
                if (withdraw(amount)) to.deposit(amount)
            }
        }
    }
}

class BankTransferTest {

    @Test
    fun `money supply is conserved while transfer is in progress`() {
        val account = BankAccount(1, 100)
        var exception: AssertionFailedError? = null

        val transferer = untilInterrupted {
            account.transfer(1, account)
        }

        val auditer = untilInterrupted {
            try {
                assertEquals(100, account.balance)
            } catch (e: AssertionFailedError) {
                exception = e
            }
        }

        sleep(100)
        exception?.let { throw it }

        transferer.apply { interrupt(); join() }
        auditer.apply { interrupt(); join() }
    }

    @Test
    @Timeout(1)
    fun `money supply is conserved after a bunch of transfers`() {
        fun randomAmount() = (1..1000).random()

        val accounts = List(2) { BankAccount(it, randomAmount()) }
        val moneyInCirculationBefore = accounts.sumOf { it.balance }

        blast(threads = 2) {
            val from = accounts.random()
            val to = accounts.random()
            val amount = randomAmount()
            from.transfer(amount, to)
        }

        val moneyInCirculationAfter = accounts.sumOf { it.balance }
        assertEquals(moneyInCirculationBefore, moneyInCirculationAfter)
    }

    private fun untilInterrupted(block: (Thread) -> Unit) = object : Thread() {
        override fun run() {
            while (!interrupted()) block(this)
        }
    }.also { it.start() }
}