package drudgescraper

import scala.collection.JavaConverters._

import org.jsoup.nodes.{Document, Element}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object LinksFromDrudgePage {

  //import PageMetadataParser.mainAndSplash
  import PageMetadataParser.enrich
  import DrudgeScraper.DrudgeLink
  
  def drudgeLinkFromJsoupElement(elem: Element, pageDate: LocalDateTime): DrudgeLink =
    DrudgeLink(
        elem.attr("href"),
        pageDate,
        elem.text,
        elem.id=="splash",
        elem.id=="top"
    )
     
  def transformPage(page: Document, pageDt: LocalDateTime): List[DrudgeLink] = {
    implicit val pageDate = pageDt
    val enrichedPage = enrich(page)
    val links = enrichedPage
      .select("a")
      .not("[target]")
      .not(":has(img)")
      .asScala
      .toList

    links.map(drudgeLinkFromJsoupElement(_, pageDt))
  }
}