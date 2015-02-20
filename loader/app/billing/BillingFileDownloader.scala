package billing

import java.io.File
import scala.collection.JavaConversions._
import aws.AWS
import com.amazonaws.services.s3.model.GetObjectRequest
import conf.Configuration
import log.ClassLogger


object BillingFileDownloader extends ClassLogger {
  val filenameSuffixTemplate = "-aws-billing-detailed-line-items-with-resources-and-tags-%04d-%02d.csv.zip"

  val dataDir = new File("data")

  def billingFileFor(month: Month): Option[File] = {
    val desiredFilenamePostfix = filenameSuffixTemplate.format(month.year, month.month)

    val result = AWS.s3.listObjects(Configuration.billingS3Bucket)

    result.getObjectSummaries.find(_.getKey.endsWith(desiredFilenamePostfix)).map { o =>
      log.info(s"object ${o.getKey} lastmod ${o.getLastModified} etag ${o.getETag}")

      val targetFile = new File(dataDir, o.getKey)
      val req = new GetObjectRequest(o.getBucketName, o.getKey)

      logAround(s"Downloading to ${targetFile.getCanonicalPath}") {
        AWS.s3.getObject(req, targetFile)
      }

      targetFile
    }
  }

}
