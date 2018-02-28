package drudgescraper

import scala.collection.JavaConverters._

import java.io.File

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode, DataNode}
import org.jsoup.select.{Elements, NodeVisitor, NodeFilter}
import org.jsoup.parser.Tag

import akka.actor.{ Actor, ActorLogging, Props, ActorSystem }


import java.time._
import java.time.temporal.ChronoUnit.DAYS;


object DrudgeScraper extends App {
  
  case class DrudgeLink(pageDt: String, url: String, hed: String, isSplash: Boolean, isTop: Boolean)

  final case class DrudgePageLink(url: String, date: LocalDate)
  final case class DayPageLink(url: String, date: LocalDate)
  
  def urlFromDate(date: LocalDate): String = {
    "http://www.drudgereportarchives.com/data/%d/%d/%d/index.htm".format(
        date.getYear(),
        date.getMonthValue(),
        date.getDayOfMonth()
    )
  }
  
  def generateDayPageLinks: Vector[DayPageLink] = {
    val start = LocalDate.of(2001, 11, 18)
    val end = LocalDate.of(2001, 11, 22)//now
    for {
      daysFromStart <- 0L to DAYS.between(start, end) toVector
    } yield DayPageLink(urlFromDate(start.plusDays(daysFromStart)), start.plusDays(daysFromStart))
  }
  
  def fetchPageWithJsoup(url: String): Document = {
    Jsoup.connect(url).get()
  }
  
  def parseDayPage(page: Document): List[String] = for {
    link <- page.select("a[href]").asScala.toList;
    if (link.text() != "^" && link.attr("href").startsWith("http://www.drudgereportArchives.com/data/"))
  } yield link.attr("href")
  
  
  val dayPageLinks = generateDayPageLinks
  for (dayPageLink <- dayPageLinks) {
    val dayPage = fetchPageWithJsoup(dayPageLink.url)
    val parsed = parseDayPage(dayPage)
    println(dayPageLink)
    println(parsed)
  }  
}




/*
 * Actors?:
 *  1) collect results and write to parquet/s3
 *  	-> number of parsed pages it should expect
 *    -> parsed pages (collections of DrudgePages)
 *    
 *  2) fetcher
 *  3) parser
 * 
 */ 


object Fetcher {
  def props: Props = Props(new Fetcher())
  
  final case class DrudgePageLink(url: String, date: LocalDate)
  final case class DayPageLink(url: String, date: LocalDate)
}

class Fetcher extends Actor with ActorLogging {
  import Fetcher._
  
  
  override def receive: Receive = {
    case DayPageLink(url, date) => {
 
    }
    case DrudgePageLink(url, date) => {
      // fetch drudge page from drudgereportarchives.com
      // send drudge page to parser
    }
  }
}



/*
class Parser extends Actor with ActorLogging {
  // for now just log that something was recieved
  override def receive: Receive = {
    case _ => println(_)
  }
}
* 
*/

/*
  
object DrudgeScraper extends App {
  
  def urlFromDate(date: LocalDate): String = {
    "http://www.drudgereportarchives.com/data/%d/%d/%d/index.htm".format(
        date.getYear(),
        date.getMonthValue(),
        date.getDayOfMonth()
    )
  }
  
  def generateDayPageUrls() = {
    val start = LocalDate.of(2001, 11, 18)
    val end = LocalDate.now
    for {
      daysFromStart <- 0L to DAYS.between(start, end) toVector
    } yield Fetcher.DayPageLink(urlFromDate(start.plusDays(daysFromStart)), start.plusDays(daysFromStart))
  }
  
	val system = ActorSystem("drudge-scraper")
	
	val fetcher = system.actorOf(Fetcher.props, "Fetcher")
	
	for(url <- generateDayPageUrls) {
	  fetcher ! url
	}

	system.terminate()
}
* 
*/


/*
 * 
  
  //val doc = Jsoup.connect("http://www.drudgereportarchives.com/data/2002/12/23/20021223_145531.htm").get()
  //val doc = Jsoup.connect("http://www.drudgereportarchives.com/data/2009/03/04/20090304_130111.htm").get()
  val doc = Jsoup.parse(new File("/Users/david/dropbox/projects/drudge_research/scraper/test/resources/test_file_4.html"), "UTF-8")
  
  val topAndSplashTags = Set(Tag.valueOf("a"), Tag.valueOf("font"))
  
  val topAndSplash = doc.select("b").asScala(0).children().asScala.filter(topAndSplashTags contains _.tag())
  val splashLinks = topAndSplash.filter(_.tag()==Tag.valueOf("a"))
  val topLinks = for {
    l <- topAndSplash;
    if(l.tag() != Tag.valueOf("a"))
  } yield l.select("a")
  
  class Visitor extends NodeVisitor {
    def head(node: Node, depth: Int) = node match {
      case elem: Element => elem.tag().getName() match {
        case "a" => println(depth, elem);
        case _ => ;
      };
      case _ => ;
    }
    
    def tail(node: Node, depth: Int) = {}
  }
  
  class TopLevelLinksOnlyFilter extends NodeFilter {
     def head(node: Node, depth: Int) = node match {
       case elem: Element => elem.hasText() match {
         case true => {
           println("*", elem)
           NodeFilter.FilterResult.CONTINUE
         }
         case false => {
           println("-", elem)
           NodeFilter.FilterResult.CONTINUE
         }
       }
       case _ => {
         println(("^", node))
         NodeFilter.FilterResult.CONTINUE
       }
     }
     def tail(node: Node, depth: Int) = NodeFilter.FilterResult.CONTINUE

  }
  val b = doc.select("font[size=+7]").select("a[href]").filter(new TopLevelLinksOnlyFilter)
  println(b)

  
  //doc.traverse(new Visitor)
  

  //val results = mainAndSplashFromRecentDayPage(doc)
  //println(results)
  //println("wut")
   * 
   * 
 */

