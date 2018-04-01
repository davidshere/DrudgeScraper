package drudgescraper

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import scala.Predef.identity

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

  val poolClientFlow = Http().cachedHostConnectionPool[Promise[HttpResponse]](
    host = "www.drudgereportarchives.com",
    settings = ConnectionPoolSettings(system).withMaxConnections(20))

  val requestToHtmlFlow = Flow[Try[HttpResponse]].map(htmlFromHttpResponse)

  val dayPageLinks = ScraperUtils.generateDayPageLinks take 10
  val dayPageLinkSource = Source(dayPageLinks)

  val linkToFutureString: Flow[Link, Future[String], NotUsed] = Flow[Link]
    .map(_.forFlow)
    .via(poolClientFlow)
    .map(_._1)
    .via(requestToHtmlFlow)

  val seqSink = Sink.seq[Future[Seq[DrudgeLink]]]

  val transformDayPageFlow = Flow[Future[String]]
    .flatMapConcat({
      dayPageHtmlFuture => Source.fromFuture(transformDayPage(dayPageHtmlFuture))
    })

  import scala.collection.immutable.Seq

  val drudgePageToLinksGraph = RunnableGraph.fromGraph(GraphDSL.create(seqSink) { implicit builder ⇒
    sink =>
      import GraphDSL.Implicits._

      // set up graph notes
      val drudgePageOut = builder.add(Broadcast[Seq[DrudgePageLink]](2))
      val tsAndHtmlToDrudgeLinks = builder.add(ZipWith[Future[String], Long, Future[Seq[DrudgeLink]]](transformDrudgePage))

      // connect graph nodes

      // from day page links to drudge page links
      dayPageLinkSource ~> linkToFutureString.async ~> transformDayPageFlow.async ~> drudgePageOut.in

      // broadcast drudge page links to extract the timestamp and process the request, generating drudge links
      drudgePageOut.out(0) ~> Flow[Seq[DrudgePageLink]].mapConcat[DrudgePageLink](identity) ~> linkToFutureString ~> tsAndHtmlToDrudgeLinks.in0
      drudgePageOut.out(1) ~> Flow[Seq[DrudgePageLink]].mapConcat[DrudgePageLink](identity).map(_.pageTimestamp) ~> tsAndHtmlToDrudgeLinks.in1

      tsAndHtmlToDrudgeLinks.out ~> sink

      ClosedShape
  })

  val result = drudgePageToLinksGraph.run()

  result.onComplete({
    case Success(a) => { System.out.println("a", (a.length, a(0), (System.nanoTime() - startTime) / 1e9)); system.terminate() }
    case Failure(e) => System.out.println("e", e)
  })
}
