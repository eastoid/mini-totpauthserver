package totpauthserver.model

import kotlinx.serialization.Serializable

@Serializable
data class SecretModel(
    val id: String,
    val secret: String,
    val ttl: Long
)