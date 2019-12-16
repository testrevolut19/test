import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import model.Account
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseUtil {
    fun init() {
        Database.connect(hikari())
        transaction { create(Account) }
    }

    fun clear() {
        transaction { SchemaUtils.drop(Account) }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = "jdbc:h2:mem:db"
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        return HikariDataSource(config)
    }
}