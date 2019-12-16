import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import kotlinx.coroutines.*
import model.CreateAccountDto
import model.MoneyTransferDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ServerTest {
    private val mapper = ObjectMapper()

    @Test
    fun `Create and get`() = withTestApplication({ module() }) {
        val id = createAccount(30L)
        val balanceRequest = handleRequest(HttpMethod.Get, "/account/${id}/balance")

        assertEquals(HttpStatusCode.OK, balanceRequest.response.status())
        assertEquals(30L, balanceRequest.response.content!!.toLong())
    }

    @Test
    fun `Delete account`() = withTestApplication({ module() }) {
        val emptyAccount = createAccount()
        val nonEmptyAccount = createAccount(100)

        val deleteEmptyRequest = handleRequest(HttpMethod.Delete, "/account/${emptyAccount}")
        val deleteNonEmptyRequest = handleRequest(HttpMethod.Delete, "/account/${nonEmptyAccount}")

        assertEquals(HttpStatusCode.OK, deleteEmptyRequest.response.status())
        assertEquals(HttpStatusCode.BadRequest, deleteNonEmptyRequest.response.status())
    }

    @Test
    fun `Illegal transfer`() = withTestApplication({ module() }) {
        val id = createAccount(30L)

        val transferRequest = handleRequest(HttpMethod.Post, "/account/transfer") {
            addHeader(HttpHeaders.ContentType, "${ContentType.Application.Json}")
            setBody(mapper.writeValueAsString(MoneyTransferDto(id, id, 30L)))
        }

        assertEquals(HttpStatusCode.BadRequest, transferRequest.response.status())
    }

    @Test
    fun `Transfer simple`() = withTestApplication({ module() }) {
        val id1 = createAccount(30L)
        val id2 = createAccount(30L)

        val transferRequest = handleRequest(HttpMethod.Post, "/account/transfer") {
            addHeader(HttpHeaders.ContentType, "${ContentType.Application.Json}")
            setBody(mapper.writeValueAsString(MoneyTransferDto(id1, id2, 30L)))
        }

        assertEquals(HttpStatusCode.OK, transferRequest.response.status())

        val balance1 = handleRequest(HttpMethod.Get, "/account/${id1}/balance")
        val balance2 = handleRequest(HttpMethod.Get, "/account/${id2}/balance")

        assertEquals(0L, balance1.response.content!!.toLong())
        assertEquals(60L, balance2.response.content!!.toLong())
    }

    @Test
    fun `Transfer not enough money`() = withTestApplication({ module() }) {
        val id1 = createAccount(30L)
        val id2 = createAccount(30L)

        val transferRequest = handleRequest(HttpMethod.Post, "/account/transfer") {
            addHeader(HttpHeaders.ContentType, "${ContentType.Application.Json}")
            setBody(mapper.writeValueAsString(MoneyTransferDto(id1, id2, 100)))
        }

        assertEquals(HttpStatusCode.BadRequest, transferRequest.response.status())

        val balance1 = handleRequest(HttpMethod.Get, "/account/${id1}/balance")
        val balance2 = handleRequest(HttpMethod.Get, "/account/${id2}/balance")

        assertEquals(30L, balance1.response.content!!.toLong())
        assertEquals(30L, balance2.response.content!!.toLong())
    }

    @Test
    fun `Transfer concurrent`() = withTestApplication({ module() }) {
        val times = 500L // simulate some level of concurrent usage, note that h2 is not ideal solution
        val id1 = createAccount(times)
        val id2 = createAccount(times)

        val body1 = mapper.writeValueAsString(MoneyTransferDto(id1, id2, 1L))
        val body2 = mapper.writeValueAsString(MoneyTransferDto(id2, id1, 1L))

        runBlocking {
            (1..times).map {
                launch {
                    handleRequest(HttpMethod.Post, "/account/transfer") {
                        addHeader(HttpHeaders.ContentType, "${ContentType.Application.Json}")
                        setBody(body1)
                    }

                    handleRequest(HttpMethod.Post, "/account/transfer") {
                        addHeader(HttpHeaders.ContentType, "${ContentType.Application.Json}")
                        setBody(body2)
                    }
                }
            }.joinAll()
        }

        val balance1 = handleRequest(HttpMethod.Get, "/account/${id1}/balance")
        val balance2 = handleRequest(HttpMethod.Get, "/account/${id2}/balance")

        assertEquals(times, balance1.response.content!!.toLong())
        assertEquals(times, balance2.response.content!!.toLong())
    }

    private fun TestApplicationEngine.createAccount(balance: Long = 0): Int {
        val createRequest = handleRequest(HttpMethod.Post, "/account/") {
            addHeader(HttpHeaders.ContentType, "${ContentType.Application.Json}")
            setBody(mapper.writeValueAsString(CreateAccountDto(balance)))
        }

        assertEquals(HttpStatusCode.OK, createRequest.response.status())

        return createRequest.response.content!!.toInt()
    }
}