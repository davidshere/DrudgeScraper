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
 
val sourceOne = Source(List(1))
val sourceTwo = Source(List(2))

type Message
type Trigger

val elements = Source(1 to 10)
val triggerSource = Source(11 to 20)

val sink = Sink.foreach(println)

val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder ⇒
  import GraphDSL.Implicits._
  
  val zip = builder.add(ZipWith[Int, Int, Int](  (a, b) => b * 10))
  
  elements ~> zip.in0
  triggerSource ~> zip.in1
  zip.out ~> sink
  ClosedShape
})

val r = graph.run()



  /*
  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val in = Source(1 to 10)
    val out = Sink.ignore
  
    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))
  
    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
  
    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge
    ClosedShape
})

  g.run()
  * 
  */
  
  type MyTuple = (Int, Int)
  
 /*
  val diamond: Graph[FlowShape[MyTuple, MyTuple], NotUsed] = GraphDSL.create() { implicit builder =>
  
    val in = Source((1 to 10) zip (1 to 10))
    val out = Sink.foreach(println)
    
    val op = Flow[Int].map(v => v * v)
    
    val split = builder.add(Broadcast[MyTuple](2))
    val zip = builder.add(ZipWith((i: Int, c: Char) => (i, c)))
    
    import GraphDSL.Implicits._
    split.out(_) ~> zip.in0
    split.out ~> op  ~> zip.in1
    
    FlowShape(split.in, zip.out)
  }
  
  val diamondGraph: Flow[MyTuple, MyTuple, NotUsed] = Flow.fromGraph(diamond)
  
  val f = diamondGraph.runWith(Sink.runForeach(println))
  
  f.onComplete({
    case _ => system.terminate()
  })
  * 
  */


  /*
  RunnableGraph.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
  
     val unzip = b.add(UnzipWith[Int, Int, Int]((b: Int) => (b, b)))
  
     source ~> unzip.in
  
     unzip.out0 ~> Sink.ignore
     unzip.out1 ~> Sink.ignore
     
     ClosedShape
  }).run()
      
  


  * 
  */
}