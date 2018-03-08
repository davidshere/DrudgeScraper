package drudgescraper

import java.io.File
import java.time._
import java.time.temporal.ChronoUnit.DAYS
import java.nio.file.Paths

import scala.util.{ Failure, Success, Try }

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import akka.actor.{ Actor, ActorLogging, Props, ActorSystem }
import akka.{ NotUsed, Done }
import akka.util.ByteString

import akka.stream._
import akka.stream.scaladsl._
import akka.stream.QueueOfferResult.Enqueued

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, HttpMethods}
import akka.http.scaladsl.settings.ConnectionPoolSettings

import com.typesafe.config.ConfigFactory

//object Main extends App {
object Ex {
  final case class Author(handle: String)
  final case class Hashtag(name: String)
  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] = 
      body.split(" ").collect {case t if t.startsWith("#") => Hashtag(t) }.toSet
  }
  
  val akkaTag = Hashtag("#akka")
  
  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  
  val tweets: Source[Tweet, NotUsed] = Source(
  Tweet(Author("rolandkuhn"), System.currentTimeMillis, "#akka rocks!") ::
    Tweet(Author("patriknw"), System.currentTimeMillis, "#akka !") ::
    Tweet(Author("bantonsson"), System.currentTimeMillis, "#akka !") ::
    Tweet(Author("drewhk"), System.currentTimeMillis, "#akka !") ::
    Tweet(Author("ktosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
    Tweet(Author("mmartynas"), System.currentTimeMillis, "wow #akka !") ::
    Tweet(Author("akkateam"), System.currentTimeMillis, "#akka rocks!") ::
    Tweet(Author("bananaman"), System.currentTimeMillis, "#bananas rock!") ::
    Tweet(Author("appleman"), System.currentTimeMillis, "#apples rock!") ::
    Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
    Nil)
  
  val authors: Source[Author, NotUsed] = 
    tweets
      .filter(_.hashtags.contains(akkaTag))
      .map(_.author)
  authors.runForeach(println)

  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags.toList)
  
  hashtags.runForeach(println)
  
  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ => 1)

  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  val counterGraph: RunnableGraph[Future[Int]] =
    tweets
      .via(count)
      .toMat(sumSink)(Keep.right)
  
  val sum: Future[Int] = counterGraph.run()
  
  sum.foreach(c => println(s"Total tweets processed: $c"))
}

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

  val poolClientFlow = Http().superPool[Promise[HttpResponse]](settings = ConnectionPoolSettings(system).withMaxConnections(10))
  

  
  val pipe = Source(requests)
    .map(x => { println("-", x); x})
    .via(poolClientFlow)
    .runWith(Sink.head)(materializer)
    
  //pipe.onComplete { (t: Try[HttpResponse], p: Promise[HttpResponse]) =>
  //    println("completed!")
  //}

  pipe.onComplete { a =>
      println(a)
      system.terminate()
  }
    //.map(x => {println("|", x); x})
    //.toMat(Sink.foreach { p => println(p) })(Keep.both)
    //.run()
    
  //val poolClientFlow = Http().cachedHostConnectionPool(...)
  /*
  val requestQueue = Source.queue[(HttpRequest, Promise[HttpResponse])](2000, OverflowStrategy.dropNew)
    .via(poolClientFlow)
    .toMat(Sink.foreach({
      case ((Success(resp), p)) => p.success(resp); ()
      case ((Failure(e), p)) => p.failure(e); ()
    }))(Keep.left)
    .run
    
    private def doRequest(req: HttpRequest): Future[HttpResponse] = {
      val promise = Promise[HttpResponse]
      requestQueue.offer(req -> promise).flatMap {
        case Enqueued => promise.future
        case _ => Future.failed(new RuntimeException())
      }
    }
  
  val resp = doRequest(requests(0)._1)
  resp.onComplete({
    case Success(r) => println(r)
    case Failure(e) => println(e)
  })
  * 
  */


    
    
    /*
    Source(requests)
       .via(poolClientFlow)
      .runWith(Sink.head)
      .flatMap { e =>e{
         case Success(e) => e.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
         case Failure(e) => "whoops!"       
      }
    }
      .map(_.utf8String)
      .foreach {println}
      * 
      */
  /*
    val pipeline =
      Source.queue[(HttpRequest, String)](100, OverflowStrategy.backpressure)
        .via(Http().cachedHostConnectionPool("www.drudgereportarchives.com"))
        .toMat(Sink.foreach { p =>
          println("*")
          println(p._2)
          p._1.get.entity.dataBytes.runFold(ByteString.empty)(_ ++ _).map(_.utf8String).foreach(println)
        })(Keep.left)
        .run()

    Source(List("/data/2001/11/18/20011118_010140.htm","/data/2013/12/18/20131218_010738.htm", "/data/2009/03/04/20090304_130111.htm")).runForeach { reqUri => {
        pipeline.offer((HttpRequest(HttpMethods.GET, reqUri), reqUri))
      }
    }
    * 
    */
  
}

//object Main extends App {
object M {
  
  implicit val system = ActorSystem("drudge-scraper")
  //implicit val ec = system.dispatcher
  
  implicit val materializer = ActorMaterializer()
  
  println(Thread.currentThread().getName)

  import system.dispatcher
  
  val source: Source[Int, NotUsed] = Source(1 to 100)
  //val done: Future[Done] = source.runForeach(i => println(i))
  
  val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

  val done: Future[Done] = 
    factorials
    .zipWith(Source(0 to 10))((num, idx) => s"$idx! = $num")
    .throttle(1, 0.25.seconds, 1, ThrottleMode.shaping)
    .runForeach(println)
  
  def processingStage(name: String): Flow[String, String, NotUsed] = 
   Flow[String].map { s ⇒
     println(name + " started processing " + s + " on thread " + Thread.currentThread().getName)
     Thread.sleep(100) // Simulate long processing *don't sleep in your real code!*
     println(name + " finished processing " + s)
     s
   }
  
  val completion = Source(List("Hello", "Streams", "World!"))
   .via(processingStage("A")).async
   .via(processingStage("B")).async
   .via(processingStage("C")).async
   .runWith(Sink.foreach(s ⇒ println("Got output " + s)))
  
  val result: Future[IOResult] =
    factorials
      .map(num => ByteString(s"$num\n"))
      .runWith(FileIO.toPath(Paths.get("factorials.txt")))

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
  
  val result2: Future[IOResult] = 
    factorials
      .map(_.toString)
      .runWith(lineSink("factorial2.txt"))

  val aggFuture = for {
    f1 <- done
    f2 <- result
    f3 <- result2
  } yield (f1, f2, f3)
  
  aggFuture.onComplete(_ => system.terminate())
  //result.onComplete(_ => system.terminate())


}



