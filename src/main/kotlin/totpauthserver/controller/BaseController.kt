package totpauthserver.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import totpauthserver.service.SecurityService
import totpauthserver.service.serviceAvailable
import java.time.Instant


abstract class BaseController() {

    @Inject
    private lateinit var securityService: SecurityService

    protected fun unavailable(): HttpResponse<String> {
        return HttpResponse.status<String?>(HttpStatus.valueOf(500)).body("Service is unavailable due to some internal error!")
    }

    protected fun bad(body: String): HttpResponse<String> {
        return HttpResponse.status<String?>(HttpStatus.BAD_REQUEST).body(body)
    }

    protected fun ok(body: String): HttpResponse<String> {
        return HttpResponse.status<String?>(HttpStatus.OK).body(body)
    }

    protected fun unauthorized(body: String): HttpResponse<String> {
        return HttpResponse.status<String?>(HttpStatus.UNAUTHORIZED).body(body)
    }

    protected fun internal(body: String): HttpResponse<String> {
        return HttpResponse.status<String?>(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }

    protected fun rateLimited(body: String = "Too many requests"): HttpResponse<String> {
        return HttpResponse.status<String?>(HttpStatus.TOO_MANY_REQUESTS).body(body)
    }

    protected fun shouldRateLimit(request: HttpRequest<*>, path: String, interval: Long): Boolean {
        securityService.getRealIp(request)?.let { ip ->
            return (securityService.rateLimitIp(ip, path, interval,))
        }
        return false
    }

    protected fun HttpRequest<*>.realIp(): String? {
        return securityService.getRealIp(this)
    }

    protected fun timeNow(): Instant = Instant.now()

    protected fun endpointLogMessage(request: HttpRequest<*>, path: String): String {
        return "${timeNow()}  ${request.realIp() ?: "no-ip-found"}  [>]  $path"
    }

    protected fun helpText(): String {
        return """
            ###
            
            Backend TOTP authenticator  |  ${Instant.now()}
            Available: ${serviceAvailable()}
            
            If the service is unavailable, some functions become disabled.
            Service can become unavailable if the secrets.json file is corrupted, badly formatted, or inaccessible.
            Console logs must be inspected, and once issues are fixed, the reload endpoint can be called.
            
            Cookies used are in format `authtoken-{id}` like `authtoken-myId`.
            Each totp ID will have its own cookie and token and can be logged out separately
            
            Save file is in [${System.getenv("SECRETFOLDER") ?: "default"}]
            default location is "/etc/totpauthserver/secrets.json"
            or windows "C:\ProgramData\secrets.json" - stored in %ALLUSERSPROFILE%
            
            Default location is changed by "SECRETFOLDER" environment variable (not validated)
            Default port is changed by "MICRONAUT_SERVER_PORT" variable
            Default auth token TTL is changed by "TOKENTTL" variable
            
            
            # See logs
            /logs/{amount}
            
            # Authenticate a client token (via cookie)
            # Separate multiple IDs with comma - /verify/myId,otherId
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
            # Separate multiple IDs with comma - /verify/myId,otherId
            /auth/logout/{id}
            
            # Reloads secrets from the file, with logout all users option (boolean)
            /totp/reload/{logout}
            
            
            https://github.com/eastoid/mini-totpauthserver
            
            ###
        """.trimIndent()
    }

}