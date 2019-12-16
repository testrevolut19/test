package service

import model.Account
import model.Account.balance
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

interface AccountService {
    fun create(balance: Long = 0L): Int
    fun delete(id: Int)
    fun getBalance(id: Int): Long
    fun transfer(fromId: Int, toId: Int, amount: Long)
}

class DatabaseAccountService : AccountService {
    override fun delete(id: Int) {
        transaction {
            val balance = Account.slice(balance)
                .select { Account.id eq id }
                .single()[balance]

            require(balance == 0L)

            Account.deleteWhere { Account.id eq id }
        }
    }

    override fun create(balance: Long): Int {
        return transaction { Account.insert { it[Account.balance] = balance } get Account.id }
    }

    override fun getBalance(id: Int): Long {
        return transaction {
            Account.slice(balance)
                .select { Account.id eq id }
                .single()[balance]
        }
    }

    override fun transfer(fromId: Int, toId: Int, amount: Long) {
        transaction {
            require(amount > 0) { "amount must be positive: $amount" }
            require(fromId != toId) { "illegal transfer" }// avoid deadlock

            val minId = minOf(fromId, toId)
            val maxId = maxOf(fromId, toId)

            // avoid deadlock
            val minIdBalance = Account.slice(balance).select { Account.id eq minId }.forUpdate().single()[balance]
            val maxIdBalance = Account.slice(balance).select { Account.id eq maxId }.forUpdate().single()[balance]

            val fromBalance = if (fromId == minId) minIdBalance else maxIdBalance

            require(fromBalance >= amount) { "Not enough money for transfer, balance: $fromBalance, amount: $amount" }

            Account.update({ Account.id eq fromId }) {
                with(SqlExpressionBuilder) {
                    it.update(balance, balance - amount)
                }
            }

            Account.update({ Account.id eq toId }) {
                with(SqlExpressionBuilder) {
                    it.update(balance, balance + amount)
                }
            }
        }
    }
}