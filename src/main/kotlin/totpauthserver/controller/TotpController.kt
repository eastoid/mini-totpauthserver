package totpauthserver.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import totpauthserver.service.AuthService
import totpauthserver.service.TotpService
import java.time.Instant


@Controller("/totp")
class TotpCont(
    private val totpService: TotpService,
    private val authService: AuthService
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
            
            Default location is changed by "SECRETFOLDER" env var (not validated)
            
            
            # Authenticate a client token (via cookie)
            # 200 "ok" or 401 "unauthorized"
            /auth/verify/{id}
            
            # Verify a TOTP code
            # 200 "ok" or 401 "unauthorized"
            /totp/verify/{id}/{token}
            
            # Generate TOTP secret
            /totp/new
            
            # Save TOTP secret under an ID
            /totp/save/{id}/{secret}
            
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
            
            
            https://github.com/eastoid/mini-totpauthserver
        """.trimIndent()
    }


    // verify a totp code against an id
    @Get("/verify/{id}/{code}", produces = [MediaType.TEXT_PLAIN])
    fun verifyCode(
        @PathVariable("id") id: String,
        @PathVariable("code") code: String,
    ): HttpResponse<String> {
        println("${Instant.now()} [>] /totp/verify/$id/$code")
        val r = totpService.verify(code, id)
        if (r.first) {
            if (r.second == "false") {
                return HttpResponse.status<String>(HttpStatus.UNAUTHORIZED).body("unauthorized")
            }
            return HttpResponse.status<String>(HttpStatus.OK).body("ok")
        }

        return HttpResponse.status<String>(HttpStatus.INTERNAL_SERVER_ERROR).body("${r.first};${r.second}")
    }


    // generate totp secret
    @Get("/new", produces = [MediaType.TEXT_PLAIN])
    fun randomSecret(): String {
        println("${Instant.now()} [>] /totp/new")
        return totpService.generateSecret().base32Encoded
    }


    // save totp secret under ID
    @Get("/save/{id}/{secret}", produces = [MediaType.TEXT_PLAIN])
    fun saveSecret(
        @PathVariable("id") id: String,
        @PathVariable("secret") secret: String
    ): HttpResponse<String> {
        println("${Instant.now()} [>] /totp/save/$id/*****")
        val r = totpService.save(id, secret)
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