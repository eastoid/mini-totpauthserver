package totpauthserver.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import totpauthserver.service.*
import java.time.Instant


@Controller("/totp")
class TotpController(
    private val totpService: TotpService,
    private val authService: AuthService,
    private val storageService: StorageService,
    private val logger: LogService
) : BaseController() {


    @Get(uris = ["/help", "/", "/info"], produces = [MediaType.TEXT_PLAIN])
    fun help(): String {
        return helpText()
    }


    // reload all secrets from file (option to log out all users)
    @Get("/reload/{logout}", produces = [MediaType.TEXT_PLAIN])
    fun reloadSecretsMapping(
        @PathVariable("logout") rawLogout: String
    ): HttpResponse<String> {
        logger.log("${Instant.now()} [>] /totp/reload/$rawLogout")

        val logout = rawLogout.toBooleanStrictOrNull() ?: return bad("Wrong parameter (/totp/logout/{boolean})")
        storageService.reload()
        authService.reload(logout)

        logger.log("Reloaded secrets (logout all users: $logout)")
        return ok("Reloaded secrets (logout all users: $logout)")
    }


    @Get("/reload", produces = [MediaType.TEXT_PLAIN])
    fun reloadSecretsErrorMapping(): HttpResponse<String> {
        logger.log("${Instant.now()} (Missing path variable) [>] /totp/reload")
        return bad("Missing path variable - \"/totp/reload\" instead of \"totp/reload/false\" \ntrue or false whether to log out all users")
    }


    // verify a totp code against an id
    @Get("/verify/{id}/{code}", produces = [MediaType.TEXT_PLAIN])
    fun verifyCode(
        @PathVariable("id") id: String,
        @PathVariable("code") code: String,
    ): HttpResponse<String> {
        logger.log("${Instant.now()} [>] /totp/verify/$id/$code")

        if (id.isEmpty()) return bad("Invalid ID")
        if (!code.all { it.isDigit() }) return unauthorized("Invalid TOTP")

        val secret = storageService.getSecretById(id)
        if (secret.first != 200) return HttpResponse.status<String?>(HttpStatus.valueOf(secret.first)).body(secret.second)

        val r = totpService.verify(secret.third!!, code)
        if (r.first) {
            if (r.second == "false") {
                return unauthorized("unauthorized")
            }
            return ok("ok")
        }

        return internal("error: ${r.second}")
    }


    // generate totp secret
    @Get("/new", produces = [MediaType.TEXT_PLAIN])
    fun randomSecret(): String {
        logger.log("${Instant.now()} [>] /totp/new")
        return totpService.generateSecret().base32Encoded
    }


    // save totp secret under ID
    @Get("/save/{id}/{ttl}/{secret}", produces = [MediaType.TEXT_PLAIN])
    fun saveSecret(
        @PathVariable("id") id: String,
        @PathVariable("ttl") rawTtl: String,
        @PathVariable("secret") secret: String,
    ): HttpResponse<String> {
        logger.log("${Instant.now()} [>] /totp/save/$id/$rawTtl/*****")

        if (!serviceAvailable()) return unavailable()
        if (id.isEmpty()) return bad("Invalid ID")
        if (secret.isBlank()) return bad("Invalid secret")

        val ttl = rawTtl.toLongOrNull() ?: return bad("Bad TTL seconds")
        val r = totpService.save(id, secret, ttl)

        if (!r.first) return internal(r.second)
        return ok(r.second)
    }


    // delete totp secret by ID
    @Get("/delete/{id}", produces = [MediaType.TEXT_PLAIN])
    fun deleteSecret(
        @PathVariable("id") id: String
    ): HttpResponse<String> {
        logger.log("${Instant.now()} [>] /totp/delete/$id")
        if (id.isEmpty()) return bad("Invalid ID")

        authService.removeIdTokens(id)
        val r = totpService.delete(id)
        if (!r.first) return internal(r.second)
        if (!serviceAvailable()) return unavailable()
        return ok(r.second)
    }


    // list of available IDs
    @Get("/list", produces = [MediaType.TEXT_PLAIN])
    fun idList(): HttpResponse<String> {
        logger.log("${Instant.now()} [>] /totp/list")
        val r = totpService.getIdList()

        val s = StringBuilder()
        fun StringBuilder.space(): StringBuilder { this.append("        ").appendLine(); return this }

        if (!serviceAvailable()) {
            s.append("(i) The service is currently unavailable").space()
        }
        s.append("[==  ${r.size} IDs available  ==]").space()
        r.forEach {
            s.append(it).space()
        }
        s.space().space().append("[==  END  ==]")

        return ok(s.toString())
    }

}