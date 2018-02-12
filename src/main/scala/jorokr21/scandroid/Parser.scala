package jorokr21.scandroid

import data._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream._

import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.util._

class Parser(system: ActorSystem)(implicit materializer: Materializer)
  extends (Parser.In => Parser.Out) {

  import system.dispatcher
  import config.scrape
  import Parser._

  private val log = Logging.getLogger(system, this)

  def apply(in: In): Out = in match {
    case (Success(response), Parse(uri, depth, by)) =>
      val url = URL(uri.toString)
      val status = response.status
      val entity = response.entity
      val contentType = entity.contentType
      val mediaType = contentType.mediaType

      if (!status.isSuccess) {
        log.warning(s"HTTP $status for $url")
        entity.discardBytes().future.map(_ => None -> Nil)
      } else if (!mediaType.isText && !mediaType.isApplication) {
        log.warning(s"Unsupported content type $contentType for $url")
        entity.discardBytes().future.map(_ => None -> Nil)
      } else for (text <- Unmarshal(entity).to[String]) yield {
        val charset = contentType.charsetOption.fold(scrape.charset)(_.toString)
        val document = Jsoup.parse(text, charset)
        document.setBaseUri(url)
        val request = Indexer.Index(WebPage(url, document.body.text), by)
        if (depth <= 0) Some(request) -> Nil
        else Some(request) -> document.select("a[href]").iterator.asScala.collect {
          Function.unlift(link => for {
            href <- Try(Uri(link.absUrl("href"))).toOption
            if isHttp(href.scheme) && href.authority.host == uri.authority.host
          } yield href.withoutFragment.withQuery(Uri.Query.Empty))
        }.map(Parse(_, depth - 1, by)).toList.distinct
      }

    case (Failure(ex), _) =>
      log.error(ex, ex.getMessage)
      Future.successful(None -> Nil)
  }

  private val isHttp: String => Boolean =
    Set(true, false).map(Uri.httpScheme)
}

object Parser {
  type In = (Try[HttpResponse], Parse)
  type Out = Future[(Option[Indexer.Index], List[Parse])]
  case class Parse(uri: Uri, depth: Int, by: Scraper.Ref)
}
