package org.scalatra

object NonMacroDSL {

  type ScalatraBase = org.scalatra.ScalatraBaseFallback
  type ScalatraServlet = org.scalatra.ScalatraServletFallback
  type ScalatraFilter = org.scalatra.ScalatraFilterFallback

}

