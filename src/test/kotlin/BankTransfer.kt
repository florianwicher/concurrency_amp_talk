import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.opentest4j.AssertionFailedError
import java.lang.Thread.sleep
import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit.*

private class BankAccount(
    val accountNumber: Int,
    private var _balance: Int
) {
    @get:Synchronized
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

    fun transfer(amount: Int, toAccount: BankAccount) {
        if (withdraw(amount)) toAccount.deposit(amount)
    }
}

class BankTransferTest {

    @Test
    fun `when self-transferring, then money supply is conserved`() {
        val account = BankAccount(1, 100)
        var exception: AssertionFailedError? = null

        fun transferer() {
            account.transfer(1, account)
        }

        fun auditer() {
            try {
                assertEquals(100, account.balance)
            } catch (e: AssertionFailedError) {
                exception = e
            }
        }

        newScheduledThreadPool(2).apply {
            forever(::auditer)
            forever(::transferer)
        }

        sleep(100)
        exception?.let { throw it }
    }


    @Test
    @Timeout(1)
    fun `money supply is conserved after a bunch of transfers between a set of accounts`() {
        fun randomAmount() = (1..1000).random()

        val accounts = List(2) { BankAccount(it, randomAmount()) }
        val moneyInCirculationBefore = accounts.sumOf { it.balance }

        blast(threadCount = 1) {
            val from = accounts.random()
            val to = accounts.random()
            val amount = randomAmount()
            from.transfer(amount, to)
        }

        val moneyInCirculationAfter = accounts.sumOf { it.balance }
        assertEquals(moneyInCirculationBefore, moneyInCirculationAfter)
    }


    fun ScheduledExecutorService.forever(block: () -> Unit) = scheduleAtFixedRate(block, 0, 1, NANOSECONDS)

}

//val (smaller, larger) = kotlin.collections.listOf(this, toAccount).sortedBy { it.accountNumber }
//synchronized(smaller) {
//    synchronized(larger) {
//        if (withdraw(amount)) toAccount.deposit(amount)
//    }
//}