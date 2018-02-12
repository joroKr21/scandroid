package jorokr21.scandroid

/** Global configuration. */
// TODO: Load from the environment.
object config {
  object memory {
    object threshold {
      val free = 0.1
      val used = 0.9
    }

    object eviction {
      val rate = 0.1
    }
  }

  object timeout {
    val ask = 10
  }

  object scrape {
    val depth = 5
    val charset = "UTF-8"
    val buffer = 100
    val parallelism = 4
  }

  object index {
    val parallelism = 4
  }

  object search {
    val topK = 10
    val parallelism = 4

    object highlight {
      val preTag = "<b>"
      val postTag = "</b>"
      val fragmentSize = 100
    }
  }

  object server {
    val host = "localhost"
    val port = 8080
  }
}
