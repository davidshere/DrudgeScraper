package drudgescraper

import scala.util.{ Failure, Success, Try }

import java.io.File

import scala.collection.JavaConverters._

import scala.concurrent._
import scala.concurrent.duration._


import akka.actor.{ Actor, ActorSystem }
import akka.{ NotUsed, Done }
import akka.util.ByteString

import akka.stream._
import akka.stream.scaladsl._

import akka.http.scaladsl.Http

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}


object DrudgeScraper extends App { 
// DrudgeScraper {
  import ScraperUtils._
  import LinksFromDrudgePage._
  
    implicit val system = ActorSystem("drudge-scraper")
    implicit val ec = system.dispatcher
    implicit val settings = system.settings
    implicit val materializer = ActorMaterializer()
    
    val now = System.nanoTime()
    
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
  
    val html = Flow[Try[HttpResponse]].map(htmlFromHttpResponse)
  
    val dayPageLinks = ScraperUtils.generateDayPageLinks
    
    val allRequests = for {
      dayPageLink <- dayPageLinks
    } yield dayPageLink.forFlow
  
    val requests = allRequests take 10
  
    //val poolClientFlow = Http().cachedHostConnectionPool(...)
    val poolClientFlow = Http().superPool[Promise[HttpResponse]](settings = ConnectionPoolSettings(system).withMaxConnections(30))
  
    val dayPageHttpResponses: Source[Try[HttpResponse], NotUsed] =
      Source(requests)
        .via(poolClientFlow)
        .map(_._1)
  
    val drudgePageLinks: Source[ScraperUtils.DrudgePageLink, NotUsed] =
      dayPageHttpResponses
        .via(html)
        .mapConcat[DrudgePageLink](asyncParseDayPage)
  
  
    val drudgePageHttpResponses: Source[Try[HttpResponse], NotUsed] =
      drudgePageLinks
        .map(_.forFlow)
        .via(poolClientFlow)
        .map(_._1)
  
    val drudgeLinks: Source[ScraperUtils.DrudgeLink, NotUsed] =
      drudgePageHttpResponses
        .via(html)
        .mapConcat[DrudgeLink](asyncTransformPage)
        
    // main thing running now
    drudgeLinks
      .runWith(Sink.seq)
      .onComplete({
        case Success(a) => {println("a", (a.length, a(0), (System.nanoTime() - now)/1e9)); system.terminate()}
        case Failure(e) => println("e", e)
      })
}
