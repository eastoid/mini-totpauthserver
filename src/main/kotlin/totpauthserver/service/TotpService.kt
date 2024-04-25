package totpauthserver.service

import com.atlassian.onetime.core.TOTPGenerator
import com.atlassian.onetime.model.TOTPSecret
import jakarta.inject.Singleton
import java.security.SecureRandom

@Singleton
class TotpService(
    private val storageService: StorageService
) {

    fun verify(code: String, id: String): Pair<Boolean, String> {
        val secretString = storageService.getSecretById(id)
        if (!secretString.first) return secretString

        val secret = TOTPSecret.fromBase32EncodedString(secretString.second)
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

    fun save(id: String, secret: String): Pair<Boolean, String> {
        return storageService.saveSecret(id, secret)
    }

    fun delete(id: String): Pair<Boolean, String> {
        return storageService.deleteSecret(id)
    }

    fun getIdList(): List<String> {
        return storageService.getIdList()
    }
}