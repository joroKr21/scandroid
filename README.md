### Scadnroid

A web crawler, scraper, parser, searcher and in-memory index based on
Akka, Akka-Http and Akka-Streams with Lucene.

I'm done now ;)

### Usage

Start `Server.scala` and send HTTP requests. E.g:

1. Request `POST localhost:8080/scrape`:
    ```json
    {
      "url": "https://doc.akka.io/docs/akka-http/current/index.html",
      "depth": 3
    }
    ```

2. Response (it's scraping in the background):
    ```json
    {
      "url": "https://doc.akka.io/docs/akka-http/current/index.html",
      "depth": 3
    }
    ```

3. Request `POST http://localhost:8080/search` (after some time):
    ```json
    {
      "query": "http superpool",
      "topK": 5
    }
    ```
    `query` can be anything that Lucene can parse out of the box.

4. Response (sample):
    ```json
    {
      "query": "http superpool",
      "hits": [
        {
          "url": "https://doc.akka.io/docs/akka-http/current/client-side/request-level.html",
          "highlight": " client-side API is presented by the <b>Http</b>().<b>superPool</b>(...)Http.get(system).<b>superPool</b>(...) method"
        },
        {
          "url": "https://doc.akka.io/docs/akka-http/current/client-side/client-transport.html",
          "highlight": " of the pool methods like <b>Http</b>().singleRequest, <b>Http</b>().<b>superPool</b>, or <b>Http</b>().cachedHostConnectionPool"
        },
        {
          "url": "https://doc.akka.io/docs/akka-http/current/client-side/client-https-support.html",
          "highlight": "Akka <b>HTTP</b> Version 10.1.0-RC1 ScalaJava ! Security Announcements ! 0. Release Notes 1. Introduction"
        },
        {
          "url": "https://doc.akka.io/docs/akka-http/current/configuration.html",
          "highlight": " this section to tweak client settings only for host connection pools APIs like `<b>Http</b>().<b>superPool</b>"
        },
        {
          "url": "https://doc.akka.io/docs/akka-http/current/common/http-model.html",
          "highlight": " APIs <b>Http</b>().singleRequestHttp.get(sys).singleRequest or <b>Http</b>().superPoolHttp.get(sys).<b>superPool</b>"
        }
      ]
    }
    ```

### Todos:
- [ ] A web page user interface
- [ ] Testing (still learning the Akka test kit)
- [ ] Split scrape queue per host (to be fair)
