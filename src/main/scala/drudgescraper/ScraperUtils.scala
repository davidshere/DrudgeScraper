package drudgescraper

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Await, Promise}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}

import java.time._
import java.time.temporal.ChronoUnit.DAYS

import akka.http.scaladsl.model._

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

object ScraperUtils {

  import LinksFromDrudgePage._

  trait Link {
    def url: String
    def forFlow = (HttpRequest(HttpMethods.GET, this.url), Promise[HttpResponse])
  }

  final case class DayPageLink(url: String, pageDt: LocalDate) extends Link
  final case class DrudgePageLink(url: String, pageTimestamp: Long) extends Link
  final case class DrudgeLink(url: String, pageTimestamp: Long, hed: String, isSplash: Boolean, isTop: Boolean) extends Link

  private def dayPagePathFromDate(date: LocalDate): String = {
    "/data/%d/%d/%d/index.htm".format(
        date.getYear(),
        date.getMonthValue(),
        date.getDayOfMonth()
    )
  }

  def generateDayPageLinks: List[DayPageLink] = {
    val start = LocalDate.of(2001, 11, 18)
    val end = LocalDate.now
    val daysFromStart = 0L to DAYS.between(start, end)
    val dates = daysFromStart.map(x => start.plusDays(x)).toList

    dates map (x => DayPageLink(dayPagePathFromDate(x), x))
  }

  val drudgePageDatetimeFormat = "yyyyMMdd_HHmmss"
  val drudgePageUrlDatetimeFormat = format.DateTimeFormatter.ofPattern(drudgePageDatetimeFormat)
  val utc = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(0))

  def transformDrudgePageUrlIntoEpoch(url: String): Long = {
    val urlDatetime = LocalDateTime.parse(url.slice(52, 67), drudgePageUrlDatetimeFormat)
    urlDatetime.atZone(utc).toEpochSecond()
  }

  def drudgePageLinkFromElement(elem: Element): DrudgePageLink = {
    val url = elem.attr("href")
    val timestamp = transformDrudgePageUrlIntoEpoch(url)
    DrudgePageLink(url, timestamp)
  }

  def dayPageToDrudgePageLinks(page: String): List[DrudgePageLink] = {
    val doc = Jsoup.parse(page)

    for {
      link <- doc.select("a[href]").asScala.toList;
      if (link.text() != "^" && link.attr("href").startsWith("http://www.drudgereportArchives.com/data/"))
    } yield drudgePageLinkFromElement(link)
  }

  def transformDayPage(pageFuture: Future[String]): Future[List[DrudgePageLink]] = pageFuture.map(dayPageToDrudgePageLinks)

  def drudgeLinkFromJsoupElement(elem: Element, pageDate: Long): DrudgeLink =
    DrudgeLink(
        elem.attr("href"),
        pageDate,
        elem.text,
        elem.id=="splash",
        elem.id=="top"
    )

}
