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
  val triggerSource = Source(11 to 20)

  val seqSink = Sink.seq[Int]

  val graph = RunnableGraph.fromGraph(GraphDSL.create(seqSink) { implicit builder ⇒ sink =>
    import GraphDSL.Implicits._

    val zip = builder.add(ZipWith[Int, Int, Int]((a, b) => b / 2))

    elements ~> zip.in0
    triggerSource ~> zip.in1

    zip.out ~> sink.in

    ClosedShape
  })
  
  val r = graph.run()
  
  def doStuffWithReturnValue(v: Seq[Int]) = {
    println(s"v is $v")
    val z = v.map(_ * 100)
    println(s"v * 100 is $z")
  }
  
  r.onComplete {
    case Success(b) => {doStuffWithReturnValue(b); system.terminate()}
    case Failure(e) => println(e)
  }


}