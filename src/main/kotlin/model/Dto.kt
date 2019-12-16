package model

import org.jetbrains.exposed.sql.Table

object Account : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val balance = long("balance")
}

data class CreateAccountDto(val balance: Long)

data class MoneyTransferDto(val fromId: Int,
                            val toId: Int,
                            val amount: Long)