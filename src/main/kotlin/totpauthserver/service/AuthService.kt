package totpauthserver.service

import io.micronaut.context.annotation.Context
import io.micronaut.core.util.clhm.ConcurrentLinkedHashMap
import jakarta.inject.Singleton
import totpauthserver.model.AuthTokenInfoModel
import totpauthserver.model.SecretModel
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

@Context
@Singleton
class AuthService(
    private val logger: LogService
) {

    private val tokens = LinkedHashMap<String, AuthTokenInfoModel>()
    private val expirations = HashMap<String, Long>()
    var removedTokens = 0
    var total = 0
    val startTime = Instant.now()

    val defaultTtl = System.getenv("TOKENTTL")?.toLongOrNull() ?: 300L

    init {
        logger.log("Auth Token Default TTL is $defaultTtl")
        logger.log("For help do HTTP GET /help")
    }


    fun logout(id: String, token: String) {
        tokens.remove(token)
    }

    fun reload(logout: Boolean, ) {
        if (logout) tokens.clear()
        expirations.clear()
    }

    fun authToken(token: String, id: String, ): Boolean {
        removeExpiredTokens()
        val info = tokens[token] ?: return false
        if (info.issuedAt.isExpired(info.id)) {
            tokens.remove(token)
            return false
        }

        if (info.id != id) return false
        return true
    }

    private fun removeExpiredTokens() {
        kotlin.runCatching {
            tokens.forEach {
                if (it.value.issuedAt.isExpired(it.value.id)) {
                    tokens.remove(it.key)
                    removedTokens++
                    total++
                }
            }
            if (removedTokens > 0 && removedTokens % 50 == 0) {
                val now = Instant.now()
                val since = now.epochSecond - startTime.epochSecond
                val hrs = since / 3600
                val days = since / 86400
                logger.log("[i] Removed $removedTokens tokens ($total total) - since ${hrs}hrs / ${days}days ago")
                removedTokens = 0
            }
        }
    }

    fun removeIdTokens(id: String) {
        kotlin.runCatching {
            tokens.forEach {
                if (it.value.id == id) {
                    tokens.remove(it.key)
                }
            }
            expirations.remove(id)
        }
    }

    private fun Long.isExpired(id: String): Boolean {
        val issued = Instant.ofEpochSecond(this)
        val now = Instant.now()

        val ttl = expirations[id] ?: defaultTtl

        return issued.plusSeconds(ttl.toLong()).isBefore(now)
    }

    fun saveToken(secret: SecretModel): String {
        val token = "${UUID.randomUUID()}turbohomo${UUID.randomUUID()}"

        val info = AuthTokenInfoModel(secret.id, Instant.now().epochSecond)
        tokens.put(token, info)

        expirations.put(secret.id, secret.ttl)
        return token
    }

}