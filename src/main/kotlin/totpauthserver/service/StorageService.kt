package totpauthserver.service

import io.micronaut.context.annotation.Context
import jakarta.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import totpauthserver.model.SecretModel
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.system.exitProcess

private var available = false
fun serviceAvailable(): Boolean { return available }

@Context
@Singleton
class StorageService(
    private val logger: LogService
) {

    //                                      ID           SECRET  TTL
    private val secretCache = LinkedHashMap<String, Pair<String, Long>>()

    private val osName: String = System.getProperty("os.name").lowercase(Locale.getDefault())
    private val isWindows = osName.contains("win")

    private var path: String

    private var secretsFile: String

    init {
        path = System.getenv("SECRETFOLDER").let {
            if (it != null && it.isNotBlank()) {
                return@let it
            }
            if (isWindows) {
                "${System.getenv("ALLUSERSPROFILE")}\\totp-auth-server"
            } else {
                "/etc/totp-auth-server"
            }
        }

        secretsFile = if (isWindows) "$path\\secrets.json" else "$path/secrets.json"

        logger.log("Storing secrets in $secretsFile")

        initFolder()
        createSecretsFile()
        loadSecrets()
    }


    fun reload() {
        secretCache.clear()
        loadSecrets()
    }


    fun loadSecrets() {
        val file = readFile(secretsFile)
        if (!file.first) {
            available = false
            for (i in 0..3) { logger.log("Secrets file is not loaded!") }
            logger.log("ERROR: [${file.second}]")
            available = false
            return
        }

        val parsed = try {
            Json.decodeFromString<List<SecretModel>>(file.second)
        } catch (e: Exception) {
            logger.log(e.stackTraceToString())
            logger.log("ERROR - Failed to parse secrets file! It may be corrupted or badly formatted.")
            available = false
            return
        }
        logger.log("Loaded ${parsed.size} secrets from file")

        parsed.forEach {
            secretCache[it.id] = it.secret to it.ttl
        }

        available = true
    }


    fun initFolder() {
        kotlin.runCatching {
            val file = File(path)
            if (!file.exists()) {
                val op = file.mkdirs()
                if (!op) {
                    logger.log("Could not create folder [$path] for program data! Exiting.")
                    exitProcess(1)
                }
            }
        }.getOrElse {

            logger.log(it.stackTraceToString())
            available = false
            logger.log("\nException while initializing storage folder [$path]")
        }
    }


    fun saveSecret(id: String, secret: String, ttl: Long): Pair<Boolean, String> {
        val file = let {
            val file = File(secretsFile)
            if (!file.exists()) {
//                createSecretsFile()
                available = false
                logger.log("ERROR - Secrets file does not exist! Did not save secret [$id].")
                return false to "Secrets file does not exist! Did not save secret."
            }
            readFile(secretsFile)
        }

        if (!file.first) return file

        val parsed = kotlin.runCatching {
            Json.decodeFromString<List<SecretModel>>(file.second)
        }.getOrElse {
            logger.log(it.stackTraceToString())
            logger.log("Failed to deserialize secrets file! It may be corrupted or badly formatted. Failed to save ID [$id]")
            available = false
            return false to "Failed to deserialize secrets file.\n It may be corrupted or badly formatted. Did not save secret."
        }.toMutableList()

        parsed.forEach {
            if (it.id == id) {
                return false to "secret ID already exists"
            }
        }

        parsed.add(SecretModel(id, secret, ttl))

        val serialized = kotlin.runCatching {
            Json.encodeToString(parsed)
        }.getOrElse {
            logger.log(it.stackTraceToString())
            logger.log("Failed to serialize secrets into json. This is an unexpected error. Did not save ID [$id]")
            available = false
            return false to "Failed to save secrets file due to an unexpected serialization error. Did not save secret."
        }

        val writeOp = writeToFileOverwriting(secretsFile, serialized)
        if (!writeOp) return false to "Error writing to secrets file."

        secretCache[id] = secret to ttl

        return true to "Saved secret [$id]."
    }


    fun createSecretsFile(): Boolean {
        val file = File(secretsFile)
        if (file.exists()) {
            return true
        }
        logger.log("Secrets file not found. Creating new file.")

        try {
            Files.writeString(file.toPath(), "[]", StandardCharsets.UTF_8, StandardOpenOption.CREATE)
            return true
        } catch (e: Exception) {
            logger.log(e.stackTraceToString())
            logger.log("Error creating new secrets.json file [${secretsFile}]")
            return false
        }
    }


    fun readFile(path: String): Pair<Boolean, String> {
        val file = File(path)
        if (!file.exists()) {
            logger.log("Failed to read file [$path] - doesnt exist")
            available = false
            return false to "Secrets file doesnt exist. Aborting operation."
        }
        return kotlin.runCatching {
            true to Files.readString(file.toPath(), StandardCharsets.UTF_8)
        }.getOrElse {
            logger.log(it.stackTraceToString())
            logger.log("Failed to read file [$path]")
            available = false
            false to "Error reading secrets file.\n${it.stackTraceToString()}"
        }
    }


    fun writeToFileOverwriting(path: String, content: String): Boolean {
        val file = File(path)
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            return true
        } catch (e: IOException) {
            logger.log(e.stackTraceToString())
            logger.log("Error writing into secrets file [$path]")
            available = false
            return false
        }
    }


    fun getSecretById(id: String): Triple<Int, String?, SecretModel?> {
        secretCache[id]?.let {
            return Triple(200, null, SecretModel(id, it.first, it.second))
        }
        if (!serviceAvailable()) return Triple(404, "ID [$id] does not exist. (The service is unavailable due to an internal error!)", null)
        return Triple(404, "ID [$id] does not exist.", null)
    }


    fun deleteSecret(id: String): Pair<Boolean, String> {
        secretCache.remove(id)
        if (!available) return false to "Service unavailable due to an internal error! Did not delete ID."

        val file = readFile(secretsFile)
        if (!file.first) return file

        val parsed = kotlin.runCatching {
            Json.decodeFromString<List<SecretModel>>(file.second)
        }.getOrElse {
            logger.log(it.stackTraceToString())
            logger.log("Error parsing secrets file for deletion of [$id]")
            available = false
            return false to "Error parsing secrets file. It may be corrupted or badly formatted. Did not delete secret."
        }.filter { it.id != id }

        val serialized = kotlin.runCatching {
            Json.encodeToString(parsed)
        }.getOrElse {
            logger.log(it.stackTraceToString())
            logger.log("Error serializing secrets file for deletion of [$id]")
            available = false
            return false to "Error saving secrets file. Did not delete secret."
        }

        val op = writeToFileOverwriting(secretsFile, serialized)
        if (op) available = true
        return op to "Deleted ID [$id]"
    }


    fun getIdList(): List<String> {
        return secretCache.map { it.key }
    }

}
