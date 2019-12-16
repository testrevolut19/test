import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.ktor.application.Application
import io.ktor.application.ApplicationStopped
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import resources.account
import service.DatabaseAccountService
import java.lang.IllegalArgumentException

fun Application.module() {
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call.respond(HttpStatusCode.BadRequest, it.message ?: "") }
        exception<MismatchedInputException> { call.respond(HttpStatusCode.BadRequest, it.message ?: "") }
        exception<NoSuchElementException> { call.respond(HttpStatusCode.NotFound, it.message ?: "") }
    }
    install(Routing) { account(DatabaseAccountService()) }

    DatabaseUtil.init()

    environment.monitor.subscribe(ApplicationStopped) { DatabaseUtil.clear() }
}

@KtorExperimentalAPI
fun main(args: Array<String>) {
    embeddedServer(CIO, commandLineEnvironment(args)).start(wait = true)
}
