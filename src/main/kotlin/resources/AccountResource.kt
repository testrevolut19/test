package resources

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import model.CreateAccountDto
import model.MoneyTransferDto
import service.DatabaseAccountService

fun Route.account(service: DatabaseAccountService) {
    route("/account") {
        get("/{id}/balance") {
            val id = requireNotNull(call.parameters["id"]?.toInt())

            call.respondText("${service.getBalance(id)}")
        }
        post("/") {
            val createAccountDto = call.receive<CreateAccountDto>()

            call.respondText("${service.create(createAccountDto.balance)}")
        }
        post("/transfer") {
            val moneyTransferDto = call.receive<MoneyTransferDto>()

            with(moneyTransferDto) { service.transfer(fromId, toId, amount) }

            call.respond(HttpStatusCode.OK)
        }
        delete("/{id}") {
            val id = requireNotNull(call.parameters["id"]?.toInt())

            service.delete(id)

            call.respond(HttpStatusCode.OK)
        }
    }
}