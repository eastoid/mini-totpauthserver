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
import totpauthserver.model.LoginFormDataModel
import org.thymeleaf.TemplateEngine
import totpauthserver.model.SecretModel
import totpauthserver.service.*
import java.time.Instant


@Controller("/auth")
class AuthController(
    private val authService: AuthService,
    private val totpService: TotpService,
    private val storageService: StorageService,
    private val logger: LogService,
    private val securityService: SecurityService
) : BaseController() {

    // authenticate cookie token for a specific ID by path variable token
    @Get("/verify/{id}/{token}", produces = [MediaType.TEXT_PLAIN])
    fun manualVerifyAuthMapping(
        @PathVariable("id") id: String,
        @PathVariable("token") token: String,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        if (id.isEmpty()) {
            logger.log("[Warning]  Internal endpoint: /auth/verify/$id/$token  -  Invalid service ID")
            return HttpResponse.status(400, "Bad service ID [$id]")
        }
        if (token.isBlank()) return unauthorized("unauthorized")

        val auth = authService.authToken(token, id)
        if (auth) return ok("ok")
        return unauthorized("unauthorized")
    }


    // authenticate cookie token for a specific ID
    @Get("/verify/{id}", produces = [MediaType.TEXT_PLAIN])
    fun verifyAuth(
        @PathVariable("id") id: String,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        if (id.isEmpty()) return bad("Bad service ID [$id]")
        val ids = id.split(",")

        val cookies = request.cookies?.all?.filter { it.name.startsWith("authtoken-") }?.filter { it.name.substringAfter("authtoken-") in id }?.map { it.value }
        if (cookies.isNullOrEmpty()) return unauthorized("unauthorized")

        val auth = authService.authToken(cookies, ids)
        if (auth) return ok("ok")
        return unauthorized("unauthorized")
    }


    // serve login page
    @Get("/loginpage", produces = [MediaType.TEXT_HTML])
    fun login(
        request: HttpRequest<*>,
    ): ModelAndView<String> {
        logger.log(endpointLogMessage(request, request.path))
        return ModelAndView<String>().apply { setView("login-inline") }
    }


    // totp login endpoint
    @Post("/login", produces = [MediaType.TEXT_HTML], consumes = [MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_FORM_URLENCODED])
    fun loginMapping(
        @Body body: LoginFormDataModel,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        if (shouldRateLimit(request, "/login", 3)) return rateLimited()
        logger.log(endpointLogMessage(request, request.path))

        if (!serviceAvailable()) return unavailable()
        if (body.id.isEmpty()) return bad("Invalid ID")
        if (!body.totp.all { it.isDigit() }) return bad("Invalid TOTP")

        val secret: Triple<Int, String?, SecretModel?> = storageService.getSecretById(body.id)
        if (secret.first != 200) return HttpResponse.status<String?>(HttpStatus.valueOf(secret.first)).body(secret.second)

        val auth = totpService.verify(secret.third!!, body.totp) // if secret.first is 200, secret.third (SecretModel) is not null
        if (!auth.first || auth.second != "true") {
            logger.log("${request.remoteAddress.address.hostAddress} [>] \"${body.id}\" Fail (input totp: ${body.totp})")
            return unauthorized(auth.second)
        }

        val token = authService.saveToken(secret.third!!)
        val cookie = Cookie.of("authtoken-${body.id}", token).apply {
            maxAge(secret.third!!.ttl)
            path("/")
            httpOnly(true)
            secure(true)
        }
        return HttpResponse.ok<String?>().body(auth.second).cookie(cookie)
    }


    @Get("/logout/{id}", produces = [MediaType.TEXT_PLAIN])
    fun logoutMapping(
        @PathVariable("id") id: String,
        request: HttpRequest<*>
    ): HttpResponse<String> {
        logger.log(endpointLogMessage(request, request.path))
        if (id.isEmpty()) return bad("Invalid ID")
        val ids = id.split(",")

        val newCookies = mutableSetOf<Cookie>()
        request.cookies?.all?.filter { it.name.substringAfter("authtoken-") in ids }?.forEach { cookie ->
            authService.logout(cookie.value)
            newCookies.add(
                Cookie.of(cookie.name, "logout-${Instant.now()}").apply {
                    maxAge(60)
                    path("/")
                    httpOnly(true)
                    secure(true)
                }
            )
        }

        return HttpResponse.ok<String?>().body("Logout [$id]").cookies(newCookies)
    }

}


