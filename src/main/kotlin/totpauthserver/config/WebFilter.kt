package totpauthserver.config

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.RequestFilter
import io.micronaut.http.annotation.ServerFilter
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import totpauthserver.service.LogService
import java.time.Instant


// external request checking not implemented


//val allowExternalRequests = (System.getenv("ALLOW_EXTERNAL_REQUESTS").toBooleanStrictOrNull() ?: false)
//    .also { if (it) println("[!] External requests are allowed") else println("External requests are disallowed") }


// // path, boolean Block external requests to all subpaths
//val localPaths = hashMapOf(
//    "/totp" to true,
//    "/logs" to true,
//    "/auth/verify" to true,
//)



//val localName = "localhost"
//val localIpv4 = "127.0.0.1"
//val localIpv6 = "0:0:0:0:0:0:0:1"
//val localIpv6Short = "::1"
//fun isLocalIp(ip: String) = ip == localIpv4 || ip == localIpv6 || ip == localIpv6Short || dockerized


// https://docs.micronaut.io/4.4.8/guide/#filters
//@ServerFilter("/**")
//class WebFilter(
//    private val logger: LogService
//) {
//
//    fun isLocal(request: HttpRequest<*>): Boolean {
//        val ip = request.remoteAddress.address.hostAddress
//        val forwardedFor = request.headers["X-Forwarded-For"]
//        val cfIp = request.headers["CF-Connecting-IP"]
//        val host = request.headers["Host"]?.substringBefore(":")
//
//        val isLocal = ip == localIpv4 || ip == localIpv6 || ip == localIpv6Short || dockerized
//        val forwardedLocal = forwardedFor.isNullOrBlank() || forwardedFor == localIpv4 || forwardedFor == localIpv6 || forwardedFor == localIpv6Short
//        val notCloudflare = cfIp.isNullOrBlank()
//        val localHostHeader = host.isNullOrBlank() || host == localName || host == localIpv4 || host == localIpv6 || host == localIpv6Short
//
//        return isLocal && forwardedLocal && notCloudflare && localHostHeader
//    }
//
//    @RequestFilter
//    @ExecuteOn(TaskExecutors.BLOCKING)
//    fun filterRequest(request: HttpRequest<*>) {
//        if (allowExternalRequests) return
//
//        if (!isLocal(request)) {
//            val ip = request.remoteAddress.address.hostAddress
//
//            logger.log("${Instant.now()}  Block external request from [$ip]")
//            throw HttpStatusException(HttpStatus.FORBIDDEN, "External request disallowed")
//        }
//    }
//
//    fun pathLocal(path: String): Boolean {
//        localPaths.forEach { entry ->
//            if (entry.value) {
//                // subpaths are authenticated
//                if (path.startsWith(entry.key)) return true
//            } else {
//                if (path == entry.key) return true
//            }
//        }
//        return false
//    }
//
//}