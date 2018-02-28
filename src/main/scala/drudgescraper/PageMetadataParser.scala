package drudgescraper

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, Node, TextNode, DataNode}
import org.jsoup.select.{Elements, NodeVisitor, NodeFilter}
import org.jsoup.parser.Tag

import scala.collection.JavaConverters._


object PageMetadataParser {
  
  import DrudgeScraper.DrudgeLink

  /*
   * More recent iterations of the DrudgeReport have a div #drudgeTopHeadlines
   * that we can use, naturally, to identify the top headlines.
   */
  def mainAndSplashFromTopElementsDiv(page: Document): (Set[Element], Set[Element]) = {
    val drudgeTopHeadlineDiv = page.select("#drudgeTopHeadlines")
    val drudgeTopHeadlineSet = drudgeTopHeadlineDiv.select("a[href]").asScala.toSet

    val splashElements: Set[Element] = drudgeTopHeadlineDiv.select("font[size=+7]").select("a[href]").asScala.toSet
    val nonSplashElements: Set[Element] = drudgeTopHeadlineSet.diff(splashElements)
    (splashElements, nonSplashElements)
  }

  def mainAndSplashFromSelectorsAndAttributes(doc: Document): (Set[Element], Set[Element]) = {
    val splashElements = doc.select("font[size=+7]").select("a[href!=http://www.drudgereport.com/]").asScala.toSet
    val nonSplashElements = doc.select("center + br + br + a").asScala.toSet
    (splashElements, nonSplashElements)
  }
  

  def mainAndSplash(doc: Document): (Set[Element], Set[DrudgeLink]) = {
    val (splashElements, topElements) = if(doc.select("div#drudgeTopHeadlines").isEmpty()) {
      mainAndSplashFromSelectorsAndAttributes(doc)
    }
    else {
      mainAndSplashFromTopElementsDiv(doc: Document)
    }
    
   val splashLinks: Set[(Element, DrudgeLink)] = splashElements.map(elem => (elem, DrudgeLink("date", elem.attr("href"), elem.text(), true, false)))
   val topLinks: Set[(Element, DrudgeLink)] = topElements.map(elem => (elem, DrudgeLink("date", elem.attr("href"), elem.text(), false, true)))

   (splashLinks ++ topLinks).unzip  
  }
}