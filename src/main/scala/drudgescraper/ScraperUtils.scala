package drudgescraper

import java.time._
import java.time.temporal.ChronoUnit.DAYS

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

import scala.collection.JavaConverters._

object ScraperUtils {
    trait Link {
    def url: String
  }

  final case class DrudgePageLink(url: String, pageDt: LocalDateTime) extends Link
  final case class DayPageLink(url: String, date: LocalDate) extends Link
  final case class DrudgeLink(url: String, pageDt: LocalDateTime, hed: String, isSplash: Boolean, isTop: Boolean) extends Link

  private def urlFromDate(date: LocalDate): String = {
    "http://www.drudgereportarchives.com/data/%d/%d/%d/index.htm".format(
        date.getYear(),
        date.getMonthValue(),
        date.getDayOfMonth()
    )
  }

  def generateDayPageLinks: List[DayPageLink] = {
    val start = LocalDate.of(2001, 11, 18)
    val end = LocalDate.of(2001, 11, 22)//now
    for {
      daysFromStart <- 0L to DAYS.between(start, end) toList
      // clean this up please
    } yield DayPageLink(urlFromDate(start.plusDays(daysFromStart)), start.plusDays(daysFromStart))
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

  def parseDayPage(page: String): List[DrudgePageLink] = {
    val doc = Jsoup.parse(page)
    for {
      link <- doc.select("a[href]").asScala.toList;
      if (link.text() != "^" && link.attr("href").startsWith("http://www.drudgereportArchives.com/data/"))
    } yield drudgePageLinkFromElement(link)
  }
}