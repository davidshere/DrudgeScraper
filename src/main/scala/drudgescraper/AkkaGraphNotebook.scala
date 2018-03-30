package drudgescraper

import akka.{ NotUsed, Done }
import akka.actor.{ Actor, ActorSystem }
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Success, Failure}

import akka.stream._
import akka.stream.scaladsl._

import ScraperUtils._

object AkkaGraphNotebook extends App {

  implicit val system = ActorSystem("graph-test")
  implicit val ec = system.dispatcher
  implicit val settings = system.settings
  implicit val materializer = ActorMaterializer()

  val elements = Source(1 to 10)

  val seqSink = Sink.seq[Int]

  val graph = RunnableGraph.fromGraph(GraphDSL.create(seqSink) { implicit builder ⇒ sink =>
    import GraphDSL.Implicits._

    val bcast = builder.add(Broadcast[Int](2))
    val zip = builder.add(ZipWith[Int, Int, Int]((a, b) => a * b))
    
    elements ~> bcast.in

    bcast.out(0) ~> zip.in0
    bcast.out(1) ~> zip.in1

    zip.out ~> sink.in

    ClosedShape
  })
  
  val r = graph.run()
  
  r.onComplete {
    case Success(b) => {println(b); system.terminate()}
    case Failure(e) => println(e)
  }


}