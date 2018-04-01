package drudgescraper

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq

import scala.concurrent.{ Future, Await, Promise }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{ Success, Failure, Try }

import java.time._
import java.time.temporal.ChronoUnit.DAYS

import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.util.ByteString

import org.jsoup.Jsoup
import org.jsoup.nodes.{ Document, Element }

object ScraperUtils {

  import LinksFromDrudgePage._

  trait Link {
    def url: String

    // akka html's cachedHostConnectionPool takes this tuple as its input
    def forFlow = (HttpRequest(HttpMethods.GET, this.url), Promise[HttpResponse])
  }

  final case class DayPageLink(url: String, pageDt: LocalDate) extends Link
  final case class DrudgePageLink(url: String, pageTimestamp: Long) extends Link
  final case class DrudgeLink(url: String, pageTimestamp: Long, hed: String, isSplash: Boolean, isTop: Boolean) extends Link

  private def dayPagePathFromDate(date: LocalDate): String = {
    "/data/%d/%d/%d/index.htm".format(
      date.getYear(),
      date.getMonthValue(),
      date.getDayOfMonth())
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

  def dayPageToDrudgePageLinks(page: String): Future[List[DrudgePageLink]] = Future({
    val doc = Jsoup.parse(page)

    for {
      link <- doc.select("a[href]").asScala.toList;
      if (link.text() != "^" && link.attr("href").startsWith("http://www.drudgereportArchives.com/data/"))
    } yield drudgePageLinkFromElement(link)
  })

  def transformDayPage(pageFuture: Future[String]): Future[Seq[DrudgePageLink]] =
    pageFuture.flatMap(x => dayPageToDrudgePageLinks(x))

  def drudgeLinkFromJsoupElement(elem: Element, pageDate: Long): DrudgeLink =
    DrudgeLink(
      elem.attr("href"),
      pageDate,
      elem.text,
      elem.id == "splash",
      elem.id == "top")


  def htmlFromHttpResponse(resp: Try[HttpResponse])(implicit materializer: ActorMaterializer): Future[String] =
    resp match {
      case Success(r) => {
        r match {
          case HttpResponse(StatusCodes.OK, headers, entity, _) =>
            entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
              body.utf8String
            }
          case resp @ HttpResponse(code, _, _, _) =>
            resp.discardEntityBytes()
            Future.successful("Not really!")
        }
      }
      case Failure(e) => {
        Future.failed(e)
      }
    }

}
