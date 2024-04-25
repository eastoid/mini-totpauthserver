package totpauthserver.service

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




@Singleton
class StorageService {

    private val secretCache = LinkedHashMap<String, String>()

    private val osName: String = System.getProperty("os.name").lowercase(Locale.getDefault())
    private val isWindows = osName.contains("win")

    private var path: String

    private var secretsFile: String

    private var available = false


    init {
        path = System.getenv("SECRETFOLDER").let {
            if (it != null && it.isNotBlank()) {
                println("Storage location changed to [$it]")
                return@let it
            }
            if (isWindows) {
                "${System.getenv("ALLUSERSPROFILE")}\\totp-auth-server"
            } else {
                "/etc/totp-auth-server"
            }
        }

        secretsFile = if (isWindows) "$path\\secrets.json" else "$path/secrets.json"

        initFolder()
    }

    fun initFolder(): Boolean {
        kotlin.runCatching {
            val file = File(path)
            if (!file.exists()) {
                val op = file.mkdirs()
                if (!op) {
                    println("Could not create folder [$path] for program data! Exiting.")
                    exitProcess(1)
                }
            }

            available = file.exists()


        }.getOrElse {
            it.printStackTrace()
            available = false
            println("\nException while initializing storage folder [$path]")
        }
        return available
    }


    fun saveSecret(id: String, secret: String): Pair<Boolean, String> {
        val file = let {
            val file = File(secretsFile)
            if (!file.exists()) createSecretsFile()
            readFile(secretsFile)
        }

        if (!file.first) return file

        val parsed = kotlin.runCatching {
            Json.decodeFromString<List<SecretModel>>(file.second)
        }.getOrElse {
            it.printStackTrace()
            println("\nFailed to deserialize")
            return false to "Failed to deserialize secrets file"
        }.toMutableList()

        parsed.forEach {
            if (it.id == id) {
                return false to "secret ID already exists"
            }
        }

        parsed.add(SecretModel(id, secret))

        val serialized = kotlin.runCatching {
            Json.encodeToString(parsed)
        }.getOrElse {
            it.printStackTrace()
            println("\nFailed to serialize new secrets list file json")
            return false to "Failed to save secrets file"
        }

        val writeOp = writeToFileOverwriting(secretsFile, serialized)
        if (!writeOp) return false to "Error writing to secrets file"

        return true to "Saved secret [$id]."
    }


    fun createSecretsFile(): Boolean {
        val file = File(secretsFile)
        if (file.exists()) {
            println("Tried to create secret store file. Already exists.")
            return true
        }

        try {
            Files.writeString(file.toPath(), "[]", StandardCharsets.UTF_8, StandardOpenOption.CREATE)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            println("\nException while writing empty json array into new file [${secretsFile}]")
            return false
        }
    }


    fun readFile(path: String): Pair<Boolean, String> {
        val file = File(path)
        if (!file.exists()) {
            println("Failed to read file [$path] - doesnt exist")
            return false to "Secrets file doesnt exist"
        }
        return kotlin.runCatching {
            true to Files.readString(file.toPath(), StandardCharsets.UTF_8)
        }.getOrElse {
            it.printStackTrace()
            println("\nFailed to read file [$path]")
            false to "Error reading secrets file.\n${it.stackTraceToString()}"
        }
    }


    fun writeToFileOverwriting(path: String, content: String): Boolean {
        val file = File(path)
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            println("Error overwriting file content [$path]")
            return false
        }
    }


    fun getSecretById(id: String): Pair<Boolean, String> {
        secretCache[id]?.let {
            return true to it
        }
        val file = readFile(secretsFile)
        if (!file.first) return file

        val parsed = kotlin.runCatching {
            Json.decodeFromString<List<SecretModel>>(file.second)
        }.getOrElse {
            it.printStackTrace()
            println("\nError parsing secrets file to id [$id]")
            return false to "Error parsing secrets file\n${it.stackTraceToString()}"
        }

        parsed.forEach {
            if (it.id == id) {
                secretCache[it.id] = it.secret
                while (secretCache.size > 100) {
                    val k = secretCache.firstEntry().key
                    secretCache.remove(k)
                }
                return true to it.secret
            }
        }

        return false to "ID [$id] does not exist."
    }


    fun deleteSecret(id: String): Pair<Boolean, String> {
        secretCache.remove(id)

        val file = readFile(secretsFile)
        if (!file.first) return file

        val parsed = kotlin.runCatching {
            Json.decodeFromString<List<SecretModel>>(file.second)
        }.getOrElse {
            it.printStackTrace()
            println("\nError parsing secrets file for deletion of [$id]")
            return false to "Error parsing secrets file\n${it.stackTraceToString()}"
        }.filter { it.id != id }

        val serialized = kotlin.runCatching {
            Json.encodeToString(parsed)
        }.getOrElse {
            it.printStackTrace()
            println("\nError serializing secrets file for deletion of [$id]")
            return false to "Failed to save secrets file"
        }

        val op = writeToFileOverwriting(secretsFile, serialized)
        return op to "deletion of [$id]"
    }


    fun getIdList(): List<String> {
        val file = readFile(secretsFile)
        if (!file.first) return listOf("Failed to read file: ${file.second}")
        val parsed = kotlin.runCatching {
            Json.decodeFromString<List<SecretModel>>(file.second)
        }.getOrElse {
            it.printStackTrace()
            println("\nError parsing secrets file to get id list")
            return listOf("Error parsing secrets file! Inspect console")
        }.map { it.id }

        return parsed
    }

}
