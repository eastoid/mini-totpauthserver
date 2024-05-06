package totpauthserver.service

import jakarta.inject.Singleton
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedQueue

@Singleton
class LogService {

    private val log = ConcurrentLinkedQueue<String>()

    fun log(message: String) {
        log.add(message)
        println(message)
        if (log.size > 200) {
            log.poll()
        }
    }

    fun getLogs(amount: Int): List<String> {
        return log.toList().takeLast(amount)
    }

}