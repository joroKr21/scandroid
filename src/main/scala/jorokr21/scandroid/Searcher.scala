package jorokr21.scandroid

import data._

import akka.Done
import akka.actor._

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.highlight._

import scala.collection.breakOut
import scala.util.control.NonFatal

import java.util.concurrent.Executors

class Searcher(writer: IndexWriter, evictor: Evictor.Ref)
  extends Actor with ActorLogging {

  import config.search
  import config.search.highlight

  private val analyzer = new StandardAnalyzer
  private val parser = new QueryParser("text", analyzer)
  private val formatter = new SimpleHTMLFormatter(highlight.preTag, highlight.postTag)
  private var reader = DirectoryReader.open(writer, false, false)
  private val threadPool = if (search.parallelism <= 1) None
    else Some(Executors.newWorkStealingPool(search.parallelism))

  def receive: Receive = {
    case Done =>
    case Request.Search(queryStr, topK) => try {
      log.info(s"Searching for top $topK '$queryStr'")
      reader = Option(DirectoryReader.openIfChanged(reader)).fold(reader)(identity)
      val searcher = threadPool.fold(new IndexSearcher(reader))(new IndexSearcher(reader, _))
      val query = parser.parse(queryStr)
      val topDocs = searcher.search(query, topK)
      log.info(s"Found ${topDocs.totalHits} hits for '$queryStr'")
      val scorer = new QueryScorer(query)
      val highlighter = new Highlighter(formatter, scorer)
      val fragmenter = new SimpleSpanFragmenter(scorer, highlight.fragmentSize)
      highlighter.setTextFragmenter(fragmenter)
      val hits: Seq[Hit] = topDocs.scoreDocs.map { score =>
        val doc = searcher.doc(score.doc)
        val url = URL(doc.get("url"))
        val text = doc.get("text")
        val highlights = highlighter.getBestFragments(analyzer, "text", text, 1)
        Hit(url, highlights.headOption.getOrElse(""))
      } (breakOut)
      evictor ! Evictor.Accessed(hits.map(_.url): _*)
      sender ! Response.Results(queryStr, hits: _*)
    } catch { case NonFatal(ex) =>
      log.error(ex, ex.getMessage)
    }
  }

  override def postStop(): Unit = {
    super.postStop()
    analyzer.close()
    reader.close()
    threadPool.foreach(_.shutdown())
  }
}

object Searcher {
  type Ref = Ref.Self
  object Ref extends Newtype.of[ActorRef]
}
