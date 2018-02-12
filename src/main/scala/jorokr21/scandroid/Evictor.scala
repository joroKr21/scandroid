package jorokr21.scandroid

import data._

import akka.Done
import akka.actor._

import org.apache.lucene.index._

import scala.collection.mutable
import scala.util.control.NonFatal

class Evictor(writer: IndexWriter) extends Actor with ActorLogging {
  import config.memory._
  import Evictor._

  // There is no immutable LinkedHashSet
  private val LRU = mutable.LinkedHashSet.empty[URL]
  private val runtime = Runtime.getRuntime

  def receive: Receive = {
    case Done =>

    // No pressure, just bubble.
    case Accessed(urls@_*) =>
      sender ! Done
      LRU --= urls
      LRU ++= urls

    // Pressure, check memory.
    case Written(url, by) =>
      LRU -= url
      LRU += url
      try if (isCritical && { runtime.gc(); isCritical }) {
        val evict = (LRU.size * eviction.rate).toInt
        log.warning(s"Memory critical - evicting $evict LRU web pages")
        val evicted = LRU.iterator.take(evict).toSeq
        writer.deleteDocuments(evicted.map(new Term("url", _)): _*)
        LRU --= evicted
        by ! Scraper.Evicted(evicted: _*)
      } catch { case NonFatal(ex) =>
        log.error(ex, ex.getMessage)
      } finally {
        sender ! Done
      }
  }

  override def postStop(): Unit = {
    super.postStop()
    LRU.clear()
  }

  private def isCritical: Boolean = {
    val maximal = runtime.maxMemory
    val total = runtime.totalMemory
    val free = runtime.freeMemory
    val used = total - free
    if (maximal == Long.MaxValue) free.toDouble / used < threshold.free
    else used.toDouble / maximal > threshold.used
  }
}

object Evictor {
  type Ref = Ref.Self
  object Ref extends Newtype.of[ActorRef]
  sealed trait Request
  case class Written(url: URL, by: Scraper.Ref) extends Request
  case class Accessed(urls: URL*) extends Request
}
