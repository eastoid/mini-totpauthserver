package totpauthserver.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import totpauthserver.service.LogService
import java.time.Instant

@Controller
class UtilController(
    private val logger: LogService
) : BaseController() {

    @Get(uris = ["/help", "/", "/info"], produces = [MediaType.TEXT_PLAIN])
    fun help(): String {
        return helpText()
    }

    @Get(uris = ["/logs"], produces = [MediaType.TEXT_PLAIN])
    fun logs(
        request: HttpRequest<*>,
    ): String {
        endpointLogMessage(request, request.path)
        return getLogs(200)
    }

    @Get(uris = ["/logs/{amount}"], produces = [MediaType.TEXT_PLAIN])
    fun customAmountLogs(
        @PathVariable("amount") rawAmount: String,
        request: HttpRequest<*>,
    ): String {
        endpointLogMessage(request, request.path)
        val amount = rawAmount.toIntOrNull() ?: return "Invalid log amount"
        return getLogs(amount)
    }

    private fun getLogs(amount: Int): String {
        val logs = logger.getLogs(amount)
        val s = StringBuilder()

        s.append(Instant.now().toString()).appendLine()
        s.append("[== START of LAST $amount logs ==]").appendLine().appendLine()
        logs.forEach {
            s.append(it).append("\n\n\n")
        }
        s.append("[== END OF LOGS ==]")

        return s.toString()
    }

}
