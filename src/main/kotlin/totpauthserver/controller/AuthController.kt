package totpauthserver.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.cookie.Cookie
import io.micronaut.views.ModelAndView
import totpauthserver.service.AuthService
import totpauthserver.service.TotpService
import totpauthserver.model.LoginFormDataModel
import org.thymeleaf.TemplateEngine
import java.time.Instant


@Controller("/auth")
class AuthController(
    private val authService: AuthService,
    private val totpService: TotpService
) {

    // authenticate cookie token for a specific ID
    @Get("/verify/{id}/{token}", produces = [MediaType.TEXT_PLAIN])
    fun manualVerifyAuthMapping(
        @PathVariable("id") id: String,
        @PathVariable("token") token: String,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        if (id.isBlank()) {
            println("500 - ID path variable blank for token authentication")
            return HttpResponse.status(500, "Bad service ID [$id]")
        }
        if (token.isBlank()) return HttpResponse.status<String?>(HttpStatus.UNAUTHORIZED).body("unauthorized")

        val auth = authService.authToken(token, id)
        if (auth) return HttpResponse.status<String?>(HttpStatus.OK).body("ok")
        return HttpResponse.status<String?>(HttpStatus.UNAUTHORIZED).body("unauthorized")
    }


    // authenticate cookie token for a specific ID
    @Get("/verify/{id}", produces = [MediaType.TEXT_PLAIN])
    fun verifyAuth(
        @PathVariable("id") id: String,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        if (id.isBlank()) {
            println("500 - ID path variable blank for token authentication")
            return HttpResponse.status(500, "Bad service ID [$id]")
        }
        val cookie = request.cookies?.get("authtoken-$id")?.value
        if (cookie.isNullOrBlank()) return HttpResponse.status<String?>(HttpStatus.UNAUTHORIZED).body("unauthorized")

        val auth = authService.authToken(cookie, id)
        if (auth) return HttpResponse.status<String?>(HttpStatus.OK).body("ok")
        return HttpResponse.status<String?>(HttpStatus.UNAUTHORIZED).body("unauthorized")
    }


    // serve login page
    @Get("/loginpage", produces = [MediaType.TEXT_HTML])
    fun login(): ModelAndView<String> {
        return ModelAndView<String>().apply { setView("login") }
    }


    // totp login endpoint
    @Post("/login", produces = [MediaType.TEXT_HTML], consumes = [MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_FORM_URLENCODED])
    fun loginMapping(
        @Body body: LoginFormDataModel,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        println("${Instant.now()} [>] /auth/login")

        val auth = totpService.verify(body.totp, body.id)
        if (!auth.first || auth.second != "true") {
            println("${request.remoteAddress.address.hostAddress} [>] \"${body.id}\" Fail (input totp: ${body.totp})")
            return HttpResponse.unauthorized<String?>().body(auth.second)
        }

        val token = authService.saveToken(body.id)
        val cookie = Cookie.of("authtoken-${body.id}", token).apply {
            maxAge(authService.ttl)
            path("/")
            httpOnly(true)
            secure(true)
        }
        return HttpResponse.ok<String?>().body(auth.second).cookie(cookie)
    }


    @Get("/logout/{id}", produces = [MediaType.TEXT_PLAIN])
    fun logoutMapping(@PathVariable("id") id: String): HttpResponse<String> {
        val cookie = Cookie.of("authtoken-$id", "logout-${Instant.now()}").apply {
            maxAge(60)
            path("/")
            httpOnly(true)
            secure(true)
        }
        return HttpResponse.ok<String?>().body("Logout").cookie(cookie)
    }

}


