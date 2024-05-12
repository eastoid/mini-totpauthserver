package totpauthserver.service

import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton
import java.net.InetAddress
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Singleton
class SecurityService(
    private val logger: LogService
){

    private val rateLimitMap = ConcurrentHashMap<String, Instant>()
    private val rateLimitedIps = ConcurrentLinkedQueue<String>()
    private val rateLimitedIpsPresence = ConcurrentHashMap.newKeySet<String>()

    private var lastClear = AtomicReference(Instant.now())

    // IP address
    // interval in seconds - minimum time gap between requests
    fun rateLimitIp(ip: String, path: String, interval: Long,): Boolean {
        val now = Instant.now()
        val lastRequest = rateLimitMap.put(ip, now)

        // cleanup old stuff
        val lastClearTime = lastClear.get()
        if (lastClearTime.plusSeconds(60).isBefore(now) && lastClear.compareAndSet(lastClearTime, now)) {
            val expirationTime = now.minusSeconds(60)
            rateLimitMap.entries.removeIf { it.value.isBefore(expirationTime) }

            while (rateLimitedIpsPresence.size > 250) {
                val removedIp = rateLimitedIps.poll() ?: break
                rateLimitedIpsPresence.remove(removedIp)
            }
        }

        if (lastRequest == null ||  lastRequest.plusSeconds(interval).isBefore(now)) {
            return false
        }

        // Rate limited ->
        if (rateLimitedIpsPresence.add(ip)) {
            logger.log("$now - Rate limit [$ip] - $path")
            rateLimitedIps.add(ip)
        }

        return true
    }

    fun getRealIp(request: HttpRequest<*>): String? {
        val ip = request.remoteAddress.address.hostAddress
        val forwardedFor = request.headers["X-Forwarded-For"]
        try {
            InetAddress.getByName(forwardedFor)
        } catch (e: Exception) {
            logger.log("${Instant.now()}  X-Forwarded-For header is invalid! Value: [$forwardedFor] from [$ip]")
        }

        return if (forwardedFor.isNullOrBlank()) null else forwardedFor
    }

}