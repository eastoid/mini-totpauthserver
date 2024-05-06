package totpauthserver.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import totpauthserver.service.serviceAvailable
import java.time.Instant


abstract class BaseController {

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

    protected fun helpText(): String {
        return """
            Backend TOTP authenticator  |  ${Instant.now()}
            Available: ${serviceAvailable()}
            
            If the service is unavailable, some functions become disabled.
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

}