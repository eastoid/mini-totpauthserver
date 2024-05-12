package totpauthserver.config

val dockerized = (System.getenv("RUNNING_DOCKERIZED").toBooleanStrictOrNull() ?: false).also { println("Detected docker: $it") }
