package drudgescraper

import scala.util.{ Failure, Success, Try }

import scala.collection.JavaConverters._

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.{ Actor, ActorSystem }
import akka.{ NotUsed, Done }

import akka.stream._
import akka.stream.scaladsl._

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, HttpMethods}
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
    .map(x => { println("-", x); x})
    .via(poolClientFlow)
    .map { case (t, p) => println("wtf!")
    }.runWith(Sink.foreach({println}))

  // shut things off once the pipeline is done doing everything it's going to do
  pipe.onComplete { a =>
      system.terminate()
  }
}
