package drudgescraper

import java.time._
import java.time.temporal.ChronoUnit.DAYS;


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
}