package totpauthserver.service

import com.atlassian.onetime.core.TOTPGenerator
import com.atlassian.onetime.model.TOTPSecret
import jakarta.inject.Singleton
import totpauthserver.model.SecretModel
import java.security.SecureRandom

@Singleton
class TotpService(
    private val storageService: StorageService
) {

    fun verify(model: SecretModel, code: String): Pair<Boolean, String> {
        val secret = TOTPSecret.fromBase32EncodedString(model.secret)
        val totpGenerator = TOTPGenerator()
        val totp = totpGenerator.generateCurrent(secret)
        return true to (code == totp.value).toString()
    }

    fun generateSecret(): TOTPSecret {
        return SecureRandom().let {
            val byteArray = ByteArray(20)
            it.nextBytes(byteArray)
            TOTPSecret(byteArray)
        }
    }

    fun save(id: String, secret: String, ttl: Long): Pair<Boolean, String> {
        return storageService.saveSecret(id, secret, ttl)
    }

    fun delete(id: String): Pair<Boolean, String> {
        return storageService.deleteSecret(id)
    }

    fun getIdList(): List<String> {
        return storageService.getIdList()
    }
}