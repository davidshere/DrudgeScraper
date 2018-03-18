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
// DrudgeScraper {
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
  //val poolClientFlow = Http().superPool[Promise[HttpResponse]](settings = ConnectionPoolSettings(system).withMaxConnections(20))

  val requestToHtmlFlow = Flow[Try[HttpResponse]].map(htmlFromHttpResponse)

  val requests = ScraperUtils.generateDayPageLinks take 1

  val drudgePageLinks: Source[ScraperUtils.DrudgePageLink, NotUsed] =
    Source(requests)
      .map(_.forFlow)
      .via(poolClientFlow)
      .map(_._1)
      .via(requestToHtmlFlow)
      .mapConcat[DrudgePageLink](asyncParseDayPage)

  val drudgeLinks: Source[ScraperUtils.DrudgeLink, NotUsed] =
    drudgePageLinks
      .map(_.forFlow) // here's where we lose the timestamp
      .via(poolClientFlow)
      .map(_._1)
      .via(requestToHtmlFlow)
      .mapConcat[DrudgeLink](asyncTransformPage)

  // main thing running now
  drudgeLinks
    .runWith(Sink.seq)
    .onComplete({
      case Success(a) => {println("a", (a.length, a(0), (System.nanoTime() - startTime)/1e9)); system.terminate()}
      case Failure(e) => println("e", e)
    })
}
