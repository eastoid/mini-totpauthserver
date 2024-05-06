package totpauthserver.controller

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

    @Get(uris = ["/log", "/logs"], produces = [MediaType.TEXT_PLAIN])
    fun logs(): String {
        return getLogs(200)
    }

    @Get(uris = ["/log/{amount}", "/logs/{amount}"], produces = [MediaType.TEXT_PLAIN])
    fun customAmountLogs(
        @PathVariable("amount") rawAmount: String
    ): String {
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
