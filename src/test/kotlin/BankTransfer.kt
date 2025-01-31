import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.opentest4j.AssertionFailedError
import java.lang.Thread.sleep
import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.NANOSECONDS

class BankAccount(
    val accountNumber: Int,
    private var _balance: Int
) {
    val balance
        get() = synchronized(this) { _balance }

    fun deposit(amount: Int) = synchronized(this) {
        _balance += amount
    }

    fun withdraw(amount: Int): Boolean = synchronized(this) {
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
    fun `when transferring, then money supply is conserved`() {
        val account = BankAccount(1, 100)
        var exception: AssertionFailedError? = null

        fun selfTransfer() = account.transfer(1, account)

        fun audit() = try {
            assertEquals(100, account.balance)
        } catch (e: AssertionFailedError) {
            exception = e
        }

        newScheduledThreadPool(1).apply {
            forever(::audit)
            forever(::selfTransfer)
        }

        sleep(100)
        exception?.let { throw it }
    }


    @Test
    @Timeout(1)
    fun `when transferring, then does not deadlock`() {
        val accounts = List(2) { BankAccount(it, 100) }

        blast(threadCount = 2) {
            val from = accounts.random()
            val to = accounts.random()
            from.transfer(1, to)
        }
    }
}

//val (smaller, larger) = listOf(this, toAccount).sortedBy { it.accountNumber }
//synchronized(smaller) {
//    synchronized(larger) {
//        if (withdraw(amount)) toAccount.deposit(amount)
//    }
//}