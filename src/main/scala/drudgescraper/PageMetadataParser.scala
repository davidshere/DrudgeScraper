package drudgescraper

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode, DataNode}
import org.jsoup.select.{Elements, NodeVisitor, NodeFilter}
import org.jsoup.parser.Tag

import scala.collection.JavaConverters._


object PageMetadataParser {
  
  //import DrudgeScraper.DrudgeLink
  
  def enrichMainAndSplashFromTopElementsDiv(page: Document): Document = {
    val drudgeTopHeadlineDiv = page.select("#drudgeTopHeadlines")
    
    // modify `page` in place
    drudgeTopHeadlineDiv.select("font[size=+7]").select("a[href]").attr("id", "splash")
    drudgeTopHeadlineDiv.select("a").select(":not(#splash)").attr("id", "top")

    page
  }


  def enrichMainAndSplashFromSelectorsAndAttributes(page: Document): Document = {
    // modify `page` in place
    page.select("font[size=+7]").select("a:not(:has(img))").attr("id", "splash")
    
    // need to find a splash element, so we know where to work backwards
    // from. either the actual splash element or the img element containing 
    // the headline. we know top headlines won't be after that. 
    val splash = {
      val splashHeadlinesFound = page.select("#splash")
      if (splashHeadlinesFound.size !=0 ) splashHeadlinesFound.first()
      else page.select("img[src~=logo9.gif]").parents.first()
    }

    // now we'll grab any links before the splash, but after the navigation bar
    // we can find what went in the navigation bar because they have the target "_top"
    val allLinks = page.select("a[href]").asScala.toList
    val indexOfSplash = allLinks.indexOf(splash)
    val firstLinks = allLinks.take(indexOfSplash).toSet
    val aElementsWithTargets = page.select("a[target]").asScala.toSet
    
    val topElements = firstLinks diff aElementsWithTargets
    for (elem <- topElements) {
      elem.attr("id", "top")
    }
    
    page
  }
  

  def enrich(doc: Document): Document =
    if(doc.select("div#drudgeTopHeadlines").isEmpty()) enrichMainAndSplashFromSelectorsAndAttributes(doc)
    else enrichMainAndSplashFromTopElementsDiv(doc: Document)

}