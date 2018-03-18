package drudgescraper

import scala.collection.JavaConverters._

import org.jsoup.nodes.{Document, Element}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object LinksFromDrudgePage {

  import PageMetadataParser.enrich
     
  def transformPage(page: Document): List[Element] = {
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
}