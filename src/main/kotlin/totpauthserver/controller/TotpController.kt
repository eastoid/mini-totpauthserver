package totpauthserver.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import totpauthserver.service.AuthService
import totpauthserver.service.StorageService
import totpauthserver.service.TotpService
import java.time.Instant


@Controller("/totp")
class TotpCont(
    private val totpService: TotpService,
    private val authService: AuthService,
    private val storageService: StorageService
) {


    @Get(uris = ["/help", "/", "/info"], produces = [MediaType.TEXT_PLAIN])
    fun help(): String {
        return """
            Backend TOTP authenticator  |  ${Instant.now()}
            
            Cookies used are in format `authtoken-{id}` like `authtoken-myId`.
            Each totp ID will have its own cookie and token and can be logged out separately
            
            Save file is in [${System.getenv("SECRETFOLDER") ?: "default"}]
            default location is "/etc/totpauthserver/secrets.json
            or "C:\ProgramData\secrets.json" - stored in %ALLUSERSPROFILE%
            
            Default location is changed by "SECRETFOLDER" environment variable (not validated)
            Default port is changed by "MICRONAUT_SERVER_PORT" variable
            Default auth token TTL is changed by "TOKENTTL" variable
            
            # Authenticate a client token (via cookie)
            # 200 "ok" or 401 "unauthorized"
            /auth/verify/{id}
            
            # Verify a TOTP code
            # 200 "ok" or 401 "unauthorized"
            /totp/verify/{id}/{token}
            
            # Generate TOTP secret
            /totp/new
            
            # Save TOTP secret under an ID with, with ttl (seconds)
            /totp/save/{id}/{ttl}/{secret}
            
            # Delete a TOTP secret via ID
            /totp/delete/{id}
            
            # List available IDs
            /totp/list
            
            # Serves login page
            /auth/loginpage
            
            # Login POST endpoint - POST params `id` and `totp`
            /auth/login
            
            # Logs client out of specific ID
            /auth/logout/{id}
            
            # Reloads secrets from the file, with logout all users option (boolean)
            /totp/reload/{logout}
            
            
            https://github.com/eastoid/mini-totpauthserver
        """.trimIndent()
    }


    // reload all secrets from file (option to log out all users)
    @Get("/reload/{logout}", produces = [MediaType.TEXT_PLAIN])
    fun reloadSecretsMapping(
        @PathVariable("logout") rawLogout: String
    ): HttpResponse<String> {
        println("${Instant.now()} [>] /totp/reload/$rawLogout")

        val logout = rawLogout.toBooleanStrictOrNull() ?: return HttpResponse.status<String>(HttpStatus.BAD_REQUEST).body("Wrong parameter (/totp/logout/{boolean})")
        storageService.reload()
        authService.reload(logout)

        println("Reloaded secrets (logout all users: $logout)")
        return HttpResponse.ok<String?>().body("Reloaded secrets (logout all users: $logout)")
    }

    @Get("/reload", produces = [MediaType.TEXT_PLAIN])
    fun reloadSecretsErrorMapping(): HttpResponse<String> {
        println("${Instant.now()} (Missing path variable) [>] /totp/reload")
        return HttpResponse.status<String?>(HttpStatus.BAD_REQUEST).body("Missing path variable - \"/totp/reload\" instead of \"totp/reload/false\" \ntrue or false whether to log out all users")
    }


    // verify a totp code against an id
    @Get("/verify/{id}/{code}", produces = [MediaType.TEXT_PLAIN])
    fun verifyCode(
        @PathVariable("id") id: String,
        @PathVariable("code") code: String,
    ): HttpResponse<String> {
        println("${Instant.now()} [>] /totp/verify/$id/$code")

        val secret = storageService.getSecretById(id)
        if (secret.first != 200) return HttpResponse.status<String?>(HttpStatus.valueOf(secret.first)).body(secret.second)

        val r = totpService.verify(secret.third!!, code)
        if (r.first) {
            if (r.second == "false") {
                return HttpResponse.status<String>(HttpStatus.UNAUTHORIZED).body("unauthorized")
            }
            return HttpResponse.status<String>(HttpStatus.OK).body("ok")
        }

        return HttpResponse.status<String>(HttpStatus.INTERNAL_SERVER_ERROR).body("error: ${r.second}")
    }


    // generate totp secret
    @Get("/new", produces = [MediaType.TEXT_PLAIN])
    fun randomSecret(): String {
        println("${Instant.now()} [>] /totp/new")
        return totpService.generateSecret().base32Encoded
    }


    // save totp secret under ID
    @Get("/save/{id}/{ttl}/{secret}", produces = [MediaType.TEXT_PLAIN])
    fun saveSecret(
        @PathVariable("id") id: String,
        @PathVariable("ttl") rawTtl: String,
        @PathVariable("secret") secret: String,
    ): HttpResponse<String> {
        println("${Instant.now()} [>] /totp/save/$id/$rawTtl/*****")

        val ttl = rawTtl.toLongOrNull() ?: return HttpResponse.status<String?>(HttpStatus.BAD_REQUEST).body("Bad TTL seconds")
        val r = totpService.save(id, secret, ttl)

        if (!r.first) return HttpResponse.status<String>(HttpStatus.INTERNAL_SERVER_ERROR).body(r.second)
        return HttpResponse.status<String?>(HttpStatus.OK).body(r.second)
    }


    // delete totp secret by ID
    @Get("/delete/{id}", produces = [MediaType.TEXT_PLAIN])
    fun deleteSecret(
        @PathVariable("id") id: String
    ): String {
        println("${Instant.now()} [>] /totp/delete/$id")
        authService.removeIdTokens(id)
        val r = totpService.delete(id)
        return "${r.first};${r.second}"
    }


    // list of available IDs
    @Get("/list", produces = [MediaType.TEXT_PLAIN])
    fun idList(): String {
        println("${Instant.now()} [>] /totp/list")
        val r = totpService.getIdList()

        val s = StringBuilder()
        fun StringBuilder.space(): StringBuilder { this.append("        ").appendLine(); return this }

        s.append("[==  ${r.size} IDs available  ==]").space()
        r.forEach {
            s.append(it).space()
        }
        s.space().space().append("[==  END  ==]")

        return s.toString()
    }

}