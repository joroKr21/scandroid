package jorokr21.scandroid

import config._
import data._

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream._
import akka.stream.scaladsl._
import akka.pattern.ask
import akka.util.Timeout

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index._
import org.apache.lucene.store.RAMDirectory

import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.Try

// TODO: Error handling.
object Server extends App with ErrorAccumulatingCirceSupport {
  import IndexWriterConfig.OpenMode

  implicit val system = ActorSystem("scandex")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val askTimeout = Timeout(timeout.ask.second)

  val directory = new RAMDirectory
  val writer = new IndexWriter(directory,
    new IndexWriterConfig(new StandardAnalyzer).setOpenMode(OpenMode.CREATE_OR_APPEND))

  val parser = new Parser(system)
  val evictor = system.actorOf(Props(new Evictor(writer)))
  val indexer = Indexer(writer)

  val scrapeQueue = Source
    .queue[Scraper.Out](scrape.buffer, OverflowStrategy.backpressure)
    .via(Http().superPool()).mapAsyncUnordered(1)(parser)
    .to(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val indexing = builder.add(Balance[Indexer.Index](index.parallelism))
      val written = builder.add(Merge[Evictor.Written](index.parallelism))
      val parsed = builder.add(Unzip[Option[Indexer.Index], List[Parser.Parse]])

      for (_ <- 1 to index.parallelism) indexing ~> indexer.async ~> written
      parsed.out0.mapConcat(_.toList) ~> indexing

      written.out.mapAsyncUnordered(index.parallelism) {
        written => evictor ? written
      } ~> builder.add(Sink.ignore)

      parsed.out1.mapConcat(identity).mapAsyncUnordered(scrape.parallelism) {
        case Parser.Parse(uri, depth, by) => by ? Scraper.Scrape(uri, depth)
      } ~> builder.add(Sink.ignore)

      SinkShape(parsed.in)
    }).run()

  val scraper = system.actorOf(Props(new Scraper(scrapeQueue)))
  val searcher = system.actorOf(Props(new Searcher(writer, Evictor.Ref(evictor))))
  val loader = getClass.getClassLoader

  val route = pathSingleSlash {
    getFromFile(loader.getResource("index.html").getPath)
  } ~ path("css" / Segments) { path =>
    getFromFile(loader.getResource(("css" :: path).mkString("/")).getPath)
  } ~ path("search") {
    post {
      entity(as[Request.Search]) { search =>
        onSuccess((searcher ? search).mapTo[Response.Results]) {
          results => complete(results)
        }
      }
    }
  } ~ path("scrape") {
    post {
      entity(as[Request.Scrape]) { case Request.Scrape(url, depth) =>
        Try(Scraper.Scrape(Uri(url), depth)).fold(
          ex => failWith(ex),
          scrape => onSuccess(scraper ? scrape) { _ =>
            complete(Response.Scraping(url, depth))
          }
        )
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, server.host, server.port)
  println(s"Server online at http://${server.host}:${server.port}/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind())
    .transformWith(_ => system.terminate())
    .onComplete { _ =>
      writer.close()
      directory.close()
    }
}
