package jorokr21.scandroid

import io.circe._
import io.circe.generic.semiauto._

object data {
  import config._

  type URL = URL.Self
  object URL extends Newtype.of[String] {
    implicit def encoder: Encoder[Self] = leibniz.substitute(Encoder[Repr])
    implicit def decoder: Decoder[Self] = leibniz.substitute(Decoder[Repr])
  }

  case class WebPage(url: URL, text: String)
  object WebPage {
    implicit val encoder: Encoder[WebPage] = deriveEncoder
    implicit val decoder: Decoder[WebPage] = deriveDecoder
  }

  case class Hit(url: URL, highlight: String)
  object Hit {
    implicit val encoder: Encoder[Hit] = deriveEncoder
    implicit val decoder: Decoder[Hit] = deriveDecoder
  }

  sealed trait Request
  object Request {
    case class Scrape(url: URL, depth: Int = scrape.depth) extends Request
    object Scrape {
      implicit val encoder: Encoder[Scrape] = deriveEncoder
      implicit val decoder: Decoder[Scrape] = deriveDecoder
    }

    case class Search(query: String, topK: Int = search.topK) extends Request
    object Search {
      implicit val encoder: Encoder[Search] = deriveEncoder
      implicit val decoder: Decoder[Search] = deriveDecoder
    }

    implicit val encoder: Encoder[Request] = deriveEncoder
    implicit val decoder: Decoder[Request] = deriveDecoder
  }

  sealed trait Response
  object Response {
    case class Scraping(url: URL, depth: Int) extends Response
    object Scraping {
      implicit val encoder: Encoder[Scraping] = deriveEncoder
      implicit val decoder: Decoder[Scraping] = deriveDecoder
    }

    case class Results(query: String, hits: Hit*) extends Response
    object Results {
      implicit val encoder: Encoder[Results] = deriveEncoder
      implicit val decoder: Decoder[Results] = deriveDecoder
    }

    implicit val encoder: Encoder[Response] = deriveEncoder
    implicit val decoder: Decoder[Response] = deriveDecoder
  }
}
