package skinny.controller.feature

import org.scalatra.{ Route, RouteTransformer, HttpMethod }
import skinny.controller.Constants
import skinny._
import javax.servlet.{ FilterRegistration, Filter, DispatcherType }

/**
 * RichRoute support.
 */
trait RichRouteFeature extends org.scalatra.NonMacroDSL.ScalatraBase { self: SkinnyControllerBase =>

  /**
   * Override to append HTTP method information to Route objects.
   */
  override protected def addRoute(method: HttpMethod, transformers: Seq[RouteTransformer], action: => Any): Route = {
    val route = super.addRoute(method, transformers, action)
    route.copy(metadata = route.metadata.updated(Constants.RouteMetadataHttpMethodCacheKey, method))
  }

  private def toNormalizedRoutePath(path: String): String = path match {
    case "/" => "/"
    case p if p.endsWith("/*") => p
    case p if p.endsWith("/") => p + "*"
    case _ => path + "/*"
  }

  private def allRoutePaths: Seq[String] = {
    routes.entryPoints
      .flatMap(_.split("\\s+").tail.headOption.map(_.split("[:\\?]+").head))
      .distinct
      .flatMap { path =>
        if (path.endsWith(".")) respondTo.map(format => path + format.name)
        else Seq(path)
      }
  }

  def mount(ctx: ServletContext): Unit = {
    this match {
      case filter: Filter =>
        allRoutePaths.foreach { path =>
          val name = this.getClass.getName
          val registration: FilterRegistration = {
            Option(ctx.getFilterRegistration(name)).getOrElse(ctx.addFilter(name, this.asInstanceOf[Filter]))
          }
          if (registration != null) {
            registration.addMappingForUrlPatterns(
              java.util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, toNormalizedRoutePath(path))
          } else {
            logger.info("FilterRegistration is empty. Skipped.")
          }
        }
      case _ =>
        try ctx.mount(this, "/")
        catch {
          case e: NullPointerException if SkinnyEnv.isTest() =>
            logger.info("Skipped NPE when mocking servlet APIs.")
        }
    }
  }

}
