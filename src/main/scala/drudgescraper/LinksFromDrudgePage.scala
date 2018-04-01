package drudgescraper

import org.jsoup.nodes.{ Document, Element }

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import scala.collection.JavaConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import ScraperUtils._

import org.jsoup.Jsoup

object LinksFromDrudgePage {

  import PageMetadataParser.enrich

  def drudgePageToLinkElements(page: Document): List[Element] = {
    val enrichedPage = enrich(page)
    val links = enrichedPage
      .select("td:not(.text9)") // eliminate archive links below the bottom of the drudge page
      .select("a")
      .not("[target]")
      .not(":has(img)")
      .asScala
      .toList

    links
  }

  def transformDrudgePage(pageFuture: Future[String], pageTs: Long): Future[List[DrudgeLink]] = {
    pageFuture.map({ html =>
      val soup = Jsoup.parse(html)
      val linkElements = drudgePageToLinkElements(soup)
      val drudgeLinks = linkElements.map(drudgeLinkFromJsoupElement(_, pageTs))
      drudgeLinks
    })
  }

}