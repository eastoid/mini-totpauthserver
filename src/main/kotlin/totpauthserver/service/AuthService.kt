package totpauthserver.service

import io.micronaut.core.util.clhm.ConcurrentLinkedHashMap
import jakarta.inject.Singleton
import totpauthserver.model.AuthTokenInfoModel
import java.time.Instant
import java.util.*
import kotlin.collections.LinkedHashMap

@Singleton
class AuthService {

    private val tokens = LinkedHashMap<String, AuthTokenInfoModel>()
    var removedTokens = 0
    var total = 0
    val startTime = Instant.now()

    fun authToken(token: String, id: String): Boolean {
        removeExpiredTokens()
        val info = tokens[token] ?: return false
        if (info.issuedAt.isExpired()) {
            tokens.remove(token)
            return false
        }

        if (info.id != id) return false
        return true
    }

    private fun removeExpiredTokens() {
        kotlin.runCatching {
            tokens.forEach {
                if (it.value.issuedAt.isExpired()) {
                    tokens.remove(it.key)
                    removedTokens++
                    total++
                }
            }
            if (removedTokens % 40 == 0) {
                val now = Instant.now()
                val since = now.epochSecond - startTime.epochSecond
                val hrs = since / 3600
                val days = since / 86400
                println("[i] Removed $removedTokens tokens ($total total) - since ${hrs}hrs / ${days}days ago")
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
        }
    }

    private fun Long.isExpired(): Boolean {
        val issued = Instant.ofEpochSecond(this)
        val now = Instant.now()

        return issued.plusSeconds(300).isBefore(now)
    }

    fun saveToken(id: String): String {
        val token = "${UUID.randomUUID()}turbohomo${UUID.randomUUID()}"
        val info = AuthTokenInfoModel(id, Instant.now().epochSecond)

        tokens.put(token, info)
        return token
    }

}