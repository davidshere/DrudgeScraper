package drudgescraper

import scala.collection.JavaConverters._

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode, DataNode}
import org.jsoup.select.{Elements, NodeVisitor, NodeFilter}
import org.jsoup.parser.Tag

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object LinksFromDrudgePage {

  //import PageMetadataParser.mainAndSplash
  import PageMetadataParser.enrichMainAndSplashFromTopElementsDiv
  import DrudgeScraper.DrudgeLink
  
  implicit def drudgeLinkFromJsoupElement(elem: Element)(implicit pageDate: LocalDateTime): DrudgeLink = 
    DrudgeLink(
        elem.attr("href"),
        pageDate,
        elem.text,
        elem.id=="splash",
        elem.id=="top"
    )
     
  
  def transformPage(page: Document, pageDt: LocalDateTime): List[DrudgeLink] = {
    implicit val pageDate = pageDt
    val enrichedPage = enrichMainAndSplashFromTopElementsDiv(page)
    val links = enrichedPage.select("a").asScala.toList
    links.map(drudgeLinkFromJsoupElement(_))
 
  }
}