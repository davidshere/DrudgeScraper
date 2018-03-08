package drudgescraper

import scala.util.{ Failure, Success, Try }

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

import com.typesafe.config.ConfigFactory


object DrudgeScraper extends App {
//object DrudgeScraper {
  import ScraperUtils._
  
  implicit val system = ActorSystem("drudge-scraper")
  implicit val ec = system.dispatcher
  implicit val settings = system.settings
  
  implicit val materializer = ActorMaterializer()
  
  val dayPageLinks = ScraperUtils.generateDayPageLinks
  
  val allRequests = for {
    link <- dayPageLinks
  //} yield dayPageLinks(i).url
  } yield (HttpRequest(HttpMethods.GET, link.url), Promise[HttpResponse])

  val requests = allRequests take 2

  //val poolClientFlow = Http().cachedHostConnectionPool(...)
  val poolClientFlow = Http().superPool[Promise[HttpResponse]](settings = ConnectionPoolSettings(system).withMaxConnections(10))
  
  val pipe = Source(requests)
    .via(poolClientFlow)
    .map(x => { println("-", x); x})
    .runWith(Sink.head)
    .map(_._1)
    .flatMap(Future.fromTry)

  // shut things off once the pipeline is done doing everything it's going to do
  pipe.onComplete {
    case Success(r) => r match {
          case HttpResponse(StatusCodes.OK, headers, entity, _) =>
            entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              println("Got response, body: " + body.utf8String)
            }
          case resp @ HttpResponse(code, _, _, _) =>
            println("Request failed, response code: " + code)
            resp.discardEntityBytes()
  }
    case Failure(e) => {println(e)}
  }
}
