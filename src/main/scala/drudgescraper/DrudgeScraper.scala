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
  
  import LinksFromDrudgePage._

  trait Link {
    def url: String
  }
  final case class DrudgePageLink(url: String, pageDt: LocalDateTime) extends Link
  final case class DayPageLink(url: String, date: LocalDate) extends Link
  final case class DrudgeLink(url: String, pageDt: LocalDateTime, hed: String, isSplash: Boolean, isTop: Boolean) extends Link

  def urlFromDate(date: LocalDate): String = {
    "http://www.drudgereportarchives.com/data/%d/%d/%d/index.htm".format(
        date.getYear(),
        date.getMonthValue(),
        date.getDayOfMonth()
    )
  }

  def generateDayPageLinks: Vector[DayPageLink] = {
    val start = LocalDate.of(2011, 11, 18)
    val end = LocalDate.of(2011, 11, 22)//now
    for {
      daysFromStart <- 0L to DAYS.between(start, end) toVector
    } yield DayPageLink(urlFromDate(start.plusDays(daysFromStart)), start.plusDays(daysFromStart))
  }

  def fetchPageWithJsoup(link: Link): Document = {
    Jsoup.connect(link.url).get()
  }

  def transformDrudgePageUrlIntoLocalDateTime(url: String): LocalDateTime = {
    val drudgePageDatetimeFormat = "yyyyMMdd_HHmmss"
    val drudgePageUrlDatetimeFormat = format.DateTimeFormatter.ofPattern(drudgePageDatetimeFormat)
    val drudgePageUrlDatetimePortion = url.split("/").last.split("\\.").head
    LocalDateTime.parse(drudgePageUrlDatetimePortion, drudgePageUrlDatetimeFormat)
  }

  def drudgePageLinkFromElement(elem: Element): DrudgePageLink = {
    val pageDt = transformDrudgePageUrlIntoLocalDateTime(elem.attr("href"))
    DrudgePageLink(elem.attr("href"), pageDt)
  }

  def parseDayPage(page: Document, url: String): List[DrudgePageLink] = {
    for {
      link <- page.select("a[href]").asScala.toList;
      if (link.text() != "^" && link.attr("href").startsWith("http://www.drudgereportArchives.com/data/"))
    } yield drudgePageLinkFromElement(link)
  }
  
  
  val dayPageLinks = generateDayPageLinks
  val oneDay = dayPageLinks(0)
  
  val dayPage = fetchPageWithJsoup(oneDay)
  val parsedDayPage = parseDayPage(dayPage, oneDay.url)
  val oneDrudgePageLink = parsedDayPage(0)
  
  val drudgePageDocument = fetchPageWithJsoup(oneDrudgePageLink)

  
  for(l <- transformPage(drudgePageDocument, oneDrudgePageLink.pageDt)) {
    println(l)
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


