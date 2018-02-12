package jorokr21.scandroid

import data._

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._

import org.apache.lucene.document._
import org.apache.lucene.index._

import scala.util.control.NonFatal

class Indexer(writer: IndexWriter)
  extends GraphStage[FlowShape[Indexer.Index, Evictor.Written]] {
  import Indexer._

  val in = Inlet[Index]("Indexer.in")
  val out = Outlet[Evictor.Written]("Indexer.out")
  val shape = FlowShape(in, out)

  def createLogic(attributes: Attributes) =
    new GraphStageLogic(shape) with StageLogging {
      setHandler(in, new InHandler {
        def onPush(): Unit = try {
          val Index(WebPage(url, text), by) = grab(in)
          val doc = new Document
          doc.add(new StringField("url", url, Field.Store.YES))
          // Must be stored to enable highlighting.
          doc.add(new TextField("text", text, Field.Store.YES))
          writer.updateDocument(new Term("url", url), doc)
          log.info(s"Indexed web page $url")
          push(out, Evictor.Written(url, by))
        } catch { case NonFatal(ex) =>
          log.error(ex, ex.getMessage)
        }
      })

      setHandler(out, new OutHandler {
        def onPull(): Unit = pull(in)
      })
    }
}

object Indexer {
  def apply(writer: IndexWriter): Flow[Index, Evictor.Written, NotUsed] =
    Flow.fromGraph(new Indexer(writer))

  case class Index(page: WebPage, by: Scraper.Ref)
}
