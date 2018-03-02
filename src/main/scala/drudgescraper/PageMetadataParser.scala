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
      if (splashHeadlinesFound.size !=0 ) splashHeadlinesFound.asScala.toList.head
      else page.select("img[src~=logo9.gif]").parents.first()
    }
    val allLinks = page.select("a[href]").asScala.toList
    val indexOfSplash = allLinks.indexOf(splash)
    
    page.select("center + br + br + a").attr("id", "top")
    
    page
  }
  

  def enrich(doc: Document): Document =
    if(doc.select("div#drudgeTopHeadlines").isEmpty()) enrichMainAndSplashFromSelectorsAndAttributes(doc)
    else enrichMainAndSplashFromTopElementsDiv(doc: Document)

}