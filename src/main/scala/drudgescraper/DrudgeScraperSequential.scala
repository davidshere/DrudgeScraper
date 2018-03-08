package drudgescraper

import java.time._
import java.time.temporal.ChronoUnit.DAYS

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.collection.JavaConverters._


object DrudgeScraperSequential { // extends App {
  
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
    val start = LocalDate.of(2001, 11, 18)
    val end = LocalDate.of(2001, 11, 22)//now
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
    println(url)
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
  println(parsedDayPage.size)
  
    def time[R](block: => R): R = {  
      val t0 = System.nanoTime()
      val result = block    // call-by-name
      val t1 = System.nanoTime()
      println("Elapsed time: " + ((t1 - t0) / 1e9) + "s")
      result
    }

  
  time {
    val dayPage = fetchPageWithJsoup(oneDay)
    val parsedDayPage = parseDayPage(dayPage, oneDay.url)
    val oneDrudgePageLink = parsedDayPage(0)
  
    for (drudgePageLink <- parsedDayPage) {
        val drudgePageDocument = fetchPageWithJsoup(drudgePageLink)
        val drudgeLinks = transformPage(drudgePageDocument, drudgePageLink.pageDt)
        println(drudgePageLink.pageDt, drudgeLinks.size)
      }
  }
   
}