package totpauthserver.model

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.serde.annotation.Serdeable.Deserializable
import kotlinx.serialization.Serializable

@Serializable
@Serdeable.Serializable
@Deserializable
@Introspected
data class LoginFormDataModel(
    val id: String,
    val totp: String
)