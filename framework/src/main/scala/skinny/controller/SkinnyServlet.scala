package skinny.controller

import skinny.controller.feature.FileUploadFeature

/**
 * SkinnyController as a Servlet.
 */
class SkinnyServlet
  extends org.scalatra.NonMacroDSL.ScalatraServlet
  with SkinnyControllerBase
  with SkinnyWebPageControllerFeatures
