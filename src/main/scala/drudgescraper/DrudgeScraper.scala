package drudgescraper

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import akka.{ NotUsed, Done }
import akka.actor.{ Actor, ActorSystem }
import akka.util.ByteString

import akka.stream._
import akka.stream.scaladsl._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings

object DrudgeScraper extends App { 

  import ScraperUtils._
  import LinksFromDrudgePage._
  
  implicit val system = ActorSystem("drudge-scraper")
  implicit val ec = system.dispatcher
  implicit val settings = system.settings
  implicit val materializer = ActorMaterializer()
  
  val startTime = System.nanoTime()
  
  def htmlFromHttpResponse(resp: Try[HttpResponse]): Future[String] =
    resp match {
      case Success(r) => {
        r match {
          case HttpResponse(StatusCodes.OK, headers, entity, _) =>
            entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
              body.utf8String
            }
          case resp @ HttpResponse(code, _, _, _) =>
            resp.discardEntityBytes()
            Future.successful("Not really!")
        }
      }
      case Failure(e) => {
       Future.failed(e)
      }
    }

  val poolClientFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](host="www.drudgereportarchives.com", settings = ConnectionPoolSettings(system).withMaxConnections(20))

  val requestToHtmlFlow = Flow[Try[HttpResponse]].map(htmlFromHttpResponse)

  val requests = ScraperUtils.generateDayPageLinks take 1

  val linkToFutureString: Flow[Link, Future[String], NotUsed] =  Flow[Link]
      .map(_.forFlow)
      .via(poolClientFlow)
      .map(_._1)
      .via(requestToHtmlFlow)

  // linear source of drudge page links
  val drudgePageLinks: Source[ScraperUtils.DrudgePageLink, NotUsed] =
    Source(requests)
      .via(linkToFutureString)
      .mapConcat[DrudgePageLink](asyncParseDayPage)

  val seqSink = Sink.seq[Seq[DrudgeLink]]

  val drudgePageToLinksGraph = RunnableGraph.fromGraph(GraphDSL.create(seqSink) { implicit builder ⇒ sink =>
    import GraphDSL.Implicits._

    val drudgePageOut = builder.add(Broadcast[DrudgePageLink](2))
    val tsAndHtmlToDrudgeLinks = builder.add(ZipWith[Future[String], Long, Seq[DrudgeLink]](asyncTransformPage))

    drudgePageLinks ~> drudgePageOut.in

    drudgePageOut.out(0) ~> linkToFutureString ~> tsAndHtmlToDrudgeLinks.in0
    drudgePageOut.out(1) ~> Flow[DrudgePageLink].map(_.pageTimestamp) ~> tsAndHtmlToDrudgeLinks.in1

    tsAndHtmlToDrudgeLinks.out ~> sink

    ClosedShape
  })

  val result = drudgePageToLinksGraph.run()

  result.onComplete({
    case Success(a) => {println("a", (a.length, a(0), (System.nanoTime() - startTime)/1e9)); system.terminate()}
    case Failure(e) => println("e", e)
  })
}
