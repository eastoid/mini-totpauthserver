package totpauthserver

import io.micronaut.runtime.Micronaut.run


fun main(args: Array<String>) {
	for (i in 0..3) {
		println("For help do HTTP GET /totp/help")
	}
	run(*args)
}