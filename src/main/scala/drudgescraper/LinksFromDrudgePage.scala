package drudgescraper

import scala.collection.JavaConverters._

import org.jsoup.nodes.{Document, Element}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object LinksFromDrudgePage {

  //import PageMetadataParser.mainAndSplash
  import PageMetadataParser.enrich
  import ScraperUtils.DrudgeLink
  
  def drudgeLinkFromJsoupElement(elem: Element, pageDate: LocalDateTime): DrudgeLink =
    DrudgeLink(
        elem.attr("href"),
        pageDate,
        elem.text,
        elem.id=="splash",
        elem.id=="top"
    )
     
  def transformPage(page: Document, pageDt: LocalDateTime): List[DrudgeLink] = {
    val enrichedPage = enrich(page)
    val links = enrichedPage
      .select("td:not(.text9)") // eliminate archive links below the bottom of the drudge page
      .select("a")
      .not("[target]")
      .not(":has(img)")
      .asScala
      .toList

    links.map(drudgeLinkFromJsoupElement(_, pageDt))
  }
}