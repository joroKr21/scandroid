package jorokr21.scandroid

import data._

import akka.Done
import akka.actor._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.pattern.pipe
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.SourceQueue

import com.softwaremill.quicklens._

import scala.collection.immutable.Queue
import scala.collection.immutable.Seq

// TODO: Split each host in a separate Scraper.
class Scraper(source: SourceQueue[Scraper.Out]) extends Actor with ActorLogging {
  import context.dispatcher
  import Scraper._
  import MediaTypes._
  import QueueOfferResult._

  private val headers = Seq(Accept(`text/html`, `text/plain`, `text/markdown`))

  def receive: Receive = active(State())

  private def active(state: State): Receive = {
    case Done =>
    case req: Request => req match {
      // No pressure, forget evicted pages.
      case Evicted(urls@_*) =>
        sender ! Done
        context become active(state
          .modify(_.visited).using(_ -- urls))

      // No pressure, pass request downstream if buffer available.
      case scrape @ Scrape(uri, depth) =>
        sender ! Done
        val url = URL(uri.toString)
        if (state.visited.getOrElse(url, -1) < depth)
          context become active(push(state
            .modify(_.queue).using(_ enqueue scrape)
            .modify(_.visited).using(_ + (url -> depth))))
    }

    case qor: QueueOfferResult => qor match {
      case Enqueued => context become active(push(state
        .modify(_.pending).using(_.dequeue._2))
        .modify(_.freeBuffer).using(_ + 1))
      case Dropped =>
        val (output, tail) = state.pending.dequeue
        source.offer(output) pipeTo self
        context become active(state.modify(_.pending).setTo(tail))
      case Failure(ex) =>
        log.error(ex, ex.getMessage)
        context stop self
      case QueueClosed =>
        log.info(s"Stopping scraper $self")
        context stop self
    }
  }

  private def push(state: State): State =
    if (state.queue.isEmpty || state.freeBuffer <= 0) state else {
      val (Scrape(uri, depth), tail) = state.queue.dequeue
      log.info(s"Scraping at depth $depth: $uri")
      val request = HttpRequest(uri = uri, headers = headers)
      val output = request -> Parser.Parse(uri, depth, Ref(self))
      source.offer(output) pipeTo self
      state.modify(_.queue).setTo(tail)
        .modify(_.pending).using(_ enqueue output)
        .modify(_.freeBuffer).using(_ - 1)
    }
}

object Scraper {
  import config.scrape

  type Ref = Ref.Self
  object Ref extends Newtype.of[ActorRef]

  sealed trait Request
  case class Evicted(urls: URL*) extends Request
  case class Scrape(uri: Uri, depth: Int) extends Request

  type Out = (HttpRequest, Parser.Parse)

  private case class State(
    queue: Queue[Scrape] = Queue.empty,
    pending: Queue[Out] = Queue.empty,
    visited: Map[URL, Int] = Map.empty,
    freeBuffer: Int = scrape.buffer,
  )
}
