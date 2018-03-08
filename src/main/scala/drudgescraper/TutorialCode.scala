package drudgescraper

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.util.ByteString

import java.nio.file.Paths

import akka.stream._
import akka.stream.scaladsl._
import akka.{ NotUsed, Done }



object TutorialCode {
  
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