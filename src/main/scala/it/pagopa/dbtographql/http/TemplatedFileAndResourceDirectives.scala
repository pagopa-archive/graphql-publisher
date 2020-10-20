package it.pagopa.dbtographql.http

import java.io.StringWriter

import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.model.{ContentType, DateTime, HttpEntity}
import akka.http.scaladsl.server.directives.BasicDirectives.{extractSettings, pass}
import akka.http.scaladsl.server.directives.CacheConditionDirectives.conditional
import akka.http.scaladsl.server.directives.FileAndResourceDirectives.ResourceFile
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.{complete, reject}
import akka.http.scaladsl.server.directives.{CodingDirectives, ContentTypeResolver, FileAndResourceDirectives, RangeDirectives}
import akka.http.scaladsl.server.{Directive0, Route}
import com.github.mustachejava.DefaultMustacheFactory

import scala.collection.mutable
import scala.collection.mutable._
import scala.jdk.CollectionConverters._

@SuppressWarnings(
  Array(
    "org.wartremover.warts.DefaultArguments",
    "org.wartremover.warts.ImplicitParameter",
    "org.wartremover.warts.Overloading",
    "org.wartremover.warts.Nothing",
    "org.wartremover.warts.AsInstanceOf"
  )
)
trait TemplatedFileAndResourceDirectives extends FileAndResourceDirectives {

  private val mf = new DefaultMustacheFactory

  private def conditionalFor(length: Long, lastModified: Long): Directive0 =
    extractSettings.flatMap(settings =>
      if (settings.fileGetConditional) {
        val tag = java.lang.Long.toHexString(lastModified ^ java.lang.Long.reverse(length))
        val lastModifiedDateTime = DateTime(math.min(lastModified, System.currentTimeMillis))
        conditional(EntityTag(tag), lastModifiedDateTime)
      } else pass)

  private val withRangeSupportAndPrecompressedMediaTypeSupport =
    RangeDirectives.withRangeSupport &
      CodingDirectives.withPrecompressedMediaTypeSupport

  def getFromResourceTemplated(resourceName: String, contentType: ContentType, scopes: HashMap[String, String], classLoader: ClassLoader = _defaultClassLoader): Route =
    if (!resourceName.endsWith("/"))
      get {
        Option(classLoader.getResource(resourceName)) flatMap ResourceFile.apply match {
          case Some(ResourceFile(_, length, lastModified)) =>
            conditionalFor(length, lastModified) {
              if (length > 0) {
                withRangeSupportAndPrecompressedMediaTypeSupport {
                  val writer = mf.compile(resourceName).execute(new StringWriter(), scopes.asJava).asInstanceOf[StringWriter]
                  writer.flush()
                  writer.close()
                  complete(HttpEntity(contentType, writer.toString.getBytes("UTF-8")))
                }
              } else complete(HttpEntity.Empty)
            }
          case _ => reject // not found or directory
        }
      }
    else reject

  def getFromResourceTemplated(resourceName: String, scopes: mutable.HashMap[String, String])(implicit resolver: ContentTypeResolver): Route =
    getFromResourceTemplated(resourceName, resolver(resourceName), scopes)
}
